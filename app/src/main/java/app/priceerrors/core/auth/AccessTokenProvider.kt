package app.priceerrors.core.auth

/**
 * Supplies the bearer token for server calls.
 *
 * Kept separate from [SupabaseAuthClient] so the API layer depends on the token
 * it needs rather than the whole sign-in stack — which also lets tests drive
 * expiry and refresh behaviour without a real Supabase project.
 */
fun interface AccessTokenProvider {
    /** Returns a valid access token, or null when the user must sign in again. */
    suspend fun currentAccessToken(): String?
}
