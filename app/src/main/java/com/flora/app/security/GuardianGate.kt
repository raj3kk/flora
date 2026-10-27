package com.flora.app.security

/**
 * Guardian gate for protected actions (e.g. PIN unlock):
 * allowed only when BOTH hold — PIN is enabled AND the PIN was
 * verified in this session (in-memory flag only, never persisted).
 * Voice samples / Guardian voice-match model are a phone-side step
 * (user's own ≤10 Boss samples, threshold 0.86) and are NOT in v0.1;
 * this gate therefore defaults to DENY for unlock actions until
 * that step is done, and never silently approves.
 */
class GuardianGate {
    private var pinVerifiedThisSession = false

    fun markPinVerified() {
        pinVerifiedThisSession = true
    }

    fun clearSession() {
        pinVerifiedThisSession = false
    }

    fun canUnlock(pinEnabled: Boolean): Boolean = pinEnabled && pinVerifiedThisSession
}
