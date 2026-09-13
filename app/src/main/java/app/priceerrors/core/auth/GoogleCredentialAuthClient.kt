package app.priceerrors.core.auth

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import app.priceerrors.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

data class AuthIdentity(
    val displayName: String,
    val email: String,
    val idToken: String,
    val profilePhotoUrl: String?,
    /** Raw nonce sent to Supabase; Google receives the SHA-256 hex digest. */
    val nonce: String? = null,
)

class AuthConfigurationException(message: String) : IllegalStateException(message)

/**
 * Android's Google identity client. The returned ID token still needs to be
 * exchanged with Supabase when backend work is enabled; this class deliberately
 * does not treat a token as a server-authenticated session.
 */
class GoogleCredentialAuthClient {
    val isConfigured: Boolean
        get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    suspend fun signIn(activity: Activity): Result<AuthIdentity> = runCatching {
        if (!isConfigured) {
            throw AuthConfigurationException(
                "Google sign-in needs PRICEERRORS_GOOGLE_WEB_CLIENT_ID in local.properties or CI.",
            )
        }

        val credentialManager = CredentialManager.create(activity)
        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = sha256Hex(rawNonce)

        // GetSignInWithGoogleOption is the API behind an explicit "Sign in with
        // Google" button: it always opens the account picker. GetGoogleIdOption
        // is One Tap, which can fail with "[16] Account reauth failed" against a
        // cached account and is the wrong control for this button.
        val response = try {
            credentialManager.getCredential(
                context = activity,
                request = GetCredentialRequest.Builder()
                    .addCredentialOption(
                        GetSignInWithGoogleOption
                            .Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID.trim())
                            .setNonce(hashedNonce)
                            .build(),
                    )
                    .build(),
            )
        } catch (error: Throwable) {
            if (error is NoCredentialException) {
                throw IllegalStateException(
                    "No Google account is available on this device. Add one in " +
                        "Settings, or sign up with an email address instead.",
                    error,
                )
            }
            throw remapGoogleSignInError(error)
        }
        val credential = response.credential
        if (
            credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw IllegalStateException("Google returned an unsupported credential type.")
        }

        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        AuthIdentity(
            displayName = googleCredential.displayName
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: googleCredential.id.substringBefore('@'),
            email = googleCredential.id,
            idToken = googleCredential.idToken,
            profilePhotoUrl = googleCredential.profilePictureUri?.toString(),
            nonce = rawNonce,
        )
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(remapGoogleSignInError(it)) },
    )

    suspend fun clear(activity: Activity) {
        runCatching {
            CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
