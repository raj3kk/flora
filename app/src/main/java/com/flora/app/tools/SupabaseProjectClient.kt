package com.flora.app.tools
import com.flora.app.data.CatcherHooks

import com.flora.app.data.BuilderService
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * B3-01 Supabase client — USER ke apne project ke liye (B3-09, R-B3-7).
 *
 * Surface split (official Management API reference se pin kiya gaya):
 * - Project-level operations — Management API, base
 *   https://api.supabase.com, user ke grant wale token se:
 *   - GET /v1/projects — list
 *   - POST /v1/projects {name, organization_slug, db_pass} —
 *     create (OAuth scope projects:write). db_pass sirf is call
 *     ka param hai — kabhi save/log nahi hota.
 *   - POST /v1/projects/{ref}/database/query {query, read_only}
 *     — SQL run (OAuth scope database:write; docs par
 *     experimental/beta likha hai — isliye caller isko beta
 *     samajh kar chalaye, dashboard fallback hamesha rahe).
 * - User-project data operations — user-project API, base
 *   https://<ref>.supabase.co, user token se (PostgREST):
 *   - GET /rest/v1/{table}?select=* — row-level data read.
 *   Arbitrary SQL ka documented raasta Management query endpoint
 *   hi hai — user-project par koi SQL endpoint invent nahi kiya
 *   gaya (R-B3-7 ka ye nuance handoff me bhi darj hai).
 *
 * Flora ke apne project ka URL/anon key yahan KABHI use nahi
 * hota — base URL prefs me saved user grant se aata hai.
 * brain ask call / brain-proxy kabhi call nahi hota (R-B3-8).
 * Refresh (R-B3-4): Management OAuth + refresh token ho to
 * silent refresh attempt (public-client style, secret-free);
 * fail par RegrantOnce — kabhi silent failure, kabhi fake success
 * nahi.
 */
/**
 * B3-01-S follow-up: Management list-projects response parsing
 * (pure — unit test me bina prefs ke chalta hai).
 *
 * Management API list-projects ek BARE JSON ARRAY deta hai
 * (list endpoints ka shape); kuch wrapper ek object me
 * `projects` array bhi de sakte hain. Dono shapes maane jate
 * hain — koi aur shape (khaali body, galat JSON, projects ke
 * bina object) null deta hai, taaki caller imaandaar SERVER
 * failure de sake, jhootha 0-count success kabhi nahi.
 */
object SupabaseProjectParsing {

    fun parseProjects(body: String): List<SupabaseProjectClient.Project>? {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val array = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                JSONObject(trimmed).optJSONArray("projects") ?: return null
            }
            val out = mutableListOf<SupabaseProjectClient.Project>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val ref = obj.optString("id", obj.optString("ref", ""))
                if (ref.isEmpty()) continue
                out.add(SupabaseProjectClient.Project(ref, obj.optString("name", "")))
            }
            out
        } catch (e: Exception) {
            CatcherHooks.networkError("supabase", "exception", "exception")
            null
        }
    }

    fun countProjects(body: String): Int? =
        parseProjects(body)?.size
}

class SupabaseProjectClient(
    private val prefs: FloraPreferences,
    private val http: OkHttpClient = BuilderOkHttp.newClient()
) {

    data class Project(val ref: String, val name: String)
    data class TokenResult(
        val accessToken: String,
        val refreshToken: String,
        /** OAuth token response ka `expires_in` (seconds); 0 = provider ne expiry nahi di. */
        val expiresInSec: Long = 0L
    )

    private data class Raw(val code: Int, val body: String)

    private fun callMgmt(method: String, path: String, token: String, json: JSONObject?): Raw {
        val builder = Request.Builder()
            .url(BuilderHosts.supabaseMgmtBase() + path)
            .addHeader("Authorization", "Bearer $token")
        if (json != null) {
            val body = json.toString().toRequestBody("application/json".toMediaType())
            if (method == "POST") builder.post(body) else builder.put(body)
        }
        val response = http.newCall(builder.build()).execute()
        return response.use { res ->
            Raw(res.code, res.body?.string() ?: "")
        }
    }

    private suspend fun token(): String = try {
        prefs.builderToken(BuilderService.SUPABASE)
    } catch (e: Exception) {
        CatcherHooks.networkError("supabase", "exception", "exception")
        ""
    }

    /** Who-am-I: Management project list (cheap, side-effect nahi). */
    suspend fun whoAmI(): BuilderOutcome<Int> = withContext(Dispatchers.IO) {
        val tok = token()
        if (tok.isEmpty()) {
            return@withContext BuilderOutcome.Failure(
                BuilderFailureKind.AUTH_EXPIRED, "not connected"
            )
        }
        try {
            val raw = callMgmt("GET", "/v1/projects", tok, null)
            if (raw.code in 200..299) {
                val projects = SupabaseProjectParsing.parseProjects(raw.body)
                if (projects == null) {
                    BuilderOutcome.Failure(
                        BuilderFailureKind.SERVER, "unexpected projects shape"
                    )
                } else {
                    BuilderOutcome.Ok(projects.size)
                }
            } else {
                BuilderOutcome.Failure(BuilderHttp.classifyHttp(raw.code), "http ${raw.code}")
            }
        } catch (e: Exception) {
            CatcherHooks.networkError("supabase", "exception", "exception")
            BuilderOutcome.Failure(BuilderFailureKind.NETWORK, "network")
        }
    }

    /** Project list — Supabase ref chune ke liye (P2-8; whoAmI wahi parse). */
    suspend fun listProjects(): BuilderOutcome<List<Project>> = withContext(Dispatchers.IO) {
        val tok = token()
        if (tok.isEmpty()) {
            return@withContext BuilderOutcome.Failure(
                BuilderFailureKind.AUTH_EXPIRED, "not connected"
            )
        }
        try {
            val raw = callMgmt("GET", "/v1/projects", tok, null)
            if (raw.code in 200..299) {
                val projects = SupabaseProjectParsing.parseProjects(raw.body)
                if (projects == null) {
                    BuilderOutcome.Failure(
                        BuilderFailureKind.SERVER, "unexpected projects shape"
                    )
                } else {
                    BuilderOutcome.Ok(projects)
                }
            } else {
                BuilderOutcome.Failure(BuilderHttp.classifyHttp(raw.code), "http ${raw.code}")
            }
        } catch (e: Exception) {
            CatcherHooks.networkError("supabase", "exception", "exception")
            BuilderOutcome.Failure(BuilderFailureKind.NETWORK, "network")
        }
    }

    suspend fun createProject(
        name: String,
        organizationSlug: String,
        dbPassword: String
    ): BuilderOutcome<Project> = withContext(Dispatchers.IO) {
        val tok = token()
        if (tok.isEmpty()) {
            return@withContext BuilderOutcome.Failure(
                BuilderFailureKind.AUTH_EXPIRED, "not connected"
            )
        }
        // dbPassword sirf is request body me jata hai — is function
        // ke bahar kabhi save/log nahi hota (param hi hai, field nahi).
        try {
            val body = JSONObject()
                .put("name", name)
                .put("organization_slug", organizationSlug)
                .put("db_pass", dbPassword)
            val raw = callMgmt("POST", "/v1/projects", tok, body)
            if (raw.code in 200..299 || raw.code == 201) {
                val json = JSONObject(raw.body)
                BuilderOutcome.Ok(
                    Project(json.optString("ref", json.optString("id", "")), name)
                )
            } else {
                BuilderOutcome.Failure(BuilderHttp.classifyHttp(raw.code), "http ${raw.code}")
            }
        } catch (e: Exception) {
            CatcherHooks.networkError("supabase", "exception", "exception")
            BuilderOutcome.Failure(BuilderFailureKind.NETWORK, "network")
        }
    }

    /** SQL run — Management query endpoint (beta per docs). */
    suspend fun runSql(
        projectRef: String,
        query: String,
        readOnly: Boolean = true
    ): BuilderOutcome<String> = withContext(Dispatchers.IO) {
        val tok = token()
        if (tok.isEmpty()) {
            return@withContext BuilderOutcome.Failure(
                BuilderFailureKind.AUTH_EXPIRED, "not connected"
            )
        }
        try {
            val body = JSONObject()
                .put("query", query)
                .put("read_only", readOnly)
            val raw = callMgmt("POST", "/v1/projects/$projectRef/database/query", tok, body)
            if (raw.code in 200..299 || raw.code == 201) {
                BuilderOutcome.Ok(raw.body)
            } else {
                BuilderOutcome.Failure(BuilderHttp.classifyHttp(raw.code), "http ${raw.code}")
            }
        } catch (e: Exception) {
            CatcherHooks.networkError("supabase", "exception", "exception")
            BuilderOutcome.Failure(BuilderFailureKind.NETWORK, "network")
        }
    }

    /**
     * User-project data read — PostgREST, user-project base URL +
     * user token se (Flora anon key kabhi nahi). Table naam caller
     * deta hai; path traversal se bachne ke liye sirf alnum/_/-
     * wale naam maane jate hain.
     */
    suspend fun userProjectData(
        projectRef: String,
        userToken: String,
        table: String
    ): BuilderOutcome<String> = withContext(Dispatchers.IO) {
        if (!table.matches(Regex("^[A-Za-z0-9_\\-]{1,64}$"))) {
            return@withContext BuilderOutcome.Failure(
                BuilderFailureKind.BAD_REQUEST, "invalid table"
            )
        }
        try {
            val request = Request.Builder()
                .url(BuilderHosts.userProjectBaseUrl(projectRef) + "/rest/v1/$table?select=*")
                .addHeader("apikey", userToken)
                .addHeader("Authorization", "Bearer $userToken")
                .build()
            val response = http.newCall(request).execute()
            response.use { res ->
                if (res.code in 200..299) {
                    BuilderOutcome.Ok(res.body?.string() ?: "")
                } else {
                    BuilderOutcome.Failure(BuilderHttp.classifyHttp(res.code), "http ${res.code}")
                }
            }
        } catch (e: Exception) {
            CatcherHooks.networkError("supabase", "exception", "exception")
            BuilderOutcome.Failure(BuilderFailureKind.NETWORK, "network")
        }
    }

    /** PKCE code exchange (Management OAuth, public-client style). */
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
                .url(BuilderOAuth.SUPABASE_TOKEN_URL)
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
                            TokenResult(
                                access,
                                json.optString("refresh_token", ""),
                                json.optLong("expires_in", 0L)
                            )
                        )
                    }
                } else {
                    BuilderOutcome.Failure(BuilderHttp.classifyHttp(res.code), "http ${res.code}")
                }
            }
        } catch (e: Exception) {
            CatcherHooks.networkError("supabase", "exception", "exception")
            BuilderOutcome.Failure(BuilderFailureKind.NETWORK, "network")
        }
    }

    /** Silent refresh attempt — fail par caller RegrantOnce branch par jata hai. */
    suspend fun refreshSilently(clientId: String): BuilderOutcome<TokenResult> =
        withContext(Dispatchers.IO) {
            val refresh = try {
                prefs.builderRefreshToken(BuilderService.SUPABASE)
            } catch (e: Exception) {
                CatcherHooks.networkError("supabase", "exception", "exception")
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
                    .url(BuilderOAuth.SUPABASE_TOKEN_URL)
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
                                TokenResult(
                                    access,
                                    json.optString("refresh_token", refresh),
                                    json.optLong("expires_in", 0L)
                                )
                            )
                        }
                    } else {
                        BuilderOutcome.Failure(BuilderHttp.classifyHttp(res.code), "http ${res.code}")
                    }
                }
            } catch (e: Exception) {
                CatcherHooks.networkError("supabase", "exception", "exception")
                BuilderOutcome.Failure(BuilderFailureKind.NETWORK, "network")
            }
        }

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")
}
