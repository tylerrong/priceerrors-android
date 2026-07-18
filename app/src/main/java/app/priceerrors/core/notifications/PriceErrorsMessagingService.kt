package app.priceerrors.core.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PriceErrorsMessagingService : FirebaseMessagingService() {
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        onRegistered(token)
    }

    override fun onRegistered(token: String) {
        NotificationTokenStore(applicationContext).pendingToken = token
    }

    override fun onUnregistered(token: String) {
        NotificationTokenStore(applicationContext).pendingToken = null
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "New PriceErrors deal"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "A new deal just dropped."
        val dealId = message.data["dealId"] ?: message.data["deal_id"]
        NotificationCoordinator(applicationContext).showDealNotification(
            title = title,
            body = body,
            dealId = dealId,
        )
    }
}
