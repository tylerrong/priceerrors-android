package app.priceerrors.feature.browse

import app.priceerrors.core.data.FakeDealData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseFiltersTest {
    private val techDeal = FakeDealData.deals.first { it.category == "Tech" }

    @Test
    fun `all category matches every deal and category matching ignores case`() {
        assertTrue(dealMatchesBrowseCategory(techDeal, "All"))
        assertTrue(dealMatchesBrowseCategory(techDeal, " tech "))
        assertFalse(dealMatchesBrowseCategory(techDeal, "Food"))
    }

    @Test
    fun `events category accepts singular backend category`() {
        assertTrue(dealMatchesBrowseCategory(techDeal.copy(category = "Event"), "Events"))
    }

    @Test
    fun `search is trimmed case insensitive and supports multiple terms`() {
        val query = "  ${techDeal.brand.uppercase()}   ${techDeal.category.lowercase()} "

        assertTrue(dealMatchesSearch(techDeal, query))
        assertTrue(dealMatchesSearch(techDeal, "   "))
        assertFalse(dealMatchesSearch(techDeal, "definitely-not-a-deal"))
    }

    @Test
    fun `filter applies category and search together without reordering`() {
        val techDeals = FakeDealData.deals.filter { it.category == "Tech" }
        val expected = techDeals.filter { dealMatchesSearch(it, "Tech") }

        assertEquals(
            expected.map { it.id },
            filterBrowseDeals(FakeDealData.deals, "Tech", "Tech").map { it.id },
        )
    }
}
