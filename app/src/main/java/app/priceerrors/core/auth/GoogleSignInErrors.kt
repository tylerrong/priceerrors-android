package app.priceerrors.core.auth

import androidx.credentials.exceptions.GetCredentialCancellationException

/**
 * Google Credential Manager reports a signing-certificate mismatch as a
 * cancellation with "[16] Account reauth failed", or as a custom exception with
 * "Developer console is not set up correctly". A real dismiss of the account
 * picker is also a cancellation, so those have to be split by message rather
 * than by exception type alone.
 */
internal class GoogleSignInCancelled : IllegalStateException("Google sign-in was cancelled.")

internal class GooglePlayRegistrationException(cause: Throwable) : IllegalStateException(
    "Google couldn't verify this Play install. The Android OAuth client must use " +
        "package app.priceerrors and the App signing SHA-1 from Google Play.",
    cause,
)

internal class GoogleOAuthPending : IllegalStateException(
    "Finish Google sign-in in the browser, then return to PriceErrors.",
)

internal fun remapGoogleSignInError(error: Throwable): Throwable {
    if (
        error is GoogleSignInCancelled ||
        error is GooglePlayRegistrationException ||
        error is AuthConfigurationException
    ) {
        return error
    }
    val combined = generateSequence(error) { it.cause }
        .mapNotNull { it.message }
        .joinToString("\n")
    val consoleMisconfigured =
        combined.contains("Account reauth failed", ignoreCase = true) ||
            combined.contains("Developer console is not set up correctly", ignoreCase = true) ||
            combined.contains("[28444]", ignoreCase = false)
    if (consoleMisconfigured) {
        return GooglePlayRegistrationException(error)
    }
    if (error is GetCredentialCancellationException) return GoogleSignInCancelled()
    return error
}
