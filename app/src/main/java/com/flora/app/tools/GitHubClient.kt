package com.flora.app.tools

import android.util.Base64
import com.flora.app.data.BuilderAuthKind
import com.flora.app.data.BuilderService
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * B3-01 GitHub client (manual constructor DI: GitHubClient(prefs)
 * — LicenceClient/BrainClient wala pattern; Hilt nahi).
 *
 * Host SIRF api.github.com (B3-09) — Flora ka website host ya
 * Flora Supabase URL yahan kabhi nahi aata. brain ask call /
 * brain-proxy kabhi call nahi hota (R-B3-8), talkSecondsUsed
 * kabhi nahi badhta. Token value kabhi log nahi hoti (B3-07:
 * HttpLoggingInterceptor HEADERS + redactHeader).
 *
 * Endpoints (GitHub REST docs):
 * - GET /user — who-am-I/scope check (side-effect nahi, isliye
 *   grant-once reuse validation isi se hoti hai).
 * - POST /user/repos — repo create (scope `repo`).
 * - GET/PUT /repos/{owner}/{repo}/contents/{path} — file
 *   create/update (scope `repo`; update ke liye pehle GET se sha).
 * - POST https://github.com/login/oauth/access_token —
 *   PKCE code exchange + refresh exchange (client secret app me
 *   kabhi nahi rakha jata; secret maangne wala provider jawab
 *   de to caller genuine-block branch par girta hai).
 * Refresh semantics (R-B3-4): PAT expire -> RepasteOnce;
 * OAuth + refresh token -> silent refresh attempt, fail par
 * RegrantOnce. Vercel/Supabase se alag, GitHub ka refresh
 * attempt secret-free PKCE style me hota hai.
 */
class GitHubClient(
    private val prefs: FloraPreferences,
    private val http: OkHttpClient = BuilderOkHttp.newClient()
) {

    data class Repo(val fullName: String, val htmlUrl: String)
    data class TokenResult(val accessToken: String, val refreshToken: String)

    private data class Raw(val code: Int, val body: String)

    private fun call(method: String, url: String, token: String, json: JSONObject?): Raw {
        val builder = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")
            .addHeader("Authorization", "Bearer $token")
        if (json != null) {
            val body = json.toString().toRequestBody("application/json".toMediaType())
            if (method == "POST") builder.post(body) else builder.put(body)
        } else if (method == "POST") {
            builder.post(ByteArray(0).toRequestBody(null))
        }
        val response = http.newCall(builder.build()).execute()
        return response.use { res ->
            Raw(res.code, res.body?.string() ?: "")
        }
    }

    private suspend fun token(): String = try {
        prefs.builderToken(BuilderService.GITHUB)
    } catch (e: Exception) {
        ""
    }

    /** Cheap who-am-I check — koi side-effect nahi (grant-once reuse validation). */
    suspend fun whoAmI(): BuilderOutcome<String> = withContext(Dispatchers.IO) {
        val tok = token()
        if (tok.isEmpty()) {
            return@withContext BuilderOutcome.Failure(
                BuilderFailureKind.AUTH_EXPIRED, "not connected"
            )
        }
        try {
            val raw = call("GET", BuilderHosts.githubApiBase() + "/user", tok, null)
            if (raw.code in 200..299) {
                val login = JSONObject(raw.body).optString("login", "")
                if (login.isEmpty()) {
                    BuilderOutcome.Failure(BuilderFailureKind.SERVER, "empty login")
                } else {
                    BuilderOutcome.Ok(login)
                }
            } else {
                BuilderOutcome.Failure(BuilderHttp.classifyHttp(raw.code), "http ${raw.code}")
            }
        } catch (e: Exception) {
            BuilderOutcome.Failure(BuilderFailureKind.NETWORK, "network")
        }
    }

    suspend fun createRepo(name: String, privateRepo: Boolean): BuilderOutcome<Repo> =
        withContext(Dispatchers.IO) {
            val tok = token()
            if (tok.isEmpty()) {
                return@withContext BuilderOutcome.Failure(
                    BuilderFailureKind.AUTH_EXPIRED, "not connected"
                )
            }
            try {
                val body = JSONObject()
                    .put("name", name)
                    .put("private", privateRepo)
                val raw = call("POST", BuilderHosts.githubApiBase() + "/user/repos", tok, body)
                if (raw.code in 200..299) {
                    val json = JSONObject(raw.body)
                    BuilderOutcome.Ok(
                        Repo(
                            json.optString("full_name", ""),
                            json.optString("html_url", "")
                        )
                    )
                } else {
                    BuilderOutcome.Failure(BuilderHttp.classifyHttp(raw.code), "http ${raw.code}")
                }
            } catch (e: Exception) {
                BuilderOutcome.Failure(BuilderFailureKind.NETWORK, "network")
            }
        }

    /**
     * File create/update — content project file se aata hai, par
     * B3-11 ke mutabiq builder token kabhi project file me nahi
     * likha jata aur ye function token ko file content me kabhi
     * nahi milata (caller ki zimmedari: KeyInputGuard se guzra
     * hua content hi bhejo).
     */
    suspend fun pushFile(
        owner: String,
        repo: String,
        path: String,
        content: String,
        message: String
    ): BuilderOutcome<String> = withContext(Dispatchers.IO) {
        val tok = token()
        if (tok.isEmpty()) {
            return@withContext BuilderOutcome.Failure(
                BuilderFailureKind.AUTH_EXPIRED, "not connected"
            )
        }
        try {
            val url = BuilderHosts.githubApiBase() + "/repos/$owner/$repo/contents/$path"
            var sha = ""
            val existing = call("GET", url, tok, null)
            if (existing.code in 200..299) {
                sha = JSONObject(existing.body).optString("sha", "")
            }
            val body = JSONObject()
                .put("message", message)
                .put(
                    "content",
                    Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                )
            if (sha.isNotEmpty()) body.put("sha", sha)
            val raw = call("PUT", url, tok, body)
            if (raw.code in 200..299) {
                val commitSha = JSONObject(raw.body)
                    .optJSONObject("commit")?.optString("sha", "") ?: ""
                BuilderOutcome.Ok(commitSha)
            } else {
                BuilderOutcome.Failure(BuilderHttp.classifyHttp(raw.code), "http ${raw.code}")
            }
        } catch (e: Exception) {
            BuilderOutcome.Failure(BuilderFailureKind.NETWORK, "network")
        }
    }

    /** PKCE code exchange — client secret app me kabhi nahi hota. */
    suspend fun exchangeCodeForToken(
        clientId: String,
        code: String,
        codeVerifier: String,
        redirectUri: String
    ): BuilderOutcome<TokenResult> = withContext(Dispatchers.IO) {
        try {
            val form = "client_id=${encode(clientId)}" +
                "&code=${encode(code)}" +
                "&code_verifier=${encode(codeVerifier)}" +
                "&redirect_uri=${encode(redirectUri)}" +
                "&grant_type=authorization_code"
            val request = Request.Builder()
                .url(BuilderOAuth.GITHUB_TOKEN_URL)
                .addHeader("Accept", "application/json")
                .post(form.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()
            val response = http.newCall(request).execute()
            response.use { res ->
                val body = res.body?.string() ?: ""
                if (res.code in 200..299) {
                    val json = JSONObject(body)
                    val access = json.optString("access_token", "")
                    if (access.isEmpty()) {
                        BuilderOutcome.Failure(BuilderFailureKind.SERVER, "empty access token")
                    } else {
                        BuilderOutcome.Ok(
                            TokenResult(access, json.optString("refresh_token", ""))
                        )
                    }
                } else {
                    BuilderOutcome.Failure(BuilderHttp.classifyHttp(res.code), "http ${res.code}")
                }
            }
        } catch (e: Exception) {
            BuilderOutcome.Failure(BuilderFailureKind.NETWORK, "network")
        }
    }

    /** Silent refresh attempt — fail par caller RegrantOnce branch par jata hai. */
    suspend fun refreshSilently(clientId: String): BuilderOutcome<TokenResult> =
        withContext(Dispatchers.IO) {
            val refresh = try {
                prefs.builderRefreshToken(BuilderService.GITHUB)
            } catch (e: Exception) {
                ""
            }
            if (refresh.isEmpty()) {
                return@withContext BuilderOutcome.Failure(
                    BuilderFailureKind.AUTH_EXPIRED, "no refresh token"
                )
            }
            try {
                val form = "client_id=${encode(clientId)}" +
                    "&grant_type=refresh_token" +
                    "&refresh_token=${encode(refresh)}"
                val request = Request.Builder()
                    .url(BuilderOAuth.GITHUB_TOKEN_URL)
                    .addHeader("Accept", "application/json")
                    .post(form.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                    .build()
                val response = http.newCall(request).execute()
                response.use { res ->
                    val body = res.body?.string() ?: ""
                    if (res.code in 200..299) {
                        val json = JSONObject(body)
                        val access = json.optString("access_token", "")
                        if (access.isEmpty()) {
                            BuilderOutcome.Failure(BuilderFailureKind.SERVER, "empty access token")
                        } else {
                            BuilderOutcome.Ok(
                                TokenResult(access, json.optString("refresh_token", refresh))
                            )
                        }
                    } else {
                        BuilderOutcome.Failure(BuilderHttp.classifyHttp(res.code), "http ${res.code}")
                    }
                }
            } catch (e: Exception) {
                BuilderOutcome.Failure(BuilderFailureKind.NETWORK, "network")
            }
        }

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")

    companion object {
        /** UI ke liye: is client ka auth kind kya mana jaye (save ke waqt). */
        fun authKindForSavedToken(hasRefreshToken: Boolean): BuilderAuthKind =
            if (hasRefreshToken) BuilderAuthKind.OAUTH else BuilderAuthKind.PAT
    }
}
