package app.priceerrors.core.model

import app.priceerrors.core.data.FakeDealData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DealTest {
    @Test
    fun `discount is calculated from integer cents`() {
        val deal = FakeDealData.deals.first { it.category == "Tech" }

        assertEquals(90, deal.discountPercent)
    }

    @Test
    fun `canonical backend id is distinct from source tweet id`() {
        val deal = FakeDealData.deals.first { it.category == "Tech" }

        assertEquals("deal-tech-generated-001", deal.id)
        assertTrue(deal.sourceId.all(Char::isDigit))
        assertNotEquals(deal.sourceId, deal.id)
    }
}
