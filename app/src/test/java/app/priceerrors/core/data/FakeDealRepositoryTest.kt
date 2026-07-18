package app.priceerrors.core.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FakeDealRepositoryTest {
    @Test
    fun `feed preserves deterministic fixture order`() = runTest {
        val repository = FakeDealRepository()

        val deals = repository.observeFeed().first()

        assertEquals(
            listOf(
                "deal-food-papa-johns-stuffed-crust-001",
                "deal-food-applebees-two-for-25-001",
                "deal-food-dunkin-parke-tumbler-001",
                "deal-food-carls-jr-double-take-001",
            ),
            deals.take(4).map { it.id },
        )
        assertEquals(100, deals.size)
        assertEquals(30, deals.count { it.category == "Food" })
        assertEquals(24, deals.count { it.category == "Tech" })
        assertEquals(8, deals.count { it.category == "Beauty" })
        assertEquals(7, deals.count { it.category == "Fashion" })
        assertEquals(3, deals.count { it.category == "Gaming" })
        assertEquals(0, deals.count { it.category == "Travel" || it.category == "Events" })
    }

    @Test
    fun `detail lookup accepts canonical id and rejects source id`() = runTest {
        val repository = FakeDealRepository()
        val fixture = FakeDealData.deals.first()

        assertEquals(fixture, repository.observeDeal(fixture.id).first())
        assertNull(repository.observeDeal(fixture.sourceId).first())
    }

    @Test
    fun `replacing feed updates canonical detail lookup`() = runTest {
        val original = FakeDealData.deals.first()
        val updated = original.copy(title = "Updated title")
        val repository = FakeDealRepository(initialDeals = listOf(original))

        repository.replaceDeals(listOf(updated))

        assertEquals("Updated title", repository.observeDeal(original.id).first()?.title)
    }
}
