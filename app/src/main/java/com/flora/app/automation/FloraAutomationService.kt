package com.flora.app.automation

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Flora automation service (v1.0 foundation).
 *
 * The user turns this ON himself in Settings → Accessibility (never
 * silently, never via ADB). OFF karte hi sab automation ruk jata hai.
 * Screen content is processed ONLY on this phone to find a button /
 * field the user asked for — it is never sent to a server, never
 * saved. A failed lookup reports "nahi mila" instead of guessing
 * a click.
 *
 * SCREENSHOT RULE (user order 2026-10-28, one-time setup):
 * takeScreenshot() sirf tab chalta hai jab user ne BOL KAR screen
 * dekhne ko kaha ho ("screen dekho" jaisa command). Koi passive
 * capture nahi, koi background capture nahi. Ye MediaProjection
 * dialog ka doosra raasta hai: accessibility ek baar ON ho jaye to
 * uske baad har screen-dekho command bina kisi Allow tap ke chalta
 * hai — session marne ka sawaal hi nahi. Banking/password (secure)
 * windows isme bhi black aayengi, ye Android ka niyam hai.
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

    /** Clicks the first label that matches, trying each in order. */
    fun clickAnyText(vararg texts: String): Boolean {
        for (t in texts) {
            if (clickText(t)) return true
        }
        return false
    }

    private fun findEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val found = findEditable(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    /** First editable node in the tree (input box, even when not focused). */
    fun findEditableNode(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return findEditable(root)
    }

    /** Sets text into the first editable node found. */
    fun setTextOnFirstEditable(text: String): Boolean {
        val field = findEditableNode() ?: return false
        val args = android.os.Bundle()
        args.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text
        )
        return field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
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

    /**
     * Swipe gesture (WhatsApp incoming call answer swipe jaisi
     * jagahon ke liye). Dispatch sirf tab jab service ON ho.
     */
    fun swipeUp(): Boolean = swipe(0.5f, 0.8f, 0.5f, 0.3f)

    fun swipeDown(): Boolean = swipe(0.5f, 0.3f, 0.5f, 0.8f)

    private fun swipe(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val metrics = resources.displayMetrics
        val startX = (x1 * metrics.widthPixels).toInt()
        val startY = (y1 * metrics.heightPixels).toInt()
        val endX = (x2 * metrics.widthPixels).toInt()
        val endY = (y2 * metrics.heightPixels).toInt()
        val path = android.graphics.Path()
        path.moveTo(startX.toFloat(), startY.toFloat())
        path.lineTo(endX.toFloat(), endY.toFloat())
        val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 300)
        val gesture = android.accessibilityservice.GestureDescription.Builder()
            .addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)

    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)

    /**
     * One-time-setup screen capture (API 30+): jab user BOL KAR
     * screen dekhne ko kahe, tab current screen ka JPEG wapas deta
     * hai — bina MediaProjection dialog, bina Allow tap ke. Service
     * OFF ho, API purani ho, screen off ho ya secure window ho to
     * null (caller tab MediaProjection wale raaste par jayega).
     * Hardware bitmap ko pehle software bitmap me badla jata hai,
     * tabhi JPEG compress ho sakta hai.
     */
    suspend fun screenshotJpeg(): ByteArray? {
        if (android.os.Build.VERSION.SDK_INT <
            android.os.Build.VERSION_CODES.R
        ) return null
        return try {
            kotlinx.coroutines.withTimeoutOrNull(8000) {
                kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                    val executor = java.util.concurrent.Executor { cmd ->
                        android.os.Handler(
                            android.os.Looper.getMainLooper()
                        ).post(cmd)
                    }
                    try {
                        takeScreenshot(
                            android.view.Display.DEFAULT_DISPLAY,
                            executor,
                            object : TakeScreenshotCallback {
                                override fun onSuccess(result: ScreenshotResult) {
                                    if (!cont.isActive) return
                                    val jpeg = try {
                                        // Asli API: ScreenshotResult me
                                        // bitmap property NAHI hai —
                                        // hardwareBuffer + colorSpace
                                        // se wrap karna padta hai (CI
                                        // fail se seekha, 2026-10-28).
                                        val hw = result.hardwareBuffer ?: run {
                                            cont.resumeWith(Result.success(null))
                                            return
                                        }
                                        val raw = android.graphics.Bitmap.wrapHardwareBuffer(
                                            hw, result.colorSpace
                                        ) ?: run {
                                            cont.resumeWith(Result.success(null))
                                            return
                                        }
                                        val soft = if (raw.config ==
                                            android.graphics.Bitmap.Config.HARDWARE
                                        ) {
                                            raw.copy(
                                                android.graphics.Bitmap.Config.ARGB_8888,
                                                false
                                            )
                                        } else {
                                            raw
                                        } ?: run {
                                            cont.resumeWith(Result.success(null))
                                            return
                                        }
                                        val out =
                                            java.io.ByteArrayOutputStream()
                                        soft.compress(
                                            android.graphics.Bitmap.CompressFormat.JPEG,
                                            80,
                                            out
                                        )
                                        out.toByteArray()
                                    } catch (e: Exception) {
                                        null
                                    }
                                    cont.resumeWith(Result.success(jpeg))
                                }

                                override fun onFailure(errorCode: Int) {
                                    if (cont.isActive) cont.resumeWith(Result.success(null))
                                }
                            }
                        )
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resumeWith(Result.success(null))
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
