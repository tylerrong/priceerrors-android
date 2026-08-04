package app.priceerrors.core.auth

import app.priceerrors.core.network.ApiConfig
import app.priceerrors.core.network.ApiError
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Exchanges a Google ID token for a Supabase session and keeps that session
 * fresh.
 *
 * This is the piece that makes the user "real" to the backend: the PriceErrors
 * server identifies callers purely by the Supabase JWT, so a Google credential
 * on its own authorises nothing.
 */
class SupabaseAuthClient(
    private val httpClient: OkHttpClient,
    private val sessionStore: SessionStorage,
    private val json: Json,
    private val supabaseUrl: String = ApiConfig.supabaseUrl,
    private val anonKey: String = ApiConfig.supabaseAnonKey,
) : AccessTokenProvider {
    private val _session = MutableStateFlow(sessionStore.load())

    /** Current session, or null when signed out. Survives process death. */
    val session: StateFlow<SupabaseSession?> = _session.asStateFlow()

    /** Serialises refreshes so concurrent requests mint one token, not N. */
    private val refreshMutex = Mutex()

    val isSignedIn: Boolean
        get() = _session.value != null

    /**
     * Trades the Google ID token from [GoogleCredentialAuthClient] for a
     * Supabase session and persists it.
     */
    suspend fun signInWithGoogle(googleIdToken: String): Result<SupabaseSession> =
        requestToken(
            query = "grant_type=id_token",
            body = buildString {
                append("{\"provider\":\"google\",\"id_token\":")
                append(json.encodeToString(googleIdToken))
                append("}")
            },
        ).onSuccess { session ->
            sessionStore.save(session)
            _session.value = session
        }

    /**
     * Signs in with an email and password against the same Supabase project the
     * iOS client uses, so an account created on either platform works on both.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<AuthIdentity> =
        post(
            url = "$supabaseUrl/auth/v1/token?grant_type=password",
            body = buildString {
                append("{\"email\":").append(json.encodeToString(email.trim()))
                append(",\"password\":").append(json.encodeToString(password))
                append("}")
            },
        ).mapCatching { payload ->
            val decoded = json.decodeFromString<SupabaseTokenResponse>(payload)
            val userId = decoded.user?.id
                ?: throw ApiError.Decoding(IllegalStateException("Session had no user id."))
            val session = SupabaseSession(
                accessToken = decoded.accessToken,
                refreshToken = decoded.refreshToken,
                expiresAt = java.time.Instant.now().plusSeconds(decoded.expiresIn),
                userId = userId,
                email = decoded.user.email,
            )
            adopt(session)
            AuthIdentity(
                displayName = displayNameFor(decoded.user.userMetadata?.fullName, decoded.user.email ?: email),
                email = decoded.user.email ?: email.trim(),
                idToken = decoded.accessToken,
                profilePhotoUrl = null,
            )
        }

    /**
     * Creates an email account. [name] is stored as `full_name` in user
     * metadata, matching what iOS writes, so the display name survives a
     * sign-in on the other platform.
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        name: String,
    ): Result<EmailSignUpResult> =
        post(
            url = "$supabaseUrl/auth/v1/signup",
            body = buildString {
                append("{\"email\":").append(json.encodeToString(email.trim()))
                append(",\"password\":").append(json.encodeToString(password))
                if (name.isNotBlank()) {
                    append(",\"data\":{\"full_name\":").append(json.encodeToString(name.trim())).append("}")
                }
                append("}")
            },
        ).mapCatching { payload ->
            val decoded = json.decodeFromString<SupabaseSignUpResponse>(payload)
            val accessToken = decoded.accessToken
            val refreshToken = decoded.refreshToken
            val userId = decoded.user?.id ?: decoded.id

            // No tokens means the project requires email confirmation. Report it
            // rather than pretending to be signed in — every API call would 401.
            if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank() || userId.isNullOrBlank()) {
                return@mapCatching EmailSignUpResult.ConfirmationRequired
            }

            val resolvedEmail = decoded.user?.email ?: decoded.email ?: email.trim()
            val session = SupabaseSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresAt = java.time.Instant.now().plusSeconds(decoded.expiresIn),
                userId = userId,
                email = resolvedEmail,
            )
            adopt(session)
            EmailSignUpResult.SignedIn(
                identity = AuthIdentity(
                    displayName = displayNameFor(name.ifBlank { null }, resolvedEmail),
                    email = resolvedEmail,
                    idToken = accessToken,
                    profilePhotoUrl = null,
                ),
                session = session,
            )
        }

    private fun adopt(session: SupabaseSession) {
        sessionStore.save(session)
        _session.value = session
    }

    private fun displayNameFor(fullName: String?, email: String): String =
        fullName?.trim()?.takeIf(String::isNotEmpty)
            ?: email.substringBefore('@').takeIf(String::isNotEmpty)
            ?: "User"

    /**
     * Returns a valid access token, refreshing first if the current one has
     * expired. Null means the caller must sign in again — either there is no
     * session, or the refresh token itself was rejected.
     */
    override suspend fun currentAccessToken(): String? {
        val existing = _session.value ?: return null
        if (!existing.isExpired()) return existing.accessToken

        return refreshMutex.withLock {
            // Re-read inside the lock: another coroutine may have refreshed
            // while this one was waiting, in which case there is nothing to do.
            val current = _session.value ?: return@withLock null
            if (!current.isExpired()) return@withLock current.accessToken

            requestToken(
                query = "grant_type=refresh_token",
                body = "{\"refresh_token\":${json.encodeToString(current.refreshToken)}}",
            ).fold(
                onSuccess = { refreshed ->
                    sessionStore.save(refreshed)
                    _session.value = refreshed
                    refreshed.accessToken
                },
                onFailure = { error ->
                    // A refused refresh token is terminal — drop the session so
                    // the UI can prompt a fresh sign-in. Transport failures are
                    // not: keep the session and let the caller retry later.
                    if (error is ApiError.Unauthorized) clearSession()
                    null
                },
            )
        }
    }

    /** Drops the local session. Google credential state is cleared separately. */
    fun clearSession() {
        sessionStore.clear()
        _session.value = null
    }

    /**
     * POSTs to a Supabase auth endpoint and returns the raw 2xx body.
     *
     * Errors are classified through [EmailAuthError] first, because Supabase
     * distinguishes "wrong password" from "unconfirmed email" only in the
     * response prose. Anything unrecognised falls back to the generic mapping.
     */
    private suspend fun post(url: String, body: String): Result<String> {
        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            return Result.failure(ApiError.NotConfigured)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return withContext(Dispatchers.IO) {
            try {
                httpClient.newCall(request).execute().use { response ->
                    val payload = response.body.string()
                    if (response.isSuccessful) {
                        Result.success(payload)
                    } else {
                        Result.failure(
                            EmailAuthError.from(payload)
                                ?: when (response.code) {
                                    401, 400 -> ApiError.Unauthorized
                                    422 -> ApiError.Server(response.code, payload)
                                    429 -> ApiError.RateLimited
                                    else -> ApiError.Server(response.code, payload)
                                },
                        )
                    }
                }
            } catch (error: IOException) {
                Result.failure(ApiError.Transport(error))
            }
        }
    }

    private suspend fun requestToken(query: String, body: String): Result<SupabaseSession> {
        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            return Result.failure(ApiError.NotConfigured)
        }

        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/token?$query")
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return withContext(Dispatchers.IO) {
            try {
                httpClient.newCall(request).execute().use { response ->
                    val payload = response.body.string()
                    if (!response.isSuccessful) {
                        return@use Result.failure(
                            // Supabase answers 400 for a bad/expired grant just
                            // as often as 401, so both mean "sign in again".
                            if (response.code == 401 || response.code == 400) {
                                ApiError.Unauthorized
                            } else {
                                ApiError.Server(response.code, payload)
                            },
                        )
                    }

                    val decoded = try {
                        json.decodeFromString<SupabaseTokenResponse>(payload)
                    } catch (error: Exception) {
                        return@use Result.failure(ApiError.Decoding(error))
                    }

                    val userId = decoded.user?.id
                    if (userId.isNullOrBlank()) {
                        return@use Result.failure(
                            ApiError.Decoding(IllegalStateException("Session had no user id.")),
                        )
                    }

                    Result.success(
                        SupabaseSession(
                            accessToken = decoded.accessToken,
                            refreshToken = decoded.refreshToken,
                            expiresAt = java.time.Instant.now().plusSeconds(decoded.expiresIn),
                            userId = userId,
                            email = decoded.user.email,
                        ),
                    )
                }
            } catch (error: IOException) {
                Result.failure(ApiError.Transport(error))
            }
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
