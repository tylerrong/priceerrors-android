package app.priceerrors.core.network

import app.priceerrors.core.auth.AccessTokenProvider
import app.priceerrors.core.model.DealVote
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * Typed access to the PriceErrors server.
 *
 * Mirrors `APIClient.swift`: every request carries `X-API-Key` for the edge
 * gate plus a `Bearer` Supabase JWT for user identity, and the paywall statuses
 * (402/403) are surfaced as distinct errors so the UI can present the paywall
 * instead of a generic failure.
 */
class PriceErrorsApi(
    private val httpClient: OkHttpClient,
    private val tokenProvider: AccessTokenProvider,
    private val json: Json,
    private val baseUrl: String = ApiConfig.serverBaseUrl,
    private val apiKey: String = ApiConfig.apiKey,
) {
    private val isConfigured: Boolean = baseUrl.isNotBlank() && apiKey.isNotBlank()

    suspend fun fetchDeals(
        page: Int = 1,
        limit: Int = 100,
        category: String? = null,
    ): Result<DealsResponseDto> {
        val url = (baseUrl.toHttpUrlOrNull() ?: return Result.failure(ApiError.NotConfigured))
            .newBuilder()
            .addPathSegment("deals")
            .addQueryParameter("page", page.toString())
            .addQueryParameter("limit", limit.toString())
            // The server buckets free-tier claims by the user's local day, so
            // the date has to come from the device, not the server's clock.
            .addQueryParameter("local_date", LocalDate.now().toString())
            .apply { if (!category.isNullOrBlank()) addQueryParameter("category", category) }
            .build()

        return execute(Request.Builder().url(url).get())
    }

    suspend fun claimDeal(dealId: String): Result<ClaimResponseDto> =
        execute(
            request("claims").post(
                jsonBody(
                    "deal_id" to dealId,
                    "local_date" to LocalDate.now().toString(),
                ),
            ),
        )

    suspend fun voteDeal(dealId: String, vote: DealVote): Result<VoteResponseDto> =
        execute(
            request("votes").post(
                jsonBody("deal_id" to dealId, "vote" to vote.wireValue),
            ),
        )

    suspend fun fetchUserDeals(
        page: Int = 1,
        limit: Int = 50,
        category: String? = null,
    ): Result<UserDealsResponseDto> {
        val url = (baseUrl.toHttpUrlOrNull() ?: return Result.failure(ApiError.NotConfigured))
            .newBuilder()
            .addPathSegment("user-deals")
            .addQueryParameter("page", page.toString())
            .addQueryParameter("limit", limit.toString())
            .apply { if (!category.isNullOrBlank()) addQueryParameter("category", category) }
            .build()

        return execute(Request.Builder().url(url).get())
    }

    /** Posts a community deal. Requires Pro; the server answers 403 otherwise. */
    suspend fun submitUserDeal(submission: UserDealSubmissionDto): Result<UserDealDto> =
        execute(
            request("user-deals").post(
                json.encodeToString(submission).toRequestBody(JSON_MEDIA_TYPE),
            ),
        )

    /** Owner-only delete; the server answers 403 for anyone else's post. */
    suspend fun deleteUserDeal(id: String): Result<Unit> =
        executeUnit(request("user-deals", id).delete())

    /**
     * Deletes the caller's account server-side. The server identifies the user
     * from the JWT, so no id is sent in the body.
     */
    suspend fun deleteAccount(): Result<Unit> =
        executeUnit(request("account").delete())

    private fun request(vararg segments: String): Request.Builder {
        val url = baseUrl.toHttpUrlOrNull()?.newBuilder()
            ?.apply { segments.forEach(::addPathSegment) }
            ?.build()
        return Request.Builder().apply { if (url != null) url(url) }
    }

    private fun jsonBody(vararg fields: Pair<String, String>): RequestBody =
        fields.joinToString(
            separator = ",",
            prefix = "{",
            postfix = "}",
        ) { (key, value) -> "${json.encodeToString(key)}:${json.encodeToString(value)}" }
            .toRequestBody(JSON_MEDIA_TYPE)

    private suspend inline fun <reified T> execute(builder: Request.Builder): Result<T> =
        send(builder) { payload ->
            try {
                Result.success(json.decodeFromString<T>(payload))
            } catch (error: Exception) {
                Result.failure(ApiError.Decoding(error))
            }
        }

    private suspend fun executeUnit(builder: Request.Builder): Result<Unit> =
        send(builder) { Result.success(Unit) }

    /**
     * Applies auth headers, performs the call, and maps the response.
     *
     * On a 401 the access token is refreshed once and the call replayed — a
     * token can expire between the pre-flight freshness check and the server
     * validating it, and a single silent retry avoids bouncing the user to the
     * sign-in screen for a clock-skew race.
     */
    private suspend fun <T> send(
        builder: Request.Builder,
        decode: (String) -> Result<T>,
    ): Result<T> {
        if (!isConfigured) return Result.failure(ApiError.NotConfigured)

        val token = tokenProvider.currentAccessToken()
            ?: return Result.failure(ApiError.Unauthorized)

        return withContext(Dispatchers.IO) {
            try {
                val first = call(builder, token)
                val response = if (first.code == 401) {
                    first.close()
                    val refreshed = tokenProvider.currentAccessToken()
                        ?: return@withContext Result.failure(ApiError.Unauthorized)
                    call(builder, refreshed)
                } else {
                    first
                }

                response.use {
                    val payload = it.body.string()
                    if (it.isSuccessful) decode(payload) else Result.failure(errorFor(it, payload))
                }
            } catch (error: IOException) {
                Result.failure(ApiError.Transport(error))
            }
        }
    }

    private fun call(builder: Request.Builder, token: String): Response =
        httpClient.newCall(
            builder
                .header("X-API-Key", apiKey)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .build(),
        ).execute()

    private fun errorFor(response: Response, payload: String): ApiError {
        val reason = runCatching {
            json.decodeFromString<ErrorResponseDto>(payload).reason
        }.getOrNull()

        return when {
            response.code == 401 -> ApiError.Unauthorized
            response.code == 403 && reason == "pro_required" -> ApiError.ProRequired
            response.code == 402 && reason == "limit_reached" -> ApiError.LimitReached
            response.code == 403 -> ApiError.ProRequired
            response.code == 429 -> ApiError.RateLimited
            else -> ApiError.Server(response.code, payload.take(500))
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
