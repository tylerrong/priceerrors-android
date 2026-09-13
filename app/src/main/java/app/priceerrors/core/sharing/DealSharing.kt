package app.priceerrors.core.sharing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import app.priceerrors.core.analytics.GrowthAnalytics
import app.priceerrors.core.model.Deal
import app.priceerrors.core.navigation.PriceErrorsDeepLink
import app.priceerrors.ui.components.formatPrice

/**
 * Deal share helpers. Mirrors iOS `DealSharing` / `Deal.shareMessage`.
 */
object DealSharing {
    fun canShare(deal: Deal): Boolean =
        deal.id.isNotBlank() && !deal.id.startsWith("placeholder")

    fun shareUrl(deal: Deal): String =
        if (canShare(deal)) {
            PriceErrorsDeepLink.dealShareUrl(deal.id)
        } else {
            PriceErrorsDeepLink.WEBSITE_BASE_URL
        }

    fun shareDiscountText(deal: Deal): String = when {
        deal.discountPercent >= 100 || deal.priceInCents == 0L -> "FREE"
        deal.discountPercent > 0 -> "${deal.discountPercent}% off"
        else -> formatPrice(deal.priceInCents, deal.currencyCode)
    }

    fun shareMessage(deal: Deal): String =
        "${deal.title} - ${shareDiscountText(deal)}\n${shareUrl(deal)}"

    fun analyticsProperties(deal: Deal, source: String? = null): Map<String, String> =
        buildMap {
            put("deal_id", deal.id)
            put("retailer", deal.store)
            put("category", deal.category)
            put("discount_pct", deal.discountPercent.toString())
            put("heat", deal.heat)
            if (source != null) put("source", source)
        }

    fun presentShareSheet(
        context: Context,
        deal: Deal,
        source: String,
        analytics: GrowthAnalytics? = null,
    ) {
        if (!canShare(deal)) return
        analytics?.track("deal_share_opened", analyticsProperties(deal, source))
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareMessage(deal))
        }
        context.startActivity(Intent.createChooser(intent, "Share deal"))
    }

    fun copyLink(
        context: Context,
        deal: Deal,
        source: String,
        analytics: GrowthAnalytics? = null,
    ) {
        if (!canShare(deal)) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("Deal link", shareUrl(deal)))
        analytics?.track("deal_share_link_copied", analyticsProperties(deal, source))
    }
}
