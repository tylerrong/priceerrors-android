package app.priceerrors.core.network

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimestampsTest {

    @Test
    fun `parses a postgres timestamptz with a numeric offset`() {
        // This is the shape PostgREST actually returns for `timestamptz`, and
        // the one Instant.parse rejects.
        assertEquals(
            Instant.parse("2026-07-19T10:15:30Z"),
            parseServerTimestamp("2026-07-19T10:15:30+00:00"),
        )
    }

    @Test
    fun `parses a postgres timestamptz with microseconds`() {
        assertEquals(
            Instant.parse("2026-07-19T10:15:30.123456Z"),
            parseServerTimestamp("2026-07-19T10:15:30.123456+00:00"),
        )
    }

    @Test
    fun `parses a non-utc offset`() {
        assertEquals(
            Instant.parse("2026-07-19T15:15:30Z"),
            parseServerTimestamp("2026-07-19T10:15:30-05:00"),
        )
    }

    @Test
    fun `parses a javascript toISOString value`() {
        assertEquals(
            Instant.parse("2026-07-19T10:15:30.123Z"),
            parseServerTimestamp("2026-07-19T10:15:30.123Z"),
        )
    }

    @Test
    fun `returns null for missing or unparseable values`() {
        assertNull(parseServerTimestamp(null))
        assertNull(parseServerTimestamp(""))
        assertNull(parseServerTimestamp("   "))
        assertNull(parseServerTimestamp("not-a-date"))
        // A date with no time component is not a valid instant.
        assertNull(parseServerTimestamp("2026-07-19"))
    }
}
