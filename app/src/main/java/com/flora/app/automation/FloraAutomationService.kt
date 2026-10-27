package com.flora.app.automation

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Flora automation service (v1.0 foundation).
 *
 * The user turns this ON himself in Settings → Accessibility (never
 * silently, never via ADB). OFF karte hi sab automation ruk jata hai.
 * Screen content is processed ONLY on this phone to find a button /
 * field the user asked for — it is never sent to a server, never
 * screenshotted, never saved. A failed lookup reports "nahi mila"
 * instead of guessing a click.
 */
class FloraAutomationService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: FloraAutomationService? = null
            private set

        fun isEnabled(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No passive listening: actions run only when the user asked
        // for one (click/scroll/type). Events are not stored.
    }

    override fun onInterrupt() {
        // Nothing queued: interruption simply stops the current action.
    }

    private fun findNode(
        root: AccessibilityNodeInfo?,
        text: String,
    ): AccessibilityNodeInfo? {
        if (root == null) return null
        val wanted = text.lowercase()
        val own = (root.text?.toString() ?: "") + " " +
            (root.contentDescription?.toString() ?: "")
        if (own.lowercase().contains(wanted) && root.isClickable) return root
        for (i in 0 until root.childCount) {
            val found = findNode(root.getChild(i), text)
            if (found != null) return found
        }
        return null
    }

    /** Clicks the first clickable node whose text/description contains [text]. */
    fun clickText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNode(root, text) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /** Types [text] into the currently focused editable field. */
    fun typeIntoFocused(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        if (!focused.isEditable) return false
        val args = android.os.Bundle()
        args.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text
        )
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    /** Scrolls the first scrollable node on screen. */
    fun scroll(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        return scrollNode(root, forward)
    }

    private fun scrollNode(node: AccessibilityNodeInfo?, forward: Boolean): Boolean {
        if (node == null) return false
        if (node.isScrollable) {
            val action = if (forward) {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            }
            if (node.performAction(action)) return true
        }
        for (i in 0 until node.childCount) {
            if (scrollNode(node.getChild(i), forward)) return true
        }
        return false
    }

    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)

    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
}
