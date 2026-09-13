package app.priceerrors

import android.content.Context
import androidx.core.content.edit
import app.priceerrors.feature.alerts.DealWatch
import app.priceerrors.feature.alerts.decodeDealWatches
import app.priceerrors.feature.alerts.encodeDealWatches
import app.priceerrors.ui.theme.AppearanceOption
import app.priceerrors.ui.theme.FeedLayoutOption
import app.priceerrors.ui.theme.PaletteOption

class PriceErrorsPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("priceerrors_ui", Context.MODE_PRIVATE)

    var hasCompletedOnboarding: Boolean
        get() = preferences.getBoolean("has_completed_onboarding", false)
        set(value) = preferences.edit { putBoolean("has_completed_onboarding", value) }

    var isSignedIn: Boolean
        get() = preferences.getBoolean("is_signed_in", false)
        set(value) = preferences.edit { putBoolean("is_signed_in", value) }

    var isPro: Boolean
        get() = preferences.getBoolean("is_pro", false)
        set(value) = preferences.edit { putBoolean("is_pro", value) }

    var pushAlertsEnabled: Boolean
        get() = preferences.getBoolean("push_alerts_enabled", false)
        set(value) = preferences.edit { putBoolean("push_alerts_enabled", value) }

    var notifyAllDeals: Boolean
        get() = preferences.getBoolean("notify_all_deals", true)
        set(value) = preferences.edit { putBoolean("notify_all_deals", value) }

    var alertMinimumDiscount: Int
        get() = preferences.getInt("alert_minimum_discount", 40)
        set(value) = preferences.edit { putInt("alert_minimum_discount", value) }

    var dealWatchesJson: String
        get() = preferences.getString("deal_watches_json", "[]") ?: "[]"
        set(value) = preferences.edit { putString("deal_watches_json", value) }

    var dealWatches: List<DealWatch>
        get() = decodeDealWatches(dealWatchesJson)
        set(value) {
            dealWatchesJson = encodeDealWatches(value)
        }

    var postPurchaseSetupCompleted: Boolean
        get() = preferences.getBoolean("post_purchase_setup_completed", false)
        set(value) = preferences.edit { putBoolean("post_purchase_setup_completed", value) }

    var scrollHintDismissed: Boolean
        get() = preferences.getBoolean("scroll_hint_dismissed", false)
        set(value) = preferences.edit { putBoolean("scroll_hint_dismissed", value) }

    var reviewPromptedVersion: String?
        get() = preferences.getString("review_prompted_version", null)
        set(value) = preferences.edit {
            if (value == null) remove("review_prompted_version")
            else putString("review_prompted_version", value)
        }

    var confirmedDealIds: Set<String>
        get() = preferences.getStringSet("confirmed_deal_ids", emptySet())?.toSet().orEmpty()
        set(value) = preferences.edit { putStringSet("confirmed_deal_ids", value) }

    var displayName: String
        get() = preferences.getString("display_name", "Tyler Rong") ?: "Tyler Rong"
        set(value) = preferences.edit { putString("display_name", value) }

    var email: String
        get() = preferences.getString("email", "tylerrong613@gmail.com") ?: "tylerrong613@gmail.com"
        set(value) = preferences.edit { putString("email", value) }

    var palette: PaletteOption
        get() = PaletteOption.fromKey(preferences.getString("palette", null))
        set(value) = preferences.edit { putString("palette", value.key) }

    var appearance: AppearanceOption
        get() = AppearanceOption.fromKey(preferences.getString("appearance", null))
        set(value) = preferences.edit { putString("appearance", value.key) }

    var feedLayout: FeedLayoutOption
        get() = FeedLayoutOption.fromKey(preferences.getString("feed_layout", null))
        set(value) = preferences.edit { putString("feed_layout", value.key) }

    var preferredCategories: Set<String>
        get() = preferences.getStringSet("preferred_categories", emptySet())?.toSet().orEmpty()
        set(value) = preferences.edit { putStringSet("preferred_categories", value) }

    var savedDealIds: Set<String>
        get() = preferences.getStringSet("saved_deal_ids", emptySet())?.toSet().orEmpty()
        set(value) = preferences.edit { putStringSet("saved_deal_ids", value) }

    var claimedDealIds: Set<String>
        get() = preferences.getStringSet("claimed_deal_ids", emptySet())?.toSet().orEmpty()
        set(value) = preferences.edit { putStringSet("claimed_deal_ids", value) }

    var voteEntries: Set<String>
        get() = preferences.getStringSet("vote_entries", emptySet())?.toSet().orEmpty()
        set(value) = preferences.edit { putStringSet("vote_entries", value) }

    fun resetAccount() {
        preferences.edit {
            putBoolean("has_completed_onboarding", false)
            putBoolean("is_signed_in", false)
            putBoolean("is_pro", false)
            putBoolean("notify_all_deals", true)
            putInt("alert_minimum_discount", 40)
            putString("deal_watches_json", "[]")
            putBoolean("post_purchase_setup_completed", false)
            putBoolean("scroll_hint_dismissed", false)
            remove("review_prompted_version")
            remove("display_name")
            remove("email")
            remove("preferred_categories")
            remove("saved_deal_ids")
            remove("claimed_deal_ids")
            remove("confirmed_deal_ids")
            remove("vote_entries")
        }
    }
}
