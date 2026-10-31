package com.flora.app.automation

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.flora.app.assistant.NeuralSpeaker
import com.flora.app.data.AutoLogStore
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.delay

/**
 * Device WhatsApp reply (user order 2026-10-31 ~14:57 IST):
 * user ke device par WhatsApp me aaye message ka reply
 * WhatsApp app KHOL KAR karna hai — on-device, screen par
 * dikhte hue. WhatsApp API (whatsapp-template-send / Business
 * Cloud API) iska hissa NAHI hai — wo route user ne kabhi nahi
 * manga tha aur CLOSED hai; yahan koi API call, token ya server
 * send nahi hota.
 *
 * Raasta (ek chat, ek reply — bulk kabhi nahi):
 *   1. Auto mode ON + Accessibility service ON + WhatsApp
 *      installed — koi bhi na ho to send nahi, saaf reason.
 *   2. WhatsApp verified-open (screen par dikhe).
 *   3. Sahi conversation verify: chat list me exact title wali
 *      ek hi row ho to use tap karo; pehle se sahi chat khuli
 *      ho to tap skip. Conversation screen par header title
 *      (exact match, screen ke upari hisse me) + message input
 *      dono milne par hi aage badho. Doubt/ambiguity ho to
 *      MAT BHEJO — log karo, wapas aao.
 *   4. Type se pehle conversation dobara verify, phir reply
 *      type karo (password field me kabhi nahi) + type hua text
 *      wapas padh kar verify karo.
 *   5. Send dabao (sirf exact-description, substring fallback
 *      nahi), phir wahi conversation dobara verify + input
 *      clear hua ya nahi dekh kar sent verify karo. Verify na
 *      ho to "bheja gaya" ka daava kabhi nahi — user ko
 *      WhatsApp me dekhne ko kaho.
 *
 * Transparency (mandatory, koi silent path nahi):
 *   - Reply ke waqt WhatsApp screen khuli hoti hai (user dekh
 *     sakta hai) + bol kar bataya jata hai + har koshish Auto
 *     Log me darj hoti hai (chat title + waqt + status).
 *   - Auto Log me message ka BODY kabhi nahi likha jata.
 *   - Har step se pehle autoMode() dobara padha jata hai —
 *     "auto mode band" hote hi flow usi jagah ruk jata hai.
 *   - NotificationListener READ-ONLY hi rehta hai: ye executor
 *     usse auto-fire nahi hota. Reply tabhi chalta hai jab
 *     user-confirmed plan / auto-mode flow isko ek explicit
 *     Request de (chat title + reply text caller deta hai;
 *     text banana is executor ka kaam nahi hai).
 *
 * Note (purane pattern se farq): ReplyDraft ka purana design
 * ask-first tha aur aakhri Send user khud dabata tha. Ye item
 * user ke 14:57 IST wale seedhe order par visible auto-send
 * karta hai (indicator + Auto Log + auto-mode stop ke saath).
 * Per-message confirmation dialog is executor me nahi hai —
 * wo chahiye to alag decision se judna chahiye, chhupke nahi.
 *
 * Honest limits (phone par prove hona baaki hai, Final Validation Flow me):
 * WhatsApp ka UI badal sakta hai (send button ka description,
 * chat row layout, header bounds). Isliye har step verify +
 * fail-safe hai, par UI-fragility ki guarantee nahi di ja sakti
 * — asli WhatsApp UI par test ke bina "chalega" ka daava nahi.
 */
object WhatsAppReply {

    const val WHATSAPP_PKG = "com.whatsapp"
    const val WHATSAPP_BUSINESS_PKG = "com.whatsapp.w4b"

    /** Ek hi chat, ek hi reply. Bulk/multiple chats is type me hi nahi ban sakte. */
    data class Request(
        val chatTitle: String,
        val replyText: String,
        val packageName: String = WHATSAPP_PKG,
    )

    sealed class Result {
        data class Sent(val chatTitle: String) : Result()
        data class NotSent(val code: String, val reason: String) : Result()
    }

    /**
     * Notification se reply ka raasta (listener ko chhede bina):
     * stored notification ka pkg WhatsApp ka ho to Request banao,
     * warna null. Title notification se aata hai, text caller se.
     */
    fun requestFor(pkg: String, chatTitle: String, replyText: String): Request? {
        if (pkg != WHATSAPP_PKG && pkg != WHATSAPP_BUSINESS_PKG) return null
        return Request(chatTitle.trim(), replyText, pkg)
    }

    /** Fail-closed: pref padhne me error aaye to auto mode OFF mano. */
    private suspend fun autoModeOn(context: Context): Boolean {
        return try {
            FloraPreferences(context).autoMode()
        } catch (e: Exception) {
            false
        }
    }

    private fun announce(context: Context, text: String) {
        try {
            NeuralSpeaker.speakFixed(context, text)
        } catch (e: Exception) {
        }
    }

    private suspend fun log(context: Context, text: String) {
        try {
            AutoLogStore(context).add(text)
        } catch (e: Exception) {
        }
    }

    private fun ownText(node: AccessibilityNodeInfo): String =
        (node.text?.toString() ?: "").trim()

    /** Node tree me exact-title wale saare nodes (text match, case-insensitive). */
    private fun findExactTitleNodes(
        root: AccessibilityNodeInfo?,
        title: String,
        out: MutableList<AccessibilityNodeInfo>,
    ) {
        if (root == null) return
        if (ownText(root).equals(title, ignoreCase = true)) out.add(root)
        for (i in 0 until root.childCount) {
            findExactTitleNodes(root.getChild(i), title, out)
        }
    }

    /** Node se upar chadh kar pehla clickable ancestor, ya null. */
    private fun clickableAncestor(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var cur = node
        while (cur != null) {
            if (cur.isClickable) return cur
            cur = try {
                cur.parent
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    private fun findEditable(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isEditable) return root
        for (i in 0 until root.childCount) {
            val found = findEditable(root.getChild(i))
            if (found != null) return found
        }
        return null
    }

    /**
     * Conversation screen ki pehchan: exact title upari hisse me
     * + message input maujood. Bounds na mile to title + input
     * ki maujoodgi par fallback (hard-fail nahi, kyunki kuch
     * nodes bounds report nahi karte).
     */
    private fun isConversationOpen(
        service: FloraAutomationService,
        title: String,
        screenHeightPx: Int,
    ): Boolean {
        val root = try {
            service.rootInActiveWindow
        } catch (e: Exception) {
            null
        } ?: return false
        val titleNodes = mutableListOf<AccessibilityNodeInfo>()
        findExactTitleNodes(root, title, titleNodes)
        if (titleNodes.isEmpty()) return false
        if (findEditable(root) == null) return false
        // Screen height na mile to bounds check skip (title + input kaafi).
        if (screenHeightPx <= 0) return true
        var anyBounded = false
        for (n in titleNodes) {
            val r = Rect()
            try {
                n.getBoundsInScreen(r)
            } catch (e: Exception) {
                continue
            }
            if (r.isEmpty) continue
            anyBounded = true
            if (r.top < (screenHeightPx * 0.35).toInt()) return true
        }
        // Kisi node ke bounds hi nahi mile: title + input kaafi.
        return !anyBounded
    }

    private fun editableText(service: FloraAutomationService): String? {
        val root = try {
            service.rootInActiveWindow
        } catch (e: Exception) {
            null
        } ?: return null
        return findEditable(root)?.text?.toString()
    }

    private suspend fun fail(
        context: Context,
        title: String,
        code: String,
        reason: String,
    ): Result.NotSent {
        log(context, "WhatsApp reply NAHI bheja — $title — $reason")
        announce(context, "WhatsApp reply nahi bheja — $reason")
        return Result.NotSent(code, reason)
    }

    /**
     * Poora visible reply flow. Kabhi throw nahi karta — har
     * failure Result.NotSent me wapas aati hai (silent fail nahi).
     * Caller ko coroutine scope se bulana hai.
     */
    suspend fun sendReply(context: Context, request: Request): Result {
        val title = request.chatTitle.trim()
        val text = request.replyText
        try {
            if (title.isBlank()) {
                return fail(context, "?", "invalid_request", "chat title khaali hai")
            }
            if (text.isBlank()) {
                return fail(context, title, "invalid_request", "reply text khaali hai")
            }
            val service = FloraAutomationService.instance
                ?: return fail(
                    context, title, "service_off",
                    "Automation service OFF hai — Settings me Accessibility ON karo"
                )
            if (!autoModeOn(context)) {
                return fail(context, title, "auto_mode_off", "auto mode band hai")
            }
            if (!AppAutomation.isInstalled(context, request.packageName)) {
                return fail(context, title, "app_missing", "WhatsApp installed nahi hai")
            }

            log(context, "WhatsApp reply shuru — $title")
            announce(context, "WhatsApp khol kar $title ko reply bhej raha hoon")

            // Step 1: verified open (dikhe bina "khol diya" nahi).
            when (VerifiedAppLauncher.openVerified(context, LaunchableApp("", request.packageName))) {
                VerifiedAppLauncher.OpenResult.NoVerifier ->
                    return fail(context, title, "service_off", "Automation service OFF hai")
                VerifiedAppLauncher.OpenResult.NoLaunchIntent ->
                    return fail(context, title, "app_missing", "WhatsApp khul nahi paya")
                VerifiedAppLauncher.OpenResult.NotVisible ->
                    return fail(context, title, "open_failed", "WhatsApp saamne nahi aaya")
                VerifiedAppLauncher.OpenResult.FiredUnverified ->
                    return fail(context, title, "open_failed", "WhatsApp open verify nahi hua")
                VerifiedAppLauncher.OpenResult.Opened,
                VerifiedAppLauncher.OpenResult.AlreadyOpen -> Unit
            }
            if (!autoModeOn(context)) {
                return fail(context, title, "cancelled", "auto mode band ho gaya — ruk gaya")
            }
            if (!service.waitForForeground(request.packageName, 5000)) {
                return fail(context, title, "open_failed", "WhatsApp foreground me nahi aaya")
            }

            val screenHeight = try {
                service.resources.displayMetrics.heightPixels
            } catch (e: Exception) {
                0
            }

            // Step 2: sahi conversation tak pahuncho. Pehle se sahi
            // chat khuli ho to row tap skip (galat tap ka risk nahi).
            var settled = false
            val deadline = System.currentTimeMillis() + 5000
            while (System.currentTimeMillis() < deadline) {
                if (isConversationOpen(service, title, screenHeight)) {
                    settled = true
                    break
                }
                delay(500)
            }
            if (!settled) {
                // Chat list me exact title wali EK hi row ho to tap karo.
                val root = try {
                    service.rootInActiveWindow
                } catch (e: Exception) {
                    null
                }
                val matches = mutableListOf<AccessibilityNodeInfo>()
                findExactTitleNodes(root, title, matches)
                if (matches.size != 1) {
                    return fail(
                        context, title, "chat_not_found",
                        if (matches.isEmpty()) "chat list me ye chat nahi mili"
                        else "ek se zyada milti-julti chat mili — galat chat ka risk, nahi bhej raha"
                    )
                }
                val row = clickableAncestor(matches[0])
                    ?: return fail(context, title, "chat_not_found", "chat row dabane layak nahi mili")
                if (!row.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return fail(context, title, "chat_not_found", "chat row dabayi nahi gayi")
                }
                delay(1200)
                if (!autoModeOn(context)) {
                    return fail(context, title, "cancelled", "auto mode band ho gaya — ruk gaya")
                }
                // Step 3: UI-ready wait + conversation verify (R4-9 nuance).
                settled = false
                val deadline2 = System.currentTimeMillis() + 6000
                while (System.currentTimeMillis() < deadline2) {
                    if (isConversationOpen(service, title, screenHeight)) {
                        settled = true
                        break
                    }
                    delay(500)
                }
                if (!settled) {
                    return fail(
                        context, title, "chat_verify_failed",
                        "sahi chat verify nahi hui — galat chat me bhejne se behtar hai na bhejna"
                    )
                }
            }

            // Step 4: type + wapas padh kar verify.
            if (!autoModeOn(context)) {
                return fail(context, title, "cancelled", "auto mode band ho gaya — ruk gaya")
            }
            // R4-9 fix (b): type se pehle conversation-open dobara
            // verify — sirf settled flag par bharosa nahi (settle
            // chat-list screen par laga ho to text search field
            // me ja sakta hai).
            if (!isConversationOpen(service, title, screenHeight)) {
                return fail(
                    context, title, "chat_verify_failed",
                    "type se pehle sahi chat verify nahi hui — galat jagah type karne se behtar hai na bhejna"
                )
            }
            if (service.firstEditableIsPassword()) {
                return fail(context, title, "input_not_found", "message box ki jagah password field mili")
            }
            if (!service.setTextOnFirstEditable(text)) {
                return fail(context, title, "input_not_found", "message box me type nahi ho paya")
            }
            delay(600)
            val typed = editableText(service)
            if (typed == null || typed != text) {
                return fail(
                    context, title, "type_verify_failed",
                    "type hua text verify nahi hua — send nahi kar raha"
                )
            }

            // Step 5: send + input-clear se sent verify.
            if (!autoModeOn(context)) {
                return fail(context, title, "cancelled", "auto mode band ho gaya — ruk gaya")
            }
            // R4-9 fix (c): Send click sirf exact-description par —
            // substring clickText fallback hata diya gaya hai (wo
            // "send" wali kisi bhi clickable node ko daba sakta tha).
            val sentClicked = service.clickByExactDescription("Send")
            if (!sentClicked) {
                return fail(
                    context, title, "send_button_not_found",
                    "Send button nahi mila — message type hua pada hai, tum Send daba sakte ho"
                )
            }
            // R4-9 fix (a): input-clear check se PEHLE conversation
            // dobara verify — match na ho to send_unverified, Sent
            // kabhi nahi (input kisi aur screen par bhi clear ho
            // sakta hai).
            if (!isConversationOpen(service, title, screenHeight)) {
                return fail(
                    context, title, "send_unverified",
                    "Send ke baad sahi chat verify nahi hui — bheja gaya ya nahi pakka nahi, WhatsApp me dekh lo"
                )
            }
            var cleared = false
            val deadline3 = System.currentTimeMillis() + 5000
            while (System.currentTimeMillis() < deadline3) {
                val now = editableText(service)
                if (now != null && now.isEmpty()) {
                    cleared = true
                    break
                }
                delay(500)
            }
            if (!cleared) {
                return fail(
                    context, title, "send_unverified",
                    "Send dabaya par bheja gaya ya nahi pakka nahi — WhatsApp me dekh lo"
                )
            }

            log(context, "WhatsApp reply bheja — $title")
            announce(context, "WhatsApp par $title ko reply bhej diya")
            return Result.Sent(title)
        } catch (e: Exception) {
            return fail(context, title, "failed", "reply flow me error aaya")
        }
    }
}
