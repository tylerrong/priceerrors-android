package app.priceerrors.core.model

import java.time.Instant

enum class DealVote(val wireValue: String) {
    WORKING("working"),
    NOT_WORKING("not_working"),
    ;

    companion object {
        fun fromWire(value: String?): DealVote? =
            entries.firstOrNull { vote -> vote.wireValue == value }
    }
}

/**
 * UI-agnostic deal model.
 *
 * [id] is always the canonical backend `deals.id`. [sourceId] is the source
 * tweet ID and must never be used for navigation, persistence, votes, or saves.
 */
data class Deal(
    val id: String,
    val sourceId: String,
    val brand: String,
    val title: String,
    val tag: String,
    val heat: String,
    val priceInCents: Long,
    val originalPriceInCents: Long,
    val currencyCode: String = "USD",
    val category: String,
    val postedAt: Instant,
    val description: String,
    val store: String,
    val workingCount: Int,
    val notWorkingCount: Int,
    val userVote: DealVote?,
    val steps: List<String>,
    val imageUrl: String?,
    val dealUrl: String?,
) {
    init {
        require(id.isNotBlank()) { "A deal must have a canonical backend ID." }
        require(priceInCents >= 0) { "Deal price cannot be negative." }
        require(originalPriceInCents >= 0) { "Original price cannot be negative." }
        require(currencyCode.length == 3) { "Currency must use a three-letter ISO code." }
    }

    val discountPercent: Int
        get() {
            if (originalPriceInCents == 0L || priceInCents >= originalPriceInCents) return 0
            return (((originalPriceInCents - priceInCents) * 100) / originalPriceInCents).toInt()
        }
}

data class DealFeedAccess(
    val isPro: Boolean,
    val freeRemaining: Int?,
    val freeDailyLimit: Int,
    val claimedDealIds: Set<String>,
)

data class DealFeedMetadata(
    val total: Int,
    val page: Int,
    val pageSize: Int,
    val access: DealFeedAccess,
)
