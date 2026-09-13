package app.priceerrors.core.notifications

import android.content.Context
import androidx.core.content.edit

/** Persists the current FCM token and its backend registration state. */
class NotificationTokenStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "priceerrors_notifications",
        Context.MODE_PRIVATE,
    )

    var pendingToken: String?
        get() = preferences.getString("pending_fcm_token", null)
        set(value) {
            preferences.edit {
                val previous = preferences.getString("pending_fcm_token", null)
                if (value == null) remove("pending_fcm_token") else putString("pending_fcm_token", value)
                if (previous != value) {
                    remove("registered_fcm_token")
                    remove("registered_user_id")
                }
            }
        }

    var notificationsEnabled: Boolean
        get() = preferences.getBoolean("notifications_enabled", false)
        set(value) = preferences.edit { putBoolean("notifications_enabled", value) }

    fun isRegistered(token: String, userId: String): Boolean =
        preferences.getString("registered_fcm_token", null) == token &&
            preferences.getString("registered_user_id", null) == userId

    fun markRegistered(token: String, userId: String) {
        preferences.edit {
            putString("registered_fcm_token", token)
            putString("registered_user_id", userId)
        }
    }

    fun clearRegistration() {
        preferences.edit {
            remove("registered_fcm_token")
            remove("registered_user_id")
        }
    }
}
