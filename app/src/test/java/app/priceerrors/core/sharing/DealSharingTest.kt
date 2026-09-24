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
        assertEquals("Check out AirPods deal:\nhttps://www.priceerrors.app/deal/42", DealSharing.shareMessage(deal))
        assertFalse(DealSharing.shareMessage(deal).contains("60% off"))
        assertFalse(DealSharing.shareMessage(deal).contains("Check it out on priceerrors"))
        assertFalse(DealSharing.shareMessage(deal).contains("PriceErrors"))
        assertFalse(DealSharing.shareMessage(deal).contains("—"))
    }

    @Test
    fun `free deals use FREE discount text`() {
        val deal = sampleDeal(priceInCents = 0, originalPriceInCents = 1000, discount = 100)
        assertEquals("FREE", DealSharing.shareDiscountText(deal))
    }

    @Test
    fun `preview and placeholder ids cannot be shared`() {
        assertFalse(DealSharing.canShare(sampleDeal(id = "preview_abc")))
        assertFalse(DealSharing.canShare(sampleDeal(id = "placeholder-1")))
        assertTrue(DealSharing.canShare(sampleDeal(id = "42")))
    }

    private fun sampleDeal(
        id: String = "42",
        priceInCents: Long = 7900,
        originalPriceInCents: Long = 19900,
        discount: Int = 60,
    ): Deal = Deal(
        id = id,
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
        require(it.discountPercent == discount || priceInCents == 0L || id.startsWith("preview_") || id.startsWith("placeholder"))
    }
}
