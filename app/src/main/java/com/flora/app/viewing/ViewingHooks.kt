package com.flora.app.viewing

import android.graphics.Bitmap

/**
 * Lane A item 37 — perception HOOKS (sirf junction, engine nahi).
 *
 * Spec §5: v1 me polling + sampling + teen hooks ki JAGAH hai —
 * us junction par koi bhaari engine nahi chalega. Poora vision
 * engine ALAG scope hai. Isliye teeno junctions yahan no-op hain:
 * wo frame ko dekhte nahi, store nahi karte, log nahi karte —
 * sirf SKIPPED_NO_ENGINE wapas dete hain, taaki pipeline ka
 * junction compile + test ho sake bina engine ke.
 *
 * Safety (spec §6.2, §6.5): hook ko frame ka content kabhi log
 * nahi karna hai, kabhi store nahi karna hai, aur password
 * field se koi extraction kabhi nahi karni hai. [ViewingFrame]
 * ka bitmap hook ke paas sirf us call tak rehta hai — hook use
 * retain nahi karega; pipeline frame process ke baad use
 * release kar deti hai (memory me ek se zyada frame kabhi nahi).
 */

data class ViewingFrame(
    val seq: Long,
    val timestampMs: Long,
    val width: Int,
    val height: Int,
    val bitmap: Bitmap,
)

enum class ViewingHookOutcome {
    PROCESSED,
    SKIPPED_NO_ENGINE,
    FAILED,
}

interface ViewingPerceptionHook {
    val hookName: String
    fun onFrame(frame: ViewingFrame): ViewingHookOutcome
}

/** Fixed UI elements ke liye junction — engine abhi nahi juda hai. */
class TemplateMatcherHook : ViewingPerceptionHook {
    override val hookName: String = "template"
    override fun onFrame(frame: ViewingFrame): ViewingHookOutcome =
        ViewingHookOutcome.SKIPPED_NO_ENGINE
}

/** On-device text recognition ke liye junction — engine abhi nahi juda hai. */
class OcrHook : ViewingPerceptionHook {
    override val hookName: String = "ocr"
    override fun onFrame(frame: ViewingFrame): ViewingHookOutcome =
        ViewingHookOutcome.SKIPPED_NO_ENGINE
}

/** Chhote detector ke liye junction — engine abhi nahi juda hai. */
class LightweightDetectorHook : ViewingPerceptionHook {
    override val hookName: String = "detector"
    override fun onFrame(frame: ViewingFrame): ViewingHookOutcome =
        ViewingHookOutcome.SKIPPED_NO_ENGINE
}

/** Pipeline me lagne wale teeno junctions, isi kram me. */
object ViewingHookSet {
    val defaults: List<ViewingPerceptionHook> = listOf(
        TemplateMatcherHook(),
        OcrHook(),
        LightweightDetectorHook(),
    )
}
