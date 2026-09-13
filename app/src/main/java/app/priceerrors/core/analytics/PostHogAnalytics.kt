package app.priceerrors.core.analytics

import android.app.Application
import android.util.Log
import app.priceerrors.BuildConfig
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

/**
 * Product analytics via PostHog. Mirrors iOS `PostHogAnalytics.swift`.
 *
 * No-ops until [BuildConfig.POSTHOG_API_KEY] is a real `phc_...` key.
 * Events are captured through [GrowthAnalytics.track] — not here directly.
 */
object PostHogAnalytics {
    private const val TAG = "PostHogAnalytics"

    val isConfigured: Boolean
        get() {
            val key = BuildConfig.POSTHOG_API_KEY
            return key.startsWith("phc_") && !key.contains("YOUR_")
        }

    fun setup(application: Application) {
        if (!isConfigured) {
            Log.i(TAG, "SDK skipped — set PRICEERRORS_POSTHOG_API_KEY.")
            return
        }
        val config = PostHogAndroidConfig(
            apiKey = BuildConfig.POSTHOG_API_KEY,
            host = BuildConfig.POSTHOG_HOST,
        ).apply {
            captureScreenViews = false
            captureDeepLinks = false
            sessionReplay = false
            errorTrackingConfig.autoCapture = true
        }
        PostHogAndroid.setup(application, config)
    }

    fun identify(userId: String) {
        if (!isConfigured) return
        PostHog.identify(distinctId = userId)
    }

    fun reset() {
        if (!isConfigured) return
        PostHog.reset()
    }

    fun capture(name: String, properties: Map<String, String> = emptyMap()) {
        if (!isConfigured) return
        PostHog.capture(event = name, properties = properties)
    }

    fun captureException(
        error: Throwable,
        properties: Map<String, String> = emptyMap(),
    ) {
        if (!isConfigured) return
        PostHog.captureException(throwable = error, properties = properties)
    }
}
