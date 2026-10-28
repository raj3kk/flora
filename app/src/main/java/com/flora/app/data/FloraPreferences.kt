package com.flora.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import com.flora.app.core.FloraConfig
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
        val AUTO_MODE = booleanPreferencesKey("auto_mode")
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
        val MODELS_MANIFEST_URL = stringPreferencesKey("models_manifest_url")
        val DEEPGRAM_API_KEY = stringPreferencesKey("deepgram_api_key")
        val VOICE_MATCH_THRESHOLD = stringPreferencesKey("voice_match_threshold")
        val LEARNED_CONTACTS = stringPreferencesKey("learned_contacts_json")
        val GEMINI_API_KEY2 = stringPreferencesKey("gemini_api_key_2")
        val GROQ_API_KEY2 = stringPreferencesKey("groq_api_key_2")
        val TAVILY_API_KEY = stringPreferencesKey("tavily_api_key")
        val BRAVE_API_KEY = stringPreferencesKey("brave_api_key")
        val SERPAPI_API_KEY = stringPreferencesKey("serpapi_api_key")
        val MAPS_API_KEY = stringPreferencesKey("maps_api_key")
        val FCM_TOKEN = stringPreferencesKey("fcm_token")
        val WAKE_ENABLED = booleanPreferencesKey("wake_enabled")
        val WAKE_THRESHOLD = floatPreferencesKey("wake_threshold")
        val LINKED_USER_ID = stringPreferencesKey("linked_user_id")
        val LINKED_EMAIL = stringPreferencesKey("linked_email")
        val LINKED_REFRESH_TOKEN = stringPreferencesKey("linked_refresh_token")
        val CALL_TALK = booleanPreferencesKey("call_talk")
        val HOME_BOX_CODE = stringPreferencesKey("home_box_code")
        val AVATAR = stringPreferencesKey("avatar")
        val TALK_DATE = stringPreferencesKey("talk_date")
        val TALK_SECONDS = longPreferencesKey("talk_seconds")
        val PERMISSIONS_GATE_DONE = booleanPreferencesKey("permissions_gate_done")
        val CONTINUOUS_MIC = booleanPreferencesKey("continuous_mic")
        val VOICE_VIEW = booleanPreferencesKey("voice_view")
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

    // App-link (v2.8, design FLORA_APP_LINK_DESIGN.md): website
    // account (Auth uid) link ho to wahi identity use hoti hai,
    // warna purana random uuid. Refresh token sirf is phone ke
    // DataStore me rahta hai — kabhi server/log/backup par nahi
    // jata. Access token kabhi save nahi hota (memory me hi).
    suspend fun linkedUserId(): String =
        context.floraDataStore.data.map { it[Keys.LINKED_USER_ID] ?: "" }.first()

    suspend fun linkedEmail(): String =
        context.floraDataStore.data.map { it[Keys.LINKED_EMAIL] ?: "" }.first()

    suspend fun effectiveUserId(): String {
        val linked = linkedUserId()
        return if (linked.isNotEmpty()) linked else userId()
    }

    suspend fun setLink(uid: String, email: String, refreshToken: String) {
        context.floraDataStore.edit {
            it[Keys.LINKED_USER_ID] = uid
            it[Keys.LINKED_EMAIL] = email
            it[Keys.LINKED_REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun clearLink() {
        context.floraDataStore.edit {
            it.remove(Keys.LINKED_USER_ID)
            it.remove(Keys.LINKED_EMAIL)
            it.remove(Keys.LINKED_REFRESH_TOKEN)
        }
    }

    // User ki Settings key ho to wahi pehle chalegi; warna app ka
    // default publishable key (FloraConfig me, public by design)
    // use hoga — user ko key daalni hi nahi padegi.
    suspend fun supabaseAnonKey(): String {
        val stored = context.floraDataStore.data.map { it[Keys.SUPABASE_ANON_KEY] ?: "" }.first()
        return stored.ifEmpty { FloraConfig.DEFAULT_SUPABASE_ANON_KEY }
    }

    /** Models manifest override (v2.21): khali = Flora default server. */
    suspend fun modelsManifestUrl(): String =
        context.floraDataStore.data.map { it[Keys.MODELS_MANIFEST_URL] ?: "" }.first()

    suspend fun setModelsManifestUrl(value: String) {
        context.floraDataStore.edit { it[Keys.MODELS_MANIFEST_URL] = value }
    }

    /** Deepgram key (mega build, user faisla: Deepgram hi rahega).
     *  Khali = system STT fallback; key aate hi sachhi barge-in. */
    suspend fun deepgramApiKey(): String =
        context.floraDataStore.data.map { it[Keys.DEEPGRAM_API_KEY] ?: "" }.first()

    /** Home box code (v2.22, P5-F22 phone half): box ka join code
     *  sirf phone par save hota hai. Server sync (S4) abhi baaki
     *  hai — code save hona control nahi hai. */
    suspend fun homeBoxCode(): String =
        context.floraDataStore.data.map { it[Keys.HOME_BOX_CODE] ?: "" }.first()

    suspend fun setHomeBoxCode(value: String) {
        context.floraDataStore.edit { it[Keys.HOME_BOX_CODE] = value }
    }

    /** Avatar style id (v2.22, S2): gulabi/neela/hara/baingani/
     *  sunehra. Galat id aaye to gulabi (default) maana jaata hai. */
    suspend fun avatar(): String =
        context.floraDataStore.data.map { it[Keys.AVATAR] ?: "gulabi" }.first()

    suspend fun setAvatar(value: String) {
        context.floraDataStore.edit { it[Keys.AVATAR] = value }
    }

    suspend fun setDeepgramApiKey(value: String) {
        context.floraDataStore.edit { it[Keys.DEEPGRAM_API_KEY] = value }
    }

    /** Voice-match threshold — 0.50 STARTING GUESS hai, lock nahi
     *  (validation: same-speaker min 0.558, noise max 0.380). */
    suspend fun voiceMatchThreshold(): Float =
        context.floraDataStore.data.map { it[Keys.VOICE_MATCH_THRESHOLD] ?: "0.50" }.first().toFloatOrNull() ?: 0.50f

    suspend fun setVoiceMatchThreshold(value: Float) {
        context.floraDataStore.edit { it[Keys.VOICE_MATCH_THRESHOLD] = value.toString() }
    }

    /** Seekhe hue naam→number mapping (v2.22): user ne ek baar
     *  bataya ("Home matlab ye number") to dobara nahi poochha
     *  jayega. Sirf phone par, kabhi server par nahi. */
    suspend fun learnedContactNumber(name: String): String {
        return try {
            val json = context.floraDataStore.data.map { it[Keys.LEARNED_CONTACTS] ?: "" }.first()
            if (json.isEmpty()) "" else org.json.JSONObject(json).optString(name.trim().lowercase(), "")
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun learnContact(name: String, number: String) {
        try {
            val current = context.floraDataStore.data.map { it[Keys.LEARNED_CONTACTS] ?: "" }.first()
            val json = if (current.isEmpty()) org.json.JSONObject() else org.json.JSONObject(current)
            json.put(name.trim().lowercase(), number)
            context.floraDataStore.edit { it[Keys.LEARNED_CONTACTS] = json.toString() }
        } catch (e: Exception) {
        }
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

    // Wake Word toggle (v2.4): default OFF. ON = foreground service
    // mic se sunta hai; OFF karte hi service ruk jati hai.
    suspend fun wakeEnabled(): Boolean =
        context.floraDataStore.data.map { it[Keys.WAKE_ENABLED] ?: false }.first()

    suspend fun setWakeEnabled(value: Boolean) {
        context.floraDataStore.edit { it[Keys.WAKE_ENABLED] = value }
    }

    // Wake sensitivity (v2.22, chunk 6): 0.60 default (prototype
    // positives 0.732-0.896, noise ≤0.314). Kam = 0.70 (kam galat
    // trigger, saaf awaaz chahiye); Zyada = 0.50 (aasani se
    // sunega, galat trigger zyada ho sakta hai). Service har
    // window par ye value padhta hai — restart ki zaroorat nahi.
    suspend fun wakeThreshold(): Float =
        context.floraDataStore.data.map { it[Keys.WAKE_THRESHOLD] ?: 0.60f }.first()

    suspend fun setWakeThreshold(value: Float) {
        context.floraDataStore.edit { it[Keys.WAKE_THRESHOLD] = value }
    }

    suspend fun automationConsent(): Boolean =
        context.floraDataStore.data.map { it[Keys.AUTOMATION_CONSENT] ?: false }.first()

    /**
     * Auto mode (v2.17 user switch; v2.18 user order se DEFAULT ON):
     * poora Flora Auto Mode me hi rahega — bolte hi kaam shuru,
     * bina har baar poochhe. Band karne ke liye switch OFF ya
     * "auto mode band" bolo. Payment isse kabhi auto nahi hogi.
     */
    suspend fun autoMode(): Boolean =
        context.floraDataStore.data.map { it[Keys.AUTO_MODE] ?: true }.first()

    suspend fun setAutoMode(value: Boolean) {
        context.floraDataStore.edit { it[Keys.AUTO_MODE] = value }
    }

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

    /** Permission gate (v2.12): pehli baar app khulte hi sab
     * permissions ek saath maange jaate hain; gate poora hone ke
     * baad ye flag true ho jata hai aur dobara gate nahi khulta
     * (Settings se "Permissions dobara dekho" se phir khul sakta
     * hai). Deny ki hui permission feature ko band karti hai,
     * poora app kabhi band nahi karti. */
    suspend fun permissionsGateDone(): Boolean =
        context.floraDataStore.data.map { it[Keys.PERMISSIONS_GATE_DONE] ?: false }.first()

    suspend fun setPermissionsGateDone(value: Boolean) {
        context.floraDataStore.edit { it[Keys.PERMISSIONS_GATE_DONE] = value }
    }

    /** Continuous mic (v2.14): default ON — app khulte hi mic chalu,
     * mute dabane tak sunta rahe. Mute ka faisla user ka hi rahega. */
    suspend fun continuousMic(): Boolean =
        context.floraDataStore.data.map { it[Keys.CONTINUOUS_MIC] ?: true }.first()

    suspend fun setContinuousMic(value: Boolean) {
        context.floraDataStore.edit { it[Keys.CONTINUOUS_MIC] = value }
    }

    /** Voice-first Baat view (v2.15): default ON — app khulte hi
     * baat-cheet wali screen (orb + mic), likha hua chat nahi.
     * Chat sirf tab jab user khud "Chat kholo" dabaye. */
    suspend fun voiceView(): Boolean =
        context.floraDataStore.data.map { it[Keys.VOICE_VIEW] ?: true }.first()

    suspend fun setVoiceView(value: Boolean) {
        context.floraDataStore.edit { it[Keys.VOICE_VIEW] = value }
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
