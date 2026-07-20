package app.priceerrors.core.network

import app.priceerrors.core.model.DealVote
import java.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DealDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `maps a full server deal onto the domain model`() {
        val dto = DealDto(
            id = "deal_1",
            tweetId = "tweet_1",
            title = "AirPods Pro 2",
            description = "Price error at Target.",
            imageUrl = "https://example.com/a.png",
            dealUrl = "https://example.com/deal",
            price = 24.99,
            originalPrice = 249.0,
            category = "Tech",
            store = "target.com",
            brand = "Target",
            heat = "🔥🔥🔥",
            tag = "PRICE ERROR",
            steps = listOf("Add to cart", "Checkout"),
            workingCount = 12,
            notWorkingCount = 3,
            postedAt = "2026-07-19T10:15:30Z",
        )

        val deal = dto.toDomain(DealVote.WORKING)

        assertEquals("deal_1", deal.id)
        assertEquals("tweet_1", deal.sourceId)
        assertEquals(2499L, deal.priceInCents)
        assertEquals(24900L, deal.originalPriceInCents)
        // 89.96% off, floored by the domain model.
        assertEquals(89, deal.discountPercent)
        assertEquals(DealVote.WORKING, deal.userVote)
        assertEquals(listOf("Add to cart", "Checkout"), deal.steps)
        assertEquals(Instant.parse("2026-07-19T10:15:30Z"), deal.postedAt)
    }

    @Test
    fun `rounds prices instead of truncating binary floating point`() {
        // 19.99 * 100 is 1998.9999... in IEEE-754; truncation would bill a cent low.
        val deal = DealDto(id = "d", title = "t", price = 19.99).toDomain()

        assertEquals(1999L, deal.priceInCents)
    }

    @Test
    fun `treats a missing original price as no discount`() {
        val deal = DealDto(id = "d", title = "t", price = 12.0, originalPrice = null).toDomain()

        assertEquals(1200L, deal.priceInCents)
        assertEquals(1200L, deal.originalPriceInCents)
        assertEquals(0, deal.discountPercent)
    }

    @Test
    fun `falls back to defaults when optional fields are absent`() {
        val deal = DealDto(id = "d", title = "Freebie").toDomain()

        assertEquals(0L, deal.priceInCents)
        assertEquals("Deals", deal.category)
        assertEquals("🔥", deal.heat)
        assertEquals("DEAL", deal.tag)
        assertEquals("", deal.description)
        assertNull(deal.imageUrl)
        assertNull(deal.userVote)
    }

    @Test
    fun `decodes a feed payload and attaches the callers votes`() {
        val payload = """
            {
              "data": [
                {"id": "a", "title": "A", "price": 1.0, "posted_at": "2026-07-19T10:00:00Z"},
                {"id": "b", "title": "B", "price": 2.0, "posted_at": "2026-07-19T10:00:00Z"}
              ],
              "total": 2, "page": 1, "limit": 100,
              "isPro": true, "freeRemaining": -1, "freeDailyLimit": 0,
              "claimedToday": ["a"],
              "userVotes": {"a": "working", "b": "not_working"}
            }
        """.trimIndent()

        val response = json.decodeFromString<DealsResponseDto>(payload)
        val deals = response.toDomainDeals()
        val metadata = response.toMetadata()

        assertEquals(2, deals.size)
        assertEquals(DealVote.WORKING, deals[0].userVote)
        assertEquals(DealVote.NOT_WORKING, deals[1].userVote)
        assertTrue(metadata.access.isPro)
        // -1 means unlimited on the wire; the domain model uses null.
        assertNull(metadata.access.freeRemaining)
        assertEquals(setOf("a"), metadata.access.claimedDealIds)
    }

    @Test
    fun `keeps a positive free remaining count`() {
        val payload = """
            {"data": [], "total": 0, "page": 1, "limit": 20,
             "isPro": false, "freeRemaining": 2, "freeDailyLimit": 3,
             "claimedToday": [], "userVotes": {}}
        """.trimIndent()

        val metadata = json.decodeFromString<DealsResponseDto>(payload).toMetadata()

        assertEquals(2, metadata.access.freeRemaining)
        assertEquals(3, metadata.access.freeDailyLimit)
    }

    @Test
    fun `drops malformed rows rather than failing the whole feed`() {
        val payload = """
            {"data": [
                {"id": "", "title": "blank id"},
                {"id": "ok", "title": "Good", "price": 5.0}
             ],
             "total": 2, "page": 1, "limit": 20, "isPro": true,
             "claimedToday": [], "userVotes": {}}
        """.trimIndent()

        val deals = json.decodeFromString<DealsResponseDto>(payload).toDomainDeals()

        assertEquals(1, deals.size)
        assertEquals("ok", deals[0].id)
    }

    @Test
    fun `ignores server fields the client does not know about`() {
        val payload = """
            {"data": [{"id": "a", "title": "A", "brand_new_field": 42}],
             "total": 1, "page": 1, "limit": 20, "isPro": true,
             "some_future_key": {"nested": true},
             "claimedToday": [], "userVotes": {}}
        """.trimIndent()

        val deals = json.decodeFromString<DealsResponseDto>(payload).toDomainDeals()

        assertEquals(1, deals.size)
    }

    @Test
    fun `parses a posted_at in the format postgres actually returns`() {
        val deal = DealDto(
            id = "d",
            title = "t",
            postedAt = "2026-07-19T10:15:30.123456+00:00",
        ).toDomain()

        assertEquals(Instant.parse("2026-07-19T10:15:30.123456Z"), deal.postedAt)
    }

    @Test
    fun `falls back to now when posted_at is unparseable`() {
        val deal = DealDto(id = "d", title = "t", postedAt = "not-a-date").toDomain()

        assertTrue(deal.postedAt.isAfter(Instant.now().minusSeconds(60)))
    }
}
