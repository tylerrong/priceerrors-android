package app.priceerrors.core.analytics

import android.app.Application
import android.os.Bundle
import android.util.Log
import app.priceerrors.BuildConfig
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import java.math.BigDecimal
import java.util.Currency

/**
 * Meta Ads install measurement (App Events). Mirrors iOS `MetaMeasurement.swift`.
 *
 * The SDK no-ops until [BuildConfig.FACEBOOK_APP_ID] and
 * [BuildConfig.FACEBOOK_CLIENT_TOKEN] are real values — not placeholders.
 */
object MetaMeasurement {
    private const val TAG = "MetaMeasurement"
    private var logger: AppEventsLogger? = null

    val isConfigured: Boolean
        get() = isUsable(BuildConfig.FACEBOOK_APP_ID) && isUsable(BuildConfig.FACEBOOK_CLIENT_TOKEN)

    fun configure(application: Application) {
        if (!isConfigured) {
            Log.i(TAG, "SDK skipped — set PRICEERRORS_FACEBOOK_APP_ID and PRICEERRORS_FACEBOOK_CLIENT_TOKEN.")
            return
        }

        FacebookSdk.setApplicationId(BuildConfig.FACEBOOK_APP_ID)
        FacebookSdk.setClientToken(BuildConfig.FACEBOOK_CLIENT_TOKEN)
        // Auto-init is disabled in the manifest so configuration cannot run
        // before our BuildConfig values are installed. Initialize explicitly
        // before calling any SDK setting that requires application context.
        @Suppress("DEPRECATION")
        FacebookSdk.sdkInitialize(application)
        FacebookSdk.setAutoInitEnabled(true)
        FacebookSdk.setAutoLogAppEventsEnabled(true)
        FacebookSdk.setAdvertiserIDCollectionEnabled(true)
        FacebookSdk.fullyInitialize()
        logger = AppEventsLogger.newLogger(application)
        activateApp(application)
        if (BuildConfig.DEBUG) {
            Log.i(
                TAG,
                "SDK configured appID=${BuildConfig.FACEBOOK_APP_ID} package=${application.packageName}",
            )
        }
    }

    /** Call on every foreground — Meta uses this for session-based reporting. */
    fun applicationDidBecomeActive(application: Application) {
        if (!isConfigured) return
        activateApp(application)
    }

    fun logCompletedRegistration() {
        if (!isConfigured) return
        logger?.logEvent(AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION)
        logger?.flush()
    }

    fun logPaywallViewed(source: String) {
        if (!isConfigured) return
        logger?.logEvent(
            AppEventsConstants.EVENT_NAME_VIEWED_CONTENT,
            bundleOf(
                AppEventsConstants.EVENT_PARAM_CONTENT_TYPE to "paywall",
                AppEventsConstants.EVENT_PARAM_CONTENT_ID to source,
            ),
        )
    }

    fun logInitiatedCheckout(plan: String) {
        if (!isConfigured) return
        logger?.logEvent(
            AppEventsConstants.EVENT_NAME_INITIATED_CHECKOUT,
            bundleOf(
                AppEventsConstants.EVENT_PARAM_CONTENT_TYPE to "subscription",
                AppEventsConstants.EVENT_PARAM_CONTENT_ID to plan,
            ),
        )
    }

    fun logSubscribe(price: Double, currencyCode: String, isTrial: Boolean) {
        if (!isConfigured) return
        val currency = currencyCode.ifBlank { "USD" }
        val amount = BigDecimal.valueOf(price)
        if (isTrial) {
            logger?.logEvent(
                AppEventsConstants.EVENT_NAME_START_TRIAL,
                amount.toDouble(),
                bundleOf(AppEventsConstants.EVENT_PARAM_CURRENCY to currency),
            )
        }
        logger?.logEvent(
            AppEventsConstants.EVENT_NAME_SUBSCRIBE,
            amount.toDouble(),
            bundleOf(AppEventsConstants.EVENT_PARAM_CURRENCY to currency),
        )
        logger?.logPurchase(amount, Currency.getInstance(currency))
        logger?.flush()
    }

    private fun activateApp(application: Application) {
        if (!isConfigured) return
        if (logger == null) {
            logger = AppEventsLogger.newLogger(application)
        }
        AppEventsLogger.activateApp(application)
        logger?.flush()
    }

    private fun bundleOf(vararg pairs: Pair<String, String>): Bundle =
        Bundle(pairs.size).apply {
            pairs.forEach { (key, value) -> putString(key, value) }
        }

    private fun isUsable(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return false
        if (trimmed.startsWith("YOUR_")) return false
        if (trimmed.contains("REPLACE", ignoreCase = true)) return false
        return true
    }
}
