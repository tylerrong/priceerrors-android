package app.priceerrors.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import app.priceerrors.MainActivity
import app.priceerrors.R
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.Executors

class NotificationCoordinator(private val context: Context) {
    private val tokenStore = NotificationTokenStore(context)

    val isFirebaseConfigured: Boolean
        get() = FirebaseBootstrap.isConfigured

    fun areNotificationsEnabled(): Boolean {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            DEAL_ALERTS_CHANNEL_ID,
            context.getString(R.string.notification_channel_deals),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_deals_description)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun setEnabled(enabled: Boolean, onComplete: (Result<Unit>) -> Unit = {}) {
        tokenStore.notificationsEnabled = enabled
        if (!enabled) {
            if (FirebaseBootstrap.initialize(context)) {
                FirebaseMessaging.getInstance().let { messaging ->
                    FCM_EXECUTOR.execute {
                        runCatching {
                            messaging.isAutoInitEnabled = false
                            messaging.unregister()
                        }
                    }
                }
            }
            tokenStore.pendingToken = null
            onComplete(Result.success(Unit))
            return
        }
        if (!FirebaseBootstrap.initialize(context)) {
            onComplete(
                Result.failure(
                    IllegalStateException("Firebase Cloud Messaging is not configured for this build."),
                ),
            )
            return
        }

        val messaging = FirebaseMessaging.getInstance()
        FCM_EXECUTOR.execute {
            runCatching {
                messaging.isAutoInitEnabled = true
                messaging.register()
                    .addOnSuccessListener(FCM_EXECUTOR) {
                        val token = awaitRegisteredToken()
                        val result = if (token != null) {
                            Result.success(Unit)
                        } else {
                            Result.failure(
                                IllegalStateException("Firebase did not provide a messaging token."),
                            )
                        }
                        completeOnMain(onComplete, result)
                    }
                    .addOnFailureListener(FCM_EXECUTOR) { error ->
                        completeOnMain(onComplete, Result.failure(error))
                    }
            }.onFailure { error ->
                completeOnMain(onComplete, Result.failure(error))
            }
        }
    }

    fun showDealNotification(
        title: String,
        body: String,
        subtitle: String? = null,
        dealId: String?,
    ) {
        if (!tokenStore.notificationsEnabled) return
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannels()
        val website = context.getString(R.string.website_url)
        val destination = dealId?.let { "$website/deals/$it" } ?: website
        val intent = Intent(Intent.ACTION_VIEW, destination.toUri(), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_NOTIFICATION_OPENED, true)
        val pendingIntent = PendingIntent.getActivity(
            context,
            dealId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val expandedText = listOfNotNull(
            subtitle?.takeIf(String::isNotBlank),
            body.takeIf(String::isNotBlank),
        ).joinToString("\n")
        val notification = NotificationCompat.Builder(context, DEAL_ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(subtitle?.takeIf(String::isNotBlank) ?: body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(
            dealId?.hashCode() ?: (System.currentTimeMillis() and Int.MAX_VALUE.toLong()).toInt(),
            notification,
        )
    }

    companion object {
        const val DEAL_ALERTS_CHANNEL_ID = "deal_alerts"
        private val FCM_EXECUTOR = Executors.newSingleThreadExecutor()
        private val MAIN_HANDLER = Handler(Looper.getMainLooper())

        private fun completeOnMain(
            callback: (Result<Unit>) -> Unit,
            result: Result<Unit>,
        ) {
            MAIN_HANDLER.post { callback(result) }
        }
    }

    private fun awaitRegisteredToken(): String? {
        repeat(50) {
            tokenStore.pendingToken?.let { return it }
            Thread.sleep(200)
        }
        return null
    }
}
