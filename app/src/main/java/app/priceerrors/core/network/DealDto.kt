package app.priceerrors.core.network

import app.priceerrors.core.model.Deal
import app.priceerrors.core.model.DealFeedAccess
import app.priceerrors.core.model.DealFeedMetadata
import app.priceerrors.core.model.DealVote
import java.time.Instant
import kotlin.math.roundToLong
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape of `GET /deals`. Field names match the server's snake_case columns.
 * Everything the server may omit is nullable with a default so an older or
 * newer server revision still decodes.
 */
@Serializable
data class DealDto(
    val id: String,
    @SerialName("tweet_id") val tweetId: String? = null,
    val title: String,
    val description: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("deal_url") val dealUrl: String? = null,
    val price: Double? = null,
    @SerialName("original_price") val originalPrice: Double? = null,
    val category: String? = null,
    val store: String? = null,
    val brand: String? = null,
    val heat: String? = null,
    val tag: String? = null,
    val steps: List<String> = emptyList(),
    @SerialName("working_count") val workingCount: Int = 0,
    @SerialName("not_working_count") val notWorkingCount: Int = 0,
    @SerialName("posted_at") val postedAt: String? = null,
)

@Serializable
data class DealsResponseDto(
    val data: List<DealDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 100,
    val isPro: Boolean = false,
    val freeRemaining: Int? = null,
    val freeDailyLimit: Int = 0,
    val claimedToday: List<String> = emptyList(),
    val userVotes: Map<String, String> = emptyMap(),
)

@Serializable
data class ClaimResponseDto(
    val ok: Boolean? = null,
    val isPro: Boolean? = null,
    val freeRemaining: Int? = null,
    val freeDailyLimit: Int? = null,
    val alreadyClaimed: Boolean? = null,
    val reason: String? = null,
)

@Serializable
data class VoteResponseDto(
    @SerialName("working_count") val workingCount: Int = 0,
    @SerialName("not_working_count") val notWorkingCount: Int = 0,
    @SerialName("user_vote") val userVote: String? = null,
)

/** Server error envelope: `{ "error": "...", "reason": "pro_required" }`. */
@Serializable
data class ErrorResponseDto(
    val error: String? = null,
    val reason: String? = null,
)

/**
 * Dollars to cents. The server sends prices as JSON numbers, so this rounds
 * rather than truncating — `19.99 * 100` is `1998.9999…` in binary floating
 * point and would otherwise land a cent low.
 */
private fun Double?.toCents(): Long = ((this ?: 0.0) * 100).roundToLong().coerceAtLeast(0)

private fun String?.toInstantOrNow(): Instant =
    parseServerTimestamp(this) ?: Instant.now()

/**
 * Maps a wire deal onto the domain model.
 *
 * The server's `original_price` is null for deals with no reference price; the
 * domain model treats "no discount" as original == current, which keeps
 * [Deal.discountPercent] at 0 instead of showing a bogus 100% off.
 */
fun DealDto.toDomain(userVote: DealVote? = null): Deal {
    val priceInCents = price.toCents()
    return Deal(
        id = id,
        sourceId = tweetId ?: id,
        brand = brand?.takeIf(String::isNotBlank) ?: store?.takeIf(String::isNotBlank) ?: "Deal",
        title = title,
        tag = tag?.takeIf(String::isNotBlank) ?: "DEAL",
        heat = heat?.takeIf(String::isNotBlank) ?: "🔥",
        priceInCents = priceInCents,
        originalPriceInCents = originalPrice?.toCents()?.takeIf { it > 0 } ?: priceInCents,
        category = category?.takeIf(String::isNotBlank) ?: "Deals",
        postedAt = postedAt.toInstantOrNow(),
        description = description.orEmpty(),
        store = store?.takeIf(String::isNotBlank) ?: "priceerrors.app",
        workingCount = workingCount,
        notWorkingCount = notWorkingCount,
        userVote = userVote,
        steps = steps,
        imageUrl = imageUrl?.takeIf(String::isNotBlank),
        dealUrl = dealUrl?.takeIf(String::isNotBlank),
    )
}

/**
 * Maps the feed envelope. Deals whose `id` is blank are dropped rather than
 * thrown on: one malformed row upstream should not blank the whole feed.
 */
fun DealsResponseDto.toDomainDeals(): List<Deal> =
    data.mapNotNull { dto ->
        if (dto.id.isBlank()) {
            null
        } else {
            runCatching { dto.toDomain(DealVote.fromWire(userVotes[dto.id])) }.getOrNull()
        }
    }

fun DealsResponseDto.toMetadata(): DealFeedMetadata =
    DealFeedMetadata(
        total = total,
        page = page,
        pageSize = limit,
        access = DealFeedAccess(
            isPro = isPro,
            // The server sends -1 for "unlimited"; the domain model uses null.
            freeRemaining = freeRemaining?.takeIf { it >= 0 },
            freeDailyLimit = freeDailyLimit,
            claimedDealIds = claimedToday.toSet(),
        ),
    )
