package com.flora.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flora.app.assistant.VoiceEngine
import com.flora.app.data.FloraPreferences
import com.flora.app.voice.SttLanes
import com.flora.app.voice.VoiceMatcher
import com.flora.app.voice.VoiceRecorder
import com.flora.app.voice.VoiceVault
import kotlinx.coroutines.launch

/**
 * Awaaz sikhao screen (v2.22, mega build chunk 3): user apni
 * awaaz 3 baar (3 second har baar) suna kar enrol karta hai.
 * Samples kabhi save nahi hote — sirf average d-vector
 * KeyStore me encrypted rehta hai. Enrol ke baad PIN unlock
 * par awaaz ka check bhi lagega (Guardian wiring).
 */
@Composable
fun VoiceEnrolScreen(voice: VoiceEngine) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var samples by remember { mutableStateOf<List<ShortArray>>(emptyList()) }
    var recording by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var transcribeNote by remember { mutableStateOf("") }
    var enrolled by remember { mutableStateOf(VoiceVault(context).hasEnrolled()) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Awaaz sikhao — taaki Flora tumhari awaaz pehchan sake.")
            Text(
                "3 baar, har baar 3 second, saaf awaaz me kuch bolo " +
                    "(jaise 'Namaste, main Priyadarshi hun'). Samples phone " +
                    "par hi rehte hain, kahin upload nahi hote."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Enrolled: " + if (enrolled) "haan ✓" else "nahi")
            Spacer(modifier = Modifier.height(8.dp))
            for (i in 0 until 3) {
                val done = samples.size > i
                Button(
                    enabled = !recording,
                    onClick = {
                        recording = true
                        note = "Sun raha hun, 3 second bolo..."
                        scope.launch {
                            val pcm = VoiceRecorder.recordSeconds(context, 3)
                            recording = false
                            if (pcm == null) {
                                note = "Recording nahi ho payi — Mic permission Permissions screen se do, phir dobara karo."
                            } else {
                                samples = samples + pcm
                                note = "Sample ${samples.size} ho gaya."
                                voice.speak("Sample ho gaya.")
                            }
                        }
                    }
                ) { Text(if (done) "Sample ${i + 1} ✓ (dobara)" else "Sample ${i + 1} record karo") }
                Spacer(modifier = Modifier.height(4.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                enabled = samples.size >= 3 && !recording,
                onClick = {
                    scope.launch {
                        note = "Awaaz seekh raha hun..."
                        val ok = VoiceMatcher.enrol(context, samples)
                        if (ok) {
                            enrolled = true
                            note = "Ho gaya! Tumhari awaaz seekh li hai. Ab PIN unlock par awaaz ka check bhi lagega."
                            voice.speak("Ho gaya, tumhari awaaz seekh li hai.")
                        } else {
                            note = "Seekh nahi paya — awaaz saaf nahi mili ya model abhi download nahi hua. Models screen se voice-encoder download karo, phir dobara karo."
                            voice.speak("Seekh nahi paya, dobara koshish karo.")
                        }
                    }
                }
            ) { Text("Save karo (enrol)") }
            Spacer(modifier = Modifier.height(8.dp))
            // STT check (v2.22, Phase 3): aakhri sample ko
            // Deepgram se text me badal kar dikhao — isse pata
            // chalta hai Deepgram lane chal rahi hai ya nahi.
            // Key na ho to guide dikhta hai, koi jhootha daava nahi.
            Button(
                enabled = samples.isNotEmpty() && !recording,
                onClick = {
                    scope.launch {
                        val prefs = FloraPreferences(context)
                        if (!SttLanes.hasDeepgramKey(prefs)) {
                            transcribeNote = "Deepgram key chahiye — Settings me daalo, phir dobara karo."
                        } else {
                            transcribeNote = "Sun kar likh raha hun..."
                            val text = SttLanes.transcribe(samples.last(), prefs)
                            transcribeNote = if (!text.isNullOrBlank()) {
                                "Deepgram ne suna: $text"
                            } else {
                                "Samajh nahi paya — dobara saaf bolo."
                            }
                        }
                    }
                }
            ) { Text("Aakhri sample ko text me badlo (STT check)") }
            if (transcribeNote.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(transcribeNote)
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (enrolled) {
                Button(onClick = {
                    VoiceVault(context).clear()
                    samples = emptyList()
                    enrolled = false
                    note = "Enrolled awaaz hata di gayi hai."
                }) { Text("Enrolled awaaz hatao") }
            }
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
        }
    }
}
