package com.flora.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.flora.app.core.FloraConfig
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Phase 2 Item 3 — In-app update, server se (Maya jaisa, par manual).
 *
 * Raasta (sirf maujooda free infra):
 * - Check file: `web/updates/version.json` repo me hai, Vercel
 *   (flora-henna-alpha.vercel.app) use static serve karta hai —
 *   push karte hi naya version.json live ho jata hai, koi alag
 *   server ya paid hosting nahi.
 * - APK file: Supabase Storage public bucket (flora-models wali
 *   pattern) se aayegi — jab bucket + upload lagenge tab
 *   version.json me apkUrl + sha256 bharna hai (app badle bina
 *   update chalu ho jayega). Tab tak apkUrl khali hai aur app
 *   website ke download page par bhejega (koi jhootha download
 *   button nahi).
 * - GitHub releases is channel me NAHI hain: repo private hai,
 *   isliye release assets bina auth ke download nahi hote — app
 *   me token kabhi embed nahi hoga.
 * - Vercel se APK NAHI milega: free-plan 50 MB limit (download
 *   page par likha hai), APK ~100 MB ka hai.
 *
 * Suraksha / niyam (todna nahi):
 * - Koi auto-check, auto-download, auto-install NAHI — har kadam
 *   sirf user ke tap par (Settings > App update).
 * - Download hui file ka SHA-256 version.json wale SHA se milna
 *   chahiye (SHA khali ho to check skip — sirf tab jab apkUrl
 *   bhi khali na ho; SHA wala APK hi install hoga). Mismatch
 *   par file delete + install rok.
 * - Har failure fail-closed hai: null/message, kabhi crash nahi,
 *   kabhi chupchaap install nahi.
 * - Install ke liye Android ka system prompt hi aayega
 *   (REQUEST_INSTALL_PACKAGES + "is app se install" permission
 *   user khud dega). Flora sideload-only hai, Play par kabhi nahi.
 */
object AppUpdate {

    data class UpdateInfo(
        val versionCode: Long,
        val versionName: String,
        val apkUrl: String,
        val pageUrl: String,
        val notes: String,
        val sha256: String,
    )

    private const val TIMEOUT_MS = 15_000

    /** version.json lao; kuch bhi gadbad ho to null (fail-closed). */
    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(FloraConfig.UPDATE_VERSION_URL)
                .openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return@withContext null
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val json = JSONObject(text)
            UpdateInfo(
                versionCode = json.optLong("versionCode", -1),
                versionName = json.optString("versionName", ""),
                apkUrl = json.optString("apkUrl", ""),
                pageUrl = json.optString(
                    "pageUrl", FloraConfig.UPDATE_PAGE_URL
                ),
                notes = json.optString("notes", ""),
                sha256 = json.optString("sha256", ""),
            ).takeIf { it.versionCode > 0 }
        } catch (e: Exception) {
            null
        }
    }

    fun installedVersionCode(context: Context): Long {
        return try {
            val info = context.packageManager.getPackageInfo(
                context.packageName, 0
            )
            PackageInfoCompat.getLongVersionCode(info)
        } catch (e: Exception) {
            -1
        }
    }

    fun isNewer(info: UpdateInfo, installedCode: Long): Boolean =
        installedCode >= 0 && info.versionCode > installedCode

    /**
     * APK download karo (cacheDir/updates/). Progress callback
     * (done, total) — total -1 ho to size pata nahi. SHA khali na
     * ho to verify karo; mismatch par file delete karke null do.
     * Koi bhi failure par null, kabhi exception bahar nahi.
     */
    suspend fun downloadApk(
        context: Context,
        url: String,
        expectedSha256: String,
        onProgress: (done: Long, total: Long) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "updates")
                .apply { if (!exists()) mkdirs() }
            val part = File(dir, "flora-update.apk.part")
            val out = File(dir, "flora-update.apk")
            if (out.exists()) out.delete()
            if (part.exists()) part.delete()
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 30_000
            conn.readTimeout = 30_000
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return@withContext null
            }
            val total = conn.contentLengthLong
            var done = 0L
            conn.inputStream.use { input ->
                FileOutputStream(part).use { fos ->
                    val buf = ByteArray(32 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        fos.write(buf, 0, n)
                        done += n
                        try { onProgress(done, total) } catch (ignored: Exception) { }
                    }
                }
            }
            conn.disconnect()
            if (expectedSha256.isNotEmpty()) {
                val actual = sha256Hex(part)
                if (!actual.equals(expectedSha256, ignoreCase = true)) {
                    part.delete()
                    return@withContext null
                }
            }
            if (!part.renameTo(out)) {
                part.delete()
                return@withContext null
            }
            out
        } catch (e: Exception) {
            null
        }
    }

    fun canInstallUnknown(context: Context): Boolean {
        return try {
            context.packageManager.canRequestPackageInstalls()
        } catch (e: Exception) {
            false
        }
    }

    /** System ka "is app se install" settings page (user khud allow karega). */
    fun unknownSourcesIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).apply {
            // LocalContext Activity bhi ho sakta hai, par non-Activity
            // context se startActivity par NEW_TASK zaroori hai —
            // bina iske crash hota, isliye hamesha lagao.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    /** System install prompt wala intent (FileProvider URI se). */
    fun installIntent(context: Context, apkFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", apkFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun browserIntent(pageUrl: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(pageUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(32 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
