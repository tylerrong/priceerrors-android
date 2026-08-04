package app.priceerrors

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.priceerrors.core.data.DealRepository
import app.priceerrors.core.data.NetworkDealRepository
import app.priceerrors.core.auth.GoogleCredentialAuthClient
import app.priceerrors.core.auth.SupabaseAuthClient
import app.priceerrors.core.network.ApiConfig
import app.priceerrors.core.network.PriceErrorsApi
import app.priceerrors.core.billing.BillingManager
import app.priceerrors.core.billing.BillingPlan
import app.priceerrors.core.navigation.NavigationIntentStore
import app.priceerrors.core.shortcuts.PriceErrorsShortcuts
import app.priceerrors.core.notifications.NotificationCoordinator
import app.priceerrors.core.model.DealVote
import app.priceerrors.feature.auth.AuthScreen
import app.priceerrors.feature.browse.BrowseScreen
import app.priceerrors.feature.community.CommunityScreen
import app.priceerrors.feature.detail.DealDetailScreen
import app.priceerrors.feature.feed.FeedScreen
import app.priceerrors.feature.feed.FeedViewModel
import app.priceerrors.feature.onboarding.OnboardingScreen
import app.priceerrors.feature.onboarding.PreferencesScreen
import app.priceerrors.feature.paywall.PaywallScreen
import app.priceerrors.feature.paywall.PaywallPlan
import app.priceerrors.feature.paywall.PaywallPricing
import app.priceerrors.feature.profile.ProfileAppearance
import app.priceerrors.feature.profile.ProfileFeedLayout
import app.priceerrors.feature.profile.ProfilePalette
import app.priceerrors.feature.profile.ProfilePost
import app.priceerrors.feature.profile.ProfileScreen
import app.priceerrors.navigation.MainTab
import app.priceerrors.ui.components.FloatingTabBar
import app.priceerrors.ui.theme.AppearanceOption
import app.priceerrors.ui.theme.FeedLayoutOption
import app.priceerrors.ui.theme.PaletteOption
import app.priceerrors.ui.theme.PriceErrorsTheme
import kotlinx.coroutines.launch

private enum class AppStage {
    ONBOARDING,
    PREFERENCES,
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
    feedViewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory(repository)),
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val preferences = remember(context) { PriceErrorsPreferences(context.applicationContext) }
    val scope = rememberCoroutineScope()
    // Null in builds with no backend configured, where the sample feed is used
    // and every mutation stays on-device.
    val networkRepository = repository as? NetworkDealRepository
    val systemDark = isSystemInDarkTheme()
    val uiState by feedViewModel.uiState.collectAsStateWithLifecycle()
    val billingState by billingManager.state.collectAsStateWithLifecycle()
    val pendingDealId by navigationIntentStore.pendingDealId.collectAsStateWithLifecycle()
    val pendingTrialOffer by navigationIntentStore.pendingTrialOffer.collectAsStateWithLifecycle()

    val initialStage = remember {
        when {
            !preferences.hasCompletedOnboarding -> AppStage.ONBOARDING
            !preferences.isSignedIn -> AppStage.AUTH
            else -> AppStage.MAIN
        }
    }

    var stageName by rememberSaveable { mutableStateOf(initialStage.name) }
    var selectedTabName by rememberSaveable { mutableStateOf(MainTab.FEED.name) }
    var selectedDealId by rememberSaveable { mutableStateOf<String?>(null) }
    var showUpgradePaywall by rememberSaveable { mutableStateOf(false) }
    var communityHasNestedScreen by rememberSaveable { mutableStateOf(false) }

    var displayName by remember { mutableStateOf(preferences.displayName) }
    var email by remember { mutableStateOf(preferences.email) }
    var isPro by remember { mutableStateOf(BuildConfig.DEBUG && preferences.isPro) }
    var palette by remember { mutableStateOf(preferences.palette) }
    var appearance by remember { mutableStateOf(preferences.appearance) }
    var feedLayout by remember { mutableStateOf(preferences.feedLayout) }
    var preferredCategories by remember { mutableStateOf(preferences.preferredCategories) }
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

    val stage = AppStage.valueOf(stageName)
    val selectedTab = MainTab.valueOf(selectedTabName)
    val selectedDeal = selectedDealId?.let { id -> uiState.deals.firstOrNull { it.id == id } }
    val personalizedFeedState = remember(uiState, preferredCategories) {
        if (preferredCategories.isEmpty() || "All" in preferredCategories) {
            uiState
        } else {
            uiState.copy(
                deals = uiState.deals.sortedBy { deal ->
                    if (preferredCategories.any { it.equals(deal.category, ignoreCase = true) }) 0 else 1
                },
            )
        }
    }
    val darkTheme = when (appearance) {
        AppearanceOption.SYSTEM -> systemDark
        AppearanceOption.LIGHT -> false
        AppearanceOption.DARK -> true
    }
    val view = LocalView.current
    val termsUrl = stringResource(R.string.terms_of_service_url)
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    val supportUrl = stringResource(R.string.support_url)
    val accountDeletionUrl = stringResource(R.string.account_deletion_url)
    val paywallPricing = PaywallPricing(
        weekly = billingState.products[BillingPlan.WEEKLY]?.formattedPrice ?: "$4.99",
        monthly = billingState.products[BillingPlan.MONTHLY]?.formattedPrice ?: "$9.99",
        yearly = billingState.products[BillingPlan.YEARLY]?.formattedPrice ?: "$49.99",
    )

    fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun openWebPage(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }.onFailure { showMessage("No browser is available to open this page.") }
    }

    fun enableNotifications() {
        notificationCoordinator.setEnabled(true) { result ->
            result.onSuccess {
                pushAlertsEnabled = true
                preferences.pushAlertsEnabled = true
            }.onFailure { error ->
                pushAlertsEnabled = false
                preferences.pushAlertsEnabled = false
                showMessage(error.message ?: "Push alerts couldn't be enabled.")
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            enableNotifications()
        } else {
            pushAlertsEnabled = false
            preferences.pushAlertsEnabled = false
            showMessage("Notification permission was not granted. You can enable it later in Android Settings.")
        }
    }

    fun setPushAlerts(enabled: Boolean) {
        if (!enabled) {
            notificationCoordinator.setEnabled(false)
            pushAlertsEnabled = false
            preferences.pushAlertsEnabled = false
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

    fun purchase(plan: PaywallPlan) {
        val billingPlan = when (plan) {
            PaywallPlan.WEEKLY -> BillingPlan.WEEKLY
            PaywallPlan.MONTHLY -> BillingPlan.MONTHLY
            PaywallPlan.YEARLY -> BillingPlan.YEARLY
        }
        if (activity == null) {
            showMessage("Google Play checkout is unavailable in this window.")
        } else {
            billingManager.purchase(activity, billingPlan)
        }
    }

    fun completeDebugPaywall() {
        if (!BuildConfig.DEBUG) return
        isPro = true
        preferences.isPro = true
        preferences.hasCompletedOnboarding = true
        showUpgradePaywall = false
        stageName = AppStage.MAIN.name
    }

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    LaunchedEffect(selectedDealId, uiState.isLoading, uiState.deals) {
        if (!uiState.isLoading && selectedDealId != null && selectedDeal == null) {
            selectedDealId = null
        }
    }

    LaunchedEffect(billingState.isPro) {
        if (billingState.isPro) {
            isPro = true
            preferences.isPro = true
            preferences.hasCompletedOnboarding = true
            showUpgradePaywall = false
            if (stageName == AppStage.PAYWALL.name) stageName = AppStage.MAIN.name
        }
    }

    // Keep the launcher long-press menu in step with the entitlement: the trial
    // offer appears for everyone who hasn't subscribed and drops off the moment
    // they do.
    LaunchedEffect(isPro) {
        PriceErrorsShortcuts.refresh(context, isPro)
    }

    LaunchedEffect(pendingTrialOffer, stageName, isPro) {
        if (!pendingTrialOffer) return@LaunchedEffect
        // Onboarding and the hard paywall already lead somewhere better, so the
        // shortcut only interrupts once the user is in the app proper.
        if (stageName != AppStage.MAIN.name) return@LaunchedEffect
        if (!isPro) showUpgradePaywall = true
        navigationIntentStore.consumeTrialOffer()
    }

    LaunchedEffect(pendingDealId, stageName, uiState.isLoading, uiState.deals) {
        val requestedId = pendingDealId ?: return@LaunchedEffect
        if (stageName != AppStage.MAIN.name || uiState.isLoading) return@LaunchedEffect
        val exists = uiState.deals.any { it.id == requestedId }
        if (exists) {
            selectedTabName = MainTab.FEED.name
            selectedDealId = requestedId
        } else {
            showMessage("That deal is no longer available.")
        }
        navigationIntentStore.consumeDeal(requestedId)
    }

    BackHandler(enabled = selectedDealId != null) {
        selectedDealId = null
    }
    BackHandler(
        enabled = stage == AppStage.MAIN && selectedDealId == null &&
            !showUpgradePaywall && selectedTab != MainTab.FEED,
    ) {
        selectedTabName = MainTab.FEED.name
        communityHasNestedScreen = false
    }
    BackHandler(enabled = showUpgradePaywall) {
        showUpgradePaywall = false
    }

    PriceErrorsTheme(darkTheme = darkTheme, palette = palette) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (stage) {
                AppStage.ONBOARDING -> OnboardingScreen(
                    onContinue = { stageName = AppStage.PREFERENCES.name },
                )

                AppStage.PREFERENCES -> PreferencesScreen(
                    onContinue = { categories ->
                        preferredCategories = categories
                        preferences.preferredCategories = categories
                        stageName = AppStage.AUTH.name
                    },
                )

                AppStage.AUTH -> AuthScreen(
                    onAuthenticated = { authenticatedName, authenticatedEmail ->
                        displayName = authenticatedName
                        email = authenticatedEmail
                        preferences.displayName = authenticatedName
                        preferences.email = authenticatedEmail
                        preferences.isSignedIn = true
                        stageName = if (preferences.hasCompletedOnboarding) {
                            AppStage.MAIN.name
                        } else {
                            AppStage.PAYWALL.name
                        }
                    },
                    allowLocalEmailAuth = BuildConfig.DEBUG,
                    // With a backend configured the Google token is exchanged
                    // for a real Supabase session below, so sign-in can
                    // complete. Without one there is nothing to verify against.
                    canCompleteGoogleSignIn = ApiConfig.isConfigured || BuildConfig.DEBUG,
                    onGoogleAuthenticate = {
                        val currentActivity = activity
                        if (currentActivity == null) {
                            Result.failure(IllegalStateException("Google sign-in is unavailable."))
                        } else {
                            googleAuthClient.signIn(currentActivity).mapCatching { identity ->
                                // The Google ID token only becomes an authorised
                                // session once Supabase accepts it; every server
                                // route authenticates on that JWT alone.
                                if (ApiConfig.isConfigured) {
                                    supabaseAuthClient.signInWithGoogle(identity.idToken)
                                        .getOrThrow()
                                }
                                identity
                            }
                        }
                    },
                )

                AppStage.PAYWALL -> PaywallScreen(
                    allowDismiss = false,
                    onDismiss = {},
                    onPurchase = ::purchase,
                    onRestore = { billingManager.restorePurchases() },
                    pricing = paywallPricing,
                    isLoading = billingState.isLoading,
                    message = billingState.message,
                    allowDebugBypass = BuildConfig.DEBUG,
                    onDebugBypass = ::completeDebugPaywall,
                    onOpenTerms = { openWebPage(termsUrl) },
                    onOpenPrivacy = { openWebPage(privacyUrl) },
                )

                AppStage.MAIN -> Box(modifier = Modifier.fillMaxSize()) {
                    if (selectedDeal != null) {
                        DealDetailScreen(
                            deal = selectedDeal,
                            isSaved = selectedDeal.id in savedDealIds,
                            isClaimed = selectedDeal.id in claimedDealIds,
                            selectedVote = votesByDealId[selectedDeal.id] ?: selectedDeal.userVote,
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
                            onClaim = {
                                claimedDealIds = claimedDealIds + selectedDeal.id
                                preferences.claimedDealIds = claimedDealIds
                                // Records the open server-side; this is what
                                // decrements the free-tier daily allowance.
                                networkRepository?.let { network ->
                                    scope.launch { network.claim(selectedDeal.id) }
                                }
                            },
                        )
                    } else {
                        when (selectedTab) {
                            MainTab.FEED -> FeedScreen(
                                uiState = personalizedFeedState,
                                layout = feedLayout,
                                onDealSelected = { selectedDealId = it },
                                onRetry = feedViewModel::refresh,
                            )

                            MainTab.COMMUNITY -> CommunityScreen(
                                isPro = isPro,
                                currentUserName = displayName,
                                onUpgrade = { showUpgradePaywall = true },
                                onNestedDestinationChanged = { communityHasNestedScreen = it },
                                api = priceErrorsApi,
                            )

                            MainTab.BROWSE -> BrowseScreen(
                                deals = uiState.deals,
                                onDealSelected = { selectedDealId = it },
                            )

                            MainTab.PROFILE -> ProfileScreen(
                                name = displayName,
                                email = email,
                                isPro = isPro,
                                dayStreak = if (claimedDealIds.isEmpty()) 0 else 1,
                                savedDeals = uiState.deals.filter { it.id in savedDealIds },
                                myPosts = listOf(
                                    ProfilePost(
                                        id = "local-profile-post",
                                        title = "Apple Giftcard - $310 Value for 90%",
                                        priceLabel = "$279",
                                        category = "Tech",
                                    ),
                                ),
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
                                onUpgrade = { showUpgradePaywall = true },
                                onDealSelected = { selectedDealId = it },
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
                                    preferences.isSignedIn = false
                                    selectedTabName = MainTab.FEED.name
                                    stageName = AppStage.AUTH.name
                                    // Drop the server session and any deals it
                                    // fetched before the next account signs in.
                                    supabaseAuthClient.clearSession()
                                    networkRepository?.clear()
                                    activity?.let { currentActivity ->
                                        scope.launch { googleAuthClient.clear(currentActivity) }
                                    }
                                },
                                onDeleteAccount = {
                                    // The server identifies the account from the
                                    // session, so the session must outlive the
                                    // delete call — clear it only once the call
                                    // has returned.
                                    scope.launch {
                                        priceErrorsApi?.deleteAccount()
                                        supabaseAuthClient.clearSession()
                                        networkRepository?.clear()
                                    }
                                    preferences.resetAccount()
                                    displayName = preferences.displayName
                                    email = preferences.email
                                    isPro = false
                                    savedDealIds = emptySet()
                                    claimedDealIds = emptySet()
                                    votesByDealId = emptyMap()
                                    preferredCategories = emptySet()
                                    selectedTabName = MainTab.FEED.name
                                    stageName = AppStage.ONBOARDING.name
                                },
                                onOpenSupport = { openWebPage(supportUrl) },
                                onOpenPrivacy = { openWebPage(privacyUrl) },
                                onOpenTerms = { openWebPage(termsUrl) },
                                onOpenAccountDeletion = { openWebPage(accountDeletionUrl) },
                            )
                        }

                        if (!communityHasNestedScreen || selectedTab != MainTab.COMMUNITY) {
                            FloatingTabBar(
                                selectedTab = selectedTab,
                                onTabSelected = { tab ->
                                    selectedTabName = tab.name
                                    if (tab != MainTab.COMMUNITY) communityHasNestedScreen = false
                                },
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }

                    if (showUpgradePaywall) {
                        PaywallScreen(
                            allowDismiss = true,
                            onDismiss = { showUpgradePaywall = false },
                            onPurchase = ::purchase,
                            onRestore = { billingManager.restorePurchases() },
                            pricing = paywallPricing,
                            isLoading = billingState.isLoading,
                            message = billingState.message,
                            allowDebugBypass = BuildConfig.DEBUG,
                            onDebugBypass = ::completeDebugPaywall,
                            onOpenTerms = { openWebPage(termsUrl) },
                            onOpenPrivacy = { openWebPage(privacyUrl) },
                        )
                    }
                }
            }
        }
    }
}
