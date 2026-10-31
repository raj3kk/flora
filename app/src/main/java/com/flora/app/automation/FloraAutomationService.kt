package com.flora.app.automation

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay
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

        /**
         * Aakhri window-state event ka package naam (sirf naam +
         * waqt — screen ka content kabhi yahan store nahi hota).
         * Verified app-open isi se jaanta hai ki target app sach
         * me saamne aayi ya nahi.
         */
        @Volatile
        var lastForegroundPackage: String? = null
            private set

        fun isEnabled(): Boolean = instance != null

        fun noteForeground(packageName: String) {
            lastForegroundPackage = packageName
        }
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
        // for one (click/scroll/type). Events are not stored — SIRF
        // window-state change ka package naam tracker me jaata hai,
        // taaki verified app-open jaan sake ki target app saamne
        // aayi ya nahi. Node text/content yahan kabhi nahi aata.
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()?.let { noteForeground(it) }
        }
    }

    override fun onInterrupt() {
        // Nothing queued: interruption simply stops the current action.
    }

    /**
     * Saamne wali window ka package: pehle live window tree
     * (sabse taaza), na mile to window-state tracker. Sirf
     * package naam — content yahan se kabhi nahi nikalta.
     */
    fun foregroundPackage(): String? {
        try {
            rootInActiveWindow?.packageName?.toString()?.let { return it }
        } catch (e: Exception) {
        }
        return lastForegroundPackage
    }

    /** [pkg] ke foreground me aane ka [timeoutMs] tak intezar karo. */
    suspend fun waitForForeground(pkg: String, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (foregroundPackage() == pkg) return true
            delay(150)
        }
        return foregroundPackage() == pkg
    }

    /**
     * Saamne wali screen par dikh raha text (node-tree traversal):
     * vision prompt ke context ke liye. Sirf text/contentDescription
     * — koi tap/type yahan nahi hota, aur ye list kahin save nahi
     * hoti, sirf usi turn ke jawab me kaam aati hai.
     */
    fun visibleTexts(limit: Int = 25): List<String> {
        val root = try {
            rootInActiveWindow
        } catch (e: Exception) {
            null
        } ?: return emptyList()
        val out = LinkedHashSet<String>()
        fun collect(node: AccessibilityNodeInfo?) {
            if (node == null || out.size >= limit) return
            val clean = ((node.text?.toString() ?: "") +
                " " + (node.contentDescription?.toString() ?: "")).trim()
            if (clean.isNotEmpty()) out.add(clean.take(60))
            for (i in 0 until node.childCount) {
                if (out.size >= limit) return
                collect(node.getChild(i))
            }
        }
        collect(root)
        return out.toList()
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

    /**
     * Phase 0, Item 1 — content-description exact-match fallback.
     *
     * clickText contains-match hai (text YA description me substring):
     * search-submit jaise button par galat node mil sakta hai (jaise
     * search field ka container, ya suggestion row). Exact description
     * ("Search", "Go", "Khoj") pehle try hota hai — icon-only submit
     * button (koi text nahi, sirf description) isi se milta hai.
     * Na mile to false; caller phir contains-match par jayega.
     */
    fun clickByExactDescription(vararg descriptions: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val wanted = descriptions.map { it.trim().lowercase() }.toSet()
        fun dfs(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            val desc = (node.contentDescription?.toString() ?: "").trim().lowercase()
            if (node.isClickable && desc in wanted) return node
            for (i in 0 until node.childCount) {
                val found = dfs(node.getChild(i))
                if (found != null) return found
            }
            return null
        }
        val target = dfs(root) ?: return false
        return target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /**
     * Phase 0, Item 1 — search submit: pehle exact description,
     * phir maujooda contains-match (Search/Go/Khoj). Koi naya
     * behaviour nahi — sirf submit milne ke raaste badhe hain.
     */
    fun clickSearchSubmit(): Boolean {
        if (clickByExactDescription("Search", "Go", "Khoj", "Search YouTube")) return true
        return clickText("Search") || clickText("Go") || clickText("Khoj")
    }

    /**
     * Planner-first routing, Batch 2 — YouTube first-result chain.
     * Cycle 1 se ye heuristic generic first-result ke liye reuse
     * hota hai (clickFirstResult alias): koi YouTube API/code
     * nahi, sirf title-like clickable container ki khoj hai.
     *
     * Search results page par pehla video result tap karta hai.
     * Video ka title TextView khud clickable nahi hota — uska item
     * container clickable hota hai — isliye sabse GEHRA clickable
     * container chuna jata hai jiske neeche (2 level tak) title-jaisa
     * text ho. Search-box/nav labels (Search/Khoj/Home/Shorts/...)
     * aur editable fields ko chhoda jata hai. Koi candidate na mile
     * to false (caller imaandaar report dega, jhootha "play ho gaya"
     * kabhi nahi). Phone par asli YouTube UI se verify Phase 10 me
     * hoga — ye heuristic hai, guarantee nahi.
     */
    fun clickFirstVideoResult(): Boolean {
        val root = rootInActiveWindow ?: return false
        val skip = setOf(
            "search", "khoj", "home", "shorts", "subscriptions", "subscription",
            "library", "you", "create", "notifications", "voice search",
            "sign in", "filter", "cast", "all",
        )
        var visited = 0
        fun own(node: AccessibilityNodeInfo): String =
            ((node.text?.toString() ?: "") + " " +
                (node.contentDescription?.toString() ?: "")).trim()
        fun titleLike(node: AccessibilityNodeInfo): Boolean {
            val t = own(node)
            return t.length >= 3 && t.lowercase() !in skip && !node.isEditable
        }
        fun hasTitleBelow(node: AccessibilityNodeInfo, depth: Int): Boolean {
            if (depth > 2) return false
            for (i in 0 until node.childCount) {
                val c = node.getChild(i) ?: continue
                if (titleLike(c)) return true
                if (hasTitleBelow(c, depth + 1)) return true
            }
            return false
        }
        var best: AccessibilityNodeInfo? = null
        var bestDepth = -1
        fun dfs(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || visited > 800) return
            visited++
            if (node.isClickable && !node.isEditable &&
                own(node).lowercase() !in skip && hasTitleBelow(node, 0) &&
                depth >= bestDepth
            ) {
                best = node
                bestDepth = depth
            }
            for (i in 0 until node.childCount) {
                dfs(node.getChild(i), depth + 1)
            }
        }
        dfs(root, 0)
        val target = best ?: return false
        return target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /**
     * Cycle 1 — generic alias: wahi title-like first-result
     * heuristic, kisi bhi app ke results par. Purana naam
     * FloraApp ke YouTube chain ke liye barkaraar hai. 0
     * candidate par false — caller abort + imaandaar report
     * (WhatsAppReply wali exact-count guard yahan lagu nahi
     * hoti: result rows ka koi naam diya hua nahi hota, isliye
     * "pehla" ka matlab ranked single pick hai, exact match nahi).
     */
    fun clickFirstResult(): Boolean = clickFirstVideoResult()

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

    /**
     * B1 G2/G8 — password-field pehchan, primitive level par:
     * InputType password variation YA label/hint me "password".
     * Sirf mask variation par bharosa nahi (phone par prove nahi
     * hua) — label check fail-closed hai (password jaisa lage to
     * type nahi hoga). Ye guard har caller par lagu hai — koi
     * caller bina check ke password field me type nahi kar sakta.
     */
    fun isPasswordNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null || !node.isEditable) return false
        val variation = node.inputType and android.text.InputType.TYPE_MASK_VARIATION
        if (variation == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        ) return true
        val label = ((node.text?.toString() ?: "") + " " +
            (node.contentDescription?.toString() ?: "")).lowercase()
        val hint = try {
            node.hintText?.toString()?.lowercase().orEmpty()
        } catch (e: Exception) {
            ""
        }
        return label.contains("password") || hint.contains("password")
    }

    /** Pehla editable field password ho to true. */
    fun firstEditableIsPassword(): Boolean = isPasswordNode(findEditableNode())

    /** Focused field password ho to true. */
    fun focusedIsPassword(): Boolean {
        val root = rootInActiveWindow ?: return false
        return isPasswordNode(root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT))
    }

    /** Sets text into the first editable node found. Password field me kabhi nahi (B1 G2). */
    fun setTextOnFirstEditable(text: String): Boolean {
        val field = findEditableNode() ?: return false
        if (isPasswordNode(field)) return false
        val args = android.os.Bundle()
        args.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text
        )
        return field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    /** Types [text] into the currently focused editable field. Password field me kabhi nahi (B1 G2). */
    fun typeIntoFocused(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        if (!focused.isEditable) return false
        if (isPasswordNode(focused)) return false
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
