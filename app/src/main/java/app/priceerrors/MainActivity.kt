package app.priceerrors

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import app.priceerrors.core.analytics.MetaMeasurement
import app.priceerrors.core.navigation.PriceErrorsDeepLink
import app.priceerrors.core.shortcuts.PriceErrorsShortcuts

class MainActivity : ComponentActivity() {
    private val appContainer: AppContainer by lazy {
        (application as PriceErrorsApplication).container
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        routeIntent(intent)
        setContent {
            PriceErrorsApp(
                repository = appContainer.dealRepository,
                googleAuthClient = appContainer.googleAuthClient,
                supabaseAuthClient = appContainer.supabaseAuthClient,
                priceErrorsApi = appContainer.priceErrorsApi,
                billingManager = appContainer.billingManager,
                navigationIntentStore = appContainer.navigationIntentStore,
                notificationCoordinator = appContainer.notificationCoordinator,
                growthAnalytics = appContainer.growthAnalytics,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        MetaMeasurement.applicationDidBecomeActive(application)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeIntent(intent)
    }

    private fun routeIntent(intent: Intent?) {
        if (PriceErrorsShortcuts.isUpgradeShortcut(PriceErrorsShortcuts.shortcutId(this, intent))) {
            appContainer.navigationIntentStore.openUpgradeOffer()
            return
        }
        intent?.getStringExtra(EXTRA_DEAL_ID)?.let { dealId ->
            appContainer.navigationIntentStore.openDeal(dealId)
            return
        }
        when (val route = PriceErrorsDeepLink.route(intent?.dataString)) {
            is PriceErrorsDeepLink.Route.Deal -> {
                if (intent?.getBooleanExtra(EXTRA_NOTIFICATION_OPENED, false) == true) {
                    appContainer.growthAnalytics.track(
                        "notification_opened",
                        mapOf("deal_id" to route.id),
                    )
                }
                appContainer.navigationIntentStore.openDeal(route.id)
            }
            is PriceErrorsDeepLink.Route.GoogleOAuthCallback ->
                appContainer.navigationIntentStore.completeGoogleOAuth(route.url)
            PriceErrorsDeepLink.Route.Home, null -> Unit
        }
    }

    companion object {
        const val EXTRA_DEAL_ID = "deal_id"
        const val EXTRA_NOTIFICATION_OPENED = "notification_opened"
    }
}
