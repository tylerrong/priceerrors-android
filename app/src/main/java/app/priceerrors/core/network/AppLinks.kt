package app.priceerrors.core.network

/**
 * Outbound links the app opens in a browser. Kept in one place — matching the
 * iOS client's `AppConfig` — so a moved invite or renamed domain is a one-line
 * change rather than a grep across screens.
 */
object AppLinks {
    /** Community Discord — linked from the banner on the Community page. */
    const val DISCORD = "https://discord.gg/ugKbqEG6hG"
}
