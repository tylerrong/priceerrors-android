package app.priceerrors.core.data

import app.priceerrors.core.model.Deal
import app.priceerrors.core.model.DealFeedAccess
import app.priceerrors.core.model.DealFeedMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class FakeDealRepository(
    initialDeals: List<Deal> = FakeDealData.deals,
    private val refreshResult: Result<Unit> = Result.success(Unit),
) : DealRepository {
    private val deals = MutableStateFlow(initialDeals)
    private val metadata = MutableStateFlow<DealFeedMetadata?>(
        DealFeedMetadata(
            total = initialDeals.size,
            page = 1,
            pageSize = 100,
            access = DealFeedAccess(
                isPro = true,
                freeRemaining = null,
                freeDailyLimit = 0,
                claimedDealIds = emptySet(),
            ),
        ),
    )

    override fun observeFeed(): Flow<List<Deal>> = deals

    override fun observeDeal(id: String): Flow<Deal?> =
        deals
            .map { currentDeals -> currentDeals.firstOrNull { deal -> deal.id == id } }
            .distinctUntilChanged()

    override fun observeMetadata(): Flow<DealFeedMetadata?> = metadata

    override suspend fun refresh(query: DealFeedQuery, forceFull: Boolean): Result<Unit> = refreshResult

    override suspend fun fetchDeal(id: String): Result<Deal> =
        deals.value.firstOrNull { it.id == id }?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Deal not found"))

    override fun upsertDeal(deal: Deal) {
        if (deals.value.none { it.id == deal.id }) {
            deals.value = listOf(deal) + deals.value
        }
    }

    fun replaceDeals(updatedDeals: List<Deal>) {
        deals.value = updatedDeals
        metadata.value = metadata.value?.copy(total = updatedDeals.size)
    }
}
