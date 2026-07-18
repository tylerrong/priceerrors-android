package app.priceerrors.ui.components

import app.priceerrors.core.model.Deal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun formatPrice(amountInCents: Long, currencyCode: String): String {
    if (amountInCents == 0L) return "FREE"
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    formatter.currency = Currency.getInstance(currencyCode)
    formatter.minimumFractionDigits = if (amountInCents % 100L == 0L) 0 else 2
    formatter.maximumFractionDigits = 2
    return formatter.format(amountInCents / 100.0)
}

fun dealAccessibilityLabel(deal: Deal): String = buildString {
    append(deal.title)
    append(", ")
    append(deal.brand)
    append(", ")
    append(formatPrice(deal.priceInCents, deal.currencyCode))
    if (deal.discountPercent > 0) {
        append(", ")
        append(deal.discountPercent)
        append(" percent off")
    }
}
