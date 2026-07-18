package app.priceerrors.core.notifications

import android.content.Context
import androidx.core.content.edit

/**
 * Keeps the newest FCM token until the backend device-registration endpoint is
 * connected. Tokens are intentionally not logged.
 */
class NotificationTokenStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "priceerrors_notifications",
        Context.MODE_PRIVATE,
    )

    var pendingToken: String?
        get() = preferences.getString("pending_fcm_token", null)
        set(value) {
            preferences.edit {
                if (value == null) remove("pending_fcm_token") else putString("pending_fcm_token", value)
            }
        }

    var notificationsEnabled: Boolean
        get() = preferences.getBoolean("notifications_enabled", false)
        set(value) = preferences.edit { putBoolean("notifications_enabled", value) }
}
