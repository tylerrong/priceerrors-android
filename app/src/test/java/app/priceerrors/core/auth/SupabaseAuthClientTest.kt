package app.priceerrors.core.auth

import app.priceerrors.core.network.ApiError
import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SupabaseAuthClientTest {

    private lateinit var server: MockWebServer
    private lateinit var storage: InMemorySessionStorage

    private class InMemorySessionStorage(var session: SupabaseSession? = null) : SessionStorage {
        var cleared = false

        override fun load(): SupabaseSession? = session

        override fun save(session: SupabaseSession) {
            this.session = session
        }

        override fun clear() {
            session = null
            cleared = true
        }
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        storage = InMemorySessionStorage()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun client(storage: SessionStorage = this.storage) = SupabaseAuthClient(
        httpClient = OkHttpClient(),
        sessionStore = storage,
        json = Json { ignoreUnknownKeys = true },
        supabaseUrl = server.url("/").toString().trimEnd('/'),
        anonKey = "anon-key",
    )

    private fun tokenResponse(access: String, refresh: String, expiresIn: Long = 3600) =
        MockResponse(
            code = 200,
            body = """
                {"access_token":"$access","refresh_token":"$refresh",
                 "expires_in":$expiresIn,
                 "user":{"id":"user-123","email":"tester@example.com"}}
            """.trimIndent(),
        )

    @Test
    fun `exchanges a google id token for a session`() = runTest {
        server.enqueue(tokenResponse("access-1", "refresh-1"))

        val session = client().signInWithGoogle("google-id-token").getOrThrow()

        assertEquals("access-1", session.accessToken)
        assertEquals("user-123", session.userId)
        assertEquals("tester@example.com", session.email)

        val recorded = server.takeRequest()
        assertEquals("id_token", recorded.url.queryParameter("grant_type"))
        assertEquals("anon-key", recorded.headers["apikey"])
        assertEquals(
            """{"provider":"google","id_token":"google-id-token"}""",
            recorded.body?.utf8(),
        )
    }

    @Test
    fun `persists the session so it survives a relaunch`() = runTest {
        server.enqueue(tokenResponse("access-1", "refresh-1"))

        client().signInWithGoogle("google-id-token")

        assertEquals("access-1", storage.session?.accessToken)
        // A fresh client reading the same storage is already signed in.
        assertTrue(client().isSignedIn)
    }

    @Test
    fun `returns the current token without a network call when it is still valid`() = runTest {
        storage.session = SupabaseSession(
            accessToken = "still-good",
            refreshToken = "refresh-1",
            expiresAt = Instant.now().plusSeconds(3600),
            userId = "user-123",
            email = null,
        )

        assertEquals("still-good", client().currentAccessToken())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `refreshes an expired token`() = runTest {
        storage.session = SupabaseSession(
            accessToken = "expired",
            refreshToken = "refresh-1",
            expiresAt = Instant.now().minusSeconds(10),
            userId = "user-123",
            email = null,
        )
        server.enqueue(tokenResponse("access-2", "refresh-2"))

        val token = client().currentAccessToken()

        assertEquals("access-2", token)
        val recorded = server.takeRequest()
        assertEquals("refresh_token", recorded.url.queryParameter("grant_type"))
        assertEquals("""{"refresh_token":"refresh-1"}""", recorded.body?.utf8())
        // The rotated refresh token must be stored, or the next refresh fails.
        assertEquals("refresh-2", storage.session?.refreshToken)
    }

    @Test
    fun `treats a token expiring within the minute as already expired`() = runTest {
        storage.session = SupabaseSession(
            accessToken = "about-to-lapse",
            refreshToken = "refresh-1",
            // Inside the 60s safety margin: must refresh rather than risk the
            // token lapsing in flight.
            expiresAt = Instant.now().plusSeconds(30),
            userId = "user-123",
            email = null,
        )
        server.enqueue(tokenResponse("access-2", "refresh-2"))

        assertEquals("access-2", client().currentAccessToken())
    }

    @Test
    fun `drops the session when the refresh token is rejected`() = runTest {
        storage.session = SupabaseSession(
            accessToken = "expired",
            refreshToken = "revoked",
            expiresAt = Instant.now().minusSeconds(10),
            userId = "user-123",
            email = null,
        )
        server.enqueue(MockResponse(code = 400, body = """{"error":"invalid_grant"}"""))

        val subject = client()
        assertNull(subject.currentAccessToken())
        assertTrue(storage.cleared)
        assertNull(subject.session.value)
    }

    @Test
    fun `keeps the session when a refresh fails on transport`() = runTest {
        val stored = SupabaseSession(
            accessToken = "expired",
            refreshToken = "refresh-1",
            expiresAt = Instant.now().minusSeconds(10),
            userId = "user-123",
            email = null,
        )
        storage.session = stored
        server.enqueue(MockResponse(code = 503, body = """{"error":"unavailable"}"""))

        val subject = client()

        assertNull(subject.currentAccessToken())
        // A server hiccup must not sign the user out.
        assertTrue(!storage.cleared)
        assertNotNull(subject.session.value)
    }

    @Test
    fun `returns null when there is no session`() = runTest {
        assertNull(client().currentAccessToken())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `reports a configuration error when supabase is not set up`() = runTest {
        val unconfigured = SupabaseAuthClient(
            httpClient = OkHttpClient(),
            sessionStore = storage,
            json = Json { ignoreUnknownKeys = true },
            supabaseUrl = "",
            anonKey = "",
        )

        assertEquals(
            ApiError.NotConfigured,
            unconfigured.signInWithGoogle("token").exceptionOrNull(),
        )
    }

    @Test
    fun `rejects a session with no user id`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"access_token":"a","refresh_token":"r","expires_in":3600}""",
            ),
        )

        val error = client().signInWithGoogle("token").exceptionOrNull()

        assertTrue(error is ApiError.Decoding)
    }

    @Test
    fun `clearSession removes the stored session`() = runTest {
        server.enqueue(tokenResponse("access-1", "refresh-1"))
        val subject = client()
        subject.signInWithGoogle("token")

        subject.clearSession()

        assertNull(storage.session)
        assertNull(subject.session.value)
        assertTrue(!subject.isSignedIn)
    }

    @Test
    fun `escapes a google token containing json control characters`() = runTest {
        server.enqueue(tokenResponse("access-1", "refresh-1"))

        client().signInWithGoogle("tok\"en\\with\nquotes")

        // Naive string concatenation would produce invalid JSON here.
        val body = server.takeRequest().body?.utf8()
        assertEquals(
            """{"provider":"google","id_token":"tok\"en\\with\nquotes"}""",
            body,
        )
    }
}
