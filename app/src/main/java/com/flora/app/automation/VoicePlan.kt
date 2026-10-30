package com.flora.app.automation

import android.content.Context
import kotlinx.coroutines.delay

/**
 * Planner-first routing, Batch 1 (2026-10-30) — deterministic, local, no AI call.
 *
 * Spec: docs/FLORA_PLANNER_ROUTING_FIX_SPEC.md. Rule ka naam:
 * "Samajh-Pehle, Confirm-Ek-Baar-Shuru-Me" — plan dikhake, confirm
 * sirf ek baar shuru me (Auto Mode ON par bina confirm ke seedha),
 * execution ke beech me kabhi sawaal nahi. 4 safety stops isse upar
 * hain: stop karna poochhna nahi hai.
 *
 * Ye PlannerEngine/PlanTool/Room plans se ALAG lightweight layer hai
 * (builder plans untouched). Executor sirf maujooda raaste hain:
 * VerifiedAppLauncher, InAppSearch, FloraAutomationService.
 * TAP_FIRST_RESULT Batch 2 me aayega (SD-5 general surface nahi).
 */
enum class VoicePlanExecutor {
    APP_OPEN,
    SEARCH_URL,
    IN_APP_SEARCH,
}

data class VoicePlanStep(
    val title: String,
    val executor: VoicePlanExecutor,
    val input: Map<String, String> = emptyMap(),
    val successCheck: String = "",
    val fallback: String = "",
)

data class VoicePlan(
    val goal: String,
    val steps: List<VoicePlanStep>,
    val stops: List<String> = emptyList(),
    val noDo: List<String> = listOf(
        "silent send",
        "password type",
        "payment confirm",
        "doosre app ka private data",
    ),
)

/**
 * Runner — FloraApp composable se Host lambdas ke zariye chalta hai
 * (state/dialog wahi rehte hain). Har step par verify, fail par ek
 * deterministic fallback, phir imaandaar report + Auto Log.
 */
object VoicePlanRunner {

    data class Host(
        val context: Context,
        val addMessage: (String) -> Unit,
        val speak: (String) -> Unit,
        val log: suspend (String) -> Unit,
    )

    suspend fun execute(plan: VoicePlan, host: Host): String {
        if (plan.stops.isNotEmpty()) {
            val msg = "Ye plan safety stop par ruka hai (${plan.stops.joinToString(", ")}), isliye chalaya nahi gaya."
            host.addMessage(msg)
            host.speak(msg)
            try { host.log("VoicePlan STOP: ${plan.goal} -> ${plan.stops.joinToString(",")}") } catch (e: Exception) { }
            return msg
        }
        host.addMessage("Plan chala raha hun: " + plan.steps.joinToString(" → ") { it.title })
        var previousWasOpen = false
        for (step in plan.steps) {
            if (previousWasOpen) delay(1000)
            previousWasOpen = false
            val ok = runStep(step, host)
            if (!ok) {
                val fb = step.fallback
                if (fb.startsWith("SEARCH_URL:")) {
                    val url = fb.removePrefix("SEARCH_URL:")
                    val r = VerifiedAppLauncher.openWebsiteVerified(host.context, url)
                    if (r == VerifiedAppLauncher.OpenResult.Opened) {
                        try { host.log("VoicePlan fallback: search URL khola (${plan.goal})") } catch (e: Exception) { }
                        continue
                    }
                }
                val msg = "Plan ruk gaya: '${step.title}' nahi ho paya. ${step.successCheck}"
                host.addMessage(msg)
                host.speak(msg)
                try { host.log("VoicePlan FAIL: ${plan.goal} -> ${step.title}") } catch (e: Exception) { }
                return msg
            }
            if (step.executor == VoicePlanExecutor.APP_OPEN) previousWasOpen = true
        }
        val done = "Ho gaya: " + plan.steps.joinToString(", ") { it.title } + "."
        host.addMessage(done)
        host.speak(done)
        try { host.log("VoicePlan DONE: ${plan.goal}") } catch (e: Exception) { }
        return done
    }

    private suspend fun runStep(step: VoicePlanStep, host: Host): Boolean {
        return when (step.executor) {
            VoicePlanExecutor.APP_OPEN -> {
                val label = step.input["label"] ?: return false
                val pkg = step.input["package"] ?: return false
                val result = VerifiedAppLauncher.openVerified(host.context, LaunchableApp(label, pkg))
                val verified = result == VerifiedAppLauncher.OpenResult.Opened ||
                    result == VerifiedAppLauncher.OpenResult.AlreadyOpen
                if (verified) {
                    ForegroundAppContext.noteOpened(pkg, label)
                    true
                } else if (result == VerifiedAppLauncher.OpenResult.NoVerifier) {
                    host.addMessage("$label kholna verify nahi kar paya — Accessibility service ON karna hoga, Settings me.")
                    host.speak("$label kholna verify nahi kar paya — Accessibility service ON karna hoga, Settings me.")
                    false
                } else {
                    false
                }
            }
            VoicePlanExecutor.SEARCH_URL -> {
                val url = step.input["url"] ?: return false
                VerifiedAppLauncher.openWebsiteVerified(host.context, url) ==
                    VerifiedAppLauncher.OpenResult.Opened
            }
            VoicePlanExecutor.IN_APP_SEARCH -> {
                val query = step.input["query"] ?: return false
                val service = FloraAutomationService.instance ?: return false
                InAppSearch.searchInApp(service, query) == InAppSearchOutcome.Done
            }
        }
    }
}
