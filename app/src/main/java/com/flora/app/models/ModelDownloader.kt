package com.flora.app.models

import android.content.Context
import com.flora.app.core.FloraConfig
import com.flora.app.core.PinnedTrust
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Flora model download system (v2.21, CEO plan M1).
 * Maya ke model-download BEHAVIOUR se seekha (manifest +
 * SHA check + .part + .ok marker + cache + bundled fallback),
 * code poora naya hai — Maya ka koi model/file kabhi nahi.
 *
 * - Bundled models phone me pehle se hain (app ke saath
 *   aate hain) — download nahi maangte, status READY.
 * - Remote models Flora ke apne server (Supabase Storage
 *   public bucket) se aate hain — SHA mismatch par kabhi
 *   use nahi hote, .part delete ho jaata hai.
 * - Manifest 10 minute cache hota hai; server na mile to
 *   bundled list hi dikhti hai (koi technical error nahi).
 */
data class FloraModel(
    val id: String,
    val name: String,
    val version: String,
    val sizeBytes: Long,
    val sha256: String,
    val url: String,
    val bundled: Boolean,
)

enum class ModelStatus { READY, NOT_DOWNLOADED, DOWNLOADING, SERVER_PENDING }

object ModelDownloader {

    /** Phone ke saath aane wale models — download nahi chahiye. */
    val BUNDLED_MODELS = listOf(
        FloraModel(
            id = "face-match",
            name = "Face Match (MobileFaceNet)",
            version = "1.0",
            sizeBytes = 5_233_396,
            sha256 = "",
            url = "",
            bundled = true,
        ),
        FloraModel(
            id = "wakeword-template",
            name = "Wake Word Template (v0)",
            version = "1.0",
            sizeBytes = 0,
            sha256 = "",
            url = "",
            bundled = true,
        ),
    )

    private val downloading = mutableSetOf<String>()

    private fun modelsDir(context: Context): File =
        File(context.filesDir, "models").apply { if (!exists()) mkdirs() }

    private fun modelDir(context: Context, id: String): File =
        File(modelsDir(context), id).apply { if (!exists()) mkdirs() }

    private fun okFile(context: Context, model: FloraModel): File =
        File(modelDir(context, model.id), ".ok")

    private fun dataFile(context: Context, model: FloraModel): File {
        val name = model.url.substringAfterLast("/").ifEmpty { model.id }
        return File(modelDir(context, model.id), name)
    }

    /** Download ho chuka model ka file (valid .ok ke saath) — na ho to null.
     *  VoiceMatcher isse bada encoder model load karta hai (APK me
     *  bundle karne ke bajay server se download — M1 system). */
    fun downloadedModelFile(context: Context, modelId: String, fileName: String): File? {
        val dir = File(modelsDir(context), modelId)
        val ok = File(dir, ".ok")
        val data = File(dir, fileName)
        return if (ok.exists() && data.exists()) data else null
    }

    fun status(context: Context, model: FloraModel): ModelStatus {        if (model.bundled) return ModelStatus.READY
        synchronized(downloading) {
            if (model.id in downloading) return ModelStatus.DOWNLOADING
        }
        val ok = okFile(context, model)
        if (!ok.exists()) return ModelStatus.NOT_DOWNLOADED
        return try {
            val saved = JSONObject(ok.readText())
            if (saved.optString("sha256") == model.sha256 &&
                saved.optString("version") == model.version &&
                dataFile(context, model).exists()
            ) ModelStatus.READY else ModelStatus.NOT_DOWNLOADED
        } catch (e: Exception) {
            ModelStatus.NOT_DOWNLOADED
        }
    }

    /** Manifest lao: remote (cache 10 min) → fail par bundled list. */
    suspend fun fetchManifest(context: Context, prefs: FloraPreferences): List<FloraModel> =
        withContext(Dispatchers.IO) {
            val cache = File(modelsDir(context), "manifest-cache.json")
            val now = System.currentTimeMillis()
            if (cache.exists() && now - cache.lastModified() < 10 * 60 * 1000) {
                parseManifest(cache.readText())?.let { return@withContext it }
            }
            val override = try { prefs.modelsManifestUrl() } catch (e: Exception) { "" }
            val url = override.ifEmpty { FloraConfig.MODELS_MANIFEST_URL }
            try {
                // G17 (Phase 8): manifest URL pin-checked
                // (Supabase host; doosre hosts par plain).
                val conn = PinnedTrust.openConnection(URL(url)) as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                if (conn.responseCode in 200..299) {
                    val text = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    val parsed = parseManifest(text)
                    if (parsed != null) {
                        cache.writeText(text)
                        return@withContext parsed
                    }
                } else {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                // Server na mile to bundled list — koi error nahi.
            }
            if (cache.exists()) {
                parseManifest(cache.readText())?.let { return@withContext it }
            }
            BUNDLED_MODELS
        }

    private fun parseManifest(text: String): List<FloraModel>? {
        return try {
            val arr = JSONObject(text).optJSONArray("models") ?: return null
            val out = mutableListOf<FloraModel>()
            for (i in 0 until arr.length()) {
                val m = arr.optJSONObject(i) ?: continue
                out += FloraModel(
                    id = m.optString("id"),
                    name = m.optString("name"),
                    version = m.optString("version"),
                    sizeBytes = m.optLong("sizeBytes"),
                    sha256 = m.optString("sha256"),
                    url = m.optString("url"),
                    bundled = m.optBoolean("bundled", false),
                )
            }
            val bundled = out.filter { it.bundled }
            // Bundled hamesha dikhe, chahe manifest me ho ya na ho.
            val merged = (BUNDLED_MODELS.filter { b -> bundled.none { it.id == b.id } } + out)
            if (merged.isEmpty()) null else merged
        } catch (e: Exception) {
            null
        }
    }

    /** Ek remote model download karo: .part (+Range resume) → SHA verify → .ok. */
    suspend fun download(context: Context, model: FloraModel): Boolean =
        withContext(Dispatchers.IO) {
            if (model.bundled || model.url.isEmpty()) return@withContext false
            synchronized(downloading) {
                if (model.id in downloading) return@withContext false
                downloading += model.id
            }
            try {
                val part = File(modelDir(context, model.id), "download.part")
                // REG-F9 residual (Part 1): resume — aadhi .part ho to
                // Range se wahi se jaari rakho; server 206 na de to
                // (200) shuru se. Digest me .part ke bytes bhi
                // shamil hain, taaki aakhri SHA poori file ka ho.
                val resumeFrom = if (part.exists()) part.length() else 0L
                // G17 (Phase 8): model URL pin-checked
                // (Supabase bucket host; doosre hosts par plain).
                val conn = PinnedTrust.openConnection(URL(model.url)) as HttpURLConnection
                conn.requestMethod = "GET"
                if (resumeFrom > 0) conn.setRequestProperty("Range", "bytes=$resumeFrom-")
                conn.connectTimeout = 20_000
                conn.readTimeout = 60_000
                if (conn.responseCode !in 200..299) {
                    conn.disconnect()
                    return@withContext false
                }
                val resumed = conn.responseCode == 206
                val digest = MessageDigest.getInstance("SHA-256")
                if (resumed) {
                    FileInputStream(part).use { existing ->
                        val buf = ByteArray(8192)
                        while (true) {
                            val n = existing.read(buf)
                            if (n < 0) break
                            digest.update(buf, 0, n)
                        }
                    }
                } else if (part.exists()) {
                    part.delete()
                }
                conn.inputStream.use { input ->
                    FileOutputStream(part, resumed).use { output ->
                        val buf = ByteArray(8192)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            digest.update(buf, 0, n)
                            output.write(buf, 0, n)
                        }
                    }
                }
                conn.disconnect()
                val hex = digest.digest().joinToString("") { "%02x".format(it) }
                if (model.sha256.isNotEmpty() && hex != model.sha256) {
                    part.delete()
                    return@withContext false
                }
                val dest = dataFile(context, model)
                if (dest.exists()) dest.delete()
                if (!part.renameTo(dest)) {
                    part.delete()
                    return@withContext false
                }
                okFile(context, model).writeText(
                    JSONObject().put("id", model.id)
                        .put("version", model.version)
                        .put("sha256", model.sha256).toString()
                )
                true
            } catch (e: Exception) {
                // Resume ke liye .part RAKHO — agla try wahi se
                // jaari rakhega. Galat SHA par upar delete hota
                // hi hai, isliye corrupt part kabhi ready nahi.
                false
            } finally {
                synchronized(downloading) { downloading -= model.id }
            }
        }
}
