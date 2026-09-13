package app.priceerrors.core.auth

import app.priceerrors.core.network.ApiConfig
import app.priceerrors.core.network.ApiError
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrl
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
    private val googleOAuthPkceStore: GoogleOAuthPkceStore = InMemoryGoogleOAuthPkceStore(),
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
     *
     * [nonce] must be the raw (unhashed) value whose SHA-256 hex digest was
     * passed to Credential Manager — matching Supabase's native Google flow.
     */
    suspend fun signInWithGoogle(
        googleIdToken: String,
        nonce: String? = null,
    ): Result<SupabaseSession> =
        requestToken(
            query = "grant_type=id_token",
            body = buildString {
                append("{\"provider\":\"google\",\"id_token\":")
                append(json.encodeToString(googleIdToken))
                if (!nonce.isNullOrBlank()) {
                    append(",\"nonce\":")
                    append(json.encodeToString(nonce))
                }
                append("}")
            },
        ).onSuccess { session ->
            sessionStore.save(session)
            _session.value = session
        }

    /**
     * Starts a browser-based Google OAuth flow through Supabase. This is a
     * standards-based fallback for Play installs whose otherwise-correct native
     * OAuth registration is rejected by Google Play services.
     */
    fun beginGoogleOAuth(): Result<String> = runCatching {
        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            throw ApiError.NotConfigured
        }

        val verifierBytes = ByteArray(32).also(SecureRandom()::nextBytes)
        val verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(verifierBytes)
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256")
                .digest(verifier.toByteArray(StandardCharsets.US_ASCII)),
        )
        googleOAuthPkceStore.save(verifier)

        "$supabaseUrl/auth/v1/authorize".toHttpUrl()
            .newBuilder()
            .addQueryParameter("provider", "google")
            .addQueryParameter("redirect_to", GOOGLE_OAUTH_REDIRECT_URI)
            .addQueryParameter("code_challenge", challenge)
            .addQueryParameter("code_challenge_method", "s256")
            .build()
            .toString()
    }.onFailure {
        googleOAuthPkceStore.clear()
    }

    /**
     * Exchanges the one-time authorization code returned to the app for the
     * same persisted Supabase session used by native and email sign-in.
     */
    suspend fun completeGoogleOAuth(callbackUrl: String): Result<AuthIdentity> = runCatching {
        val callback = URI(callbackUrl)
        if (
            !callback.scheme.equals("priceerrors", ignoreCase = true) ||
            !callback.host.equals("auth", ignoreCase = true) ||
            callback.path != "/callback"
        ) {
            throw IllegalArgumentException("Google returned an invalid callback.")
        }

        val parameters = callback.rawQuery.orEmpty()
            .split('&')
            .filter(String::isNotBlank)
            .associate { pair ->
                val pieces = pair.split('=', limit = 2)
                decodeUrlComponent(pieces[0]) to decodeUrlComponent(pieces.getOrElse(1) { "" })
            }
        parameters["error"]?.takeIf(String::isNotBlank)?.let { error ->
            googleOAuthPkceStore.clear()
            val description = parameters["error_description"]?.takeIf(String::isNotBlank)
            throw IllegalStateException(description ?: "Google sign-in failed: $error")
        }

        val authorizationCode = parameters["code"]?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("Google did not return an authorization code.")
        val verifier = googleOAuthPkceStore.consume()
            ?: throw IllegalStateException("Google sign-in expired. Please try again.")

        val session = requestToken(
            query = "grant_type=pkce",
            body = buildString {
                append("{\"auth_code\":").append(json.encodeToString(authorizationCode))
                append(",\"code_verifier\":").append(json.encodeToString(verifier))
                append("}")
            },
        ).getOrThrow()
        adopt(session)

        val resolvedEmail = session.email.orEmpty()
        AuthIdentity(
            displayName = resolvedEmail.substringBefore('@').takeIf(String::isNotBlank) ?: "User",
            email = resolvedEmail,
            idToken = session.accessToken,
            profilePhotoUrl = null,
        )
    }.onFailure {
        googleOAuthPkceStore.clear()
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
        const val GOOGLE_OAUTH_REDIRECT_URI = "priceerrors://auth/callback"

        fun decodeUrlComponent(value: String): String =
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }
}
