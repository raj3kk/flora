package com.flora.app.voice

/**
 * STT result picker (fix-cycle B3) — pure JVM, koi Android import nahi
 * (isliye unit test me seedha chalta hai, NotchAmplitudeTest pattern jaisa).
 *
 * Recognizer ki RESULTS_RECOGNITION list + CONFIDENCE_SCORES array se
 * behtar candidate chuno:
 * - Scores maujood + size list ke barabar ho to sabse zyada confidence
 *   wala chuno (-1 aur NaN ko unavailable maano, unhe skip karo).
 * - Scores na milen, size mismatch ho, ya sab unavailable hon to
 *   PEHLA result (maujooda behavior, fail-closed — koi regression nahi).
 * - Khali list par null (caller onResult nahi bhejega — maujooda behavior).
 */
internal fun pickBestResult(results: List<String>, confidences: FloatArray?): String? {
    if (results.isEmpty()) return null
    if (confidences == null || confidences.size != results.size) return results[0]
    var bestIdx = -1
    var bestScore = -1f
    for (i in confidences.indices) {
        val s = confidences[i]
        if (s.isNaN() || s < 0f) continue
        if (s > bestScore) {
            bestScore = s
            bestIdx = i
        }
    }
    return if (bestIdx >= 0) results[bestIdx] else results[0]
}
