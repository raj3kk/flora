package com.flora.app.voice
import com.flora.app.data.CatcherHooks

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
import com.flora.app.assistant.BatteryTransientNote
import com.flora.app.assistant.BrainClient
import com.flora.app.automation.OverlayMicService
import com.flora.app.data.AutoLogStore
import com.flora.app.data.ChatMemoryStore
import com.flora.app.assistant.VisionClient
import com.flora.app.assistant.VoiceEngine
import com.flora.app.automation.FloraAutomationService
import com.flora.app.core.FeatureFlags
import com.flora.app.core.PlanFeatures
import com.flora.app.data.FloraPreferences
import com.flora.app.data.LicenceClient
import com.flora.app.data.LicenceGate
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
 * - READ-ONLY screen sawaal (v2.22, user order 2026-10-28:
 *   "dusri app me hun, Flora wahi se bata de"): user kisi bhi
 *   app me ho, background mic sun kar screen sawaal pehchanta
 *   hai to accessibility screenshot + vision se jawab BOL KAR
 *   deta hai — app aage laye bina, user usi app me rehta hai.
 *   Screenshot sirf isi sawaal par hota hai; accessibility OFF
 *   ho ya key na ho to seedha bataya jata hai, andaza nahi.
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
        /** APP-C1-022: cachedPlan TTL — 15 minute (LicenceGate cache jaisa). */
        private const val PLAN_CACHE_TTL_MS = 15L * 60L * 1000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val restartRunnable = Runnable { if (wanted) startListening() }
    private var listener: ContinuousListener? = null
    private var voice: VoiceEngine? = null
    private var wanted = false
    private var active = false
    // N1 background island tap ke liye: bol rahi hai ya nahi.
    private var bgSpeaking = false
    private var cachedPlan: String? = null
    private var cachedPlanAtMs = 0L
    // Licence plan cache (service lifetime, TTL ke saath):
    // Pro = unlimited, isliye free-limit sirf tab jab plan pro
    // na ho. Pehle sawaal par ek baar server se verify, phir
    // cache — har sawaal par network call nahi.
    // Cycle 2, APP-C1-022: cachedPlan par TTL hai — 15 minute
    // (LicenceGate ke 15-minute cache jaisa, wahi value yahan
    // document hai). TTL beette hi agla sawaal dobara verify
    // karega, isliye expire/revoke ya nayi Pro service restart
    // tak galat nahi rahegi (service restart par cache waise hi
    // khatam ho jata hai). TTL ke andar ka stale window (max
    // 15 min) is design ka jaana hua trade-off hai — fail-open
    // ke liye verify fail ho to purana cached plan hi use hoga
    // aur agle sawaal par dobara verify ki koshish hogi.

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
        // N1 background island drive (sirf TAKEOVER/active me):
        // bolte waqt SPEAKING, khatam par CONVERSATION_OPEN.
        // STANDBY me foreground FloraApp island chalata hai.
        engine.onSpeechStart = {
            if (active) {
                bgSpeaking = true
                // B1 (fix-cycle): bolte waqt listener roko — Flora ki
                // apni awaaz input na bane (self-trigger loop ka guard).
                // onSpeechDone ka maujooda startListening() dobara
                // naya recognizer banakar shuru karega.
                listener?.stop()
                NotchState.setState(IslandState.SPEAKING)
                syncNotchShow()
            }
        }
        engine.onSpeechDone = {
            bgSpeaking = false
            if (wanted && active) {
                NotchState.setState(IslandState.CONVERSATION_OPEN)
                syncNotchShow()
                startListening()
            }
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
                    CatcherHooks.laneError("mic", "exception")
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
                // N1/R-N-2 background island tap: bolte waqt
                // tap = interrupt (speech-done par sunna apne
                // aap wapas shuru hoga), warna chat aage lao.
                NotchState.onIslandTap = {
                    val v = voice
                    if (bgSpeaking && v != null) {
                        v.stop()
                        NotchState.interrupted = true
                    } else {
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
                            CatcherHooks.laneError("mic", "exception")
                        }
                    }
                }
                NotchState.setState(IslandState.CONVERSATION_OPEN)
                syncNotchShow()
                startListening()
            }
            ACTION_STANDBY -> {
                active = false
                mainHandler.removeCallbacks(restartRunnable)
                listener?.stop()
                // Handler yahan clear NAHI karte — MainActivity
                // onResume ka foreground handler baad me aata
                // hai (service call async hai); clear sirf
                // onPause/onDestroy se hota hai.
            }
        }
        return START_STICKY
    }

    /**
     * N1: island OverlayMicService ke FGS par dikhao (koi
     * doosra FGS nahi). Permission na ho to chupchaap kuch
     * nahi — background me guidance spam nahi karenge.
     */
    private fun syncNotchShow() {
        try {
            if (!Settings.canDrawOverlays(this)) return
            val show = Intent(this, OverlayMicService::class.java).apply {
                action = OverlayMicService.ACTION_NOTCH_SHOW
                putExtra(
                    OverlayMicService.EXTRA_NOTCH_STATE,
                    NotchState.islandState.name,
                )
            }
            startForegroundService(show)
        } catch (e: Exception) {
            CatcherHooks.laneError("mic", "exception")
        }
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

    // Natural Hindi suffix bhi command hai ("youtube kholo") —
    // background se queue hokar chat router ki suffix branch par
    // jaata hai, wahi verified flow par. Sirf " kholo" — "khol do"
    // nahi, kyunki router me uska branch nahi hai.
    private fun isCommand(lower: String): Boolean {
        return lower.startsWith("call ") || lower.startsWith("sms ") ||
            lower.startsWith("open ") || lower.startsWith("kholo ") ||
            lower.startsWith("image banao ") || lower.startsWith("photo banao ") ||
            lower.startsWith("image generate karo ") || lower.startsWith("gemini se image generate karo ") ||
            lower.startsWith("macro chalao ") || lower == "macros batao" || lower == "macro batao" ||
            lower == "form bharo" || lower == "is form ko bharo" ||
            lower.startsWith("field ") && lower.endsWith(" likho") ||
            lower == "form submit karo" ||
            lower == "notification ka jawab batao" || lower == "is notification ka jawab batao" ||
            lower.endsWith(" ka jawab batao") ||
            (lower.startsWith("is app me ") || lower.startsWith("is me ") || lower.startsWith("yahan ")) &&
            lower.endsWith(" search karo") ||
            lower.startsWith("install karo ") || lower.startsWith("install kar do ") ||
            lower.startsWith("download karo ") ||
            lower.endsWith(" install karo") || lower.endsWith(" install kar do") ||
            lower.endsWith(" kholo") ||
            lower.startsWith("chrome me ") || lower.startsWith("website ") ||
            lower.startsWith("website kholo ") ||
            lower.startsWith("gaana bajao ") || lower.startsWith("gaana chalao ") ||
            // Voice-fix: combined video command bhi command hai
            // (foreground confirmation ke liye park hoga, brain Q&A
            // par nahi girega). TIGHT patterns hi: video verb aakhir
            // me, ya " kholo " + video verb ka combined roop.
            lower.endsWith(" video chalao") || lower.endsWith(" video play karo") ||
            (lower.contains(" kholo ") &&
                (lower.contains(" video chalao") || lower.contains(" video play karo"))) ||
            lower == "photo lo" || lower == "scan karo" || lower == "camera kholo" ||
            lower == "sos" || lower == "sos bhejo" || lower == "madad" ||
            lower == "yaadein kholo" || lower == "memories kholo"
    }

    /** User dusri app me hai aur screen/current kaam ke baare me poochh raha hai. */
    private fun isScreenQuestion(lower: String): Boolean {
        return lower.contains("screen") || lower.contains("is app me") ||
            lower.contains("ye app") || lower.contains("kya karu") ||
            lower.contains("ye kya hai") || lower.contains("is screen")
    }

    private suspend fun handleHeard(
        text: String,
        prefs: FloraPreferences,
        brain: BrainClient,
        licenceClient: LicenceClient,
    ) {
        val question = text.trim()
        if (question.isEmpty()) return
        // Cycle 3, F1 (d): background Q&A / screen-vision /
        // image raaste sab isi handleHeard se jaate hain — Face
        // Lock ON aur session unlock na ho to background me
        // KOI jawab nahi (brain.ask / vision bhi nahi). Service
        // FloraApp ka chatUnlocked state dekh nahi sakti,
        // isliye prefs wala unlock session (setFaceLockUnlock)
        // hi sach hai. Locked ho to app aage la kar PIN maango
        // (command pending queue NAHI kiya — warna foreground
        // me bina lock check ke chal jata) + imaandaar message.
        val faceLocked = try {
            prefs.faceLock() && !prefs.isFaceLockUnlocked()
        } catch (e: Exception) {
            CatcherHooks.laneError("mic", "exception")
            true
        }
        if (faceLocked) {
            voice?.speak("Chat lock hai — pehle app khol kar PIN daal kar unlock karo, phir dobara bolo.")
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
                CatcherHooks.laneError("mic", "exception")
            }
            return
        }
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
                CatcherHooks.laneError("mic", "exception")
            }
            return
        }
        // Screen sawaal background me bhi (v2.22): user kisi app
        // me ho, Flora usi app ki screen dekh kar BOL KAR jawab
        // degi — app aage laye bina. Execution command nahi hai,
        // isliye confirmation rule yahan nahi tootta (sirf dekhna
        // + bolna). Screenshot/key na mile to seedha batao.
        if (isScreenQuestion(lower)) {
            // Cycle 1, APP-C1-003: background vision path par
            // bhi wahi gate jo chat commands par hai — flag OFF
            // ho to vision na chale, aur LicenceGate (blocked /
            // free-cap) se pehle hi ruk jaye, taaki blocked ya
            // free-cap-khatm user ka vision kharcha na ho.
            // (Command ka raasta isse alag hai — isCommand par
            // foreground + confirmation hoti hai, use chhua nahi.)
            if (!FeatureFlags.isEnabled("screen_vision") ||
                !PlanFeatures.isAllowed("screen_vision")
            ) {
                voice?.speak("Ye feature abhi band hai.")
                return
            }
            val visionGate = try {
                LicenceGate.check(prefs)
            } catch (e: Exception) {
                CatcherHooks.laneError("mic", "exception")
                null
            }
            if (visionGate is LicenceGate.Result.Blocked) {
                voice?.speak(visionGate.message)
                return
            }
            val jpeg = try {
                FloraAutomationService.instance?.screenshotJpeg()
            } catch (e: Exception) {
                CatcherHooks.laneError("mic", "exception")
                null
            }
            val key = try { prefs.geminiApiKey() } catch (e: Exception) {
                CatcherHooks.laneError("mic", "exception")
            "" }
            if (jpeg != null && key.isNotEmpty()) {
                // Tree context (Phase 3, Maya jaisa): background me bhi
                // vision ko sirf photo nahi, saamne wali app + likha text
                // bhi batao — jawab photo + tree dono par tika ho.
                val treeTexts = try {
                    FloraAutomationService.instance?.visibleTexts(25)
                } catch (e: Exception) {
                    CatcherHooks.laneError("mic", "exception")
                    null
                }
                val richQuestion = if (!treeTexts.isNullOrEmpty()) {
                    val fg = try {
                        FloraAutomationService.instance?.foregroundPackage()
                    } catch (e: Exception) {
                        CatcherHooks.laneError("mic", "exception")
                        null
                    }
                    "Saamne wali app: ${fg ?: "pata nahi"}. " +
                        "Screen par ye likha dikh raha hai: " +
                        treeTexts.joinToString(" | ") + ". " + question
                } else {
                    question
                }
                val reply = try {
                    VisionClient().askAboutImage(richQuestion, jpeg, key)
                } catch (e: Exception) {
                    CatcherHooks.laneError("mic", "exception")
                    null
                }
                voice?.speak(reply?.take(500) ?: "Is baar dekh nahi payi — dobara boliye.")
                return
            }
            if (jpeg == null && FloraAutomationService.isEnabled().not()) {
                voice?.speak("Screen dekhne ke liye Accessibility ek baar ON karna hoga, Settings me. Uske baad main kisi bhi app se dekh kar bata dungi.")
                return
            }
            if (key.isEmpty()) {
                voice?.speak("Screen dekhne ke liye Settings me apni free Gemini key daaliye.")
                return
            }
            // Screenshot fail (screen off/locked jaisa): neeche
            // brain wala general raasta chalega.
        }
        // Image (Pollinations, bina key): background me bhi banegi.
        // Cycle 1, APP-C1-003: is path par bhi image_gen flag +
        // LicenceGate — taaki ye raasta kabhi ungated na chale.
        // (Aaj isCommand is prefix ko pehle pakad kar foreground
        // + confirmation par bhej deta hai; ye gate us routing
        // ke badalne par bhi suraksha rakhega — routing chhui nahi.)
        if (lower.startsWith("image banao ") || lower.startsWith("photo banao ")) {
            if (!FeatureFlags.isEnabled("image_gen") ||
                !PlanFeatures.isAllowed("image_gen")
            ) {
                voice?.speak("Ye feature abhi band hai.")
                return
            }
            val imageGate = try {
                LicenceGate.check(prefs)
            } catch (e: Exception) {
                CatcherHooks.laneError("mic", "exception")
                null
            }
            if (imageGate is LicenceGate.Result.Blocked) {
                voice?.speak(imageGate.message)
                return
            }
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
        // Plan server se verify karke TTL-cache hota hai (APP-C1-022).
        val nowMs = System.currentTimeMillis()
        if (cachedPlan == null || nowMs - cachedPlanAtMs > PLAN_CACHE_TTL_MS) {
            try {
                val androidId = Settings.Secure.getString(
                    applicationContext.contentResolver, Settings.Secure.ANDROID_ID
                ) ?: "flora-device"
                cachedPlan = licenceClient.verify(
                    prefs.effectiveUserId(), prefs.deviceId(androidId)
                ).plan
                cachedPlanAtMs = nowMs
            } catch (e: Exception) {
                CatcherHooks.laneError("mic", "exception")
                // Verify fail: purana cached plan hi use hoga
                // (fail-open); timestamp update nahi, taaki agla
                // sawaal dobara verify ki koshish kare.
            }
        }
        val today = java.text.SimpleDateFormat(
            "yyyy-MM-dd", java.util.Locale.US
        ).format(java.util.Date())
        val used = try { prefs.talkSecondsUsed(today) } catch (e: Exception) {
            CatcherHooks.laneError("mic", "exception")
        0L }
        if (cachedPlan != "pro" && used >= 600) {
            // B5 (fix-cycle): message ki doosri copy nahi — LicenceGate ka const.
            voice?.speak(LicenceGate.FREE_LIMIT_MESSAGE)
            try { AutoLogStore(applicationContext).add("Free talk time khatam (10 min/day) — cap laga.") } catch (e: Exception) {
                CatcherHooks.laneError("mic", "exception")
            }
            return
        }
        // B5 (fix-cycle): cap se pehle ek baar chetavni (din me ek baar) —
        // cap par surprise nahi hona chahiye. Pro par kabhi nahi.
        if (cachedPlan != "pro" && used >= 540) {
            val warned = try { prefs.capWarnDate() } catch (e: Exception) {
                CatcherHooks.laneError("mic", "exception")
            "" }
            if (warned != today) {
                try { prefs.setCapWarnDate(today) } catch (e: Exception) {
                    CatcherHooks.laneError("mic", "exception")
                }
                voice?.speak("Free talk time bas 1 minute bacha hai.")
                try { AutoLogStore(applicationContext).add("Free talk time warning: bas 1 minute bacha hai.") } catch (e: Exception) {
                    CatcherHooks.laneError("mic", "exception")
                }
            }
        }
        // N5/NOTCH-07: foreground jaisi history — persisted
        // store se (cap BrainClient.ask ke andar hi lagta hai:
        // 12 turns / ~1500 chars, foreground wala hi cap).
        // Router divergence yahan CHHUA NAHI gaya (R-N-5) —
        // sirf history argument juda hai, route wahi hai.
        val bgHistory = try {
            ChatMemoryStore(applicationContext).loadAll()
                .map { BrainClient.ChatTurn(it.fromUser, it.text) }
        } catch (e: Exception) {
            CatcherHooks.laneError("mic", "exception")
            emptyList()
        }
        // A1 follow-up F3: background-mic conversational path bhi
        // battery note pata hai — shared non-composable
        // BatteryTransientNote se, wahi BATTERY_LAST_WARNED
        // no-repeat key ke saath (chat + background-mic ek hi
        // source: ek state par ek surface ne warn kar diya to
        // doosri chup rahegi). handleHeard suspend hai aur
        // scope.launch se chalta hai, isliye DataStore read
        // suspend-safe hai. BrainClient server contract badla
        // nahi — transientNote param pehle se tha. Note sirf
        // is ek call ke prompt me jata hai, persisted store
        // me sirf user turn + bola gaya jawab jate hain.
        val reply = try {
            brain.ask(
                question,
                history = bgHistory,
                transientNote = BatteryTransientNote.note(applicationContext, prefs),
            )
        } catch (e: Exception) {
            CatcherHooks.laneError("mic", "exception")
            "Brain se baat nahi ho payi, dobara boliye."
        }
        // Background turn bhi yaad rahe (warna agla foreground
        // turn is baat-cheet ko bhool jayega) — user turn +
        // bola gaya jawab dono persisted store me.
        try {
            val store = ChatMemoryStore(applicationContext)
            store.addTurn(true, question)
            store.addTurn(false, reply)
        } catch (e: Exception) {
            CatcherHooks.laneError("mic", "exception")
        }
        voice?.speak(reply)
    }

    // Task swipe ke baad restart ki koshish (v2.23, Phase 5): user ne
    // background mic chaha tha (wanted) to service wapas lane ki
    // koshish karo. Android 12+ / Vivo par ye block ho sakta hai
    // (OS niyam) — try/catch hai, fail ho to crash nahi; tab
    // battery-unrestricted + auto-start hi bachaav hai (guidance
    // Settings/Permissions screen par hai).
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (wanted) {
            try {
                startForegroundService(Intent(this, BackgroundMicService::class.java))
            } catch (e: Exception) {
                CatcherHooks.laneError("mic", "exception")
            }
        }
    }

    override fun onDestroy() {
        wanted = false
        active = false
        bgSpeaking = false
        NotchState.onIslandTap = null
        NotchState.clearAmplitude()
        mainHandler.removeCallbacks(restartRunnable)
        listener?.stop()
        voice?.stop()
        voice?.shutdown()
        MicOwner.backgroundActive = false
        scope.cancel()
        super.onDestroy()
    }
}
