package app.priceerrors.core.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import app.priceerrors.MainActivity
import app.priceerrors.R

/**
 * Launcher long-press shortcuts — the Android counterpart to the iOS Home
 * Screen quick actions.
 *
 * Long-pressing the icon is also the gesture people use to uninstall, so the
 * menu doubles as an intervention point: the upgrade offer sits right next to
 * "Uninstall". Shortcuts are published dynamically rather than declared in a
 * static `shortcuts.xml` so the offer can disappear the moment someone
 * subscribes. The launcher persists what we publish, so the menu survives a
 * cold start.
 */
object PriceErrorsShortcuts {

    /** Matches the iOS quick-action identifier so both clients log the same id. */
    const val UPGRADE_ID = "app.priceerrors.upgrade"

    /** @deprecated Prefer [UPGRADE_ID]; kept so cold-start intents from older builds still route. */
    const val TRIAL_ID = "app.priceerrors.trial"

    /** Intent extra naming the shortcut that launched the activity. */
    const val EXTRA_SHORTCUT = "shortcut_action"

    /**
     * Republishes the menu for the current entitlement. Subscribers already have
     * everything, so pitching an upgrade would be noise — they get no custom menu.
     */
    fun refresh(context: Context, isPro: Boolean) {
        val appContext = context.applicationContext
        if (isPro) {
            ShortcutManagerCompat.removeAllDynamicShortcuts(appContext)
            return
        }

        val intent = Intent(appContext, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(EXTRA_SHORTCUT, UPGRADE_ID)

        val upgrade = ShortcutInfoCompat.Builder(appContext, UPGRADE_ID)
            .setShortLabel("Upgrade to Pro")
            .setLongLabel("Unlock every deal, every day")
            .setIcon(IconCompat.createWithResource(appContext, R.drawable.ic_shortcut_gift))
            .setIntent(intent)
            .build()

        // Failures here are cosmetic — a launcher may be rate-limiting shortcut
        // updates — and must never take the app down on a cold start.
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(appContext, listOf(upgrade)) }
    }

    /**
     * The shortcut that launched this intent, if any. Reports the usage back to
     * the launcher so it can rank the shortcut and surface it in predictions.
     */
    fun shortcutId(context: Context, intent: Intent?): String? {
        val id = intent?.getStringExtra(EXTRA_SHORTCUT)?.takeIf(String::isNotBlank) ?: return null
        runCatching { ShortcutManagerCompat.reportShortcutUsed(context.applicationContext, id) }
        return id
    }

    fun isUpgradeShortcut(id: String?): Boolean =
        id == UPGRADE_ID || id == TRIAL_ID
}
