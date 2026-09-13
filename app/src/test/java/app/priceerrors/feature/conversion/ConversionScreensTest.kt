package app.priceerrors.feature.conversion

import app.priceerrors.feature.alerts.WatchKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversionScreensTest {
    @Test
    fun parsePostPurchaseWatchesSplitsEverySupportedSeparator() {
        val watches = parsePostPurchaseWatches(
            query = "AirPods, Nike; Target\nMacBook",
            minimumDiscount = 40,
        )

        assertEquals(listOf("AirPods", "Nike", "Target", "MacBook"), watches.map { it.query })
        assertEquals(List(4) { WatchKind.KEYWORD.wire }, watches.map { it.kind })
        assertEquals(List(4) { 40 }, watches.map { it.minimumDiscount })
    }

    @Test
    fun parsePostPurchaseWatchesDropsBlankTermsAndZeroDiscount() {
        val watches = parsePostPurchaseWatches(
            query = "  AirPods, ; \n",
            minimumDiscount = 0,
        )

        assertEquals(1, watches.size)
        assertEquals("AirPods", watches.single().query)
        assertNull(watches.single().minimumDiscount)
    }
}
