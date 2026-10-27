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

/**
 * Talks to Supabase Edge Functions only. No key is hardcoded: the anon
 * key comes from FloraPreferences (entered once by the user in Settings).
 * With no key configured, the app honestly reports local free mode and
 * never claims a server check happened.
 */
class LicenceClient(private val prefs: FloraPreferences) {

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
        LicenceStatus(if (plan == "pro") "pro" else "free", status, true)
    }

    suspend fun countUsage(userId: String, seconds: Long): UsageResult = withContext(Dispatchers.IO) {
        val key = prefs.supabaseAnonKey()
        if (key.isEmpty()) return@withContext UsageResult(false, "no anon key configured")
        val body = JSONObject().put("user_id", userId).put("seconds", seconds)
        val res = post(FloraConfig.USAGE_FUNCTION_PATH, body, key)
            ?: return@withContext UsageResult(false, "server unreachable")
        UsageResult(res.optBoolean("counted", false), res.optString("detail", ""))
    }
}
