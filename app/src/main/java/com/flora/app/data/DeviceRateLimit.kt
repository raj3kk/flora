package com.flora.app.data

/**
 * P3-03 — Rate limit + abuse rok (device side).
 *
 * Server-side licence-key rate limit (S1-30) Part 2 team ka scope
 * hai; ye usse ALIGN hai par alag item hai — device par:
 *
 * - Har protected kaam se pehle licence check zaroori hai
 *   ([requiresLicenceCheck] — ye sirf batata hai, gate ka
 *   faisla LicenceGate ka hi rehta hai; yahan gate nahi badla).
 * - Ek action ek window me [maxPerWindow] se zyada baar nahi.
 * - Lagatar auth failures par temporary block ([BLOCK_AFTER_FAILURES]
 *   failures [FAILURE_WINDOW_MS] me → [BLOCK_MS] ka block).
 *
 * Pure JVM — unit test me seedha chalta hai. Time caller deta hai
 * taaki test deterministic rahe.
 */
class DeviceRateLimit(
    private val maxPerWindow: Int = 30,
    private val windowMs: Long = 60_000L
) {
    companion object {
        const val BLOCK_AFTER_FAILURES = 5
        const val FAILURE_WINDOW_MS = 15 * 60_000L
        const val BLOCK_MS = 15 * 60_000L

        /** Protected kaam — in par licence check ke bina aage nahi. */
        val PROTECTED_ACTIONS: Set<String> = setOf(
            "brain-call", "provider-call", "whatsapp-reply", "automation-run",
            "builder-generate", "image-generate", "search-live", "home-sync",
            "backup-upload", "payment-submit"
        )

        fun requiresLicenceCheck(action: String): Boolean = action in PROTECTED_ACTIONS
    }

    private val actionTimes = mutableMapOf<String, MutableList<Long>>()
    private val failureTimes = mutableListOf<Long>()
    private var blockedUntilMs: Long = 0L

    @Synchronized
    fun tryAction(action: String, nowMs: Long): Boolean {
        if (nowMs < blockedUntilMs) return false
        val times = actionTimes.getOrPut(action) { mutableListOf() }
        times.removeAll { it <= nowMs - windowMs }
        if (times.size >= maxPerWindow) return false
        times.add(nowMs)
        return true
    }

    @Synchronized
    fun recordAuthFailure(nowMs: Long) {
        failureTimes.removeAll { it <= nowMs - FAILURE_WINDOW_MS }
        failureTimes.add(nowMs)
        if (failureTimes.size >= BLOCK_AFTER_FAILURES) {
            blockedUntilMs = nowMs + BLOCK_MS
            failureTimes.clear()
        }
    }

    @Synchronized
    fun isBlocked(nowMs: Long): Boolean = nowMs < blockedUntilMs
}
