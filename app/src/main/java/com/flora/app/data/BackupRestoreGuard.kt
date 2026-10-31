package com.flora.app.data

/**
 * P3-08 — Backup/restore test (restore-safety state machine).
 *
 * Backup HAVE hai; restore ka TEST missing tha. Ye file restore
 * ka safety rule code me bandhti hai — test design yahan hai,
 * phone execution Final Validation Flow me hogi:
 *
 * - Restore shuru karne se pehle maujooda data ka backup LO.
 * - Naya data tabhi adopt karo jab verify PASS ho.
 * - Verify FAIL ho to purana data waisa ka waisa rahe — restore
 *   fail hone par data KABHI nahi khona chahiye.
 * - Ye machine sirf faisla batati hai; asli file copy/verify
 *   FloraBackupManager ki wiring me hogi (us file ka behaviour
 *   yahan nahi badla gaya — sirf guard ka contract hai).
 */
class BackupRestoreGuard {

    enum class State {
        IDLE, BACKUP_TAKEN, RESTORING, RESTORED, FAILED_KEPT_ORIGINAL
    }

    var state: State = State.IDLE
        private set

    /** Restore tabhi shuru jab pehle backup liya gaya ho. */
    fun beginRestore(hasFreshBackup: Boolean): Boolean {
        if (!hasFreshBackup) {
            state = State.FAILED_KEPT_ORIGINAL
            return false
        }
        state = State.RESTORING
        return true
    }

    fun markBackupTaken() {
        if (state == State.IDLE) state = State.BACKUP_TAKEN
    }

    /**
     * Verify ka natija: pass → RESTORED; fail → purana data hi
     * rehta hai (FAILED_KEPT_ORIGINAL) — data loss kabhi nahi.
     * Fixer L3 (strict): verify sirf RESTORING se hi transition
     * karta hai — IDLE/BACKUP_TAKEN se verify bulane par state
     * badalti nahi (pehle IDLE se bhi FAILED mark ho jata tha).
     */
    fun verifyRestored(ok: Boolean): State {
        if (state != State.RESTORING) return state
        state = if (ok) State.RESTORED else State.FAILED_KEPT_ORIGINAL
        return state
    }

    fun originalKept(): Boolean = state == State.FAILED_KEPT_ORIGINAL
}
