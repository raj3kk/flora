package com.flora.app.planner

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flora.app.data.MemoryCrypto
import com.flora.app.tools.KeyInputGuard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.learnedPatternsDataStore: DataStore<Preferences> by preferencesDataStore(name = "flora_learned_patterns")

/**
 * B5-05 E5 learned engine — "trained" ka matlab LLM retraining
 * NAHI hai: on-device learned patterns + skill/decision engine.
 *
 * - Repeat kaam (jaana-pehchaana, pehle safal hua) -> zero AI
 *   call (PlannerEngine.createPlan me learned shortcut — sirf
 *   SAFE tools: NOTE aur MACRO_UI_STEP. NOTE ka koi side-effect
 *   nahi; MACRO sirf adapter-queued hota hai, Routines confirm
 *   ke bina kabhi execute nahi hota).
 * - Naya / atka kaam (koi pattern nahi, ya pattern baar-baar
 *   fail hua) -> AI chain (PlannerBrain) se.
 * - Kabhi jhootha daava nahi: pattern na mile to engine
 *   NeedsAi deta hai, "ho gaya" kabhi nahi kehta. Airplane-mode
 *   + naya kaam ho to caller imaandaar "AI chahiye" batayega.
 *
 * SILENT PROMOTION MANA HAI: Macro / Routine / Job (user ke
 * explicit automations) ko chupchaap learned pattern bana kar
 * auto-execute KABHI nahi kiya jayega — record() me
 * fromExplicitAutomation=true aate hi refuse hota hai, aur
 * learned execution bhi side-effect tools par kabhi nahi hoti.
 *
 * NO-REPEAT / CONFIRM RULES is par bhi lagu: decide() ka
 * caller (engine) spend / irreversible / login-2FA steps par
 * wahi 4 stops lagata hai — learned hona confirm ko bypass
 * nahi karta. Isliye decide() me needsConfirmReason param hai:
 * reason mile to NeedsConfirm hi milega, Offline kabhi nahi.
 *
 * Store: DataStore (encrypted, corrupt = khali, crash nahi).
 * Pattern me sirf command + tool + chhota input map hota hai —
 * input par wahi guards (key-like / forbidden assignment reject).
 */
data class LearnedPattern(
    val id: String,
    val normalizedCommand: String,
    val tool: String,
    val actionLabel: String,
    val input: Map<String, String>,
    val successCount: Int,
    val failCount: Int,
    val lastUsedMs: Long,
    val createdMs: Long,
)

object LearnedEngineLogic {

    /** Normalize: lowercase + whitespace collapse. Matching isi par hoti hai. */
    fun normalize(text: String): String =
        text.trim().lowercase().split(Regex("\\s+")).joinToString(" ")

    /** Pattern match: barabar ho, ya query me poora pattern sama jaye (lamba pattern hi). */
    fun matches(pattern: LearnedPattern, query: String): Boolean {
        val q = normalize(query)
        val p = pattern.normalizedCommand
        if (q.isEmpty() || p.isEmpty()) return false
        if (q == p) return true
        return p.length >= 8 && q.contains(p)
    }

    /** 3+ fail wala pattern unreliable hai — dobara match nahi hoga, AI chain par jayega. */
    fun isReliable(pattern: LearnedPattern): Boolean = pattern.failCount < 3

    /** Sirf ye tools learned-offline chal sakte hain (koi direct side-effect nahi). */
    fun isSafeOfflineTool(tool: String): Boolean =
        tool == PlanTool.NOTE.name || tool == PlanTool.MACRO_UI_STEP.name
}

class LearnedEngine(private val context: Context) {

    sealed class Route {
        data class Offline(val pattern: LearnedPattern) : Route()
        object NeedsAi : Route()
        data class NeedsConfirm(val reason: String) : Route()
    }

    private val key = stringPreferencesKey("learned_patterns_json")

    companion object {
        private const val MAX_PATTERNS = 200
    }

    private fun decode(raw: String): List<LearnedPattern> {
        val plain = MemoryCrypto.decodeStored(raw)
        val out = mutableListOf<LearnedPattern>()
        try {
            val arr = JSONArray(plain)
            for (i in 0 until arr.length()) {
                val el = arr.optJSONObject(i) ?: continue
                val command = el.optString("c", "")
                val tool = el.optString("t", "")
                if (command.isBlank() || tool.isBlank()) continue
                val inputObj = el.optJSONObject("i") ?: JSONObject()
                val input = mutableMapOf<String, String>()
                val keys = inputObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    input[k] = inputObj.optString(k, "")
                }
                out.add(
                    LearnedPattern(
                        id = el.optString("id", ""),
                        normalizedCommand = command,
                        tool = tool,
                        actionLabel = el.optString("a", ""),
                        input = input,
                        successCount = el.optInt("s", 0),
                        failCount = el.optInt("f", 0),
                        lastUsedMs = el.optLong("lu", 0L),
                        createdMs = el.optLong("cr", 0L),
                    )
                )
            }
        } catch (e: Exception) {
        }
        return out
    }

    private suspend fun saveAll(list: List<LearnedPattern>) {
        val arr = JSONArray()
        list.forEach { pattern ->
            val inputObj = JSONObject()
            pattern.input.forEach { (k, v) -> inputObj.put(k, v) }
            arr.put(
                JSONObject()
                    .put("id", pattern.id)
                    .put("c", pattern.normalizedCommand)
                    .put("t", pattern.tool)
                    .put("a", pattern.actionLabel)
                    .put("i", inputObj)
                    .put("s", pattern.successCount)
                    .put("f", pattern.failCount)
                    .put("lu", pattern.lastUsedMs)
                    .put("cr", pattern.createdMs)
            )
        }
        context.learnedPatternsDataStore.edit { prefs ->
            val enc = MemoryCrypto.encodeStored(arr.toString())
            if (enc != null) prefs[key] = enc
        }
    }

    suspend fun loadAll(): List<LearnedPattern> {
        return try {
            val raw = context.learnedPatternsDataStore.data.map { it[key] ?: "[]" }.first()
            decode(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Pattern record karo — false = refuse hua. Refuse hota hai
     * jab: explicit automation se aaya ho (silent promotion
     * mana), unsafe tool ho, input me key-like / forbidden
     * assignment ho, ya command khali ho.
     */
    suspend fun record(
        command: String,
        tool: String,
        actionLabel: String,
        input: Map<String, String>,
        fromExplicitAutomation: Boolean,
    ): Boolean {
        if (fromExplicitAutomation) return false
        if (!LearnedEngineLogic.isSafeOfflineTool(tool)) return false
        val normalized = LearnedEngineLogic.normalize(command)
        if (normalized.isEmpty()) return false
        for ((k, v) in input) {
            if (PlanValidator.isForbiddenKey(k)) return false
            if (KeyInputGuard.looksKeyLike(v)) return false
            if (PlanValidator.containsForbiddenAssignment(v)) return false
        }
        if (KeyInputGuard.looksKeyLike(command)) return false
        val now = System.currentTimeMillis()
        val current = loadAll().toMutableList()
        val existing = current.firstOrNull { it.normalizedCommand == normalized && it.tool == tool }
        if (existing != null) {
            current.remove(existing)
            current.add(
                existing.copy(
                    actionLabel = actionLabel,
                    input = input,
                    lastUsedMs = now,
                )
            )
        } else {
            current.add(
                LearnedPattern(
                    id = "lp-$now",
                    normalizedCommand = normalized,
                    tool = tool,
                    actionLabel = actionLabel,
                    input = input,
                    successCount = 0,
                    failCount = 0,
                    lastUsedMs = now,
                    createdMs = now,
                )
            )
        }
        val kept = if (current.size > MAX_PATTERNS) {
            current.drop(current.size - MAX_PATTERNS)
        } else {
            current
        }
        return try {
            saveAll(kept)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun recordSuccess(patternId: String) {
        val current = loadAll().toMutableList()
        val existing = current.firstOrNull { it.id == patternId } ?: return
        current.remove(existing)
        current.add(existing.copy(successCount = existing.successCount + 1, lastUsedMs = System.currentTimeMillis()))
        try {
            saveAll(current)
        } catch (e: Exception) {
        }
    }

    suspend fun recordFailure(patternId: String) {
        val current = loadAll().toMutableList()
        val existing = current.firstOrNull { it.id == patternId } ?: return
        current.remove(existing)
        current.add(existing.copy(failCount = existing.failCount + 1, lastUsedMs = System.currentTimeMillis()))
        try {
            saveAll(current)
        } catch (e: Exception) {
        }
    }

    /** Routing decision — yehi repeat-detector + offline-fallback routing hai. */
    suspend fun decide(query: String, needsConfirmReason: String?): Route {
        if (needsConfirmReason != null) return Route.NeedsConfirm(needsConfirmReason)
        val patterns = loadAll()
        for (pattern in patterns) {
            if (!LearnedEngineLogic.isReliable(pattern)) continue
            if (!LearnedEngineLogic.isSafeOfflineTool(pattern.tool)) continue
            if (LearnedEngineLogic.matches(pattern, query)) {
                return Route.Offline(pattern)
            }
        }
        return Route.NeedsAi
    }
}
