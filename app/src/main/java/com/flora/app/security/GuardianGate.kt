package com.flora.app.security

/**
 * Guardian gate for protected actions (e.g. PIN unlock):
 * allowed only when BOTH hold — PIN is enabled AND the PIN was
 * verified recently (in-memory timestamps only, never persisted).
 *
 * v2.22 (mega build chunk 3): voice step ab implemented hai
 * (VoiceMatcher + VoiceVault). Jab user ki awaaz enrolled hai
 * aur matcher available hai, tab unlock ke liye PIN + voice
 * dono chahiye. Enrolled nahi hai to PIN hi kaafi hai (pehle
 * jaisa) — aur face-match akela kabhi unlock nahi karta.
 *
 * NOTE (Guardian enforcement build, 2026-10-29): check methods
 * ab wired hain — canAccessSensitive FloraApp ke runGuardedVoice
 * (Auto Mode call/SMS/app-kholo, routine steps, confirm dialogs)
 * aur Settings ke runGuardedSettings (Keys save, PIN change,
 * Guardian/Face Lock OFF, website link/unlink, VPA/UTR save) se
 * lagta hai. Chat lock apna PIN + awaaz check alag se karta hai
 * (canUnlockFull wahi). Ek baar PIN verify hone se session
 * hamesha ke liye unlock NAHI hota.
 *
 * Cycle 2, APP-C1-012 (user decision 2026-10-29 ~10:45 IST —
 * 04:03 wala "ONE PIN per session" rule is hisse me supersede
 * hai): Guardian session TIME-BOXED hai. PIN verify ke baad
 * session sirf tab tak valid hai jab tak gated kaam chalte
 * rahein — 15 minute IDLE (koi successful gated pass nahi)
 * ke baad agla gated kaam dobara PIN dialog maangega. Timeout
 * ki value EK HI JAGAH hai (SESSION_TIMEOUT_MS neeche) —
 * kahin aur hardcode nahi. Har successful gated pass par
 * last-activity timestamp update hota hai, isliye active use
 * me PIN dobara nahi maanga jayega. Sahi PIN par pending
 * kaam EK BAAR chalta hai (one-pending rule waisa hi);
 * gate fail par kaam nahi chalta + imaandaar message +
 * Auto Log + PIN dialog (waisa hi). Forgot PIN par Guardian
 * OFF bina PIN ke nahi hota (anti-bypass waisa hi).
 */
class GuardianGate {
    companion object {
        /** Guardian session idle timeout — EK HI JAGAH. Default 15 minute. */
        const val SESSION_TIMEOUT_MS = 15L * 60L * 1000L

        /**
         * Cycle 3, F1 (e): Face Lock unlock session timeout —
         * Guardian ke 15-minute pattern jaisa, par ALAG constant
         * (Face Lock ka apna timeout — Guardian timeout badalne
         * se ye nahi badlega). Unlock ke is waqt baad chat
         * dobara lock ho jati hai (PIN dobara chahiye). Value
         * EK HI JAGAH hai — FloraApp + prefs isi ko dekhte hain.
         */
        const val FACE_LOCK_SESSION_TIMEOUT_MS = 15L * 60L * 1000L
    }

    private var pinVerifiedAtMs = 0L
    private var lastActivityMs = 0L
    private var voiceVerifiedThisSession = false

    fun markPinVerified() {
        val now = System.currentTimeMillis()
        pinVerifiedAtMs = now
        lastActivityMs = now
    }

    fun markVoiceVerified() {
        voiceVerifiedThisSession = true
    }

    fun clearSession() {
        pinVerifiedAtMs = 0L
        lastActivityMs = 0L
        voiceVerifiedThisSession = false
    }

    /**
     * PIN fresh hai ya nahi: verify hua ho AUR aakhri
     * successful gated pass se timeout na beeta ho. Successful
     * pass par last-activity update hoti hai (active use me
     * interrupt nahi). Expired ho to dobara verify chahiye.
     */
    private fun isPinFresh(): Boolean {
        if (pinVerifiedAtMs == 0L) return false
        val now = System.currentTimeMillis()
        if (now - lastActivityMs > SESSION_TIMEOUT_MS) return false
        lastActivityMs = now
        return true
    }

    fun canUnlock(pinEnabled: Boolean): Boolean = pinEnabled && isPinFresh()

    /** Poora gate: PIN (fresh) + (enrolled ho to voice bhi). */
    fun canUnlockFull(pinEnabled: Boolean, voiceEnrolled: Boolean): Boolean =
        pinEnabled && isPinFresh() && (!voiceEnrolled || voiceVerifiedThisSession)

    /**
     * Sensitive actions (keys, PIN change): when Guardian mode is OFF
     * they are allowed; when ON they need a FRESH PIN verification.
     * Voice gate sirf unlock (canUnlockFull) par hai —
     * sensitive actions PIN-gated hi rehte hain.
     */
    fun canAccessSensitive(pinEnabled: Boolean, guardianMode: Boolean): Boolean =
        !guardianMode || (pinEnabled && isPinFresh())
}
