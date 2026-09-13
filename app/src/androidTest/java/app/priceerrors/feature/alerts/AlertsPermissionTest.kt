package app.priceerrors.feature.alerts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.priceerrors.ui.theme.PriceErrorsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AlertsPermissionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun enablingAlertsRequestsAndroidNotificationPermission() {
        var permissionRequested = false
        compose.setContent {
            PriceErrorsTheme {
                AlertsScreen(
                    notifyAllDeals = false,
                    onNotifyAllDealsChanged = {},
                    watches = emptyList(),
                    onAddWatch = {},
                    onRemoveWatch = {},
                    preferredCategories = emptySet(),
                    onPreferredCategoriesChanged = {},
                    alertMinimumDiscount = 50,
                    onAlertMinimumDiscountChanged = {},
                    recentAlerts = emptyList(),
                    notificationsPermissionGranted = false,
                    onRequestNotificationPermission = { permissionRequested = true },
                )
            }
        }

        compose.onNodeWithText("Notifications are off — tap to enable").assertIsDisplayed()
        compose.onNodeWithTag("all_deal_alerts_toggle").performClick()
        compose.runOnIdle { assertTrue(permissionRequested) }
    }
}
