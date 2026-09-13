package app.priceerrors.feature.paywall

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import app.priceerrors.ui.theme.PriceErrorsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RescueOfferScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun closeButtonDismissesTheOffer() {
        var dismissed = false
        compose.setContent {
            PriceErrorsTheme {
                RescueOfferScreen(
                    monthlyPrice = "$4.99",
                    renewalPrice = "$9.99",
                    usesIntroductoryPrice = true,
                    isLoading = false,
                    message = null,
                    onDismiss = { dismissed = true },
                    onPurchase = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Close offer").performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }
}
