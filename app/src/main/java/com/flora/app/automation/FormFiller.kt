package com.flora.app.automation

import android.text.InputType
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Phase 6 — Form-fill anywhere (tree field detection).
 *
 * Kisi bhi app ke form me: tree se fields pehchano (label →
 * input box) → value bharo → submit. Label ka raasta: field ka
 * hintText, warna paas wale sibling ka text, warna parent ka
 * text. Jo node mile wo sirf usi turn me kaam aata hai (tree
 * nodes stale ho jaate hain — isliye find + fill ek hi call
 * me hota hai, node ko turn ke baad kabhi save nahi kiya jata).
 *
 * PASSWORD RULE (kabhi nahi tootega): password-type field me
 * Flora kabhi khud nahi likhegi — fillField PasswordRefused
 * dega aur user ko bolega ki wo khud bhare. Submit bhi tabhi
 * hoga jab screen par password field na ho, warna caller ko
 * pehle confirm dialog dikhana hoga (submitAllowed() check).
 */
data class FormField(
    val label: String,
    val isPassword: Boolean,
    internal val node: AccessibilityNodeInfo,
)

sealed class FillOutcome {
    data class Filled(val label: String) : FillOutcome()
    data class NotFound(val label: String) : FillOutcome()
    data class PasswordRefused(val label: String) : FillOutcome()
    data class SetFailed(val label: String) : FillOutcome()
}

object FormFiller {

    private fun isPasswordField(node: AccessibilityNodeInfo): Boolean {
        if (!node.isEditable) return false
        val variation = node.inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
    }

    private fun nodeText(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val t = (node.text?.toString() ?: "") + " " +
            (node.contentDescription?.toString() ?: "")
        return t.trim()
    }

    private fun hintText(node: AccessibilityNodeInfo): String {
        return try {
            node.hintText?.toString()?.trim().orEmpty()
        } catch (e: Exception) {
            ""
        }
    }

    private fun labelFor(node: AccessibilityNodeInfo, parent: AccessibilityNodeInfo?, selfIndex: Int): String {
        val hint = hintText(node)
        if (hint.isNotEmpty()) return hint.take(40)
        if (parent != null) {
            for (i in 0 until parent.childCount) {
                if (i == selfIndex) continue
                val sib = parent.getChild(i)
                val t = nodeText(sib)
                if (t.isNotEmpty()) return t.take(40)
            }
            val pt = nodeText(parent)
            if (pt.isNotEmpty()) return pt.take(40)
        }
        return "bina naam ka field"
    }

    fun findFields(service: FloraAutomationService): List<FormField> {
        val root = try {
            service.rootInActiveWindow
        } catch (e: Exception) {
            null
        } ?: return emptyList()
        val out = mutableListOf<FormField>()
        fun walk(node: AccessibilityNodeInfo?, parent: AccessibilityNodeInfo?, selfIndex: Int) {
            if (node == null) return
            if (node.isEditable) {
                out.add(FormField(labelFor(node, parent, selfIndex), isPasswordField(node), node))
            }
            for (i in 0 until node.childCount) {
                walk(node.getChild(i), node, i)
            }
        }
        walk(root, null, -1)
        return out
    }

    fun hasPasswordField(service: FloraAutomationService): Boolean {
        return findFields(service).any { it.isPassword }
    }

    fun fillField(service: FloraAutomationService, labelContains: String, value: String): FillOutcome {
        val wanted = labelContains.trim().lowercase()
        if (wanted.isEmpty()) return FillOutcome.NotFound(labelContains)
        val field = findFields(service).firstOrNull { it.label.lowercase().contains(wanted) }
            ?: return FillOutcome.NotFound(labelContains)
        if (field.isPassword) return FillOutcome.PasswordRefused(field.label)
        val args = android.os.Bundle()
        args.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value
        )
        val ok = try {
            field.node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } catch (e: Exception) {
            false
        }
        return if (ok) FillOutcome.Filled(field.label) else FillOutcome.SetFailed(field.label)
    }

    private val SUBMIT_LABELS = listOf("Submit", "Continue", "Aage", "Save", "Done", "Bhejo")

    fun clickSubmit(service: FloraAutomationService): Boolean {
        return service.clickAnyText(*SUBMIT_LABELS.toTypedArray())
    }
}
