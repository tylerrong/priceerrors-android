package app.priceerrors

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
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
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeIntent(intent)
    }

    private fun routeIntent(intent: Intent?) {
        if (PriceErrorsShortcuts.shortcutId(this, intent) == PriceErrorsShortcuts.TRIAL_ID) {
            appContainer.navigationIntentStore.openTrialOffer()
            return
        }
        val dealId = intent?.getStringExtra(EXTRA_DEAL_ID)
            ?: PriceErrorsDeepLink.dealId(intent?.dataString)
        appContainer.navigationIntentStore.openDeal(dealId)
    }

    companion object {
        const val EXTRA_DEAL_ID = "deal_id"
    }
}
