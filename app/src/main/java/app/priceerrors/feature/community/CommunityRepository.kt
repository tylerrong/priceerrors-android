package app.priceerrors.feature.community

import app.priceerrors.core.network.PriceErrorsApi
import app.priceerrors.core.network.UserDealDto
import app.priceerrors.core.network.UserDealSubmissionDto
import app.priceerrors.core.network.parseServerTimestamp

/**
 * Community deals, backed by `/user-deals`.
 *
 * Kept in the feature package because [CommunityDeal] is internal to it — the
 * network layer speaks DTOs and the translation happens here.
 */
internal class CommunityRepository(
    private val api: PriceErrorsApi,
) {
    suspend fun loadFeed(): Result<List<CommunityDeal>> =
        api.fetchUserDeals().map { response ->
            response.data.mapNotNull { dto ->
                runCatching { dto.toCommunityDeal() }.getOrNull()
            }
        }

    suspend fun post(deal: CommunityDeal): Result<CommunityDeal> =
        api.submitUserDeal(
            UserDealSubmissionDto(
                userDisplayName = deal.userDisplayName,
                title = deal.title,
                description = deal.description,
                category = deal.category,
                brand = deal.brand,
                url = deal.url,
                price = deal.price,
            ),
        ).map { created -> created.toCommunityDeal() }

    suspend fun delete(id: String): Result<Unit> = api.deleteUserDeal(id)
}

private fun UserDealDto.toCommunityDeal(): CommunityDeal =
    CommunityDeal(
        id = id,
        userDisplayName = userDisplayName.ifBlank { "PriceErrors user" },
        title = title,
        description = description,
        category = category,
        brand = brand,
        url = url,
        // The column is nullable; the UI renders 0 as "FREE", which is the
        // correct reading for a community freebie post.
        price = price ?: 0.0,
        createdAtMillis = createdAt.toEpochMillisOrNow(),
        isAdmin = isAdmin,
    )

private fun String?.toEpochMillisOrNow(): Long =
    parseServerTimestamp(this)?.toEpochMilli() ?: System.currentTimeMillis()
