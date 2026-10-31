package com.flora.app.wakeword
import com.flora.app.data.CatcherHooks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.flora.app.MainActivity
import com.flora.app.assistant.NeuralSpeaker
import com.flora.app.data.FloraPreferences
import com.flora.app.security.BiometricTemplates
import com.flora.app.voice.MicOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Hey Flora wake-word listener — v2.4 v0, default OFF.
 *
 * - Foreground service (mic type, visible notification) — chhupke
 *   sunna kabhi nahi: notification hamesha dikhegi, Settings se
 *   OFF karte hi service ruk jayegi.
 * - Har 2 second ki window ka MFCC mean nikalta hai, enrolled
 *   template se cosine nikalta hai. Threshold default 0.60
 *   (Python prototype: positives 0.732-0.896, silence/
 *   noise 0.232-0.314) — v2.22 se user Settings se sensitivity
 *   badal sakta hai (Kam 0.70 / Medium 0.60 / Zyada 0.50);
 *   phone par asli speech se tune hona abhi bhi baaki hai,
 *   isliye v0 me false trigger ho sakta hai.
 * - Wake par sirf NeuralSpeaker se "Haan, boliye" bolta hai, phir 10 second
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID, "Flora Wake Word", NotificationManager.IMPORTANCE_LOW
        )
        manager?.createNotificationChannel(channel)
        // Cycle 2, Q-NEU-001: wake ack ka apna system-TTS field
        // hata diya gaya — ack ab NeuralSpeaker.speakFixed se
        // bolta hai (engine ho to neural, warna system fallback
        // waisa hi; fallback ka QUEUE_FLUSH behaviour barkaraar).
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
        val stored = templates.loadWake()
        // Default template (v2.22, Phase 3): enrolled template
        // na ho to assets/wakeword/default_template.json se
        // 6-clip mean vector lo — wake word enrol kiye bina
        // bhi kaam karega. Enrolled template hamesha pehle.
        // Dono na hon to service chupchaap kuch nahi karti.
        val template: DoubleArray = when {
            stored != null && stored.isNotEmpty() ->
                DoubleArray(stored.size) { stored[it].toDouble() }
            else -> loadDefaultTemplate() ?: return
        }
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
            val threshold = try {
                FloraPreferences(this@WakeWordService).wakeThreshold().toDouble()
            } catch (e: Exception) {
                CatcherHooks.laneError("wakeword", "exception")
                WAKE_THRESHOLD
            }
            if (score >= threshold) {
                val now = System.currentTimeMillis()
                if (now - lastWakeMs > 10_000) {
                    lastWakeMs = now
                    // Q-NEU-001: NeuralSpeaker.speakFixed — engine
                    // ho to neural ack, warna system TTS fallback
                    // (usme QUEUE_FLUSH hai, isliye wake ack
                    // interrupt karta hai, pehle jaisa hi).
                    NeuralSpeaker.speakFixed(this@WakeWordService, "Haan, boliye")
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
                        CatcherHooks.laneError("wakeword", "exception")
                    }
                }
            }
        }
    }

    /** Default template asset padho: {"vector": [12 floats]}.
     *  Galat/missing asset par null — service kuch nahi karegi. */
    private fun loadDefaultTemplate(): DoubleArray? {
        return try {
            val text = assets.open("wakeword/default_template.json")
                .bufferedReader().use { it.readText() }
            val vecObj = org.json.JSONObject(text).optJSONArray("vector") ?: return null
            if (vecObj.length() != 12) return null
            DoubleArray(12) { vecObj.getDouble(it) }
        } catch (e: Exception) {
            CatcherHooks.laneError("wakeword", "exception")
            null
        }
    }

    // Task swipe ke baad restart ki koshish (v2.23, Phase 5): service
    // chal rahi hai to Wake Word user ne ON kiya hua hai (BootReceiver
    // sirf ON par start karta hai, Settings OFF par stop karta hai) —
    // isliye wapas lane ki koshish karo. Android 12+ / Vivo par ye
    // block ho sakta hai (OS niyam) — try/catch hai, fail ho to
    // crash nahi; tab battery-unrestricted + auto-start hi bachaav
    // hai (guidance Settings/Permissions screen par hai).
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            startForegroundService(Intent(this, WakeWordService::class.java))
        } catch (e: Exception) {
            CatcherHooks.laneError("wakeword", "exception")
        }
    }

    override fun onDestroy() {
        listenJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}
