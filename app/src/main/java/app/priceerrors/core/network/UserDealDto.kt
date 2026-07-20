package app.priceerrors.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape of a `user_deals` row — a community-submitted deal, distinct from
 * the scraped deals served by `/deals`.
 */
@Serializable
data class UserDealDto(
    val id: String,
    @SerialName("user_display_name") val userDisplayName: String = "",
    val title: String = "",
    val description: String = "",
    val url: String? = null,
    val category: String = "Other",
    val brand: String? = null,
    val price: Double? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class UserDealsResponseDto(
    val data: List<UserDealDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20,
)

/** Body for `POST /user-deals`. Sent as a typed object so the JSON is escaped. */
@Serializable
data class UserDealSubmissionDto(
    @SerialName("user_display_name") val userDisplayName: String,
    val title: String,
    val description: String,
    val category: String,
    val brand: String? = null,
    val url: String? = null,
    val price: Double? = null,
)
