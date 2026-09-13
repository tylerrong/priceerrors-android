package app.priceerrors.core.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleSignInErrorsTest {
    @Test
    fun `reauth failure becomes a signing-certificate message`() {
        val remapped = remapGoogleSignInError(
            GetCredentialCancellationException("[16] Account reauth failed."),
        )
        assertTrue(remapped.message.orEmpty().contains("App signing"))
        assertTrue(remapped.message.orEmpty().contains("app.priceerrors"))
    }

    @Test
    fun `maps developer console misconfiguration to a setup hint`() {
        val remapped = remapGoogleSignInError(
            IllegalStateException("[28444] Developer console is not set up correctly."),
        )
        assertTrue(remapped.message.orEmpty().contains("App signing"))
    }

    @Test
    fun `generic cancellation is treated as a user dismiss`() {
        val remapped = remapGoogleSignInError(GetCredentialCancellationException("[16]"))
        assertTrue(remapped is GoogleSignInCancelled)
    }

    @Test
    fun `other failures pass through`() {
        val original = IllegalStateException("No Google account is available on this device.")
        assertSame(original, remapGoogleSignInError(original))
    }

    @Test
    fun `configuration errors stay typed`() {
        val original = AuthConfigurationException("missing client id")
        assertEquals(original, remapGoogleSignInError(original))
    }
}
