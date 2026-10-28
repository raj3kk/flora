package com.flora.app.voice

import android.content.Context
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * VoiceMatcher — Flora ka apna neural voice-match (CEO plan P1).
 * Validated Python protocol (wakeword/validate_voice_match.py,
 * 6 clips par PASS) ka Kotlin port — parameter/protocol wahi,
 * code naya. Mel matrix TF se generate ki gayi asset hai
 * (HTK formula kabhi nahi — usme max diff 1.0 tha).
 *
 * Pipeline: 16kHz PCM16 → frames 512/hop 160 → preemph 0.97
 * → periodic Hann 511 → rfft 1024 magnitude → TF mel 128 →
 * log(max(1, mel)) → repeat-first x3 + window 4 stride 3 →
 * VAD (528-d, exp(out)>0.1) → encoder chunks of 2 →
 * d-vector = aakhri chunk ki pehli row (256-d).
 *
 * Threshold 0.50 sirf STARTING GUESS hai (validation: same
 * speaker min 0.558, noise max 0.380) — doosre speaker ke
 * negatives + phone test ke bina lock nahi hoga.
 * Assets na hon to matcher UNAVAILABLE batata hai, kabhi
 * fake match nahi deta.
 */
object VoiceMatcher {

    data class MatchResult(
        val available: Boolean,
        val matched: Boolean,
        val score: Float,
        val enrolled: Boolean,
    )

    private const val ASSET_DIR = "voice_match"
    private const val MEL_ROWS = 513
    private const val MEL_COLS = 128

    fun isAvailable(context: Context): Boolean {
        val encoderOk = downloadedEncoderFile(context) != null ||
            assetExists(context, "conformer_tisid_medium.tflite")
        return encoderOk &&
            assetExists(context, "vad_long_model.tflite") &&
            assetExists(context, "mel_matrix_f32.bin")
    }

    private fun assetExists(context: Context, name: String): Boolean {
        return try {
            context.assets.open("$ASSET_DIR/$name").close()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Server se download hua encoder (M1) — na ho to null. */
    fun downloadedEncoderFile(context: Context): File? {
        return try {
            com.flora.app.models.ModelDownloader.downloadedModelFile(
                context, "voice-encoder", "conformer_tisid_medium.tflite"
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun loadModel(context: Context, name: String): Interpreter {
        val downloaded = if (name == "conformer_tisid_medium.tflite") {
            downloadedEncoderFile(context)
        } else {
            null
        }
        if (downloaded != null) {
            val input = FileInputStream(downloaded)
            val mapped = input.channel.map(FileChannel.MapMode.READ_ONLY, 0, downloaded.length())
            return Interpreter(mapped)
        }
        val fd = context.assets.openFd("$ASSET_DIR/$name")
        val input = FileInputStream(fd.fileDescriptor)
        val mapped = input.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        return Interpreter(mapped)
    }

    private fun loadMelMatrix(context: Context): FloatArray {
        val bytes = context.assets.open("$ASSET_DIR/mel_matrix_f32.bin").readBytes()
        val out = FloatArray(MEL_ROWS * MEL_COLS)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(out)
        return out
    }

    private fun loadVadStats(context: Context): Pair<FloatArray, FloatArray> {
        val text = context.assets.open("$ASSET_DIR/vad_long_mean_stddev.csv")
            .bufferedReader().readText().trim().split("\n")
        fun row(i: Int): FloatArray =
            text[i].trim().split(",").map { it.trim().toFloat() }.toFloatArray()
        return Pair(row(0), row(1))
    }

    /** Radix-2 FFT (1024) — magnitude spectrum nikaalne ke liye. */
    private fun fftMagnitude(frame: FloatArray, n: Int): FloatArray {
        val re = FloatArray(n)
        val im = FloatArray(n)
        for (i in frame.indices) re[i] = frame[i]
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wr = cos(ang).toFloat()
            val wi = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var wrk = 1.0f
                var wik = 0.0f
                for (k in 0 until len / 2) {
                    val ur = re[i + k]
                    val ui = im[i + k]
                    val vr = re[i + k + len / 2] * wrk - im[i + k + len / 2] * wik
                    val vi = re[i + k + len / 2] * wik + im[i + k + len / 2] * wrk
                    re[i + k] = ur + vr
                    im[i + k] = ui + vi
                    re[i + k + len / 2] = ur - vr
                    im[i + k + len / 2] = ui - vi
                    val nwr = wrk * wr - wik * wi
                    wik = wrk * wi + wik * wr
                    wrk = nwr
                }
                i += len
            }
            len = len shl 1
        }
        val out = FloatArray(n / 2 + 1)
        for (i in out.indices) out[i] = sqrt(re[i] * re[i] + im[i] * im[i])
        return out
    }

    /** PCM16 (16kHz mono) → stacked log-mel [T,512] (stride 3). */
    private fun frontend(pcm: ShortArray, melMat: FloatArray): Array<FloatArray> {
        val audio = FloatArray(pcm.size) { pcm[it].toFloat() }
        val frameLen = 512
        val hop = 160
        val frames = mutableListOf<FloatArray>()
        var start = 0
        while (start + frameLen <= audio.size) {
            frames.add(audio.copyOfRange(start, start + frameLen))
            start += hop
        }
        if (frames.isEmpty()) return emptyArray()
        // preemph: frames[:,1:] - 0.97*frames[:,:-1] → 511
        val win = FloatArray(511) { 0.5f - 0.5f * cos(2.0 * PI * it / 511).toFloat() }
        val logmel = Array(frames.size) { t ->
            val pre = FloatArray(511) { i ->
                (frames[t][i + 1] - 0.97f * frames[t][i]) * win[i]
            }
            val spec = fftMagnitude(pre, 1024) // 513
            FloatArray(MEL_COLS) { j ->
                var sum = 0.0f
                for (i in 0 until MEL_ROWS) sum += spec[i] * melMat[i * MEL_COLS + j]
                if (sum < 1.0f) 0.0f else ln(sum)
            }
        }
        // repeat-first x3, window 4, phir har teesri row
        val padded = Array(logmel.size + 3) { i ->
            if (i < 3) logmel[0] else logmel[i - 3]
        }
        val stacked = Array(logmel.size) { t ->
            FloatArray(512) { k ->
                padded[t + k / MEL_COLS][k % MEL_COLS]
            }
        }
        val out = mutableListOf<FloatArray>()
        var i = 0
        while (i < stacked.size) {
            out.add(stacked[i])
            i += 3
        }
        return out.toTypedArray()
    }

    /** VAD: speech rows (RAW stacked) wapas; koi speech na ho to empty. */
    private fun vadFilter(
        stacked: Array<FloatArray>,
        vad: Interpreter,
        mean: FloatArray,
        stddev: FloatArray,
    ): List<FloatArray> {
        val meanVec = FloatArray(512) { mean[it / 4] }
        val stdVec = FloatArray(512) { stddev[it / 4] }
        val outIdx = vad.getOutputTensor(0).index()
        val outShape = vad.getOutputTensor(0).shape()
        val outCols = if (outShape.size >= 2) outShape[1] else 1
        val kept = mutableListOf<FloatArray>()
        for (row in stacked) {
            val vin = FloatArray(528)
            for (k in 0 until 512) vin[k] = (row[k] - meanVec[k]) / stdVec[k]
            vin[512 + 2] = 1.0f // one-hot cluster 2
            val input = Array(1) { vin }
            val output = Array(1) { FloatArray(outCols) }
            val outputs = mutableMapOf<Int, Any>(outIdx to output)
            vad.runForMultipleInputsOutputs(arrayOf<Any>(input), outputs)
            val score = kotlin.math.exp(output[0][0].toDouble()).toFloat()
            if (score > 0.1f) kept.add(row)
        }
        return kept
    }

    /** Encoder: chunks of 2, states carry, d-vector = aakhri row. */
    private fun encode(
        feats: List<FloatArray>,
        enc: Interpreter,
    ): FloatArray? {
        if (feats.size < 2) return null
        val inCount = enc.inputTensorCount
        val outCount = enc.outputTensorCount
        val states = ArrayList<Any>()
        val stateShapes = ArrayList<IntArray>()
        for (i in 1 until inCount) {
            val shape = enc.getInputTensor(i).shape()
            stateShapes.add(shape)
            states.add(zeroArray(shape))
        }
        var last: FloatArray? = null
        var t = 0
        while (t + 2 <= feats.size) {
            val chunk = Array(2) { r -> feats[t + r] }
            val inputs = ArrayList<Any>()
            inputs.add(chunk)
            inputs.addAll(states)
            val outputs = mutableMapOf<Int, Any>()
            val outArrays = ArrayList<Any>()
            for (o in 0 until outCount) {
                val shape = enc.getOutputTensor(o).shape()
                val arr = zeroArray(shape)
                outArrays.add(arr)
                outputs[enc.getOutputTensor(o).index()] = arr
            }
            enc.runForMultipleInputsOutputs(inputs.toTypedArray(), outputs)
            val first = outArrays[0]
            last = when (first) {
                is Array<*> -> (first[0] as FloatArray).copyOf()
                is FloatArray -> first.copyOf()
                else -> null
            }
            // states = outputs[1:]
            states.clear()
            for (o in 1 until outCount) states.add(outArrays[o])
            t += 2
        }
        return last
    }

    private fun zeroArray(shape: IntArray): Any {
        // TFLite Java: multi-dim float arrays; -1 ko 1 maano.
        val dims = shape.map { if (it <= 0) 1 else it }
        fun build(depth: Int): Any {
            return if (depth == dims.size - 1) FloatArray(dims[depth])
            else Array(dims[depth]) { build(depth + 1) }
        }
        return if (dims.isEmpty()) FloatArray(0) else build(0)
    }

    /** Ek clip ka d-vector (null = unavailable / no speech). */
    suspend fun embed(context: Context, pcm: ShortArray): FloatArray? =
        withContext(Dispatchers.Default) {
            try {
                if (!isAvailable(context)) return@withContext null
                val melMat = loadMelMatrix(context)
                val stacked = frontend(pcm, melMat)
                if (stacked.isEmpty()) return@withContext null
                val (mean, stddev) = loadVadStats(context)
                val vad = loadModel(context, "vad_long_model.tflite")
                val speech = try {
                    vadFilter(stacked, vad, mean, stddev)
                } finally {
                    vad.close()
                }
                if (speech.isEmpty()) return@withContext null
                val enc = loadModel(context, "conformer_tisid_medium.tflite")
                try {
                    encode(speech, enc)
                } finally {
                    enc.close()
                }
            } catch (e: Exception) {
                null
            }
        }

    /** Kai clips se enrol: average d-vector, KeyStore me encrypted. */
    suspend fun enrol(context: Context, samples: List<ShortArray>): Boolean =
        withContext(Dispatchers.Default) {
            val vecs = mutableListOf<FloatArray>()
            for (s in samples) {
                embed(context, s)?.let { vecs.add(it) }
            }
            if (vecs.isEmpty()) return@withContext false
            val dim = vecs[0].size
            val avg = FloatArray(dim)
            for (v in vecs) for (i in 0 until dim) avg[i] += v[i] / vecs.size
            var norm = 0.0f
            for (x in avg) norm += x * x
            norm = sqrt(norm)
            if (norm > 0) for (i in avg.indices) avg[i] /= norm
            VoiceVault(context).store(avg)
        }

    /** Match: enrolled vector se cosine. Threshold pref (default 0.50 guess). */
    suspend fun match(context: Context, pcm: ShortArray, prefs: FloraPreferences): MatchResult =
        withContext(Dispatchers.Default) {
            if (!isAvailable(context)) {
                return@withContext MatchResult(available = false, matched = false, score = 0f, enrolled = false)
            }
            val enrolled = VoiceVault(context).load()
            if (enrolled == null) {
                return@withContext MatchResult(available = true, matched = false, score = 0f, enrolled = false)
            }
            val vec = embed(context, pcm)
                ?: return@withContext MatchResult(available = true, matched = false, score = 0f, enrolled = true)
            var dot = 0.0f
            var na = 0.0f
            var nb = 0.0f
            for (i in enrolled.indices) {
                dot += enrolled[i] * vec[i]
                na += enrolled[i] * enrolled[i]
                nb += vec[i] * vec[i]
            }
            val score = if (na > 0 && nb > 0) dot / (sqrt(na) * sqrt(nb)) else 0f
            val threshold = try { prefs.voiceMatchThreshold() } catch (e: Exception) { 0.50f }
            MatchResult(available = true, matched = score >= threshold, score = score, enrolled = true)
        }
}
