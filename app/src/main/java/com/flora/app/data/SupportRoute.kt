package com.flora.app.data

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

    fun routeDescription(): String {
        return "In-app feedback: aap likhein, review karein, apne tap par bhejein. " +
            "Isme aapka personal data ya chat text nahi jata — sirf aapki likhi line " +
            "aur safe error code (agar ho to)."
    }
}
