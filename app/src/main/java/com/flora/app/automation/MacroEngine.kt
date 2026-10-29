package com.flora.app.automation

import android.content.Context
import android.text.InputType
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

/**
 * Phase 6 — JSON macro engine (user-approved ONLY).
 *
 * Ek macro = naam + steps ki list. Steps: open (verified app-open),
 * tap (label dhoondh kar tap), type (pehle editable field me likho),
 * wait, waitText (text dikhne ka intezar), scroll, back. Har step
 * bol kar chalega; koi step fail ho to macro wahin ruk jayega —
 * fail step ka kaaran alag bolega (app nahi mili vs label nahi
 * mila vs launch fail — ek hi line kabhi nahi).
 *
 * KABHI SILENT NAHI: macro sirf tab chalta hai jab user ne
 * saaf bola ho ("macro chalao <naam>") ya Routines screen par
 * confirm dialog me Haan dabaya ho. Sensitive step (password
 * field wali screen par pay/submit/login jaisa tap) par runner
 * NeedsConfirm deta hai — caller dialog dikhata hai, Haan par
 * wahi step se resume hota hai, Ruko par macro ruk jata hai.
 * Password field me macro kabhi type nahi karta (fill wahi
 * rule FormFiller me bhi hai).
 */
data class Macro(val name: String, val steps: List<MacroStep>)

sealed class MacroStep {
    data class Open(val app: String) : MacroStep()
    data class Tap(val label: String) : MacroStep()
    data class Type(val text: String) : MacroStep()
    data class Wait(val ms: Long) : MacroStep()
    data class WaitText(val text: String, val timeoutMs: Long = 10000L) : MacroStep()
    data class Scroll(val forward: Boolean) : MacroStep()
    object Back : MacroStep()
}

sealed class MacroOutcome {
    object Done : MacroOutcome()
    data class Failed(val stepIndex: Int, val reason: String) : MacroOutcome()
    data class NeedsConfirm(val stepIndex: Int, val reason: String) : MacroOutcome()
}

object MacroJson {
    fun parse(json: String): Macro? {
        return try {
            val obj = JSONObject(json)
            val name = obj.optString("name", "").trim()
            if (name.isEmpty()) return null
            val arr = obj.optJSONArray("steps") ?: return null
            val steps = mutableListOf<MacroStep>()
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                when (s.optString("type", "")) {
                    "open" -> {
                        val app = s.optString("app", "").trim()
                        if (app.isEmpty()) return null
                        steps.add(MacroStep.Open(app))
                    }
                    "tap" -> {
                        val label = s.optString("label", "").trim()
                        if (label.isEmpty()) return null
                        steps.add(MacroStep.Tap(label))
                    }
                    "type" -> {
                        val text = s.optString("text", "")
                        if (text.isEmpty()) return null
                        steps.add(MacroStep.Type(text))
                    }
                    "wait" -> {
                        val ms = s.optLong("ms", 0L).coerceIn(0L, 30000L)
                        steps.add(MacroStep.Wait(ms))
                    }
                    "waitText" -> {
                        val text = s.optString("text", "").trim()
                        if (text.isEmpty()) return null
                        val timeout = s.optLong("timeoutMs", 10000L).coerceIn(1000L, 30000L)
                        steps.add(MacroStep.WaitText(text, timeout))
                    }
                    "scroll" -> steps.add(MacroStep.Scroll(s.optBoolean("forward", true)))
                    "back" -> steps.add(MacroStep.Back)
                    else -> return null
                }
            }
            if (steps.isEmpty()) return null
            Macro(name, steps)
        } catch (e: Exception) {
            null
        }
    }

    fun toJson(macro: Macro): String {
        val arr = JSONArray()
        for (step in macro.steps) {
            val o = JSONObject()
            when (step) {
                is MacroStep.Open -> {
                    o.put("type", "open")
                    o.put("app", step.app)
                }
                is MacroStep.Tap -> {
                    o.put("type", "tap")
                    o.put("label", step.label)
                }
                is MacroStep.Type -> {
                    o.put("type", "type")
                    o.put("text", step.text)
                }
                is MacroStep.Wait -> {
                    o.put("type", "wait")
                    o.put("ms", step.ms)
                }
                is MacroStep.WaitText -> {
                    o.put("type", "waitText")
                    o.put("text", step.text)
                    o.put("timeoutMs", step.timeoutMs)
                }
                is MacroStep.Scroll -> {
                    o.put("type", "scroll")
                    o.put("forward", step.forward)
                }
                is MacroStep.Back -> o.put("type", "back")
            }
            arr.put(o)
        }
        return JSONObject().put("name", macro.name).put("steps", arr).toString()
    }
}

object MacroEngine {

    private val SENSITIVE_TAP_WORDS = listOf(
        "pay", "bhugtan", "submit", "login", "sign in", "log in",
        "buy", "checkout", "confirm payment", "place order",
        "payment", "khareedo", "order karo"
    )

    private fun isPasswordField(node: AccessibilityNodeInfo): Boolean {
        if (!node.isEditable) return false
        val variation = node.inputType and InputType.TYPE_MASK_VARIATION
        if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        ) return true
        // B1 G8: label me "password" dikhe to bhi password maano (fail-closed).
        val label = ((node.text?.toString() ?: "") + " " +
            (node.contentDescription?.toString() ?: "")).lowercase()
        return label.contains("password")
    }

    fun hasPasswordField(service: FloraAutomationService): Boolean {
        val root = try {
            service.rootInActiveWindow
        } catch (e: Exception) {
            null
        } ?: return false
        fun walk(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            if (isPasswordField(node)) return true
            for (i in 0 until node.childCount) {
                if (walk(node.getChild(i))) return true
            }
            return false
        }
        return walk(root)
    }

    private fun isSensitiveTap(label: String): Boolean {
        val lower = label.lowercase()
        return SENSITIVE_TAP_WORDS.any { lower.contains(it) }
    }

    /**
     * B1 G3 — confirm ka mapping (pipeline stops se takrao nahi):
     * login-like (login / sign in / log in) -> stop #3 (user login);
     * baaki sensitive (submit / pay / buy / checkout / order) ->
     * stop #2 (irreversible — form bahar bhejna / payment).
     */
    private fun sensitiveCategory(label: String): String {
        val lower = label.lowercase()
        return if (lower.contains("login") || lower.contains("sign in") ||
            lower.contains("log in")
        ) {
            "login jaisa step (stop #3 — user login)"
        } else {
            "submit/payment jaisa step (stop #2 — irreversible)"
        }
    }

    suspend fun runMacro(
        context: Context,
        service: FloraAutomationService,
        macro: Macro,
        fromStep: Int,
        onStep: (String) -> Unit,
        // B1 G4: jo step index user confirm kar chuka hai, us par
        // dobara confirm dialog nahi aayega — warna Haan par wahi
        // step se resume hote hi wahi NeedsConfirm wapas aata tha
        // (dead-loop). Confirm ke baad step aage badhta hai;
        // suraksha fail-closed hi hai (bina confirm koi sensitive
        // step nahi chalta).
        confirmedSteps: Set<Int> = emptySet(),
    ): MacroOutcome {
        var index = fromStep.coerceIn(0, macro.steps.size)
        while (index < macro.steps.size) {
            val step = macro.steps[index]
            when (step) {
                is MacroStep.Open -> {
                    onStep("Step ${index + 1}: ${step.app} khol rahi hun.")
                    val matches = VerifiedAppLauncher.findByName(context, step.app)
                    if (matches.isEmpty()) {
                        return MacroOutcome.Failed(index, "app '${step.app}' nahi mili")
                    }
                    if (matches.size > 1) {
                        return MacroOutcome.Failed(
                            index,
                            "app '${step.app}' ke liye ek se zyada app mili — naam poora bolo"
                        )
                    }
                    when (val res = VerifiedAppLauncher.openVerified(context, matches[0])) {
                        VerifiedAppLauncher.OpenResult.Opened,
                        VerifiedAppLauncher.OpenResult.AlreadyOpen -> {
                            ForegroundAppContext.noteOpened(matches[0].packageName, matches[0].label)
                        }
                        VerifiedAppLauncher.OpenResult.NoVerifier -> {
                            return MacroOutcome.Failed(
                                index,
                                "app kholna verify nahi kar paya — Accessibility service ON karni hogi"
                            )
                        }
                        VerifiedAppLauncher.OpenResult.NoLaunchIntent -> {
                            return MacroOutcome.Failed(index, "app '${step.app}' ka launch nahi mila")
                        }
                        else -> {
                            return MacroOutcome.Failed(index, "app '${step.app}' khul nahi payi")
                        }
                    }
                    delay(1000)
                }
                is MacroStep.Tap -> {
                    // B1 G3: trigger "submit-ness" hai — sensitive
                    // tap par confirm, password field ki maujoodgi
                    // par nirbhar nahi. B1 G4: confirm ho chuka
                    // step dobara confirm nahi maangega.
                    if (isSensitiveTap(step.label) && index !in confirmedSteps) {
                        return MacroOutcome.NeedsConfirm(
                            index,
                            "'${step.label}' dabana sensitive hai — ${sensitiveCategory(step.label)}"
                        )
                    }
                    onStep("Step ${index + 1}: '${step.label}' daba rahi hun.")
                    if (!service.clickText(step.label)) {
                        return MacroOutcome.Failed(index, "label '${step.label}' nahi mila")
                    }
                    delay(800)
                }
                is MacroStep.Type -> {
                    // B1 G4: confirm ho chuka step dobara confirm
                    // nahi maangega (dead-loop fix). Password field
                    // me type primitive guard (service) bhi rokta hai.
                    if (hasPasswordField(service) && index !in confirmedSteps) {
                        return MacroOutcome.NeedsConfirm(
                            index,
                            "password field wali screen par likhna sensitive hai"
                        )
                    }
                    onStep("Step ${index + 1}: text likh rahi hun.")
                    val ok = service.setTextOnFirstEditable(step.text) ||
                        service.typeIntoFocused(step.text)
                    if (!ok) {
                        return MacroOutcome.Failed(index, "likhne ki jagah nahi mili")
                    }
                    delay(500)
                }
                is MacroStep.Wait -> {
                    delay(step.ms)
                }
                is MacroStep.WaitText -> {
                    val deadline = System.currentTimeMillis() + step.timeoutMs
                    var seen = false
                    while (System.currentTimeMillis() < deadline) {
                        val texts = try {
                            service.visibleTexts(50)
                        } catch (e: Exception) {
                            emptyList()
                        }
                        if (texts.any { it.lowercase().contains(step.text.lowercase()) }) {
                            seen = true
                            break
                        }
                        delay(500)
                    }
                    if (!seen) {
                        return MacroOutcome.Failed(index, "text '${step.text}' nahi dikha")
                    }
                }
                is MacroStep.Scroll -> {
                    if (!service.scroll(step.forward)) {
                        return MacroOutcome.Failed(index, "scroll nahi hua")
                    }
                    delay(500)
                }
                is MacroStep.Back -> {
                    if (!service.goBack()) {
                        return MacroOutcome.Failed(index, "wapas nahi ja payi")
                    }
                    delay(500)
                }
            }
            index++
        }
        return MacroOutcome.Done
    }
}
