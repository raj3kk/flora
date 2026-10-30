package com.flora.app.assistant

import com.flora.app.core.PinnedTrust
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Provider lanes (parity batch 2026-10-31 — brain inventory §I
 * missing lanes). 8 providers OpenAI-compatible /chat/completions
 * shape par chalte hain, Anthropic alag client hai. Key ki value
 * kabhi log nahi hoti, sirf HTTP code laneCodes me record hota
 * hai (BrainClient ke Catcher record ke liye).
 *
 * Default models starting points hain — provider apne model
 * naam badal sakta hai; galat naam par lane 404 dekar agle
 * lane par gir jayega, kabhi fake jawab nahi dega. Koi bhi
 * lane kharcha kiye bina probe nahi hota: checkKeys sirf
 * /models GET karta hai, generation call sirf ask() me hoti hai.
 */
object ProviderLanes {

    data class ProviderDef(
        val displayName: String,
        val baseUrl: String,
        val defaultModel: String,
        val extraHeaders: Map<String, String> = emptyMap(),
    )

    val OPENAI_COMPATIBLE: List<ProviderDef> = listOf(
        ProviderDef("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
        ProviderDef("Mistral", "https://api.mistral.ai/v1", "mistral-small-latest"),
        ProviderDef("Together", "https://api.together.xyz/v1", "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo"),
        ProviderDef("xAI", "https://api.x.ai/v1", "grok-3-mini"),
        ProviderDef("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat"),
        ProviderDef("DeepInfra", "https://api.deepinfra.com/v1/openai", "meta-llama/Meta-Llama-3.1-8B-Instruct"),
        ProviderDef("NVIDIA", "https://integrate.api.nvidia.com/v1", "meta/llama-3.1-8b-instruct"),
        ProviderDef(
            "OpenRouter", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini",
            mapOf("HTTP-Referer" to "https://flora-henna-alpha.vercel.app", "X-Title" to "Flora")
        ),
    )

    const val ANTHROPIC_BASE = "https://api.anthropic.com"
    const val ANTHROPIC_MODEL = "claude-3-5-haiku-20241022"
    const val ANTHROPIC_VERSION = "2023-06-01"

    private const val SYSTEM_INSTRUCTION = "Tum Flora ho, ek friendly AI assistant. " +
        "Muskura kar, pyar se, meethi boli me jawab do — jaise apne dost se has kar baat " +
        "karte hain. Chhota jawab do, aam taur par 1-2 line me, lamba ya formal mat bano. " +
        "Hindi/English mix me, user jis language me bole usi me jawab do. Kaam ki baat " +
        "pehle; extra detail sirf poochhe par. Jawab me kabhi emoji, markdown formatting " +
        "ya technical baat (code, model ka naam, key ki baat) mat likhna — sirf saaf, " +
        "saadi boli me jawab do."

    private fun postJsonRaw(
        urlText: String,
        body: JSONObject,
        headers: Map<String, String>,
    ): Pair<Int, String?> {
        val conn = PinnedTrust.openConnection(URL(urlText)) as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 20_000
            conn.readTimeout = 30_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            for ((name, value) in headers) conn.setRequestProperty(name, value)
            conn.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val text = try {
                BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
            } catch (e: Exception) {
                null
            }
            conn.responseCode to text
        } catch (e: Exception) {
            -1 to null
        } finally {
            conn.disconnect()
        }
    }

    private fun openAiMessages(
        prompt: String,
        memoryContext: String,
        history: List<BrainClient.ChatTurn>,
    ): JSONArray {
        val messages = JSONArray()
        val system = SYSTEM_INSTRUCTION +
            (if (memoryContext.isNotEmpty()) " User ke baare me tumhe ye yaad hai " +
                "(sirf tab istemal karo jab sawaal se juda ho, kabhi seedha " +
                "repeat mat karo): $memoryContext." else "")
        messages.put(JSONObject().put("role", "system").put("content", system))
        for (turn in history) {
            messages.put(
                JSONObject().put("role", if (turn.fromUser) "user" else "assistant")
                    .put("content", turn.text)
            )
        }
        messages.put(JSONObject().put("role", "user").put("content", prompt))
        return messages
    }

    /** OpenAI-compatible lane: code + reply (fail par null reply). */
    fun openAiCompatibleAsk(
        def: ProviderDef,
        key: String,
        prompt: String,
        memoryContext: String,
        history: List<BrainClient.ChatTurn>,
    ): Pair<Int, String?> {
        if (key.isEmpty()) return -1 to null
        val body = JSONObject()
            .put("model", def.defaultModel)
            .put("messages", openAiMessages(prompt, memoryContext, history))
        val headers = mutableMapOf("Authorization" to "Bearer $key")
        headers.putAll(def.extraHeaders)
        val (code, text) = postJsonRaw("${def.baseUrl}/chat/completions", body, headers)
        if (code !in 200..299 || text == null) return code to null
        val reply = try {
            JSONObject(text).optJSONArray("choices")
                ?.optJSONObject(0)?.optJSONObject("message")?.optString("content", "")
                .orEmpty()
        } catch (e: Exception) {
            ""
        }
        return code to (reply.ifBlank { null })
    }

    /** Anthropic lane (alag shape): code + reply (fail par null). */
    fun anthropicAsk(
        key: String,
        prompt: String,
        memoryContext: String,
        history: List<BrainClient.ChatTurn>,
    ): Pair<Int, String?> {
        if (key.isEmpty()) return -1 to null
        val messages = JSONArray()
        var lastRole: String? = null
        fun add(role: String, text: String) {
            if (text.isBlank()) return
            if (lastRole == role) {
                val prev = messages.getJSONObject(messages.length() - 1)
                prev.put("content", prev.optString("content") + "\n" + text)
            } else {
                messages.put(JSONObject().put("role", role).put("content", text))
                lastRole = role
            }
        }
        for (turn in history) add(if (turn.fromUser) "user" else "assistant", turn.text)
        add("user", prompt)
        val system = SYSTEM_INSTRUCTION +
            (if (memoryContext.isNotEmpty()) " User ke baare me tumhe ye yaad hai " +
                "(sirf tab istemal karo jab sawaal se juda ho, kabhi seedha " +
                "repeat mat karo): $memoryContext." else "")
        val body = JSONObject()
            .put("model", ANTHROPIC_MODEL)
            .put("max_tokens", 1024)
            .put("system", system)
            .put("messages", messages)
        val (code, text) = postJsonRaw(
            "$ANTHROPIC_BASE/v1/messages",
            body,
            mapOf("x-api-key" to key, "anthropic-version" to ANTHROPIC_VERSION)
        )
        if (code !in 200..299 || text == null) return code to null
        val reply = try {
            JSONObject(text).optJSONArray("content")
                ?.optJSONObject(0)?.optString("text", "")
                .orEmpty()
        } catch (e: Exception) {
            ""
        }
        return code to (reply.ifBlank { null })
    }
}
