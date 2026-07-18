package app.priceerrors.core.navigation

import java.net.URI

object PriceErrorsDeepLink {
    fun dealId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (!uri.host.equals("priceerrors.app", ignoreCase = true)) return null
        val segments = uri.path.orEmpty().split('/').filter(String::isNotBlank)
        if (segments.size != 2 || segments[0] !in setOf("deal", "deals")) return null
        return segments[1].trim().takeIf(String::isNotEmpty)
    }
}
