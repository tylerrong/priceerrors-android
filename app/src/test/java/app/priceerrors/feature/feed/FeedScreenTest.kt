package app.priceerrors.feature.feed

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedScreenTest {

    private val refreshedAt = Instant.parse("2026-09-12T12:00:00Z")

    @Test
    fun `refresh label handles missing and recent timestamps`() {
        assertEquals("loading…", refreshedAgo(lastRefreshed = null))
        assertEquals(
            "refreshed just now",
            refreshedAgo(refreshedAt, now = refreshedAt.plusSeconds(59)),
        )
    }

    @Test
    fun `refresh label advances through minutes and hours`() {
        assertEquals(
            "refreshed 5m ago",
            refreshedAgo(refreshedAt, now = refreshedAt.plusSeconds(5 * 60)),
        )
        assertEquals(
            "refreshed 3h ago",
            refreshedAgo(refreshedAt, now = refreshedAt.plusSeconds(3 * 60 * 60)),
        )
    }
}
