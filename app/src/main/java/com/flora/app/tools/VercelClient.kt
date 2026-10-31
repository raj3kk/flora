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
 * B3-01 Vercel client (manual constructor DI: VercelClient(prefs)).
 *
 * Host SIRF api.vercel.com (B3-09). Vercel ka auth model
 * scope-string wala nahi hai — dashboard se bana Full Access
 * token (account/team scope) hi credential hai; isliye yahan koi
 * scope string invent nahi kiya gaya aur koi authorize endpoint
 * nahi hai (guided-login = dashboard token guide + manual /
 * clipboard raasta). Token ka koi refresh token nahi hota —
 * refreshSilently() hamesha NotRefreshable deta hai; expire/
 * revoke par plan ka RepasteOnce exception lagta hai (R-B3-4).
 *
 * Endpoints (Vercel REST API):
 * - GET /v2/user — who-am-I (side-effect nahi).
 * - POST /v9/projects {name} — project create.
 * - POST /v13/deployments {name, files:[{file, data}]} —
 *   deployment create (text files; bade/binary files ke liye
 *   /v2/files upload alag step hai, is client me nahi — limit
 *   screen par likhi hai, chhupayi nahi).
 * brain ask call / brain-proxy kabhi call nahi hota (R-B3-8).
 */
class VercelClient(
    private val prefs: FloraPreferences,
    private val http: OkHttpClient = BuilderOkHttp.newClient()
) {

    data class Project(val id: String, val name: String)
    data class Deployment(val id: String, val url: String)

    private data class Raw(val code: Int, val body: String)

    private fun call(method: String, path: String, token: String, json: JSONObject?): Raw {
        val builder = Request.Builder()
            .url(BuilderHosts.vercelApiBase() + path)
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
        prefs.builderToken(BuilderService.VERCEL)
    } catch (e: Exception) {
        CatcherHooks.networkError("vercel", "exception", "exception")
        ""
    }

    suspend fun whoAmI(): BuilderOutcome<String> = withContext(Dispatchers.IO) {
        val tok = token()
        if (tok.isEmpty()) {
            return@withContext BuilderOutcome.Failure(
                BuilderFailureKind.AUTH_EXPIRED, "not connected"
            )
        }
        try {
            val raw = call("GET", "/v2/user", tok, null)
            if (raw.code in 200..299) {
                val user = JSONObject(raw.body).optJSONObject("user")
                val name = user?.optString("username", "")
                    ?: JSONObject(raw.body).optString("username", "")
                if (name.isEmpty()) {
                    BuilderOutcome.Failure(BuilderFailureKind.SERVER, "empty user")
                } else {
                    BuilderOutcome.Ok(name)
                }
            } else {
                BuilderOutcome.Failure(BuilderHttp.classifyHttp(raw.code), "http ${raw.code}")
            }
        } catch (e: Exception) {
            CatcherHooks.networkError("vercel", "exception", "exception")
            BuilderOutcome.Failure(BuilderFailureKind.NETWORK, "network")
        }
    }

    suspend fun createProject(name: String): BuilderOutcome<Project> =
        withContext(Dispatchers.IO) {
            val tok = token()
            if (tok.isEmpty()) {
                return@withContext BuilderOutcome.Failure(
                    BuilderFailureKind.AUTH_EXPIRED, "not connected"
                )
            }
            try {
                val raw = call("POST", "/v9/projects", tok, JSONObject().put("name", name))
                if (raw.code in 200..299) {
                    val json = JSONObject(raw.body)
                    BuilderOutcome.Ok(
                        Project(json.optString("id", ""), json.optString("name", name))
                    )
                } else {
                    BuilderOutcome.Failure(BuilderHttp.classifyHttp(raw.code), "http ${raw.code}")
                }
            } catch (e: Exception) {
                CatcherHooks.networkError("vercel", "exception", "exception")
                BuilderOutcome.Failure(BuilderFailureKind.NETWORK, "network")
            }
        }

    /**
     * Deployment — files: path -> text content. Bade/binary
     * files is raaste se nahi jate (Vercel /v2/files upload
     * alag step hai); caller text-only raasta hi use kare.
     */
    suspend fun createDeployment(
        projectName: String,
        files: Map<String, String>
    ): BuilderOutcome<Deployment> = withContext(Dispatchers.IO) {
        val tok = token()
        if (tok.isEmpty()) {
            return@withContext BuilderOutcome.Failure(
                BuilderFailureKind.AUTH_EXPIRED, "not connected"
            )
        }
        try {
            val fileArray = JSONArray()
            for ((path, content) in files) {
                fileArray.put(JSONObject().put("file", path).put("data", content))
            }
            val body = JSONObject()
                .put("name", projectName)
                .put("files", fileArray)
            val raw = call("POST", "/v13/deployments", tok, body)
            if (raw.code in 200..299) {
                val json = JSONObject(raw.body)
                BuilderOutcome.Ok(
                    Deployment(json.optString("id", ""), json.optString("url", ""))
                )
            } else {
                BuilderOutcome.Failure(BuilderHttp.classifyHttp(raw.code), "http ${raw.code}")
            }
        } catch (e: Exception) {
            CatcherHooks.networkError("vercel", "exception", "exception")
            BuilderOutcome.Failure(BuilderFailureKind.NETWORK, "network")
        }
    }

    /** Vercel token ka refresh concept hi nahi hai — hamesha yehi jawab. */
    fun refreshSilently(): BuilderOutcome<Unit> =
        BuilderOutcome.Failure(
            BuilderFailureKind.AUTH_EXPIRED,
            "vercel tokens have no refresh token; re-paste is the genuine-block path"
        )
}
