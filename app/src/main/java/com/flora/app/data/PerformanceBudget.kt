package com.flora.app.data

/**
 * P3-14 — Battery + performance budget (measurement method + targets).
 *
 * Ye numbers TARGET hain, measured claim nahi — phone par measure
 * hone ke baad hi "pass/fail" kaha jayega (Final Validation Flow).
 * Guidance HAVE hai (battery saver guidance); measurement method
 * yahan darj hai taaki har release ek hi tarike se naapi jaye:
 *
 * - Cold start: app icon tap se home screen ready tak (phone stopwatch).
 * - Frame: scroll/animation me atakna nahi chahiye (16ms frame budget).
 * - 24x7 mic: battery guidance ke saath — user ko saaf bataya jaye
 *   ki hamesha-on mic battery khayega; iska koi jhootha "zero
 *   battery" daava nahi hoga.
 */
object PerformanceBudget {

    /** Targets — phone measurement ke baad hi pass/fail. */
    const val COLD_START_MS_TARGET = 3_000L
    const val FRAME_BUDGET_MS = 16L
    const val MAX_ANR_SECONDS = 5L

    val MEASUREMENT_METHODS: List<String> = listOf(
        "cold start: icon tap se home-ready tak, phone par 3 baar, median lo",
        "scroll/animation: atakna (jank) dikhe to budget fail",
        "battery: 24x7 mic ON vs OFF, ek din ka phone battery screen comparison",
        "memory: lambi baat-cheet (30 min) ke baad app band na ho, crash na ho"
    )

    val BATTERY_GUIDANCE: String =
        "24x7 mic battery khata hai. Agar battery jaldi khatam ho to " +
            "wake-listening OFF karke sirf button se baat karein — " +
            "ye setting aapke haath me hai."

    fun coldStartPass(measuredMs: Long): Boolean = measuredMs <= COLD_START_MS_TARGET
}
