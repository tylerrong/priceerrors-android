package app.priceerrors.core.analytics

import android.util.Log
import app.priceerrors.core.auth.AccessTokenProvider
import app.priceerrors.core.network.ApiConfig
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Dual-writes product events to PostHog and Supabase `analytics_events`,
 * matching iOS `GrowthAnalytics.swift`.
 */
class GrowthAnalytics(
    private val httpClient: OkHttpClient,
    private val tokenProvider: AccessTokenProvider,
    private val userIdProvider: () -> String?,
    private val json: Json,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun track(name: String, properties: Map<String, String> = emptyMap()) {
        PostHogAnalytics.capture(name, properties)

        val userId = userIdProvider()?.lowercase()
        if (userId == null) {
            if (properties.isEmpty()) {
                Log.d(TAG, name)
            } else {
                Log.d(TAG, "$name $properties")
            }
            return
        }

        val row = GrowthEventRow(
            userId = userId,
            eventName = name,
            properties = properties,
        )
        scope.launch {
            runCatching {
                val accessToken = tokenProvider.currentAccessToken()
                    ?: throw IOException("No Supabase session for analytics insert.")
                insertEvent(row, accessToken)
            }.onFailure { error ->
                Log.w(TAG, "$name analytics error: ${error.message}")
            }
        }
    }

    fun identify(userId: String) {
        PostHogAnalytics.identify(userId.lowercase())
    }

    fun reset() {
        PostHogAnalytics.reset()
    }

    private fun insertEvent(row: GrowthEventRow, accessToken: String) {
        if (!ApiConfig.isConfigured) return
        val payload = json.encodeToString(row)
        val request = Request.Builder()
            .url("${ApiConfig.supabaseUrl}/rest/v1/analytics_events")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .header("apikey", ApiConfig.supabaseAnonKey)
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .header("Prefer", "return=minimal")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("analytics_events insert failed: HTTP ${response.code}")
            }
        }
    }

    @Serializable
    private data class GrowthEventRow(
        @SerialName("user_id") val userId: String,
        @SerialName("event_name") val eventName: String,
        val properties: Map<String, String>,
    )

    private companion object {
        const val TAG = "GrowthAnalytics"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
