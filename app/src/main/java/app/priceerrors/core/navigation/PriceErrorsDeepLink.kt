package app.priceerrors.core.navigation

import java.net.URI

object PriceErrorsDeepLink {
    /**
     * The site 307s the apex to www, so links that reach us may carry either
     * host. Both are trusted; anything else is not.
     */
    private val trustedHosts = setOf("priceerrors.app", "www.priceerrors.app")

    fun dealId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (uri.host?.lowercase() !in trustedHosts) return null
        val segments = uri.path.orEmpty().split('/').filter(String::isNotBlank)
        if (segments.size != 2 || segments[0] !in setOf("deal", "deals")) return null
        return segments[1].trim().takeIf(String::isNotEmpty)
    }
}
