package app.priceerrors

import android.content.Context
import app.priceerrors.core.auth.GoogleCredentialAuthClient
import app.priceerrors.core.auth.SupabaseAuthClient
import app.priceerrors.core.auth.SupabaseSessionStore
import app.priceerrors.core.billing.BillingManager
import app.priceerrors.core.data.DealRepository
import app.priceerrors.core.data.FakeDealRepository
import app.priceerrors.core.data.NetworkDealRepository
import app.priceerrors.core.navigation.NavigationIntentStore
import app.priceerrors.core.network.ApiConfig
import app.priceerrors.core.network.PriceErrorsApi
import app.priceerrors.core.notifications.NotificationCoordinator
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

interface AppContainer {
    val dealRepository: DealRepository
    val googleAuthClient: GoogleCredentialAuthClient
    val supabaseAuthClient: SupabaseAuthClient

    /** Null when this build has no backend configured. */
    val priceErrorsApi: PriceErrorsApi?
    val billingManager: BillingManager
    val navigationIntentStore: NavigationIntentStore
    val notificationCoordinator: NotificationCoordinator
}

class DefaultAppContainer(context: Context) : AppContainer {

    private val json = Json {
        // The server adds fields ahead of client releases; unknown keys must
        // never fail a decode.
        ignoreUnknownKeys = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    override val supabaseAuthClient = SupabaseAuthClient(
        httpClient = httpClient,
        sessionStore = SupabaseSessionStore(context),
        json = json,
    )

    private val api = PriceErrorsApi(
        httpClient = httpClient,
        tokenProvider = supabaseAuthClient,
        json = json,
    )

    override val priceErrorsApi: PriceErrorsApi? = api.takeIf { ApiConfig.isConfigured }

    /**
     * Falls back to the sample feed only when no backend is configured, so a
     * developer without credentials still gets a runnable app while any build
     * that has them always talks to the real server.
     */
    override val dealRepository: DealRepository =
        if (ApiConfig.isConfigured) NetworkDealRepository(api) else FakeDealRepository()

    override val googleAuthClient = GoogleCredentialAuthClient()
    override val billingManager = BillingManager(context)
    override val navigationIntentStore = NavigationIntentStore()
    override val notificationCoordinator = NotificationCoordinator(context)
}
