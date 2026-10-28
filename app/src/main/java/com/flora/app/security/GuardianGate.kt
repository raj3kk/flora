package com.flora.app.security

/**
 * Guardian gate for protected actions (e.g. PIN unlock):
 * allowed only when BOTH hold — PIN is enabled AND the PIN was
 * verified in this session (in-memory flag only, never persisted).
 *
 * v2.22 (mega build chunk 3): voice step ab implemented hai
 * (VoiceMatcher + VoiceVault). Jab user ki awaaz enrolled hai
 * aur matcher available hai, tab unlock ke liye PIN + voice
 * dono chahiye. Enrolled nahi hai to PIN hi kaafi hai (pehle
 * jaisa) — aur face-match akela kabhi unlock nahi karta.
 *
 * NOTE (sweep 2026-10-29): is class ke check methods
 * (canUnlock/canUnlockFull/canAccessSensitive) abhi kisi
 * sensitive action se wired nahi hain — chat lock apna PIN +
 * awaaz check alag se karta hai; sensitive-action wiring
 * pending hai (user faisla: kaunse actions gate honge).
 */
class GuardianGate {
    private var pinVerifiedThisSession = false
    private var voiceVerifiedThisSession = false

    fun markPinVerified() {
        pinVerifiedThisSession = true
    }

    fun markVoiceVerified() {
        voiceVerifiedThisSession = true
    }

    fun clearSession() {
        pinVerifiedThisSession = false
        voiceVerifiedThisSession = false
    }

    fun canUnlock(pinEnabled: Boolean): Boolean = pinEnabled && pinVerifiedThisSession

    /** Poora gate: PIN + (enrolled ho to voice bhi). */
    fun canUnlockFull(pinEnabled: Boolean, voiceEnrolled: Boolean): Boolean =
        pinEnabled && pinVerifiedThisSession && (!voiceEnrolled || voiceVerifiedThisSession)

    /**
     * Sensitive actions (keys, PIN change): when Guardian mode is OFF
     * they are allowed; when ON they need a PIN verified in this
     * session. Voice gate sirf unlock (canUnlockFull) par hai —
     * sensitive actions PIN-gated hi rehte hain.
     */
    fun canAccessSensitive(pinEnabled: Boolean, guardianMode: Boolean): Boolean =
        !guardianMode || (pinEnabled && pinVerifiedThisSession)
}
