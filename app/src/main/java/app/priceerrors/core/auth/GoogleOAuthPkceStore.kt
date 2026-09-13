package app.priceerrors.core.auth

import android.content.Context
import androidx.core.content.edit
import java.time.Instant

/**
 * Keeps the short-lived PKCE verifier across the browser hand-off and process
 * recreation. The verifier is single-use and expires after ten minutes.
 */
interface GoogleOAuthPkceStore {
    fun save(verifier: String, createdAt: Instant = Instant.now())

    fun consume(now: Instant = Instant.now()): String?

    fun clear()
}

class SharedPreferencesGoogleOAuthPkceStore(context: Context) : GoogleOAuthPkceStore {
    private val preferences = context.getSharedPreferences(
        "priceerrors_google_oauth",
        Context.MODE_PRIVATE,
    )

    override fun save(verifier: String, createdAt: Instant) {
        preferences.edit {
            putString(KEY_VERIFIER, verifier)
            putLong(KEY_CREATED_AT, createdAt.epochSecond)
        }
    }

    override fun consume(now: Instant): String? {
        val verifier = preferences.getString(KEY_VERIFIER, null)
        val createdAt = preferences.getLong(KEY_CREATED_AT, 0L)
        clear()
        if (verifier.isNullOrBlank() || createdAt <= 0L) return null
        if (now.epochSecond - createdAt > MAX_AGE_SECONDS) return null
        return verifier
    }

    override fun clear() {
        preferences.edit { clear() }
    }

    private companion object {
        const val KEY_VERIFIER = "code_verifier"
        const val KEY_CREATED_AT = "created_at"
        const val MAX_AGE_SECONDS = 10 * 60L
    }
}

internal class InMemoryGoogleOAuthPkceStore : GoogleOAuthPkceStore {
    private var verifier: String? = null
    private var createdAt: Instant? = null

    override fun save(verifier: String, createdAt: Instant) {
        this.verifier = verifier
        this.createdAt = createdAt
    }

    override fun consume(now: Instant): String? {
        val value = verifier
        val timestamp = createdAt
        clear()
        if (value == null || timestamp == null) return null
        if (now.epochSecond - timestamp.epochSecond > 10 * 60L) return null
        return value
    }

    override fun clear() {
        verifier = null
        createdAt = null
    }
}
