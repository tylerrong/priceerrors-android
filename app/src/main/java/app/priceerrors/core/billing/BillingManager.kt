package app.priceerrors.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import app.priceerrors.BuildConfig
import app.priceerrors.core.analytics.PostHogAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class BillingPlan {
    WEEKLY,
    MONTHLY,
    YEARLY,
    RESCUE,
}

data class BillingProduct(
    val plan: BillingPlan,
    val productId: String,
    val formattedPrice: String,
    val offerToken: String,
    val introductoryPrice: String? = null,
)

data class BillingState(
    val isConfigured: Boolean = false,
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val isPro: Boolean = false,
    val products: Map<BillingPlan, BillingProduct> = emptyMap(),
    val message: String? = null,
) {
    val rescueAvailable: Boolean
        get() = products.containsKey(BillingPlan.RESCUE)
}

/**
 * Complete Play Billing client flow. Purchase tokens are acknowledged locally
 * so test products can be exercised; production entitlement verification must
 * become server-authoritative when the backend milestone is implemented.
 */
class BillingManager(context: Context) : PurchasesUpdatedListener {
    var onSubscriptionPurchased: ((BillingPlan, Double, String, Boolean) -> Unit)? = null
    private val productDetails = mutableMapOf<BillingPlan, ProductDetails>()
    private val coreProductIds: Map<BillingPlan, String> = mapOf(
        BillingPlan.WEEKLY to BuildConfig.BILLING_WEEKLY_PRODUCT_ID,
        BillingPlan.MONTHLY to BuildConfig.BILLING_MONTHLY_PRODUCT_ID,
        BillingPlan.YEARLY to BuildConfig.BILLING_YEARLY_PRODUCT_ID,
    )
    private val rescueProductId: String = BuildConfig.BILLING_RESCUE_PRODUCT_ID
    private val productIds: Map<BillingPlan, String> = buildMap {
        putAll(coreProductIds)
        if (rescueProductId.isNotBlank()) put(BillingPlan.RESCUE, rescueProductId)
    }
    private val configured = coreProductIds.values.all(String::isNotBlank)
    private val _state = MutableStateFlow(BillingState(isConfigured = configured))
    val state: StateFlow<BillingState> = _state.asStateFlow()

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    init {
        if (configured) connect()
    }

    fun connect() {
        if (!configured || billingClient.isReady) return
        _state.update { it.copy(isLoading = true, message = null) }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.update { it.copy(isConnected = true, isLoading = false) }
                    queryProducts()
                    restorePurchases(silent = true)
                } else {
                    publishBillingError(result, "Couldn't connect to Google Play Billing")
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.update {
                    it.copy(
                        isConnected = false,
                        isLoading = false,
                        message = "Google Play Billing disconnected. Try again.",
                    )
                }
            }
        })
    }

    fun purchase(activity: Activity, plan: BillingPlan) {
        if (!configured) {
            _state.update {
                it.copy(message = "Subscription products have not been configured yet.")
            }
            return
        }
        if (!billingClient.isReady) {
            connect()
            _state.update { it.copy(message = "Connecting to Google Play. Try again in a moment.") }
            return
        }
        val details = productDetails[plan]
        val offerToken = state.value.products[plan]?.offerToken
        if (details == null || offerToken.isNullOrBlank()) {
            _state.update { it.copy(message = "This subscription plan is not available.") }
            queryProducts()
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()
        val result = billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build(),
        )
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            publishBillingError(result, "Couldn't open Google Play checkout")
        } else {
            _state.update { it.copy(isLoading = true, message = null) }
        }
    }

    fun restorePurchases(silent: Boolean = false) {
        if (!configured) {
            if (!silent) {
                _state.update {
                    it.copy(message = "Subscription products have not been configured yet.")
                }
            }
            return
        }
        if (!billingClient.isReady) {
            connect()
            if (!silent) {
                _state.update { it.copy(message = "Connecting to Google Play. Try restore again shortly.") }
            }
            return
        }
        _state.update { it.copy(isLoading = !silent, message = null) }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val active = purchases.filter { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.products.any { it in productIds.values }
                }
                active.forEach(::acknowledgeIfNeeded)
                _state.update {
                    it.copy(
                        isLoading = false,
                        isPro = active.isNotEmpty(),
                        message = when {
                            silent -> null
                            active.isNotEmpty() -> "Subscription restored."
                            else -> "No active PriceErrors Pro subscription was found."
                        },
                    )
                }
            } else {
                publishBillingError(result, "Couldn't restore purchases")
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchased = purchases.orEmpty()
                    .filter { purchase ->
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            purchase.products.any { it in productIds.values }
                    }
                purchased.forEach { purchase ->
                    acknowledgeIfNeeded(purchase)
                    purchase.products
                        .firstNotNullOfOrNull { productId -> productIds.entries.firstOrNull { it.value == productId }?.key }
                        ?.let { plan ->
                            productDetails[plan]?.let { details ->
                                val offer = details.subscriptionOfferDetails?.firstOrNull()
                                val phase = offer?.pricingPhases?.pricingPhaseList?.lastOrNull()
                                val price = phase?.priceAmountMicros?.toDouble()?.div(1_000_000.0) ?: 0.0
                                val currency = phase?.priceCurrencyCode ?: "USD"
                                val isTrial = (offer?.pricingPhases?.pricingPhaseList?.size ?: 0) > 1 &&
                                    offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceAmountMicros == 0L
                                onSubscriptionPurchased?.invoke(plan, price, currency, isTrial)
                            }
                        }
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        isPro = it.isPro || purchased.isNotEmpty(),
                        message = if (purchased.isNotEmpty()) {
                            "Welcome to PriceErrors Pro."
                        } else {
                            "Your purchase is pending approval."
                        },
                    )
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.update { it.copy(isLoading = false, message = null) }
            }

            else -> publishBillingError(result, "Purchase couldn't be completed")
        }
    }

    private fun queryProducts() {
        val products = productIds.map { (_, id) ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()
        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                publishBillingError(result, "Couldn't load subscription plans")
                return@queryProductDetailsAsync
            }

            val mapped = buildMap {
                queryResult.productDetailsList.forEach { details ->
                    val plan = productIds.entries.firstOrNull { it.value == details.productId }?.key
                        ?: return@forEach
                    val offer = details.subscriptionOfferDetails?.firstOrNull()
                        ?: return@forEach
                    val phases = offer.pricingPhases.pricingPhaseList
                    val price = phases.lastOrNull()?.formattedPrice ?: return@forEach
                    val intro = phases.firstOrNull()
                        ?.takeIf { phases.size > 1 }
                        ?.formattedPrice
                    productDetails[plan] = details
                    put(
                        plan,
                        BillingProduct(
                            plan = plan,
                            productId = details.productId,
                            formattedPrice = price,
                            offerToken = offer.offerToken,
                            introductoryPrice = intro,
                        ),
                    )
                }
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    products = mapped,
                    message = if (mapped.filterKeys { plan -> plan != BillingPlan.RESCUE }.isEmpty()) {
                        "No active subscription products were returned by Google Play."
                    } else {
                        null
                    },
                )
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                publishBillingError(result, "Purchase succeeded but acknowledgement failed")
            }
        }
    }

    private fun publishBillingError(result: BillingResult, prefix: String) {
        val detail = result.debugMessage.takeIf(String::isNotBlank)
        PostHogAnalytics.captureException(
            IllegalStateException("$prefix (billing code ${result.responseCode})"),
            mapOf(
                "flow" to "play_billing",
                "response_code" to result.responseCode.toString(),
            ),
        )
        _state.update {
            it.copy(
                isLoading = false,
                message = if (detail == null) prefix else "$prefix: $detail",
            )
        }
    }
}
