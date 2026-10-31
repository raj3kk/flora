package com.flora.app.models

import android.content.Context
import com.flora.app.core.FloraConfig
import com.flora.app.core.PinnedTrust
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
 * ENGINE (Lane C, ORT route): sherpa-onnx route ab istemal
 * nahi hota — wajah docs me (docs/FLORA_NEURAL_VOICE.md).
 * Engine ab KokoroOrtEngine hai (onnxruntime-android, CPU-only,
 * phone par hi): model ready hote hi NeuralVoiceAttach usko
 * NeuralSpeaker.engine par set karta hai (app-start par async
 * + Settings download-success par re-attach). Attach fail ho
 * to engine null hi rehta hai aur system TTS (Lily) hi bolega
 * — awaaz kabhi nahi rukegi. Cloud TTS par chupchaap kabhi
 * nahi jaya jayega. (Is file me sirf ye comment badla hai;
 * download logic bilkul wahi hai.)
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

    /**
     * FIXER ISSUE-1: .part naam ab URL + expected SHA se banta hai
     * (hash, hex-only) — sirf fixed target filename se nahi.
     * URL ya SHA badle to purani .part naye download me append
     * NAHI hogi (alag naam = naya part; purana orphan, mix nahi).
     * SHA check: caller download() me strict equality hai —
     * khali expected SHA kabhi file hash se match nahi karta,
     * isliye Kokoro path me ISSUE-3 jaisa fail-open hole NAHI
     * hai (khali SHA par 3 attempts fail + koi .ok nahi) —
     * isliye yahan SHA logic chheda nahi gaya (Kokoro path clean).
     */
    private fun partFileFor(target: File, urlText: String, wantSha: String): File {
        val key = "$urlText|$wantSha"
        val hex = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }.take(16)
        return File(target.path + "-$hex.part")
    }

    fun isReady(context: Context): Boolean {
        // REG-F9 residual (Part 1): .ok-marker validation — sirf
        // existence nahi, .ok ke andar likhe SHA config ke SHA
        // se milne chahiye, warna stale .ok ready nahi manega.
        // (Poori 354 MB re-hash har check par nahi — SHA verify
        // download ke waqt hi hota hai, .ok tabhi likha jata hai.)
        return try {
            val ok = okFile(context)
            if (!ok.exists()) return false
            val saved = JSONObject(ok.readText())
            if (saved.optString("model_sha") != FloraConfig.KOKORO_MODEL_SHA256 ||
                saved.optString("voices_sha") != FloraConfig.KOKORO_VOICES_SHA256
            ) return false
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
     * UI percent dikhata hai. Fail ho to false; aadhi file
     * kabhi ready nahi mani jayegi (SHA verify + .ok tabhi).
     * REG-F9 residual (Part 1): resume — aadhi .part ho to
     * agla attempt Range se wahi se jaari rakhta hai (server
     * 206 na de to shuru se); SHA galat nikle to hi .part
     * delete hoti hai.
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
                        if (downloadOne(urlText, target, wantSha) { chunk ->
                            done += chunk
                            onProgress(done, total)
                        } && sha256(target).equals(wantSha, ignoreCase = true)) {
                            ok = true
                            break
                        } else {
                            try { target.delete() } catch (e: Exception) { }
                            try { partFileFor(target, urlText, wantSha).delete() } catch (e: Exception) { }
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
     * 8 KB par update karna bekaar hota). HTTP 200/206 ke
     * alawa kuch mile to false. Resume: aadhi .part ho to
     * Range header se wahi se jaari; server 206 na de to
     * shuru se. Fail par .part rakhi jati hai (agla attempt
     * resume karega); SHA check caller (download) karta hai.
     */
    private fun downloadOne(
        urlText: String, target: File, wantSha: String, onChunk: (Long) -> Unit
    ): Boolean {
        val part = partFileFor(target, urlText, wantSha)
        var resumeFrom = if (part.exists()) part.length() else 0L
        return try {
            var conn = PinnedTrust.openConnection(URL(urlText)) as HttpURLConnection
            conn.requestMethod = "GET"
            if (resumeFrom > 0) conn.setRequestProperty("Range", "bytes=$resumeFrom-")
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            // FIXER ISSUE-2: 416 par .part delete karke EK BAAR
            // fresh (bina Range) retry — bounded; dobara 416
            // aaye to neeche false hi rahega, loop kabhi nahi.
            if (conn.responseCode == 416 && resumeFrom > 0) {
                conn.disconnect()
                part.delete()
                resumeFrom = 0L
                conn = PinnedTrust.openConnection(URL(urlText)) as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 20_000
                conn.readTimeout = 60_000
            }
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return false
            }
            val resumed = conn.responseCode == 206
            var sinceReport = 0L
            conn.inputStream.use { input ->
                FileOutputStream(part, resumed).use { out ->
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
            // .part rakho — agla attempt resume karega.
            false
        }
    }
}
