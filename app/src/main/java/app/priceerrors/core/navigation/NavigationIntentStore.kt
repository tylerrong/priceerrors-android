package app.priceerrors.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NavigationIntentStore {
    private val _pendingDealId = MutableStateFlow<String?>(null)
    val pendingDealId: StateFlow<String?> = _pendingDealId.asStateFlow()

    /**
     * Set when the launcher's "Try for free" shortcut starts the app. The UI
     * isn't composed yet on a cold start, so the request is replayed once the
     * main stage is ready.
     */
    private val _pendingTrialOffer = MutableStateFlow(false)
    val pendingTrialOffer: StateFlow<Boolean> = _pendingTrialOffer.asStateFlow()

    fun openDeal(dealId: String?) {
        _pendingDealId.value = dealId?.trim()?.takeIf(String::isNotEmpty)
    }

    fun consumeDeal(dealId: String) {
        if (_pendingDealId.value == dealId) _pendingDealId.value = null
    }

    fun openTrialOffer() {
        _pendingTrialOffer.value = true
    }

    fun consumeTrialOffer() {
        _pendingTrialOffer.value = false
    }
}
