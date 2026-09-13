package app.priceerrors.core.data

import app.priceerrors.core.network.DealDto
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Persists the last successful feed snapshot between launches, scoped per user.
 *
 * Mirrors iOS `DealService` disk cache: deals can contain paid details, so the
 * file lives in the app cache directory and is deleted on sign-out.
 */
class CachedDealFeedStore(
    cacheRoot: java.io.File,
    private val json: Json,
) {
    private val root = cacheRoot.also { it.mkdirs() }

    fun load(userId: String): CachedFeedSnapshot? {
        val file = cacheFile(userId)
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString<CachedFeedSnapshot>(file.readText())
        }.getOrNull()?.takeIf { it.version == CACHE_VERSION && it.userId == userId }
    }

    fun save(snapshot: CachedFeedSnapshot) {
        val payload = json.encodeToString(snapshot)
        cacheFile(snapshot.userId).writeText(payload)
    }

    fun clear(userId: String) {
        cacheFile(userId).delete()
    }

    private fun cacheFile(userId: String) =
        root.resolve("${userId.replace(Regex("[^A-Za-z0-9_-]"), "_")}.json")

    @Serializable
    data class CachedFeedSnapshot(
        val version: Int = CACHE_VERSION,
        val userId: String,
        val deals: List<DealDto>,
        val total: Int,
        val isPro: Boolean,
        val userVotes: Map<String, String> = emptyMap(),
        @SerialName("sync_cursor") val syncCursor: String,
        @SerialName("last_full_sync") val lastFullSyncEpochSeconds: Long,
        @SerialName("saved_at") val savedAtEpochSeconds: Long = Instant.now().epochSecond,
    )

    companion object {
        const val CACHE_VERSION = 1
        val FULL_REFRESH_INTERVAL_SECONDS: Long = 6 * 60 * 60
        val FEED_RETENTION_SECONDS: Long = 7 * 24 * 60 * 60
    }
}
