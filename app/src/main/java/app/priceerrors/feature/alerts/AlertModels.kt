package app.priceerrors.feature.alerts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

enum class WatchKind(val wire: String, val label: String) {
    PRODUCT("product", "Product"),
    KEYWORD("keyword", "Keyword"),
    BRAND("brand", "Brand"),
    STORE("store", "Store"),
}

@Serializable
data class DealWatch(
    val id: String = UUID.randomUUID().toString(),
    val kind: String = WatchKind.KEYWORD.wire,
    val query: String,
    @SerialName("max_price") val maxPrice: Double? = null,
    @SerialName("minimum_discount") val minimumDiscount: Int? = null,
) {
    val subtitle: String
        get() {
            val parts = buildList {
                maxPrice?.let { add("under $${it.toInt()}") }
                minimumDiscount?.let { add("$it%+ off") }
            }
            return if (parts.isEmpty()) "Any matching deal" else parts.joinToString(" · ")
        }
}

data class RecentDealAlert(
    val id: String,
    val dealId: String? = null,
    val title: String,
    val body: String,
    val dateMillis: Long,
)

data class AlertCategory(
    val name: String,
    val icon: String,
)

val alertCategories = listOf(
    AlertCategory("Tech", "💻"),
    AlertCategory("Beauty", "💅"),
    AlertCategory("Food", "🌮"),
    AlertCategory("Fashion", "👟"),
    AlertCategory("Travel", "🛫"),
    AlertCategory("Events", "🎫"),
    AlertCategory("Gaming", "🕹️"),
    AlertCategory("Other", "📦"),
)

fun keywordWatches(
    input: String,
    maxPrice: Double? = null,
    minimumDiscount: Int? = null,
): List<DealWatch> =
    input
        .split(',', ';', '\n')
        .map { it.trim() }
        .filter(String::isNotEmpty)
        .map { query ->
            DealWatch(
                kind = WatchKind.KEYWORD.wire,
                query = query,
                maxPrice = maxPrice,
                minimumDiscount = minimumDiscount,
            )
        }

private val alertsJson = Json { ignoreUnknownKeys = true }

fun encodeDealWatches(watches: List<DealWatch>): String =
    alertsJson.encodeToString(watches)

fun decodeDealWatches(json: String): List<DealWatch> =
    runCatching { alertsJson.decodeFromString<List<DealWatch>>(json) }
        .getOrDefault(emptyList())
