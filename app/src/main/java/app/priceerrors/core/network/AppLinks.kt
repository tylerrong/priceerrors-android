package app.priceerrors.core.network

/**
 * Outbound links the app opens in a browser. Kept in one place — matching the
 * iOS client's `AppConfig` — so a moved invite or renamed domain is a one-line
 * change rather than a grep across screens.
 */
object AppLinks {
    /** Discord invite — linked from Profile. */
    const val DISCORD = "https://discord.gg/ugKbqEG6hG"

    /** Cashback / stacking referral links — Maximize deals sheet on You. */
    const val RAKUTEN = "https://www.rakuten.com/r/TYLERR1402?eeid=28187"
    const val CAPITAL_ONE_SHOPPING =
        "https://capitaloneshopping.com/r/30943794-5322-4ef1-b296-7da1079bc3e5"
    const val CHECKMATE = "https://chckmt.app/iyv0ae"
}
