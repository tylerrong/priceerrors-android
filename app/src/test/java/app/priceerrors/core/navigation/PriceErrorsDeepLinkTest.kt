package app.priceerrors.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PriceErrorsDeepLinkTest {
    @Test
    fun `extracts canonical deal id`() {
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
    fun `rejects untrusted or malformed links`() {
        assertNull(PriceErrorsDeepLink.dealId("http://priceerrors.app/deals/123"))
        assertNull(PriceErrorsDeepLink.dealId("https://evil.example/deals/123"))
        assertNull(PriceErrorsDeepLink.dealId("https://priceerrors.app/profile"))
        assertNull(PriceErrorsDeepLink.dealId("not a url"))
        // Lookalike hosts must not slip through the www allowance.
        assertNull(PriceErrorsDeepLink.dealId("https://wwwpriceerrors.app/deals/123"))
        assertNull(PriceErrorsDeepLink.dealId("https://www.priceerrors.app.evil.com/deals/123"))
        assertNull(PriceErrorsDeepLink.dealId("https://evil.www.priceerrors.app/deals/123"))
    }
}
