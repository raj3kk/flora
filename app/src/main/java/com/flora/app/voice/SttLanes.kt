package com.flora.app.voice

import com.flora.app.data.FloraPreferences

/**
 * STT lanes (v2.22, Phase 3) — Deepgram REST lane + system
 * fallback ka faisla ek jagah:
 * - Deepgram key ho to DeepgramClient.transcribe() (REST,
 *   nova-2, Hindi) chalta hai.
 * - Key na ho to null milta hai — caller system STT par
 *   chalta hai, kabhi crash ya khali daava nahi.
 * Streaming barge-in lane alag hai (BargeInController,
 * FloraApp me Flora ke bolne par hi chalta hai).
 * Koi key yahan kabhi likhi ya log nahi hoti.
 */
object SttLanes {

    /** Deepgram key phone par maujood hai ya nahi. */
    suspend fun hasDeepgramKey(prefs: FloraPreferences): Boolean {
        return try {
            prefs.deepgramApiKey().isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * PCM ko text me badlo. Key ho to Deepgram REST, warna
     * null (caller system STT fallback lega). Khali PCM par
     * bhi null.
     */
    suspend fun transcribe(pcm: ShortArray, prefs: FloraPreferences): String? {
        if (pcm.isEmpty()) return null
        if (!hasDeepgramKey(prefs)) return null
        return try {
            DeepgramClient.transcribe(pcm, prefs)
        } catch (e: Exception) {
            null
        }
    }
}
