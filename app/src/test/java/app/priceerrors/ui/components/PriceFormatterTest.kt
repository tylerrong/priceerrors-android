package app.priceerrors.ui.components

import app.priceerrors.core.data.FakeDealData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceFormatterTest {
    @Test
    fun `zero price is announced as free`() {
        assertEquals("FREE", formatPrice(0, "USD"))
    }

    @Test
    fun `whole and fractional dollar prices use appropriate precision`() {
        assertEquals("$25", formatPrice(2_500, "USD"))
        assertEquals("$14.99", formatPrice(1_499, "USD"))
    }

    @Test
    fun `deal accessibility label summarizes the actionable card`() {
        val deal = FakeDealData.deals.first()

        val label = dealAccessibilityLabel(deal)

        assertTrue(label.contains(deal.title))
        assertTrue(label.contains(deal.brand))
        assertTrue(label.contains(formatPrice(deal.priceInCents, deal.currencyCode)))
        assertEquals(deal.discountPercent > 0, label.contains("percent off"))
        assertFalse(label.contains("https://"))
    }
}
