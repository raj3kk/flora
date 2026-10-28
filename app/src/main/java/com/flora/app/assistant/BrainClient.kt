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
 * with the app, and no key value is ever logged, printed or sent
 * anywhere except its own provider endpoint.
 *
 * v2.12 fix — "key daali phir bhi key maang raha hai" wali shikayat
 * ki jad: pehle har failure par ek hi generic message aata tha,
 * chahe key saved hi na ho, chahe saved key galat/expired ho.
 * Ab failure ka kaaran alag bataya jata hai (khali / galat /
 * quota / network), aur Settings me "Keys check karo" button se
 * har lane ki validity bina generation cost ke check hoti hai.
 */
class BrainClient(private val prefs: FloraPreferences) {

    enum class BrainStatus { OK, NO_KEYS, KEYS_INVALID, QUOTA, NETWORK }

    data class LaneCheck(
        val name: String,
        val saved: Boolean,
        val valid: Boolean,
        val note: String,
    )

    private data class PostResult(val code: Int, val body: String?)

    suspend fun ask(prompt: String): String = withContext(Dispatchers.IO) {
        val keys = listOf(
            prefs.geminiApiKey().trim(),
            prefs.geminiApiKey2().trim(),
            prefs.groqApiKey().trim(),
            prefs.groqApiKey2().trim(),
        )
        if (keys.all { it.isEmpty() }) {
            return@withContext "Koi brain key saved nahi hai. Settings me apni " +
                "free Gemini key daalo, 'Keys save karo' dabao, phir " +
                "'Keys check karo' se verify karo."
        }
        val codes = mutableListOf<Int>()
        val reply = geminiAsk(prompt, keys[0], codes)
            ?: geminiAsk(prompt, keys[1], codes)
            ?: groqAsk(prompt, keys[2], codes)
            ?: groqAsk(prompt, keys[3], codes)
        if (reply != null) return@withContext reply
        when {
            codes.any { it == 401 || it == 403 } ->
                "Key saved hai par kaam nahi kar rahi — key galat ya " +
                    "expired ho sakti hai. Settings me 'Keys check karo' " +
                    "se dekho kaunsi lane fail hai, phir sahi key " +
                    "dobara daal kar save karo."
            codes.any { it == 429 } ->
                "Key sahi lag rahi hai par uska quota khatam ho gaya " +
                    "hai (free tier limit). Thodi der baad ya doosri " +
                    "key se koshish karo."
            else ->
                "Network ya server se baat nahi ho payi. Internet " +
                    "check karke dobara koshish karo."
        }
    }

    /**
     * Har lane ki validity check — bina generation call ke (sirf
     * models-list GET, isliye koi token kharch nahi hota). Key ki
     * value kabhi result me nahi aati, sirf saved/valid status.
     */
    suspend fun checkKeys(): List<LaneCheck> = withContext(Dispatchers.IO) {
        val checks = mutableListOf<LaneCheck>()
        val gemini = prefs.geminiApiKey().trim()
        val gemini2 = prefs.geminiApiKey2().trim()
        val groq = prefs.groqApiKey().trim()
        val groq2 = prefs.groqApiKey2().trim()
        checks += checkOne("Gemini 1", gemini) { key ->
            getCode(
                "https://generativelanguage.googleapis.com/v1beta/models?key=$key",
                null
            )
        }
        checks += checkOne("Gemini 2", gemini2) { key ->
            getCode(
                "https://generativelanguage.googleapis.com/v1beta/models?key=$key",
                null
            )
        }
        checks += checkOne("Groq 1", groq) { key ->
            getCode("https://api.groq.com/openai/v1/models", "Bearer $key")
        }
        checks += checkOne("Groq 2", groq2) { key ->
            getCode("https://api.groq.com/openai/v1/models", "Bearer $key")
        }
        checks
    }

    private fun checkOne(name: String, key: String, probe: (String) -> Int): LaneCheck {
        if (key.isEmpty()) {
            return LaneCheck(name, saved = false, valid = false, note = "saved nahi hai")
        }
        return when (val code = probe(key)) {
            in 200..299 -> LaneCheck(name, saved = true, valid = true, note = "sahi hai, kaam karegi")
            401, 403 -> LaneCheck(name, saved = true, valid = false, note = "galat ya expired hai — dobara daalo")
            429 -> LaneCheck(name, saved = true, valid = false, note = "quota khatam hai, baad me chalegi")
            -1 -> LaneCheck(name, saved = true, valid = false, note = "network se check nahi ho paya")
            else -> LaneCheck(name, saved = true, valid = false, note = "server ne mana kiya (code $code)")
        }
    }

    private fun getCode(urlText: String, authHeader: String?): Int {
        val conn = URL(urlText).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            if (authHeader != null) conn.setRequestProperty("Authorization", authHeader)
            conn.responseCode
        } catch (e: Exception) {
            -1
        } finally {
            conn.disconnect()
        }
    }

    private fun postJson(urlText: String, body: JSONObject, authHeader: String?): PostResult {
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
            val text = try {
                BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
            } catch (e: Exception) {
                null
            }
            PostResult(conn.responseCode, text)
        } catch (e: Exception) {
            PostResult(-1, null)
        } finally {
            conn.disconnect()
        }
    }

    private fun geminiAsk(prompt: String, key: String, codes: MutableList<Int>): String? {
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
        val result = postJson(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$key",
            body, null
        )
        codes += result.code
        val text = result.body ?: return null
        if (result.code !in 200..299) return null
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

    private fun groqAsk(prompt: String, key: String, codes: MutableList<Int>): String? {
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
        val result = postJson(
            "https://api.groq.com/openai/v1/chat/completions",
            body, "Bearer $key"
        )
        codes += result.code
        val text = result.body ?: return null
        if (result.code !in 200..299) return null
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
