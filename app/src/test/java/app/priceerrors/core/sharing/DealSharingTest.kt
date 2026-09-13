package app.priceerrors.core.sharing

import app.priceerrors.core.model.Deal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DealSharingTest {
    @Test
    fun `share message matches iOS format`() {
        val deal = sampleDeal()
        assertEquals("https://www.priceerrors.app/deal/42", DealSharing.shareUrl(deal))
        assertTrue(DealSharing.shareMessage(deal).startsWith("AirPods - 60% off\n"))
        assertTrue(DealSharing.shareMessage(deal).contains("https://www.priceerrors.app/deal/42"))
        assertFalse(DealSharing.shareMessage(deal).contains("Check it out on priceerrors"))
        assertFalse(DealSharing.shareMessage(deal).contains("PriceErrors"))
        assertFalse(DealSharing.shareMessage(deal).contains("—"))
    }

    @Test
    fun `free deals use FREE discount text`() {
        val deal = sampleDeal(priceInCents = 0, originalPriceInCents = 1000, discount = 100)
        assertEquals("FREE", DealSharing.shareDiscountText(deal))
    }

    private fun sampleDeal(
        priceInCents: Long = 7900,
        originalPriceInCents: Long = 19900,
        discount: Int = 60,
    ): Deal = Deal(
        id = "42",
        sourceId = "src-42",
        brand = "Target",
        title = "AirPods",
        tag = "Hot",
        heat = "High",
        priceInCents = priceInCents,
        originalPriceInCents = originalPriceInCents,
        category = "Tech",
        postedAt = Instant.parse("2026-01-01T00:00:00Z"),
        description = "Test",
        store = "Target",
        workingCount = 1,
        notWorkingCount = 0,
        userVote = null,
        steps = listOf("Step"),
        imageUrl = null,
        dealUrl = null,
    ).also {
        // discountPercent is derived; keep sample inputs consistent with expected %.
        require(it.discountPercent == discount || priceInCents == 0L)
    }
}
