package app.priceerrors.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NavigationIntentStore {
    private val _pendingDealId = MutableStateFlow<String?>(null)
    val pendingDealId: StateFlow<String?> = _pendingDealId.asStateFlow()

    private val _pendingGoogleOAuthCallback = MutableStateFlow<String?>(null)
    val pendingGoogleOAuthCallback: StateFlow<String?> =
        _pendingGoogleOAuthCallback.asStateFlow()

    /**
     * Set when the launcher's "Upgrade to Pro" shortcut starts the app. The UI
     * isn't composed yet on a cold start, so the request is replayed once the
     * main stage is ready.
     */
    private val _pendingUpgradeOffer = MutableStateFlow(false)
    val pendingUpgradeOffer: StateFlow<Boolean> = _pendingUpgradeOffer.asStateFlow()

    fun openDeal(dealId: String?) {
        _pendingDealId.value = dealId?.trim()?.takeIf(String::isNotEmpty)
    }

    fun consumeDeal(dealId: String) {
        if (_pendingDealId.value == dealId) _pendingDealId.value = null
    }

    fun completeGoogleOAuth(callbackUrl: String) {
        _pendingGoogleOAuthCallback.value = callbackUrl
    }

    fun consumeGoogleOAuth(callbackUrl: String) {
        if (_pendingGoogleOAuthCallback.value == callbackUrl) {
            _pendingGoogleOAuthCallback.value = null
        }
    }

    fun openUpgradeOffer() {
        _pendingUpgradeOffer.value = true
    }

    fun consumeUpgradeOffer() {
        _pendingUpgradeOffer.value = false
    }
}
