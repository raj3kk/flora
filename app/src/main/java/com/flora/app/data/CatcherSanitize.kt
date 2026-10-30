package com.flora.app.data

/**
 * Catcher sanitizer (in-app Catcher, G2) — pure, unit-testable.
 *
 * Report me ye kabhi poora nahi aayega: passwords, API keys, licence
 * key poori, OTP, UPI/UTR poora. Rules is order me lagte hain
 * (pehle URL query hatao, phir key=value, phir email, phir key
 * shapes, phir lamba token) — aur idempotent hain: masked output
 * dobara sanitize karne par nahi badalta (masks me na 12-digit
 * sequence hai, na 32+ token chars, na @).
 *
 * Note: chat ka poora text report me aata hi nahi (sirf crash text,
 * events, Auto Log action lines) — ye sanitizer uske upar aakhri
 * safety net hai, pehla darwaza nahi.
 */
object CatcherSanitize {
    private val urlQueryRegex = Regex("(https?://[^\\s?]+)\\?[^\\s]*")
    private val secretAssignRegex =
        Regex("(?i)(key|token|password|secret|otp)\\s*=\\s*[^\\s&,;]+")
    private val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val floraKeyRegex = Regex("FLORA-[A-Za-z0-9-]+")
    private val twelveDigitRegex = Regex("(?<!\\d)\\d{12}(?!\\d)")
    private val longTokenRegex = Regex("[A-Za-z0-9_-]{32,}")

    fun sanitize(input: String): String {
        var out = input
        out = urlQueryRegex.replace(out, "$1")
        out = secretAssignRegex.replace(out, "$1=[masked]")
        out = emailRegex.replace(out, "[email-masked]")
        out = floraKeyRegex.replace(out, "FLORA-****-****-****")
        out = twelveDigitRegex.replace(out) { m -> "********" + m.value.takeLast(4) }
        out = longTokenRegex.replace(out, "[token-masked]")
        return out
    }
}
