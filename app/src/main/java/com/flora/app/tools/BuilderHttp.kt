package com.flora.app.tools

import com.flora.app.data.BuilderAuthKind
import com.flora.app.data.BuilderService
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * B3 shared HTTP layer — teeno builder clients isi se chalte hain.
 *
 * Host separation (B3-09, P1): builder calls SIRF teeno
 * third-party hosts par jate hain. Flora ka apna Supabase URL
 * (FloraConfig) yahan kabhi nahi aata — user-project base URL
 * prefs me saved user grant se aata hai, Flora ke project se nahi.
 * PinnedTrust ko chheda nahi gaya: wo sirf Flora ke do hosts par
 * pin lagata hai, baaki hosts par plain system trust deta hai —
 * isliye builder hosts par koi naya pin jodne ki zaroorat nahi.
 *
 * Brain-seconds (R-B3-8): ye clients brain ask call ya
 * brain-proxy ko kabhi call nahi karte, talkSecondsUsed kabhi
 * nahi badhate — seedha third-party API par jate hain.
 */
object BuilderHosts {
    const val GITHUB_API = "api.github.com"
    const val GITHUB_WEB = "github.com"
    const val VERCEL_API = "api.vercel.com"
    const val SUPABASE_MGMT = "api.supabase.com"

    fun githubApiBase(): String = "https://$GITHUB_API"
    fun vercelApiBase(): String = "https://$VERCEL_API"
    fun supabaseMgmtBase(): String = "https://$SUPABASE_MGMT"
    fun userProjectBaseUrl(projectRef: String): String =
        "https://$projectRef.supabase.co"
}

enum class BuilderFailureKind {
    AUTH_EXPIRED,
    SCOPE_SHORT,
    RATE_LIMITED,
    SERVER,
    BAD_REQUEST,
    NETWORK
}

/**
 * B3-05 expiry mapping (pure): 401/403 kabhi chupchaap fail nahi
 * hote, kabhi jhootha success nahi — hamesha ek imaandaar branch:
 * - SilentRefresh: OAuth + refresh token maujood (attempt; fail
 *   hone par caller Repaste/Regrant branch par girta hai).
 * - RepasteOnce: PAT/token, koi refresh raasta nahi — plan ka
 *   eklauta exception, ek baar dobara paste maango.
 * - RegrantOnce: Supabase OAuth bina usable refresh ke — dobara
 *   grant maango (genuine block, wahi imaandaari).
 * - Blocked: rate-limit/server jaise genuine blocks — dashboard
 *   fallback ka raasta dikhao, "ho gaya" kabhi mat bolo.
 */
sealed class BuilderAuthFailure {
    object SilentRefresh : BuilderAuthFailure()
    object RepasteOnce : BuilderAuthFailure()
    object RegrantOnce : BuilderAuthFailure()
    object Blocked : BuilderAuthFailure()
}

sealed class BuilderOutcome<out T> {
    data class Ok<out T>(val value: T) : BuilderOutcome<T>()
    data class Failure(val kind: BuilderFailureKind, val detail: String) :
        BuilderOutcome<Nothing>()
}

object BuilderHttp {

    fun classifyHttp(code: Int): BuilderFailureKind = when (code) {
        401 -> BuilderFailureKind.AUTH_EXPIRED
        403 -> BuilderFailureKind.SCOPE_SHORT
        429 -> BuilderFailureKind.RATE_LIMITED
        in 500..599 -> BuilderFailureKind.SERVER
        else -> BuilderFailureKind.BAD_REQUEST
    }

    fun authFailureFor(
        service: BuilderService,
        kind: BuilderFailureKind,
        authKind: BuilderAuthKind?,
        hasRefreshToken: Boolean
    ): BuilderAuthFailure {
        if (kind != BuilderFailureKind.AUTH_EXPIRED &&
            kind != BuilderFailureKind.SCOPE_SHORT
        ) return BuilderAuthFailure.Blocked
        if (authKind == BuilderAuthKind.OAUTH && hasRefreshToken) {
            return BuilderAuthFailure.SilentRefresh
        }
        if (authKind == BuilderAuthKind.OAUTH) {
            return BuilderAuthFailure.RegrantOnce
        }
        return BuilderAuthFailure.RepasteOnce
    }

    /** Genuine-block ke liye warm, non-technical message (chat/voice ke liye). */
    fun warmMessage(kind: BuilderFailureKind): String = when (kind) {
        BuilderFailureKind.AUTH_EXPIRED ->
            "Is service ka connection expire ho gaya hai — Services screen se dobara jodo."
        BuilderFailureKind.SCOPE_SHORT ->
            "Is service par permission kam pad rahi hai — Services screen me scope dobara check karo, ya dashboard se karo."
        BuilderFailureKind.RATE_LIMITED ->
            "Service ne abhi zyada requests rok di hain — thodi der baad dobara koshish karo, ya dashboard se karo."
        BuilderFailureKind.SERVER ->
            "Service ki taraf se kuch gadbad hui hai — thodi der baad dobara koshish karo."
        BuilderFailureKind.BAD_REQUEST ->
            "Request service ne nahi maani — Services screen me details check karo."
        BuilderFailureKind.NETWORK ->
            "Network nahi mil raha — connection check karke dobara koshish karo."
    }

    /** Dashboard fallback (plan: API block par user khud dashboard se kar sake). */
    fun dashboardFallback(service: BuilderService): String = when (service) {
        BuilderService.GITHUB -> "GitHub dashboard se repo banao/push karo, phir Flora me dobara koshish karo."
        BuilderService.VERCEL -> "Vercel dashboard se project/deploy karo, phir Flora me dobara koshish karo."
        BuilderService.SUPABASE -> "Supabase dashboard se project/SQL chalao, phir Flora me dobara koshish karo."
    }
}

/**
 * B3-07 logging-redact enforcement (code me dikhta hua rule):
 * - HttpLoggingInterceptor Level.HEADERS par hai — BODY kabhi
 *   nahi (body me token/secret aa sakta hai, isliye BODY level
 *   is codebase me banned hai; grep gate: body-level logging 0 hits).
 * - redactHeader() calls Authorization + Cookie + apikey +
 *   x-api-key ko hamesha mask karte hain — interceptor ke log
 *   me token value kabhi nahi aati.
 * - sanitizeHeaders() wahi rule pure function ke roop me hai
 *   (unit test me check hota hai) — koi bhi B3 log line isse
 *   guzre bina token-bearing header nahi chhapti. Asli rule:
 *   full token kabhi log/chat/screen/Auto Log me nahi.
 */
object BuilderRedaction {

    private val tokenHeaders = setOf("authorization", "cookie", "apikey", "x-api-key")

    fun isTokenBearingHeader(name: String): Boolean =
        name.lowercase() in tokenHeaders

    fun sanitizeHeaders(headers: Map<String, String>): Map<String, String> =
        headers.mapValues { (name, value) ->
            if (isTokenBearingHeader(name)) "███" else value
        }
}

object BuilderOkHttp {

    fun newClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
            redactHeader("Authorization")
            redactHeader("Cookie")
            redactHeader("apikey")
            redactHeader("x-api-key")
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
