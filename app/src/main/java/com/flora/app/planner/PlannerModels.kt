package com.flora.app.planner

import kotlinx.serialization.Serializable

/**
 * B4 planner data model (F01) — goal, ordered steps, stops, no-do,
 * rollback, depends_on, version. YE planner model hai; core/
 * PlanFeatures.kt licence-plan hai, usse confuse nahi karna.
 *
 * Plan ka source of truth Room hai (PlanRoom.kt) — DataStore
 * iska substitute nahi (F02). Password-ish keys (password,
 * db_pass, secret, token) stored plan input me KABHI nahi aati
 * (validator reject karta hai; transient amend alag raasta hai).
 */
enum class PlanTool {
    GITHUB_CREATE_REPO,
    GITHUB_PUSH_FILE,
    VERCEL_CREATE_PROJECT,
    VERCEL_DEPLOY,
    SUPABASE_CREATE_PROJECT,
    SUPABASE_RUN_SQL,
    PROJECT_SAVE_FILE,
    PROJECT_READ_FILE,
    MACRO_UI_STEP,
    NOTE;

    companion object {
        fun fromString(raw: String): PlanTool? = try {
            valueOf(raw.trim().uppercase())
        } catch (e: Exception) {
            null
        }

        /** Side-effect wale tools — in par M8 preview + M1 marker lagta hai. */
        fun isSideEffect(tool: PlanTool): Boolean = when (tool) {
            PlanTool.GITHUB_CREATE_REPO -> true
            PlanTool.GITHUB_PUSH_FILE -> true
            PlanTool.VERCEL_CREATE_PROJECT -> true
            PlanTool.VERCEL_DEPLOY -> true
            PlanTool.SUPABASE_CREATE_PROJECT -> true
            PlanTool.SUPABASE_RUN_SQL -> true
            PlanTool.PROJECT_SAVE_FILE -> true
            PlanTool.PROJECT_READ_FILE -> false
            PlanTool.MACRO_UI_STEP -> true
            PlanTool.NOTE -> false
        }
    }
}

enum class PlanStop {
    MONEY,
    DELETE_IRREVERSIBLE,
    LOGIN_2FA,
    GENUINE_BLOCK;

    companion object {
        fun fromString(raw: String): PlanStop? = try {
            valueOf(raw.trim().uppercase())
        } catch (e: Exception) {
            null
        }
    }
}

enum class StepStatus {
    PENDING,
    RUNNING,
    VERIFIED,
    FAILED,
    SKIPPED,
    BLOCKED
}

enum class PlanStatus {
    DRAFT,
    RUNNING,
    PAUSED,
    DONE,
    FAILED,
    BLOCKED,
    STOPPED
}

/** AI se aane wala raw plan draft — strict parse + validate ke baad hi Room me jata hai. */
@Serializable
data class PlanDraft(
    val goal: String,
    val steps: List<PlanStepDraft>,
    val stops: List<String> = emptyList(),
    val noDo: List<String> = emptyList(),
    val rollback: String = "",
)

@Serializable
data class PlanStepDraft(
    val title: String,
    val tool: String,
    val input: Map<String, String> = emptyMap(),
    val successCheck: String = "",
    val fallback: String = "",
    val dependsOn: List<Int> = emptyList(),
    val sideEffectPreview: String = "",
    val externalName: String = "",
    val spend: Boolean = false,
    val irreversible: Boolean = false,
)

/** Estimate (M4/F17) — sirf jaankari, gate nahi; plan tabhi rukega jab paisa kharch ho (stop #1). */
data class PlanEstimate(
    val aiCalls: Int,
    val approxMinutes: Int,
    val freeLimitNote: String,
) {
    fun display(): String =
        "Andaza: $aiCalls AI calls, ~$approxMinutes minute. $freeLimitNote"
}

object PlanEstimateCalc {
    fun forPlan(stepCount: Int, planCalls: Int): PlanEstimate {
        val calls = planCalls + stepCount
        val minutes = (stepCount + 1) / 2 + 1
        return PlanEstimate(
            aiCalls = calls,
            approxMinutes = minutes,
            freeLimitNote = "Free limit par asar: plan calls free talk cap me gine jayenge; " +
                "limit cross par imaandaar message aayega, chupchaap aage nahi badhega.",
        )
    }
}

/** F09 validation — malformed plan reject + dobara plan banao. */
object PlanValidator {
    const val MAX_STEPS = 20

    private val FORBIDDEN_INPUT_KEYS = listOf("password", "db_pass", "secret", "token", "passwd")

    /** B5-03: project-memory save path bhi isi list se gate hota hai (ek source, duplicate nahi). */
    fun isForbiddenKey(key: String): Boolean {
        val lowerKey = key.lowercase()
        return FORBIDDEN_INPUT_KEYS.any { lowerKey.contains(it) }
    }

    private val FORBIDDEN_ASSIGNMENT = Regex(
        "(?i)\\b(password|db_pass|secret|token|passwd)\\b\\s*[:=]"
    )

    /** Free text me forbidden naam ke saath value lagi ho (jaise "token: ...") to true. */
    fun containsForbiddenAssignment(text: String): Boolean =
        FORBIDDEN_ASSIGNMENT.containsMatchIn(text)

    fun validate(draft: PlanDraft): List<String> {
        val errors = mutableListOf<String>()
        if (draft.goal.isBlank()) errors += "goal khali hai"
        if (draft.steps.isEmpty()) errors += "steps khali hain"
        if (draft.steps.size > MAX_STEPS) errors += "steps $MAX_STEPS se zyada hain (${draft.steps.size})"
        for ((index, step) in draft.steps.withIndex()) {
            if (step.title.isBlank()) errors += "step $index: title khali hai"
            val tool = PlanTool.fromString(step.tool)
            if (tool == null) {
                errors += "step $index: tool '${step.tool}' nahi jaana gaya"
                continue
            }
            if (step.successCheck.isBlank()) errors += "step $index: success-check khali hai"
            if (tool != PlanTool.NOTE && step.fallback.isBlank()) {
                errors += "step $index: fallback khali hai"
            }
            if (PlanTool.isSideEffect(tool) && step.sideEffectPreview.isBlank()) {
                errors += "step $index: side-effect preview khali hai (M8)"
            }
            for (dep in step.dependsOn) {
                if (dep < 0 || dep >= index) {
                    errors += "step $index: depends_on $dep galat hai (sirf pehle wale steps par nirbhar ho sakta hai)"
                }
            }
            if (step.dependsOn.size != step.dependsOn.toSet().size) {
                errors += "step $index: depends_on me duplicate hai"
            }
            for (key in step.input.keys) {
                val lowerKey = key.lowercase()
                if (FORBIDDEN_INPUT_KEYS.any { lowerKey.contains(it) }) {
                    errors += "step $index: input key '$key' stored plan me mana hai (password/secret kabhi save nahi)"
                }
            }
        }
        return errors
    }

    /** depends_on levels (M3/F07): ek level ke steps parallel chal sakte hain. */
    fun levels(stepCount: Int, dependsOn: List<List<Int>>): List<List<Int>> {
        val done = mutableSetOf<Int>()
        val out = mutableListOf<List<Int>>()
        var remaining = (0 until stepCount).toMutableSet()
        while (remaining.isNotEmpty()) {
            val ready = remaining.filter { i -> dependsOn[i].all { it in done } }
            if (ready.isEmpty()) {
                // Cycle (validator ne pehle hi pakadna chahiye) — bacha hua ek-ek karke.
                out += listOf(remaining.first())
                done += remaining.first()
                remaining.remove(remaining.first())
            } else {
                out += ready
                done += ready
                remaining.removeAll(ready.toSet())
            }
        }
        return out
    }
}
