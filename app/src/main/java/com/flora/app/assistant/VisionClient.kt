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
 * Live screen vision (v1.3): user ke sawaal par screen ka frame
 * Gemini vision ko bheja jaata hai — sirf tab jab user khud
 * button dabakar poochhe. Frame sirf user ke sawaal ke liye
 * vision ko jaata hai (user ki Gemini key se, opt-in).
 *
 * v2.22 ROOT FIX — "screen nahi dekh pa rahi" ki jad: ye file
 * pehle hardcoded gemini-1.5-flash call karti thi, jo retired
 * ho chuka hai — har vision call 404 par girti thi, isliye
 * screen ka jawab kabhi aata hi nahi tha. Ab chat wali
 * auto-discovery chain hi use hoti hai (Maya ka bhi fixed
 * model nahi tha — "latest free chain" tha): models-list se
 * jo flash models chalte hain, unhi ko try karo, 404 par
 * agle par jao. Koi hardcoded retired naam nahi.
 * Gemini key user ki apni Settings wali hai; key na ho to
 * saaf null, koi server key use nahi hoti.
 */
class VisionClient {

    suspend fun askAboutImage(prompt: String, jpeg: ByteArray, key: String): String? =
        withContext(Dispatchers.IO) {
            if (key.isEmpty()) return@withContext null
            val discovered = discoverVisionModels(key)
            val models = if (discovered.isNotEmpty()) discovered.take(3) else
                listOf("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.0-flash")
            for (model in models) {
                val reply = tryOne(prompt, jpeg, key, model)
                if (reply != null) return@withContext reply
            }
            null
        }

    private fun tryOne(prompt: String, jpeg: ByteArray, key: String, model: String): String? {
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
                "$model:generateContent?key=$key"
        ).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 20_000
            conn.readTimeout = 30_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val text = try {
                BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
            } catch (e: Exception) {
                ""
            }
            if (conn.responseCode !in 200..299) return null
            val reply = JSONObject(text).optJSONArray("candidates")
                ?.optJSONObject(0)?.optJSONObject("content")
                ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text", "")
                .orEmpty()
            return reply.ifBlank { null }
        } catch (e: Exception) {
            return null
        } finally {
            conn.disconnect()
        }
    }

    /** Chat wali auto-discovery ka vision version: jo models
     *  generateContent kar sakte hain, flash-prefer order me. */
    private fun discoverVisionModels(key: String): List<String> {
        return try {
            val conn = URL(
                "https://generativelanguage.googleapis.com/v1beta/models?key=$key"
            ).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                if (conn.responseCode !in 200..299) return emptyList()
                val text = BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8)).readText()
                val names = mutableListOf<String>()
                val arr = JSONObject(text).optJSONArray("models") ?: return emptyList()
                for (i in 0 until arr.length()) {
                    val m = arr.optJSONObject(i) ?: continue
                    val methods = m.optJSONArray("supportedGenerationMethods")
                    var ok = false
                    if (methods != null) {
                        for (j in 0 until methods.length()) {
                            if (methods.optString(j) == "generateContent") { ok = true; break }
                        }
                    }
                    if (!ok) continue
                    val full = m.optString("name", "")
                    val short = full.removePrefix("models/")
                    if (short.isBlank() || short.contains("image") || short.contains("tts") ||
                        short.contains("embedding") || short.contains("aqa")
                    ) continue
                    names += short
                }
                val flash = names.filter { it.contains("flash") }
                val ordered = mutableListOf<String>()
                val first = names.firstOrNull { it == "gemini-2.5-flash" }
                val lite = names.firstOrNull { it == "gemini-2.5-flash-lite" }
                if (first != null) ordered += first
                if (lite != null && lite != first) ordered += lite
                for (n in flash) if (n !in ordered) ordered += n
                for (n in names) if (n !in ordered) ordered += n
                ordered
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
