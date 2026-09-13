package app.priceerrors.feature.alerts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertModelsTest {
    @Test
    fun encodeAndDecodeRoundTrip() {
        val watches = listOf(
            DealWatch(
                id = "w1",
                kind = WatchKind.KEYWORD.wire,
                query = "AirPods",
                maxPrice = 100.0,
                minimumDiscount = 40,
            ),
            DealWatch(
                id = "w2",
                kind = WatchKind.BRAND.wire,
                query = "Nike",
            ),
        )
        val decoded = decodeDealWatches(encodeDealWatches(watches))
        assertEquals(2, decoded.size)
        assertEquals("AirPods", decoded[0].query)
        assertEquals(100.0, decoded[0].maxPrice)
        assertEquals(40, decoded[0].minimumDiscount)
        assertEquals("under $100 · 40%+ off", decoded[0].subtitle)
        assertEquals("Any matching deal", decoded[1].subtitle)
    }

    @Test
    fun invalidJsonYieldsEmptyList() {
        assertTrue(decodeDealWatches("not-json").isEmpty())
        assertTrue(decodeDealWatches("").isEmpty())
    }

    @Test
    fun keywordWatchesSplitCommaSemicolonAndNewlineInput() {
        val watches = keywordWatches(
            input = " AirPods, Nike;\nTarget\n\n",
            maxPrice = 100.0,
            minimumDiscount = 50,
        )

        assertEquals(listOf("AirPods", "Nike", "Target"), watches.map(DealWatch::query))
        assertTrue(watches.all { it.kind == WatchKind.KEYWORD.wire })
        assertTrue(watches.all { it.maxPrice == 100.0 && it.minimumDiscount == 50 })
    }

    @Test
    fun categoriesMatchTheIosSetAndOrder() {
        assertEquals(
            listOf("Tech", "Beauty", "Food", "Fashion", "Travel", "Events", "Gaming", "Other"),
            alertCategories.map(AlertCategory::name),
        )
    }
}
