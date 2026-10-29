package com.flora.app.tools

import com.flora.app.data.BuilderAuthKind
import com.flora.app.data.BuilderGrantOnce
import com.flora.app.data.BuilderService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * B3 unit tests — sirf pure-JVM layer (guard, grant-once, PKCE).
 * Saare fixtures SYNTHETIC shape hain (dohraya hua char, koi asli
 * ya example key value nahi). Android classes (prefs, clipboard,
 * WebView) yahan nahi chalte — unke raaste CI compile + Phase 10
 * phone checklist se prove honge. Repo CI sirf :app:assembleDebug
 * chalata hai, isliye ye tests CI par execute nahi hote — local
 * `./gradlew :app:testDebugUnitTest` se chalane ke liye hain.
 */
class BuilderGuardTest {

    // Synthetic shape fixtures — repeated chars, never a real value.
    private val githubClassic = "ghp_" + "a".repeat(36)
    private val githubFineGrained = "github_pat_" + "b".repeat(40)
    private val vercelShape = "c".repeat(24)
    private val supabasePat = "sbp_" + "d".repeat(40)
    private val supabasePublishable = "sb_publishable_" + "e".repeat(30)
    private val jwtShape = "aaaabbbbcccc" + "." + "dddd11112222" + "." + "eeee33334444"

    @Test
    fun githubShapesAccepted() {
        assertTrue(KeyInputGuard.matchesServiceShape(BuilderService.GITHUB, githubClassic))
        assertTrue(KeyInputGuard.matchesServiceShape(BuilderService.GITHUB, githubFineGrained))
        assertFalse(KeyInputGuard.matchesServiceShape(BuilderService.GITHUB, vercelShape))
    }

    @Test
    fun vercelShapeAccepted() {
        assertTrue(KeyInputGuard.matchesServiceShape(BuilderService.VERCEL, vercelShape))
        assertFalse(KeyInputGuard.matchesServiceShape(BuilderService.VERCEL, githubClassic))
        assertFalse(KeyInputGuard.matchesServiceShape(BuilderService.VERCEL, "short"))
    }

    @Test
    fun supabaseShapesAccepted() {
        assertTrue(KeyInputGuard.matchesServiceShape(BuilderService.SUPABASE, supabasePat))
        assertTrue(KeyInputGuard.matchesServiceShape(BuilderService.SUPABASE, supabasePublishable))
        assertTrue(KeyInputGuard.matchesServiceShape(BuilderService.SUPABASE, jwtShape))
        assertFalse(KeyInputGuard.matchesServiceShape(BuilderService.SUPABASE, githubClassic))
    }

    @Test
    fun cookieSessionPasswordRejected() {
        val cookie = "session=abcdef123456; Path=/; HttpOnly"
        assertTrue(KeyInputGuard.looksLikeCookieOrSession(cookie))
        assertFalse(KeyInputGuard.matchesServiceShape(BuilderService.GITHUB, cookie))
        assertEquals(
            KeyInputGuard.INVALID_SHAPE_MESSAGE,
            KeyInputGuard.validateForService(BuilderService.GITHUB, cookie)
        )
        val labelled = "password=somewords1234567890"
        assertTrue(KeyInputGuard.looksLikeCookieOrSession(labelled))
        assertEquals(
            KeyInputGuard.INVALID_SHAPE_MESSAGE,
            KeyInputGuard.validateForService(BuilderService.VERCEL, labelled)
        )
    }

    @Test
    fun whitespaceAndRandomWordsRejected() {
        assertEquals(
            KeyInputGuard.INVALID_SHAPE_MESSAGE,
            KeyInputGuard.validateForService(BuilderService.SUPABASE, "ye koi key nahi hai")
        )
        assertEquals(
            KeyInputGuard.INVALID_SHAPE_MESSAGE,
            KeyInputGuard.validateForService(BuilderService.GITHUB, "")
        )
        assertFalse(KeyInputGuard.looksKeyLike("aaj mausam bahut achha hai, chalo kaam karte hain"))
    }

    @Test
    fun chatVoiceGuardCatchesEmbeddedKey() {
        assertTrue(KeyInputGuard.looksKeyLike("meri key ye hai $githubClassic please save karo"))
        assertTrue(KeyInputGuard.looksKeyLike(githubClassic))
        assertTrue(KeyInputGuard.looksKeyLike("ye lo $vercelShape"))
        assertFalse(KeyInputGuard.looksKeyLike("hello flora, aaj ka kaam batao"))
    }

    @Test
    fun grantOnceDecisions() {
        val now = 1_000_000L
        assertEquals(
            BuilderGrantOnce.Decision.REUSE,
            BuilderGrantOnce.decide(true, true, now + 60_000L, now, BuilderAuthKind.PAT, false)
        )
        assertEquals(
            BuilderGrantOnce.Decision.REUSE,
            BuilderGrantOnce.decide(true, true, 0L, now, BuilderAuthKind.PAT, false)
        )
        assertEquals(
            BuilderGrantOnce.Decision.REPROMPT_ONCE,
            BuilderGrantOnce.decide(true, true, now - 1L, now, BuilderAuthKind.PAT, false)
        )
        assertEquals(
            BuilderGrantOnce.Decision.REFRESH_SILENTLY,
            BuilderGrantOnce.decide(true, true, now - 1L, now, BuilderAuthKind.OAUTH, true)
        )
        assertEquals(
            BuilderGrantOnce.Decision.REPROMPT_ONCE,
            BuilderGrantOnce.decide(true, true, now - 1L, now, BuilderAuthKind.OAUTH, false)
        )
        assertEquals(
            BuilderGrantOnce.Decision.NOT_CONNECTED,
            BuilderGrantOnce.decide(false, false, 0L, now, null, false)
        )
        assertEquals(
            BuilderGrantOnce.Decision.NOT_CONNECTED,
            BuilderGrantOnce.decide(true, false, 0L, now, BuilderAuthKind.PAT, false)
        )
    }

    @Test
    fun pkceChallengeIsBase64UrlWithoutPadding() {
        val verifier = "test-verifier-value-for-challenge-check-123456"
        val challenge = BuilderOAuth.challengeFor(verifier)
        assertFalse(challenge.contains("="))
        assertFalse(challenge.contains("+"))
        assertFalse(challenge.contains("/"))
        assertEquals(43, challenge.length)
        assertEquals(challenge, BuilderOAuth.challengeFor(verifier))
    }

    @Test
    fun authorizeUrlCarriesScopeAndChallenge() {
        val url = BuilderOAuth.buildAuthorizeUrl(
            BuilderOAuth.GITHUB_AUTHORIZE_URL,
            "test-client",
            "flora://oauth/github",
            BuilderOAuth.GITHUB_SCOPE,
            "test-challenge-value",
            "test-state"
        )
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("scope=repo"))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("code_challenge=test-challenge-value"))
    }

    @Test
    fun redirectCodeParsedOnlyFromExpectedRedirect() {
        val code = BuilderOAuth.parseCodeFromRedirect(
            "flora://oauth/github?code=abc123&state=test-state",
            "flora://oauth/github"
        )
        assertEquals("abc123", code)
        assertNull(
            BuilderOAuth.parseCodeFromRedirect(
                "https://evil.example/callback?code=abc123",
                "flora://oauth/github"
            )
        )
        assertNull(
            BuilderOAuth.parseCodeFromRedirect(
                "flora://oauth/github?error=access_denied",
                "flora://oauth/github"
            )
        )
        assertNull(
            BuilderOAuth.parseCodeFromRedirect(
                "flora://oauth/github?state=test-state",
                "flora://oauth/github"
            )
        )
    }

    @Test
    fun hostSeparationConstants() {
        assertEquals("api.github.com", BuilderHosts.GITHUB_API)
        assertEquals("api.vercel.com", BuilderHosts.VERCEL_API)
        assertEquals("api.supabase.com", BuilderHosts.SUPABASE_MGMT)
        assertEquals(
            "https://myref123.supabase.co",
            BuilderHosts.userProjectBaseUrl("myref123")
        )
    }

    @Test
    fun httpClassification() {
        assertEquals(BuilderFailureKind.AUTH_EXPIRED, BuilderHttp.classifyHttp(401))
        assertEquals(BuilderFailureKind.SCOPE_SHORT, BuilderHttp.classifyHttp(403))
        assertEquals(BuilderFailureKind.RATE_LIMITED, BuilderHttp.classifyHttp(429))
        assertEquals(BuilderFailureKind.SERVER, BuilderHttp.classifyHttp(500))
        assertEquals(BuilderFailureKind.BAD_REQUEST, BuilderHttp.classifyHttp(422))
    }

    @Test
    fun authFailureMapping() {
        assertEquals(
            BuilderAuthFailure.RepasteOnce,
            BuilderHttp.authFailureFor(BuilderService.GITHUB, BuilderFailureKind.AUTH_EXPIRED, BuilderAuthKind.PAT, false)
        )
        assertEquals(
            BuilderAuthFailure.SilentRefresh,
            BuilderHttp.authFailureFor(BuilderService.SUPABASE, BuilderFailureKind.AUTH_EXPIRED, BuilderAuthKind.OAUTH, true)
        )
        assertEquals(
            BuilderAuthFailure.RegrantOnce,
            BuilderHttp.authFailureFor(BuilderService.SUPABASE, BuilderFailureKind.AUTH_EXPIRED, BuilderAuthKind.OAUTH, false)
        )
        assertEquals(
            BuilderAuthFailure.Blocked,
            BuilderHttp.authFailureFor(BuilderService.VERCEL, BuilderFailureKind.RATE_LIMITED, BuilderAuthKind.PAT, false)
        )
    }

    @Test
    fun redactionMasksTokenHeaders() {
        val cleaned = BuilderRedaction.sanitizeHeaders(
            mapOf(
                "Authorization" to "Bearer secret-value",
                "Content-Type" to "application/json",
                "apikey" to "some-key-value",
                "Cookie" to "session=abc"
            )
        )
        assertEquals("application/json", cleaned["Content-Type"])
        assertFalse(cleaned["Authorization"]!!.contains("secret-value"))
        assertFalse(cleaned["apikey"]!!.contains("some-key-value"))
        assertFalse(cleaned["Cookie"]!!.contains("abc"))
    }
}
