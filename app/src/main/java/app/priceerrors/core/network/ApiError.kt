package app.priceerrors.core.network

/**
 * Failure modes the UI needs to tell apart. Mirrors the iOS `APIClient.APIError`
 * cases so both clients react to the same server contract.
 */
sealed class ApiError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** No backend configured in this build — see [ApiConfig.isConfigured]. */
    object NotConfigured : ApiError(
        "This build has no backend configured. Set PRICEERRORS_SERVER_BASE_URL, " +
            "PRICEERRORS_SERVER_API_KEY, PRICEERRORS_SUPABASE_URL and " +
            "PRICEERRORS_SUPABASE_ANON_KEY in local.properties.",
    ) {
        private fun readResolve(): Any = NotConfigured
    }

    /** No Supabase session, or the server rejected the one we sent (401). */
    object Unauthorized : ApiError("Sign in to continue.") {
        private fun readResolve(): Any = Unauthorized
    }

    /** 403 with `reason=pro_required`. */
    object ProRequired : ApiError("PriceErrors Pro is required for this deal.") {
        private fun readResolve(): Any = ProRequired
    }

    /** 402 with `reason=limit_reached` — free daily claims exhausted. */
    object LimitReached : ApiError("You've used all your free deals for today.") {
        private fun readResolve(): Any = LimitReached
    }

    /** 429 — caller tripped a per-user rate limit. */
    object RateLimited : ApiError("Too many requests. Try again shortly.") {
        private fun readResolve(): Any = RateLimited
    }

    /** Any other non-2xx response. */
    class Server(val statusCode: Int, val body: String? = null) :
        ApiError("The server returned an error ($statusCode).")

    /** Connection failure, timeout, DNS, TLS. */
    class Transport(cause: Throwable) :
        ApiError("Couldn't reach PriceErrors. Check your connection.", cause)

    /** Response did not match the expected shape. */
    class Decoding(cause: Throwable) :
        ApiError("Received an unexpected response from PriceErrors.", cause)
}
