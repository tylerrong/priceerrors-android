package app.priceerrors.core.data

import app.priceerrors.core.model.Deal
import app.priceerrors.core.model.DealFeedMetadata
import app.priceerrors.core.model.DealVote
import app.priceerrors.core.network.PriceErrorsApi
import app.priceerrors.core.network.toDomainDeals
import app.priceerrors.core.network.toMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The live [DealRepository], backed by `GET /deals`.
 *
 * Holds the last successful response in memory and emits it to observers. A
 * failed refresh leaves the previous deals in place and surfaces the error
 * through the returned [Result], so a dropped connection never blanks a feed
 * the user is already reading.
 */
class NetworkDealRepository(
    private val api: PriceErrorsApi,
) : DealRepository {

    private val deals = MutableStateFlow<List<Deal>>(emptyList())
    private val metadata = MutableStateFlow<DealFeedMetadata?>(null)

    /** Prevents overlapping refreshes from interleaving their writes. */
    private val refreshMutex = Mutex()

    override fun observeFeed(): Flow<List<Deal>> = deals.asStateFlow()

    override fun observeDeal(id: String): Flow<Deal?> =
        deals
            .map { current -> current.firstOrNull { deal -> deal.id == id } }
            .distinctUntilChanged()

    override fun observeMetadata(): Flow<DealFeedMetadata?> = metadata.asStateFlow()

    override suspend fun refresh(query: DealFeedQuery): Result<Unit> = refreshMutex.withLock {
        api.fetchDeals(
            page = query.page,
            limit = query.pageSize,
            category = query.category,
        ).map { response ->
            deals.value = response.toDomainDeals()
            metadata.value = response.toMetadata()
        }
    }

    /**
     * Records the user's vote and folds the server's authoritative counts back
     * into the cached deal, so the tally the user sees matches the tally the
     * server stored rather than a local guess.
     */
    suspend fun vote(dealId: String, vote: DealVote): Result<Unit> =
        api.voteDeal(dealId, vote).map { response ->
            deals.value = deals.value.map { deal ->
                if (deal.id != dealId) {
                    deal
                } else {
                    deal.copy(
                        workingCount = response.workingCount,
                        notWorkingCount = response.notWorkingCount,
                        userVote = DealVote.fromWire(response.userVote),
                    )
                }
            }
        }

    /**
     * Registers that the user opened a deal, which is what decrements the
     * free-tier daily allowance. Returns [app.priceerrors.core.network.ApiError.LimitReached]
     * when the allowance is spent so the caller can raise the paywall.
     */
    suspend fun claim(dealId: String): Result<Unit> =
        api.claimDeal(dealId).map { response ->
            val currentMetadata = metadata.value ?: return@map
            metadata.value = currentMetadata.copy(
                access = currentMetadata.access.copy(
                    isPro = response.isPro ?: currentMetadata.access.isPro,
                    freeRemaining = response.freeRemaining?.takeIf { it >= 0 },
                    freeDailyLimit = response.freeDailyLimit ?: currentMetadata.access.freeDailyLimit,
                    claimedDealIds = currentMetadata.access.claimedDealIds + dealId,
                ),
            )
        }

    /** Drops cached deals on sign-out so the next user never sees them. */
    fun clear() {
        deals.value = emptyList()
        metadata.value = null
    }
}
