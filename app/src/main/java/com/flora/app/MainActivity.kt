package com.flora.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = FloraPreferences(applicationContext)
        voice = VoiceEngine(applicationContext)
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
                    licenceClient.countUsage(prefs.userId(), seconds)
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
                onMicRequest = {
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
            )
        }
    }

    override fun onDestroy() {
        voice.stop()
        voice.shutdown()
        super.onDestroy()
    }
}
