package com.flora.app.data

/**
 * P3-07 — Update + rollback (design + app-side record).
 *
 * Update client + version.json channel HAVE hai (AppUpdate), par
 * rollback path missing tha. Ye file rollback ka design deti hai:
 *
 * - Har successful update se pehle maujooda stable version ka
 *   record rakha jayega ([PreviousStable]) — taaki kharab release
 *   par wapas jaane ka raasta ho.
 * - Rollback bhi har update ki tarah SIRF user-tap par hoga —
 *   auto-download/auto-install kabhi nahi.
 * - GATED note: LIVE rollback tabhi chalega jab P2-06 update
 *   channel poora ho (version.json me apkUrl + sha256 bhare hon,
 *   real release SHA se). Channel na ho to item GATED rehta hai —
 *   design yahan darj hai, force nahi kiya gaya. P2-06 Part 2
 *   team ka scope hai; uska code yahan nahi chheda gaya.
 * - SHA kabhi poora UI par nahi dikhega — sirf aakhri 8 chars
 *   pehchaan ke liye.
 */
object UpdateRollback {

    data class PreviousStable(
        val versionCode: Long,
        val versionName: String,
        val sha256: String
    )

    /** P2-06 channel ready hai ya nahi — dono bhare hon tabhi true. */
    fun channelReady(apkUrl: String, sha256: String): Boolean {
        return apkUrl.isNotBlank() && sha256.isNotBlank()
    }

    /** Rollback tabhi jab pichhla stable maujood ho aur current se alag ho. */
    fun isRollbackAvailable(previous: PreviousStable?, currentVersionCode: Long): Boolean {
        return previous != null && previous.versionCode != currentVersionCode
    }

    fun shaShort(sha256: String): String {
        return if (sha256.length >= 8) sha256.takeLast(8) else "********"
    }

    /** Rollback ke kadam — sab user-tap par, koi auto step nahi. */
    fun rollbackSteps(previous: PreviousStable): List<String> {
        return listOf(
            "1. Pichhla stable dikhao: ${previous.versionName} (sha …${shaShort(previous.sha256)})",
            "2. User-tap par hi pichhla version download karo (SHA verify ke saath)",
            "3. SHA mismatch par install roko + file delete karo",
            "4. System install prompt se hi install ho (app khud install nahi karegi)",
            "5. Rollback ke baad Catcher me note darj karo (kaunsa version, kyun)"
        )
    }
}
