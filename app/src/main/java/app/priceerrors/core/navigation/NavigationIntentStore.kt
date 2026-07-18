package app.priceerrors.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NavigationIntentStore {
    private val _pendingDealId = MutableStateFlow<String?>(null)
    val pendingDealId: StateFlow<String?> = _pendingDealId.asStateFlow()

    fun openDeal(dealId: String?) {
        _pendingDealId.value = dealId?.trim()?.takeIf(String::isNotEmpty)
    }

    fun consumeDeal(dealId: String) {
        if (_pendingDealId.value == dealId) _pendingDealId.value = null
    }
}
