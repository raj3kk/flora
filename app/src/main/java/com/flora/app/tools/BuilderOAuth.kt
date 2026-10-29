package com.flora.app.tools

import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * B3-03 guided login — OAuth authorization-code + PKCE helpers
 * (pure JVM — java.util.Base64, isliye unit test me seedha chalta hai).
 *
 * R-B3-1 (binding):
 * - Sirf authorization-code flow, PKCE ke saath jahan provider
 *   support karta hai. Implicit flow kabhi nahi.
 * - Code/token SIRF redirect URL se pakda jata hai — page content,
 *   password field, cookie kabhi nahi padhe jate (WebView me koi
 *   evaluateJavascript, koi CookieManager use nahi hota).
 * - Redirect URI kabhi fabricate nahi hota: user apne OAuth app
 *   (dashboard step, Phase 9/web track) se mili client ID +
 *   redirect URI khud daalta hai. Bina asli user grant ke live
 *   OAuth proof ka daava nahi — wo Phase 10 ka kaam hai.
 *
 * Scope strings — OFFICIAL docs se (R-B3-2, invent nahi):
 * - GitHub: `repo` = public + private repos par full access
 *   (docs.github.com, "Scopes for OAuth apps").
 * - Supabase Management API OAuth: colon-form scopes —
 *   `projects:read projects:write database:read database:write
 *   organizations:read` (supabase.com Management API reference:
 *   create-project = projects:write, run-query = database:write;
 *   OAuth scopes page). Scope-param encoding Phase 10 live grant
 *   par confirm hogi — code me yahi documented set hai.
 * - Vercel: scope-string model hi nahi hai — Vercel API auth
 *   dashboard se bana Full Access token hai (account/team
 *   scope). Isliye Vercel ke liye koi authorize endpoint/scope
 *   yahan invent nahi kiya gaya; Vercel guided-login = dashboard
 *   token guide + manual/clipboard raasta.
 */
object BuilderOAuth {

    const val GITHUB_AUTHORIZE_URL = "https://github.com/login/oauth/authorize"
    const val GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token"
    const val GITHUB_SCOPE = "repo"

    const val SUPABASE_AUTHORIZE_URL = "https://api.supabase.com/v1/oauth/authorize"
    const val SUPABASE_TOKEN_URL = "https://api.supabase.com/v1/oauth/token"
    const val SUPABASE_SCOPE =
        "projects:read projects:write database:read database:write organizations:read"

    data class PkcePair(val verifier: String, val challenge: String)

    /**
     * P2-2: har OAuth flow ke liye random state (CSRF). Fixed
     * string kabhi nahi — caller ise flow ke liye store karta
     * hai aur redirect par parseCodeFromRedirect se validate
     * karwata hai. Base64url, no padding (PKCE challenge jaisa).
     */
    fun newState(): String {
        val random = ByteArray(16)
        SecureRandom().nextBytes(random)
        return java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(random)
    }

    fun newPkcePair(): PkcePair {
        val random = ByteArray(32)
        SecureRandom().nextBytes(random)
        val verifier = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(random)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        val challenge = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(digest)
        return PkcePair(verifier, challenge)
    }

    fun challengeFor(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(digest)
    }

    fun buildAuthorizeUrl(
        authorizeEndpoint: String,
        clientId: String,
        redirectUri: String,
        scope: String,
        challenge: String,
        state: String
    ): String {
        fun enc(value: String): String =
            java.net.URLEncoder.encode(value, "UTF-8")
        return authorizeEndpoint +
            "?response_type=code" +
            "&client_id=" + enc(clientId) +
            "&redirect_uri=" + enc(redirectUri) +
            "&scope=" + enc(scope) +
            "&code_challenge=" + enc(challenge) +
            "&code_challenge_method=S256" +
            "&state=" + enc(state)
    }

    /**
     * Redirect URL se sirf `code` nikalta hai — tabhi jab URL
     * user ke diye hue redirect URI se shuru hota ho AUR query
     * ka `state` caller ke stored state se EXACT match kare.
     * State missing/mismatch, error redirect, code-less redirect,
     * koi aur URL -> null (koi code capture nahi). Page content
     * isme kabhi nahi aata. 2-arg overload JAANBOOJH kar nahi
     * hai — bina state validation ke parse karna hi mana hai.
     */
    fun parseCodeFromRedirect(
        url: String,
        expectedRedirectPrefix: String,
        expectedState: String
    ): String? {
        if (expectedRedirectPrefix.isBlank()) return null
        if (expectedState.isBlank()) return null
        if (!url.startsWith(expectedRedirectPrefix)) return null
        val query = url.substringAfter('?', "")
        if (query.isEmpty()) return null
        val params = mutableMapOf<String, String>()
        for (part in query.split('&')) {
            val key = part.substringBefore('=', "")
            if (key.isEmpty() || params.containsKey(key)) continue
            params[key] = try {
                URLDecoder.decode(part.substringAfter('=', ""), "UTF-8")
            } catch (e: Exception) {
                return null
            }
        }
        if (params.containsKey("error")) return null
        if (params["state"] != expectedState) return null
        val code = params["code"] ?: return null
        if (code.isEmpty()) return null
        return code
    }
}
