package com.flora.app.data

import com.flora.app.core.FloraConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/** Server-authoritative licence result. Local free mode is the fallback. */
data class LicenceStatus(
    val plan: String, // "free" or "pro"
    val status: String, // "active", "expired", "local_free", "error"
    val serverChecked: Boolean,
)

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

    // verify() se aakhri jaana hua plan (memory me hi). countUsage
    // isi se decide karta hai ki server count bhejna hai ya nahi:
    // pro = unlimited, server count ki zaroorat nahi.
    @Volatile
    private var lastPlan: String? = null

    private fun post(path: String, body: JSONObject, anonKey: String): JSONObject? {
        val conn = (URL(FloraConfig.SUPABASE_URL + path).openConnection() as HttpURLConnection)
        return try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            if (anonKey.isNotEmpty()) {
                conn.setRequestProperty("apikey", anonKey)
                conn.setRequestProperty("Authorization", "Bearer $anonKey")
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
            JSONObject(text)
        } catch (e: Exception) {
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
        val res = post(FloraConfig.LICENCE_FUNCTION_PATH, body, key)
            ?: return@withContext LicenceStatus("free", "error", false)
        val plan = res.optString("plan", "free")
        val status = res.optString("status", "active")
        lastPlan = if (plan == "pro") "pro" else "free"
        LicenceStatus(if (plan == "pro") "pro" else "free", status, true)
    }

    suspend fun countUsage(userId: String, seconds: Long): UsageResult = withContext(Dispatchers.IO) {
        if (lastPlan == "pro") return@withContext UsageResult(false, "pro unlimited")
        val key = prefs.supabaseAnonKey()
        if (key.isEmpty()) return@withContext UsageResult(false, "no anon key configured")
        val body = JSONObject().put("user_id", userId).put("seconds", seconds)
        val res = post(FloraConfig.USAGE_FUNCTION_PATH, body, key)
            ?: return@withContext UsageResult(false, "server unreachable")
        UsageResult(res.optBoolean("counted", false), res.optString("detail", ""))
    }

    /**
     * FCM token server sync (v2.5). Fire-and-forget: fail ho to
     * local token phir bhi saved rehta hai, agle app-open par
     * dobara try hoga. Token kabhi log nahi hota. Server
     * function users row bhi bana deta hai (isliye usage-count
     * FK + admin-approve device lookup us user ke liye chalne
     * lagte hain). JWT auth nahi hai (app me Auth session
     * nahi) — user_id random UUID hi pehchaan hai.
     */
    suspend fun syncPushToken(userId: String, deviceId: String, token: String): Boolean =
        withContext(Dispatchers.IO) {
            val key = prefs.supabaseAnonKey()
            if (key.isEmpty() || token.isEmpty()) return@withContext false
            val body = JSONObject().put("user_id", userId)
                .put("device_id", deviceId).put("fcm_token", token)
            val res = post(FloraConfig.PUSH_TOKEN_FUNCTION_PATH, body, key)
                ?: return@withContext false
            res.optBoolean("ok", false)
        }
}
