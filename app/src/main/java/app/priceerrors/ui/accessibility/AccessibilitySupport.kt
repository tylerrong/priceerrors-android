package app.priceerrors.ui.accessibility

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Stable identifiers for the critical user journeys exercised by UI tests. */
object PriceErrorsTestTags {
    const val MAIN_NAVIGATION = "main_navigation"
    const val FEED_TAB = "tab_feed"
    const val ALERTS_TAB = "tab_alerts"
    @Deprecated("Community tab removed; use ALERTS_TAB")
    const val COMMUNITY_TAB = "tab_community"
    const val BROWSE_TAB = "tab_browse"
    const val PROFILE_TAB = "tab_profile"

    const val FEED_SCREEN = "feed_screen"
    const val FEED_DEAL = "feed_deal"
    const val REFRESH_DEALS = "refresh_deals"
    const val BROWSE_SCREEN = "browse_screen"
    const val BROWSE_SEARCH = "browse_search"
    const val DEAL_DETAIL = "deal_detail"
    const val SAVE_DEAL = "save_deal"
    const val CLAIM_DEAL = "claim_deal"
    const val PROFILE_SCREEN = "profile_screen"

    const val ONBOARDING_SCREEN = "onboarding_screen"
    const val ONBOARDING_CONTINUE = "onboarding_continue"
    const val PREFERENCES_SCREEN = "preferences_screen"
    const val PREFERENCES_CONTINUE = "preferences_continue"
    const val AUTH_SCREEN = "auth_screen"
    const val AUTH_SUBMIT = "auth_submit"
    const val GOOGLE_AUTH = "google_auth"
    const val GOOGLE_AUTH_LOGO = "google_auth_logo"
    const val PAYWALL_SCREEN = "paywall_screen"
    const val PAYWALL_PURCHASE = "paywall_purchase"
}

/** Android exposes “Remove animations” by setting the global animator scale to zero. */
fun animationsEnabled(animatorDurationScale: Float): Boolean = animatorDurationScale > 0f

@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)
        animationsEnabled(scale)
    }
}
