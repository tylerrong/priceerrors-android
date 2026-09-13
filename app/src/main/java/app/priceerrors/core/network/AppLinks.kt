package app.priceerrors.core.network

/**
 * Outbound links the app opens in a browser. Kept in one place — matching the
 * iOS client's `AppConfig` — so a moved invite or renamed domain is a one-line
 * change rather than a grep across screens.
 */
object AppLinks {
    /** Discord invite — linked from Profile. */
    const val DISCORD = "https://discord.gg/ugKbqEG6hG"
}
