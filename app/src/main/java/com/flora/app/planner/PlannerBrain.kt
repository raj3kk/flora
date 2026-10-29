package com.flora.app.planner

import com.flora.app.assistant.BrainClient
import com.flora.app.data.ChatMemoryStore
import com.flora.app.data.FloraPreferences
import com.flora.app.data.LicenceClient
import com.flora.app.data.LicenceGate
import kotlinx.serialization.json.Json

/**
 * B4 plan generation (F01/F06/F13/F17).
 *
 * Order (imaandaar, koi daava mix nahi):
 * 1. Licence-gate hook — Blocked par KOI AI call nahi, sirf
 *    imaandaar message (F05-a). Ye gate BrainClient.ask ke
 *    andar wale gate se ALAG explicit planner hook hai.
 * 2. M12 memory lookup — plan + replan waqt MANDATORY, par har
 *    step se pehle poori search nahi (AI-call bachana hai).
 * 3. Server brain-proxy client (PlannerBrainProxyClient) pehle.
 * 4. Proxy Unavailable/Error par hi BrainClient.ask direct-lane
 *    fallback — result source DIRECT_LANE likha jayega, proxy
 *    kabhi nahi kaha jayega. Proxy FreeLimit par fallback NAHI
 *    (server cap ko bypass karke direct lane par chupchaap
 *    jaana imaandaari ke khilaaf hoga) — wo genuine block hai.
 * 5. Strict kotlinx.serialization parse + PlanValidator — reject
 *    par ek replan attempt, phir genuine block (F06/F09).
 *
 * BrainClient.ask sirf free-text reply deta hai — uske upar
 * strict parsing + validation hai, koi askPlan() nahi hai.
 */
class PlannerBrain(
    private val prefs: FloraPreferences,
    private val licenceClient: LicenceClient,
    private val brain: BrainClient,
    private val chatMemoryStore: ChatMemoryStore?,
) {
    enum class BrainSource { PROXY, DIRECT_LANE }

    sealed class Generation {
        data class Ok(val draft: PlanDraft, val source: BrainSource, val calls: Int) : Generation()
        data class Blocked(val message: String) : Generation()
        data class Failed(val reason: String) : Generation()
    }

    private val proxyClient = PlannerBrainProxyClient(prefs, licenceClient)
    private val json = Json { ignoreUnknownKeys = false }

    /** M12: plan/replan waqt mandatory memory lookup (chat memory only — B4 me flora_project_memory NAHI, wo B5 namespace hai). */
    suspend fun memoryContext(): String {
        val store = chatMemoryStore ?: return ""
        return try {
            val turns = store.loadAll().takeLast(12)
            if (turns.isEmpty()) return ""
            turns.joinToString("\n") { turn ->
                (if (turn.fromUser) "User: " else "Flora: ") + turn.text
            }.take(1500)
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun generate(goal: String, deviceId: String, failureNote: String = ""): Generation {
        when (val gate = LicenceGate.check(prefs)) {
            is LicenceGate.Result.Blocked -> return Generation.Blocked(gate.message)
            is LicenceGate.Result.Allowed -> {}
        }
        val memory = memoryContext()
        val prompt = buildPrompt(goal, memory, failureNote)
        var calls = 0
        var raw: String? = null
        var source = BrainSource.PROXY
        when (val proxy = proxyClient.askPlan(prompt, deviceId)) {
            is PlannerBrainProxyClient.ProxyResult.Reply -> {
                calls += 1
                raw = proxy.text
                source = BrainSource.PROXY
            }
            is PlannerBrainProxyClient.ProxyResult.FreeLimit -> {
                return Generation.Blocked(proxy.message)
            }
            is PlannerBrainProxyClient.ProxyResult.Unavailable -> {
                val direct = runDirect(prompt)
                calls += 1
                if (direct == null) return Generation.Failed(proxy.reason)
                raw = direct
                source = BrainSource.DIRECT_LANE
            }
            is PlannerBrainProxyClient.ProxyResult.Error -> {
                val direct = runDirect(prompt)
                calls += 1
                if (direct == null) return Generation.Failed(proxy.reason)
                raw = direct
                source = BrainSource.DIRECT_LANE
            }
        }
        val first = parsePlan(raw)
        if (first != null) return Generation.Ok(first, source, calls)
        // Ek replan attempt — reject ki wajah ke saath dobara prompt.
        val retryPrompt = buildPrompt(
            goal, memory,
            "Pichhla plan reject hua: sirf valid JSON do, schema ke bahar kuch nahi. " +
                "Har step me title, tool, successCheck, fallback zaroori hai; " +
                "side-effect step me sideEffectPreview zaroori hai; " +
                "password/secret/token key kabhi mat do.",
        )
        val retryRaw = when (val proxy = proxyClient.askPlan(retryPrompt, deviceId)) {
            is PlannerBrainProxyClient.ProxyResult.Reply -> {
                calls += 1
                source = BrainSource.PROXY
                proxy.text
            }
            is PlannerBrainProxyClient.ProxyResult.FreeLimit -> {
                return Generation.Blocked(proxy.message)
            }
            else -> {
                val direct = runDirect(retryPrompt)
                calls += 1
                source = BrainSource.DIRECT_LANE
                direct
            }
        } ?: return Generation.Failed("plan ka jawab nahi mila")
        val second = parsePlan(retryRaw)
        if (second != null) return Generation.Ok(second, source, calls)
        return Generation.Failed(
            "plan ka JSON samajh nahi aaya (2 koshish) — goal dobara saaf shabdon me bolo."
        )
    }

    private suspend fun runDirect(prompt: String): String? {
        return try {
            val reply = brain.ask(prompt)
            if (reply.isBlank()) null else reply
        } catch (e: Exception) {
            null
        }
    }

    private fun buildPrompt(goal: String, memory: String, failureNote: String): String {
        val schema = "Sirf ye JSON do, uske bahar koi text nahi: " +
            "{\"goal\":\"...\",\"steps\":[{\"title\":\"...\",\"tool\":\"GITHUB_CREATE_REPO|GITHUB_PUSH_FILE|" +
            "VERCEL_CREATE_PROJECT|VERCEL_DEPLOY|SUPABASE_CREATE_PROJECT|SUPABASE_RUN_SQL|" +
            "PROJECT_SAVE_FILE|PROJECT_READ_FILE|MACRO_UI_STEP|NOTE\",\"input\":{}," +
            "\"successCheck\":\"...\",\"fallback\":\"...\",\"dependsOn\":[]," +
            "\"sideEffectPreview\":\"...\",\"externalName\":\"...\",\"spend\":false,\"irreversible\":false}]," +
            "\"stops\":[],\"noDo\":[],\"rollback\":\"...\"} " +
            "Niyam: dependsOn me sirf pehle wale step index; side-effect step me " +
            "sideEffectPreview me 'kya banega, kahan, kis naam se' likho; " +
            "input me password/db_pass/secret/token key kabhi mat do; " +
            "Supabase project ke liye db password mat maango, wo step user se alag milega."
        val memoryPart = if (memory.isBlank()) "" else " Pichhla context (sirf kaam ka ho to istemal karo): $memory."
        val failPart = if (failureNote.isBlank()) "" else " Note: $failureNote"
        return ("Goal: $goal.$memoryPart$failPart $schema").take(3800)
    }

    /** Prose ke andar lapeta JSON bhi nikaal kar strict parse karo; fail = null (reject). */
    fun parsePlan(raw: String?): PlanDraft? {
        if (raw.isNullOrBlank()) return null
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val candidate = raw.substring(start, end + 1)
        val draft = try {
            json.decodeFromString<PlanDraft>(candidate)
        } catch (e: Exception) {
            return null
        }
        if (PlanValidator.validate(draft).isNotEmpty()) return null
        return draft
    }
}
