package com.flora.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import com.flora.app.core.FloraConfig
import com.flora.app.security.GuardianGate
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
 * to a backup or a server by this class — keys live in DataStore
 * on this phone only, and the PIN lives only in PinVault (KeyStore).
 * Cycle 1, APP-C1-005: user keys, linked refresh token, licence
 * key aur FCM token ab DataStore me KeyStore AES/GCM se
 * encrypted save hote hain (MemoryCrypto pattern — wahi alias
 * family, wahi IV+ciphertext Base64 shape). Non-sensitive
 * prefs (toggles, theme, counters) waisi hi plaintext rehti
 * hain. Supabase anon key publishable hai by design, isliye
 * wo is encryption me nahi hai.
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
        // Cycle 3, F1 (d): Face Lock unlock session ka prefs
        // nishaan — sirf BackgroundMicService ke liye, kyunki
        // service FloraApp ka composition state (chatUnlocked)
        // dekh hi nahi sakti. FloraApp unlock par ise set karta
        // hai (timeout tak valid), timeout/app-start par clear
        // karta hai. Ye toggle (FACE_LOCK) nahi hai — sirf
        // "is waqt unlock hai ya nahi" ka waqt-nishaan hai.
        val FACE_LOCK_UNLOCKED_UNTIL = longPreferencesKey("face_lock_unlocked_until_ms")
        val ADMIN_UPI_VPA = stringPreferencesKey("admin_upi_vpa")
        val LAST_UTR = stringPreferencesKey("last_utr")
        val LICENCE_KEY = stringPreferencesKey("licence_key")
        // Wave B (LFMS): admin se mili licence key (FLORA-XXXX
        // format) — user ki apni key, sirf isi phone par, app ke
        // private prefs me (LAST_UTR jaisi hi jagah). Server par
        // key plaintext-with-controls design hai (jab mann tab
        // copy chahiye, isliye hash nahi) — phone par bhi key
        // sirf is app ke private storage me rehti hai, kabhi
        // log/chat/backup me nahi jati. Unlink par clear NAHI
        // hoti (key account link se alag hai — device-bound
        // licence hai).
        val CALL_ANNOUNCE = booleanPreferencesKey("call_announce")
        val EVENT_ANNOUNCE = booleanPreferencesKey("event_announce")
        // Lane A — A1 (R-A1-4): battery no-repeat state — aakhri warned
        // level/state key ("low"/"critical"/"charging"/"full"/"normal").
        // EVENT_ANNOUNCE pattern par DataStore key (reboot-persistent).
        val BATTERY_LAST_WARNED = stringPreferencesKey("battery_last_warned")
        val DRIVING_MODE = booleanPreferencesKey("driving_mode")
        val PERSONA = stringPreferencesKey("persona")
        val VOICE_SAMPLE = stringPreferencesKey("voice_sample")
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
        // Phase 8 link fix: server row confirm hui ya nahi. Sirf
        // tab true jab push-token-sync ne ok:true diya ho — isse
        // pehle "Linked" UI kabhi nahi dikhega (pending dikhega).
        val LINK_SERVER_CONFIRMED = booleanPreferencesKey("link_server_confirmed")
        // Phase 8 feature flags: aakhri mili hui flags ka JSON
        // (offline me wahi chalti hain; fail-open bhi yahi se).
        val FEATURE_FLAGS_JSON = stringPreferencesKey("feature_flags_json")
        val CALL_TALK = booleanPreferencesKey("call_talk")
        val HOME_BOX_CODE = stringPreferencesKey("home_box_code")
        val PC_PAIR_CODE = stringPreferencesKey("pc_pair_code")
        val AVATAR = stringPreferencesKey("avatar")
        val TALK_DATE = stringPreferencesKey("talk_date")
        val TALK_SECONDS = longPreferencesKey("talk_seconds")
        val PERMISSIONS_GATE_DONE = booleanPreferencesKey("permissions_gate_done")
        val CONTINUOUS_MIC = booleanPreferencesKey("continuous_mic")
        val VOICE_VIEW = booleanPreferencesKey("voice_view")
        // Cycle 1, APP-C1-009 (home sync state): Home screen se
        // server sync safal hua ya nahi — awaaz wale jawab isi
        // se "sync baaki hai" ka sach bolenge.
        val HOME_SERVER_SYNCED = booleanPreferencesKey("home_server_synced")
    }

    /**
     * APP-C1-005 secret helpers (MemoryCrypto pattern):
     * - readSecret: ciphertext mile to decrypt karke do; purana
     *   plaintext mile to ek baar encrypted save karo (migration)
     *   aur wahi value do — phir plaintext DataStore me nahi
     *   bachega.
     * - Cycle 2, APP-C1-024: decrypt fail par farq karo. (a)
     *   Raw legacy plaintext lage (printable, provider-key
     *   format jaisa, ciphertext-blob shape ka nahi) to migrate
     *   karo (encrypt + return, Cycle 1 wali migration
     *   barkaraar). (b) Doosre phone ka ciphertext blob lage
     *   (Base64 blob shape: lamba, sirf Base64 chars, decoded
     *   size IV+GCM tag se zyada) to KHALI do + stored blob ko
     *   bhi khali karo — re-wrap mat karo, blob ko key maan
     *   kar caller ko mat do. Khali = maujooda missing-key UI
     *   ka re-enter signal hai. Koi key value kabhi log nahi
     *   hoti (yahan sirf shape check hota hai, value nahi).
     * - writeSecret (Cycle 3, F3 — fail-closed): encrypt karke
     *   save karo; encrypt fail ho to KUCH NAHI likha jata
     *   (plaintext fallback HATA DIYA gaya hai) aur false
     *   wapas aata hai — purani saved value waisi hi rehti
     *   hai. Khali ho to key hata do.
     * KeyStore key isi phone + isi app se bandhi hai — naye
     * phone/restore par purana ciphertext nahi khulega (PIN
     * vault jaisa hi niyam); tab user ko key dobara daalni hogi.
     * Saare secret readers/writers inhi do helpers se jaate
     * hain — DataStore keys ko seedha koi aur file nahi chhuti.
     */
    private suspend fun readSecret(key: Preferences.Key<String>): String {
        val raw = context.floraDataStore.data.map { it[key] ?: "" }.first()
        if (raw.isEmpty()) return ""
        val decrypted = try {
            MemoryCrypto.decrypt(raw)
        } catch (e: Exception) {
            null
        }
        if (decrypted != null) return decrypted
        if (looksLikeForeignCiphertext(raw)) {
            // Doosre phone ka blob: is phone ki KeyStore se kabhi
            // nahi khulega — ise key maan kar caller ko dena galat
            // hoga. Stored blob hatao, khali = re-enter signal.
            context.floraDataStore.edit { it.remove(key) }
            return ""
        }
        try {
            val encrypted = MemoryCrypto.encrypt(raw)
            if (encrypted != null) {
                context.floraDataStore.edit { it[key] = encrypted }
            }
        } catch (e: Exception) {
        }
        return raw
    }

    /**
     * APP-C1-024 blob-shape check (sirf shape, value kabhi
     * log/compare nahi hoti): MemoryCrypto blob = IV (12) +
     * ciphertext + GCM tag (16), Base64 NO_WRAP — isliye lamba,
     * sirf Base64 chars, length 4 ka multiple, decoded size
     * kam se kam 28. Legacy plaintext keys me aam taur par
     * prefix/separator (jaise '_' '-' '.') hota hai ya length
     * is shape se match nahi karti — wo migrate raaste par
     * jayengi, blob raaste par nahi.
     */
    private fun looksLikeForeignCiphertext(raw: String): Boolean {
        if (raw.length < 44 || raw.length % 4 != 0) return false
        if (!raw.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '+' || it == '/' || it == '=' }) return false
        return try {
            val decoded = android.util.Base64.decode(raw, android.util.Base64.NO_WRAP)
            decoded.size >= 28
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Cycle 3, F3: fail-closed write. Encrypt fail ho to false
     * (kuch nahi likha, purani value waisi hi). Saare setters
     * isi se jaate hain, isliye koi secret kabhi plaintext
     * DataStore me nahi jata. Caller jo return value dekhe
     * (jaise setLink) wahi user ko imaandaar fail message de
     * sakta hai; baaki setters ka vyavhaar (Unit call) waisa
     * hi hai, sirf plaintext likhna band hua hai.
     */
    private suspend fun writeSecret(key: Preferences.Key<String>, value: String): Boolean {
        if (value.isEmpty()) {
            context.floraDataStore.edit { it.remove(key) }
            return true
        }
        val stored = try {
            MemoryCrypto.encrypt(value)
        } catch (e: Exception) {
            null
        } ?: return false
        context.floraDataStore.edit { it[key] = stored }
        return true
    }

    /** APP-C1-009: Home server sync hua ya nahi (plain flag). */
    suspend fun homeServerSynced(): Boolean =
        context.floraDataStore.data.map { it[Keys.HOME_SERVER_SYNCED] ?: false }.first()

    suspend fun setHomeServerSynced(value: Boolean) {
        context.floraDataStore.edit { it[Keys.HOME_SERVER_SYNCED] = value }
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

    /** Linked account ka refresh token (sirf is phone ke DataStore
     *  me, APP-C1-005 se KeyStore-encrypted; app-start session
     *  renew ke liye. Khali = link nahi hai. */
    suspend fun linkedRefreshToken(): String =
        readSecret(Keys.LINKED_REFRESH_TOKEN)

    suspend fun effectiveUserId(): String {
        val linked = linkedUserId()
        return if (linked.isNotEmpty()) linked else userId()
    }

    /**
     * Cycle 3, F3: fail-closed link save. Refresh token encrypt
     * na ho to POORA link save nahi hota (uid/email bhi nahi —
     * aadha link "linked" dikha kar token ke bina chhod dena
     * galat hoga) aur false wapas aata hai; caller tab
     * imaandaar "save nahi ho paya, dobara koshish karo"
     * message dega. Refresh-renew callers return value ignore
     * karte hain (waisa hi) — token save na ho to purana token
     * waisa hi rehta hai, agla renew dobara koshish karega.
     * Khali token = token hatao (purana vyavhaar waisa hi).
     */
    suspend fun setLink(uid: String, email: String, refreshToken: String): Boolean {
        // APP-C1-005: refresh token encrypted save hota hai.
        val encRefresh = if (refreshToken.isEmpty()) "" else
            (try { MemoryCrypto.encrypt(refreshToken) } catch (e: Exception) { null }
                ?: return false)
        context.floraDataStore.edit {
            it[Keys.LINKED_USER_ID] = uid
            it[Keys.LINKED_EMAIL] = email
            if (encRefresh.isEmpty()) it.remove(Keys.LINKED_REFRESH_TOKEN)
            else it[Keys.LINKED_REFRESH_TOKEN] = encRefresh
            // APP-C1-020: naya link = naya account — purana
            // "home sync ho gaya" daava yahan nahi chalega.
            // linkServerConfirmed wala pattern: fresh link par
            // ye flag false hota hai.
            it[Keys.HOME_SERVER_SYNCED] = false
        }
        return true
    }

    suspend fun clearLink() {
        context.floraDataStore.edit {
            it.remove(Keys.LINKED_USER_ID)
            it.remove(Keys.LINKED_EMAIL)
            it.remove(Keys.LINKED_REFRESH_TOKEN)
            it.remove(Keys.LINK_SERVER_CONFIRMED)
            // APP-C1-020: account badla/hataya to purana
            // "home sync ho gaya" daava bhi khatam.
            it[Keys.HOME_SERVER_SYNCED] = false
        }
    }

    /** Phase 8: server ne users row banayi (sync ok:true) ya nahi. */
    suspend fun linkServerConfirmed(): Boolean =
        context.floraDataStore.data.map { it[Keys.LINK_SERVER_CONFIRMED] ?: false }.first()

    suspend fun setLinkServerConfirmed(value: Boolean) {
        context.floraDataStore.edit { it[Keys.LINK_SERVER_CONFIRMED] = value }
    }

    /** Phase 8: aakhri mili hui feature flags ka JSON (khali = koi nahi). */
    suspend fun featureFlagsJson(): String =
        context.floraDataStore.data.map { it[Keys.FEATURE_FLAGS_JSON] ?: "" }.first()

    suspend fun setFeatureFlagsJson(value: String) {
        context.floraDataStore.edit { it[Keys.FEATURE_FLAGS_JSON] = value }
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

    /** Deepgram key (mega build, user faisla: Deepgram hi rahega).
     *  Khali = system STT fallback; key aate hi sachhi barge-in.
     *  APP-C1-005 se KeyStore-encrypted save hoti hai. */
    suspend fun deepgramApiKey(): String =
        readSecret(Keys.DEEPGRAM_API_KEY)

    /** Home box code (v2.22, P5-F22 phone half): box ka join code
     *  sirf phone par save hota hai. Server sync (S4) abhi baaki
     *  hai — code save hona control nahi hai.
     *  Cycle 2, APP-C1-025: baaki 12 secrets jaisa hi
     *  readSecret/writeSecret se KeyStore-encrypted (app-private
     *  + rotatable hone se risk Low tha, consistency ke liye).
     *  Purana plaintext pehli read par migrate ho jayega. */
    suspend fun homeBoxCode(): String =
        readSecret(Keys.HOME_BOX_CODE)

    suspend fun setHomeBoxCode(value: String) {
        writeSecret(Keys.HOME_BOX_CODE, value)
    }

    /** PC pairing code (v2.22, Phase 5, G13) — sirf is phone par.
     *  Cycle 2, APP-C1-025: homeBoxCode jaisa hi encrypted. */
    suspend fun pcPairCode(): String =
        readSecret(Keys.PC_PAIR_CODE)

    suspend fun setPcPairCode(value: String) {
        writeSecret(Keys.PC_PAIR_CODE, value)
    }

    /** Avatar style id (v2.22, S2): gulabi/neela/hara/baingani/
     *  sunehra. Galat id aaye to gulabi (default) maana jaata hai. */
    suspend fun avatar(): String =
        context.floraDataStore.data.map { it[Keys.AVATAR] ?: "gulabi" }.first()

    suspend fun setAvatar(value: String) {
        context.floraDataStore.edit { it[Keys.AVATAR] = value }
    }

    suspend fun setDeepgramApiKey(value: String) {
        writeSecret(Keys.DEEPGRAM_API_KEY, value)
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

    // APP-C1-005: neeche ki saari user keys KeyStore-encrypted
    // save hoti hain (readSecret/writeSecret se).
    suspend fun geminiApiKey(): String =
        readSecret(Keys.GEMINI_API_KEY)

    suspend fun setGeminiApiKey(value: String) {
        writeSecret(Keys.GEMINI_API_KEY, value)
    }

    suspend fun groqApiKey(): String =
        readSecret(Keys.GROQ_API_KEY)

    suspend fun setGroqApiKey(value: String) {
        writeSecret(Keys.GROQ_API_KEY, value)
    }

    /** Optional second keys + optional search keys (v2.1): khali = lane band. */
    suspend fun geminiApiKey2(): String =
        readSecret(Keys.GEMINI_API_KEY2)

    suspend fun setGeminiApiKey2(value: String) {
        writeSecret(Keys.GEMINI_API_KEY2, value)
    }

    suspend fun groqApiKey2(): String =
        readSecret(Keys.GROQ_API_KEY2)

    suspend fun setGroqApiKey2(value: String) {
        writeSecret(Keys.GROQ_API_KEY2, value)
    }

    suspend fun tavilyApiKey(): String =
        readSecret(Keys.TAVILY_API_KEY)

    suspend fun setTavilyApiKey(value: String) {
        writeSecret(Keys.TAVILY_API_KEY, value)
    }

    suspend fun braveApiKey(): String =
        readSecret(Keys.BRAVE_API_KEY)

    suspend fun setBraveApiKey(value: String) {
        writeSecret(Keys.BRAVE_API_KEY, value)
    }

    suspend fun serpApiKey(): String =
        readSecret(Keys.SERPAPI_API_KEY)

    suspend fun setSerpApiKey(value: String) {
        writeSecret(Keys.SERPAPI_API_KEY, value)
    }

    suspend fun mapsApiKey(): String =
        readSecret(Keys.MAPS_API_KEY)

    suspend fun setMapsApiKey(value: String) {
        writeSecret(Keys.MAPS_API_KEY, value)
    }

    // FCM device token (v2.2): sirf is phone par DataStore me rahega;
    // kabhi log, chat, backup ya message me nahi jayega. Server sync
    // tabhi hoga jab push_tokens table + server function taiyaar ho.
    suspend fun fcmToken(): String =
        readSecret(Keys.FCM_TOKEN)

    suspend fun setFcmToken(value: String) {
        writeSecret(Keys.FCM_TOKEN, value)
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

    /**
     * Cycle 3, F1 (d/e): Face Lock unlock session (prefs side).
     * Unlock par FloraApp setFaceLockUnlock() bulata hai —
     * tab tak valid jab tak FACE_LOCK_SESSION_TIMEOUT_MS na
     * beete (GuardianGate me alag constant, EK HI JAGAH).
     * BackgroundMicService isi ko padh kar tay karta hai ki
     * background jawab de ya PIN maange. App-start par
     * MainActivity ise clear karta hai (naya session = locked).
     */
    suspend fun faceLockUnlockedUntil(): Long =
        context.floraDataStore.data.map { it[Keys.FACE_LOCK_UNLOCKED_UNTIL] ?: 0L }.first()

    suspend fun isFaceLockUnlocked(): Boolean =
        faceLockUnlockedUntil() > System.currentTimeMillis()

    suspend fun setFaceLockUnlock() {
        context.floraDataStore.edit {
            it[Keys.FACE_LOCK_UNLOCKED_UNTIL] =
                System.currentTimeMillis() + GuardianGate.FACE_LOCK_SESSION_TIMEOUT_MS
        }
    }

    suspend fun clearFaceLockUnlock() {
        context.floraDataStore.edit { it.remove(Keys.FACE_LOCK_UNLOCKED_UNTIL) }
    }

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

    /** Wave B: saved licence key (khali = koi key fill nahi hui).
     *  APP-C1-005 se phone par KeyStore-encrypted save hoti hai
     *  (server-side design waisa hi hai — ye sirf phone storage
     *  ka badlaav hai, key ka matlab/masking waisa hi). */
    suspend fun licenceKey(): String =
        readSecret(Keys.LICENCE_KEY)

    suspend fun setLicenceKey(value: String) {
        writeSecret(Keys.LICENCE_KEY, value)
    }

    /** Wave B: pehle se stored stable device id (khali = abhi set nahi hua). */
    suspend fun storedDeviceId(): String =
        context.floraDataStore.data.map { it[Keys.DEVICE_ID] ?: "" }.first()

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

    /** Lane A — A1: battery no-repeat key (khali = kabhi warn nahi hua). */
    suspend fun batteryLastWarnedKey(): String =
        context.floraDataStore.data.map { it[Keys.BATTERY_LAST_WARNED] ?: "" }.first()

    suspend fun setBatteryLastWarnedKey(value: String) {
        context.floraDataStore.edit { it[Keys.BATTERY_LAST_WARNED] = value }
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

    /** Voice sample (v2.22, user selection 2026-10-28): Sample 2
     *  Lily har jagah fixed. Default "lily". */
    suspend fun voiceSample(): String =
        context.floraDataStore.data.map { it[Keys.VOICE_SAMPLE] ?: "lily" }.first()

    suspend fun setVoiceSample(value: String) {
        context.floraDataStore.edit { it[Keys.VOICE_SAMPLE] = value }
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

    // B3 builder-service tokens (B3-04): GitHub/Vercel/user-Supabase
    // ke token SIRF in slots se jaate hain — licence/link slots
    // kabhi reuse nahi hote, website-account link state kabhi
    // builder token nahi banta. Token + refresh token hamesha
    // readSecret/writeSecret se (KeyStore AES/GCM, fail-closed:
    // encrypt fail par kuch nahi likha jata, plaintext fallback
    // kabhi nahi). Metadata (connected flag, expiry, auth kind,
    // user-project base URL/ref) plain prefs hain — inme koi
    // token value kabhi nahi hoti. OAuth access token setBuilderToken
    // se KeyStore-encrypted SAVE hota hai (access token + refresh
    // token dono encrypted slots me; memory-only nahi) — Connect
    // OAuth path setBuilderToken se hi save karta hai (P2-7 comment
    // sudhaar: pehle wala "kabhi save nahi hota" galat tha).
    // Reinstall/data-clear par KeyStore ciphertext nahi khulta —
    // tab connected flag bhi saath me mit jata hai aur user ko
    // dobara grant karna hota hai (R-B3-6, expected behaviour).
    private object BuilderKeys {
        val GITHUB_TOKEN = stringPreferencesKey("builder_github_token")
        val GITHUB_REFRESH = stringPreferencesKey("builder_github_refresh")
        val GITHUB_CONNECTED = booleanPreferencesKey("builder_github_connected")
        val GITHUB_EXPIRES = longPreferencesKey("builder_github_expires_ms")
        val GITHUB_AUTH_KIND = stringPreferencesKey("builder_github_auth_kind")
        // OAuth client ID — PUBLIC identifier hai (secret nahi),
        // isliye plain metadata me: silent refresh ko yehi
        // chahiye (refresh grant me client_id lagta hai). Verifier
        // kabhi yahan nahi aata — wo single-use aur composition
        // state me hi rehta hai, use ke baad clear hota hai.
        val GITHUB_OAUTH_CLIENT_ID = stringPreferencesKey("builder_github_oauth_client_id")
        val VERCEL_TOKEN = stringPreferencesKey("builder_vercel_token")
        // Kabhi likha nahi jata — Vercel token ka refresh token
        // hota hi nahi; key sirf mapping ko poora rakhne ke liye hai.
        val VERCEL_REFRESH = stringPreferencesKey("builder_vercel_refresh_unused")
        val VERCEL_CONNECTED = booleanPreferencesKey("builder_vercel_connected")
        val VERCEL_EXPIRES = longPreferencesKey("builder_vercel_expires_ms")
        val VERCEL_AUTH_KIND = stringPreferencesKey("builder_vercel_auth_kind")
        val VERCEL_OAUTH_CLIENT_ID = stringPreferencesKey("builder_vercel_oauth_client_id")
        val SUPABASE_TOKEN = stringPreferencesKey("builder_supabase_token")
        val SUPABASE_REFRESH = stringPreferencesKey("builder_supabase_refresh")
        val SUPABASE_CONNECTED = booleanPreferencesKey("builder_supabase_connected")
        val SUPABASE_EXPIRES = longPreferencesKey("builder_supabase_expires_ms")
        val SUPABASE_AUTH_KIND = stringPreferencesKey("builder_supabase_auth_kind")
        val SUPABASE_OAUTH_CLIENT_ID = stringPreferencesKey("builder_supabase_oauth_client_id")
        val SUPABASE_BASE_URL = stringPreferencesKey("builder_supabase_base_url")
        val SUPABASE_PROJECT_REF = stringPreferencesKey("builder_supabase_project_ref")
        // R-B3-9 clipboard opt-out: sirf chhoti setting, kabhi
        // token slot nahi, kabhi plaintext token nahi. Default
        // true = save ke baad clipboard auto-clear hota hai.
        val CLIPBOARD_AUTO_CLEAR = booleanPreferencesKey("builder_clipboard_autoclear")
        // B6 Drive slots (R-B6-5): token keys sirf readSecret/
        // writeSecret se chhue jaate hain — seedha edit kabhi nahi.
        val DRIVE_CLIENT_ID = stringPreferencesKey("drive_oauth_client_id")
        val DRIVE_REDIRECT_URI = stringPreferencesKey("drive_oauth_redirect_uri")
        val DRIVE_ACCESS_TOKEN = stringPreferencesKey("drive_access_token")
        val DRIVE_REFRESH_TOKEN = stringPreferencesKey("drive_refresh_token")
        val DRIVE_EXPIRES_AT_MS = longPreferencesKey("drive_expires_at_ms")
        val DRIVE_FOLDER_ID = stringPreferencesKey("drive_folder_id")
    }

    private fun builderTokenKey(service: BuilderService): Preferences.Key<String> =
        when (service) {
            BuilderService.GITHUB -> BuilderKeys.GITHUB_TOKEN
            BuilderService.VERCEL -> BuilderKeys.VERCEL_TOKEN
            BuilderService.SUPABASE -> BuilderKeys.SUPABASE_TOKEN
        }

    private fun builderRefreshKey(service: BuilderService): Preferences.Key<String> =
        when (service) {
            BuilderService.GITHUB -> BuilderKeys.GITHUB_REFRESH
            BuilderService.VERCEL -> BuilderKeys.VERCEL_REFRESH
            BuilderService.SUPABASE -> BuilderKeys.SUPABASE_REFRESH
        }

    private fun builderConnectedKey(service: BuilderService): Preferences.Key<Boolean> =
        when (service) {
            BuilderService.GITHUB -> BuilderKeys.GITHUB_CONNECTED
            BuilderService.VERCEL -> BuilderKeys.VERCEL_CONNECTED
            BuilderService.SUPABASE -> BuilderKeys.SUPABASE_CONNECTED
        }

    private fun builderExpiresKey(service: BuilderService): Preferences.Key<Long> =
        when (service) {
            BuilderService.GITHUB -> BuilderKeys.GITHUB_EXPIRES
            BuilderService.VERCEL -> BuilderKeys.VERCEL_EXPIRES
            BuilderService.SUPABASE -> BuilderKeys.SUPABASE_EXPIRES
        }

    private fun builderAuthKindKey(service: BuilderService): Preferences.Key<String> =
        when (service) {
            BuilderService.GITHUB -> BuilderKeys.GITHUB_AUTH_KIND
            BuilderService.VERCEL -> BuilderKeys.VERCEL_AUTH_KIND
            BuilderService.SUPABASE -> BuilderKeys.SUPABASE_AUTH_KIND
        }

    private fun builderOAuthClientIdKey(service: BuilderService): Preferences.Key<String> =
        when (service) {
            BuilderService.GITHUB -> BuilderKeys.GITHUB_OAUTH_CLIENT_ID
            BuilderService.VERCEL -> BuilderKeys.VERCEL_OAUTH_CLIENT_ID
            BuilderService.SUPABASE -> BuilderKeys.SUPABASE_OAUTH_CLIENT_ID
        }

    /** Persisted OAuth client ID (public identifier; khali = OAuth grant nahi ya purana save). */
    suspend fun builderOAuthClientId(service: BuilderService): String =
        context.floraDataStore.data.map { it[builderOAuthClientIdKey(service)] ?: "" }.first()

    suspend fun setBuilderOAuthClientId(service: BuilderService, clientId: String) {
        context.floraDataStore.edit {
            if (clientId.isEmpty()) it.remove(builderOAuthClientIdKey(service))
            else it[builderOAuthClientIdKey(service)] = clientId
        }
    }

    suspend fun builderToken(service: BuilderService): String =
        readSecret(builderTokenKey(service))

    suspend fun builderRefreshToken(service: BuilderService): String {
        // Vercel token ka koi refresh token nahi hota — isliye
        // yahan hamesha khali (client layer me NotRefreshable).
        if (service == BuilderService.VERCEL) return ""
        return readSecret(builderRefreshKey(service))
    }

    suspend fun builderConnected(service: BuilderService): Boolean =
        context.floraDataStore.data.map { it[builderConnectedKey(service)] ?: false }.first()

    suspend fun builderExpiresAtMs(service: BuilderService): Long =
        context.floraDataStore.data.map { it[builderExpiresKey(service)] ?: 0L }.first()

    suspend fun builderAuthKind(service: BuilderService): BuilderAuthKind? {
        val raw = context.floraDataStore.data.map { it[builderAuthKindKey(service)] ?: "" }.first()
        return when (raw) {
            "oauth" -> BuilderAuthKind.OAUTH
            "pat" -> BuilderAuthKind.PAT
            else -> null
        }
    }

    /** Supabase: user ke apne project ka base URL (khali = abhi set nahi). */
    suspend fun builderBaseUrl(service: BuilderService): String {
        if (service != BuilderService.SUPABASE) return ""
        return context.floraDataStore.data.map { it[BuilderKeys.SUPABASE_BASE_URL] ?: "" }.first()
    }

    /** Supabase project ref (khali = abhi set nahi). */
    suspend fun builderProjectRef(service: BuilderService): String {
        if (service != BuilderService.SUPABASE) return ""
        return context.floraDataStore.data.map { it[BuilderKeys.SUPABASE_PROJECT_REF] ?: "" }.first()
    }

    suspend fun builderConnection(service: BuilderService): BuilderConnection {
        val token = builderToken(service)
        return BuilderConnection(
            service = service,
            connected = builderConnected(service),
            hasToken = token.isNotEmpty(),
            authKind = builderAuthKind(service),
            expiresAtMs = builderExpiresAtMs(service),
            baseUrl = builderBaseUrl(service),
            projectRef = builderProjectRef(service)
        )
    }

    /**
     * Builder token save — fail-closed: token encrypt na ho to
     * false aur connected flag kabhi set nahi hota (aadha grant
     * "connected" dikha kar chhodna galat hoga). Refresh token
     * diya gaya ho aur uska encrypt fail ho to bhi poora save
     * fail hota hai (setLink wala pattern). Khali token = save
     * nahi, disconnectBuilder() use karo.
     */
    suspend fun setBuilderToken(
        service: BuilderService,
        token: String,
        authKind: BuilderAuthKind,
        refreshToken: String = "",
        expiresAtMs: Long = 0L,
        baseUrl: String = "",
        projectRef: String = ""
    ): Boolean {
        if (token.isEmpty()) return false
        if (!writeSecret(builderTokenKey(service), token)) return false
        if (service != BuilderService.VERCEL) {
            if (!writeSecret(builderRefreshKey(service), refreshToken)) return false
        }
        context.floraDataStore.edit {
            it[builderConnectedKey(service)] = true
            it[builderExpiresKey(service)] = expiresAtMs
            it[builderAuthKindKey(service)] =
                if (authKind == BuilderAuthKind.OAUTH) "oauth" else "pat"
            if (service == BuilderService.SUPABASE) {
                if (baseUrl.isEmpty()) it.remove(BuilderKeys.SUPABASE_BASE_URL)
                else it[BuilderKeys.SUPABASE_BASE_URL] = baseUrl
                if (projectRef.isEmpty()) it.remove(BuilderKeys.SUPABASE_PROJECT_REF)
                else it[BuilderKeys.SUPABASE_PROJECT_REF] = projectRef
            }
        }
        return true
    }

    /** Disconnect/revoke (B3-04): token + refresh + flag + metadata sab hat jate hain. */
    suspend fun disconnectBuilder(service: BuilderService) {
        context.floraDataStore.edit {
            it.remove(builderTokenKey(service))
            if (service != BuilderService.VERCEL) it.remove(builderRefreshKey(service))
            it.remove(builderConnectedKey(service))
            it.remove(builderExpiresKey(service))
            it.remove(builderAuthKindKey(service))
            it.remove(builderOAuthClientIdKey(service))
            if (service == BuilderService.SUPABASE) {
                it.remove(BuilderKeys.SUPABASE_BASE_URL)
                it.remove(BuilderKeys.SUPABASE_PROJECT_REF)
            }
        }
    }

    /**
     * P2-8: Supabase project ref/base URL metadata-only update —
     * token ko dobara chhue bina (token value yahan kabhi nahi
     * aati). Clipboard/OAuth save ke baad ref chune par yehi
     * chalta hai; khali ref = metadata hat jati hai.
     */
    suspend fun setBuilderSupabaseRef(projectRef: String) {
        val ref = projectRef.trim()
        context.floraDataStore.edit {
            if (ref.isEmpty()) {
                it.remove(BuilderKeys.SUPABASE_BASE_URL)
                it.remove(BuilderKeys.SUPABASE_PROJECT_REF)
            } else {
                // Wahi formula jo BuilderHosts.userProjectBaseUrl
                // deta hai (tools import se package-cycle bachane
                // ke liye yahan inline — dono ek hi shape hain).
                it[BuilderKeys.SUPABASE_BASE_URL] = "https://$ref.supabase.co"
                it[BuilderKeys.SUPABASE_PROJECT_REF] = ref
            }
        }
    }

    /**
     * B6 Drive — OAuth + token slots (R-B6-4, R-B6-5).
     * Client ID / redirect URI PUBLIC identifiers hain (secret
     * nahi) — user apne Google OAuth app se daalta hai (K4/G14:
     * GOOGLE_WEB_CLIENT_ID khaali hai, isliye B6 bhi user-entered
     * client ID par chalta hai; live grant Phase 10 ka kaam hai).
     * Access/refresh token readSecret/writeSecret se KeyStore-
     * encrypted rehte hain — kabhi plaintext, kabhi log me nahi.
     * Folder ID sirf metadata hai (user ke Drive me "Flora
     * Backups" folder ka ID).
     */
    suspend fun driveClientId(): String =
        context.floraDataStore.data.map { it[BuilderKeys.DRIVE_CLIENT_ID] ?: "" }.first()

    suspend fun setDriveClientId(value: String) {
        context.floraDataStore.edit {
            if (value.isBlank()) it.remove(BuilderKeys.DRIVE_CLIENT_ID)
            else it[BuilderKeys.DRIVE_CLIENT_ID] = value.trim()
        }
    }

    suspend fun driveRedirectUri(): String =
        context.floraDataStore.data.map { it[BuilderKeys.DRIVE_REDIRECT_URI] ?: "" }.first()

    suspend fun setDriveRedirectUri(value: String) {
        context.floraDataStore.edit {
            if (value.isBlank()) it.remove(BuilderKeys.DRIVE_REDIRECT_URI)
            else it[BuilderKeys.DRIVE_REDIRECT_URI] = value.trim()
        }
    }

    suspend fun driveAccessToken(): String = readSecret(BuilderKeys.DRIVE_ACCESS_TOKEN)

    suspend fun driveRefreshToken(): String = readSecret(BuilderKeys.DRIVE_REFRESH_TOKEN)

    suspend fun driveExpiresAtMs(): Long =
        context.floraDataStore.data.map { it[BuilderKeys.DRIVE_EXPIRES_AT_MS] ?: 0L }.first()

    suspend fun setDriveTokens(accessToken: String, refreshToken: String, expiresAtMs: Long): Boolean {
        if (accessToken.isEmpty()) return false
        if (!writeSecret(BuilderKeys.DRIVE_ACCESS_TOKEN, accessToken)) return false
        if (refreshToken.isNotEmpty()) {
            if (!writeSecret(BuilderKeys.DRIVE_REFRESH_TOKEN, refreshToken)) return false
        }
        context.floraDataStore.edit { it[BuilderKeys.DRIVE_EXPIRES_AT_MS] = expiresAtMs }
        return true
    }

    suspend fun clearDriveTokens() {
        context.floraDataStore.edit {
            it.remove(BuilderKeys.DRIVE_ACCESS_TOKEN)
            it.remove(BuilderKeys.DRIVE_REFRESH_TOKEN)
            it.remove(BuilderKeys.DRIVE_EXPIRES_AT_MS)
        }
    }

    suspend fun driveFolderId(): String =
        context.floraDataStore.data.map { it[BuilderKeys.DRIVE_FOLDER_ID] ?: "" }.first()

    suspend fun setDriveFolderId(value: String) {
        context.floraDataStore.edit {
            if (value.isBlank()) it.remove(BuilderKeys.DRIVE_FOLDER_ID)
            else it[BuilderKeys.DRIVE_FOLDER_ID] = value.trim()
        }
    }

    /** Clipboard auto-clear opt-out (R-B3-9): default true. */
    suspend fun builderClipboardAutoClear(): Boolean =
        context.floraDataStore.data.map { it[BuilderKeys.CLIPBOARD_AUTO_CLEAR] ?: true }.first()

    suspend fun setBuilderClipboardAutoClear(value: Boolean) {
        context.floraDataStore.edit { it[BuilderKeys.CLIPBOARD_AUTO_CLEAR] = value }
    }
}
