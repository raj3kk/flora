package com.flora.app.data

import com.flora.app.core.FloraConfig
import com.flora.app.core.PinnedTrust
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * In-memory user-JWT cache (Wave B, LFMS v2 JWT gate).
 *
 * Linked users ko licence-verify / usage-count par apna user JWT
 * bhejna hai (anon-only v2 par 401 dega — server builder note).
 * Access token memory me hi rehta hai (kabhi save nahi), 55 minute
 * tak reuse hota hai (Supabase default 1 ghanta) — har verify par
 * refresh nahi hoga (rotation churn + reuse-detection ka khatra
 * nahi). Mutex se ek waqt par ek hi refresh (race nahi).
 * Refresh fail ho to null (caller anon fallback lega).
 */
private object SessionTokenCache {
    private val mutex = Mutex()
    private var access: String = ""
    private var expiresAtMs = 0L

    suspend fun accessToken(
        prefs: FloraPreferences,
        client: LicenceClient,
    ): String? {
        var result: String? = null
        mutex.withLock {
            val now = System.currentTimeMillis()
            if (access.isNotEmpty() && now < expiresAtMs) {
                result = access
                return@withLock
            }
            val rt = try {
                prefs.linkedRefreshToken()
            } catch (e: Exception) {
                CatcherHooks.licenceRecord("exception")
                ""
            }
            if (rt.isEmpty()) {
                return@withLock
            }
            val pair = try {
                client.refreshAccessToken(rt)
            } catch (e: Exception) {
                CatcherHooks.licenceRecord("exception")
                null
            } ?: return@withLock
            access = pair.first
            expiresAtMs = now + 55L * 60L * 1000L
            val next = pair.second
            if (!next.isNullOrEmpty() && next != rt) {
                try {
                    prefs.setLink(prefs.linkedUserId(), prefs.linkedEmail(), next)
                } catch (e: Exception) {
                    CatcherHooks.licenceRecord("exception")
                }
            }
            result = access
        }
        return result
    }
}

/** Server-authoritative licence result. Local free mode is the fallback. */
data class LicenceStatus(
    val plan: String, // "free" or "pro"
    val status: String, // "active", "expired", "local_free", "error", v2: + revoked/superseded/bound_mismatch
    val serverChecked: Boolean,
    // Licence-verify v2 (Phase 8 coordination, LFMS server):
    // valid=false + status revoked/expired/superseded/
    // bound_mismatch par plan kabhi "pro" nahi hota (verify()
    // me hi free kar diya jata hai — revoke ke baad bhi Pro
    // features chalte rahenge, ye bug nahi hone diya gaya).
    // Purani shape (valid field na ho) par valid = (status ==
    // "active") — backward compatible.
    val valid: Boolean = true,
    // v2: licence jis device par bound hai, uska short code
    // (mismatch par user ko dikhane ke liye; personal data nahi).
    val boundDeviceShort: String = "",
    // v2: server ka reason (khali ho sakta hai).
    val reason: String = "",
    // v2: plan ke features (khali ho sakta hai).
    val features: List<String> = emptyList(),
    // v2: expiry timestamp (khali ho sakta hai).
    val expiresAt: String = "",
    // Wave B: server ka device lock (15 din se pehle nayi
    // device par).
    val deviceLocked: Boolean = false,
) {
    /**
     * v2 status par imaandaar Hindi message; null = koi khaas
     * baat nahi (active / local_free / error par koi message
     * nahi — offline behavior bilkul wahi rehta hai).
     * Generic error kabhi nahi dikhaya jata — har status ka
     * apna seedha message hai.
     */
    fun honestMessage(): String? = when (status) {
        "revoked" ->
            "Licence revoke ho gayi hai — Pro features band hain. Admin se sampark karo."
        "bound_mismatch" ->
            "Ye licence kisi aur device par lagi hai" +
                (if (boundDeviceShort.isNotEmpty()) " (device: $boundDeviceShort)" else "") +
                " — is phone par Pro nahi chalega. Rebind sirf admin panel se hoga, apne aap nahi."
        "expired" ->
            "Licence ki expiry ho gayi hai — Pro band hai, renew karo."
        "superseded" ->
            "Ye licence nayi licence se badal di gayi hai — nayi licence check karo."
        "device_locked" ->
            "Device lock hai — ye licence abhi kisi aur device par chal rahi hai." +
                " Admin se turant rebind bhi kara sakte ho."
        else -> null
    }
}

data class UsageResult(val counted: Boolean, val detail: String)

data class LinkResult(val uid: String, val refreshToken: String, val error: String)

/**
 * Talks to Supabase Edge Functions only. Anon key FloraPreferences
 * se aati hai (user ki Settings key ho to wahi, warna app ka
 * default publishable key — v2.3 se default wiring hai).
 * Service_role key app me kabhi nahi hoti.
 */
class LicenceClient(private val prefs: FloraPreferences) {

    // Website account link (v2.8): Supabase Auth password grant.
    // Access token wapas milta hai par save NAHI hota — sirf uid
    // + refresh token caller ko milta hai (refresh DataStore me,
    // access memory me hi). Password kabhi log/store nahi hota.
    suspend fun linkWebsiteAccount(email: String, password: String): LinkResult =
        withContext(Dispatchers.IO) {
            val key = prefs.supabaseAnonKey()
            if (key.isEmpty()) return@withContext LinkResult("", "", "Anon key nahi mili")
            val body = JSONObject().put("email", email).put("password", password)
            val res = post("/auth/v1/token?grant_type=password", body, key)
                ?: return@withContext LinkResult("", "", "Server se jawab nahi mila")
            val user = res.optJSONObject("user")
            val uid = user?.optString("id", "") ?: ""
            val refresh = res.optString("refresh_token", "")
            if (uid.isEmpty() || refresh.isEmpty()) {
                val msg = res.optString("msg", res.optString("error_description", "Login fail"))
                return@withContext LinkResult("", "", msg)
            }
            LinkResult(uid, refresh, "")
        }

    // Google id-token link (v2.22, Phase 5, G14): Google
    // sign-in se mila id_token Supabase ko do -> uid +
    // refresh token (password wale link jaisa hi result).
    // Web OAuth client ID dashboard par banana baaki hai
    // (FloraConfig.GOOGLE_WEB_CLIENT_ID khali hai tab tak
    // button setup guidance dikhata hai, sign-in nahi).
    suspend fun linkWithGoogleIdToken(idToken: String): LinkResult =
        withContext(Dispatchers.IO) {
            val key = prefs.supabaseAnonKey()
            if (key.isEmpty() || idToken.isEmpty()) {
                return@withContext LinkResult("", "", "Anon key ya token nahi mila")
            }
            val body = JSONObject()
                .put("provider", "google")
                .put("id_token", idToken)
            val res = post("/auth/v1/token?grant_type=id_token", body, key)
                ?: return@withContext LinkResult("", "", "Server se jawab nahi mila")
            val user = res.optJSONObject("user")
            val uid = user?.optString("id", "") ?: ""
            val refresh = res.optString("refresh_token", "")
            if (uid.isEmpty() || refresh.isEmpty()) {
                val msg = res.optString("msg", res.optString("error_description", "Login fail"))
                return@withContext LinkResult("", "", msg)
            }
            LinkResult(uid, refresh, "")
        }

    // Session renew (v2.22, mega build): saved refresh token se
    // naya refresh token lao (rotation par naya wala save karna
    // caller ka kaam hai). Access token kabhi save nahi hota —
    // ye sirf session zinda rakhne ke liye hai, licence/usage
    // calls par iska asar nahi (wo anon key se chalte hain).
    // Fail ho to null — app local mode me chalti rahegi, koi
    // error user ko nahi dikhega.
    suspend fun refreshSession(refreshToken: String): String? =
        withContext(Dispatchers.IO) {
            val key = prefs.supabaseAnonKey()
            if (key.isEmpty() || refreshToken.isEmpty()) return@withContext null
            val body = JSONObject().put("refresh_token", refreshToken)
            val res = post("/auth/v1/token?grant_type=refresh_token", body, key)
                ?: return@withContext null
            val next = res.optString("refresh_token", "")
            if (next.isEmpty()) null else next
        }

    // Phase 8 heartbeat ke liye: refresh token se access token
    // (JWT) + rotated refresh token, dono memory me hi.
    // Access token kabhi save nahi hota — caller use turant
    // heartbeat header me bhej kar bhool jaye. Rotated refresh
    // mile to caller use setLink se save kare (warna agla
    // refresh purane token se fail hoga). Fail ho to null.
    suspend fun refreshAccessToken(refreshToken: String): Pair<String, String?>? =
        withContext(Dispatchers.IO) {
            val key = prefs.supabaseAnonKey()
            if (key.isEmpty() || refreshToken.isEmpty()) return@withContext null
            val body = JSONObject().put("refresh_token", refreshToken)
            val res = post("/auth/v1/token?grant_type=refresh_token", body, key)
                ?: return@withContext null
            val access = res.optString("access_token", "")
            if (access.isEmpty()) return@withContext null
            val next = res.optString("refresh_token", "")
            Pair(access, if (next.isEmpty()) null else next)
        }

    // verify() se aakhri jaana hua plan (memory me hi). countUsage
    // isi se decide karta hai ki server count bhejna hai ya nahi:
    // pro = unlimited, server count ki zaroorat nahi.
    @Volatile
    private var lastPlan: String? = null

    // B2 (fix-cycle): gate-path posts (verify/verifyWithKey) ko chhota
    // socket timeout milta hai; baaki callers defaults (15s/15s) par hi
    // rahenge — koi caller behavior change nahi.
    private fun post(path: String, body: JSONObject, anonKey: String, userJwt: String? = null, connectTimeoutMs: Int = 15_000, readTimeoutMs: Int = 15_000): JSONObject? {
        // G17 (Phase 8): Supabase host pin-checked connection.
        val conn = (PinnedTrust.openConnection(URL(FloraConfig.SUPABASE_URL + path)) as HttpURLConnection)
        return try {
            conn.requestMethod = "POST"
            conn.connectTimeout = connectTimeoutMs
            conn.readTimeout = readTimeoutMs
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            if (anonKey.isNotEmpty()) {
                conn.setRequestProperty("apikey", anonKey)
                // Wave B: linked user ka JWT ho to Authorization
                // wahi hoga (v2 function gate isi se user nikaalta
                // hai); nahi to anon key (v1 + unlinked path).
                val auth = if (!userJwt.isNullOrEmpty()) userJwt else anonKey
                conn.setRequestProperty("Authorization", "Bearer $auth")
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
            JSONObject(text)
        } catch (e: Exception) {
            CatcherHooks.licenceRecord("exception")
            null
        } finally {
            conn.disconnect()
        }
    }

    suspend fun verify(userId: String, deviceId: String): LicenceStatus = withContext(Dispatchers.IO) {
        val key = prefs.supabaseAnonKey()
        if (key.isEmpty()) {
            return@withContext LicenceStatus("free", "local_free", false)
        }
        val body = JSONObject().put("user_id", userId).put("device_id", deviceId)
        // Wave B, LFMS v2 JWT gate (server builder must-do):
        // linked users apna user JWT bhejen — anon-only v2 par
        // 401 dega. JWT v1 ko nahi todta (v1 header padhta hi
        // nahi). Unlinked (koi refresh token nahi) anon hi
        // bhejega (v2 401 → error status → local mode).
        val jwt = try {
            SessionTokenCache.accessToken(prefs, this@LicenceClient)
        } catch (e: Exception) {
            CatcherHooks.licenceRecord("exception")
            null
        }
        val res = post(FloraConfig.LICENCE_FUNCTION_PATH, body, key, jwt, connectTimeoutMs = 5_000, readTimeoutMs = 8_000)
            ?: return@withContext LicenceStatus("free", "error", false)
        parseVerifyResponse(res)
    }

    // Wave B: admin se mili licence key se verify (key + device).
    // Key server par user se judi hai; server hi faisla karta hai
    // (status/binding/expiry). Server key support LFMS v2 me hai
    // (deploy pending) — tab tak error shape aaye to error status
    // milega, crash nahi.
    // Key path par JWT NAHI bheja jata: key khud credential hai
    // (bearer) — linked JWT ka user-match galat user ki key ko
    // reject kar dega (admin ne key kisi aur account ke liye
    // banayi ho to bhi key chalni chahiye). OPEN QUESTION (parent
    // + server builder ko): agar server key path par bhi JWT
    // maangta hai to unlinked key users tootenge — server ko key
    // path bearer-authed rakhna hoga.
    suspend fun verifyWithKey(licenceKey: String, deviceId: String): LicenceStatus = withContext(Dispatchers.IO) {
        val key = prefs.supabaseAnonKey()
        if (key.isEmpty() || licenceKey.isEmpty()) {
            return@withContext LicenceStatus("free", "local_free", false)
        }
        val body = JSONObject().put("licence_key", licenceKey).put("device_id", deviceId)
        val res = post(FloraConfig.LICENCE_FUNCTION_PATH, body, key, connectTimeoutMs = 5_000, readTimeoutMs = 8_000)
            ?: return@withContext LicenceStatus("free", "error", false)
        parseVerifyResponse(res)
    }

    // Licence-verify v2 response parser (verify + verifyWithKey
    // dono isi se — ek hi parsing, do jagah nahi).
    // Response: {valid, status, plan, features[], expires_at,
    // bound_device_short, reason?, device_locked}.
    // Device binding rule: ek
    // licence ek time me ek device par — first claim bind karta
    // hai; 15 din poore par server transfer karta hai (purana
    // device free); 15 din se pehle device_locked; admin rebind
    // turant. App khud rebind ki koshish kabhi nahi karti —
    // sirf deviceId bhejti hai, faisla server ka hai.
    // Error body ({error} bina status ke) ko free/active NAHI
    // mana jata — wo "error" status hai (pehle galat parse
    // hota tha: error body bhi free/active dikhti thi).
    private fun parseVerifyResponse(res: JSONObject): LicenceStatus {
        if (res.has("error") && !res.has("status")) {
            return LicenceStatus("free", "error", false)
        }
        val rawStatus = res.optString("status", "active")
        val valid = if (res.has("valid")) {
            res.optBoolean("valid", false)
        } else {
            rawStatus == "active"
        }
        val respPlan = res.optString("plan", "free")
        // Effective plan: Pro sirf tab jab server valid + active
        // kahe. Revoked/expired/superseded/bound_mismatch par
        // plan free — warna revoke ke baad bhi Pro chalta rahega.
        val plan = if (valid && rawStatus == "active" && respPlan == "pro") "pro" else "free"
        val featuresArr = res.optJSONArray("features")
        val features = if (featuresArr != null) {
            List(featuresArr.length()) { featuresArr.optString(it, "") }
                .filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
        val status = LicenceStatus(
            plan = plan,
            status = rawStatus,
            serverChecked = true,
            valid = valid,
            boundDeviceShort = res.optString("bound_device_short", ""),
            reason = res.optString("reason", ""),
            features = features,
            expiresAt = res.optString("expires_at", ""),
            deviceLocked = res.optBoolean("device_locked", false)
        )
        // v1 shape: status "free" + device_locked flag — use
        // device_locked status banao taaki honestMessage chale.
        val fixed = if (status.deviceLocked && status.status == "free") {
            status.copy(status = "device_locked")
        } else {
            status
        }
        lastPlan = fixed.plan
        // Wave B: plan features har verify par refresh (admin
        // panel se badli hui list turant lagu ho).
        com.flora.app.core.PlanFeatures.updateFrom(fixed.features)
        return fixed
    }

    suspend fun countUsage(userId: String, seconds: Long): UsageResult = withContext(Dispatchers.IO) {        if (lastPlan == "pro") return@withContext UsageResult(false, "pro unlimited")
        val key = prefs.supabaseAnonKey()
        if (key.isEmpty()) return@withContext UsageResult(false, "no anon key configured")
        val body = JSONObject().put("user_id", userId).put("seconds", seconds)
        // Wave B (server builder note): unlinked reporting ke liye
        // device_id bhejo (warna counted:false); linked path par
        // user JWT bhi (v2 gate). Signature nahi badli — device
        // stored prefs se liya (MainActivity call site waisa hi).
        val dev = try {
            prefs.storedDeviceId()
        } catch (e: Exception) {
            CatcherHooks.licenceRecord("exception")
            ""
        }
        if (dev.isNotEmpty()) body.put("device_id", dev)
        val jwt = try {
            SessionTokenCache.accessToken(prefs, this@LicenceClient)
        } catch (e: Exception) {
            CatcherHooks.licenceRecord("exception")
            null
        }
        val res = post(FloraConfig.USAGE_FUNCTION_PATH, body, key, jwt)
            ?: return@withContext UsageResult(false, "server unreachable")
        UsageResult(res.optBoolean("counted", false), res.optString("detail", ""))
    }

    // Wave B, D1: server UPI (public-settings, no-auth read) —
    // Pro screen + website pay page ka EK source. Khali mile to
    // caller imaandaar fallback dikhaye (local VPA kabhi nahi).
    // NOTE (audit D10 correction): usage-count sirf REPORT
    // karta hai — cap yahan enforce NAHI hota. Cap proxy gates
    // (brain-proxy v5 / tts-proxy v3, server) + app gate
    // (LicenceGate, BrainClient.ask se pehle) me hai.
    suspend fun fetchServerUpi(): String = withContext(Dispatchers.IO) {
        val key = prefs.supabaseAnonKey()
        if (key.isEmpty()) return@withContext ""
        val res = post(FloraConfig.PUBLIC_SETTINGS_PATH, JSONObject(), key)
            ?: return@withContext ""
        res.optString("upi_vpa", "")
    }

    /**
     * FCM token server sync (v2.5). Fire-and-forget: fail ho to
     * local token phir bhi saved rehta hai, agle app-open par
     * dobara try hoga. Token kabhi log nahi hota.
     * Do raaste (push-token-sync v2): LINKED path (linked ho to
     * app SessionTokenCache se user JWT bhejta hai; caller-match
     * par server users row upsert karta hai — yehi raasta link
     * flow ki users row banata hai); UNLINKED path (unlinked ho
     * to anon hi — app me Auth session nahi) users row claim
     * NAHI karta, sirf pehle se registered device ka token
     * upsert karta hai, warna "device not registered (link ke
     * baad sync hoga)". App-link fix: syncPushToken ab JWT
     * bhejta hai (pehle anon-only tha, isliye linked hone par
     * bhi users row kabhi nahi banti thi).
     */
    suspend fun syncPushToken(userId: String, deviceId: String, token: String): Boolean =
        withContext(Dispatchers.IO) {
            val key = prefs.supabaseAnonKey()
            if (key.isEmpty() || token.isEmpty()) return@withContext false
            val body = JSONObject().put("user_id", userId)
                .put("device_id", deviceId).put("fcm_token", token)
            // App-link fix: linked user ka JWT bhejo (verify() /
            // countUsage() wala SessionTokenCache raasta) taaki
            // server LINKED path chale aur users row upsert ho
            // (id=user_id, device_id). JWT na mile (unlinked) to
            // anon hi — UNLINKED path, pehle jaisa vyavhaar.
            val jwt = try {
                SessionTokenCache.accessToken(prefs, this@LicenceClient)
            } catch (e: Exception) {
                CatcherHooks.licenceRecord("exception")
                null
            }
            val res = post(FloraConfig.PUSH_TOKEN_FUNCTION_PATH, body, key, jwt)
                ?: return@withContext false
            res.optBoolean("ok", false)
        }
}
