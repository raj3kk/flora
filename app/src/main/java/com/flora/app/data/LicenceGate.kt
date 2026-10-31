package com.flora.app.data

import com.flora.app.core.FloraConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * App-side licence gate (Wave B, D8 "dono gate" decision).
 *
 * Har provider use (BrainClient.ask ke direct calls, Deepgram
 * streaming start) se PEHLE ye check chalta hai:
 *  - Har gated use par server se taaza licence status ka
 *    FRESH verify attempt hota hai (013: 15-minute cache
 *    suppression hataya gaya — cache par bharosa karke
 *    gated faisla nahi hota, isliye revoke/expire ke baad
 *    Pro 15 minute tak nahi chalta).
 *  - Pro valid + active ho to Allowed.
 *  - Revoked / expired / superseded / bound_mismatch /
 *    device_locked ho to Blocked + imaandaar message (koi
 *    provider call nahi, koi kharcha nahi).
 *  - Free / server na pahunche to maujooda local counter
 *    (talkSecondsUsed, 600 sec/day) — uske andar ho to
 *    Allowed, nahi to Blocked (free-limit message).
 *
 * FAIL-OPEN note (imaandaar limit, K7 — 013 me BADLA NAHI):
 * server na pahunche aur aakhri jaana hua status Pro valid
 * raha ho to Allowed (kuch der ki outage me paying user
 * downgrade nahi hoga; server wapas aate hi asli status
 * lagu hoga). Iska matlab: D8 ka "har use par check" har
 * use par fresh attempt hai, outage ke dauraan guarantee
 * nahi — outage me last-known fail-open hi lagu hota hai,
 * aur revoke ko outage se alag nahi pehchana ja sakta.
 * Isi tarah client-side gate ko hacked app bypass kar sakta
 * hai — ye hamesha sach hai. Isliye doosra hissa server
 * gate hai (proxy lane, brain-proxy v5 / tts-proxy v3, har
 * proxy call par fresh check) — par proxy gate sirf proxy
 * lane ko cover karta hai, direct lane (user ki apni keys)
 * ka har-use check isi app gate se hota hai, isliye yahan
 * cache suppression hatana zaroori tha.
 * User ki keys ka model NAHI badla: direct lane user ki apni
 * keys se hi chalti hai, koi server-keys routing force nahi.
 */
object LicenceGate {

    sealed class Result {
        object Allowed : Result()
        data class Blocked(val message: String) : Result()
    }

    private const val FREE_DAILY_SECONDS =
        FloraConfig.FREE_TALK_SECONDS_PER_DAY.toLong()
    // B5 (fix-cycle): internal — BackgroundMicService isi const ka istemal
    // karta hai (do copies ka drift khatam; wording bilkul wahi rahegi).
    internal const val FREE_LIMIT_MESSAGE =
        "Aaj ka free talk time (10 minute) khatam ho gaya hai. " +
            "Kal phir milega, ya Pro (₹199/month) lijiye."

    // Last-known status — SIRF network-fail fallback ke liye
    // (fail-open, K7). Gated faisla kabhi is par bharosa karke
    // fresh verify ko skip nahi karta (013).
    @Volatile
    private var cached: LicenceStatus? = null

    suspend fun check(prefs: FloraPreferences): Result {
        return try {
            checkInner(prefs)
        } catch (e: Exception) {
            CatcherHooks.licenceRecord("exception")
            // Gate kabhi chat ko crash nahi karega — local cap
            // rule par gir jao (free-cap ke andar ho to Allowed).
            localCapResult(prefs)
        }
    }

    private suspend fun checkInner(prefs: FloraPreferences): Result {
        val st = freshStatus(prefs) ?: cached
        if (st != null && st.serverChecked) {
            when (st.status) {
                "revoked", "expired", "superseded", "bound_mismatch", "device_locked" ->
                    return Result.Blocked(
                        st.honestMessage() ?: "Licence abhi active nahi hai."
                    )
            }
            if (st.plan == "pro" && st.valid) {
                return Result.Allowed
            }
        }
        return localCapResult(prefs)
    }

    private suspend fun localCapResult(prefs: FloraPreferences): Result {
        if (freeRemainingSeconds(prefs) > 0L) {
            return Result.Allowed
        }
        return Result.Blocked(FREE_LIMIT_MESSAGE)
    }

    /**
     * Wave B scope addition (A3): free-limit "kitna bacha hai" —
     * gate aur display EK hi counter se chalte hain (local
     * talk-seconds counter). Server usage-count ka aaj ka total
     * wapas nahi milta (wo sirf count karta hai), isliye display
     * ka source bhi yahi counter hai — do sach nahi banenge.
     * Pro ke liye Long.MAX_VALUE (unlimited dikhane ke liye).
     */
    suspend fun freeRemainingSeconds(prefs: FloraPreferences): Long {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val used = try {
            prefs.talkSecondsUsed(today)
        } catch (e: Exception) {
            CatcherHooks.licenceRecord("exception")
            0L
        }
        return (FREE_DAILY_SECONDS - used).coerceAtLeast(0L)
    }

    // 013: har gated use par fresh verify attempt — koi
    // time-based cache suppression nahi. Sirf tab null milta
    // hai jab uid/dev na ho ya verify fail ho; tab caller
    // (checkInner) last-known cached par girta hai (fail-open).
    private suspend fun freshStatus(prefs: FloraPreferences): LicenceStatus? {
        val uid = try {
            prefs.effectiveUserId()
        } catch (e: Exception) {
            CatcherHooks.licenceRecord("exception")
            ""
        }
        val dev = try {
            prefs.storedDeviceId()
        } catch (e: Exception) {
            CatcherHooks.licenceRecord("exception")
            ""
        }
        if (uid.isEmpty() || dev.isEmpty()) {
            return null
        }
        val st = try {
            LicenceClient(prefs).verify(uid, dev)
        } catch (e: Exception) {
            CatcherHooks.licenceRecord("exception")
            null
        }
        if (st != null && st.serverChecked) {
            cached = st
            return st
        }
        return null
    }
}
