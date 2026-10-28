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
 * Brain lane order: Gemini (user's free-tier key) first, then Groq
 * (user's key) as backup, then an honest message. No key is bundled
 * with the app. A failed lane returns null internally so the next
 * lane can try; only the final message reaches the user.
 */
class BrainClient(private val prefs: FloraPreferences) {

    suspend fun ask(prompt: String): String = withContext(Dispatchers.IO) {
        val geminiKey = prefs.geminiApiKey()
        val geminiKey2 = prefs.geminiApiKey2()
        val groqKey = prefs.groqApiKey()
        val groqKey2 = prefs.groqApiKey2()
        // Fixed-order rotation (v2.1): Gemini 1 -> Gemini 2 -> Groq 1 -> Groq 2.
        // Key value kabhi log/print nahi hoti, sirf lane try hota hai.
        geminiAsk(prompt, geminiKey) ?: geminiAsk(prompt, geminiKey2)
            ?: groqAsk(prompt, groqKey) ?: groqAsk(prompt, groqKey2)
            ?: "Dono brain lane nahi chale. Settings me apni free Gemini key " +
                "ya Groq key daaliye, phir dobara koshish kijiye."
    }

    private fun postJson(urlText: String, body: JSONObject, authHeader: String?): String? {
        val conn = URL(urlText).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 20_000
            conn.readTimeout = 30_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            if (authHeader != null) conn.setRequestProperty("Authorization", authHeader)
            conn.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
            if (conn.responseCode !in 200..299) null else text
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun geminiAsk(prompt: String, key: String): String? {
        if (key.isEmpty()) return null
        val body = JSONObject()
            .put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put(
                        "text",
                        "Tum Flora ho, ek friendly AI assistant. User se aise baat karo " +
                            "jaise dost se casual conversation ho — chhota jawab do, " +
                            "aam taur par 1-2 line me, lamba ya formal mat bano. " +
                            "Hindi/English mix me, user jis language me bole usi me " +
                            "jawab do. Kaam ki baat pehle; extra detail sirf poochhe " +
                            "par. Sawal: $prompt"
                    )
                ))
            ))
        val text = postJson(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$key",
            body, null
        ) ?: return null
        return try {
            val reply = JSONObject(text).optJSONArray("candidates")
                ?.optJSONObject(0)?.optJSONObject("content")
                ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text", "")
                .orEmpty()
            reply.ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    private fun groqAsk(prompt: String, key: String): String? {
        if (key.isEmpty()) return null
        val body = JSONObject()
            .put("model", "llama-3.1-8b-instant")
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put(
                    "content",
                    "Tum Flora ho, ek friendly AI assistant. User se aise baat karo " +
                        "jaise dost se casual conversation ho — chhota jawab do, " +
                        "aam taur par 1-2 line me, lamba ya formal mat bano. " +
                        "Hindi/English mix me, user jis language me bole usi me " +
                        "jawab do. Kaam ki baat pehle; extra detail sirf poochhe par."
                ))
                .put(JSONObject().put("role", "user").put("content", prompt))
            )
        val text = postJson(
            "https://api.groq.com/openai/v1/chat/completions",
            body, "Bearer $key"
        ) ?: return null
        return try {
            val reply = JSONObject(text).optJSONArray("choices")
                ?.optJSONObject(0)?.optJSONObject("message")?.optString("content", "")
                .orEmpty()
            reply.ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }
}
