package app.priceerrors.feature.paywall

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.priceerrors.ui.accessibility.PriceErrorsTestTags
import app.priceerrors.ui.theme.PriceErrorsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PaywallScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun selectedPlanIsSentToCheckout() {
        var purchasedPlan: PaywallPlan? = null
        compose.setContent {
            PriceErrorsTheme {
                PaywallScreen(
                    allowDismiss = true,
                    onDismiss = {},
                    onPurchase = { purchasedPlan = it },
                    onRestore = {},
                )
            }
        }

        compose.onNodeWithTag("plan_monthly").performClick().assertIsSelected()
        compose.onNodeWithTag(PriceErrorsTestTags.PAYWALL_PURCHASE).performClick()

        compose.runOnIdle { assertEquals(PaywallPlan.MONTHLY, purchasedPlan) }
    }
}
