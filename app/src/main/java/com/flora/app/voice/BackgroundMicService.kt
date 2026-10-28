package com.flora.app.voice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.flora.app.MainActivity
import com.flora.app.assistant.BrainClient
import com.flora.app.assistant.VoiceEngine
import com.flora.app.data.FloraPreferences
import com.flora.app.data.LicenceClient
import com.flora.app.tools.ImageTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Mic ka malik (v2.15): jab continuous mic ON ho (foreground
 * ya background), tab mic isi service ke paas hota hai — Wake
 * Word service tab apna AudioRecord loop rok deta hai, taaki
 * dono ek saath mic na chheenin.
 */
object MicOwner {
    @Volatile
    var backgroundActive: Boolean = false
}

/** Background se bola gaya command — app aage aate hi Chat
 * pipeline isi se chalega (confirmation wahi hogi). */
object FloraCommandBus {
    @Volatile
    var pending: String? = null
}

/**
 * Background continuous mic (v2.15) — user ki seedhi shikayat
 * ka fix: "app ko back karte hi sunna band ho jata hai".
 *
 * - Foreground service (mic type, hamesha notification) — chhupke
 *   sunna kabhi nahi: notification me saaf likha hota hai ki
 *   Flora background me sun raha hai, Mute se band hoga.
 * - Mic ON hote hi service start hoti hai (foreground me safe),
 *   app foreground me ho to STANDBY (activity khud sunti hai),
 *   app back ho to TAKEOVER (service sunti + jawab deti hai).
 * - Baat-cheet ka pipeline (brain + TTS + talk-count) service
 *   me hi chalta hai — sawaal ka jawab background me bhi
 *   bol kar milega. Command (call/sms/app kholo) background se
 *   execute NAHI hota — uske liye app aage la kar confirmation
 *   li jati hai (ek-confirmation rule kabhi nahi toota).
 * - Mic hamesha ON rehta hai (mute dabane tak): Flora bole ya
 *   user bole, service chalti rehti hai. Sirf Flora ke bolte
 *   waqt sunna roka jata hai taaki wo apni awaaz khud na sune
 *   — bolte hi sunna turant wapas shuru.
 * - Audio kabhi save/log/server par nahi jata — sirf pehchana
 *   hua text brain ko milta hai, jaise foreground me.
 */
class BackgroundMicService : Service() {
    companion object {
        const val ACTION_TAKEOVER = "com.flora.app.TAKEOVER"
        const val ACTION_STANDBY = "com.flora.app.STANDBY"
        private const val CHANNEL_ID = "flora_bg_mic"
        private const val NOTIF_ID = 2002
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val restartRunnable = Runnable { if (wanted) startListening() }
    private var listener: ContinuousListener? = null
    private var voice: VoiceEngine? = null
    private var wanted = false
    private var active = false
    private var cachedPlan: String? = null
    // Licence plan cache (service lifetime): Pro = unlimited, isliye
    // free-limit sirf tab jab plan pro na ho. Pehle sawaal par ek
    // baar server se verify, phir cache — har sawaal par network
    // call nahi. Pro khareedne par mute karke mic dobara ON karo
    // taaki cache refresh ho jaye.

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, "Flora Background Mic",
                NotificationManager.IMPORTANCE_LOW
            )
        )
        val prefs = FloraPreferences(applicationContext)
        val brain = BrainClient(prefs)
        val licenceClient = LicenceClient(prefs)
        val engine = VoiceEngine(applicationContext)
        voice = engine
        engine.onSpeechDone = {
            if (wanted && active) startListening()
        }
        engine.onSpoken = { seconds ->
            scope.launch {
                try {
                    val today = java.text.SimpleDateFormat(
                        "yyyy-MM-dd", java.util.Locale.US
                    ).format(java.util.Date())
                    prefs.addTalkSeconds(today, seconds)
                    licenceClient.countUsage(prefs.effectiveUserId(), seconds)
                } catch (e: Exception) {
                }
            }
        }
        listener = ContinuousListener(
            applicationContext,
            onResult = { text ->
                mainHandler.removeCallbacks(restartRunnable)
                mainHandler.postDelayed(restartRunnable, 45_000)
                scope.launch { handleHeard(text, prefs, brain, licenceClient) }
            },
            onError = {
                if (wanted && active) {
                    mainHandler.removeCallbacks(restartRunnable)
                    mainHandler.postDelayed(restartRunnable, 600)
                }
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Flora background me sun raha hai")
            .setContentText("Mute dabane tak sunta rahunga. Band karne ke liye app me Mute dabao.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)
        if (!wanted) {
            wanted = true
            MicOwner.backgroundActive = true
        }
        when (intent?.action) {
            ACTION_TAKEOVER -> {
                active = true
                startListening()
            }
            ACTION_STANDBY -> {
                active = false
                mainHandler.removeCallbacks(restartRunnable)
                listener?.stop()
            }
        }
        return START_STICKY
    }

    private fun startListening() {
        mainHandler.removeCallbacks(restartRunnable)
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        listener?.start()
    }

    private fun isCommand(lower: String): Boolean {
        return lower.startsWith("call ") || lower.startsWith("sms ") ||
            lower.startsWith("open ") || lower.startsWith("kholo ") ||
            lower.startsWith("image banao ") || lower.startsWith("photo banao ") ||
            lower.startsWith("install karo ") || lower.startsWith("install kar do ") ||
            lower.startsWith("download karo ") ||
            lower.endsWith(" install karo") || lower.endsWith(" install kar do") ||
            lower.startsWith("chrome me ") || lower.startsWith("website ") ||
            lower.startsWith("website kholo ") ||
            lower.startsWith("gaana bajao ") || lower.startsWith("gaana chalao ") ||
            lower == "photo lo" || lower == "scan karo" || lower == "camera kholo" ||
            lower == "sos" || lower == "sos bhejo" || lower == "madad" ||
            lower == "yaadein kholo" || lower == "memories kholo"
    }

    private suspend fun handleHeard(
        text: String,
        prefs: FloraPreferences,
        brain: BrainClient,
        licenceClient: LicenceClient,
    ) {
        val question = text.trim()
        if (question.isEmpty()) return
        val lower = question.lowercase()
        // Command background se execute nahi — app aage lao,
        // confirmation wahi hogi (rule kabhi nahi toota).
        if (isCommand(lower)) {
            FloraCommandBus.pending = question
            voice?.speak("Ye kaam background se nahi hoga, app khol raha hun confirm karne ke liye.")
            try {
                val launch = Intent(this, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
                startActivity(launch)
            } catch (e: Exception) {
            }
            return
        }
        // Image (Pollinations, bina key): background me bhi banegi.
        if (lower.startsWith("image banao ") || lower.startsWith("photo banao ")) {
            val prompt = question.substringAfter("banao", "").trim()
            val saved = ImageTools.generateImage(applicationContext, prompt)
            val msg = if (saved != null) {
                "Image ban gayi, Gallery ke Flora folder me save ho gayi."
            } else {
                "Image nahi ban payi, internet check karke dobara boliye."
            }
            voice?.speak(msg)
            return
        }
        // Free talk limit (activity jaisa hi): Pro nahi to 600s/day.
        // Plan ek baar server se verify karke cache hota hai.
        if (cachedPlan == null) {
            cachedPlan = try {
                val androidId = Settings.Secure.getString(
                    applicationContext.contentResolver, Settings.Secure.ANDROID_ID
                ) ?: "flora-device"
                licenceClient.verify(
                    prefs.effectiveUserId(), prefs.deviceId(androidId)
                ).plan
            } catch (e: Exception) {
                null
            }
        }
        val today = java.text.SimpleDateFormat(
            "yyyy-MM-dd", java.util.Locale.US
        ).format(java.util.Date())
        val used = try { prefs.talkSecondsUsed(today) } catch (e: Exception) { 0L }
        if (cachedPlan != "pro" && used >= 600) {
            voice?.speak("Aaj ka free talk time (10 minute) khatam ho gaya hai. Kal phir milega, ya Pro (₹199/month) lijiye.")
            return
        }
        val reply = try {
            brain.ask(question)
        } catch (e: Exception) {
            "Brain se baat nahi ho payi, dobara boliye."
        }
        voice?.speak(reply)
    }

    override fun onDestroy() {
        wanted = false
        active = false
        mainHandler.removeCallbacks(restartRunnable)
        listener?.stop()
        voice?.stop()
        voice?.shutdown()
        MicOwner.backgroundActive = false
        scope.cancel()
        super.onDestroy()
    }
}
