package com.flora.app.viewing

/**
 * Lane A item 37 — Live Screen Viewing (spec v2, Accessibility-screenshot-only).
 *
 * Ye file PURE Kotlin hai: isme koi Android import nahi hai, isliye
 * iska poora logic plain JUnit me CI-safe test hota hai.
 *
 * Binding numbers (spec v2 §3.3 — v1 numbers yahan kabhi nahi aayenge):
 * - Sampling: default 2 fps se shuru, session purpose par 5 fps tak
 *   (har 200-500ms ek frame). Koi video stream nahi — lagatar
 *   periodic screenshots hain.
 * - Capture ~200-400ms, poora perception loop ~300-600ms.
 *   Koi 1ms ka vaada nahi.
 *
 * Sampler rule (spec §0 + §2.1, overlap design):
 * - Sampler pichhle frame ke perception result ka intezaar NAHI
 *   karta — naya frame aate hi wahi latest hai, purana drop.
 * - Frame ki queue kabhi nahi banti (backlog = galat faisla).
 */

/** Sampling rate: fps ko 2-5 ke andar rakhta hai (spec §2.1). */
object ViewingRate {
    const val MIN_FPS = 2
    const val MAX_FPS = 5
    const val DEFAULT_FPS = 2

    fun clamp(requestedFps: Int): Int =
        requestedFps.coerceIn(MIN_FPS, MAX_FPS)

    /** Ek frame ke beech ka interval, ms me: 2 fps -> 500ms, 5 fps -> 200ms. */
    fun intervalMs(fps: Int): Long = 1000L / clamp(fps)
}

/**
 * Guard order (spec §8) — fail-closed, isi kram me check hoga.
 * Koi step skip nahi, koi bypass nahi. Pehla fail hi verdict hai.
 */
enum class ViewingGuardFailure {
    USER_REQUEST_MISSING,
    PLANNER_NOT_CONFIRMED,
    SERVICE_OFF,
    API_TOO_LOW,
    INDICATOR_UNAVAILABLE,
    ALREADY_RUNNING,
}

object ViewingGuards {
    /** Sab guards pass hon to null, warna pehla fail. */
    fun evaluate(
        userConfirmed: Boolean,
        plannerConfirmed: Boolean,
        serviceOn: Boolean,
        apiLevel: Int,
        indicatorReady: Boolean,
    ): ViewingGuardFailure? {
        if (!userConfirmed) return ViewingGuardFailure.USER_REQUEST_MISSING
        if (!plannerConfirmed) return ViewingGuardFailure.PLANNER_NOT_CONFIRMED
        if (!serviceOn) return ViewingGuardFailure.SERVICE_OFF
        if (apiLevel < 30) return ViewingGuardFailure.API_TOO_LOW
        if (!indicatorReady) return ViewingGuardFailure.INDICATOR_UNAVAILABLE
        return null
    }
}

/**
 * Latest-frame-wins sampler (spec §2.1 point 4).
 * [accept] sirf tab true deta hai jab frame sequence ab tak ke
 * sabse naye frame se aage ho — purana/stale frame drop hota hai
 * aur [droppedStale] badhta hai. Koi queue nahi rakhi jaati.
 */
class ViewingSampler {
    var lastAcceptedSeq: Long = -1L
        private set
    var accepted: Long = 0L
        private set
    var droppedStale: Long = 0L
        private set

    fun accept(seq: Long): Boolean {
        if (seq <= lastAcceptedSeq) {
            droppedStale++
            return false
        }
        lastAcceptedSeq = seq
        accepted++
        return true
    }

    /** Kya [seq] abhi bhi latest frame hai (hook chalane se pehle check). */
    fun isLatest(seq: Long): Boolean = seq == lastAcceptedSeq
}

/**
 * Rate-cap gate (spec §2.1 point 2): polling loop har tick par
 * poochhta hai ki naya screenshot request bhejna hai ya nahi.
 * Interval se pehle dobara request nahi bheji jaati.
 */
class ViewingPollGate {
    private var lastRequestMs: Long = -1L

    fun shouldRequest(nowMs: Long, intervalMs: Long): Boolean {
        if (lastRequestMs < 0L) return true
        return nowMs - lastRequestMs >= intervalMs
    }

    fun recordRequest(nowMs: Long) {
        lastRequestMs = nowMs
    }

    fun reset() {
        lastRequestMs = -1L
    }
}
