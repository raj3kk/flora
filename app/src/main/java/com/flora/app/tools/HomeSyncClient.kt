package com.flora.app.tools

import com.flora.app.core.FloraConfig
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Home sync client (v2.22, Phase 5, G10, phone half) —
 * Flora ke Home box code ko server (Supabase Edge
 * Function home-sync) se jodta hai.
 * Server half: supabase/functions/home-sync/index.ts
 * (repo me hai, deploy Phase 4 dashboard se hoga).
 * Fail ho to null — app local mode me chalti rahegi.
 * Yahan koi secret nahi; sirf anon key header lagta hai.
 */
object HomeSyncClient {

    suspend fun sync(prefs: FloraPreferences, boxCode: String): String? {
        if (boxCode.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val anon = prefs.supabaseAnonKey()
                if (anon.isEmpty()) return@withContext null
                val url = URL(FloraConfig.SUPABASE_URL + "/functions/v1/home-sync")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", anon)
                conn.setRequestProperty("Authorization", "Bearer $anon")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.doOutput = true
                val deviceId = prefs.effectiveUserId()
                val body = "{\"device_id\":\"$deviceId\",\"box_code\":\"$boxCode\"}"
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val text = try {
                    (if (code in 200..299) conn.inputStream else conn.errorStream)
                        ?.bufferedReader()?.readText()
                } catch (e: Exception) {
                    null
                }
                conn.disconnect()
                if (code in 200..299) text else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
