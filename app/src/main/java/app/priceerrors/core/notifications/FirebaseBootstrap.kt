package app.priceerrors.core.notifications

import android.content.Context
import app.priceerrors.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseBootstrap {
    val isConfigured: Boolean
        get() = listOf(
            BuildConfig.FIREBASE_APPLICATION_ID,
            BuildConfig.FIREBASE_API_KEY,
            BuildConfig.FIREBASE_PROJECT_ID,
            BuildConfig.FIREBASE_SENDER_ID,
        ).all(String::isNotBlank)

    fun initialize(context: Context): Boolean {
        if (!isConfigured) return false
        if (FirebaseApp.getApps(context).isNotEmpty()) return true

        val options = FirebaseOptions.Builder()
            .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
            .build()
        FirebaseApp.initializeApp(context, options)
        return true
    }
}
