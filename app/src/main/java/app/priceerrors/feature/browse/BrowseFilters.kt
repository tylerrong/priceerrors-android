package app.priceerrors.feature.browse

import app.priceerrors.core.model.Deal

fun dealMatchesBrowseCategory(deal: Deal, category: String): Boolean {
    val normalizedCategory = category.trim().lowercase()
    if (normalizedCategory == "all") return true
    val dealCategory = deal.category.trim().lowercase()
    return dealCategory == normalizedCategory ||
        (normalizedCategory == "events" && dealCategory == "event")
}

fun dealMatchesSearch(deal: Deal, query: String): Boolean {
    val terms = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    if (terms.isEmpty()) return true
    val searchableText = listOf(deal.title, deal.brand, deal.tag, deal.category, deal.store)
        .joinToString(separator = " ")
        .lowercase()
    return terms.all { term -> searchableText.contains(term.lowercase()) }
}

fun filterBrowseDeals(
    deals: List<Deal>,
    category: String,
    query: String,
): List<Deal> = deals.filter { deal ->
    dealMatchesBrowseCategory(deal, category) && dealMatchesSearch(deal, query)
}
