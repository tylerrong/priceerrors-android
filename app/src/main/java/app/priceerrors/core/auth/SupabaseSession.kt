package app.priceerrors.core.auth

import android.content.Context
import androidx.core.content.edit
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A Supabase-authenticated session. [accessToken] is the JWT the PriceErrors
 * server verifies on every user-scoped route; [refreshToken] outlives it and is
 * used to mint replacements without another Google prompt.
 */
data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
    val userId: String,
    val email: String?,
) {
    /**
     * Treated as expired a minute early so a token cannot lapse in flight
     * between the check and the server receiving it.
     */
    fun isExpired(now: Instant = Instant.now()): Boolean =
        !now.isBefore(expiresAt.minusSeconds(60))
}

/** Wire shape of Supabase's `/auth/v1/token` response. */
@Serializable
internal data class SupabaseTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long = 3600,
    val user: SupabaseUserDto? = null,
)

@Serializable
internal data class SupabaseUserDto(
    val id: String,
    val email: String? = null,
    @SerialName("user_metadata") val userMetadata: SupabaseUserMetadataDto? = null,
)

/** iOS writes the signer's name here at sign-up; read it back for display. */
@Serializable
internal data class SupabaseUserMetadataDto(
    @SerialName("full_name") val fullName: String? = null,
)

/**
 * Wire shape of `/auth/v1/signup`. Unlike the token endpoint this returns the
 * bare user object — with no tokens — when the project requires email
 * confirmation, so every session field has to be optional.
 */
@Serializable
internal data class SupabaseSignUpResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 3600,
    val user: SupabaseUserDto? = null,
    // Confirmation-required responses put the user at the top level.
    val id: String? = null,
    val email: String? = null,
)

/** Where a [SupabaseSession] is kept between launches. */
interface SessionStorage {
    fun load(): SupabaseSession?

    fun save(session: SupabaseSession)

    fun clear()
}

/**
 * Persists the session across launches.
 *
 * Stored in the app's private SharedPreferences, which is sandboxed per-app and
 * excluded from backup (see `backup_rules.xml`) so a restored device does not
 * carry someone else's token.
 */
class SupabaseSessionStore(context: Context) : SessionStorage {
    private val preferences = context.getSharedPreferences(
        "priceerrors_session",
        Context.MODE_PRIVATE,
    )

    override fun load(): SupabaseSession? {
        val accessToken = preferences.getString(KEY_ACCESS, null) ?: return null
        val refreshToken = preferences.getString(KEY_REFRESH, null) ?: return null
        val userId = preferences.getString(KEY_USER_ID, null) ?: return null
        val expiresAtEpoch = preferences.getLong(KEY_EXPIRES_AT, 0L)
        if (expiresAtEpoch <= 0L) return null

        return SupabaseSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = Instant.ofEpochSecond(expiresAtEpoch),
            userId = userId,
            email = preferences.getString(KEY_EMAIL, null),
        )
    }

    override fun save(session: SupabaseSession) {
        preferences.edit {
            putString(KEY_ACCESS, session.accessToken)
            putString(KEY_REFRESH, session.refreshToken)
            putLong(KEY_EXPIRES_AT, session.expiresAt.epochSecond)
            putString(KEY_USER_ID, session.userId)
            putString(KEY_EMAIL, session.email)
        }
    }

    override fun clear() {
        preferences.edit { clear() }
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
    }
}
