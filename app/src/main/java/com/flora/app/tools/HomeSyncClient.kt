package com.flora.app.tools

import android.content.Context
import android.provider.Settings
import com.flora.app.core.FeatureFlags
import com.flora.app.core.PlanFeatures
import com.flora.app.core.FloraConfig
import com.flora.app.core.PinnedTrust
import com.flora.app.data.FloraPreferences
import com.flora.app.data.LicenceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Home sync client (Phase 8 rewrite).
 *
 * Pehle: anon key + effectiveUserId() ko device_id bana kar
 * bheja jata tha, aur function sirf "received, not stored"
 * kehta tha. Ab:
 *  - Auth: linked account ka refresh token -> access token
 *    (JWT, memory me hi, kabhi save nahi) + Authorization
 *    Bearer JWT. Unlinked ho (koi refresh token nahi) to sync
 *    hota hi nahi — null, imaandaar message UI me.
 *  - Body: {"user_id": <linked uid>, "device_id": <ANDROID_ID
 *    se bana stable device id — kabhi invent nahi>, "box_code"}.
 *    Function JWT user == body user_id check karta hai (403
 *    warna) aur service-role se home_boxes me upsert karta hai.
 *  - Connection G17 pin-checked (Supabase host).
 * Fail ho to null — app local mode me chalti rahegi. Koi
 * secret is file me nahi; JWT caller ke haath me hi rehta hai.
 */
object HomeSyncClient {

    suspend fun sync(prefs: FloraPreferences, boxCode: String, context: Context): String? {
        if (boxCode.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val anon = prefs.supabaseAnonKey()
                if (anon.isEmpty()) return@withContext null
                // Home flag OFF ho to sync hi nahi (panel control).
                // Wave B: plan features bhi allow karein tabhi sync.
                if (!FeatureFlags.isEnabled("home") || !PlanFeatures.isAllowed("home")) return@withContext null
                val linkedUid = prefs.linkedUserId()
                if (linkedUid.isEmpty()) return@withContext null
                val rt = prefs.linkedRefreshToken()
                if (rt.isEmpty()) return@withContext null
                val pair = LicenceClient(prefs).refreshAccessToken(rt)
                    ?: return@withContext null
                val access = pair.first
                val nextRefresh = pair.second
                if (!nextRefresh.isNullOrEmpty() && nextRefresh != rt) {
                    try {
                        prefs.setLink(linkedUid, prefs.linkedEmail(), nextRefresh)
                    } catch (e: Exception) {
                    }
                }
                val androidId = Settings.Secure.getString(
                    context.contentResolver, Settings.Secure.ANDROID_ID
                ) ?: "flora-device"
                val deviceId = prefs.deviceId(androidId)
                val url = URL(FloraConfig.SUPABASE_URL + FloraConfig.HOME_SYNC_FUNCTION_PATH)
                val conn = PinnedTrust.openConnection(url) as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", anon)
                conn.setRequestProperty("Authorization", "Bearer $access")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.doOutput = true
                val body = "{\"user_id\":\"$linkedUid\",\"device_id\":\"$deviceId\",\"box_code\":\"$boxCode\"}"
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
