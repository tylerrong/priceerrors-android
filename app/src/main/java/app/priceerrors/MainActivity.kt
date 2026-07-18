package app.priceerrors

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import app.priceerrors.core.navigation.PriceErrorsDeepLink

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
        val dealId = intent?.getStringExtra(EXTRA_DEAL_ID)
            ?: PriceErrorsDeepLink.dealId(intent?.dataString)
        appContainer.navigationIntentStore.openDeal(dealId)
    }

    companion object {
        const val EXTRA_DEAL_ID = "deal_id"
    }
}
