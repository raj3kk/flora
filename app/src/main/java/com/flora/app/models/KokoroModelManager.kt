package com.flora.app.models

import android.content.Context
import com.flora.app.core.FloraConfig
import com.flora.app.core.PinnedTrust
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Kokoro neural voice model manager (Phase 8, user-final voice:
 * Kokoro hf_alpha). Model APK me bundle NAHI hota — runtime me
 * ek baar public HuggingFace URL se download hota hai, uske
 * baad poori tarah offline (phone par hi rehta hai).
 *
 * Source (PUBLIC, user-final): hexgrad/Kokoro-82M v1.0
 *  Model:  https://huggingface.co/hexgrad/Kokoro-82M/resolve/main/kokoro-v1.0.onnx
 *          (325,532,387 bytes; SHA-256 neeche)
 *  Voices: https://huggingface.co/hexgrad/Kokoro-82M/resolve/main/voices-v1.0.bin
 *          (28,214,398 bytes; SHA-256 neeche)
 *  Total ~354 MB — isliye download Wi-Fi par karna behtar hai
 *  (UI me ye baat likhi hai). SHA verify ke bina file kabhi
 *  "ready" nahi mani jayegi; galat hash = download reject +
 *  .part delete, dobara try par phir se download.
 *
 * Storage: filesDir/models/kokoro/{kokoro-v1.0.onnx,
 * voices-v1.0.bin} + .ok marker (dono file verify hone par hi
 * likha jata hai). Ready = .ok + dono file maujood.
 *
 * HONEST LIMIT (is build me): download + readiness + VoiceEngine
 * routing poora hai, par native playback engine (sherpa-onnx
 * OfflineTts) is build me JUDA NAHI HAI — wajah docs me
 * (docs/FLORA_NEURAL_VOICE.md): koi verified Gradle AAR
 * coordinate nahi mila, NDK source build Flora CI (JDK-only)
 * me nahi chalega. Isliye model ready hone par bhi aaj
 * system TTS (Lily) hi bolega; engine judte hi NeuralSpeaker
 * ke ek hook se har jagah neural awaaz aa jayegi. Cloud TTS
 * par chupchaap kabhi nahi jaya jayega.
 */
object KokoroModelManager {

    const val VOICE_ID = "hf_alpha"

    @Volatile
    var downloading: Boolean = false
        private set

    private fun dir(context: Context): File =
        File(File(context.filesDir, "models"), "kokoro")

    fun modelFile(context: Context): File = File(dir(context), "kokoro-v1.0.onnx")

    fun voicesFile(context: Context): File = File(dir(context), "voices-v1.0.bin")

    private fun okFile(context: Context): File = File(dir(context), ".ok")

    fun isReady(context: Context): Boolean {
        return try {
            okFile(context).exists() &&
                modelFile(context).exists() && modelFile(context).length() > 0 &&
                voicesFile(context).exists() && voicesFile(context).length() > 0
        } catch (e: Exception) {
            false
        }
    }

    fun statusText(context: Context): String {
        return when {
            downloading -> "Neural voice model download ho raha hai…"
            isReady(context) -> "Neural voice model taiyaar hai (Kokoro hf_alpha)."
            else -> "Neural voice model abhi download nahi hua (~354 MB, Wi-Fi par karo)."
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Dono file download karo (3 attempts tak). Progress
     * callback ko (downloadedBytes, totalBytes) milta hai —
     * UI percent dikhata hai. Fail ho to false; .part delete
     * kar di jati hai taaki aadhi file kabhi ready na mane.
     * Resume is build me nahi hai (dobara try poori file
     * phir se download karega) — docs me limit ke roop me
     * likha hai.
     */
    suspend fun download(
        context: Context,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): Boolean {
        if (downloading) return false
        downloading = true
        try {
            return withContext(Dispatchers.IO) {
                val d = dir(context)
                if (!d.exists()) d.mkdirs()
                val total = FloraConfig.KOKORO_MODEL_SIZE + FloraConfig.KOKORO_VOICES_SIZE
                var done = 0L
                val jobs = listOf(
                    Triple(
                        FloraConfig.KOKORO_MODEL_URL, modelFile(context),
                        FloraConfig.KOKORO_MODEL_SHA256
                    ),
                    Triple(
                        FloraConfig.KOKORO_VOICES_URL, voicesFile(context),
                        FloraConfig.KOKORO_VOICES_SHA256
                    )
                )
                for ((urlText, target, wantSha) in jobs) {
                    var ok = false
                    for (attempt in 1..3) {
                        if (downloadOne(urlText, target) { chunk ->
                            done += chunk
                            onProgress(done, total)
                        } && sha256(target).equals(wantSha, ignoreCase = true)) {
                            ok = true
                            break
                        } else {
                            try { target.delete() } catch (e: Exception) { }
                            try { File(target.path + ".part").delete() } catch (e: Exception) { }
                        }
                    }
                    if (!ok) return@withContext false
                }
                try {
                    okFile(context).writeText(
                        JSONObject()
                            .put("model_sha", FloraConfig.KOKORO_MODEL_SHA256)
                            .put("voices_sha", FloraConfig.KOKORO_VOICES_SHA256)
                            .put("voice", VOICE_ID).toString()
                    )
                } catch (e: Exception) {
                    return@withContext false
                }
                true
            }
        } finally {
            downloading = false
        }
    }

    /**
     * Ek file .part me download karo. Progress callback ko
     * har 512 KB ke baad chunk bytes milte hain (UI ko har
     * 8 KB par update karna bekaar hota). HTTP 200 ke alawa
     * kuch mile to false.
     */
    private fun downloadOne(
        urlText: String, target: File, onChunk: (Long) -> Unit
    ): Boolean {
        val part = File(target.path + ".part")
        return try {
            val conn = PinnedTrust.openConnection(URL(urlText)) as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return false
            }
            var sinceReport = 0L
            conn.inputStream.use { input ->
                part.outputStream().use { out ->
                    val buf = ByteArray(8 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        sinceReport += n
                        if (sinceReport >= 512 * 1024) {
                            onChunk(sinceReport)
                            sinceReport = 0L
                        }
                    }
                    if (sinceReport > 0) onChunk(sinceReport)
                }
            }
            conn.disconnect()
            if (!part.renameTo(target)) {
                part.copyTo(target, overwrite = true)
                part.delete()
            }
            true
        } catch (e: Exception) {
            try { part.delete() } catch (ignored: Exception) { }
            false
        }
    }
}
