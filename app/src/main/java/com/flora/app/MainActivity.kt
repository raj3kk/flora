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
import com.flora.app.assistant.VoiceEngine
import com.flora.app.data.FloraPreferences
import com.flora.app.data.LicenceClient
import com.flora.app.data.MemoriesStore
import com.flora.app.data.SosStore
import com.flora.app.tools.SearchTools
import com.flora.app.security.GuardianGate
import com.flora.app.security.PinVault
import com.flora.app.ui.FloraApp

/**
 * Flora v0.3 — clean-room rebuild.
 * Chat / Settings / Yaadein / Scan / Tools / SOS. Guardian mode
 * PIN-gated hai; voice-match aur Face Lock alag steps me aayenge —
 * is file me unka koi daava nahi hai.
 */
class MainActivity : ComponentActivity() {
    private lateinit var voice: VoiceEngine
    private val voiceResult = mutableStateOf("")

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
        val brain = BrainClient(prefs)
        val licenceClient = LicenceClient(prefs)
        val pinVault = PinVault(applicationContext)
        val guardian = GuardianGate()
        val memoriesStore = MemoriesStore(applicationContext)
        val searchTools = SearchTools()
        val sosStore = SosStore(applicationContext)
        setContent {
            FloraApp(
                prefs = prefs,
                brain = brain,
                voice = voice,
                licenceClient = licenceClient,
                pinVault = pinVault,
                guardian = guardian,
                memoriesStore = memoriesStore,
                searchTools = searchTools,
                sosStore = sosStore,
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
