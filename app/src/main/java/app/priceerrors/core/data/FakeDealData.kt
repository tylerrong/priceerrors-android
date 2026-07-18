package app.priceerrors.core.data

import app.priceerrors.core.model.Deal
import app.priceerrors.core.model.DealVote
import java.time.Instant

object FakeDealData {
    private val generatedAt = Instant.now()

    private fun papaJohnsDeal() = Deal(
        id = "deal-food-papa-johns-stuffed-crust-001",
        sourceId = "1899200000000000001",
        brand = "Papa John’s",
        title = "Papa John’s Epic Pepperoni-Stuffed Crust Pizza is Back",
        tag = "FOOD DEAL",
        heat = "🔥🔥🔥",
        priceInCents = 1_499,
        originalPriceInCents = 1_499,
        category = "Food",
        postedAt = generatedAt.minusSeconds(15),
        description = "Papa John’s Epic Pepperoni-Stuffed Crust Pizza is back for a limited time at participating locations.",
        store = "Papa John’s",
        workingCount = 214,
        notWorkingCount = 4,
        userVote = null,
        steps = listOf(
            "Open the Papa John’s app or website.",
            "Choose the Epic Pepperoni-Stuffed Crust Pizza.",
            "Confirm the price before placing your order.",
        ),
        imageUrl = null,
        dealUrl = "https://priceerrors.app/deals/deal-food-papa-johns-stuffed-crust-001",
    )

    private fun applebeesDeal() = Deal(
        id = "deal-food-applebees-two-for-25-001",
        sourceId = "1899200000000000002",
        brand = "Applebee’s",
        title = "Applebee’s 2 for $25 Menu with New Bacon Cheeseburger Wonton Tacos",
        tag = "FOOD DEAL",
        heat = "🔥🔥🔥",
        priceInCents = 2_500,
        originalPriceInCents = 2_500,
        category = "Food",
        postedAt = generatedAt.minusSeconds(35),
        description = "Applebee’s adds new Bacon Cheeseburger Wonton Tacos to their 2 for $25 deal.",
        store = "Applebee’s",
        workingCount = 0,
        notWorkingCount = 0,
        userVote = null,
        steps = listOf(
            "Visit your local Applebee’s.",
            "Order the 2 for $25 Menu deal.",
            "Select the new Bacon Cheeseburger Wonton Tacos as one of your two signature items.",
        ),
        imageUrl = null,
        dealUrl = "https://priceerrors.app/deals/deal-food-applebees-two-for-25-001",
    )

    private fun dunkinDeal() = Deal(
        id = "deal-food-dunkin-parke-tumbler-001",
        sourceId = "1899200000000000003",
        brand = "Dunkin’",
        title = "FREE Dunkin’ x Parke Tumbler with Purchase",
        tag = "FREEBIE",
        heat = "🔥🔥",
        priceInCents = 0,
        originalPriceInCents = 0,
        category = "Food",
        postedAt = generatedAt.minusSeconds(95),
        description = "Select locations are offering a limited Dunkin’ x Parke tumbler with a qualifying purchase while supplies last.",
        store = "Dunkin’",
        workingCount = 128,
        notWorkingCount = 12,
        userVote = null,
        steps = listOf(
            "Open the Dunkin’ app and select a participating store.",
            "Add the qualifying purchase to your order.",
            "Confirm the free tumbler appears before checkout.",
        ),
        imageUrl = null,
        dealUrl = "https://priceerrors.app/deals/deal-food-dunkin-parke-tumbler-001",
    )

    private fun carlsJrDeal() = Deal(
        id = "deal-food-carls-jr-double-take-001",
        sourceId = "1899200000000000004",
        brand = "Carl’s Jr.",
        title = "85¢ Carl’s Jr. Double Take Deal with Any Large Drink",
        tag = "FOOD DEAL",
        heat = "🔥🔥",
        priceInCents = 85,
        originalPriceInCents = 410,
        category = "Food",
        postedAt = generatedAt.minusSeconds(210),
        description = "Add an eligible large drink to unlock the limited-time 85¢ Double Take offer at participating Carl’s Jr. locations.",
        store = "Carl’s Jr.",
        workingCount = 173,
        notWorkingCount = 9,
        userVote = DealVote.WORKING,
        steps = listOf(
            "Open the Carl’s Jr. app and choose a participating location.",
            "Add the Double Take item and an eligible large drink.",
            "Check that the 85¢ offer is applied before paying.",
        ),
        imageUrl = null,
        dealUrl = "https://priceerrors.app/deals/deal-food-carls-jr-double-take-001",
    )

    private fun generatedDeals(
        category: String,
        count: Int,
        sourceStart: Int,
        minuteStart: Int,
    ): List<Deal> {
        val fixtures = fixturesByCategory.getValue(category)
        return List(count) { zeroBasedIndex ->
            val ordinal = zeroBasedIndex + 1
            val fixture = fixtures[zeroBasedIndex % fixtures.size]
            val id = "deal-${category.lowercase()}-generated-${ordinal.toString().padStart(3, '0')}"
            val price = fixture.priceInCents + ((zeroBasedIndex / fixtures.size) * 25L)
            val originalPrice = maxOf(price, fixture.originalPriceInCents + ((zeroBasedIndex / fixtures.size) * 50L))

            Deal(
                id = id,
                sourceId = (1_899_200_000_000_000_000L + sourceStart + zeroBasedIndex).toString(),
                brand = fixture.brand,
                title = fixture.title,
                tag = fixture.tag,
                heat = when (zeroBasedIndex % 3) {
                    0 -> "🔥🔥🔥"
                    1 -> "🔥🔥"
                    else -> "🔥"
                },
                priceInCents = price,
                originalPriceInCents = originalPrice,
                category = category,
                postedAt = generatedAt.minusSeconds((minuteStart + zeroBasedIndex).toLong() * 60L),
                description = fixture.description,
                store = fixture.brand,
                workingCount = 42 + ((sourceStart + zeroBasedIndex * 17) % 260),
                notWorkingCount = 2 + ((sourceStart + zeroBasedIndex * 7) % 24),
                userVote = null,
                steps = listOf(
                    "Open the ${fixture.brand} deal page.",
                    "Add the eligible item to your cart.",
                    "Verify the final price before completing checkout.",
                ),
                imageUrl = null,
                dealUrl = "https://priceerrors.app/deals/$id",
            )
        }
    }

    private data class GeneratedFixture(
        val brand: String,
        val title: String,
        val tag: String,
        val priceInCents: Long,
        val originalPriceInCents: Long,
        val description: String,
    )

    private val fixturesByCategory = mapOf(
        "Food" to listOf(
            GeneratedFixture("Target", "Family-size snack bundle marked down in the app", "FOOD DEAL", 175, 1_899, "A limited grocery markdown that may vary by store."),
            GeneratedFixture("Chipotle", "Free guacamole with an eligible entrée order", "FREEBIE", 0, 295, "An app-only food offer available at participating locations."),
            GeneratedFixture("Wendy’s", "Breakfast combo ringing up for less than two dollars", "PRICE DROP", 199, 699, "Check the restaurant app for local participation and final pricing."),
            GeneratedFixture("Starbucks", "Bonus drink offer appearing for Rewards members", "MEMBER DEAL", 250, 625, "A targeted Rewards offer that may not appear on every account."),
        ),
        "Tech" to listOf(
            GeneratedFixture("Amazon", "Noise-cancelling headphones listed at a checkout price error", "PRICE ERROR", 1_299, 12_999, "A short-lived electronics pricing mismatch. Verify the seller and final price."),
            GeneratedFixture("Best Buy", "Portable SSD drops to its lowest checkout price", "TECH DEAL", 2_499, 8_999, "A limited electronics markdown available while inventory lasts."),
            GeneratedFixture("Walmart", "Smart home starter kit marked down more than 70%", "PRICE DROP", 1_899, 7_999, "Shipping and pickup availability can vary by location."),
            GeneratedFixture("Anker", "USB-C charging bundle stacks with an instant coupon", "STACKABLE", 999, 3_999, "Apply the listed coupon before checkout to see the final price."),
        ),
        "Beauty" to listOf(
            GeneratedFixture("Sephora", "Mini fragrance set quietly marked down online", "BEAUTY DROP", 1_200, 3_500, "A limited beauty markdown available while supplies last."),
            GeneratedFixture("Ulta Beauty", "Viral skincare duo stacks with a member coupon", "STACKABLE", 899, 2_800, "Sign in before checkout to see eligible member pricing."),
            GeneratedFixture("Target", "Hair-care gift set scanning below shelf price", "PRICE ERROR", 650, 2_499, "Store inventory and pricing may vary."),
        ),
        "Fashion" to listOf(
            GeneratedFixture("Nike", "Everyday sneakers hit a surprise clearance price", "FASHION DROP", 2_999, 9_500, "Sizes are limited and the sale price appears in the cart."),
            GeneratedFixture("Adidas", "Classic hoodie stacks with an extra promo", "STACKABLE", 1_899, 6_500, "Enter the displayed promotion during checkout."),
            GeneratedFixture("Old Navy", "Summer basics drop below five dollars", "CLEARANCE", 499, 2_499, "Colors and sizes vary by store."),
        ),
        "Gaming" to listOf(
            GeneratedFixture("GameStop", "New release game briefly listed at a used price", "PRICE ERROR", 1_999, 6_999, "Confirm the item condition and price before ordering."),
            GeneratedFixture("PlayStation", "Popular co-op title gets a surprise digital discount", "GAMING DEAL", 999, 3_999, "The digital offer requires an eligible platform account."),
            GeneratedFixture("Xbox", "Wireless controller bundle drops to clearance pricing", "PRICE DROP", 2_499, 7_499, "Color selection and inventory are limited."),
        ),
        "Amazon" to listOf(
            GeneratedFixture("Amazon", "Today-only household essential coupon stacks at checkout", "AMAZON DEAL", 799, 2_499, "Clip the coupon on the product page before checkout."),
            GeneratedFixture("Amazon", "Popular travel accessory hits a lightning price", "LIGHTNING DEAL", 1_099, 3_999, "The promotional quantity is limited."),
            GeneratedFixture("Amazon", "Subscribe & Save offer drops below the usual sale price", "STACKABLE", 599, 1_999, "Choose an eligible subscription frequency to see the offer."),
        ),
        "General" to listOf(
            GeneratedFixture("Costco", "Member favorite bundle returns at a lower warehouse price", "MEMBER DEAL", 1_499, 3_999, "A membership and participating warehouse may be required."),
            GeneratedFixture("CVS", "Weekly coupon stack makes this everyday item nearly free", "COUPON STACK", 99, 1_299, "Send eligible coupons to your card before checkout."),
            GeneratedFixture("Home Depot", "Useful home essential marked down for one day", "PRICE DROP", 799, 2_999, "Local pickup inventory may vary."),
        ),
    )

    /**
     * A 100-item local feed that mirrors the size and category counts shown by
     * the iOS Browse screen. Travel and Events intentionally contain no deals.
     */
    val deals: List<Deal> = buildList {
        add(papaJohnsDeal())
        add(applebeesDeal())
        add(dunkinDeal())
        add(carlsJrDeal())

        // Four featured cards above are Food deals, so 26 more produces 30.
        addAll(generatedDeals(category = "Food", count = 26, sourceStart = 2_001, minuteStart = 8))
        addAll(generatedDeals(category = "Tech", count = 24, sourceStart = 3_001, minuteStart = 40))
        addAll(generatedDeals(category = "Beauty", count = 8, sourceStart = 4_001, minuteStart = 70))
        addAll(generatedDeals(category = "Fashion", count = 7, sourceStart = 5_001, minuteStart = 84))
        addAll(generatedDeals(category = "Gaming", count = 3, sourceStart = 6_001, minuteStart = 96))

        // These 28 deals appear in All, but do not inflate the visible Browse
        // category cards. Amazon has dedicated artwork; General uses Deals art.
        addAll(generatedDeals(category = "Amazon", count = 14, sourceStart = 7_001, minuteStart = 106))
        addAll(generatedDeals(category = "General", count = 14, sourceStart = 8_001, minuteStart = 126))
    }
}
