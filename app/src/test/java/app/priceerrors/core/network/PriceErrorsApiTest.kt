package app.priceerrors.core.network

import app.priceerrors.core.auth.AccessTokenProvider
import app.priceerrors.core.model.DealVote
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PriceErrorsApiTest {

    private lateinit var server: MockWebServer
    private val json = Json { ignoreUnknownKeys = true }

    /** Hands out a fresh token each call so refresh behaviour is observable. */
    private class FakeTokenProvider(
        private val tokens: MutableList<String?>,
    ) : AccessTokenProvider {
        var calls = 0
            private set

        override suspend fun currentAccessToken(): String? {
            calls++
            return if (tokens.isEmpty()) null else tokens.removeAt(0)
        }
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun api(
        provider: AccessTokenProvider = FakeTokenProvider(mutableListOf("token-1")),
        closedTestAccess: Boolean = false,
    ) = PriceErrorsApi(
        httpClient = OkHttpClient(),
        tokenProvider = provider,
        json = json,
        baseUrl = server.url("/").toString().trimEnd('/'),
        apiKey = "test-api-key",
        closedTestAccess = closedTestAccess,
    )

    @Test
    fun `sends the api key and bearer token on every request`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"data":[],"total":0,"page":1,"limit":100,"isPro":true}""",
            ),
        )

        api().fetchDeals()

        val recorded = server.takeRequest()
        assertEquals("test-api-key", recorded.headers["X-API-Key"])
        assertEquals("Bearer token-1", recorded.headers["Authorization"])
        assertEquals(null, recorded.headers["X-PriceErrors-Closed-Test"])
    }

    @Test
    fun `closed test requests identify the Android beta build`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"data":[],"total":0,"page":1,"limit":100,"isPro":true}""",
            ),
        )

        api(closedTestAccess = true).fetchDeals()

        assertEquals(
            "android",
            server.takeRequest().headers["X-PriceErrors-Closed-Test"],
        )
    }

    @Test
    fun `registers an Android FCM token with its platform`() = runTest {
        server.enqueue(MockResponse(code = 200, body = """{"success":true}"""))

        api().registerAndroidDevice(
            token = "fcm-token",
            bundleId = "app.priceerrors",
            closedTestAccess = true,
        )

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/devices/register", recorded.url.encodedPath)
        assertEquals(
            """{"token":"fcm-token","bundle_id":"app.priceerrors","platform":"android","closed_test_access":true}""",
            recorded.body?.utf8(),
        )
    }

    @Test
    fun `unregisters a device before sign out`() = runTest {
        server.enqueue(MockResponse(code = 200, body = """{"success":true}"""))

        api().unregisterDevice("fcm-token")

        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals("/devices/register", recorded.url.encodedPath)
        assertEquals("""{"token":"fcm-token"}""", recorded.body?.utf8())
    }

    @Test
    fun `account deletion failure remains visible to the caller`() = runTest {
        server.enqueue(MockResponse(code = 500, body = """{"error":"Failed to delete account"}"""))

        val result = api().deleteAccount()

        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals("/account", recorded.url.encodedPath)
        assertTrue(result.isFailure)
    }

    @Test
    fun `marks a device opened to reset its unseen count`() = runTest {
        server.enqueue(MockResponse(code = 200, body = """{"success":true}"""))

        api().markDeviceOpened("fcm-token")

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/devices/seen", recorded.url.encodedPath)
        assertEquals("""{"token":"fcm-token"}""", recorded.body?.utf8())
    }

    @Test
    fun `requests the deals page with paging and local date`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"data":[],"total":0,"page":2,"limit":50,"isPro":true}""",
            ),
        )

        api().fetchDeals(page = 2, limit = 50, category = "Tech")

        val path = server.takeRequest().url
        assertEquals("/deals", path.encodedPath)
        assertEquals("2", path.queryParameter("page"))
        assertEquals("50", path.queryParameter("limit"))
        assertEquals("Tech", path.queryParameter("category"))
        assertTrue(path.queryParameter("local_date")!!.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
    }

    @Test
    fun `decodes a deals response`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """
                    {"data":[{"id":"a","title":"A","price":9.99,"working_count":4}],
                     "total":1,"page":1,"limit":100,"isPro":true,
                     "claimedToday":[],"userVotes":{}}
                """.trimIndent(),
            ),
        )

        val result = api().fetchDeals()

        val response = result.getOrThrow()
        assertEquals(1, response.data.size)
        assertEquals("a", response.data[0].id)
        assertEquals(4, response.data[0].workingCount)
    }

    @Test
    fun `surfaces pro required so the caller can show the paywall`() = runTest {
        server.enqueue(
            MockResponse(
                code = 403,
                body = """{"error":"Pro required","reason":"pro_required"}""",
            ),
        )

        val error = api().fetchDeals().exceptionOrNull()

        assertEquals(ApiError.ProRequired, error)
    }

    @Test
    fun `surfaces the free tier limit distinctly from other failures`() = runTest {
        server.enqueue(
            MockResponse(
                code = 402,
                body = """{"error":"Limit reached","reason":"limit_reached"}""",
            ),
        )

        val error = api().claimDeal("deal-1").exceptionOrNull()

        assertEquals(ApiError.LimitReached, error)
    }

    @Test
    fun `fetches a claim and decodes claim savings fields`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """
                    {
                      "ok":true,
                      "status":"opened",
                      "trackable":true,
                      "price":19.99,
                      "original_price":39.99,
                      "potential_savings":20.0,
                      "confirmed_at":"2026-09-12T18:00:00Z",
                      "isPro":true,
                      "freeRemaining":2,
                      "freeDailyLimit":3,
                      "alreadyClaimed":true
                    }
                """.trimIndent(),
            ),
        )

        val result = api().fetchClaim("claim-7")

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/claims/claim-7", recorded.url.encodedPath)
        val response = result.getOrThrow()
        assertEquals("opened", response.status)
        assertTrue(response.trackable)
        assertEquals(19.99, response.price)
        assertEquals(39.99, response.originalPrice)
        assertEquals(20.0, response.potentialSavings)
        assertEquals("2026-09-12T18:00:00Z", response.confirmedAt)
        assertEquals(true, response.isPro)
        assertEquals(2, response.freeRemaining)
        assertEquals(3, response.freeDailyLimit)
        assertEquals(true, response.alreadyClaimed)
    }

    @Test
    fun `confirms a claim with a patch and local date`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """
                    {
                      "ok":true,
                      "status":"confirmed",
                      "trackable":true,
                      "potential_savings":20.0,
                      "month_savings":45.5,
                      "lifetime_savings":125.75,
                      "currency":"USD"
                    }
                """.trimIndent(),
            ),
        )

        val result = api().confirmDealClaim("claim-7")

        val recorded = server.takeRequest()
        assertEquals("PATCH", recorded.method)
        assertEquals("/claims/claim-7/confirm", recorded.url.encodedPath)
        assertTrue(
            recorded.body!!.utf8()
                .matches(Regex("""\{"local_date":"\d{4}-\d{2}-\d{2}"}""")),
        )
        val response = result.getOrThrow()
        assertEquals("confirmed", response.status)
        assertTrue(response.trackable)
        assertEquals(20.0, response.potentialSavings)
        assertEquals(45.5, response.monthSavings, 0.001)
        assertEquals(125.75, response.lifetimeSavings, 0.001)
        assertEquals("USD", response.currency)
    }

    @Test
    fun `fetches the savings summary route before treating summary as an id`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """
                    {
                      "month_savings":45.5,
                      "lifetime_savings":125.75,
                      "currency":"USD"
                    }
                """.trimIndent(),
            ),
        )

        val result = api().fetchSavingsSummary("2026-09")

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/claims/summary", recorded.url.encodedPath)
        assertEquals("2026-09", recorded.url.queryParameter("month"))
        val response = result.getOrThrow()
        assertEquals(45.5, response.monthSavings, 0.001)
        assertEquals(125.75, response.lifetimeSavings, 0.001)
        assertEquals("USD", response.currency)
    }

    @Test
    fun `maps claim confirmation authorization failures`() = runTest {
        server.enqueue(
            MockResponse(
                code = 403,
                body = """{"error":"Pro required","reason":"pro_required"}""",
            ),
        )

        val error = api().confirmDealClaim("claim-7").exceptionOrNull()

        assertEquals(ApiError.ProRequired, error)
    }

    @Test
    fun `surfaces rate limiting`() = runTest {
        server.enqueue(MockResponse(code = 429, body = """{"error":"Too many votes"}"""))

        val error = api().voteDeal("deal-1", DealVote.WORKING).exceptionOrNull()

        assertEquals(ApiError.RateLimited, error)
    }

    @Test
    fun `retries once with a refreshed token after a 401`() = runTest {
        server.enqueue(MockResponse(code = 401, body = """{"error":"Invalid token"}"""))
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"data":[],"total":0,"page":1,"limit":100,"isPro":true}""",
            ),
        )
        val provider = FakeTokenProvider(mutableListOf("stale-token", "fresh-token"))

        val result = api(provider).fetchDeals()

        assertTrue(result.isSuccess)
        assertEquals("Bearer stale-token", server.takeRequest().headers["Authorization"])
        assertEquals("Bearer fresh-token", server.takeRequest().headers["Authorization"])
        assertEquals(2, provider.calls)
    }

    @Test
    fun `gives up when the refreshed token is also rejected`() = runTest {
        server.enqueue(MockResponse(code = 401, body = """{"error":"Invalid token"}"""))
        server.enqueue(MockResponse(code = 401, body = """{"error":"Invalid token"}"""))
        val provider = FakeTokenProvider(mutableListOf("stale-token", "also-stale"))

        val error = api(provider).fetchDeals().exceptionOrNull()

        assertEquals(ApiError.Unauthorized, error)
    }

    @Test
    fun `fails fast when there is no session at all`() = runTest {
        val error = api(FakeTokenProvider(mutableListOf())).fetchDeals().exceptionOrNull()

        assertEquals(ApiError.Unauthorized, error)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `reports a configuration error when no backend is set`() = runTest {
        val unconfigured = PriceErrorsApi(
            httpClient = OkHttpClient(),
            tokenProvider = FakeTokenProvider(mutableListOf("token")),
            json = json,
            baseUrl = "",
            apiKey = "",
        )

        assertEquals(ApiError.NotConfigured, unconfigured.fetchDeals().exceptionOrNull())
    }

    @Test
    fun `posts the vote body the server expects`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"working_count":5,"not_working_count":1,"user_vote":"working"}""",
            ),
        )

        val result = api().voteDeal("deal-9", DealVote.NOT_WORKING)

        val recorded = server.takeRequest()
        assertEquals("/votes", recorded.url.encodedPath)
        assertEquals(
            """{"deal_id":"deal-9","vote":"not_working"}""",
            recorded.body?.utf8(),
        )
        assertEquals(5, result.getOrThrow().workingCount)
    }

    @Test
    fun `maps an unexpected payload to a decoding error`() = runTest {
        server.enqueue(MockResponse(code = 200, body = "not json at all"))

        val error = api().fetchDeals().exceptionOrNull()

        assertTrue(error is ApiError.Decoding)
    }

    @Test
    fun `maps other statuses to a server error carrying the code`() = runTest {
        server.enqueue(MockResponse(code = 500, body = """{"error":"boom"}"""))

        val error = api().fetchDeals().exceptionOrNull()

        assertTrue(error is ApiError.Server)
        assertEquals(500, (error as ApiError.Server).statusCode)
    }
}
