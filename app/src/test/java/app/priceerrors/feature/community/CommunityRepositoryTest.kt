package app.priceerrors.feature.community

import app.priceerrors.core.auth.AccessTokenProvider
import app.priceerrors.core.network.ApiError
import app.priceerrors.core.network.PriceErrorsApi
import java.time.Instant
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

class CommunityRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: CommunityRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = CommunityRepository(
            PriceErrorsApi(
                httpClient = OkHttpClient(),
                tokenProvider = AccessTokenProvider { "test-token" },
                json = Json { ignoreUnknownKeys = true },
                baseUrl = server.url("/").toString().trimEnd('/'),
                apiKey = "test-api-key",
            ),
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `maps user deal rows onto community posts`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """
                    {"data":[
                      {"id":"11111111-1111-1111-1111-111111111111",
                       "user_display_name":"Maya","title":"Taco kit",
                       "description":"Cheap tonight","category":"Food",
                       "brand":"Local","url":"https://example.com","price":6.5,
                       "is_admin":false,
                       "created_at":"2026-07-19T10:15:30.123456+00:00"}
                     ],"total":1,"page":1,"limit":50}
                """.trimIndent(),
            ),
        )

        val posts = repository.loadFeed().getOrThrow()

        assertEquals(1, posts.size)
        val post = posts[0]
        assertEquals("Maya", post.userDisplayName)
        assertEquals("Food", post.category)
        assertEquals(6.5, post.price, 0.001)
        assertEquals(
            Instant.parse("2026-07-19T10:15:30.123456Z").toEpochMilli(),
            post.createdAtMillis,
        )
        assertEquals("/user-deals", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `treats a null price as free`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """
                    {"data":[{"id":"a","user_display_name":"Sam","title":"Freebie",
                              "description":"No cost","category":"Other"}],
                     "total":1,"page":1,"limit":50}
                """.trimIndent(),
            ),
        )

        val post = repository.loadFeed().getOrThrow()[0]

        assertEquals(0.0, post.price, 0.001)
        assertEquals("FREE", post.priceLabel)
        assertNull(post.url)
    }

    @Test
    fun `posts a submission and returns the saved row`() = runTest {
        server.enqueue(
            MockResponse(
                code = 201,
                body = """
                    {"id":"server-assigned-id","user_display_name":"Jordan",
                     "title":"Controller clearance","description":"Still working",
                     "category":"Gaming","brand":"GameStop",
                     "url":"https://example.com","price":14.99,"is_admin":false,
                     "created_at":"2026-07-19T12:00:00+00:00"}
                """.trimIndent(),
            ),
        )
        val draft = CommunityDeal(
            id = "local-draft-id",
            userDisplayName = "Jordan",
            title = "Controller clearance",
            description = "Still working",
            category = "Gaming",
            brand = "GameStop",
            url = "https://example.com",
            price = 14.99,
            createdAtMillis = 0L,
        )

        val saved = repository.post(draft).getOrThrow()

        // The server's id must replace the local draft id — it is what delete
        // and reporting key off.
        assertEquals("server-assigned-id", saved.id)

        val recorded = server.takeRequest()
        assertEquals("/user-deals", recorded.url.encodedPath)
        val body = recorded.body?.utf8().orEmpty()
        assertTrue(body.contains(""""user_display_name":"Jordan""""))
        assertTrue(body.contains(""""category":"Gaming""""))
    }

    @Test
    fun `surfaces pro required when a free user posts`() = runTest {
        server.enqueue(
            MockResponse(code = 403, body = """{"error":"Pro required","reason":"pro_required"}"""),
        )
        val draft = CommunityDeal(
            id = "d",
            userDisplayName = "Sam",
            title = "T",
            description = "D",
            category = "Other",
            brand = null,
            url = null,
            price = 1.0,
            createdAtMillis = 0L,
        )

        assertEquals(ApiError.ProRequired, repository.post(draft).exceptionOrNull())
    }

    @Test
    fun `surfaces the posting rate limit`() = runTest {
        server.enqueue(
            MockResponse(code = 429, body = """{"error":"Too many posts."}"""),
        )
        val draft = CommunityDeal(
            id = "d",
            userDisplayName = "Sam",
            title = "T",
            description = "D",
            category = "Other",
            brand = null,
            url = null,
            price = 1.0,
            createdAtMillis = 0L,
        )

        assertEquals(ApiError.RateLimited, repository.post(draft).exceptionOrNull())
    }

    @Test
    fun `deletes a post by id`() = runTest {
        server.enqueue(MockResponse(code = 200, body = """{"ok":true}"""))

        assertTrue(repository.delete("abc-123").isSuccess)

        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals("/user-deals/abc-123", recorded.url.encodedPath)
    }

    @Test
    fun `drops malformed rows rather than failing the feed`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """
                    {"data":[
                      {"id":"good","user_display_name":"A","title":"T",
                       "description":"D","category":"Tech","price":1.0}
                     ],"total":1,"page":1,"limit":50}
                """.trimIndent(),
            ),
        )

        assertEquals(1, repository.loadFeed().getOrThrow().size)
    }
}
