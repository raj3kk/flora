package com.flora.app.tools

import com.flora.app.data.BuilderService

/**
 * B3-06 + B3-02 + B3-03 — key-shape guard (pure, no Android
 * dependency, isliye unit test me seedha chalta hai).
 *
 * Niyam (plan + R-B3-3):
 * - Sirf SHAPE check hota hai (length/charset/prefix-class). Koi
 *   asli ya example key value yahan kabhi nahi likhi gayi — test
 *   fixtures bhi sirf synthetic shape hain (dohraya hua char).
 * - Shape match hona validity ka saboot NAHI hai — asli check
 *   who-am-I/scope call hai (client layer). Shape sirf galat
 *   cheez ko jaldi rokta hai.
 * - Cookie/session/password shapes outright REJECT hote hain —
 *   chahe clipboard me hon, chahe chat/voice me. Message hamesha
 *   ek hi warm line: "ye key jaisi nahi lag rahi" (plan wording).
 * - Chat/voice guard: looksKeyLike() kisi bhi input me key jaisi
 *   cheez pakadta hai — FloraApp sendChat + BuilderScreen voice
 *   append dono isi ek function se gate hote hain, kahin aur
 *   bikhra hua guard nahi hai.
 */
object KeyInputGuard {

    /** Chat/voice refusal — warm, non-technical (R-8). */
    const val REFUSAL_MESSAGE =
        "Key kabhi voice ya chat se nahi li jaati — Builder ke Services screen se manual paste ya clipboard option use karo."

    /** Shape-validation failure — plan ki wording. */
    const val INVALID_SHAPE_MESSAGE =
        "Ye key jaisi nahi lag rahi — dobara copy karke phir se koshish karo."

    /** Project-file save refusal (B3-11 token-exclusion rule). */
    const val PROJECT_FILE_REFUSAL_MESSAGE =
        "Is file me key jaisi cheez hai — key kabhi project file me save nahi hoti. Key ko hata kar phir save karo, ya Services screen se jodo."

    private val githubClassic = Regex("^(ghp_|gho_|ghu_|ghs_|ghr_)[A-Za-z0-9]{20,}$")
    private val githubFineGrained = Regex("^github_pat_[A-Za-z0-9_]{20,}$")
    private val vercelToken = Regex("^[A-Za-z0-9]{24}$")
    private val supabasePat = Regex("^sbp_[A-Za-z0-9]{20,}$")
    private val supabasePublishableOrSecret =
        Regex("^sb_(publishable|secret)_[A-Za-z0-9_\\-]{20,}$")
    private val jwtShape =
        Regex("^[A-Za-z0-9_\\-]{10,}\\.[A-Za-z0-9_\\-]{10,}\\.[A-Za-z0-9_\\-]{10,}$")
    private val genericLongToken =
        Regex("^[A-Za-z0-9_\\-]{40,}$")

    /** Is service ke liye ye value key SHAPE ki hai ya nahi. */
    fun matchesServiceShape(service: BuilderService, raw: String): Boolean {
        val text = raw.trim()
        if (text.isEmpty() || looksLikeCookieOrSession(text)) return false
        return when (service) {
            BuilderService.GITHUB ->
                githubClassic.matches(text) || githubFineGrained.matches(text)
            BuilderService.VERCEL ->
                vercelToken.matches(text)
            BuilderService.SUPABASE ->
                supabasePat.matches(text) ||
                    supabasePublishableOrSecret.matches(text) ||
                    jwtShape.matches(text)
        }
    }

    /**
     * Cookie/session/password provenance — ye kabhi key nahi mani
     * jati, chahe usme token jaisa hissa bhi ho:
     * - newline/whitespace wali value (key single-line hoti hai),
     * - '=' label wali value (cookie/header/label shape; key me
     *   '=' kabhi nahi hota — JWT segments bhi unpadded hote hain),
     * - cookie attributes (expires/domain/path/httponly/max-age)
     *   ya session marker ke saath ';' wali value,
     * - "Cookie:" header ya password label wali value.
     */
    fun looksLikeCookieOrSession(raw: String): Boolean {
        val text = raw.trim()
        if (text.isEmpty()) return false
        if (text.any { it.isWhitespace() }) return true
        val lower = text.lowercase()
        if (lower.startsWith("cookie:") || lower.startsWith("password") ||
            lower.contains("password=") || lower.contains("passwd=")
        ) return true
        if (text.contains('=')) return true
        if (text.contains(';')) {
            if (lower.contains("httponly") || lower.contains("expires") ||
                lower.contains("domain=") || lower.contains("path=") ||
                lower.contains("max-age") || lower.contains("session")
            ) return true
        }
        if (lower.contains("__session") || lower.contains("session=")) return true
        return false
    }

    /**
     * Kisi bhi input me key jaisi cheez hai ya nahi (chat/voice
     * guard). Poora text ek key ho, ya koi token service shape ka
     * ho, ya 40+ char ka mixed alnum run ho — sab pakde jate
     * hain. Aam Hindi/English vaakya (chhote shabd, space wale)
     * isme nahi aate.
     */
    fun looksKeyLike(raw: String): Boolean {
        val text = raw.trim()
        if (text.isEmpty()) return false
        for (service in BuilderService.values()) {
            if (matchesServiceShape(service, text)) return true
        }
        val tokens = text.split(Regex("\\s+"))
        for (tokenRaw in tokens) {
            val token = tokenRaw.trim('.', ',', ';', ':', '!', '?', '"', '\'', '(', ')', '[', ']', '{', '}', '<', '>')
            if (token.isEmpty()) continue
            for (service in BuilderService.values()) {
                if (matchesServiceShape(service, token)) return true
            }
            if (genericLongToken.matches(token) &&
                token.any { it.isDigit() } && token.any { it.isLetter() }
            ) return true
            // Cookie/session jaisa lamba token bhi sensitive paste
            // hai (chat/voice/file me kabhi nahi jana chahiye).
            val lowerToken = token.lowercase()
            if (token.length >= 20 &&
                (lowerToken.contains("session=") ||
                    lowerToken.contains("httponly") ||
                    (token.contains(';') && lowerToken.contains("expires")))
            ) return true
        }
        return false
    }

    /** Save karne se pehle: null = theek hai, warna user ko dikhane wala message. */
    fun validateForService(service: BuilderService, raw: String): String? {
        val text = raw.trim()
        if (text.isEmpty()) return INVALID_SHAPE_MESSAGE
        if (looksLikeCookieOrSession(text)) return INVALID_SHAPE_MESSAGE
        if (!matchesServiceShape(service, text)) return INVALID_SHAPE_MESSAGE
        return null
    }
}
