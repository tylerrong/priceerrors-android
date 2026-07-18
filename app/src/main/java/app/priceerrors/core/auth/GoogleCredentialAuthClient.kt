package app.priceerrors.core.auth

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import app.priceerrors.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

data class AuthIdentity(
    val displayName: String,
    val email: String,
    val idToken: String,
    val profilePhotoUrl: String?,
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

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val response = try {
            CredentialManager.create(activity).getCredential(
                context = activity,
                request = request,
            )
        } catch (error: NoCredentialException) {
            throw IllegalStateException(
                "No eligible Google account was found on this device.",
                error,
            )
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
        )
    }

    suspend fun clear(activity: Activity) {
        runCatching {
            CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest())
        }
    }
}
