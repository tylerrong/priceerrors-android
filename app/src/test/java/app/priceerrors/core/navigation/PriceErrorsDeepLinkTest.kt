package app.priceerrors.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceErrorsDeepLinkTest {
    @Test
    fun `extracts canonical deal id from https links`() {
        assertEquals(
            "deal-food-123",
            PriceErrorsDeepLink.dealId("https://priceerrors.app/deals/deal-food-123"),
        )
        assertEquals("123", PriceErrorsDeepLink.dealId("https://priceerrors.app/deal/123"))
    }

    @Test
    fun `accepts the www host the site canonicalizes to`() {
        assertEquals("123", PriceErrorsDeepLink.dealId("https://www.priceerrors.app/deal/123"))
        assertEquals("123", PriceErrorsDeepLink.dealId("https://WWW.PriceErrors.app/deals/123"))
    }

    @Test
    fun `routes custom scheme deal and home`() {
        assertEquals(
            PriceErrorsDeepLink.Route.Deal("deal-123"),
            PriceErrorsDeepLink.route("priceerrors://deal/deal-123"),
        )
        assertEquals(
            PriceErrorsDeepLink.Route.Home,
            PriceErrorsDeepLink.route("priceerrors://home"),
        )
    }

    @Test
    fun `routes a google oauth callback without interpreting its code`() {
        val url = "priceerrors://auth/callback?code=one-time%2Bcode"

        assertEquals(
            PriceErrorsDeepLink.Route.GoogleOAuthCallback(url),
            PriceErrorsDeepLink.route(url),
        )
        assertNull(PriceErrorsDeepLink.route("priceerrors://auth/not-callback?code=ignored"))
    }

    @Test
    fun `builds share and custom-scheme urls`() {
        assertEquals(
            "https://www.priceerrors.app/deal/deal-123",
            PriceErrorsDeepLink.dealShareUrl("deal-123"),
        )
        assertEquals(
            "priceerrors://deal/deal-123",
            PriceErrorsDeepLink.dealDeepLinkUrl("deal-123"),
        )
    }

    @Test
    fun `rejects untrusted or malformed links`() {
        assertNull(PriceErrorsDeepLink.dealId("http://priceerrors.app/deals/123"))
        assertNull(PriceErrorsDeepLink.dealId("https://evil.example/deals/123"))
        assertNull(PriceErrorsDeepLink.dealId("not a url"))
        assertNull(PriceErrorsDeepLink.dealId("https://wwwpriceerrors.app/deals/123"))
        assertNull(PriceErrorsDeepLink.dealId("https://www.priceerrors.app.evil.com/deals/123"))
        assertNull(PriceErrorsDeepLink.dealId("https://evil.www.priceerrors.app/deals/123"))
        // Non-deal paths resolve to home, not a deal id.
        assertEquals(
            PriceErrorsDeepLink.Route.Home,
            PriceErrorsDeepLink.route("https://priceerrors.app/profile"),
        )
        assertTrue(PriceErrorsDeepLink.dealId("https://priceerrors.app/profile") == null)
    }
}
