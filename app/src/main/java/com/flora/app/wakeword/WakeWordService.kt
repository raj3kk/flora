package com.flora.app.wakeword

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.flora.app.MainActivity
import com.flora.app.security.BiometricTemplates
import com.flora.app.voice.MicOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Hey Flora wake-word listener — v2.4 v0, default OFF.
 *
 * - Foreground service (mic type, visible notification) — chhupke
 *   sunna kabhi nahi: notification hamesha dikhegi, Settings se
 *   OFF karte hi service ruk jayegi.
 * - Har 2 second ki window ka MFCC mean nikalta hai, enrolled
 *   template se cosine nikalta hai. Threshold 0.60 sirf shuruaati
 *   guess hai (Python prototype: positives 0.732-0.896, silence/
 *   noise 0.232-0.314) — phone par asli speech se tune hona baaki
 *   hai, isliye v0 me false trigger ho sakta hai.
 * - Wake par sirf TTS "Haan, boliye" bolta hai, phir 10 second
 *   cooldown — koi action, koi unlock, koi posting nahi karta.
 * - Template sirf encrypted on-device hai (BiometricTemplates),
 *   audio sample kabhi save nahi hota, kabhi server nahi jata.
 * - Apna CoroutineScope hai (lifecycleScope wali galti dobara
 *   nahi — v1.3 lesson).
 */
class WakeWordService : Service() {
    companion object {
        const val WAKE_THRESHOLD = 0.60
        private const val CHANNEL_ID = "flora_wake"
        private const val NOTIF_ID = 1001
    }

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var listenJob: Job? = null
    private var tts: TextToSpeech? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID, "Flora Wake Word", NotificationManager.IMPORTANCE_LOW
        )
        manager?.createNotificationChannel(channel)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Flora sun raha hai")
            .setContentText("Hey Flora bolne par jawab dunga. Band karne ke liye Settings me Wake Word OFF karo.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)
        if (listenJob?.isActive != true) {
            listenJob = scope.launch { listenLoop() }
        }
        return START_STICKY
    }

    private suspend fun listenLoop() {
        val templates = BiometricTemplates(this@WakeWordService)
        val stored = templates.loadWake() ?: return
        if (stored.isEmpty()) return
        val template = DoubleArray(stored.size) { stored[it].toDouble() }
        var lastWakeMs = 0L
        while (scope.isActive) {
            // Mic-owner rule (v2.15): continuous mic ON ho to mic
            // BackgroundMicService ka hai — wake loop yahan ruk
            // kar wait karega, AudioRecord chheenega nahi.
            if (MicOwner.backgroundActive) {
                delay(1000)
                continue
            }
            if (ContextCompat.checkSelfPermission(
                    this@WakeWordService, android.Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val audio = WakeWordRecorder.recordMillis(2000) ?: continue
            val mean = WakeWordMFCC.mfccMean(audio) ?: continue
            val score = WakeWordMFCC.cosine(mean, template)
            if (score >= WAKE_THRESHOLD) {
                val now = System.currentTimeMillis()
                if (now - lastWakeMs > 10_000) {
                    lastWakeMs = now
                    tts?.speak("Haan, boliye", TextToSpeech.QUEUE_FLUSH, null, "wake")
                    // Background baat (v2.14): app ko aage lao taaki
                    // continuous mic se baat shuru ho jaye. MainActivity
                    // "flora_wake" dekh kar isi session ke liye sunna
                    // shuru karega (2s baad, greeting ke baad).
                    try {
                        val launch = Intent(
                            this@WakeWordService,
                            MainActivity::class.java,
                        ).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                            )
                            putExtra("flora_wake", true)
                        }
                        startActivity(launch)
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        listenJob?.cancel()
        scope.cancel()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
