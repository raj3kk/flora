package com.flora.app.voice
import com.flora.app.data.CatcherHooks

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
 * Phase 1 (server-shift) Item B — tts-proxy app client.
 *
 * Server function (supabase/functions/tts-proxy) ka code kabhi
 * nahi chhua gaya — sirf ye app client hai. Deepgram key
 * server-side ONLY hai; app me koi provider key nahi aati,
 * koi key log/response me nahi jaati. Text bhi kabhi log
 * nahi hota — sirf HTTP code + error key dekhi jaati hai.
 *
 * Caller ko linked Supabase Auth JWT chahiye (server JWT
 * gate: JWT user hi user hai). JWT LicenceClient ke
 * refreshAccessToken() se milta hai — koi naya token flow,
 * koi secret app me save nahi. Rotated refresh token mile
 * to prefs me save hota hai (SessionTokenCache wala hi
 * pattern — warna agla refresh purane token se fail hoga).
 *
 * Result imaandaar hai: Audio sirf tab jab server ne sachhi
 * audio bytes di hon; Unavailable/Error/FreeLimit par caller
 * (VoiceEngine) SYSTEM TTS FALLBACK par girta hai — awaaz
 * kabhi nahi rukti. FreeLimit par fallback chalta hai kyunki
 * ye TTS lane ka cap hai, plan-generation ka server cap
 * nahi (PlannerBrain ka FreeLimit no-bypass rule yahan
 * nahi lagta — wahan direct lane provider key burn karta,
 * yahan fallback on-device system TTS hai, koi key nahi).
 *
 * LIVE-GATE: Deepgram key abhi PENDING hai — jab tak key
 * set + function deploy nahi hota, tts-proxy 503 dega aur
 * app system TTS par hi bolegi. Ye fallback tab tak rahega;
 * key invent nahi ki gayi.
 */
class ServerTtsClient(
    private val prefs: FloraPreferences,
    private val licenceClient: LicenceClient,
) {
    sealed class TtsResult {
        data class Audio(val bytes: ByteArray) : TtsResult()
        data class Unavailable(val reason: String) : TtsResult()
        data class FreeLimit(val message: String) : TtsResult()
        data class Error(val reason: String) : TtsResult()
    }

    companion object {
        /** Server cap (tts-proxy v1): text 1000 chars se zyada nahi. Pure — unit test me. */
        fun capText(text: String): String = text.take(1000)

        /** Free-limit pehchaan: 429 ya error key "free_limit". Pure — unit test me. */
        fun isFreeLimit(code: Int, error: String): Boolean =
            code == 429 || error == "free_limit"
    }

    suspend fun fetchAudio(text: String): TtsResult = withContext(Dispatchers.IO) {
        val capped = capText(text)
        if (capped.isBlank()) {
            return@withContext TtsResult.Error("bolne ke liye text khali mila.")
        }
        val refresh = try {
            prefs.linkedRefreshToken()
        } catch (e: Exception) {
            CatcherHooks.networkError("server-tts", "exception", "exception")
            ""
        }
        if (refresh.isBlank()) {
            return@withContext TtsResult.Unavailable(
                "tts-proxy ke liye app-link (linked account) chahiye — " +
                    "linked nahi hai, isliye proxy call nahi ho sakti."
            )
        }
        val pair = try {
            licenceClient.refreshAccessToken(refresh)
        } catch (e: Exception) {
            CatcherHooks.networkError("server-tts", "exception", "exception")
            null
        } ?: return@withContext TtsResult.Unavailable(
            "tts-proxy ka session refresh nahi hua — dobara link karo."
        )
        val jwt = pair.first
        if (jwt.isBlank()) {
            return@withContext TtsResult.Unavailable("tts-proxy ka session khali mila.")
        }
        val next = pair.second
        if (!next.isNullOrEmpty() && next != refresh) {
            try {
                prefs.setLink(prefs.linkedUserId(), prefs.linkedEmail(), next)
            } catch (e: Exception) {
                CatcherHooks.networkError("server-tts", "exception", "exception")
            }
        }
        val body = JSONObject().put("text", capped)
        val conn = PinnedTrust.openConnection(
            URL(FloraConfig.SUPABASE_URL + "/functions/v1/tts-proxy")
        ) as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $jwt")
            conn.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val errText = try {
                    BufferedReader(InputStreamReader(conn.errorStream, StandardCharsets.UTF_8)).readText()
                } catch (e: Exception) {
                    CatcherHooks.networkError("server-tts", "exception", "exception")
                    ""
                }
                val err = try {
                    JSONObject(errText).optString("error", "")
                } catch (e: Exception) {
                    CatcherHooks.networkError("server-tts", "exception", "exception")
                    ""
                }
                if (isFreeLimit(code, err)) {
                    val msg = try {
                        JSONObject(errText).optString("message", "")
                    } catch (e: Exception) {
                        CatcherHooks.networkError("server-tts", "exception", "exception")
                        ""
                    }
                    return@withContext TtsResult.FreeLimit(
                        msg.ifBlank { "Free limit poora ho gaya hai — Pro lene par unlimited milega." }
                    )
                }
                return@withContext TtsResult.Error("tts-proxy ne mana kiya (code $code)")
            }
            val bytes = try {
                conn.inputStream.readBytes()
            } catch (e: Exception) {
                CatcherHooks.networkError("server-tts", "exception", "exception")
                ByteArray(0)
            }
            if (bytes.isEmpty()) {
                return@withContext TtsResult.Error("tts-proxy se khali audio mila.")
            }
            return@withContext TtsResult.Audio(bytes)
        } catch (e: Exception) {
            CatcherHooks.networkError("server-tts", "exception", "exception")
            return@withContext TtsResult.Error("tts-proxy se network nahi juda.")
        } finally {
            conn.disconnect()
        }
    }
}
