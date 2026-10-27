package com.flora.app.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Live screen vision (v1.2): user ke sawaal par screen ka EK frame
 * Gemini vision ko bheja jaata hai — sirf tab jab user khud button
 * dabakar poochhe, aur frame lene ke baad projection turant band
 * ho jaata hai. Continuous chhup kar screen dekhna kabhi nahi hota;
 * MediaProjection ka system indicator capture ke waqt dikhta hai.
 * Sirf text (OCR) nahi — poori tasveer: icon, colour, layout, jo
 * kuch screen par dikh raha hai, uska jawab.
 * Gemini key user ki apni Settings wali hai; key na ho to saaf
 * mana kar diya jaata hai, koi server key use nahi hoti.
 */
class VisionClient {

    suspend fun askAboutImage(prompt: String, jpeg: ByteArray, key: String): String? =
        withContext(Dispatchers.IO) {
            if (key.isEmpty()) return@withContext null
            val body = JSONObject()
                .put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray()
                        .put(JSONObject().put(
                            "text",
                            "Tum Flora ho. Is screen ki tasveer ko poori tarah " +
                                "dekho — sirf text nahi, icon, button, colour, layout, " +
                                "sab kuch — aur Hindi/English mix me chhota, saaf " +
                                "jawab do. Sawal: $prompt"
                        ))
                        .put(JSONObject().put("inline_data", JSONObject()
                            .put("mime_type", "image/jpeg")
                            .put("data", Base64.getEncoder().encodeToString(jpeg))
                        ))
                    )
                ))
            val conn = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-1.5-flash:generateContent?key=$key"
            ).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 20_000
                conn.readTimeout = 30_000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val text = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
                if (conn.responseCode !in 200..299) return@withContext null
                val reply = JSONObject(text).optJSONArray("candidates")
                    ?.optJSONObject(0)?.optJSONObject("content")
                    ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text", "")
                    .orEmpty()
                reply.ifBlank { null }
            } catch (e: Exception) {
                null
            } finally {
                conn.disconnect()
            }
        }
}
