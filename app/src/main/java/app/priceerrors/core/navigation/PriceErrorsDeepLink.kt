package app.priceerrors.core.navigation

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Universal links + custom-scheme deal routes. Mirrors iOS `DeepLinkRouting`.
 */
object PriceErrorsDeepLink {
    const val WEBSITE_BASE_URL = "https://www.priceerrors.app"
    const val CUSTOM_SCHEME = "priceerrors"

    private val trustedHosts = setOf("priceerrors.app", "www.priceerrors.app")

    sealed class Route {
        data object Home : Route()
        data class Deal(val id: String) : Route()
        data class GoogleOAuthCallback(val url: String) : Route()
    }

    fun route(url: String?): Route? {
        if (url.isNullOrBlank()) return null
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        return when (uri.scheme?.lowercase()) {
            CUSTOM_SCHEME -> routeCustomScheme(uri, url)
            "https", "http" -> routeUniversalLink(uri)
            else -> null
        }
    }

    /** Convenience for callers that only need a deal id. */
    fun dealId(url: String?): String? =
        (route(url) as? Route.Deal)?.id

    fun dealShareUrl(dealId: String): String =
        "$WEBSITE_BASE_URL/deal/${encodePathComponent(dealId)}"

    fun dealDeepLinkUrl(dealId: String): String =
        "$CUSTOM_SCHEME://deal/${encodePathComponent(dealId)}"

    private fun routeCustomScheme(uri: URI, originalUrl: String): Route? {
        return when (uri.host?.lowercase()) {
            "home", null -> Route.Home
            "deal" -> {
                val id = dealIdFromPath(uri.path) ?: return Route.Home
                Route.Deal(id)
            }
            "auth" -> {
                if (uri.path == "/callback") Route.GoogleOAuthCallback(originalUrl) else null
            }
            else -> null
        }
    }

    private fun routeUniversalLink(uri: URI): Route? {
        if (uri.scheme.equals("http", ignoreCase = true)) return null
        if (uri.host?.lowercase() !in trustedHosts) return null
        val segments = uri.path.orEmpty().split('/').filter(String::isNotBlank)
        if (segments.isEmpty()) return Route.Home
        val first = segments[0].lowercase()
        if (first in setOf("deal", "deals") && segments.size >= 2) {
            return Route.Deal(decodePathComponent(segments[1]))
        }
        return Route.Home
    }

    private fun dealIdFromPath(path: String?): String? {
        val segment = path.orEmpty().split('/').filter(String::isNotBlank).firstOrNull()
            ?: return null
        return decodePathComponent(segment).takeIf(String::isNotEmpty)
    }

    private fun encodePathComponent(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

    private fun decodePathComponent(value: String): String =
        runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }.getOrDefault(value)
}
