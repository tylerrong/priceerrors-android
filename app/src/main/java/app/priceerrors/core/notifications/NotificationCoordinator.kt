package app.priceerrors.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import app.priceerrors.MainActivity
import app.priceerrors.R
import com.google.firebase.messaging.FirebaseMessaging

class NotificationCoordinator(private val context: Context) {
    private val tokenStore = NotificationTokenStore(context)

    val isFirebaseConfigured: Boolean
        get() = FirebaseBootstrap.isConfigured

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
                FirebaseMessaging.getInstance().apply {
                    isAutoInitEnabled = false
                    unregister()
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
        messaging.isAutoInitEnabled = enabled
        messaging.register()
            .addOnSuccessListener { onComplete(Result.success(Unit)) }
            .addOnFailureListener { error -> onComplete(Result.failure(error)) }
    }

    fun showDealNotification(
        title: String,
        body: String,
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
        val destination = dealId?.let { "https://priceerrors.app/deals/$it" }
            ?: "https://priceerrors.app"
        val intent = Intent(Intent.ACTION_VIEW, destination.toUri(), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            dealId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, DEAL_ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
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
    }
}
