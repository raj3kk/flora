package com.flora.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.floraDataStore: DataStore<Preferences> by preferencesDataStore(name = "flora_prefs")

/**
 * On-device settings only. No password, no API key value is ever written
 * to a file, a backup or a server by this class — keys live in DataStore
 * on this phone only, and the PIN lives only in PinVault (KeyStore).
 */
class FloraPreferences(private val context: Context) {

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val SUPABASE_ANON_KEY = stringPreferencesKey("supabase_anon_key")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val AUTOMATION_CONSENT = booleanPreferencesKey("automation_consent")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val TALK_DATE = stringPreferencesKey("talk_date")
        val TALK_SECONDS = longPreferencesKey("talk_seconds")
    }

    suspend fun userId(): String {
        val existing = context.floraDataStore.data.map { it[Keys.USER_ID] }.first()
        if (existing != null) return existing
        val fresh = UUID.randomUUID().toString()
        context.floraDataStore.edit { it[Keys.USER_ID] = fresh }
        return fresh
    }

    suspend fun deviceId(androidId: String): String {
        val existing = context.floraDataStore.data.map { it[Keys.DEVICE_ID] }.first()
        if (existing != null) return existing
        context.floraDataStore.edit { it[Keys.DEVICE_ID] = androidId }
        return androidId
    }

    suspend fun supabaseAnonKey(): String =
        context.floraDataStore.data.map { it[Keys.SUPABASE_ANON_KEY] ?: "" }.first()

    suspend fun setSupabaseAnonKey(value: String) {
        context.floraDataStore.edit { it[Keys.SUPABASE_ANON_KEY] = value }
    }

    suspend fun geminiApiKey(): String =
        context.floraDataStore.data.map { it[Keys.GEMINI_API_KEY] ?: "" }.first()

    suspend fun setGeminiApiKey(value: String) {
        context.floraDataStore.edit { it[Keys.GEMINI_API_KEY] = value }
    }

    suspend fun automationConsent(): Boolean =
        context.floraDataStore.data.map { it[Keys.AUTOMATION_CONSENT] ?: false }.first()

    suspend fun setAutomationConsent(value: Boolean) {
        context.floraDataStore.edit { it[Keys.AUTOMATION_CONSENT] = value }
    }

    suspend fun pinEnabled(): Boolean =
        context.floraDataStore.data.map { it[Keys.PIN_ENABLED] ?: false }.first()

    suspend fun setPinEnabled(value: Boolean) {
        context.floraDataStore.edit { it[Keys.PIN_ENABLED] = value }
    }

    /** Talk seconds used on [today] (yyyy-MM-dd). Rolls over automatically. */
    suspend fun talkSecondsUsed(today: String): Long {
        val prefs = context.floraDataStore.data.first()
        return if (prefs[Keys.TALK_DATE] == today) prefs[Keys.TALK_SECONDS] ?: 0L else 0L
    }

    suspend fun addTalkSeconds(today: String, seconds: Long) {
        context.floraDataStore.edit { prefs ->
            val used = if (prefs[Keys.TALK_DATE] == today) prefs[Keys.TALK_SECONDS] ?: 0L else 0L
            prefs[Keys.TALK_DATE] = today
            prefs[Keys.TALK_SECONDS] = used + seconds
        }
    }
}
