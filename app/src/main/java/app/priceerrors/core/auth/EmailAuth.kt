package app.priceerrors.core.auth

/**
 * Outcome of an email sign-up.
 *
 * Supabase only returns a session immediately when the project has email
 * confirmation switched off. With it on, the account exists but cannot act until
 * the user clicks the link — so this is modelled explicitly rather than faking a
 * signed-in state the server would reject on the first API call.
 */
sealed interface EmailSignUpResult {
    data class SignedIn(val identity: AuthIdentity, val session: SupabaseSession) : EmailSignUpResult

    data object ConfirmationRequired : EmailSignUpResult
}

/**
 * Email/password failures the auth screen words differently.
 *
 * Supabase reports these as prose in the response body rather than distinct
 * codes, so they are matched on substrings — the same approach the iOS client
 * uses, keeping the two platforms' messages in step.
 */
sealed class EmailAuthError(message: String) : Exception(message) {

    /** Address already has an account — possibly via a social provider. */
    data object AlreadyRegistered :
        EmailAuthError("This email is already registered. Try signing in instead.")

    data object InvalidCredentials : EmailAuthError("Incorrect email or password.")

    data object EmailNotConfirmed :
        EmailAuthError("Confirm your email before signing in — check your inbox.")

    /** The account exists but was created with Google or Apple, not a password. */
    data object UseSocialSignIn :
        EmailAuthError("This account was created with Google. Use the button above.")

    data object WeakPassword : EmailAuthError("Password must be at least 6 characters.")

    companion object {
        /**
         * Maps a Supabase auth error body onto the cases above. Mirrors the
         * substring checks in the iOS client so both platforms classify the same
         * server response identically.
         */
        fun from(payload: String): EmailAuthError? {
            val text = payload.lowercase()
            return when {
                text.contains("already registered") ||
                    text.contains("already been registered") ||
                    text.contains("user already exists") -> AlreadyRegistered

                text.contains("email not confirmed") ||
                    text.contains("not confirmed") -> EmailNotConfirmed

                text.contains("password should be") ||
                    text.contains("weak password") -> WeakPassword

                // Supabase says "Invalid login credentials" both for a wrong
                // password and for an address that only has a social identity,
                // so this must stay below the more specific checks.
                text.contains("invalid login credentials") ||
                    text.contains("invalid_grant") ||
                    text.contains("invalid credentials") -> InvalidCredentials

                else -> null
            }
        }
    }
}
