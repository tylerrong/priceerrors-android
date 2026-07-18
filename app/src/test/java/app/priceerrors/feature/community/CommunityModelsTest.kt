package app.priceerrors.feature.community

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityModelsTest {
    @Test
    fun `canonical user keys ignore casing and repeated whitespace`() {
        assertEquals("price errors team", canonicalCommunityUserKey("  PRICE   Errors Team "))
    }

    @Test
    fun `link normalization permits only safe http links with a host`() {
        assertEquals("https://priceerrors.app/deals", normalizeCommunityLink(" priceerrors.app/deals "))
        assertEquals("http://example.com/item", normalizeCommunityLink("http://example.com/item"))
        assertNull(normalizeCommunityLink("javascript:alert(1)"))
        assertNull(normalizeCommunityLink("https://name:password@example.com/private"))
        assertNull(normalizeCommunityLink("not a link"))
    }

    @Test
    fun `moderation removes reported deals and every post by blocked accounts`() {
        val deals = listOf(
            deal(id = "one", user = "Maya"),
            deal(id = "two", user = "Jordan"),
            deal(id = "three", user = "  MAYA "),
        )

        val visible = visibleCommunityDeals(
            deals = deals,
            reportedDealIds = setOf("two"),
            blockedUserKeys = setOf(canonicalCommunityUserKey("Maya")),
        )

        assertTrue(visible.isEmpty())
    }

    @Test
    fun `post validation requires terms and rejects unsafe links`() {
        val withoutTerms = validateCommunityPost(
            title = "Great deal",
            description = "This is a complete deal description.",
            price = 12.99,
            link = "priceerrors.app/deal",
            termsAccepted = false,
        )
        assertFalse(withoutTerms.isValid)
        assertEquals("Agree to the Community Guidelines before posting.", withoutTerms.message)

        val unsafeLink = validateCommunityPost(
            title = "Great deal",
            description = "This is a complete deal description.",
            price = 12.99,
            link = "javascript:alert(1)",
            termsAccepted = true,
        )
        assertFalse(unsafeLink.isValid)
        assertEquals("Enter a valid http or https link.", unsafeLink.message)
    }

    @Test
    fun `post validation accepts complete safe content`() {
        assertTrue(
            validateCommunityPost(
                title = "Great deal",
                description = "This is a complete deal description.",
                price = 0.0,
                link = "https://priceerrors.app/deal",
                termsAccepted = true,
            ).isValid,
        )
    }

    private fun deal(id: String, user: String) = CommunityDeal(
        id = id,
        userDisplayName = user,
        title = "Example deal",
        description = "Example community deal description",
        category = "Other",
        brand = null,
        url = null,
        price = 1.0,
        createdAtMillis = 0L,
    )
}
