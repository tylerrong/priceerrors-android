package app.priceerrors

import android.app.Application
import app.priceerrors.core.analytics.MetaMeasurement
import app.priceerrors.core.analytics.PostHogAnalytics
import app.priceerrors.core.notifications.FirebaseBootstrap

class PriceErrorsApplication : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        PostHogAnalytics.setup(this)
        MetaMeasurement.configure(this)
        container.notificationCoordinator.createChannels()
        FirebaseBootstrap.initialize(applicationContext)
    }
}
