package app.priceerrors.feature.community

import android.content.Context
import androidx.core.content.edit

/**
 * Device-local safety state. Reports are deliberately retained as pending local records until a
 * backend sync contract exists; this class never implies they have reached Price Errors staff.
 */
internal class CommunityModerationStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var termsAccepted: Boolean
        get() = preferences.getInt(KEY_TERMS_VERSION_ACCEPTED, 0) == CURRENT_TERMS_VERSION
        set(value) {
            preferences.edit {
                putInt(KEY_TERMS_VERSION_ACCEPTED, if (value) CURRENT_TERMS_VERSION else 0)
            }
        }

    val reportedDealIds: Set<String>
        get() = preferences.getStringSet(KEY_REPORTED_DEAL_IDS, emptySet()).orEmpty().toSet()

    val blockedUserKeys: Set<String>
        get() = preferences.getStringSet(KEY_BLOCKED_USER_KEYS, emptySet()).orEmpty().toSet()

    val pendingReportCount: Int
        get() = preferences.getStringSet(KEY_PENDING_REPORTS, emptySet()).orEmpty().size

    fun queueReport(
        deal: CommunityDeal,
        reason: CommunityReportReason,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val reportRecord = listOf(
            deal.id,
            canonicalCommunityUserKey(deal.userDisplayName),
            reason.name,
            nowMillis.toString(),
        ).joinToString(RECORD_SEPARATOR)
        preferences.edit {
            putStringSet(KEY_REPORTED_DEAL_IDS, reportedDealIds + deal.id)
            putStringSet(
                KEY_PENDING_REPORTS,
                preferences.getStringSet(KEY_PENDING_REPORTS, emptySet()).orEmpty() + reportRecord,
            )
        }
    }

    fun blockUser(displayName: String) {
        val key = canonicalCommunityUserKey(displayName)
        if (key.isBlank()) return
        preferences.edit { putStringSet(KEY_BLOCKED_USER_KEYS, blockedUserKeys + key) }
    }

    fun unblockUser(userKey: String) {
        preferences.edit {
            putStringSet(KEY_BLOCKED_USER_KEYS, blockedUserKeys - canonicalCommunityUserKey(userKey))
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "community_moderation"
        const val CURRENT_TERMS_VERSION = 1
        const val KEY_TERMS_VERSION_ACCEPTED = "terms_version_accepted"
        const val KEY_REPORTED_DEAL_IDS = "reported_deal_ids"
        const val KEY_BLOCKED_USER_KEYS = "blocked_user_keys"
        const val KEY_PENDING_REPORTS = "pending_reports"
        const val RECORD_SEPARATOR = "\u001F"
    }
}
