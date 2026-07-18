package app.priceerrors.feature.community

import androidx.compose.ui.graphics.Color
import java.net.URI
import java.util.Locale

internal data class CommunityDeal(
    val id: String,
    val userDisplayName: String,
    val title: String,
    val description: String,
    val category: String,
    val brand: String?,
    val url: String?,
    val price: Double,
    val createdAtMillis: Long,
    val isAdmin: Boolean = false,
) {
    val categoryVisual: CommunityCategory
        get() = communityCategories.firstOrNull {
            it.name.equals(category, ignoreCase = true)
        } ?: otherCategory

    val priceLabel: String
        get() = when {
            price == 0.0 -> "FREE"
            price % 1.0 == 0.0 -> "\$${price.toInt()}"
            else -> String.format(Locale.US, "\$%.2f", price)
        }
}

internal data class CommunityCategory(
    val name: String,
    val emoji: String,
    val color: Color,
)

internal enum class CommunityReportReason(val label: String) {
    SpamOrScam("Spam or scam"),
    MisleadingOrExpired("Misleading or expired"),
    UnsafeOrIllegal("Unsafe or illegal"),
    HarassmentOrHate("Harassment or hate"),
    PersonalInformation("Personal information"),
    Other("Something else"),
}

internal data class CommunityPostValidation(
    val isValid: Boolean,
    val message: String? = null,
)

internal val communityCategories = listOf(
    CommunityCategory("Tech", "💻", Color(0xFF2D5BFF)),
    CommunityCategory("Beauty", "💅", Color(0xFFFF7EB6)),
    CommunityCategory("Food", "🌮", Color(0xFF4CAF50)),
    CommunityCategory("Fashion", "👟", Color(0xFFC2185B)),
    CommunityCategory("Travel", "\uD83D\uDEEB", Color(0xFF2D5BFF)),
    CommunityCategory("Events", "🎫", Color(0xFFE91E63)),
    CommunityCategory("Gaming", "🕹️", Color(0xFFFFC93D)),
    CommunityCategory("Other", "📦", Color(0xFF607D8B)),
)

private val otherCategory = communityCategories.last()

internal fun sampleCommunityDeals(now: Long = System.currentTimeMillis()): List<CommunityDeal> = listOf(
    CommunityDeal(
        id = "team-tech-drop",
        userDisplayName = "Price Errors Team",
        title = "MacBook accessories bundle ringing up under \$20",
        description = "The bundle is briefly showing the incorrect checkout price. Add it to your cart and verify the total before placing the order.",
        category = "Tech",
        brand = "Price Errors",
        url = "https://priceerrors.app",
        price = 19.0,
        createdAtMillis = now - 18 * 60 * 1000L,
        isAdmin = true,
    ),
    CommunityDeal(
        id = "community-food-drop",
        userDisplayName = "Maya",
        title = "Family taco kit marked down for tonight only",
        description = "Found this while ordering dinner. The discount appeared automatically after I selected pickup and added the family-size kit.",
        category = "Food",
        brand = "Local pickup",
        url = null,
        price = 6.50,
        createdAtMillis = now - 42 * 60 * 1000L,
    ),
    CommunityDeal(
        id = "community-gaming-drop",
        userDisplayName = "Jordan",
        title = "Wireless controller clearance price still working",
        description = "Several colors are included. Choose store pickup to see the lower price in the cart before the inventory disappears.",
        category = "Gaming",
        brand = "GameStop",
        url = "https://priceerrors.app",
        price = 14.99,
        createdAtMillis = now - 2 * 60 * 60 * 1000L,
    ),
)

internal fun relativeCommunityTime(
    createdAtMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
): String {
    val seconds = ((nowMillis - createdAtMillis) / 1_000L).coerceAtLeast(0L)
    return when {
        seconds < 60L -> "Just now"
        seconds < 3_600L -> "${seconds / 60L}m ago"
        seconds < 21_600L -> "${seconds / 3_600L}h ago"
        seconds < 86_400L -> "Today"
        seconds < 172_800L -> "Yesterday"
        else -> "${seconds / 86_400L}d ago"
    }
}

internal fun normalizeCommunityLink(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    val candidate = if (
        trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
        trimmed
    } else {
        "https://$trimmed"
    }
    return runCatching { URI(candidate) }
        .getOrNull()
        ?.takeIf {
            (it.scheme.equals("http", ignoreCase = true) ||
                it.scheme.equals("https", ignoreCase = true)) &&
                !it.host.isNullOrBlank() &&
                it.userInfo.isNullOrBlank()
        }
        ?.toASCIIString()
}

internal fun canonicalCommunityUserKey(displayName: String): String = displayName
    .trim()
    .lowercase(Locale.ROOT)
    .replace(Regex("\\s+"), " ")

internal fun visibleCommunityDeals(
    deals: List<CommunityDeal>,
    reportedDealIds: Set<String>,
    blockedUserKeys: Set<String>,
): List<CommunityDeal> = deals.filterNot { deal ->
    deal.id in reportedDealIds || canonicalCommunityUserKey(deal.userDisplayName) in blockedUserKeys
}

internal fun validateCommunityPost(
    title: String,
    description: String,
    price: Double?,
    link: String?,
    termsAccepted: Boolean,
): CommunityPostValidation {
    val trimmedTitle = title.trim()
    val trimmedDescription = description.trim()
    return when {
        trimmedTitle.length < 4 -> CommunityPostValidation(false, "Add a title with at least 4 characters.")
        trimmedTitle.length > 120 -> CommunityPostValidation(false, "Keep the title under 120 characters.")
        trimmedDescription.length < 10 -> CommunityPostValidation(false, "Add at least 10 characters describing the deal.")
        trimmedDescription.length > 1_000 -> CommunityPostValidation(false, "Keep the description under 1,000 characters.")
        price == null || !price.isFinite() || price < 0.0 || price > 1_000_000.0 ->
            CommunityPostValidation(false, "Enter a valid price.")
        !link.isNullOrBlank() && normalizeCommunityLink(link) == null ->
            CommunityPostValidation(false, "Enter a valid http or https link.")
        !termsAccepted -> CommunityPostValidation(false, "Agree to the Community Guidelines before posting.")
        else -> CommunityPostValidation(true)
    }
}
