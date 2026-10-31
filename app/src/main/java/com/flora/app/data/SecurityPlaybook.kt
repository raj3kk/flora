package com.flora.app.data

/**
 * P3-13 — Security headers + secret rotation (playbook + scan process).
 *
 * - Service key kabhi app me nahi — ye rule code + review dono me hai.
 * - Neeche scan patterns sirf PATTERN hain (regex text), koi real
 *   secret/key/password value yahan nahi hai aur kabhi nahi hogi.
 * - Rotation playbook: key leak/disclosure par turant rotate karo,
 *   purani key revoke karo, naya build + user notice do.
 * - Website headers checklist server/website team ke liye hai
 *   (Part 2 scope se overlap nahi — ye sirf checklist hai, yahan
 *   server code nahi badla gaya).
 */
object SecurityPlaybook {

    /** Service key kabhi app binary me embed nahi hogi. */
    const val SERVICE_KEY_NEVER_IN_APP = true

    /** Secret-scan patterns — code review/CI me in par grep chalta hai. */
    val SCAN_PATTERNS: List<String> = listOf(
        "(?i)(api[_-]?key|secret|password|token)\\s*=\\s*\"[^\"]{8,}\"",
        "FLORA-[A-Za-z0-9-]{8,}",
        "(?i)sk-[A-Za-z0-9]{8,}",
        "(?i)bearer\\s+[A-Za-z0-9._-]{16,}"
    )

    /** Website/security headers checklist (Part 2 website work ke liye). */
    val HEADER_CHECKLIST: List<String> = listOf(
        "https-only (http redirect to https)",
        "strict-transport-security",
        "content-security-policy (website pages)",
        "x-content-type-options: nosniff",
        "referrer-policy: no-referrer-when-downgrade (ya sakht)",
        "permissions-policy (camera/mic sirf zaroori jagah)"
    )

    /** Key leak/disclosure par rotation ke kadam. */
    val ROTATION_STEPS: List<String> = listOf(
        "1. Leak wali key ko turant revoke karo (provider dashboard se)",
        "2. Nayi key banao, sirf secure jagah par rakho (kabhi repo/chat me nahi)",
        "3. Purani key se chalne wale saare client/server update karo",
        "4. Naya build nikaalo + user ko batado ki purani key band ho gayi",
        "5. Catcher/Auto Log me sirf ye note: 'key rotated' (koi value kabhi nahi)"
    )

    /** Scan pattern me se koi ek text me mile to true (review gate). */
    fun looksLikeSecret(text: String): Boolean {
        return SCAN_PATTERNS.any { pattern ->
            try {
                Regex(pattern).containsMatchIn(text)
            } catch (e: Exception) {
                false
            }
        }
    }
}
