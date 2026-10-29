package com.flora.app.data

/**
 * B3 Builder Pro connect — builder-service token model (pure, no
 * Android dependency, isliye unit test me seedha chalta hai).
 *
 * Teen services, teeno alag: GitHub (repo), Vercel (project/deploy),
 * Supabase (user ka apna project — Flora ke apne project se kabhi
 * nahi judta; FloraConfig ka Supabase URL yahan kabhi nahi aata).
 * Token values is file me kabhi nahi hoti — sirf metadata (connected
 * flag, expiry, auth kind, user-project base URL/ref). Asli token
 * FloraPreferences ke readSecret/writeSecret se KeyStore-encrypted
 * DataStore me rehta hai (B3-04).
 */
enum class BuilderService {
    GITHUB,
    VERCEL,
    SUPABASE
}

enum class BuilderAuthKind {
    OAUTH,
    PAT
}

data class BuilderConnection(
    val service: BuilderService,
    val connected: Boolean,
    val hasToken: Boolean,
    val authKind: BuilderAuthKind?,
    val expiresAtMs: Long,
    /** Supabase ke liye user ke apne project ka base URL; baaki ke liye khali. */
    val baseUrl: String,
    /** Supabase project ref; baaki ke liye khali. */
    val projectRef: String
)

/**
 * B3-08 grant-once state machine (pure decision — prefs/client isko
 * padhte hain, khud faisla nahi gadte).
 *
 * - NOT_CONNECTED: koi grant hi nahi — pehli baar connect maango.
 * - REUSE: grant maujood aur valid — dobara prompt KABHI NAHI.
 * - REFRESH_SILENTLY: OAuth token expire hua par refresh token
 *   maujood hai — chupchaap refresh, user ko dobara login nahi.
 * - REPROMPT_ONCE: PAT/token expire hua aur koi refresh raasta
 *   nahi — plan ka eklauta exception: ek baar dobara paste maango.
 * Reinstall/data-clear par KeyStore-bound ciphertext nahi bachta,
 * isliye wahan NOT_CONNECTED wapas aana expected hai, grant-once
 * ka fail nahi (R-B3-6 — UI copy me bhi yahi likha hai).
 */
object BuilderGrantOnce {

    enum class Decision {
        NOT_CONNECTED,
        REUSE,
        REFRESH_SILENTLY,
        REPROMPT_ONCE
    }

    fun decide(
        connected: Boolean,
        hasToken: Boolean,
        expiresAtMs: Long,
        nowMs: Long,
        authKind: BuilderAuthKind?,
        hasRefreshToken: Boolean
    ): Decision {
        if (!connected || !hasToken) return Decision.NOT_CONNECTED
        val expired = expiresAtMs > 0L && nowMs >= expiresAtMs
        if (!expired) return Decision.REUSE
        return if (authKind == BuilderAuthKind.OAUTH && hasRefreshToken) {
            Decision.REFRESH_SILENTLY
        } else {
            Decision.REPROMPT_ONCE
        }
    }
}
