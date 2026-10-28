package com.flora.app

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import com.flora.app.assistant.BrainClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.flora.app.assistant.VoiceEngine
import com.flora.app.voice.BackgroundMicService
import com.flora.app.voice.ContinuousListener
import com.flora.app.voice.FloraCommandBus
import com.flora.app.data.FloraPreferences
import com.flora.app.data.LicenceClient
import com.flora.app.data.DocsStore
import com.flora.app.data.MemoriesStore
import com.flora.app.data.JournalStore
import com.flora.app.data.SosStore
import com.flora.app.tools.SearchTools
import com.flora.app.security.GuardianGate
import com.flora.app.security.PinVault
import com.flora.app.ui.FloraApp

/**
 * Flora v1.3 — clean-room rebuild.
 * Audit gaps fix: free talk-limit enforce (600 sec/day, Pro
 * unlimited), Memories export + delete-all, event triggers.
 * Dialer call-talk + continuous live + driving mode abhi baaki hain.
 */
class MainActivity : ComponentActivity() {
    private lateinit var voice: VoiceEngine
    private val voiceResult = mutableStateOf("")
    // Continuous mic (v2.14): suni hui baat + sunne ki haalat.
    private val voiceCommand = mutableStateOf("")
    private val listeningState = mutableStateOf(false)
    // Bolne ki haalat (v2.14): UI ko baari dikhane ke liye.
    private val speakingState = mutableStateOf(false)
    private var continuousListener: ContinuousListener? = null
    private var continuousWanted = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val restartRunnable = Runnable {
        if (continuousWanted) startContinuous()
    }

    private fun startContinuous() {
        mainHandler.removeCallbacks(restartRunnable)
        continuousListener?.start()
        listeningState.value = continuousListener?.isListening() == true
    }

    /**
     * Background mic service (v2.15): mic ON ho to service hamesha
     * chalegi — foreground me STANDBY (activity khud sunti hai),
     * app back hote hi TAKEOVER (service sunti + jawab deti hai).
     * Start foreground se hota hai (safe), fail ho to try/catch —
     * foreground sunna tab bhi chalta rahega.
     */
    private fun bgService(action: String?) {
        try {
            val intent = Intent(this, BackgroundMicService::class.java)
            if (action != null) intent.action = action
            androidx.core.content.ContextCompat.startForegroundService(this, intent)
        } catch (e: Exception) {
        }
    }

    private fun stopBgService() {
        try {
            stopService(Intent(this, BackgroundMicService::class.java))
        } catch (e: Exception) {
        }
    }

    /** Background se aaya command (call/sms/app) — Chat pipeline
     * isi se chalega, confirmation app me hi hogi. */
    private fun drainCommandBus() {
        val pending = FloraCommandBus.pending ?: return
        FloraCommandBus.pending = null
        voiceCommand.value = pending
    }

    /**
     * Talk-seconds record karne ke liye apna scope (lifecycleScope
     * par bharosa nahi — CI par uska launch resolve nahi hua tha).
     * kotlinx-coroutines-android pehle se dependency me hai.
     */
    private val talkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val micLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) voiceResult.value = spoken
        }
    }

    /** Mic button + overlay mic button dono isi se bolna shuru
     *  karte hain (system speech recognizer, hi-IN). */
    private fun startMicRecognition() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            }
            micLauncher.launch(intent)
        } catch (e: Exception) {
            // No speech recognizer on device: mic button simply does nothing.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = FloraPreferences(applicationContext)
        voice = VoiceEngine(applicationContext)
        // Continuous mic (v2.14): suno -> text FloraApp ko -> jawab
        // bolne ke baad TTS-done par dobara suno. Error/chuppi par
        // chhota delay, aur har result ke baad 45s safety restart.
        continuousListener = ContinuousListener(
            applicationContext,
            onResult = { text ->
                listeningState.value = false
                voiceCommand.value = text
                mainHandler.removeCallbacks(restartRunnable)
                mainHandler.postDelayed(restartRunnable, 45_000)
            },
            onError = {
                listeningState.value = false
                if (continuousWanted) {
                    mainHandler.removeCallbacks(restartRunnable)
                    mainHandler.postDelayed(restartRunnable, 600)
                }
            },
        )
        voice.onSpeechDone = {
            speakingState.value = false
            if (continuousWanted) startContinuous()
        }
        voice.onSpeechStart = {
            speakingState.value = true
        }
        val licenceClient = LicenceClient(prefs)
        voice.onSpoken = { seconds ->
            talkScope.launch {
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(java.util.Date())
                prefs.addTalkSeconds(today, seconds)
                // v2.6-prep (push v2.5 PASS ke baad): server count
                // SIRF yahan se — FloraApp ke 3 check sites se kabhi
                // nahi (triple-count ho jayega). Fail ho to local
                // counter hi sach hai, baat-cheet nahi rukti.
                try {
                    licenceClient.countUsage(prefs.effectiveUserId(), seconds)
                } catch (e: Exception) {
                }
            }
        }
        val brain = BrainClient(prefs)
        val pinVault = PinVault(applicationContext)
        val guardian = GuardianGate()
        val memoriesStore = MemoriesStore(applicationContext)
        val journalStore = JournalStore(applicationContext)
        val searchTools = SearchTools(prefs)
        val sosStore = SosStore(applicationContext)
        val docsStore = DocsStore(applicationContext)
        setContent {
            FloraApp(
                prefs = prefs,
                brain = brain,
                voice = voice,
                licenceClient = licenceClient,
                pinVault = pinVault,
                guardian = guardian,
                memoriesStore = memoriesStore,
                journalStore = journalStore,
                searchTools = searchTools,
                sosStore = sosStore,
                docsStore = docsStore,
                voiceResult = voiceResult,
                onMicRequest = { startMicRecognition() },
                voiceCommand = voiceCommand,
                listeningState = listeningState,
                speakingState = speakingState,
                onContinuousStart = {
                    continuousWanted = true
                    talkScope.launch {
                        try { prefs.setContinuousMic(true) } catch (e: Exception) { }
                    }
                    bgService(null)
                    startContinuous()
                },
                onContinuousStop = {
                    continuousWanted = false
                    mainHandler.removeCallbacks(restartRunnable)
                    continuousListener?.stop()
                    listeningState.value = false
                    stopBgService()
                    talkScope.launch {
                        try { prefs.setContinuousMic(false) } catch (e: Exception) { }
                    }
                },
            )
        }
        // Returning user (v2.14 fix): wizard pehle se poora ho aur mic
        // mila ho to app khulte hi sunna apne aap shuru — wizard wale
        // user ke liye onDone raasta alag se hai (upar FloraApp me).
        talkScope.launch {
            try {
                val micOk = ContextCompat.checkSelfPermission(
                    applicationContext, android.Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                val wasOn = prefs.continuousMic()
                if (wasOn && micOk) {
                    continuousWanted = true
                    runOnUiThread { bgService(null); startContinuous() }
                }
            } catch (e: Exception) {
            }
        }
        // Background wake (v2.14): "Hey Flora" se app aage aayi ho to
        // isi session ke liye sunna shuru karo — mute bhi ho to bhi,
        // kyunki wake khud "ab baat karo" ka ishara hai. 2s delay
        // taaki wake greeting ("Haan, boliye") mic me na aaye.
        if (intent?.getBooleanExtra("flora_wake", false) == true) {
            continuousWanted = true
            mainHandler.postDelayed({ startContinuous() }, 2000)
        }
        // Overlay mic button (v2.22, Phase 1): button dabate hi
        // thodi der baad sunna shuru — activity resume hone ke
        // baad, taaki recognizer sahi se khule.
        if (intent?.getBooleanExtra("flora_mic", false) == true) {
            mainHandler.postDelayed({ startMicRecognition() }, 500)
        }
        drainCommandBus()
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        if (newIntent.getBooleanExtra("flora_wake", false)) {
            continuousWanted = true
            mainHandler.postDelayed({ startContinuous() }, 2000)
        }
        if (newIntent.getBooleanExtra("flora_mic", false)) {
            startMicRecognition()
        }
        drainCommandBus()
    }

    override fun onResume() {
        super.onResume()
        // Foreground me wapas: activity khud sunegi, service
        // STANDBY me jayegi (band nahi — agle back par phir
        // TAKEOVER karegi). Mic OFF ho to service poori band.
        if (continuousWanted) {
            bgService(BackgroundMicService.ACTION_STANDBY)
            startContinuous()
        } else {
            stopBgService()
        }
    }

    override fun onPause() {
        // v2.15: mic ON ho to back hote hi service TAKEOVER karegi
        // (sunna + jawab background me jaari). Mic OFF ho to
        // pehle jaisa — sunna band, service bhi band.
        continuousListener?.stop()
        listeningState.value = false
        mainHandler.removeCallbacks(restartRunnable)
        if (continuousWanted) {
            bgService(BackgroundMicService.ACTION_TAKEOVER)
        } else {
            stopBgService()
        }
        super.onPause()
    }

    override fun onDestroy() {
        continuousListener?.stop()
        voice.stop()
        voice.shutdown()
        super.onDestroy()
    }
}
