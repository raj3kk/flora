package com.flora.app.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * SD-P1 Batch 2 (SD-1 app clients, Lane A) — Telegram bot, Todoist,
 * Linear, seedha app se. Drive IS FILE ME NAHI HAI: Drive item 14
 * Drive OAuth (user-gated) ka intezar kar raha hai — uske bina
 * Drive ka koi client/UI is batch me nahi banega.
 *
 * Har client sirf token string leta hai; token ka storage
 * FloraPreferences ke KeyStore-encrypted secret slots me hai
 * (Deepgram pattern) — ye file token kabhi log nahi karti, kabhi
 * screen/chat par poora token nahi dikhati, aur har call ke baad
 * caller ko sirf success/fail + chhota sa reason deta hai.
 * Har action se pehle UI par user ka button-tap hi consent hai;
 * har action ka natija caller Auto Log me likhta hai.
 *
 * Fail-soft: not-connected / API down / galat token par ye
 * clients sirf SdFailure wapas dete hain — app local mode me
 * chalti rehti hai, chhupke se fail nahi hota.
 */
sealed class SdResult<out T> {
    data class Ok<T>(val value: T) : SdResult<T>()
    data class Failure(val reason: String) : SdResult<Nothing>()
}

private fun sdRaw(
    http: OkHttpClient,
    request: Request,
): Pair<Int, String> {
    val response = http.newCall(request).execute()
    return response.use { res -> res.code to (res.body?.string() ?: "") }
}

private fun sdReason(code: Int, body: String): String {
    if (code == 401 || code == 403) return "token galat ya revoke ho gaya hai — dobara connect karo"
    if (code == 429) return "server ne rate limit lagayi hai — thodi der baad dobara koshish karo"
    if (code == 0) return "network nahi mila"
    if (code >= 500) return "server error ($code) — baad me dobara koshish karo"
    val msg = try {
        JSONObject(body).optString("description", "")
            .ifEmpty { JSONObject(body).optString("error", "") }
    } catch (e: Exception) {
        ""
    }
    return if (msg.isNotEmpty()) msg.take(120) else "request fail ho gaya (HTTP $code)"
}

/** Telegram Bot API — user ka apna bot token (secure paste se). */
object TelegramClient {
    data class Me(val username: String)

    suspend fun getMe(token: String, http: OkHttpClient = BuilderOkHttp.newClient()): SdResult<Me> =
        withContext(Dispatchers.IO) {
            if (token.isBlank()) return@withContext SdResult.Failure("Telegram connected nahi hai")
            try {
                val req = Request.Builder()
                    .url("https://api.telegram.org/bot$token/getMe")
                    .get().build()
                val (code, body) = sdRaw(http, req)
                if (code !in 200..299) return@withContext SdResult.Failure(sdReason(code, body))
                val result = JSONObject(body).optJSONObject("result") ?: return@withContext SdResult.Failure("Telegram se jawab samajh nahi aaya")
                SdResult.Ok(Me(result.optString("username", "")))
            } catch (e: Exception) {
                SdResult.Failure("network nahi mila ya jawab samajh nahi aaya")
            }
        }

    suspend fun sendMessage(
        token: String,
        chatId: String,
        text: String,
        http: OkHttpClient = BuilderOkHttp.newClient(),
    ): SdResult<Unit> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext SdResult.Failure("Telegram connected nahi hai")
        if (chatId.isBlank()) return@withContext SdResult.Failure("chat id khaali hai")
        if (text.isBlank()) return@withContext SdResult.Failure("message khaali hai")
        try {
            val json = JSONObject().put("chat_id", chatId).put("text", text)
            val req = Request.Builder()
                .url("https://api.telegram.org/bot$token/sendMessage")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val (code, body) = sdRaw(http, req)
            if (code !in 200..299) SdResult.Failure(sdReason(code, body)) else SdResult.Ok(Unit)
        } catch (e: Exception) {
            SdResult.Failure("network nahi mila ya jawab samajh nahi aaya")
        }
    }
}

/** Todoist REST API v2 — task create/list/complete. */
object TodoistClient {
    data class TodoTask(val id: String, val content: String, val due: String)

    private fun authReq(token: String, url: String): Request.Builder =
        Request.Builder().url(url).addHeader("Authorization", "Bearer $token")

    suspend fun listTasks(token: String, http: OkHttpClient = BuilderOkHttp.newClient()): SdResult<List<TodoTask>> =
        withContext(Dispatchers.IO) {
            if (token.isBlank()) return@withContext SdResult.Failure("Todoist connected nahi hai")
            try {
                val (code, body) = sdRaw(http, authReq(token, "https://api.todoist.com/rest/v2/tasks").get().build())
                if (code !in 200..299) return@withContext SdResult.Failure(sdReason(code, body))
                val out = mutableListOf<TodoTask>()
                val arr = JSONArray(body)
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val due = o.optJSONObject("due")?.optString("date", "") ?: ""
                    out.add(TodoTask(o.optString("id", ""), o.optString("content", ""), due))
                }
                SdResult.Ok(out)
            } catch (e: Exception) {
                SdResult.Failure("network nahi mila ya jawab samajh nahi aaya")
            }
        }

    suspend fun createTask(
        token: String,
        content: String,
        http: OkHttpClient = BuilderOkHttp.newClient(),
    ): SdResult<TodoTask> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext SdResult.Failure("Todoist connected nahi hai")
        if (content.isBlank()) return@withContext SdResult.Failure("task khaali hai")
        try {
            val json = JSONObject().put("content", content)
            val (code, body) = sdRaw(
                http,
                authReq(token, "https://api.todoist.com/rest/v2/tasks")
                    .post(json.toString().toRequestBody("application/json".toMediaType())).build(),
            )
            if (code !in 200..299) return@withContext SdResult.Failure(sdReason(code, body))
            val o = JSONObject(body)
            SdResult.Ok(TodoTask(o.optString("id", ""), o.optString("content", ""), ""))
        } catch (e: Exception) {
            SdResult.Failure("network nahi mila ya jawab samajh nahi aaya")
        }
    }

    suspend fun completeTask(
        token: String,
        taskId: String,
        http: OkHttpClient = BuilderOkHttp.newClient(),
    ): SdResult<Unit> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext SdResult.Failure("Todoist connected nahi hai")
        if (taskId.isBlank()) return@withContext SdResult.Failure("task id khaali hai")
        try {
            val (code, body) = sdRaw(
                http,
                authReq(token, "https://api.todoist.com/rest/v2/tasks/$taskId/close")
                    .post("".toRequestBody("application/json".toMediaType())).build(),
            )
            if (code !in 200..299) SdResult.Failure(sdReason(code, body)) else SdResult.Ok(Unit)
        } catch (e: Exception) {
            SdResult.Failure("network nahi mila ya jawab samajh nahi aaya")
        }
    }
}

/** Linear GraphQL API — issue list/create. */
object LinearClient {
    data class Issue(val id: String, val title: String)

    private suspend fun graphQl(
        token: String,
        query: String,
        http: OkHttpClient,
    ): SdResult<JSONObject> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext SdResult.Failure("Linear connected nahi hai")
        try {
            val json = JSONObject().put("query", query)
            val req = Request.Builder()
                .url("https://api.linear.app/graphql")
                .addHeader("Authorization", token)
                .addHeader("Content-Type", "application/json")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val (code, body) = sdRaw(http, req)
            if (code !in 200..299) return@withContext SdResult.Failure(sdReason(code, body))
            val root = JSONObject(body)
            val errors = root.optJSONArray("errors")
            if (errors != null && errors.length() > 0) {
                return@withContext SdResult.Failure(
                    errors.optJSONObject(0)?.optString("message", "Linear error")?.take(120)
                        ?: "Linear error",
                )
            }
            SdResult.Ok(root.optJSONObject("data") ?: JSONObject())
        } catch (e: Exception) {
            SdResult.Failure("network nahi mila ya jawab samajh nahi aaya")
        }
    }

    suspend fun listIssues(token: String, http: OkHttpClient = BuilderOkHttp.newClient()): SdResult<List<Issue>> {
        val res = graphQl(token, "{ issues(first: 20) { nodes { id title } } }", http)
        return when (res) {
            is SdResult.Failure -> res
            is SdResult.Ok -> {
                val out = mutableListOf<Issue>()
                val nodes = res.value.optJSONObject("issues")?.optJSONArray("nodes") ?: JSONArray()
                for (i in 0 until nodes.length()) {
                    val o = nodes.optJSONObject(i) ?: continue
                    out.add(Issue(o.optString("id", ""), o.optString("title", "")))
                }
                SdResult.Ok(out)
            }
        }
    }

    suspend fun createIssue(
        token: String,
        teamId: String,
        title: String,
        http: OkHttpClient = BuilderOkHttp.newClient(),
    ): SdResult<Issue> {
        if (teamId.isBlank()) return SdResult.Failure("team id khaali hai — Linear me team id daalo")
        if (title.isBlank()) return SdResult.Failure("issue title khaali hai")
        val safeTeam = teamId.replace("\\", "").replace("\"", "")
        val safeTitle = title.replace("\\", "").replace("\"", "")
        val query = "mutation { issueCreate(input: { teamId: \"$safeTeam\", title: \"$safeTitle\" }) " +
            "{ success issue { id title } } }"
        val res = graphQl(token, query, http)
        return when (res) {
            is SdResult.Failure -> res
            is SdResult.Ok -> {
                val created = res.value.optJSONObject("issueCreate")
                if (created == null || !created.optBoolean("success", false)) {
                    SdResult.Failure("Linear ne issue banane se mana kar diya")
                } else {
                    val issue = created.optJSONObject("issue") ?: JSONObject()
                    SdResult.Ok(Issue(issue.optString("id", ""), issue.optString("title", "")))
                }
            }
        }
    }
}
