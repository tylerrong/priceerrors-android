package app.priceerrors.core.data

import app.priceerrors.core.auth.AccessTokenProvider
import java.nio.file.Files
import app.priceerrors.core.model.DealVote
import app.priceerrors.core.network.ApiError
import app.priceerrors.core.network.PriceErrorsApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Exercises the repository against a mock server, so the wire format, the
 * mapping, and the caching behaviour are all covered by the same test.
 */
class NetworkDealRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: NetworkDealRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val cacheRoot = Files.createTempDirectory("deal-feed-test").toFile()
        repository = NetworkDealRepository(
            PriceErrorsApi(
                httpClient = OkHttpClient(),
                tokenProvider = AccessTokenProvider { "test-token" },
                json = Json { ignoreUnknownKeys = true },
                baseUrl = server.url("/").toString().trimEnd('/'),
                apiKey = "test-api-key",
            ),
            cacheRoot = cacheRoot,
            userIdProvider = { null },
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueueFeed(body: String) = server.enqueue(MockResponse(code = 200, body = body))

    private val twoDeals = """
        {"data":[
            {"id":"a","title":"Deal A","price":10.0,"original_price":20.0,"working_count":3},
            {"id":"b","title":"Deal B","price":5.0}
         ],
         "total":2,"page":1,"limit":100,"isPro":true,"freeRemaining":-1,
         "freeDailyLimit":0,"claimedToday":[],"userVotes":{"a":"working"}}
    """.trimIndent()

    @Test
    fun `refresh publishes deals to observers`() = runTest {
        enqueueFeed(twoDeals)

        val result = repository.refresh()

        assertTrue(result.isSuccess)
        val deals = repository.observeFeed().first()
        assertEquals(listOf("a", "b"), deals.map { it.id })
        assertEquals(1000L, deals[0].priceInCents)
        assertEquals(50, deals[0].discountPercent)
        assertEquals(DealVote.WORKING, deals[0].userVote)
    }

    @Test
    fun `refresh publishes feed metadata`() = runTest {
        enqueueFeed(twoDeals)

        repository.refresh()

        val metadata = repository.observeMetadata().first()!!
        assertEquals(2, metadata.total)
        assertTrue(metadata.access.isPro)
        assertNull(metadata.access.freeRemaining)
    }

    @Test
    fun `observeDeal emits the matching deal and null for unknown ids`() = runTest {
        enqueueFeed(twoDeals)
        repository.refresh()

        assertEquals("Deal B", repository.observeDeal("b").first()?.title)
        assertNull(repository.observeDeal("missing").first())
    }

    @Test
    fun `a failed refresh keeps the deals already on screen`() = runTest {
        enqueueFeed(twoDeals)
        repository.refresh()
        server.enqueue(MockResponse(code = 500, body = """{"error":"boom"}"""))

        val result = repository.refresh()

        assertTrue(result.isFailure)
        // The user keeps reading the previous feed rather than seeing it blank.
        assertEquals(2, repository.observeFeed().first().size)
    }

    @Test
    fun `a paywalled refresh surfaces pro required`() = runTest {
        server.enqueue(
            MockResponse(code = 403, body = """{"error":"Pro required","reason":"pro_required"}"""),
        )

        assertEquals(ApiError.ProRequired, repository.refresh().exceptionOrNull())
    }

    @Test
    fun `voting folds the servers counts back into the cached deal`() = runTest {
        enqueueFeed(twoDeals)
        repository.refresh()
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"working_count":9,"not_working_count":2,"user_vote":"not_working"}""",
            ),
        )

        val result = repository.vote("a", DealVote.NOT_WORKING)

        assertTrue(result.isSuccess)
        val deal = repository.observeDeal("a").first()!!
        assertEquals(9, deal.workingCount)
        assertEquals(2, deal.notWorkingCount)
        assertEquals(DealVote.NOT_WORKING, deal.userVote)
    }

    @Test
    fun `voting leaves other deals untouched`() = runTest {
        enqueueFeed(twoDeals)
        repository.refresh()
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"working_count":9,"not_working_count":2,"user_vote":"working"}""",
            ),
        )

        repository.vote("a", DealVote.WORKING)

        assertEquals(0, repository.observeDeal("b").first()!!.workingCount)
    }

    @Test
    fun `claiming records the deal and updates the remaining allowance`() = runTest {
        enqueueFeed(
            """
            {"data":[{"id":"a","title":"A","price":1.0}],"total":1,"page":1,"limit":100,
             "isPro":false,"freeRemaining":3,"freeDailyLimit":3,
             "claimedToday":[],"userVotes":{}}
            """.trimIndent(),
        )
        repository.refresh()
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"ok":true,"isPro":false,"freeRemaining":2,"freeDailyLimit":3}""",
            ),
        )

        val result = repository.claim("a")

        assertTrue(result.isSuccess)
        val access = repository.observeMetadata().first()!!.access
        assertEquals(2, access.freeRemaining)
        assertEquals(setOf("a"), access.claimedDealIds)
    }

    @Test
    fun `an exhausted allowance surfaces limit reached`() = runTest {
        server.enqueue(
            MockResponse(
                code = 402,
                body = """{"error":"Limit reached","reason":"limit_reached"}""",
            ),
        )

        assertEquals(ApiError.LimitReached, repository.claim("a").exceptionOrNull())
    }

    @Test
    fun `clear drops cached deals so the next account starts empty`() = runTest {
        enqueueFeed(twoDeals)
        repository.refresh()

        repository.clear()

        assertTrue(repository.observeFeed().first().isEmpty())
        assertNull(repository.observeMetadata().first())
    }
}
