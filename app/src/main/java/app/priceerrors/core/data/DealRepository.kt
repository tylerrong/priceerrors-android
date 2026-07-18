package app.priceerrors.core.data

import app.priceerrors.core.model.Deal
import app.priceerrors.core.model.DealFeedMetadata
import kotlinx.coroutines.flow.Flow

data class DealFeedQuery(
    val page: Int = 1,
    val pageSize: Int = 100,
    val category: String? = null,
)

interface DealRepository {
    fun observeFeed(): Flow<List<Deal>>

    fun observeDeal(id: String): Flow<Deal?>

    fun observeMetadata(): Flow<DealFeedMetadata?>

    suspend fun refresh(query: DealFeedQuery = DealFeedQuery()): Result<Unit>
}
