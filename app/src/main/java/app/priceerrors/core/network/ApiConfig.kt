package app.priceerrors.core.network

import app.priceerrors.BuildConfig

/**
 * Backend endpoints and keys, supplied through `local.properties` / CI in the
 * same way as the Google and Firebase settings. Nothing here is a secret the
 * server trusts on its own: `X-API-Key` only gets a caller past the edge gate,
 * and every user-scoped route additionally requires a Supabase JWT.
 */
object ApiConfig {
    /** Trailing slashes are trimmed so path joins never produce `//deals`. */
    val serverBaseUrl: String = BuildConfig.SERVER_BASE_URL.trimEnd('/')

    val apiKey: String = BuildConfig.SERVER_API_KEY

    val supabaseUrl: String = BuildConfig.SUPABASE_URL.trimEnd('/')

    val supabaseAnonKey: String = BuildConfig.SUPABASE_ANON_KEY

    /**
     * False when a build has no backend wired up. Callers surface a
     * configuration error rather than firing requests at an empty host.
     */
    val isConfigured: Boolean
        get() = serverBaseUrl.isNotBlank() &&
            apiKey.isNotBlank() &&
            supabaseUrl.isNotBlank() &&
            supabaseAnonKey.isNotBlank()
}
