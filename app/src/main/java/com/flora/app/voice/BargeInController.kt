package com.flora.app.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.flora.app.data.FloraPreferences
import com.flora.app.data.LicenceGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * BargeInController (v2.22, chunk 7) — Flora bol rahi ho
 * tab user beech me bole to uski awaaz Deepgram live stream
 * par bhejo; FINAL transcript milte hi caller ko do (caller
 * TTS rok kar use naya command banayega). Mic permission
 * na ho, key khali ho ya mic kisi aur ke paas ho to start
 * false deta hai — chupchaap no-barge-in, kabhi crash nahi.
 * Audio kabhi save/upload ke alawa kahin nahi jata; zyada
 * se zyada 30 second ke baad apne aap ruk jata hai.
 * Cycle 1, APP-C1-002: Deepgram streaming start se pehle
 * wahi LicenceGate check lagta hai jo FloraRecognitionService
 * me hai — blocked / free-cap-khatm user ka Deepgram kharcha
 * nahi hoga. (FloraApp call site par bhi gate hai; ye
 * controller-level guard hai taaki koi bhi caller isse
 * bypass karke stream na khol sake.)
 */
class BargeInController(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null
    private var client: DeepgramStreamClient? = null

    @Volatile
    var running = false
        private set

    // Suspend isliye hai taaki LicenceGate (suspend) stream
    // start se pehle chal sake — ekmatra caller FloraApp
    // pehle se scope.launch ke andar se bulata hai.
    suspend fun start(key: String, onFinal: (String) -> Unit): Boolean {
        if (running || key.isEmpty()) return false
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        // APP-C1-002: FloraRecognitionService wala hi gate —
        // stream khulne se pehle. Blocked ho to false, koi
        // Deepgram call nahi, koi kharcha nahi.
        val gate = try {
            LicenceGate.check(FloraPreferences(context))
        } catch (e: Exception) {
            null
        }
        if (gate is LicenceGate.Result.Blocked) return false
        val sampleRate = 16000
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) return false
        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2
            )
        } catch (e: Exception) {
            return false
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            try { recorder.release() } catch (e: Exception) { }
            return false
        }
        val stream = DeepgramStreamClient()
        val ok = stream.start(key, object : DeepgramStreamClient.Listener {
            override fun onFinal(text: String) {
                onFinal(text)
            }

            override fun onError() {
                stop()
            }
        })
        if (!ok) {
            try { recorder.release() } catch (e: Exception) { }
            return false
        }
        client = stream
        running = true
        job = scope.launch {
            try {
                recorder.startRecording()
                val buf = ShortArray(1600)
                val deadline = System.currentTimeMillis() + 30_000L
                while (running && System.currentTimeMillis() < deadline) {
                    val n = recorder.read(buf, 0, buf.size)
                    if (n > 0) {
                        stream.sendPcm(buf.copyOf(n))
                    } else if (n < 0) {
                        break
                    }
                }
            } catch (e: Exception) {
            } finally {
                try { recorder.stop() } catch (e: Exception) { }
                try { recorder.release() } catch (e: Exception) { }
                stop()
            }
        }
        return true
    }

    fun stop() {
        if (!running && job == null && client == null) return
        running = false
        try { job?.cancel() } catch (e: Exception) { }
        job = null
        try { client?.stop() } catch (e: Exception) { }
        client = null
    }
}
