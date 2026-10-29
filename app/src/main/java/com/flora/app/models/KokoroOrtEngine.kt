package com.flora.app.models

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Looper
import android.util.Log
import com.flora.app.assistant.NeuralEngine
import com.flora.app.assistant.NeuralSpeaker
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipInputStream

/**
 * Kokoro-82M neural voice engine — RAW ONNX Runtime route
 * (Lane C). Sherpa-onnx route is NOT used: no verified
 * Gradle AAR exists and NDK source build does not run in
 * Flora CI (JDK-only). This engine uses onnxruntime-android
 * (Maven Central, MIT) directly, CPU-only.
 *
 * Verified base (do NOT change without re-verification):
 *  - Model: hexgrad/Kokoro-82M kokoro-v1.0.onnx, opset 17.
 *  - Voice: hf_alpha, voices-v1.0.bin NPZ/ZIP, entry
 *    "hf_alpha.npy", 128-byte .npy header skip, float32 LE,
 *    510 rows x 256 cols (payload 510*256*4 = 522,240 bytes).
 *  - input_ids: int64, wrapped [[0, *tokens, 0]].
 *  - style: float32 [1, 256], row = UNWRAPPED token count.
 *  - speed: float32 [1], value 1.0.
 *  - Output: waveform, 24 kHz mono PCM (float32 [-1, 1]).
 *  - Model files are RUNTIME DOWNLOADS ONLY — never bundled
 *    in the APK (see KokoroModelManager).
 *
 * P1-1 (CRITICAL): input/output tensor NAMES are never used
 * blind. At session creation the real names are dumped to
 * log (Logcat) and matched to the three input roles + the
 * waveform output role (exact match preferred, substring
 * fallback). If any role cannot be resolved to a DISTINCT
 * real name, construction THROWS — the attach helper catches
 * it, NeuralSpeaker.engine stays null, and system TTS (Lily)
 * keeps speaking. A silent wrong-tensor run is never allowed.
 *
 * Threading: session creation + every inference + every
 * AudioTrack write runs on this engine's single worker
 * thread, NEVER the main thread. create() itself throws if
 * called on the main thread (programmatic guard).
 *
 * G2P seam: this file does NOT name Worker 1's class. It
 * takes a plain `tokenize: (String) -> List<Int>` function.
 * ASSUMED HindiG2P API (coordinator merges both lanes):
 *   object HindiG2P { fun tokenize(text: String): List<Int> }
 * returning UNWRAPPED Kokoro vocab indices (no BOS/EOS;
 * wrapping happens here). If Worker 1's real API differs,
 * only the one call site in NeuralVoiceAttach.kt changes.
 *
 * Contract (NeuralEngine): speak() is async; a new speak()
 * interrupts the previous utterance (QUEUE_FLUSH semantics);
 * onStart fires exactly once when audio actually starts,
 * onDone fires exactly once per speak() call — on full
 * completion, on stop, or on interrupt (VoiceEngine ignores
 * stale-seq onDone, so an interrupted utterance's onDone is
 * safe). stop() breaks the playback loop immediately.
 * This engine NEVER throws out of speak()/stop() — any
 * failure ends with a single onDone and the caller falls
 * back to system TTS (voice never stops).
 */
class KokoroOrtEngine private constructor(
    private val session: OrtSession,
    private val voiceRows: FloatArray,
    private val tokenize: (String) -> List<Int>,
    private val inputIdsName: String,
    private val styleName: String,
    private val speedName: String,
    private val outputName: String,
) : NeuralEngine {

    companion object {
        private const val TAG = "KokoroOrt"

        const val SAMPLE_RATE = 24000
        const val MAX_TOKENS = 509
        const val STYLE_DIM = 256
        const val VOICE_ROWS = 510
        const val VOICE_PAYLOAD_BYTES = VOICE_ROWS * STYLE_DIM * 4 // 522,240
        const val NPY_HEADER_BYTES = 128

        /**
         * Build a ready engine. MUST be called off the main
         * thread (attach helper runs on Dispatchers.IO).
         * Throws on ANY failure (missing files, bad NPZ,
         * session failure, I/O role mismatch) — caller must
         * treat throw as "no neural engine" (fail-closed).
         */
        fun create(
            modelPath: String,
            voicesPath: String,
            tokenize: (String) -> List<Int>,
        ): KokoroOrtEngine {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                throw IllegalStateException(
                    "KokoroOrtEngine.create on main thread — forbidden"
                )
            }
            val voiceRows = parseVoices(File(voicesPath))
            val env = OrtEnvironment.getEnvironment()
            // CPU-ONLY SessionOptions: no NNAPI, no GPU delegate
            // is ever added here (NNAPI on old devices risks
            // silent wrong output / crashes).
            val options = OrtSession.SessionOptions()
            val session: OrtSession
            try {
                session = env.createSession(modelPath, options)
            } catch (e: Exception) {
                try { options.close() } catch (ignored: Exception) { }
                throw IllegalStateException("ORT session create failed", e)
            }
            try {
                // P1-1: dump REAL names, then match roles.
                val inNames = session.inputNames
                val outNames = session.outputNames
                Log.i(TAG, "ORT inputs=$inNames outputs=$outNames")
                val inputIds = resolveRole(inNames, "input_ids", "input")
                    ?: throw IllegalStateException(
                        "ORT input role not found in $inNames"
                    )
                val style = resolveRole(inNames, "style", "style")
                    ?: throw IllegalStateException(
                        "ORT style role not found in $inNames"
                    )
                val speed = resolveRole(inNames, "speed", "speed")
                    ?: throw IllegalStateException(
                        "ORT speed role not found in $inNames"
                    )
                val output = resolveRole(outNames, "waveform", "waveform")
                    ?: outNames.singleOrNull()
                    ?: outNames.firstOrNull()
                    ?: throw IllegalStateException(
                        "ORT output not found in $outNames"
                    )
                val distinct = setOf(inputIds, style, speed)
                if (distinct.size != 3) {
                    throw IllegalStateException(
                        "ORT input roles not distinct: " +
                            "$inputIds/$style/$speed in $inNames"
                    )
                }
                Log.i(
                    TAG,
                    "ORT roles: input_ids=$inputIds style=$style " +
                        "speed=$speed output=$output"
                )
                return KokoroOrtEngine(
                    session, voiceRows, tokenize,
                    inputIds, style, speed, output
                )
            } catch (e: Exception) {
                try { session.close() } catch (ignored: Exception) { }
                try { options.close() } catch (ignored: Exception) { }
                throw e
            }
        }

        private fun resolveRole(
            names: Set<String>,
            exact: String,
            contains: String,
        ): String? {
            names.firstOrNull { it.equals(exact, ignoreCase = true) }?.let {
                return it
            }
            return names.firstOrNull { it.contains(contains, ignoreCase = true) }
        }

        /**
         * NPZ/ZIP se hf_alpha.npy nikaalo. Header 128 bytes
         * skip, payload EXACT 522,240 bytes float32 LE hona
         * chahiye (kam ho to corrupt — throw). Entry ke baad
         * extra trailing bytes ignore hote hain.
         */
        fun parseVoices(voicesFile: File): FloatArray {
            ZipInputStream(FileInputStream(voicesFile)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    if (name != "hf_alpha.npy" &&
                        !name.endsWith("/hf_alpha.npy")
                    ) {
                        continue
                    }
                    val raw = zip.readBytes()
                    if (raw.size < NPY_HEADER_BYTES + VOICE_PAYLOAD_BYTES) {
                        throw IllegalStateException(
                            "hf_alpha.npy too small: ${raw.size} bytes"
                        )
                    }
                    // Minimal .npy magic sanity (full dict parse
                    // nahi — verified base ke mutabiq header
                    // hamesha 128 bytes hota hai).
                    if (raw[0] != 0x93.toByte() ||
                        raw[1] != 'N'.code.toByte() ||
                        raw[2] != 'U'.code.toByte() ||
                        raw[3] != 'M'.code.toByte() ||
                        raw[4] != 'P'.code.toByte() ||
                        raw[5] != 'Y'.code.toByte()
                    ) {
                        throw IllegalStateException(
                            "hf_alpha.npy missing NUMPY magic"
                        )
                    }
                    val payload = raw.copyOfRange(
                        NPY_HEADER_BYTES,
                        NPY_HEADER_BYTES + VOICE_PAYLOAD_BYTES
                    )
                    val floats = FloatArray(VOICE_ROWS * STYLE_DIM)
                    ByteBuffer.wrap(payload)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .asFloatBuffer()
                        .get(floats)
                    return floats
                }
            }
            throw IllegalStateException("hf_alpha.npy not found in voices file")
        }
    }

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "kokoro-ort").apply { isDaemon = true }
    }
    private val generation = AtomicInteger(0)

    @Volatile
    private var currentTrack: AudioTrack? = null

    override fun speak(text: String, onStart: () -> Unit, onDone: () -> Unit) {
        val gen = generation.incrementAndGet()
        try {
            executor.execute { runUtterance(gen, text, onStart, onDone) }
        } catch (e: Exception) {
            // Executor dead (app tearing down): caller ko
            // latkao mat — ek onDone de do, system fallback
            // aage sambhal lega.
            try { onDone() } catch (ignored: Exception) { }
        }
    }

    override fun stop() {
        // Generation bump: chal rahi utterance ka loop agle
        // chunk-check par toot jayega; track.stop() blocking
        // write ko bhi chhod deta hai.
        generation.incrementAndGet()
        try {
            currentTrack?.stop()
        } catch (e: Exception) {
        }
    }

    /** App-lifetime singleton hai — close sirf tests ke liye. */
    fun close() {
        generation.incrementAndGet()
        try { currentTrack?.stop() } catch (e: Exception) { }
        try { currentTrack?.release() } catch (e: Exception) { }
        currentTrack = null
        executor.shutdownNow()
        try { session.close() } catch (e: Exception) { }
    }

    private fun runUtterance(
        gen: Int,
        text: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
    ) {
        val doneFired = AtomicBoolean(false)
        fun fireDoneOnce() {
            if (doneFired.compareAndSet(false, true)) {
                try { onDone() } catch (ignored: Exception) { }
            }
        }
        var track: AudioTrack? = null
        try {
            // Caller (VoiceEngine) pehle hi sanitize karta hai;
            // dobara sanitize idempotent hai (emoji/URL kabhi
            // G2P tak nahi pahunchenge).
            val clean = NeuralSpeaker.sanitize(text)
            if (clean.isEmpty()) {
                fireDoneOnce()
                return
            }
            val tokens = try {
                tokenize(clean)
            } catch (e: Exception) {
                Log.w(TAG, "G2P failed", e)
                fireDoneOnce()
                return
            }
            if (tokens.isEmpty()) {
                fireDoneOnce()
                return
            }
            // Token-chunking (honest v1 limit): 509 se lambe
            // text ko token-boundary par todo; har chunk ka
            // apna style row = us chunk ka token count (Kokoro
            // semantics: ref_s per call). Chunk boundary par
            // prosody reset ho sakta hai — lambi replies ke
            // liye sentence-split Phase 10 phone test ke baad.
            val chunks = tokens.chunked(MAX_TOKENS)
            val started = AtomicBoolean(false)
            for (chunk in chunks) {
                if (generation.get() != gen) break
                val waveform = infer(chunk) ?: break
                if (waveform.isEmpty()) continue
                if (track == null) {
                    track = createTrack() ?: break
                    currentTrack = track
                    try { track.play() } catch (e: Exception) {
                        Log.w(TAG, "AudioTrack.play failed", e)
                        break
                    }
                }
                // Chunk-wise PCM16 write (24 kHz mono).
                var offset = 0
                val pcm = ShortArray(2048)
                while (offset < waveform.size) {
                    if (generation.get() != gen) break
                    val n = minOf(pcm.size, waveform.size - offset)
                    for (i in 0 until n) {
                        val v = waveform[offset + i].coerceIn(-1f, 1f)
                        pcm[i] = (v * 32767f).toInt().toShort()
                    }
                    val written = try {
                        track.write(pcm, 0, n)
                    } catch (e: Exception) {
                        Log.w(TAG, "AudioTrack.write failed", e)
                        break
                    }
                    if (written < 0) break
                    if (!started.getAndSet(true)) {
                        try { onStart() } catch (ignored: Exception) { }
                    }
                    offset += n
                }
                if (generation.get() != gen) break
            }
        } catch (e: Exception) {
            Log.w(TAG, "Kokoro utterance failed", e)
        } finally {
            try { track?.stop() } catch (ignored: Exception) { }
            try { track?.release() } catch (ignored: Exception) { }
            if (currentTrack === track) currentTrack = null
            // Interrupt/stop par bhi EXACTLY-ONCE onDone:
            // VoiceEngine stale seq ko ignore karta hai, isliye
            // ye safe hai aur caller kabhi latakta nahi.
            fireDoneOnce()
        }
    }

    private fun infer(tokens: List<Int>): FloatArray? {
        if (tokens.isEmpty() || tokens.size > MAX_TOKENS) return null
        val ids = LongArray(tokens.size + 2)
        ids[0] = 0L
        for (i in tokens.indices) ids[i + 1] = tokens[i].toLong()
        ids[ids.size - 1] = 0L
        // Style row = FINAL (chunk) token count — wrap se
        // pehle wali unwrapped length, Kokoro semantics.
        val styleRow = FloatArray(STYLE_DIM)
        val src = tokens.size * STYLE_DIM
        voiceRows.copyInto(styleRow, 0, src, src + STYLE_DIM)
        val env = OrtEnvironment.getEnvironment()
        // NOTE: OnnxTensor / Result are AutoCloseable, NOT
        // Closeable — Kotlin .use does NOT apply to them, so
        // closes are explicit try/finally (compile safety, no
        // local JDK/Gradle here to prove it otherwise).
        var idsTensor: OnnxTensor? = null
        var styleTensor: OnnxTensor? = null
        var speedTensor: OnnxTensor? = null
        var result: OrtSession.Result? = null
        try {
            idsTensor = OnnxTensor.createTensor(
                env, LongBuffer.wrap(ids), longArrayOf(1, ids.size.toLong())
            )
            styleTensor = OnnxTensor.createTensor(
                env, FloatBuffer.wrap(styleRow), longArrayOf(1, STYLE_DIM.toLong())
            )
            speedTensor = OnnxTensor.createTensor(
                env, FloatBuffer.wrap(floatArrayOf(1.0f)), longArrayOf(1)
            )
            val inputs = mapOf(
                inputIdsName to idsTensor,
                styleName to styleTensor,
                speedName to speedTensor
            )
            result = session.run(inputs, setOf(outputName))
            val value = (result[outputName] as? OnnxTensor)?.value ?: return null
            return flattenWaveform(value)
        } catch (e: Exception) {
            Log.w(TAG, "ORT inference failed", e)
            return null
        } finally {
            try { result?.close() } catch (ignored: Exception) { }
            try { speedTensor?.close() } catch (ignored: Exception) { }
            try { styleTensor?.close() } catch (ignored: Exception) { }
            try { idsTensor?.close() } catch (ignored: Exception) { }
        }
    }

    private fun flattenWaveform(value: Any): FloatArray? {
        return when (value) {
            is FloatArray -> value
            is Array<*> -> {
                // Shape [1, T] -> pehli row; [T] ke roop me
                // aaye to wahi; deeper nesting fail-closed.
                val first = value.firstOrNull() ?: return null
                when (first) {
                    is FloatArray -> first
                    is Number -> FloatArray(value.size) { i ->
                        (value[i] as Number).toFloat()
                    }
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun createTrack(): AudioTrack? {
        return try {
            val minBytes = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferBytes = maxOf(minBytes, SAMPLE_RATE * 2 / 2)
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "AudioTrack create failed", e)
            null
        }
    }
}
