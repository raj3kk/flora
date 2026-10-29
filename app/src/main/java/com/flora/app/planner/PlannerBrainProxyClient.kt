package com.flora.app.planner

import com.flora.app.core.FloraConfig
import com.flora.app.core.PinnedTrust
import com.flora.app.data.FloraPreferences
import com.flora.app.data.LicenceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * B4 planner ke liye NAYA server-brain-proxy app client (F05-a).
 * Server function ka code (supabase/functions/brain-proxy) kabhi
 * nahi chhua gaya — sirf ye app client hai.
 *
 * Proxy ko linked Supabase Auth JWT chahiye (server JWT gate:
 * user_id token se match hona chahiye). JWT LicenceClient ke
 * public refreshAccessToken() se milta hai — koi naya token
 * flow, koi secret app me save nahi. Token value kabhi log
 * nahi hoti, sirf HTTP code + error key dekhi jati hai.
 *
 * BrainClient direct lanes (Gemini/Groq HttpURLConnection) ko
 * "brain-proxy use ho gaya" kehna MANA hai — isliye result me
 * source alag-alag hai: PROXY vs DIRECT_LANE, kabhi mix nahi.
 * B3 third-party clients (GitHub/Vercel/Supabase) ka
 * "brain-proxy kabhi nahi" rule alag lane hai — planner AI lane
 * par wo rule nahi lagta, dono alag hain.
 */
class PlannerBrainProxyClient(
    private val prefs: FloraPreferences,
    private val licenceClient: LicenceClient,
) {
    sealed class ProxyResult {
        data class Reply(val text: String) : ProxyResult()
        data class Unavailable(val reason: String) : ProxyResult()
        data class FreeLimit(val message: String) : ProxyResult()
        data class Error(val reason: String) : ProxyResult()
    }

    suspend fun askPlan(prompt: String, deviceId: String): ProxyResult = withContext(Dispatchers.IO) {
        val userId = try {
            prefs.linkedUserId()
        } catch (e: Exception) {
            ""
        }
        val refresh = try {
            prefs.linkedRefreshToken()
        } catch (e: Exception) {
            ""
        }
        if (userId.isBlank() || refresh.isBlank()) {
            return@withContext ProxyResult.Unavailable(
                "brain-proxy ke liye app-link (linked account) chahiye — " +
                    "linked nahi hai, isliye proxy call nahi ho sakti."
            )
        }
        val pair = try {
            licenceClient.refreshAccessToken(refresh)
        } catch (e: Exception) {
            null
        } ?: return@withContext ProxyResult.Unavailable(
            "brain-proxy ka session refresh nahi hua — dobara link karo."
        )
        val jwt = pair.first
        if (jwt.isBlank()) {
            return@withContext ProxyResult.Unavailable("brain-proxy ka session khali mila.")
        }
        val body = JSONObject()
            .put("user_id", userId)
            .put("device_id", deviceId)
            .put("prompt", prompt.take(4000))
        val conn = PinnedTrust.openConnection(
            URL(FloraConfig.SUPABASE_URL + "/functions/v1/brain-proxy")
        ) as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $jwt")
            conn.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = try {
                BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
            } catch (e: Exception) {
                ""
            }
            if (code !in 200..299) {
                val err = try {
                    JSONObject(text).optString("error", "")
                } catch (e: Exception) {
                    ""
                }
                if (code == 429 || err == "free_limit") {
                    val msg = try {
                        JSONObject(text).optString("message", "")
                    } catch (e: Exception) {
                        ""
                    }
                    return@withContext ProxyResult.FreeLimit(
                        msg.ifBlank { "Free limit poora ho gaya hai — Pro lene par unlimited milega." }
                    )
                }
                return@withContext ProxyResult.Error("brain-proxy ne mana kiya (code $code)")
            }
            val reply = try {
                JSONObject(text).optString("reply", "")
            } catch (e: Exception) {
                ""
            }
            if (reply.isBlank()) {
                return@withContext ProxyResult.Error("brain-proxy ka jawab khali mila.")
            }
            return@withContext ProxyResult.Reply(reply)
        } catch (e: Exception) {
            return@withContext ProxyResult.Error("brain-proxy se network nahi juda.")
        } finally {
            conn.disconnect()
        }
    }
}
