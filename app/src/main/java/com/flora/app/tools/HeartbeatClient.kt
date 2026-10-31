package com.flora.app.tools
import com.flora.app.data.CatcherHooks

import com.flora.app.core.FloraConfig
import com.flora.app.core.PinnedTrust
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Heartbeat (presence) client (Phase 8, panel phase 2 ke server
 * ke liye). App foreground par, licence check ke baad, linked
 * accounts ke liye ek chhota POST — panel ka last_seen column
 * isi se bharta hai.
 *
 * Contract: POST {SUPABASE_URL}/functions/v1/heartbeat,
 * Headers: apikey <anon>, Authorization Bearer <user JWT>,
 * Body: {"user_id": "<JWT user se match karta uid>"}.
 * Sirf last_seen timestamp jata hai — koi content, location
 * ya screen data kabhi nahi.
 *
 * Silent-skip (sakht niyam): function 404 de, network fail ho,
 * ya JWT na mile — har case me chupchaap skip, app na rukegi,
 * na crash hogi, na user ko koi message dikhega. Unlinked
 * accounts (koi JWT nahi) heartbeat bhejte hi nahi.
 */
object HeartbeatClient {

    suspend fun send(userId: String, accessToken: String, anonKey: String) {
        if (userId.isEmpty() || accessToken.isEmpty() || anonKey.isEmpty()) return
        withContext(Dispatchers.IO) {
            try {
                val url = URL(FloraConfig.SUPABASE_URL + FloraConfig.HEARTBEAT_PATH)
                val conn = PinnedTrust.openConnection(url) as HttpURLConnection
                try {
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 15_000
                    conn.readTimeout = 15_000
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("apikey", anonKey)
                    conn.setRequestProperty("Authorization", "Bearer $accessToken")
                    val body = "{\"user_id\":\"$userId\"}"
                    conn.outputStream.use {
                        it.write(body.toByteArray(StandardCharsets.UTF_8))
                    }
                    // Response code kuch bhi ho (200 ya 404 ya 403):
                    // sirf padh kar chhod do — heartbeat kabhi
                    // app ka kaam nahi rokta.
                    try { conn.responseCode } catch (e: Exception) {
                        CatcherHooks.networkError("heartbeat", "exception", "exception")
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                CatcherHooks.networkError("heartbeat", "exception", "exception")
                // Silent skip — upar ke niyam ke mutaabiq.
            }
        }
    }
}
