package app.priceerrors.core.auth

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import app.priceerrors.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
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

        val credentialManager = CredentialManager.create(activity)

        // Two different APIs, and the distinction matters. GetGoogleIdOption is
        // the One Tap path: it resolves an account without a picker, which is
        // the nicest experience for someone returning — but it raises
        // NoCredentialException whenever it cannot do that silently, even with
        // filtering off. Behind an explicit "Sign in with Google" button that
        // reads as a dead end.
        //
        // GetSignInWithGoogleOption is the button's API: it always opens the
        // account picker. Try the smooth path, fall back to the explicit one.
        val response = try {
            credentialManager.getCredential(
                context = activity,
                request = GetCredentialRequest.Builder()
                    .addCredentialOption(
                        GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setAutoSelectEnabled(false)
                            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                            .build(),
                    )
                    .build(),
            )
        } catch (oneTapUnavailable: NoCredentialException) {
            try {
                credentialManager.getCredential(
                    context = activity,
                    request = GetCredentialRequest.Builder()
                        .addCredentialOption(
                            GetSignInWithGoogleOption
                                .Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                                .build(),
                        )
                        .build(),
                )
            } catch (noAccount: NoCredentialException) {
                // Both paths declined: there really is no usable Google account.
                throw IllegalStateException(
                    "No Google account is available on this device. Add one in " +
                        "Settings, or sign up with an email address instead.",
                    noAccount,
                )
            }
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
