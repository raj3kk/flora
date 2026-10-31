package com.flora.app.data

import android.content.Intent
import android.net.Uri

/**
 * P3-11 — Support + contact + feedback route, in-app.
 *
 * Route decision D-31 ka record hai (standing decision); ye file
 * us record ke baad ka in-app route deti hai:
 *
 * - Feedback in-app draft banta hai — user khud review karke
 *   user-tap par hi bhejega. Auto-send kabhi nahi.
 * - Draft me owner personal details KABHI NAHI aayenge — naam,
 *   phone, email, business naam, address, Udyam/Aadhaar/PAN/GSTIN
 *   jaise fields draft banane ke waqt hi mana hain (neeche list
 *   sirf FIELD NAAM hai, koi value nahi).
 * - Draft me chat full text kabhi nahi — sirf user ki likhi hui
 *   feedback line + safe error code (Catcher rules wahi).
 * - Send destination (fixer M4): code me koi naya contact method
 *   invent nahi kiya gaya — website par pehle se published
 *   support mailbox hi destination hai (privacy/terms/index
 *   pages). [sendIntent] sirf mail composer kholta hai; bhejna
 *   user ke tap par hota hai, is function se kabhi nahi.
 */
object SupportRoute {

    /** Ye fields feedback draft me kabhi nahi aayenge (naam hi kaafi hai). */
    val FORBIDDEN_FIELDS: List<String> = listOf(
        "owner-name", "owner-phone", "owner-email", "legal-business-name",
        "udyam", "aadhaar", "pan", "gstin", "bank-account",
        "home-address", "pincode-private"
    )

    /** User ki feedback line ko safe draft banao (500 chars cap). */
    fun feedbackDraft(userText: String): String {
        val clean = CatcherSanitize.sanitize(userText)
            .replace('\n', ' ').replace('\r', ' ').trim()
            .take(500)
        return clean
    }

    /** Draft bhejne layak hai ya nahi (khaali draft kabhi nahi jayega). */
    fun isSendable(draft: String): Boolean = draft.isNotBlank()

    /**
     * Send destination — website par published support mailbox.
     * Ye mailbox website ke privacy/terms/index pages par pehle
     * se public hai; is file me koi naya contact invent nahi hua.
     */
    const val SUPPORT_ADDRESS = "flipify.com@gmail.com"

    fun destinationLabel(): String {
        return "Support (website par published): $SUPPORT_ADDRESS"
    }

    /**
     * Mail composer intent — sirf kholta hai, bhejta nahi.
     * Caller user-tap par startActivity karega; auto-send kabhi nahi.
     */
    fun sendIntent(draft: String): Intent {
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$SUPPORT_ADDRESS")
            putExtra(Intent.EXTRA_SUBJECT, "Flora feedback")
            putExtra(Intent.EXTRA_TEXT, feedbackDraft(draft))
        }
    }

    fun routeDescription(): String {
        return "In-app feedback: aap likhein, review karein, apne tap par bhejein. " +
            "Isme aapka personal data ya chat text nahi jata — sirf aapki likhi line " +
            "aur safe error code (agar ho to)."
    }
}
