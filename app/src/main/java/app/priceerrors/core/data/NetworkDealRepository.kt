package app.priceerrors.core.data

import app.priceerrors.core.model.Deal
import app.priceerrors.core.model.DealFeedMetadata
import app.priceerrors.core.model.DealVote
import app.priceerrors.core.network.ApiError
import app.priceerrors.core.network.DealDto
import app.priceerrors.core.network.DealsFetchResult
import app.priceerrors.core.network.DealsResponseDto
import app.priceerrors.core.network.PriceErrorsApi
import app.priceerrors.core.network.parseServerTimestamp
import app.priceerrors.core.network.toDomain
import app.priceerrors.core.network.toDomainDeals
import app.priceerrors.core.network.toMetadata
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * The live [DealRepository], backed by `GET /deals`.
 *
 * Caches successful responses on disk and reconciles incremental updates using
 * the server's sync cursor, mirroring iOS `DealService`.
 */
class NetworkDealRepository(
    private val api: PriceErrorsApi,
    cacheRoot: java.io.File,
    private val userIdProvider: () -> String?,
    json: Json = Json { ignoreUnknownKeys = true },
) : DealRepository {

    private val cacheStore = CachedDealFeedStore(cacheRoot, json)
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

    override suspend fun refresh(query: DealFeedQuery, forceFull: Boolean): Result<Unit> =
        refreshMutex.withLock {
            val userId = userIdProvider()
            if (userId.isNullOrBlank()) {
                return refreshWithoutCache(query)
            }

            val cached = cacheStore.load(userId)
            val expectedPro = cached?.isPro ?: metadata.value?.access?.isPro ?: false
            if (cached != null && cached.isPro == expectedPro) {
                publishFromCache(cached)
            }

            val needsFullRefresh = forceFull || cached == null ||
                Instant.now().epochSecond - cached.lastFullSyncEpochSeconds >=
                CachedDealFeedStore.FULL_REFRESH_INTERVAL_SECONDS

            return if (needsFullRefresh) {
                refreshFull(userId, query, lastFullSync = Instant.now())
            } else {
                refreshIncremental(userId, requireNotNull(cached), query)
            }
        }

    private suspend fun refreshWithoutCache(query: DealFeedQuery): Result<Unit> =
        api.fetchAllDeals(
            page = query.page,
            limit = query.pageSize,
            category = query.category,
            onFirstPage = { firstPage -> publishNetworkPage(firstPage, query) },
        ).map { result ->
            publishNetworkResult(result, query)
        }

    private suspend fun refreshFull(
        userId: String,
        query: DealFeedQuery,
        lastFullSync: Instant,
    ): Result<Unit> =
        api.fetchAllDeals(
            page = query.page,
            limit = query.pageSize,
            category = query.category,
            onFirstPage = { firstPage -> publishNetworkPage(firstPage, query) },
        ).map { result ->
            val snapshot = makeCacheSnapshot(
                userId = userId,
                result = result,
                lastFullSync = lastFullSync,
            )
            cacheStore.save(snapshot)
            publishNetworkResult(result, query)
        }

    private suspend fun refreshIncremental(
        userId: String,
        cached: CachedDealFeedStore.CachedFeedSnapshot,
        query: DealFeedQuery,
    ): Result<Unit> {
        val freshPage = api.fetchDealSnapshot().getOrElse { return Result.failure(it) }
        if (freshPage.isPro != cached.isPro) {
            return refreshFull(userId, query, lastFullSync = Instant.now())
        }

        val activeIds = freshPage.activeDealIds?.toSet()
        val activeCachedDeals = activeIds?.let { ids ->
            cached.deals.filter { it.id in ids }
        } ?: cached.deals
        val pageReconciledDeals = mergeDeals(freshPage.deals, activeCachedDeals)
        val activeCachedVotes = activeIds?.let { ids ->
            cached.userVotes.filterKeys { it in ids }
        } ?: cached.userVotes
        val pageReconciledVotes = activeCachedVotes + freshPage.userVotes
        val pageSnapshot = makeCacheSnapshot(
            userId = userId,
            deals = pageReconciledDeals,
            total = freshPage.total,
            isPro = freshPage.isPro,
            votes = pageReconciledVotes,
            syncCursor = cached.syncCursor,
            lastFullSync = Instant.ofEpochSecond(cached.lastFullSyncEpochSeconds),
        )
        publishFromCache(pageSnapshot)

        val delta = api.fetchAllDeals(since = cached.syncCursor).getOrElse { return Result.failure(it) }
        if (delta.isPro != cached.isPro) {
            return refreshFull(userId, query, lastFullSync = Instant.now())
        }

        val mergedDeals = mergeDeals(
            freshPage.deals + delta.deals,
            activeCachedDeals,
        )
        val mergedVotes = pageReconciledVotes + delta.userVotes
        val snapshot = makeCacheSnapshot(
            userId = userId,
            deals = mergedDeals,
            total = delta.total,
            isPro = delta.isPro,
            votes = mergedVotes,
            syncCursor = delta.syncCursor ?: cached.syncCursor,
            lastFullSync = Instant.ofEpochSecond(cached.lastFullSyncEpochSeconds),
        )
        cacheStore.save(snapshot)
        publishNetworkResult(delta.copy(deals = mergedDeals, userVotes = mergedVotes), query)
        return Result.success(Unit)
    }

    override suspend fun fetchDeal(id: String): Result<Deal> =
        api.fetchDeal(id).map { dto -> dto.toDomain() }

    override fun upsertDeal(deal: Deal) {
        if (deals.value.any { it.id == deal.id }) return
        deals.value = listOf(deal) + deals.value
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
            userIdProvider()?.let { userId ->
                val cached = cacheStore.load(userId) ?: return@let
                val votes = cached.userVotes + (dealId to vote.wireValue)
                cacheStore.save(cached.copy(userVotes = votes))
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
        userIdProvider()?.let(cacheStore::clear)
        deals.value = emptyList()
        metadata.value = null
    }

    private fun publishFromCache(cached: CachedDealFeedStore.CachedFeedSnapshot) {
        val response = cached.toResponseDto()
        deals.value = response.toDomainDeals()
        metadata.value = response.toMetadata()
    }

    private fun publishNetworkPage(result: DealsFetchResult, query: DealFeedQuery) {
        val response = result.toResponseDto(query.pageSize)
        deals.value = response.toDomainDeals()
        metadata.value = response.toMetadata()
    }

    private fun publishNetworkResult(result: DealsFetchResult, query: DealFeedQuery) {
        val response = result.toResponseDto(query.pageSize)
        deals.value = response.toDomainDeals()
        metadata.value = response.toMetadata()
    }

    private fun makeCacheSnapshot(
        userId: String,
        result: DealsFetchResult,
        lastFullSync: Instant,
    ): CachedDealFeedStore.CachedFeedSnapshot =
        makeCacheSnapshot(
            userId = userId,
            deals = result.deals,
            total = result.total,
            isPro = result.isPro,
            votes = result.userVotes,
            syncCursor = result.syncCursor ?: Instant.now().toString(),
            lastFullSync = lastFullSync,
        )

    private fun makeCacheSnapshot(
        userId: String,
        deals: List<DealDto>,
        total: Int,
        isPro: Boolean,
        votes: Map<String, String>,
        syncCursor: String,
        lastFullSync: Instant,
    ): CachedDealFeedStore.CachedFeedSnapshot =
        CachedDealFeedStore.CachedFeedSnapshot(
            userId = userId,
            deals = deals,
            total = total,
            isPro = isPro,
            userVotes = votes,
            syncCursor = syncCursor,
            lastFullSyncEpochSeconds = lastFullSync.epochSecond,
        )

    private fun mergeDeals(
        newDeals: List<DealDto>,
        cachedDeals: List<DealDto>,
    ): List<DealDto> {
        val cutoff = Instant.now().minusSeconds(CachedDealFeedStore.FEED_RETENTION_SECONDS)
        val seen = LinkedHashSet<String>()
        return (newDeals + cachedDeals)
            .filter { deal ->
                if (!seen.add(deal.id)) return@filter false
                val postedAt = parseServerTimestamp(deal.postedAt) ?: return@filter true
                !postedAt.isBefore(cutoff)
            }
            .sortedByDescending { deal ->
                parseServerTimestamp(deal.postedAt) ?: Instant.EPOCH
            }
    }

    private fun CachedDealFeedStore.CachedFeedSnapshot.toResponseDto() =
        DealsFetchResult(
            deals = deals,
            total = total,
            syncCursor = syncCursor,
            activeDealIds = null,
            isPro = isPro,
            freeRemaining = null,
            freeDailyLimit = 0,
            claimedToday = emptyList(),
            userVotes = userVotes,
            pageSize = 50,
        ).toResponseDto(50)

    private fun DealsFetchResult.toResponseDto(pageSize: Int) =
        DealsResponseDto(
            data = deals,
            total = total,
            feedTotal = total,
            page = 1,
            limit = pageSize,
            syncCursor = syncCursor,
            activeDealIds = activeDealIds,
            isPro = isPro,
            freeRemaining = freeRemaining,
            freeDailyLimit = freeDailyLimit,
            claimedToday = claimedToday,
            userVotes = userVotes,
        )
}
