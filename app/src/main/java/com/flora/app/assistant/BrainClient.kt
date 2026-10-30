package com.flora.app.assistant

import com.flora.app.core.PinnedTrust
import com.flora.app.data.CatcherEvents
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

    /**
     * Phase 2 voice pipeline: chat history ka ek turn.
     * fromUser = user ka message, warna Flora ka jawab.
     * FloraApp isko apne in-memory messages se banata hai
     * (assistant lines ka "Flora: " prefix hat kar).
     */
    data class ChatTurn(val fromUser: Boolean, val text: String)

    private data class PostResult(val code: Int, val body: String?)

    /**
     * Lane A — A1 (R-A1-3/R-A1-5): transientNote sirf is ek call ke
     * liye provider prompt ke aage judta hai (jaise personaPrefix) —
     * memoryContext/history me kabhi nahi jata, stored turn me kabhi
     * nahi jata, server contract (request shape) nahi badalta.
     * Default khali hai, isliye saare purane call sites bina badle
     * compile hote hain. BasicTalk (no-keys) raaste par note nahi
     * judta — local canned jawab pehle se chhota hota hai.
     */
    suspend fun ask(prompt: String, memoryContext: String = "", history: List<ChatTurn> = emptyList(), transientNote: String = ""): String = withContext(Dispatchers.IO) {
        // Wave B, D8 "dono gate": direct provider calls se PEHLE
        // app-side licence gate (server gate proxy lane par hai,
        // LFMS server builder). Blocked ho to koi provider call
        // nahi, koi kharcha nahi — imaandaar message hi jawab hai.
        // BasicTalk local hai, isliye gate ke baad bhi chalta hai
        // (no-keys path) — gate sirf provider use rokta hai.
        when (val gate = com.flora.app.data.LicenceGate.check(prefs)) {
            is com.flora.app.data.LicenceGate.Result.Blocked ->
                return@withContext gate.message
            else -> {}
        }
        val cappedHistory = capHistory(history)
        val keys = listOf(
            prefs.geminiApiKey().trim(),
            prefs.geminiApiKey2().trim(),
            prefs.groqApiKey().trim(),
            prefs.groqApiKey2().trim(),
        )
        if (keys.all { it.isEmpty() }) {
            // v2.20 user order: Flora apne bare me technical baat
            // kabhi nahi bolegi — na "key", na "code", na model ka
            // naam. Basic baat local hi ho jayegi; gehra sawaal ho
            // to pyar se ek line me line jodne ko bolegi.
            return@withContext BasicTalk.reply(prompt)
                ?: "Main tumse baat to kar rahi hun, par gehre sawaalon " +
                    "ke liye meri soch wali line abhi judi nahi hai. " +
                    "Settings me meri line jod do, phir main sab kuch " +
                    "achhe se bata paungi. Tab tak kuch halka poochho na!"
        }
        // v2.22 model-chain fix (user ki key par 2026-10-28 ko
        // live verify): gemini-2.5-flash aur 2.5-flash-lite is key
        // par 404 dete hain (do baar check), jabki
        // gemini-flash-lite-latest, gemini-3-flash-preview aur
        // gemini-3.5-flash 200 par chalte hain. Pehle chain sirf
        // 3 models tak jaati thi — working model chautha tha, isliye
        // baat kabhi banti hi nahi thi. Ab 6 tak try + har failure
        // par agla (quota bhi per-model hota hai, isliye 429 par
        // bhi rukna galat tha). Key ki value kahin log nahi hoti.
        val laneCodes = mutableListOf<Pair<String, Int>>()
        // Transient-only: note sirf provider prompt me, memory/history me nahi.
        val providerPrompt = if (transientNote.isBlank()) prompt else transientNote + "\n" + prompt
        val reply = geminiAsk(providerPrompt, keys[0], laneCodes, memoryContext, cappedHistory)
            ?: geminiAsk(providerPrompt, keys[1], laneCodes, memoryContext, cappedHistory)
            ?: groqAsk(providerPrompt, keys[2], laneCodes, memoryContext, cappedHistory)
            ?: groqAsk(providerPrompt, keys[3], laneCodes, memoryContext, cappedHistory)
        if (reply != null) return@withContext reply
        // Catcher (G2): saare lanes fail — sirf HTTP codes record
        // hote hain, na model naam, na key, na body.
        CatcherEvents.record("API", "brain lanes fail codes=${laneCodes.map { it.second }}")
        // v2.20 user order: brain fail ho to chat me technical
        // error KABHI nahi — na code, na model naam, na "key
        // gadbad". Pehle basic baat local karo, phir warm line.
        // Asli kaaran sirf Settings > "Keys check karo" me dikhega.
        return@withContext BasicTalk.reply(prompt)
            ?: "Hmm, abhi meri soch wali line thodi atak gayi hai, " +
                "isliye poora jawab nahi de pa rahi hun. Tum phir se " +
                "bolo na, main sun rahi hun — main yahin hun, " +
                "tumhare saath."
    }

    /**
     * Har lane ki validity check — bina generation call ke (sirf
     * models-list GET, isliye koi token kharch nahi hota). Key ki
     * value kabhi result me nahi aati, sirf saved/valid status.
     */
    /** Saved keys check (Settings > "Keys check karo"): prefs se
     *  padh kar probe karta hai. Deepgram bhi shaamil hai. */
    suspend fun checkKeys(): List<LaneCheck> = withContext(Dispatchers.IO) {
        checkKeysFor(
            prefs.geminiApiKey().trim(),
            prefs.geminiApiKey2().trim(),
            prefs.groqApiKey().trim(),
            prefs.groqApiKey2().trim(),
            prefs.deepgramApiKey().trim(),
            prefs.braveApiKey().trim(),
            prefs.serpApiKey().trim(),
            prefs.mapsApiKey().trim(),
        )
    }

    /** Bhare hue (abhi save na hue) keys check (Settings >
     *  "Bhare hue keys check karo"): field values ko probe karta
     *  hai — save user ke haath me rehta hai, sahi ho ya galat.
     *  Key values kabhi log nahi hoti, sirf HTTP code dekha jata hai. */
    /** Batch 2 (F9): Brave/SerpAPI/Maps bhi isi check me — naye
     *  params ke defaults hain, isliye purani call sites (5 args)
     *  bina badle compile hoti hain; Settings wali call site
     *  8 args bhejti hai. Sirf validity batayi jati hai, key ki
     *  value kabhi result ya screen par nahi aati. */
    suspend fun checkKeysFor(
        gemini: String,
        gemini2: String,
        groq: String,
        groq2: String,
        deepgram: String,
        brave: String = "",
        serp: String = "",
        maps: String = "",
    ): List<LaneCheck> = withContext(Dispatchers.IO) {
        val checks = mutableListOf<LaneCheck>()
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
        checks += checkOne("Deepgram", deepgram) { key ->
            getCode("https://api.deepgram.com/v1/projects", "Token $key")
        }
        checks += checkOne("Brave", brave) { key ->
            getCode(
                "https://api.search.brave.com/res/v1/web/search?q=Flora&count=1",
                key, "X-Subscription-Token"
            )
        }
        checks += checkOne("SerpAPI", serp) { key ->
            getCode("https://serpapi.com/search.json?q=Flora&num=1&api_key=$key", null)
        }
        if (maps.isEmpty()) {
            checks += LaneCheck("Maps", saved = false, valid = false, note = "saved nahi hai")
        } else {
            checks += mapsCheck(maps)
        }
        checks
    }

    /** Maps validity: Google Geocode HTTP 200 par bhi body ka
     *  status dekhta hai — OK/ZERO_RESULTS = key sahi,
     *  REQUEST_DENIED = galat, OVER_QUERY_LIMIT = quota. */
    private fun mapsCheck(key: String): LaneCheck {
        val conn = PinnedTrust.openConnection(
            URL("https://maps.googleapis.com/maps/api/geocode/json?address=Delhi&key=$key")
        ) as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            if (conn.responseCode !in 200..299) {
                return LaneCheck("Maps", saved = true, valid = false, note = "server ne mana kiya (code ${conn.responseCode})")
            }
            val text = try {
                BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8)).readText()
            } catch (e: Exception) {
                ""
            }
            val status = try { JSONObject(text).optString("status", "") } catch (e: Exception) { "" }
            when (status) {
                "OK", "ZERO_RESULTS" -> LaneCheck("Maps", saved = true, valid = true, note = "sahi hai, kaam karegi")
                "REQUEST_DENIED" -> LaneCheck("Maps", saved = true, valid = false, note = "galat ya expired hai — dobara daalo")
                "OVER_QUERY_LIMIT" -> LaneCheck("Maps", saved = true, valid = false, note = "quota khatam hai, baad me chalegi")
                "" -> LaneCheck("Maps", saved = true, valid = false, note = "network se check nahi ho paya")
                else -> LaneCheck("Maps", saved = true, valid = false, note = "server ne mana kiya ($status)")
            }
        } catch (e: Exception) {
            LaneCheck("Maps", saved = true, valid = false, note = "network se check nahi ho paya")
        } finally {
            conn.disconnect()
        }
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
            // v2.20: Gemini models-list par 404 ka matlab naam
            // galat nahi — is key wale project me Gemini API on
            // nahi hai, ya ye Gemini key hi nahi hai. Yehi wajah
            // generate par bhi 404 deti hai (naam sahi hone par bhi).
            404 -> if (name.startsWith("Gemini")) LaneCheck(name, saved = true, valid = false, note = "is key wale project me Gemini API on nahi lag rahi — AI Studio se nayi key banao")
                else LaneCheck(name, saved = true, valid = false, note = "server ne mana kiya (code 404)")
            else -> LaneCheck(name, saved = true, valid = false, note = "server ne mana kiya (code $code)")
        }
    }

    private fun getCode(urlText: String, authHeader: String?, headerName: String = "Authorization"): Int {
        val conn = PinnedTrust.openConnection(URL(urlText)) as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            if (authHeader != null) conn.setRequestProperty(headerName, authHeader)
            conn.responseCode
        } catch (e: Exception) {
            -1
        } finally {
            conn.disconnect()
        }
    }

    private fun postJson(urlText: String, body: JSONObject, authHeader: String?): PostResult {
        // G17 (Phase 8): proxy URL pin-checked (doosre hosts par
        // ye helper plain connection deta hai).
        val conn = PinnedTrust.openConnection(URL(urlText)) as HttpURLConnection
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

    /**
     * Phase 2 voice pipeline: history ko cap karo — aakhri 12
     * turns, total ~1500 chars se zyada nahi (prompt cap ka
     * khayal; purane turns poore-poore drop hote hain, aadhe
     * nahi). Khali turns nikaal diye jaate hain. Sabse recent
     * turn hamesha rakha jata hai, chahe wo akela hi cap se
     * zyada lamba ho.
     */
    private fun capHistory(history: List<ChatTurn>): List<ChatTurn> {
        val clean = history.filter { it.text.isNotBlank() }.takeLast(12)
        val kept = mutableListOf<ChatTurn>()
        var total = 0
        for (turn in clean.asReversed()) {
            if (kept.isNotEmpty() && total + turn.text.length > 1500) break
            kept += turn
            total += turn.text.length
        }
        return kept.asReversed()
    }

    /**
     * Gemini ko roles alternate karne chahiye (user/model) —
     * lagatar ek hi role ke turns "\n" se jod diye jaate hain.
     * Aakhri user turn = final instruction + sawaal.
     */
    private fun mergeGeminiTurns(history: List<ChatTurn>, finalText: String): List<Pair<String, String>> {
        val raw = mutableListOf<Pair<String, String>>()
        for (turn in history) {
            raw += (if (turn.fromUser) "user" else "model") to turn.text
        }
        raw += "user" to finalText
        val merged = mutableListOf<Pair<String, String>>()
        for ((role, text) in raw) {
            val last = merged.lastOrNull()
            if (last != null && last.first == role) {
                merged[merged.lastIndex] = role to (last.second + "\n" + text)
            } else {
                merged += role to text
            }
        }
        return merged
    }

    private fun geminiAsk(
        prompt: String,
        key: String,
        laneCodes: MutableList<Pair<String, Int>>,
        memoryContext: String = "",
        history: List<ChatTurn> = emptyList(),
    ): String? {
        if (key.isEmpty()) return null
        // v2.20 auto-discovery: hardcoded naam phir kabhi sade
        // nahi — pehle is key par kaunse models chalte hain, wo
        // list se poochho, phir unhi me se flash-prefer karke
        // try karo. List na mile to purani fallback list.
        val discovered = discoverGeminiModels(key)
        val models = if (discovered.isNotEmpty()) discovered.take(6) else
            listOf("gemini-flash-lite-latest", "gemini-3.5-flash", "gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.0-flash")
        // Phase 2 voice pipeline: final instruction + sawaal, usse
        // pehle history ke turns (user/model roles ke saath).
        // Ban line: jawab me kabhi emoji, markdown ya technical
        // baat nahi — ye PERMANENT RULE prompt level par bhi hai
        // (TTS sanitizer VoiceEngine me alag se hai).
        val finalText = "Tum Flora ho, ek friendly AI assistant. Muskura kar, pyar se, " +
            "meethi boli me jawab do — jaise apne dost se has kar baat " +
            "karte hain. User jis tone me bole (pyar se, mazak me), waisa " +
            "hi apnao. Chhota jawab do, aam taur par 1-2 line me, lamba " +
            "ya formal mat bano. Hindi/English mix me, user jis language " +
            "me bole usi me jawab do. Kaam ki baat pehle; extra detail " +
            "sirf poochhe par. Jawab me kabhi emoji, markdown formatting " +
            "ya technical baat (code, model ka naam, key ki baat) mat likhna — " +
            "sirf saaf, saadi boli me jawab do." +
            (if (memoryContext.isNotEmpty()) " User ke baare me tumhe ye yaad hai " +
                "(sirf tab istemal karo jab sawaal se juda ho, kabhi seedha " +
                "repeat mat karo): $memoryContext." else "") +
            " Sawal: $prompt"
        val turns = mergeGeminiTurns(history, finalText)
        for (model in models) {
            val contents = JSONArray()
            for ((role, text) in turns) {
                contents.put(
                    JSONObject().put("role", role).put("parts", JSONArray().put(
                        JSONObject().put("text", text)
                    ))
                )
            }
            val body = JSONObject().put("contents", contents)
            val result = postJson(
                "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key",
                body, null
            )
            laneCodes += ("Gemini $model" to result.code)
            val text = result.body ?: continue
            // v2.22: koi bhi failure (404/429/network) agle model
            // par jao — rukna hi asli bug tha.
            if (result.code !in 200..299) continue
            val reply = try {
                JSONObject(text).optJSONArray("candidates")
                    ?.optJSONObject(0)?.optJSONObject("content")
                    ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text", "")
                    .orEmpty()
            } catch (e: Exception) {
                ""
            }
            if (reply.isNotBlank()) return reply
            return null
        }
        return null
    }

    /**
     * v2.20: is key par kaunse Gemini models generateContent
     * kar sakte hain — models-list se poochho. Key ki value
     * kabhi log nahi hoti, sirf naam wapas aate hain.
     * Order: 2.5-flash > 2.5-flash-lite > koi flash > koi bhi.
     */
    private fun discoverGeminiModels(key: String): List<String> {
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
                val first = names.firstOrNull { it == "gemini-2.5-flash" }
                val lite = names.firstOrNull { it == "gemini-2.5-flash-lite" }
                val ordered = mutableListOf<String>()
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

    private fun groqAsk(
        prompt: String,
        key: String,
        laneCodes: MutableList<Pair<String, Int>>,
        memoryContext: String = "",
        history: List<ChatTurn> = emptyList(),
    ): String? {
        if (key.isEmpty()) return null
        // v2.15: llama-3.1-8b-instant 16 Aug 2026 ko free/dev se
        // hata diya gaya — Groq ki sifarish openai/gpt-oss-20b
        // pehle, phir gpt-oss-120b (dono free tier par hain).
        val models = listOf("openai/gpt-oss-20b", "openai/gpt-oss-120b")
        for (model in models) {
            val messages = JSONArray()
                .put(JSONObject().put("role", "system").put(
                    "content",
                    "Tum Flora ho, ek friendly AI assistant. Muskura kar, pyar se, " +
                        "meethi boli me jawab do — jaise apne dost se has kar baat " +
                        "karte hain. User jis tone me bole (pyar se, mazak me), waisa " +
                        "hi apnao. Chhota jawab do, aam taur par 1-2 line me, lamba " +
                        "ya formal mat bano. Hindi/English mix me, user jis language " +
                        "me bole usi me jawab do. Kaam ki baat pehle; extra detail " +
                        "sirf poochhe par. Jawab me kabhi emoji, markdown formatting " +
                        "ya technical baat (code, model ka naam, key ki baat) mat likhna — " +
                        "sirf saaf, saadi boli me jawab do." +
                        (if (memoryContext.isNotEmpty()) " User ke baare me yaad " +
                            "(sirf zaroorat par istemal karo, kabhi seedha repeat mat karo): " +
                            "$memoryContext." else "")
                ))
            for (turn in history) {
                messages.put(
                    JSONObject().put("role", if (turn.fromUser) "user" else "assistant")
                        .put("content", turn.text)
                )
            }
            messages.put(JSONObject().put("role", "user").put("content", prompt))
            val body = JSONObject()
                .put("model", model)
                .put("messages", messages)
            val result = postJson(
                "https://api.groq.com/openai/v1/chat/completions",
                body, "Bearer $key"
            )
            laneCodes += ("Groq $model" to result.code)
            val text = result.body ?: continue
            if (result.code !in 200..299) continue
            val reply = try {
                JSONObject(text).optJSONArray("choices")
                    ?.optJSONObject(0)?.optJSONObject("message")?.optString("content", "")
                    .orEmpty()
            } catch (e: Exception) {
                ""
            }
            if (reply.isNotBlank()) return reply
            return null
        }
        return null
    }
}
