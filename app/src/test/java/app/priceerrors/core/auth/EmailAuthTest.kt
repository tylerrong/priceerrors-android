package app.priceerrors.core.auth

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

class EmailAuthTest {

    private lateinit var server: MockWebServer
    private lateinit var storage: InMemoryStorage

    private class InMemoryStorage(var session: SupabaseSession? = null) : SessionStorage {
        override fun load(): SupabaseSession? = session
        override fun save(session: SupabaseSession) { this.session = session }
        override fun clear() { session = null }
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        storage = InMemoryStorage()
    }

    @After
    fun tearDown() = server.close()

    private fun client() = SupabaseAuthClient(
        httpClient = OkHttpClient(),
        sessionStore = storage,
        json = Json { ignoreUnknownKeys = true },
        supabaseUrl = server.url("/").toString().trimEnd('/'),
        anonKey = "anon-key",
    )

    @Test
    fun `signs in with email and keeps the session`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """
                    {"access_token":"acc","refresh_token":"ref","expires_in":3600,
                     "user":{"id":"u1","email":"tester@example.com",
                             "user_metadata":{"full_name":"Test User"}}}
                """.trimIndent(),
            ),
        )

        val identity = client().signInWithEmail("tester@example.com", "secret123").getOrThrow()

        assertEquals("Test User", identity.displayName)
        assertEquals("tester@example.com", identity.email)
        assertEquals("acc", storage.session?.accessToken)

        val recorded = server.takeRequest()
        assertEquals("password", recorded.url.queryParameter("grant_type"))
    }

    @Test
    fun `falls back to the email prefix when no name is stored`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """
                    {"access_token":"a","refresh_token":"r","expires_in":3600,
                     "user":{"id":"u1","email":"nameless@example.com"}}
                """.trimIndent(),
            ),
        )

        val identity = client().signInWithEmail("nameless@example.com", "secret123").getOrThrow()
        assertEquals("nameless", identity.displayName)
    }

    @Test
    fun `wrong password reports invalid credentials`() = runTest {
        server.enqueue(
            MockResponse(
                code = 400,
                body = """{"error":"invalid_grant","error_description":"Invalid login credentials"}""",
            ),
        )

        val error = client().signInWithEmail("tester@example.com", "nope").exceptionOrNull()
        assertTrue(error is EmailAuthError.InvalidCredentials)
    }

    @Test
    fun `unconfirmed email is distinguished from a bad password`() = runTest {
        server.enqueue(
            MockResponse(code = 400, body = """{"code":400,"msg":"Email not confirmed"}"""),
        )

        val error = client().signInWithEmail("tester@example.com", "secret123").exceptionOrNull()
        assertTrue(error is EmailAuthError.EmailNotConfirmed)
    }

    @Test
    fun `sign up returns a session and stores the name as full_name`() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """
                    {"access_token":"acc","refresh_token":"ref","expires_in":3600,
                     "user":{"id":"u9","email":"new@example.com"}}
                """.trimIndent(),
            ),
        )

        val result = client().signUpWithEmail("new@example.com", "secret123", "New Person").getOrThrow()

        assertTrue(result is EmailSignUpResult.SignedIn)
        assertEquals("New Person", (result as EmailSignUpResult.SignedIn).identity.displayName)

        val body = server.takeRequest().body?.utf8().orEmpty()
        assertTrue(body.contains("\"full_name\":\"New Person\""))
    }

    @Test
    fun `sign up without tokens means confirmation is required, not signed in`() = runTest {
        // Supabase returns the bare user when the project confirms emails.
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"id":"u9","email":"new@example.com","aud":"authenticated"}""",
            ),
        )

        val result = client().signUpWithEmail("new@example.com", "secret123", "New").getOrThrow()

        assertEquals(EmailSignUpResult.ConfirmationRequired, result)
        // Critically: no session was adopted, so the app cannot think it is signed in.
        assertNull(storage.session)
    }

    @Test
    fun `existing address is reported as already registered`() = runTest {
        server.enqueue(
            MockResponse(
                code = 422,
                body = """{"msg":"User already registered"}""",
            ),
        )

        val error = client().signUpWithEmail("dupe@example.com", "secret123", "Dupe").exceptionOrNull()
        assertTrue(error is EmailAuthError.AlreadyRegistered)
    }
}
