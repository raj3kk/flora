package com.flora.app.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * System speech recognizer (v2.23, Phase 5) — jab user system
 * me Flora ko recognizer chune, tab ye service Flora ki apni
 * Deepgram lane (nova-2, Hindi) se pehchanti hai. System STT
 * lanes (ContinuousListener, barge-in) chhede nahi — ye naya
 * path sirf tab chalta hai jab system is service ko bulaye.
 *
 * Imaandaar limits (chhupayi nahi):
 * - Hindi-only: Deepgram lane hi-IN par chalta hai; recognizer
 *   intent me koi aur language aaye to bhi Hindi hi pehchana
 *   jayega.
 * - One-shot: pehla FINAL transcript milte hi session khatam
 *   (onResults) — lagatar dictation nahi; interim results
 *   deliver nahi hote.
 * - Key chahiye: user ki apni Deepgram key na ho to
 *   ERROR_CLIENT (bina key ke chupchaap system STT par nahi
 *   gira — wo Flora ki apni lane nahi hoti).
 * - Audio sirf stream hota hai, kabhi save nahi hota
 *   (DeepgramStreamClient ka contract wahi hai).
 */
class FloraRecognitionService : RecognitionService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var sessionId = 0

    @Volatile
    private var delivered = false
    private var activeCallback: Callback? = null
    private var recorder: AudioRecord? = null
    private var stream: DeepgramStreamClient? = null
    private var readJob: Job? = null

    override fun onStartListening(intent: Intent?, callback: Callback?) {
        val cb = callback ?: return
        if (activeCallback != null) {
            cb.onError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cb.onError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
            return
        }
        sessionId++
        val mySession = sessionId
        activeCallback = cb
        delivered = false
        scope.launch {
            val key = try {
                FloraPreferences(applicationContext).deepgramApiKey()
            } catch (e: Exception) {
                ""
            }
            if (mySession != sessionId) return@launch
            if (key.isEmpty()) {
                failOnMain(SpeechRecognizer.ERROR_CLIENT)
                return@launch
            }
            val rec = createRecorder()
            if (mySession != sessionId) {
                if (rec != null) {
                    try {
                        rec.release()
                    } catch (e: Exception) {
                    }
                }
                return@launch
            }
            if (rec == null) {
                failOnMain(SpeechRecognizer.ERROR_AUDIO)
                return@launch
            }
            recorder = rec
            val client = DeepgramStreamClient()
            val ok = client.start(key, object : DeepgramStreamClient.Listener {
                override fun onFinal(text: String) {
                    deliverFinal(text)
                }

                override fun onError() {
                    if (!delivered) {
                        failOnMain(SpeechRecognizer.ERROR_NETWORK)
                    }
                }
            })
            if (mySession != sessionId) {
                try {
                    client.stop()
                } catch (e: Exception) {
                }
                releaseRecorder()
                return@launch
            }
            if (!ok) {
                releaseRecorder()
                failOnMain(SpeechRecognizer.ERROR_NETWORK)
                return@launch
            }
            stream = client
            mainHandler.post { activeCallback?.onReadyForSpeech(Bundle()) }
            try {
                rec.startRecording()
            } catch (e: Exception) {
                releaseRecorder()
                try {
                    client.stop()
                } catch (e: Exception) {
                }
                stream = null
                failOnMain(SpeechRecognizer.ERROR_AUDIO)
                return@launch
            }
            mainHandler.post { activeCallback?.onBeginningOfSpeech() }
            readJob = scope.launch {
                val buf = ShortArray(2048)
                while (true) {
                    val current = recorder ?: break
                    val read = try {
                        current.read(buf, 0, buf.size)
                    } catch (e: Exception) {
                        break
                    }
                    if (read <= 0) break
                    val currentStream = stream ?: break
                    currentStream.sendPcm(buf.copyOf(read))
                }
            }
        }
    }

    override fun onStopListening(callback: Callback?) {
        if (callback == null) return
        if (activeCallback == null) return
        if (delivered) {
            stopSession()
            return
        }
        mainHandler.post {
            val active = activeCallback ?: return@post
            if (delivered) return@post
            delivered = true
            active.onError(SpeechRecognizer.ERROR_NO_MATCH)
            stopSession()
        }
    }

    override fun onCancel(callback: Callback?) {
        stopSession()
    }

    override fun onDestroy() {
        stopSession()
        scope.cancel()
        super.onDestroy()
    }

    private fun deliverFinal(text: String) {
        if (text.isBlank()) return
        mainHandler.post {
            val cb = activeCallback ?: return@post
            if (delivered) return@post
            delivered = true
            val results = Bundle().apply {
                putStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION,
                    arrayListOf(text)
                )
            }
            cb.onResults(results)
            stopSession()
        }
    }

    private fun failOnMain(code: Int) {
        mainHandler.post {
            val cb = activeCallback ?: return@post
            if (delivered) return@post
            delivered = true
            cb.onError(code)
            stopSession()
        }
    }

    private fun stopSession() {
        sessionId++
        readJob?.cancel()
        readJob = null
        releaseRecorder()
        try {
            stream?.stop()
        } catch (e: Exception) {
        }
        stream = null
        activeCallback = null
    }

    private fun releaseRecorder() {
        val rec = recorder
        recorder = null
        try {
            rec?.stop()
        } catch (e: Exception) {
        }
        try {
            rec?.release()
        } catch (e: Exception) {
        }
    }

    private fun createRecorder(): AudioRecord? {
        val sampleRate = 16000
        val minBuf = try {
            AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
        } catch (e: Exception) {
            0
        }
        if (minBuf <= 0) return null
        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
        )
        for (source in sources) {
            try {
                val rec = AudioRecord(
                    source,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2
                )
                if (rec.state == AudioRecord.STATE_INITIALIZED) return rec
                try {
                    rec.release()
                } catch (e: Exception) {
                }
            } catch (e: Exception) {
            }
        }
        return null
    }
}
