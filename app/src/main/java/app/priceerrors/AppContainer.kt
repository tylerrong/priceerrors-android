package app.priceerrors

import android.content.Context
import app.priceerrors.core.auth.GoogleCredentialAuthClient
import app.priceerrors.core.billing.BillingManager
import app.priceerrors.core.data.DealRepository
import app.priceerrors.core.data.FakeDealRepository
import app.priceerrors.core.navigation.NavigationIntentStore
import app.priceerrors.core.notifications.NotificationCoordinator

interface AppContainer {
    val dealRepository: DealRepository
    val googleAuthClient: GoogleCredentialAuthClient
    val billingManager: BillingManager
    val navigationIntentStore: NavigationIntentStore
    val notificationCoordinator: NotificationCoordinator
}

class DefaultAppContainer(context: Context) : AppContainer {
    override val dealRepository: DealRepository = FakeDealRepository()
    override val googleAuthClient = GoogleCredentialAuthClient()
    override val billingManager = BillingManager(context)
    override val navigationIntentStore = NavigationIntentStore()
    override val notificationCoordinator = NotificationCoordinator(context)
}
