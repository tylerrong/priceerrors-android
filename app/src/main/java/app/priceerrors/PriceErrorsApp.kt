package app.priceerrors

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.play.core.review.ReviewManagerFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import app.priceerrors.core.analytics.GrowthAnalytics
import app.priceerrors.core.analytics.MetaMeasurement
import app.priceerrors.core.analytics.PostHogAnalytics
import app.priceerrors.core.data.DealRepository
import app.priceerrors.core.data.NetworkDealRepository
import app.priceerrors.core.auth.EmailSignUpResult
import app.priceerrors.core.auth.GoogleCredentialAuthClient
import app.priceerrors.core.auth.GoogleOAuthPending
import app.priceerrors.core.auth.GooglePlayRegistrationException
import app.priceerrors.core.auth.SupabaseAuthClient
import android.os.Bundle
import app.priceerrors.core.network.ApiConfig
import app.priceerrors.core.network.ApiError
import app.priceerrors.core.network.AppLinks
import app.priceerrors.core.network.PriceErrorsApi
import app.priceerrors.core.billing.BillingManager
import app.priceerrors.core.billing.BillingPlan
import app.priceerrors.core.navigation.NavigationIntentStore
import app.priceerrors.core.sharing.DealSharing
import app.priceerrors.core.shortcuts.PriceErrorsShortcuts
import app.priceerrors.core.notifications.NotificationCoordinator
import app.priceerrors.core.notifications.NotificationTokenStore
import app.priceerrors.core.model.DealVote
import app.priceerrors.feature.alerts.AlertsScreen
import app.priceerrors.feature.alerts.DealWatch
import app.priceerrors.feature.alerts.LockedAlertsPreview
import app.priceerrors.feature.alerts.RecentDealAlert
import app.priceerrors.feature.auth.AuthScreen
import app.priceerrors.feature.browse.BrowseScreen
import app.priceerrors.feature.conversion.PostPurchaseSetupScreen
import app.priceerrors.feature.conversion.ReviewMomentScreen
import app.priceerrors.feature.detail.DealDetailScreen
import app.priceerrors.feature.feed.FeedScreen
import app.priceerrors.feature.feed.FeedViewModel
import app.priceerrors.feature.onboarding.OnboardingScreen
import app.priceerrors.feature.onboarding.NotificationPermissionStep
import app.priceerrors.feature.onboarding.PreferencesScreen
import app.priceerrors.feature.paywall.PaywallScreen
import app.priceerrors.feature.paywall.PaywallPlan
import app.priceerrors.feature.paywall.PaywallPricing
import app.priceerrors.feature.paywall.RescueOfferScreen
import app.priceerrors.feature.profile.ProfileAppearance
import app.priceerrors.feature.profile.ProfileFeedLayout
import app.priceerrors.feature.profile.ProfilePalette
import app.priceerrors.feature.profile.ProfileScreen
import app.priceerrors.navigation.MainTab
import app.priceerrors.ui.components.FloatingTabBar
import app.priceerrors.ui.theme.AppearanceOption
import app.priceerrors.ui.theme.FeedLayoutOption
import app.priceerrors.ui.theme.PaletteOption
import app.priceerrors.ui.theme.PriceErrorsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.TimeZone

private enum class AppStage {
    ONBOARDING,
    PREFERENCES,
    NOTIFICATIONS,
    AUTH,
    PAYWALL,
    MAIN,
}

/**
 * Owns the Android presentation flow and the local interaction state shared by
 * every screen. Network-backed auth, billing, and deal mutations can replace
 * these callbacks without changing the feature composables.
 */
@Composable
fun PriceErrorsApp(
    repository: DealRepository,
    googleAuthClient: GoogleCredentialAuthClient,
    supabaseAuthClient: SupabaseAuthClient,
    priceErrorsApi: PriceErrorsApi?,
    billingManager: BillingManager,
    navigationIntentStore: NavigationIntentStore,
    notificationCoordinator: NotificationCoordinator,
    growthAnalytics: GrowthAnalytics,
    feedViewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory(repository)),
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val preferences = remember(context) { PriceErrorsPreferences(context.applicationContext) }
    val notificationTokenStore = remember(context) { NotificationTokenStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    // Null in builds with no backend configured, where the sample feed is used
    // and every mutation stays on-device.
    val networkRepository = repository as? NetworkDealRepository
    val systemDark = isSystemInDarkTheme()
    val uiState by feedViewModel.uiState.collectAsStateWithLifecycle()
    val billingState by billingManager.state.collectAsStateWithLifecycle()
    val feedMetadata by repository.observeMetadata().collectAsStateWithLifecycle(null)
    val authSession by supabaseAuthClient.session.collectAsStateWithLifecycle()
    val pendingDealId by navigationIntentStore.pendingDealId.collectAsStateWithLifecycle()
    val pendingUpgradeOffer by navigationIntentStore.pendingUpgradeOffer.collectAsStateWithLifecycle()
    val pendingGoogleOAuthCallback by
        navigationIntentStore.pendingGoogleOAuthCallback.collectAsStateWithLifecycle()

    val initialStage = remember {
        when {
            !preferences.hasCompletedOnboarding -> AppStage.ONBOARDING
            !preferences.isSignedIn || authSession == null -> AppStage.AUTH
            else -> AppStage.MAIN
        }
    }

    var stageName by rememberSaveable { mutableStateOf(initialStage.name) }
    var selectedTabName by rememberSaveable { mutableStateOf(MainTab.FEED.name) }
    var selectedDealId by rememberSaveable { mutableStateOf<String?>(null) }
    var fetchedDeal by remember { mutableStateOf<app.priceerrors.core.model.Deal?>(null) }
    var showUpgradePaywall by rememberSaveable { mutableStateOf(false) }
    var showPostPurchaseSetup by rememberSaveable { mutableStateOf(false) }
    var showReviewMoment by rememberSaveable { mutableStateOf(false) }
    var showRescueOffer by rememberSaveable { mutableStateOf(false) }
    var hasEnteredLockedPreview by rememberSaveable { mutableStateOf(false) }
    var isFinishingPostPurchase by remember { mutableStateOf(false) }
    var pendingClaimDealId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingClaimSavings by rememberSaveable { mutableStateOf(0.0) }
    var claimLeftApp by rememberSaveable { mutableStateOf(false) }
    var showClaimConfirmation by rememberSaveable { mutableStateOf(false) }
    var isConfirmingClaim by remember { mutableStateOf(false) }
    var celebrationSavings by rememberSaveable { mutableStateOf<Double?>(null) }
    var monthSavings by rememberSaveable { mutableStateOf(0.0) }
    var lifetimeSavings by rememberSaveable { mutableStateOf(0.0) }
    var pendingNotificationPermission by remember {
        mutableStateOf<CompletableDeferred<Unit>?>(null)
    }
    var notificationSyncJob by remember { mutableStateOf<Job?>(null) }

    var displayName by remember { mutableStateOf(preferences.displayName) }
    var email by remember { mutableStateOf(preferences.email) }
    var isPro by remember { mutableStateOf(BuildConfig.DEBUG && preferences.isPro) }
    val hasFullAccess = isPro || BuildConfig.CLOSED_TEST_FREE_ACCESS
    var palette by remember { mutableStateOf(preferences.palette) }
    var appearance by remember { mutableStateOf(preferences.appearance) }
    var feedLayout by remember { mutableStateOf(preferences.feedLayout) }
    var preferredCategories by remember { mutableStateOf(preferences.preferredCategories) }
    var notifyAllDeals by remember { mutableStateOf(preferences.notifyAllDeals) }
    var alertMinimumDiscount by remember { mutableStateOf(preferences.alertMinimumDiscount) }
    var dealWatches by remember { mutableStateOf(preferences.dealWatches) }
    var pushAlertsEnabled by remember {
        mutableStateOf(
            preferences.pushAlertsEnabled &&
                notificationCoordinator.isFirebaseConfigured &&
                (
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
                    ),
        )
    }
    var savedDealIds by remember { mutableStateOf(preferences.savedDealIds) }
    var claimedDealIds by remember { mutableStateOf(preferences.claimedDealIds) }
    var confirmedDealIds by remember { mutableStateOf(preferences.confirmedDealIds) }
    var votesByDealId by remember {
        mutableStateOf(
            preferences.voteEntries.mapNotNull { entry ->
                val pieces = entry.split('|', limit = 2)
                if (pieces.size == 2) {
                    DealVote.fromWire(pieces[1])?.let { pieces[0] to it }
                } else {
                    null
                }
            }.toMap(),
        )
    }

    val recentAlerts = remember(context) {
        runCatching {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return@runCatching emptyList()
            manager.activeNotifications
                .map { status ->
                    val extras = status.notification.extras
                    RecentDealAlert(
                        id = status.key,
                        dealId = dealIdFromNotification(status.key, extras),
                        title = extras.getString(Notification.EXTRA_TITLE).orEmpty(),
                        body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
                        dateMillis = status.postTime,
                    )
                }
                .sortedByDescending { it.dateMillis }
        }.getOrDefault(emptyList())
    }

    val stage = AppStage.valueOf(stageName)
    val selectedTab = MainTab.valueOf(selectedTabName)
    val selectedDeal = selectedDealId?.let { id ->
        uiState.deals.firstOrNull { it.id == id } ?: fetchedDeal?.takeIf { it.id == id }
    }
    val darkTheme = when (appearance) {
        AppearanceOption.SYSTEM -> systemDark
        AppearanceOption.LIGHT -> false
        AppearanceOption.DARK -> true
    }
    val notificationsPermissionGranted =
        pushAlertsEnabled &&
            (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                )
    val view = LocalView.current
    val termsUrl = stringResource(R.string.terms_of_service_url)
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    val accountDeletionUrl = stringResource(R.string.account_deletion_url)
    val paywallPricing = PaywallPricing(
        weekly = billingState.products[BillingPlan.WEEKLY]?.formattedPrice ?: "$4.99",
        monthly = billingState.products[BillingPlan.MONTHLY]?.formattedPrice ?: "$9.99",
        yearly = billingState.products[BillingPlan.YEARLY]?.formattedPrice ?: "$49.99",
    )
    val rescueProduct = billingState.products[BillingPlan.RESCUE]
    val rescueMonthlyPrice = rescueProduct?.introductoryPrice ?: rescueProduct?.formattedPrice ?: "$4.99"
    val rescueRenewalPrice = rescueProduct?.formattedPrice ?: "$9.99"

    fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun openWebPage(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }.onFailure { showMessage("No browser is available to open this page.") }
    }

    fun completeAuthentication(authenticatedName: String, authenticatedEmail: String) {
        displayName = authenticatedName
        email = authenticatedEmail
        preferences.displayName = authenticatedName
        preferences.email = authenticatedEmail
        preferences.isSignedIn = true
        MetaMeasurement.logCompletedRegistration()
        supabaseAuthClient.session.value?.userId?.let { userId ->
            growthAnalytics.identify(userId)
        }
        stageName = if (
            preferences.hasCompletedOnboarding ||
            BuildConfig.CLOSED_TEST_FREE_ACCESS
        ) {
            AppStage.MAIN.name
        } else {
            AppStage.PAYWALL.name
        }
    }

    fun requestPlayReview() {
        val currentActivity = activity
        if (currentActivity == null) {
            openWebPage("https://play.google.com/store/apps/details?id=${context.packageName}")
            showReviewMoment = false
            return
        }
        val reviewManager = ReviewManagerFactory.create(context)
        reviewManager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                reviewManager.launchReviewFlow(currentActivity, request.result)
                    .addOnCompleteListener { showReviewMoment = false }
            } else {
                openWebPage("https://play.google.com/store/apps/details?id=${context.packageName}")
                showReviewMoment = false
            }
        }
    }

    fun syncNotificationPreferences(
        onComplete: (Result<Unit>) -> Unit = {},
    ) {
        val token = notificationTokenStore.pendingToken ?: return
        val api = priceErrorsApi ?: return
        val userId = authSession?.userId ?: return
        notificationSyncJob?.cancel()
        notificationSyncJob = scope.launch {
            delay(150)
            if (!notificationTokenStore.isRegistered(token, userId)) {
                val registration = api.registerAndroidDevice(
                    token = token,
                    bundleId = context.packageName,
                    closedTestAccess = BuildConfig.CLOSED_TEST_FREE_ACCESS,
                )
                if (registration.isFailure) {
                    onComplete(registration)
                    return@launch
                }
                notificationTokenStore.markRegistered(token, userId)
            }
            val result = api.updateDevicePreferences(
                token = token,
                enabled = notificationTokenStore.notificationsEnabled &&
                    notificationCoordinator.areNotificationsEnabled(),
                notifyAllDeals = notifyAllDeals,
                categories = preferredCategories.filter { it != "All" }.toList(),
                minimumDiscount = alertMinimumDiscount,
                timezone = TimeZone.getDefault().id,
                watches = dealWatches,
            )
            onComplete(result)
        }
    }

    fun enableNotifications(onComplete: () -> Unit = {}) {
        notificationCoordinator.setEnabled(true) { result ->
            result.onSuccess {
                pushAlertsEnabled = true
                preferences.pushAlertsEnabled = true
                syncNotificationPreferences { syncResult ->
                    syncResult.onFailure { error ->
                        pushAlertsEnabled = false
                        preferences.pushAlertsEnabled = false
                        val failedToken = notificationTokenStore.pendingToken
                        notificationTokenStore.clearRegistration()
                        notificationCoordinator.setEnabled(false)
                        if (failedToken != null) {
                            scope.launch { priceErrorsApi?.unregisterDevice(failedToken) }
                        }
                        Log.e("PriceErrorsPush", "Backend registration failed", error)
                        PostHogAnalytics.captureException(
                            error,
                            mapOf("flow" to "push_backend_registration"),
                        )
                        showMessage(error.message ?: "Push alerts couldn't be registered.")
                    }
                }
            }.onFailure { error ->
                pushAlertsEnabled = false
                preferences.pushAlertsEnabled = false
                notificationCoordinator.setEnabled(false)
                Log.e("PriceErrorsPush", "FCM activation failed", error)
                PostHogAnalytics.captureException(
                    error,
                    mapOf("flow" to "push_fcm_activation"),
                )
                showMessage(error.message ?: "Push alerts couldn't be enabled.")
            }
            onComplete()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            enableNotifications {
                pendingNotificationPermission?.complete(Unit)
                pendingNotificationPermission = null
            }
        } else {
            pushAlertsEnabled = false
            preferences.pushAlertsEnabled = false
            showMessage("Notification permission was not granted. You can enable it later in Android Settings.")
            pendingNotificationPermission?.complete(Unit)
            pendingNotificationPermission = null
        }
    }

    fun setPushAlerts(enabled: Boolean) {
        if (!enabled) {
            notificationSyncJob?.cancel()
            val token = notificationTokenStore.pendingToken
            val api = priceErrorsApi
            pushAlertsEnabled = false
            preferences.pushAlertsEnabled = false
            scope.launch {
                if (token != null && api != null && authSession != null) {
                    api.updateDevicePreferences(
                        token = token,
                        enabled = false,
                        notifyAllDeals = notifyAllDeals,
                        categories = preferredCategories.filter { it != "All" }.toList(),
                        minimumDiscount = alertMinimumDiscount,
                        timezone = TimeZone.getDefault().id,
                        watches = dealWatches,
                    )
                    api.unregisterDevice(token)
                }
                notificationTokenStore.clearRegistration()
                notificationCoordinator.setEnabled(false)
            }
            return
        }
        if (!notificationCoordinator.isFirebaseConfigured) {
            pushAlertsEnabled = false
            preferences.pushAlertsEnabled = false
            showMessage("Push alerts need the Firebase project values documented in android/README.md.")
            return
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            enableNotifications()
        }
    }

    suspend fun requestOnboardingNotifications() {
        growthAnalytics.track("notification_pre_prompt_enable_tapped")
        if (!notificationCoordinator.isFirebaseConfigured) {
            showMessage("Push alerts need the Firebase project values documented in android/README.md.")
            return
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            val result = CompletableDeferred<Unit>()
            pendingNotificationPermission = result
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            result.await()
        } else {
            val result = CompletableDeferred<Unit>()
            enableNotifications { result.complete(Unit) }
            result.await()
        }
    }

    fun purchase(plan: PaywallPlan) {
        val billingPlan = when (plan) {
            PaywallPlan.WEEKLY -> BillingPlan.WEEKLY
            PaywallPlan.MONTHLY -> BillingPlan.MONTHLY
            PaywallPlan.YEARLY -> BillingPlan.YEARLY
        }
        val planName = plan.name.lowercase()
        growthAnalytics.track("purchase_started", mapOf("plan" to planName))
        MetaMeasurement.logInitiatedCheckout(planName)
        if (activity == null) {
            showMessage("Google Play checkout is unavailable in this window.")
        } else {
            billingManager.purchase(activity, billingPlan)
        }
    }

    fun purchaseRescue() {
        growthAnalytics.track("rescue_offer_purchase_started")
        MetaMeasurement.logInitiatedCheckout("monthly_rescue")
        if (activity == null) {
            showMessage("Google Play checkout is unavailable in this window.")
        } else {
            billingManager.purchase(activity, BillingPlan.RESCUE)
        }
    }

    fun presentUpgradePaywall(source: String) {
        if (BuildConfig.CLOSED_TEST_FREE_ACCESS) {
            growthAnalytics.track(
                "paywall_bypassed",
                mapOf("source" to source, "reason" to "closed_test"),
            )
            return
        }
        growthAnalytics.track("paywall_viewed", mapOf("source" to source))
        MetaMeasurement.logPaywallViewed(source)
        showUpgradePaywall = true
    }

    fun openDeal(dealId: String) {
        if (hasFullAccess) {
            selectedDealId = dealId
        } else {
            presentUpgradePaywall("deal_gate")
        }
    }

    fun refreshSavings() {
        val api = priceErrorsApi ?: return
        scope.launch {
            api.fetchSavingsSummary(YearMonth.now().toString()).onSuccess { summary ->
                monthSavings = summary.monthSavings
                lifetimeSavings = summary.lifetimeSavings
            }
        }
    }

    fun recordClaimOpen(deal: app.priceerrors.core.model.Deal) {
        val localSavings =
            ((deal.originalPriceInCents - deal.priceInCents).coerceAtLeast(0) / 100.0)
        growthAnalytics.track(
            "deal_claim_button_tapped",
            mapOf(
                "deal_id" to deal.id,
                "already_confirmed" to (deal.id in confirmedDealIds).toString(),
            ),
        )
        pendingClaimDealId = deal.id
        pendingClaimSavings = localSavings
        claimLeftApp = false
        showClaimConfirmation = false
        celebrationSavings = null

        val api = priceErrorsApi
        if (api == null) {
            claimedDealIds = claimedDealIds + deal.id
            preferences.claimedDealIds = claimedDealIds
            return
        }
        scope.launch {
            api.claimDeal(deal.id)
                .onSuccess { response ->
                    claimedDealIds = claimedDealIds + deal.id
                    preferences.claimedDealIds = claimedDealIds
                    pendingClaimSavings = response.potentialSavings ?: localSavings
                    if (response.status == "confirmed") {
                        confirmedDealIds = confirmedDealIds + deal.id
                        preferences.confirmedDealIds = confirmedDealIds
                        pendingClaimDealId = null
                        claimLeftApp = false
                        showClaimConfirmation = false
                    }
                    growthAnalytics.track(
                        "deal_claim_open_recorded",
                        mapOf(
                            "deal_id" to deal.id,
                            "trackable" to response.trackable.toString(),
                            "claim_status" to (response.status ?: "unknown"),
                            "potential_savings" to
                                (response.potentialSavings ?: localSavings).toString(),
                        ),
                    )
                }
                .onFailure { error ->
                    pendingClaimDealId = null
                    growthAnalytics.track(
                        "deal_claim_open_failed",
                        mapOf("deal_id" to deal.id, "error" to error::class.simpleName.orEmpty()),
                    )
                    when (error) {
                        is ApiError.LimitReached -> {
                            if (BuildConfig.CLOSED_TEST_FREE_ACCESS) {
                                claimedDealIds = claimedDealIds + deal.id
                                preferences.claimedDealIds = claimedDealIds
                            } else {
                                presentUpgradePaywall("claim_limit")
                            }
                        }
                        else -> showMessage(error.message ?: "Couldn't record this deal.")
                    }
                }
        }
    }

    fun declinePendingClaim() {
        showClaimConfirmation = false
        pendingClaimDealId = null
        pendingClaimSavings = 0.0
        claimLeftApp = false
        growthAnalytics.track("deal_claim_not_yet")
    }

    fun confirmPendingClaim() {
        val dealId = pendingClaimDealId ?: return
        val api = priceErrorsApi
        growthAnalytics.track(
            "deal_claim_confirm_button_tapped",
            mapOf("deal_id" to dealId, "potential_savings" to pendingClaimSavings.toString()),
        )
        if (api == null) {
            showClaimConfirmation = false
            confirmedDealIds = confirmedDealIds + dealId
            preferences.confirmedDealIds = confirmedDealIds
            celebrationSavings = pendingClaimSavings
            pendingClaimDealId = null
            return
        }

        isConfirmingClaim = true
        scope.launch {
            api.confirmDealClaim(dealId)
                .onSuccess { response ->
                    confirmedDealIds = confirmedDealIds + dealId
                    preferences.confirmedDealIds = confirmedDealIds
                    monthSavings = response.monthSavings
                    lifetimeSavings = response.lifetimeSavings
                    celebrationSavings = response.potentialSavings ?: pendingClaimSavings
                    showClaimConfirmation = false
                    pendingClaimDealId = null
                    claimLeftApp = false
                    growthAnalytics.track(
                        "deal_claim_confirmed",
                        mapOf(
                            "deal_id" to dealId,
                            "savings_amount" to (response.potentialSavings ?: pendingClaimSavings).toString(),
                            "month_savings" to response.monthSavings.toString(),
                            "lifetime_savings" to response.lifetimeSavings.toString(),
                        ),
                    )
                    if (preferences.reviewPromptedVersion != BuildConfig.VERSION_NAME) {
                        preferences.reviewPromptedVersion = BuildConfig.VERSION_NAME
                        showReviewMoment = true
                        growthAnalytics.track(
                            "review_moment_shown",
                            mapOf("trigger" to "confirmed_deal"),
                        )
                    }
                }
                .onFailure { error ->
                    growthAnalytics.track(
                        "deal_claim_confirmation_failed",
                        mapOf("deal_id" to dealId, "error" to error::class.simpleName.orEmpty()),
                    )
                    showMessage(error.message ?: "Couldn't confirm this deal. Try again.")
                }
            isConfirmingClaim = false
        }
    }

    fun openDealById(dealId: String, onResolved: ((Boolean) -> Unit)? = null) {
        selectedTabName = MainTab.FEED.name
        if (uiState.deals.any { it.id == dealId }) {
            openDeal(dealId)
            onResolved?.invoke(true)
            return
        }
        scope.launch {
            repository.fetchDeal(dealId).fold(
                onSuccess = { deal ->
                    // The single-deal endpoint is pro-gated server-side.
                    isPro = true
                    preferences.isPro = true
                    repository.upsertDeal(deal)
                    fetchedDeal = deal
                    selectedDealId = deal.id
                    onResolved?.invoke(true)
                },
                onFailure = { error ->
                    when (error) {
                        is ApiError.ProRequired -> {
                            if (BuildConfig.CLOSED_TEST_FREE_ACCESS) {
                                feedViewModel.refresh(forceFull = true)
                                showMessage("This deal is no longer available in the current feed.")
                                onResolved?.invoke(false)
                            } else {
                                presentUpgradePaywall("deep_link")
                                onResolved?.invoke(true)
                            }
                        }
                        else -> {
                            feedViewModel.refresh(forceFull = true)
                            onResolved?.invoke(false)
                        }
                    }
                },
            )
        }
    }

    fun handleSubscriptionActivated(plan: String = "unknown") {
        showUpgradePaywall = false
        showRescueOffer = false
        hasEnteredLockedPreview = false
        growthAnalytics.track("subscription_activated", mapOf("plan" to plan))
        if (!preferences.postPurchaseSetupCompleted) {
            showPostPurchaseSetup = true
        }
        feedViewModel.refresh(forceFull = true)
    }

    fun dismissUpgradePaywall() {
        showUpgradePaywall = false
        hasEnteredLockedPreview = true
        preferences.hasCompletedOnboarding = true
        growthAnalytics.track("paywall_dismissed", mapOf("count" to "1"))
        scope.launch {
            delay(7000)
            if (!hasFullAccess && billingState.rescueAvailable) {
                showRescueOffer = true
            }
        }
    }

    fun dismissOnboardingPaywall() {
        preferences.hasCompletedOnboarding = true
        hasEnteredLockedPreview = true
        stageName = AppStage.MAIN.name
        growthAnalytics.track("paywall_closed", mapOf("source" to "onboarding"))
        scope.launch {
            delay(7000)
            if (!hasFullAccess && billingState.rescueAvailable) {
                showRescueOffer = true
            }
        }
    }

    fun completeDebugPaywall() {
        if (!BuildConfig.DEBUG) return
        isPro = true
        preferences.isPro = true
        preferences.hasCompletedOnboarding = true
        showUpgradePaywall = false
        showRescueOffer = false
        stageName = AppStage.MAIN.name
        handleSubscriptionActivated()
    }

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (pendingClaimDealId != null) claimLeftApp = true
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (pendingClaimDealId != null && claimLeftApp && !showClaimConfirmation) {
            showClaimConfirmation = true
            growthAnalytics.track(
                "deal_claim_confirmation_shown",
                mapOf(
                    "deal_id" to pendingClaimDealId.orEmpty(),
                    "potential_savings" to pendingClaimSavings.toString(),
                ),
            )
        }
        notificationTokenStore.pendingToken?.let { token ->
            if (authSession != null && pushAlertsEnabled) {
                priceErrorsApi?.let { api ->
                    scope.launch { api.markDeviceOpened(token) }
                }
                syncNotificationPreferences()
            }
        }
    }

    LaunchedEffect(selectedDealId) {
        if (selectedDealId == null) fetchedDeal = null
        val dealId = selectedDealId ?: return@LaunchedEffect
        val api = priceErrorsApi ?: return@LaunchedEffect
        api.fetchClaim(dealId).onSuccess { response ->
            if (response.status == "confirmed") {
                confirmedDealIds = confirmedDealIds + dealId
                preferences.confirmedDealIds = confirmedDealIds
            }
        }
    }

    LaunchedEffect(billingState.isPro, feedMetadata?.access?.isPro) {
        val serverPro = feedMetadata?.access?.isPro == true
        val next = billingState.isPro || serverPro || (BuildConfig.DEBUG && preferences.isPro)
        val nextHasFullAccess = next || BuildConfig.CLOSED_TEST_FREE_ACCESS
        if (next != isPro) {
            isPro = next
            if (next) preferences.isPro = true
        }
        if (nextHasFullAccess) {
            preferences.hasCompletedOnboarding = true
            showUpgradePaywall = false
            showRescueOffer = false
            if (stageName == AppStage.PAYWALL.name) stageName = AppStage.MAIN.name
        } else if (
            stageName == AppStage.MAIN.name &&
            feedMetadata != null &&
            !hasEnteredLockedPreview
        ) {
            // Match iOS: first non-Pro feed snapshot surfaces the paywall once.
            presentUpgradePaywall("feed_lock")
        }
    }

    // Keep the launcher long-press menu in step with the entitlement.
    LaunchedEffect(hasFullAccess) {
        PriceErrorsShortcuts.refresh(context, hasFullAccess)
    }

    LaunchedEffect(Unit) {
        billingManager.onSubscriptionPurchased = { plan, price, currency, isTrial ->
            MetaMeasurement.logSubscribe(price, currency, isTrial)
            billingManager.clearMessage()
            handleSubscriptionActivated(plan.name.lowercase())
        }
    }

    LaunchedEffect(showRescueOffer) {
        if (showRescueOffer && !showUpgradePaywall && !hasFullAccess) {
            growthAnalytics.track("rescue_offer_viewed")
            MetaMeasurement.logPaywallViewed("rescue")
        }
    }

    LaunchedEffect(showUpgradePaywall) {
        if (showUpgradePaywall) {
            growthAnalytics.track(
                "paywall_screen_viewed",
                mapOf("default_plan" to PaywallPlan.YEARLY.name.lowercase()),
            )
        }
    }

    LaunchedEffect(pendingUpgradeOffer, stageName, hasFullAccess) {
        if (!pendingUpgradeOffer) return@LaunchedEffect
        // Onboarding and the hard paywall already lead somewhere better, so the
        // shortcut only interrupts once the user is in the main shell.
        if (stageName != AppStage.MAIN.name) return@LaunchedEffect
        if (!hasFullAccess) presentUpgradePaywall("shortcut")
        navigationIntentStore.consumeUpgradeOffer()
    }

    LaunchedEffect(stageName) {
        when (stageName) {
            AppStage.MAIN.name -> growthAnalytics.track("app_opened")
            AppStage.NOTIFICATIONS.name ->
                growthAnalytics.track("notification_pre_prompt_viewed")
            else -> Unit
        }
    }

    LaunchedEffect(selectedTabName, stageName) {
        if (stageName == AppStage.MAIN.name && selectedTabName == MainTab.PROFILE.name) {
            refreshSavings()
        }
    }

    LaunchedEffect(authSession?.userId, pushAlertsEnabled) {
        authSession?.userId?.let { userId ->
            growthAnalytics.identify(userId)
            if (pushAlertsEnabled) enableNotifications()
        }
    }

    LaunchedEffect(pendingGoogleOAuthCallback) {
        val callbackUrl = pendingGoogleOAuthCallback ?: return@LaunchedEffect
        supabaseAuthClient.completeGoogleOAuth(callbackUrl)
            .onSuccess { identity ->
                completeAuthentication(identity.displayName, identity.email)
            }
            .onFailure { error ->
                PostHogAnalytics.captureException(
                    error,
                    mapOf("flow" to "google_oauth_callback"),
                )
                showMessage(error.message ?: "Google sign-in couldn't be completed. Try again.")
            }
        navigationIntentStore.consumeGoogleOAuth(callbackUrl)
    }

    LaunchedEffect(pendingDealId, stageName) {
        val requestedId = pendingDealId ?: return@LaunchedEffect
        if (stageName != AppStage.MAIN.name) return@LaunchedEffect
        openDealById(requestedId) { resolved ->
            if (resolved) navigationIntentStore.consumeDeal(requestedId)
        }
    }

    BackHandler(enabled = selectedDealId != null) {
        selectedDealId = null
    }
    BackHandler(
        enabled = stage == AppStage.MAIN && selectedDealId == null &&
            !showUpgradePaywall && !showRescueOffer && !showPostPurchaseSetup &&
            !showReviewMoment && selectedTab != MainTab.FEED,
    ) {
        selectedTabName = MainTab.FEED.name
    }
    BackHandler(enabled = showUpgradePaywall) {
        dismissUpgradePaywall()
    }
    BackHandler(enabled = showRescueOffer && !showUpgradePaywall) {
        showRescueOffer = false
    }
    BackHandler(enabled = showReviewMoment) {
        showReviewMoment = false
    }

    PriceErrorsTheme(darkTheme = darkTheme, palette = palette) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (stage) {
                AppStage.ONBOARDING -> OnboardingScreen(
                    onContinue = { stageName = AppStage.PREFERENCES.name },
                    totalSteps = 6,
                    onStepCompleted = { step ->
                        growthAnalytics.track(
                            "onboarding_step_completed",
                            mapOf("step" to step.toString()),
                        )
                    },
                )

                AppStage.PREFERENCES -> PreferencesScreen(
                    onContinue = { categories ->
                        val allSelected = "All" in categories
                        preferredCategories = categories - "All"
                        preferences.preferredCategories = preferredCategories
                        if (allSelected) {
                            notifyAllDeals = true
                            preferences.notifyAllDeals = true
                        }
                        stageName = AppStage.NOTIFICATIONS.name
                    },
                    onBack = { stageName = AppStage.ONBOARDING.name },
                    totalSteps = 6,
                    currentStep = 2,
                )

                AppStage.NOTIFICATIONS -> NotificationPermissionStep(
                    onContinue = { stageName = AppStage.AUTH.name },
                    onEnableAlerts = ::requestOnboardingNotifications,
                    onBack = { stageName = AppStage.PREFERENCES.name },
                    totalSteps = 6,
                    currentStep = 3,
                )

                AppStage.AUTH -> AuthScreen(
                    onAuthenticated = ::completeAuthentication,
                    showOnboardingProgress = !preferences.hasCompletedOnboarding,
                    onboardingTotalSteps = 6,
                    onboardingCurrentStep = 4,
                    onBack = if (preferences.hasCompletedOnboarding) {
                        null
                    } else {
                        { stageName = AppStage.NOTIFICATIONS.name }
                    },
                    // Only reached in builds with no backend, where the sample
                    // feed stands in for the server. A configured build always
                    // authenticates against Supabase via the callbacks below.
                    allowLocalEmailAuth = BuildConfig.DEBUG,
                    // With a backend configured the Google token is exchanged
                    // for a real Supabase session below, so sign-in can
                    // complete. Without one there is nothing to verify against.
                    canCompleteGoogleSignIn = ApiConfig.isConfigured || BuildConfig.DEBUG,
                    // Email/password against the same Supabase project as iOS,
                    // so one account works on both platforms.
                    onEmailSignIn = if (ApiConfig.isConfigured) {
                        { email, password -> supabaseAuthClient.signInWithEmail(email, password) }
                    } else {
                        null
                    },
                    onEmailSignUp = if (ApiConfig.isConfigured) {
                        { name, email, password ->
                            supabaseAuthClient.signUpWithEmail(email, password, name).map { result ->
                                when (result) {
                                    is EmailSignUpResult.SignedIn -> result.identity
                                    // Null tells the screen to ask the user to
                                    // confirm their address instead of entering.
                                    EmailSignUpResult.ConfirmationRequired -> null
                                }
                            }
                        }
                    } else {
                        null
                    },
                    onGoogleAuthenticate = {
                        val currentActivity = activity
                        if (currentActivity == null) {
                            Result.failure(IllegalStateException("Google sign-in is unavailable."))
                        } else {
                            runCatching {
                                // The temporary closed-test build uses the
                                // verified browser flow directly, avoiding a
                                // redundant native picker before Google's
                                // Play-certificate rejection triggers fallback.
                                if (
                                    BuildConfig.CLOSED_TEST_FREE_ACCESS &&
                                    ApiConfig.isConfigured
                                ) {
                                    val authorizationUrl =
                                        supabaseAuthClient.beginGoogleOAuth().getOrThrow()
                                    currentActivity.startActivity(
                                        Intent(Intent.ACTION_VIEW, authorizationUrl.toUri()),
                                    )
                                    throw GoogleOAuthPending()
                                }

                                val identity = googleAuthClient.signIn(currentActivity)
                                    .getOrElse { error ->
                                        if (
                                            error is GooglePlayRegistrationException &&
                                            ApiConfig.isConfigured
                                        ) {
                                            val authorizationUrl =
                                                supabaseAuthClient.beginGoogleOAuth().getOrThrow()
                                            currentActivity.startActivity(
                                                Intent(Intent.ACTION_VIEW, authorizationUrl.toUri()),
                                            )
                                            throw GoogleOAuthPending()
                                        }
                                        throw error
                                    }

                                // The Google ID token only becomes an authorised
                                // session once Supabase accepts it; every server
                                // route authenticates on that JWT alone.
                                if (ApiConfig.isConfigured) {
                                    supabaseAuthClient.signInWithGoogle(
                                        googleIdToken = identity.idToken,
                                        nonce = identity.nonce,
                                    ).getOrElse { error ->
                                        throw when (error) {
                                            is ApiError.Unauthorized -> IllegalStateException(
                                                "Google signed in, but Supabase rejected the token. " +
                                                    "In Supabase → Authentication → Providers → Google, " +
                                                    "list the Web client ID (same as " +
                                                    "PRICEERRORS_GOOGLE_WEB_CLIENT_ID) first, then any " +
                                                    "Android client IDs, comma-separated.",
                                                error,
                                            )
                                            else -> error
                                        }
                                    }
                                }
                                identity
                            }
                        }
                    },
                )

                AppStage.PAYWALL -> PaywallScreen(
                    allowDismiss = true,
                    onDismiss = ::dismissOnboardingPaywall,
                    onPurchase = ::purchase,
                    onRestore = { billingManager.restorePurchases() },
                    pricing = paywallPricing,
                    isLoading = billingState.isLoading,
                    message = billingState.message,
                    allowDebugBypass = BuildConfig.DEBUG,
                    onDebugBypass = ::completeDebugPaywall,
                    onOpenTerms = { openWebPage(termsUrl) },
                    onOpenPrivacy = { openWebPage(privacyUrl) },
                    analyticsSource = "onboarding",
                    onboardingStep = 5,
                    onboardingTotalSteps = 6,
                    onPlanSelected = { plan ->
                        growthAnalytics.track(
                            "paywall_plan_selected",
                            mapOf("plan" to plan.name.lowercase()),
                        )
                    },
                )

                AppStage.MAIN -> Box(modifier = Modifier.fillMaxSize()) {
                    if (selectedDeal != null) {
                        DealDetailScreen(
                            deal = selectedDeal,
                            isSaved = selectedDeal.id in savedDealIds,
                            isClaimed = selectedDeal.id in confirmedDealIds,
                            selectedVote = votesByDealId[selectedDeal.id] ?: selectedDeal.userVote,
                            growthAnalytics = growthAnalytics,
                            showClaimConfirmation =
                                showClaimConfirmation && pendingClaimDealId == selectedDeal.id,
                            claimSavings = pendingClaimSavings,
                            isConfirmingClaim = isConfirmingClaim,
                            onConfirmClaim = ::confirmPendingClaim,
                            onDeclineClaim = ::declinePendingClaim,
                            celebrationSavings = celebrationSavings,
                            onCelebrationFinished = { celebrationSavings = null },
                            onBack = { selectedDealId = null },
                            onSave = {
                                savedDealIds = if (selectedDeal.id in savedDealIds) {
                                    savedDealIds - selectedDeal.id
                                } else {
                                    savedDealIds + selectedDeal.id
                                }
                                preferences.savedDealIds = savedDealIds
                            },
                            onVote = { vote ->
                                val previousVotes = votesByDealId
                                votesByDealId = votesByDealId + (selectedDeal.id to vote)
                                preferences.voteEntries = votesByDealId.map { (id, value) ->
                                    "$id|${value.wireValue}"
                                }.toSet()

                                networkRepository?.let { network ->
                                    scope.launch {
                                        network.vote(selectedDeal.id, vote).onFailure {
                                            // Roll back so the UI never shows a
                                            // vote the server did not record.
                                            votesByDealId = previousVotes
                                            preferences.voteEntries = previousVotes
                                                .map { (id, value) -> "$id|${value.wireValue}" }
                                                .toSet()
                                        }
                                    }
                                }
                            },
                            onClaim = { recordClaimOpen(selectedDeal) },
                        )
                    } else {
                        when (selectedTab) {
                            MainTab.FEED -> FeedScreen(
                                uiState = uiState,
                                layout = feedLayout,
                                isLocked = !hasFullAccess,
                                onUpgrade = { presentUpgradePaywall("feed") },
                                onDealSelected = ::openDeal,
                                onRetry = { feedViewModel.refresh(forceFull = true) },
                                lastRefreshed = uiState.lastRefreshed,
                                showScrollHint = !preferences.scrollHintDismissed,
                                onScrollHintDismissed = {
                                    preferences.scrollHintDismissed = true
                                },
                                onShareDeal = { deal ->
                                    DealSharing.presentShareSheet(
                                        context = context,
                                        deal = deal,
                                        source = "feed",
                                        analytics = growthAnalytics,
                                    )
                                },
                                onCopyDealLink = { deal ->
                                    DealSharing.copyLink(
                                        context = context,
                                        deal = deal,
                                        source = "feed",
                                        analytics = growthAnalytics,
                                    )
                                    showMessage("Link copied")
                                },
                            )

                            MainTab.ALERTS -> if (hasFullAccess) {
                                AlertsScreen(
                                    notifyAllDeals = notifyAllDeals,
                                    notificationsPermissionGranted = notificationsPermissionGranted,
                                    onRequestNotificationPermission = { setPushAlerts(true) },
                                    onNotifyAllDealsChanged = { enabled ->
                                        notifyAllDeals = enabled
                                        preferences.notifyAllDeals = enabled
                                        syncNotificationPreferences()
                                    },
                                    watches = dealWatches,
                                    onAddWatch = { watch ->
                                        dealWatches = dealWatches + watch
                                        preferences.dealWatches = dealWatches
                                        syncNotificationPreferences()
                                    },
                                    onAddWatches = { watches ->
                                        dealWatches = dealWatches + watches
                                        preferences.dealWatches = dealWatches
                                        syncNotificationPreferences()
                                    },
                                    onRemoveWatch = { id ->
                                        dealWatches = dealWatches.filterNot { it.id == id }
                                        preferences.dealWatches = dealWatches
                                        syncNotificationPreferences()
                                    },
                                    preferredCategories = preferredCategories,
                                    onPreferredCategoriesChanged = { categories ->
                                        preferredCategories = categories
                                        preferences.preferredCategories = categories
                                        syncNotificationPreferences()
                                    },
                                    alertMinimumDiscount = alertMinimumDiscount,
                                    onAlertMinimumDiscountChanged = { value ->
                                        alertMinimumDiscount = value
                                        preferences.alertMinimumDiscount = value
                                        syncNotificationPreferences()
                                    },
                                    recentAlerts = recentAlerts,
                                    onRecentAlertSelected = { dealId -> openDealById(dealId) },
                                )
                            } else {
                                LockedAlertsPreview(onUpgrade = { presentUpgradePaywall("alerts") })
                            }

                            MainTab.BROWSE -> BrowseScreen(
                                deals = uiState.deals,
                                isLocked = !hasFullAccess,
                                onUpgrade = { presentUpgradePaywall("feed") },
                                onDealSelected = ::openDeal,
                            )

                            MainTab.PROFILE -> ProfileScreen(
                                name = displayName,
                                email = email,
                                isPro = hasFullAccess,
                                dayStreak = 0,
                                savedDeals = uiState.deals.filter { it.id in savedDealIds },
                                myPosts = emptyList(),
                                monthSavings = monthSavings,
                                lifetimeSavings = lifetimeSavings,
                                pushAlertsEnabled = pushAlertsEnabled,
                                selectedPalette = ProfilePalette.valueOf(palette.name),
                                selectedFeedLayout = ProfileFeedLayout.valueOf(feedLayout.name),
                                selectedAppearance = ProfileAppearance.valueOf(appearance.name),
                                showNavigation = false,
                                onEditName = { updatedName ->
                                    displayName = updatedName
                                    preferences.displayName = updatedName
                                },
                                onManageSubscription = {
                                    openWebPage(
                                        "https://play.google.com/store/account/subscriptions?package=${context.packageName}",
                                    )
                                },
                                onUpgrade = { presentUpgradePaywall("feed") },
                                onDealSelected = ::openDeal,
                                onOpenAlertSettings = {
                                    selectedTabName = MainTab.ALERTS.name
                                },
                                onPushAlertsChanged = ::setPushAlerts,
                                onPaletteSelected = { selected ->
                                    palette = PaletteOption.valueOf(selected.name)
                                    preferences.palette = palette
                                },
                                onFeedLayoutSelected = { selected ->
                                    feedLayout = FeedLayoutOption.valueOf(selected.name)
                                    preferences.feedLayout = feedLayout
                                },
                                onAppearanceSelected = { selected ->
                                    appearance = AppearanceOption.valueOf(selected.name)
                                    preferences.appearance = appearance
                                },
                                onSignOut = {
                                    val signingOutUserId = authSession?.userId
                                    val registeredToken = notificationTokenStore.pendingToken
                                    preferences.isSignedIn = false
                                    preferences.pushAlertsEnabled = false
                                    pushAlertsEnabled = false
                                    notificationSyncJob?.cancel()
                                    notificationTokenStore.clearRegistration()
                                    notificationCoordinator.setEnabled(false)
                                    isPro = BuildConfig.DEBUG && preferences.isPro
                                    hasEnteredLockedPreview = false
                                    showUpgradePaywall = false
                                    showRescueOffer = false
                                    showPostPurchaseSetup = false
                                    showReviewMoment = false
                                    showClaimConfirmation = false
                                    pendingClaimDealId = null
                                    celebrationSavings = null
                                    selectedDealId = null
                                    selectedTabName = MainTab.FEED.name
                                    stageName = AppStage.AUTH.name
                                    growthAnalytics.reset()
                                    // Unregister while the old JWT is still
                                    // available. The user-id guard prevents a
                                    // fast re-login from being cleared by this
                                    // older sign-out task.
                                    scope.launch {
                                        if (registeredToken != null) {
                                            priceErrorsApi?.unregisterDevice(registeredToken)
                                        }
                                        if (
                                            signingOutUserId == null ||
                                            supabaseAuthClient.session.value?.userId == signingOutUserId
                                        ) {
                                            supabaseAuthClient.clearSession()
                                        }
                                    }
                                    // Drop deals fetched for the old account.
                                    networkRepository?.clear()
                                    activity?.let { currentActivity ->
                                        scope.launch { googleAuthClient.clear(currentActivity) }
                                    }
                                },
                                onDeleteAccount = {
                                    // The server identifies the account from the
                                    // session. Preserve all local state unless
                                    // deletion is confirmed so a network failure
                                    // cannot strand a still-live account.
                                    scope.launch {
                                        val deletion = priceErrorsApi?.deleteAccount()
                                            ?: Result.failure(ApiError.NotConfigured)
                                        deletion.onSuccess {
                                            notificationSyncJob?.cancel()
                                            notificationTokenStore.clearRegistration()
                                            notificationCoordinator.setEnabled(false)
                                            pushAlertsEnabled = false
                                            supabaseAuthClient.clearSession()
                                            networkRepository?.clear()
                                            preferences.resetAccount()
                                            displayName = preferences.displayName
                                            email = preferences.email
                                            isPro = false
                                            hasEnteredLockedPreview = false
                                            savedDealIds = emptySet()
                                            claimedDealIds = emptySet()
                                            confirmedDealIds = emptySet()
                                            votesByDealId = emptyMap()
                                            preferredCategories = emptySet()
                                            notifyAllDeals = preferences.notifyAllDeals
                                            alertMinimumDiscount = preferences.alertMinimumDiscount
                                            dealWatches = preferences.dealWatches
                                            showUpgradePaywall = false
                                            showPostPurchaseSetup = false
                                            showReviewMoment = false
                                            showRescueOffer = false
                                            showClaimConfirmation = false
                                            pendingClaimDealId = null
                                            celebrationSavings = null
                                            monthSavings = 0.0
                                            lifetimeSavings = 0.0
                                            selectedDealId = null
                                            selectedTabName = MainTab.FEED.name
                                            stageName = AppStage.ONBOARDING.name
                                        }.onFailure { error ->
                                            PostHogAnalytics.captureException(
                                                error,
                                                mapOf("flow" to "account_deletion"),
                                            )
                                            showMessage(
                                                "We couldn't confirm account deletion. Please try again or contact support.",
                                            )
                                        }
                                    }
                                },
                                onOpenSupport = {
                                    openWebPage("mailto:priceerrorsapp@gmail.com")
                                },
                                onOpenPrivacy = { openWebPage(privacyUrl) },
                                onOpenTerms = { openWebPage(termsUrl) },
                                onOpenAccountDeletion = { openWebPage(accountDeletionUrl) },
                                onOpenDiscord = { openWebPage(AppLinks.DISCORD) },
                            )
                        }

                        FloatingTabBar(
                            selectedTab = selectedTab,
                            onTabSelected = { tab -> selectedTabName = tab.name },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }

                    if (showUpgradePaywall) {
                        PaywallScreen(
                            allowDismiss = true,
                            onDismiss = { dismissUpgradePaywall() },
                            onPurchase = ::purchase,
                            onRestore = { billingManager.restorePurchases() },
                            pricing = paywallPricing,
                            isLoading = billingState.isLoading,
                            message = billingState.message,
                            allowDebugBypass = BuildConfig.DEBUG,
                            onDebugBypass = ::completeDebugPaywall,
                            onOpenTerms = { openWebPage(termsUrl) },
                            onOpenPrivacy = { openWebPage(privacyUrl) },
                            analyticsSource = "paywall",
                            onPlanSelected = { plan ->
                                growthAnalytics.track(
                                    "paywall_plan_selected",
                                    mapOf("plan" to plan.name.lowercase()),
                                )
                            },
                        )
                    }

                    if (showRescueOffer && !showUpgradePaywall && !hasFullAccess) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            RescueOfferScreen(
                                monthlyPrice = rescueMonthlyPrice,
                                renewalPrice = rescueRenewalPrice,
                                usesIntroductoryPrice = rescueProduct?.introductoryPrice != null,
                                isLoading = billingState.isLoading,
                                message = billingState.message,
                                onDismiss = { showRescueOffer = false },
                                onPurchase = ::purchaseRescue,
                            )
                        }
                    }

                    if (showPostPurchaseSetup && isPro) {
                        PostPurchaseSetupScreen(
                            initialCategories = preferredCategories,
                            initialMinimumDiscount = alertMinimumDiscount,
                            isFinishing = isFinishingPostPurchase,
                            onFinishWatches = { categories, watches, minimumDiscount ->
                                isFinishingPostPurchase = true
                                preferredCategories = categories
                                preferences.preferredCategories = categories
                                alertMinimumDiscount = minimumDiscount
                                preferences.alertMinimumDiscount = minimumDiscount
                                dealWatches = dealWatches + watches
                                preferences.dealWatches = dealWatches
                                preferences.postPurchaseSetupCompleted = true
                                setPushAlerts(true)
                                syncNotificationPreferences()
                                growthAnalytics.track(
                                    "post_purchase_setup_completed",
                                    mapOf(
                                        "category_count" to categories.size.toString(),
                                        "watch_count" to watches.size.toString(),
                                    ),
                                )
                                isFinishingPostPurchase = false
                                showPostPurchaseSetup = false
                                selectedTabName = MainTab.ALERTS.name
                            },
                        )
                    }

                    if (showReviewMoment) {
                        ReviewMomentScreen(
                            onRate = ::requestPlayReview,
                            onDismiss = { showReviewMoment = false },
                        )
                    }
                }
            }
        }
    }
}

private fun dealIdFromNotification(key: String, extras: Bundle): String? {
    extras.getString("dealId")?.let { return it }
    extras.getString("deal_id")?.let { return it }
    if (key.startsWith("deal-")) return key.removePrefix("deal-")
    return null
}
