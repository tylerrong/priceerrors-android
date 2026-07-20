package app.priceerrors.core.network

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Parses a timestamp as sent by the server.
 *
 * Two formats arrive in practice and both must work:
 *  - `2026-07-19T10:15:30.123456+00:00` — a Postgres `timestamptz` as rendered
 *    by PostgREST, which `Instant.parse` rejects because of the `+00:00` offset.
 *  - `2026-07-19T10:15:30.123Z` — a JavaScript `toISOString()`, used for rows
 *    the server builds itself.
 *
 * Returns null when the value is missing or unparseable, leaving the fallback
 * to the caller.
 */
fun parseServerTimestamp(raw: String?): Instant? {
    if (raw.isNullOrBlank()) return null
    return try {
        // Accepts both a numeric offset and a bare `Z`.
        OffsetDateTime.parse(raw).toInstant()
    } catch (_: DateTimeParseException) {
        try {
            Instant.parse(raw)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
