package app.priceerrors.feature.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.priceerrors.core.model.Deal
import app.priceerrors.ui.theme.PriceErrorsTheme
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DealDetailScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun claimConfirmationCannotBeSkippedAndReportsTheChoice() {
        var declined = false
        compose.setContent {
            PriceErrorsTheme {
                DealDetailScreen(
                    deal = testDeal(),
                    onBack = {},
                    showClaimConfirmation = true,
                    claimSavings = 80.0,
                    onDeclineClaim = { declined = true },
                )
            }
        }

        compose.onNodeWithText("Did you get it?").assertIsDisplayed()
        compose.onNodeWithText("YES").assertIsDisplayed()
        compose.onNodeWithText("NO").performClick()
        compose.runOnIdle { assertTrue(declined) }
    }

    private fun testDeal() = Deal(
        id = "deal-1",
        sourceId = "source-1",
        brand = "Target",
        title = "Headphones",
        tag = "ERROR",
        heat = "🔥",
        priceInCents = 2_000,
        originalPriceInCents = 10_000,
        category = "Tech",
        postedAt = Instant.parse("2026-09-12T18:00:00Z"),
        description = "A live deal",
        store = "Target",
        workingCount = 10,
        notWorkingCount = 1,
        userVote = null,
        steps = listOf("Open the retailer"),
        imageUrl = null,
        dealUrl = "https://example.com/deal",
    )
}
