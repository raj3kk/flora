package com.flora.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import com.flora.app.core.FloraConfig
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
        val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        val AUTOMATION_CONSENT = booleanPreferencesKey("automation_consent")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val GUARDIAN_MODE = booleanPreferencesKey("guardian_mode")
        val FACE_LOCK = booleanPreferencesKey("face_lock")
        val ADMIN_UPI_VPA = stringPreferencesKey("admin_upi_vpa")
        val LAST_UTR = stringPreferencesKey("last_utr")
        val CALL_ANNOUNCE = booleanPreferencesKey("call_announce")
        val EVENT_ANNOUNCE = booleanPreferencesKey("event_announce")
        val DRIVING_MODE = booleanPreferencesKey("driving_mode")
        val PERSONA = stringPreferencesKey("persona")
        val THEME = stringPreferencesKey("theme")
        val GEMINI_API_KEY2 = stringPreferencesKey("gemini_api_key_2")
        val GROQ_API_KEY2 = stringPreferencesKey("groq_api_key_2")
        val TAVILY_API_KEY = stringPreferencesKey("tavily_api_key")
        val BRAVE_API_KEY = stringPreferencesKey("brave_api_key")
        val SERPAPI_API_KEY = stringPreferencesKey("serpapi_api_key")
        val MAPS_API_KEY = stringPreferencesKey("maps_api_key")
        val FCM_TOKEN = stringPreferencesKey("fcm_token")
        val CALL_TALK = booleanPreferencesKey("call_talk")
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

    // User ki Settings key ho to wahi pehle chalegi; warna app ka
    // default publishable key (FloraConfig me, public by design)
    // use hoga — user ko key daalni hi nahi padegi.
    suspend fun supabaseAnonKey(): String {
        val stored = context.floraDataStore.data.map { it[Keys.SUPABASE_ANON_KEY] ?: "" }.first()
        return stored.ifEmpty { FloraConfig.DEFAULT_SUPABASE_ANON_KEY }
    }

    suspend fun setSupabaseAnonKey(value: String) {
        context.floraDataStore.edit { it[Keys.SUPABASE_ANON_KEY] = value }
    }

    suspend fun geminiApiKey(): String =
        context.floraDataStore.data.map { it[Keys.GEMINI_API_KEY] ?: "" }.first()

    suspend fun setGeminiApiKey(value: String) {
        context.floraDataStore.edit { it[Keys.GEMINI_API_KEY] = value }
    }

    suspend fun groqApiKey(): String =
        context.floraDataStore.data.map { it[Keys.GROQ_API_KEY] ?: "" }.first()

    suspend fun setGroqApiKey(value: String) {
        context.floraDataStore.edit { it[Keys.GROQ_API_KEY] = value }
    }

    /** Optional second keys + optional search keys (v2.1): khali = lane band. */
    suspend fun geminiApiKey2(): String =
        context.floraDataStore.data.map { it[Keys.GEMINI_API_KEY2] ?: "" }.first()

    suspend fun setGeminiApiKey2(value: String) {
        context.floraDataStore.edit { it[Keys.GEMINI_API_KEY2] = value }
    }

    suspend fun groqApiKey2(): String =
        context.floraDataStore.data.map { it[Keys.GROQ_API_KEY2] ?: "" }.first()

    suspend fun setGroqApiKey2(value: String) {
        context.floraDataStore.edit { it[Keys.GROQ_API_KEY2] = value }
    }

    suspend fun tavilyApiKey(): String =
        context.floraDataStore.data.map { it[Keys.TAVILY_API_KEY] ?: "" }.first()

    suspend fun setTavilyApiKey(value: String) {
        context.floraDataStore.edit { it[Keys.TAVILY_API_KEY] = value }
    }

    suspend fun braveApiKey(): String =
        context.floraDataStore.data.map { it[Keys.BRAVE_API_KEY] ?: "" }.first()

    suspend fun setBraveApiKey(value: String) {
        context.floraDataStore.edit { it[Keys.BRAVE_API_KEY] = value }
    }

    suspend fun serpApiKey(): String =
        context.floraDataStore.data.map { it[Keys.SERPAPI_API_KEY] ?: "" }.first()

    suspend fun setSerpApiKey(value: String) {
        context.floraDataStore.edit { it[Keys.SERPAPI_API_KEY] = value }
    }

    suspend fun mapsApiKey(): String =
        context.floraDataStore.data.map { it[Keys.MAPS_API_KEY] ?: "" }.first()

    suspend fun setMapsApiKey(value: String) {
        context.floraDataStore.edit { it[Keys.MAPS_API_KEY] = value }
    }

    // FCM device token (v2.2): sirf is phone par DataStore me rahega;
    // kabhi log, chat, backup ya message me nahi jayega. Server sync
    // tabhi hoga jab push_tokens table + server function taiyaar ho.
    suspend fun fcmToken(): String =
        context.floraDataStore.data.map { it[Keys.FCM_TOKEN] ?: "" }.first()

    suspend fun setFcmToken(value: String) {
        context.floraDataStore.edit { it[Keys.FCM_TOKEN] = value }
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

    /** Guardian mode: OFF by default. ON = sensitive actions need a fresh PIN check. */
    suspend fun guardianMode(): Boolean =
        context.floraDataStore.data.map { it[Keys.GUARDIAN_MODE] ?: false }.first()

    suspend fun setGuardianMode(value: Boolean) {
        context.floraDataStore.edit { it[Keys.GUARDIAN_MODE] = value }
    }

    /** Face Lock toggle: OFF by default. ON = Chat PIN verify ke bina nahi khulega. */
    suspend fun faceLock(): Boolean =
        context.floraDataStore.data.map { it[Keys.FACE_LOCK] ?: false }.first()

    suspend fun setFaceLock(value: Boolean) {
        context.floraDataStore.edit { it[Keys.FACE_LOCK] = value }
    }

    /** Admin UPI VPA (owner enters it; customers pay to this VPA). Empty = not set yet. */
    suspend fun adminUpiVpa(): String =
        context.floraDataStore.data.map { it[Keys.ADMIN_UPI_VPA] ?: "" }.first()

    suspend fun setAdminUpiVpa(value: String) {
        context.floraDataStore.edit { it[Keys.ADMIN_UPI_VPA] = value }
    }

    /** Last UTR the user entered for Pro activation (local note only; admin verifies it). */
    suspend fun lastUtr(): String =
        context.floraDataStore.data.map { it[Keys.LAST_UTR] ?: "" }.first()

    suspend fun setLastUtr(value: String) {
        context.floraDataStore.edit { it[Keys.LAST_UTR] = value }
    }

    /** Call announcement toggle (video #65 ka Flora version). Default OFF. */
    suspend fun callAnnounce(): Boolean =
        context.floraDataStore.data.map { it[Keys.CALL_ANNOUNCE] ?: false }.first()

    suspend fun setCallAnnounce(value: Boolean) {
        context.floraDataStore.edit { it[Keys.CALL_ANNOUNCE] = value }
    }

    /** Event announcement toggle (charger/battery). Default OFF. */
    suspend fun eventAnnounce(): Boolean =
        context.floraDataStore.data.map { it[Keys.EVENT_ANNOUNCE] ?: false }.first()

    suspend fun setEventAnnounce(value: Boolean) {
        context.floraDataStore.edit { it[Keys.EVENT_ANNOUNCE] = value }
    }

    suspend fun drivingMode(): Boolean =
        context.floraDataStore.data.map { it[Keys.DRIVING_MODE] ?: false }.first()

    suspend fun setDrivingMode(value: Boolean) {
        context.floraDataStore.edit { it[Keys.DRIVING_MODE] = value }
    }

    /** Persona (v1.8): Flora ke apne tone — flora/saathi/guru. Default flora. */
    suspend fun persona(): String =
        context.floraDataStore.data.map { it[Keys.PERSONA] ?: "flora" }.first()

    suspend fun setPersona(value: String) {
        context.floraDataStore.edit { it[Keys.PERSONA] = value }
    }

    /** Theme (v1.9): light/dark/warm. Default light. */
    suspend fun theme(): String =
        context.floraDataStore.data.map { it[Keys.THEME] ?: "light" }.first()

    suspend fun setTheme(value: String) {
        context.floraDataStore.edit { it[Keys.THEME] = value }
    }

    /** Call-talk (dialer role): default OFF, user khud ON karega. */
    suspend fun callTalk(): Boolean =
        context.floraDataStore.data.map { it[Keys.CALL_TALK] ?: false }.first()

    suspend fun setCallTalk(value: Boolean) {
        context.floraDataStore.edit { it[Keys.CALL_TALK] = value }
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
