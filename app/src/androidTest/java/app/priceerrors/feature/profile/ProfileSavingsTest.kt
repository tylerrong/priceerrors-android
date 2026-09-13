package app.priceerrors.feature.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.priceerrors.ui.theme.PriceErrorsTheme
import org.junit.Rule
import org.junit.Test

class ProfileSavingsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun savingsHeroShowsMonthlyAndLifetimeTotals() {
        compose.setContent {
            PriceErrorsTheme {
                ProfileScreen(
                    name = "Deal Hunter",
                    email = "hunter@example.com",
                    isPro = true,
                    dayStreak = 0,
                    savedDeals = emptyList(),
                    monthSavings = 45.50,
                    lifetimeSavings = 125.75,
                    showNavigation = false,
                )
            }
        }

        compose.onNodeWithText("YOUR SAVINGS").assertIsDisplayed()
        compose.onNodeWithText("\$45.50").assertIsDisplayed()
        compose.onNodeWithText("\$125.75").assertIsDisplayed()
        compose.onNodeWithText("saved this month").assertIsDisplayed()
    }
}
