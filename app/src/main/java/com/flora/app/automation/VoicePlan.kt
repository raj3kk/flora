package com.flora.app.automation

import android.content.Context
import com.flora.app.data.CatcherEvents
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
 * TAP_FIRST_RESULT pehle sirf YouTube first-result chain ke
 * liye tha (Batch 2); Cycle 1 se generic hai — pehla result
 * (title-like clickable container heuristic, koi app-specific
 * API nahi) kisi bhi app me, 0 candidate par abort + imaandaar
 * failMessage. SCROLL generic scroll hai (aage = forward,
 * peeche = backward), MacroEngine ke scroll surface jaisa.
 */
enum class VoicePlanExecutor {
    APP_OPEN,
    SEARCH_URL,
    IN_APP_SEARCH,
    TAP_FIRST_RESULT,
    SCROLL,
}

data class VoicePlanStep(
    val title: String,
    val executor: VoicePlanExecutor,
    val input: Map<String, String> = emptyMap(),
    val successCheck: String = "",
    val fallback: String = "",
    // Fail par generic message ki jagah ye dikhega (jaise TAP fail par
    // imaandaar "search khol diya, khud tap karo" line).
    val failMessage: String = "",
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
 *
 * Pacing rule (parity cycle 2026-10-31, user report "step by step
 * wait nahi karti"): agla step tabhi jab pichhla step VERIFY ho —
 * blind delay nahi, verification hai. APP_OPEN par foreground
 * poll (3s tak) — khula par foreground me na aaye to agla step
 * nahi chalega. IN_APP_SEARCH par results-settle poll (3s tak,
 * soft — timeout par TAP step khud verify karega, isliye yahan
 * fail nahi). TAP par foreground + click + dobara foreground
 * check. Har verified step Auto Log me darj hota hai.
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
        // D-06 R4-1: noDo enforcement (pehle display-only tha, ab hard
        // gate hai). Kisi step ka title/input kisi noDo phrase se match
        // kare to poora plan nahi chalega — wajah me entry + step.
        for (step in plan.steps) {
            val hay = (step.title + " " + step.input.values.joinToString(" ")).lowercase()
            val hit = plan.noDo.firstOrNull { entry ->
                entry.isNotBlank() && hay.contains(entry.trim().lowercase())
            }
            if (hit != null) {
                val msg = "Ye plan no-do gate par ruka hai: '${step.title}' step no-do '$hit' se takrata hai, isliye chalaya nahi gaya."
                host.addMessage(msg)
                host.speak(msg)
                try { host.log("VoicePlan NODO: ${plan.goal} -> ${step.title} ($hit)") } catch (e: Exception) { }
                return msg
            }
        }
        host.addMessage("Plan chala raha hun: " + plan.steps.joinToString(" → ") { it.title })
        var usedFallback = false
        for (step in plan.steps) {
            val ok = runStep(step, host)
            if (!ok) {
                val fb = step.fallback
                if (fb.startsWith("SEARCH_URL:")) {
                    val url = fb.removePrefix("SEARCH_URL:")
                    val r = VerifiedAppLauncher.openWebsiteVerified(host.context, url)
                    if (r == VerifiedAppLauncher.OpenResult.Opened) {
                        usedFallback = true
                        try { host.log("VoicePlan fallback: search URL khola (${plan.goal})") } catch (e: Exception) { }
                        continue
                    }
                }
                val msg = step.failMessage.ifEmpty {
                    "Plan ruk gaya: '${step.title}' nahi ho paya. ${step.successCheck}"
                }
                host.addMessage(msg)
                host.speak(msg)
                try { host.log("VoicePlan FAIL: ${plan.goal} -> ${step.title}") } catch (e: Exception) { }
                // Catcher (G2): TAP/app-open/search fail sab isi raaste se aate hain.
                CatcherEvents.record("PLANNER", "${plan.goal} -> ${step.title}")
                return msg
            }
            try { host.log("VoicePlan step VERIFIED: ${step.title}") } catch (e: Exception) { }
        }
        val done = "Ho gaya: " + plan.steps.joinToString(", ") { it.title } + "." +
            if (usedFallback) " (search web se khola gaya)" else ""
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
                var result = VerifiedAppLauncher.openVerified(host.context, LaunchableApp(label, pkg))
                // Cold-start retry (audit W4): badi app pehli baar der se
                // khule to NotVisible milega — ek baar dobara koshish.
                if (result == VerifiedAppLauncher.OpenResult.NotVisible) {
                    result = VerifiedAppLauncher.openVerified(host.context, LaunchableApp(label, pkg))
                }
                val verified = result == VerifiedAppLauncher.OpenResult.Opened ||
                    result == VerifiedAppLauncher.OpenResult.AlreadyOpen
                if (verified) {
                    // Pacing: khulna kaafi nahi — foreground me aane
                    // tak poll karo (cold start par app der se aata
                    // hai; bina iske agla step adhoori screen par
                    // chalta tha — "app khol kar kaam nahi kar pati"
                    // wali shikayat ki jad). Foreground na aaye to
                    // agla step nahi, imaandaar fail.
                    val service = FloraAutomationService.instance
                    if (service != null && !service.waitForForeground(pkg, 3000)) {
                        host.addMessage("$label khula par foreground me nahi aaya — agla step nahi chala raha.")
                        host.speak("$label khula par foreground me nahi aaya.")
                        return false
                    }
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
                if (InAppSearch.searchInApp(service, query) != InAppSearchOutcome.Done) return false
                // Pacing: Done = submit button click hua, results
                // dikhna abhi baaki ho sakta hai. UI ke sthir hone
                // tak poll karo (snapshot do baar ek jaisa = settle,
                // max ~3s). Timeout par bhi aage badho — TAP step
                // khud verify karega; slow network par yahan fail
                // karna galat hota.
                var last: List<String>? = null
                var settled = false
                val deadline = System.currentTimeMillis() + 3000
                while (System.currentTimeMillis() < deadline && !settled) {
                    delay(300)
                    val now = try { service.visibleTexts(25) } catch (e: Exception) { emptyList() }
                    if (now.isNotEmpty() && now == last) settled = true
                    last = now
                }
                try { host.log("VoicePlan search settle: settled=$settled") } catch (e: Exception) { }
                true
            }
            VoicePlanExecutor.TAP_FIRST_RESULT -> {
                val pkg = step.input["package"] ?: return false
                val service = FloraAutomationService.instance ?: return false
                if (!service.waitForForeground(pkg, 3000)) return false
                // Cycle 1: generic first-result (YouTube wala
                // clickFirstVideoResult hi iska alias hai — wahi
                // heuristic, koi naya tap code nahi). Fixer
                // (tester D2): TAP step ki query aage do, taaki
                // title me query-token wale result ko preference
                // mile; query na ho to first-pick behaviour.
                if (!service.clickFirstResult(step.input["query"])) return false
                delay(1500)
                service.foregroundPackage() == pkg
            }
            VoicePlanExecutor.SCROLL -> {
                val service = FloraAutomationService.instance ?: return false
                service.scroll(step.input["forward"] != "false")
            }
        }
    }
}
