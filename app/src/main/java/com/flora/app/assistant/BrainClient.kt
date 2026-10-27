package com.flora.app.assistant

import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Brain lane: Gemini (user's own free-tier key, entered in Settings)
 * first, honest local fallback second. No key is bundled with the app.
 * No answer is ever invented as a "server reply" — a failed call says so.
 */
class BrainClient(private val prefs: FloraPreferences) {

    suspend fun ask(prompt: String): String = withContext(Dispatchers.IO) {
        val key = prefs.geminiApiKey()
        if (key.isEmpty()) {
            return@withContext "Gemini key abhi set nahi hai. Settings me apni free Gemini key daaliye, " +
                "tab main poora jawab de paungi. Tab tak: aapka message mil gaya — \"$prompt\"."
        }
        try {
            val url = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$key"
            )
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 20_000
                conn.readTimeout = 30_000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val body = JSONObject()
                    .put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().put(
                            JSONObject().put(
                                "text",
                                "Tum Flora ho, ek friendly aur professional AI assistant. " +
                                    "Hindi/English mix me, chhota aur saaf jawab do. Sawal: $prompt"
                            )
                        ))
                    ))
                conn.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val text = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
                if (conn.responseCode !in 200..299) {
                    return@withContext "Gemini se jawab nahi mila (server error). Key ya network check karke dobara koshish kijiye."
                }
                val candidates = JSONObject(text).optJSONArray("candidates")
                val reply = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "")
                    .orEmpty()
                if (reply.isBlank()) "Gemini ne khaali jawab diya, dobara koshish kijiye." else reply
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            "Network ya server ki dikkat se jawab nahi mila. Dobara koshish kijiye."
        }
    }
}
