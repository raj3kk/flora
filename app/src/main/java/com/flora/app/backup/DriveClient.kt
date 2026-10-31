package com.flora.app.backup
import com.flora.app.data.CatcherHooks

import com.flora.app.data.FloraPreferences
import com.flora.app.tools.BuilderOAuth
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import org.json.JSONArray
import org.json.JSONObject

/**
 * B6 Drive destination (R-B6-4, R-B6-5) — Drive REST v3, maujooda
 * OkHttp par. KOI naya Drive SDK dependency nahi (play-services-
 * drive deprecated hai hi). Scope SIRF drive.file (least
 * privilege): user ko dikhne wali backup files + folder choice.
 * Koi hidden app-data folder nahi — "Flora Backups" folder user
 * ke Drive root me dikhta hai, wahi success proof hai.
 *
 * OAuth pattern B3 jaisa hai: dedicated WebView + redirect-URL-
 * only code capture (BuilderOAuth PKCE/state mechanics; Drive
 * ke apne authorize/token endpoints + drive.file scope — GitHub/
 * Vercel/Supabase scopes kabhi nahi). Token FloraPreferences ke
 * KeyStore-encrypted secret pattern me rehta hai, kabhi
 * plaintext/log me nahi. Upload: direct multipart + 3-attempt
 * retry + progress callbacks.
 *
 * Live grant Phase 10 pending hai (user ka Google OAuth-app
 * registration chahiye — client ID/redirect URI user daalta
 * hai); bina asli grant ke live-test pass ka daava nahi.
 */
class DriveClient(private val prefs: FloraPreferences) {

    companion object {
        const val AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        const val DRIVE_API = "https://www.googleapis.com/drive/v3"
        const val DRIVE_UPLOAD_API = "https://www.googleapis.com/upload/drive/v3"
        const val SCOPE = "https://www.googleapis.com/auth/drive.file"
        const val DEFAULT_FOLDER_NAME = "Flora Backups"
        private const val MAX_ATTEMPTS = 3
    }

    data class DriveFile(
        val id: String,
        val name: String,
        val createdTimeMs: Long,
        val size: Long,
    )

    data class AuthorizeStart(
        val url: String,
        val verifier: String,
        val state: String,
    )

    private fun client(longTimeout: Boolean): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
            redactHeader("Authorization")
            redactHeader("Cookie")
        }
        val builder = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
        if (longTimeout) {
            builder.readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
        } else {
            builder.readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
        }
        return builder.build()
    }

    /** Google authorize URL banata hai (PKCE S256 + random state). */
    fun authorizeStart(clientId: String, redirectUri: String): AuthorizeStart {
        val pkce = BuilderOAuth.newPkcePair()
        val state = BuilderOAuth.newState()
        val url = BuilderOAuth.buildAuthorizeUrl(
            AUTHORIZE_URL, clientId, redirectUri, SCOPE, pkce.challenge, state
        ) + "&access_type=offline&prompt=consent"
        return AuthorizeStart(url, pkce.verifier, state)
    }

    private fun formBody(params: Map<String, String>): RequestBody {
        val encoded = params.entries.joinToString("&") { (key, value) ->
            URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
        }
        return RequestBody.create(
            "application/x-www-form-urlencoded".toMediaType(), encoded
        )
    }

    /** Code -> token exchange; safal par token prefs me save hote hain. */
    suspend fun exchangeCode(
        code: String,
        verifier: String,
        clientId: String,
        redirectUri: String,
    ): Boolean {
        return try {
            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(
                    formBody(
                        mapOf(
                            "code" to code,
                            "client_id" to clientId,
                            // Koi client_secret nahi bheja jaata: ye flow
                            // PKCE-only hai (Desktop-type OAuth app —
                            // UI guide me yahi type likha hai), isliye
                            // secret collect/store kabhi nahi hota.
                            "redirect_uri" to redirectUri,
                            "grant_type" to "authorization_code",
                            "code_verifier" to verifier,
                        )
                    )
                )
                .build()
            val response = client(false).newCall(request).execute()
            val body = response.body?.string() ?: ""
            response.close()
            if (!response.isSuccessful) return false
            val json = JSONObject(body)
            val access = json.optString("access_token", "")
            if (access.isEmpty()) return false
            val refresh = json.optString("refresh_token", "")
            val expiresIn = json.optLong("expires_in", 3600L)
            prefs.setDriveTokens(access, refresh, System.currentTimeMillis() + expiresIn * 1000L)
        } catch (e: Exception) {
            CatcherHooks.networkError("drive", "exception", "exception")
            false
        }
    }

    /** Expired access token ko refresh token se badalta hai. */
    suspend fun refreshAccessToken(clientId: String): Boolean {
        return try {
            val refresh = prefs.driveRefreshToken()
            if (refresh.isEmpty()) return false
            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(
                    formBody(
                        mapOf(
                            "refresh_token" to refresh,
                            "client_id" to clientId,
                            "grant_type" to "refresh_token",
                        )
                    )
                )
                .build()
            val response = client(false).newCall(request).execute()
            val body = response.body?.string() ?: ""
            response.close()
            if (!response.isSuccessful) return false
            val json = JSONObject(body)
            val access = json.optString("access_token", "")
            if (access.isEmpty()) return false
            val nextRefresh = json.optString("refresh_token", refresh)
            val expiresIn = json.optLong("expires_in", 3600L)
            prefs.setDriveTokens(access, nextRefresh, System.currentTimeMillis() + expiresIn * 1000L)
        } catch (e: Exception) {
            CatcherHooks.networkError("drive", "exception", "exception")
            false
        }
    }

    /** Valid access token (zaroorat par silent refresh ke saath). */
    suspend fun validAccessToken(): String? {
        val expiresAt = prefs.driveExpiresAtMs()
        val current = prefs.driveAccessToken()
        if (current.isNotEmpty() && expiresAt > System.currentTimeMillis() + 60_000L) {
            return current
        }
        val clientId = prefs.driveClientId()
        if (clientId.isEmpty()) return null
        if (!refreshAccessToken(clientId)) return null
        val refreshed = prefs.driveAccessToken()
        return refreshed.ifEmpty { "" }.takeIf { it.isNotEmpty() }
    }

    suspend fun isConnected(): Boolean = validAccessToken() != null

    private fun driveRequest(url: String, token: String): Request.Builder =
        Request.Builder().url(url).addHeader("Authorization", "Bearer $token")

    private fun parseIsoTime(value: String): Long {
        return try {
            java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US
            ).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.parse(value)?.time ?: 0L
        } catch (e: Exception) {
            CatcherHooks.networkError("drive", "exception", "exception")
            0L
        }
    }

    /** "Flora Backups" folder dhoondhta hai, na mile to banata hai. */
    suspend fun ensureFolder(): String? {
        val token = validAccessToken() ?: return null
        // A3 M1-3: cached ID par bina check ke bharosa nahi —
        // user Drive se folder delete kar de to stale ID milta
        // tha. Pehle files.get se existence re-check karo; na
        // mile/trashed ho to neeche find-or-create + cache update.
        // "Drive hatao" manual reset waisa hi barkaraar hai.
        val stored = prefs.driveFolderId()
        if (stored.isNotEmpty()) {
            val alive = try {
                val check = driveRequest(
                    "$DRIVE_API/files/$stored?fields=id,trashed", token
                ).build()
                val resp = client(false).newCall(check).execute()
                val body = resp.body?.string() ?: ""
                val ok = resp.isSuccessful &&
                    JSONObject(body).optString("id", "").isNotEmpty() &&
                    !JSONObject(body).optBoolean("trashed", false)
                resp.close()
                ok
            } catch (e: Exception) {
                CatcherHooks.networkError("drive", "exception", "exception")
                false
            }
            if (alive) return stored
        }
        return try {
            val query = URLEncoder.encode(
                "name = '$DEFAULT_FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false",
                "UTF-8"
            )
            val request = driveRequest(
                "$DRIVE_API/files?q=$query&fields=files(id,name)", token
            ).build()
            val response = client(false).newCall(request).execute()
            val body = response.body?.string() ?: ""
            response.close()
            if (response.isSuccessful) {
                val files = JSONObject(body).optJSONArray("files") ?: JSONArray()
                if (files.length() > 0) {
                    val id = files.getJSONObject(0).optString("id", "")
                    if (id.isNotEmpty()) {
                        prefs.setDriveFolderId(id)
                        return id
                    }
                }
            }
            val meta = JSONObject()
                .put("name", DEFAULT_FOLDER_NAME)
                .put("mimeType", "application/vnd.google-apps.folder")
            val create = driveRequest("$DRIVE_API/files", token)
                .post(RequestBody.create("application/json".toMediaType(), meta.toString()))
                .build()
            val created = client(false).newCall(create).execute()
            val createdBody = created.body?.string() ?: ""
            created.close()
            if (!created.isSuccessful) return null
            val id = JSONObject(createdBody).optString("id", "")
            if (id.isEmpty()) return null
            prefs.setDriveFolderId(id)
            id
        } catch (e: Exception) {
            CatcherHooks.networkError("drive", "exception", "exception")
            null
        }
    }

    private class CountingBody(
        private val delegate: RequestBody,
        private val onProgress: (sent: Long, total: Long) -> Unit,
    ) : RequestBody() {
        override fun contentType() = delegate.contentType()
        override fun contentLength() = delegate.contentLength()
        override fun writeTo(sink: BufferedSink) {
            val total = contentLength()
            var sent = 0L
            val forwarding: Sink = object : ForwardingSink(sink) {
                override fun write(source: Buffer, byteCount: Long) {
                    super.write(source, byteCount)
                    sent += byteCount
                    onProgress(sent, total)
                }
            }
            delegate.writeTo(forwarding.buffer())
            forwarding.flush()
        }
    }

    /**
     * Upload — 3-attempt retry, progress callbacks (0..100 nahi,
     * bytes bheje gaye; UI percent banata hai). Safal par Drive
     * file id milta hai (success proof); fail par null.
     */
    suspend fun upload(
        fileName: String,
        bytes: ByteArray,
        folderId: String,
        onProgress: (sent: Long, total: Long) -> Unit,
    ): String? {
        val meta = JSONObject()
            .put("name", fileName)
            .put("parents", JSONArray().put(folderId))
        val mediaType = "application/octet-stream".toMediaType()
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val token = validAccessToken() ?: return null
                val multipart = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "metadata", null,
                        RequestBody.create("application/json".toMediaType(), meta.toString())
                    )
                    .addFormDataPart(
                        "file", fileName,
                        RequestBody.create(mediaType, bytes)
                    )
                    .build()
                val counting = CountingBody(multipart) { sent, total -> onProgress(sent, total) }
                val request = driveRequest(
                    "$DRIVE_UPLOAD_API/files?uploadType=multipart", token
                ).post(counting).build()
                val response = client(true).newCall(request).execute()
                val body = response.body?.string() ?: ""
                response.close()
                if (response.isSuccessful) {
                    val id = JSONObject(body).optString("id", "")
                    if (id.isNotEmpty()) return id
                } else if (attempt == MAX_ATTEMPTS - 1) {
                    return null
                }
            } catch (e: IOException) {
                CatcherHooks.networkError("drive", "exception", "exception")
                if (attempt == MAX_ATTEMPTS - 1) return null
            } catch (e: Exception) {
                CatcherHooks.networkError("drive", "exception", "exception")
                return null
            }
        }
        return null
    }

    /** Folder me .flora-backup files, nayi se purani (date-ordered pick). */
    suspend fun listBackups(folderId: String): List<DriveFile> {
        val token = validAccessToken() ?: return emptyList()
        return try {
            val query = URLEncoder.encode(
                "'$folderId' in parents and trashed = false", "UTF-8"
            )
            val request = driveRequest(
                "$DRIVE_API/files?q=$query&orderBy=createdTime desc&pageSize=100" +
                    "&fields=files(id,name,createdTime,size)",
                token
            ).build()
            val response = client(false).newCall(request).execute()
            val body = response.body?.string() ?: ""
            response.close()
            if (!response.isSuccessful) return emptyList()
            val files = JSONObject(body).optJSONArray("files") ?: JSONArray()
            val out = mutableListOf<DriveFile>()
            for (i in 0 until files.length()) {
                val file = files.getJSONObject(i)
                val name = file.optString("name", "")
                if (!FloraBackupCrypto.isBackupFileName(name)) continue
                out.add(
                    DriveFile(
                        id = file.optString("id", ""),
                        name = name,
                        createdTimeMs = parseIsoTime(file.optString("createdTime", "")),
                        size = file.optLong("size", 0L),
                    )
                )
            }
            out.filter { it.id.isNotEmpty() }
        } catch (e: Exception) {
            CatcherHooks.networkError("drive", "exception", "exception")
            emptyList()
        }
    }

    /** Download — progress ke saath poora blob bytes me. */
    suspend fun download(
        fileId: String,
        onProgress: (received: Long, total: Long) -> Unit,
    ): ByteArray? {
        val token = validAccessToken() ?: return null
        return try {
            val request = driveRequest("$DRIVE_API/files/$fileId?alt=media", token).build()
            val response = client(true).newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return null
            }
            val body = response.body ?: run {
                response.close()
                return null
            }
            val total = body.contentLength()
            val source = body.source()
            val out = okio.Buffer()
            var received = 0L
            while (true) {
                val read = source.read(out, 8192L)
                if (read == -1L) break
                received += read
                onProgress(received, total)
            }
            response.close()
            out.readByteArray()
        } catch (e: Exception) {
            CatcherHooks.networkError("drive", "exception", "exception")
            null
        }
    }

    /** Drive listing me file maujood hai ya nahi (upload success proof). */
    suspend fun existsInListing(folderId: String, fileId: String): Boolean =
        listBackups(folderId).any { it.id == fileId }
}
