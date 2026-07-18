package app.priceerrors

import android.app.Application
import app.priceerrors.core.notifications.FirebaseBootstrap

class PriceErrorsApplication : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        container.notificationCoordinator.createChannels()
        FirebaseBootstrap.initialize(applicationContext)
    }
}
