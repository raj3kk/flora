package com.flora.app.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flora.app.assistant.VisionClient
import com.flora.app.assistant.VoiceEngine
import com.flora.app.automation.VerifiedAppLauncher
import com.flora.app.automation.FloraAutomationService
import com.flora.app.automation.ScreenCapture
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Live screen (v2.22, voice-first): user poochhe to Flora screen ka
 * frame dekh kar jawab deti hai — sirf text nahi, poori tasveer.
 * Permission + token FloraApp level par hain (Chat se bhi "screen
 * dekho" bolne par kaam kare). App ka naam TYPE nahi — mic button
 * se BOLO, Flora khud khol kar dekh legi.
 */
@Composable
fun LiveScreen(
    voice: VoiceEngine,
    prefs: FloraPreferences,
    screenGranted: Boolean,
    screenResultCode: Int,
    screenResultData: Intent?,
    onScreenResult: (Int, Intent?) -> Unit,
    onScreenReset: () -> Unit,
    screenLauncher: ActivityResultLauncher<Intent>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var answer by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var continuousRunning by remember { mutableStateOf(false) }
    var frameCount by remember { mutableStateOf(0) }
    var lastSummary by remember { mutableStateOf("") }
    var appName by remember { mutableStateOf("") }
    val vision = remember { VisionClient() }
    // FloraApp se permission mile to note dikhao (launcher callback
    // FloraApp me hai, isliye yahan effect se note update).
    LaunchedEffect(screenGranted) {
        if (screenGranted) note = "Screen permission mil gayi. Ab sawaal poochhiye."
    }
    // v2.22 fix — session ab composable ke `remember` me nahi,
    // ScreenCapture.LiveSessionHolder me rehta hai: screen
    // chhodne par bhi zinda, band sirf user ke kehne par ya
    // projection ke marne par. Isse "baar baar permission" wali
    // loop khatam hoti hai (process marne par Android ka niyam
    // phir bhi dobara permission maangega — wo OS ka niyam hai).

    fun openSession(): ScreenCapture.Session? {
        if (!screenGranted) return null
        return ScreenCapture.openShared(context, screenResultCode, screenResultData)
    }

    /**
     * Continuous live (v2.22): sirf user ke Chalu dabane par, har 10 sec
     * ek frame, ROKO dabane tak — pehle wala "max 10 frame" cap hata
     * diya gaya hai (user order: jitne frame ho sake, poore milein).
     * Har frame ek Gemini call hai = user key ka kharcha, isliye
     * 10 sec ka gap rakha hai. Roko dabate hi loop band, session
     * zinda rehta hai agle sawaal ke liye.
     */
    LaunchedEffect(continuousRunning) {
        if (!continuousRunning) return@LaunchedEffect
        if (!screenGranted || screenResultData == null) {
            note = "Pehle screen permission dijiye."
            voice.speak("Pehle screen permission dijiye.")
            continuousRunning = false
            return@LaunchedEffect
        }
        val key = prefs.geminiApiKey()
        if (key.isEmpty()) {
            note = "Continuous ke liye Settings me apni free Gemini key daaliye."
            voice.speak("Continuous ke liye Gemini key chahiye.")
            continuousRunning = false
            return@LaunchedEffect
        }
        // v2.20: shared persistent session — har frame usi se,
        // khatm par session band NAHI (agle sawaal ke liye zinda).
        val session = openSession()
        if (session == null) {
            note = "Screen session nahi khuli, dobara permission dijiye."
            voice.speak("Arre, screen session nahi khuli — ek baar phir se permission de do.")
            continuousRunning = false
            return@LaunchedEffect
        }
        var count = 0
        while (continuousRunning) {
            val jpeg = session.captureFrame()
            if (jpeg != null) {
                val reply = vision.askAboutImage(
                    "Is screen par abhi kya dikh raha hai, 2 line me batao?", jpeg, key
                )
                if (reply != null) {
                    lastSummary = reply
                    voice.speak(reply.take(300))
                }
            }
            count++
            frameCount = count
            delay(10_000)
        }
        note = "Continuous band. Session zinda hai, sawaal phir poochh sakte ho."
        continuousRunning = false
    }

    val launcher = screenLauncher

    fun ask(question: String) {
        if (!screenGranted || screenResultData == null) {
            note = "Pehle screen permission dijiye."
            voice.speak("Pehle screen permission dijiye.")
            return
        }
        note = "Screen dekh rahi hun..."
        scope.launch {
            // One-time-setup raasta PEHLE: accessibility ON ho to
            // bina dialog, bina Allow tap ke screenshot lo.
            val accessJpeg = try {
                FloraAutomationService.instance?.screenshotJpeg()
            } catch (e: Exception) {
                null
            }
            if (accessJpeg != null) {
                val key = prefs.geminiApiKey()
                if (key.isEmpty()) {
                    note = "Live screen ke liye Settings me apni free Gemini key daaliye."
                    voice.speak("Live screen ke liye Gemini key chahiye.")
                    return@launch
                }
                val reply = vision.askAboutImage(question, accessJpeg, key)
                if (reply == null) {
                    note = "Vision jawab nahi de paya, dobara koshish kijiye."
                    voice.speak("Arre, is baar dekh nahi payi — ek baar phir poochho na.")
                } else {
                    answer = reply
                    note = "Dekh liya."
                    voice.speak(reply.take(500))
                }
                return@launch
            }
            // v2.20: persistent session + 3 baar retry (har baar
            // naya virtual display). Session mara ho to pyar se
            // dobara permission maango + dialog khud kholo.
            var jpeg: ByteArray? = null
            for (attempt in 0 until 3) {
                val session = openSession() ?: break
                jpeg = session.captureFrame()
                if (jpeg != null) break
            }
            if (jpeg == null) {
                note = "Arre, screen abhi dikh nahi rahi — ek baar phir se permission de do, phir main dekh lungi."
                voice.speak("Arre, screen abhi dikh nahi rahi — ek baar phir se permission de do, phir main dekh lungi.")
                ScreenCapture.LiveSessionHolder.clear()
                onScreenReset()
                val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager
                launcher.launch(mgr.createScreenCaptureIntent())
                return@launch
            }
            val key = prefs.geminiApiKey()
            if (key.isEmpty()) {
                note = "Live screen ke liye Settings me apni free Gemini key daaliye."
                voice.speak("Live screen ke liye Gemini key chahiye.")
                return@launch
            }
            val reply = vision.askAboutImage(question, jpeg, key)
            if (reply == null) {
                note = "Vision jawab nahi de paya, dobara koshish kijiye."
                voice.speak("Arre, is baar dekh nahi payi — ek baar phir poochho na.")
            } else {
                answer = reply
                note = "Dekh liya."
                voice.speak(reply.take(500))
            }
        }
    }

    // v2.20: "app kholo aur dekho" — user ne bola koi app khulti
    // hi nahi: ab naam likho, Flora app kholegi, 3 sec rukegi,
    // phir usi session se us app ki screen dekh kar batayegi.
    fun openAppAndLook() {
        val name = appName.trim()
        if (name.isEmpty()) {
            note = "Pehle app ka naam bolo — mic dabao aur naam boliye, jaise WhatsApp."
            voice.speak("Pehle app ka naam bolo.")
            return
        }
        if (!screenGranted || screenResultData == null) {
            note = "Pehle screen permission dijiye."
            voice.speak("Pehle screen permission dijiye.")
            return
        }
        scope.launch {
            val matches = VerifiedAppLauncher.findByName(context, name)
            when {
                matches.isEmpty() -> {
                    note = "'$name' naam ki app nahi mili."
                    voice.speak("'$name' naam ki app nahi mili, dobara naam bolo.")
                    return@launch
                }
                matches.size > 1 -> {
                    val names = matches.take(3).joinToString(", ") { it.label }
                    note = "Ek se zyada app mili: $names."
                    voice.speak("Ek se zyada app mili: $names. Poora naam boliye.")
                    return@launch
                }
                else -> {
                    val match = matches[0]
                    when (VerifiedAppLauncher.openVerified(context, match)) {
                        VerifiedAppLauncher.OpenResult.Opened,
                        VerifiedAppLauncher.OpenResult.AlreadyOpen -> {
                            note = "${match.label} khol diya, screen dekh rahi hun..."
                            voice.speak("${match.label} khol diya, ab screen dekh rahi hun.")
                            delay(1_500)
                            ask("Is screen par kya dikh raha hai, sab kuch detail me batao?")
                        }
                        VerifiedAppLauncher.OpenResult.NoVerifier -> {
                            note = "${match.label} kholna verify nahi kar paya — Accessibility ON karna hoga."
                            voice.speak("${match.label} kholna verify nahi kar paya — Accessibility service ON karna hoga, Settings me.")
                        }
                        else -> {
                            note = "${match.label} nahi khul paya."
                            voice.speak("${match.label} nahi khul paya, dobara naam bolo.")
                        }
                    }
                }
            }
        }
    }

    // App ka naam BOL kar (v2.22 voice-first): mic se suno, naam
    // mile to khud khol kar dekho — type karne ki zaroorat nahi.
    // openAppAndLook ke BAAD (order trap — pehle hota to CI fail).
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                appName = spoken
                openAppAndLook()
            }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Live Screen — poochhiye, Flora dekh kar batayegi.")
            Text(
                "Permission aap system dialog se denge. Session ek baar " +
                    "khulne ke baad zinda rehta hai — screen badalne par bhi " +
                    "dobara permission nahi maangega, jab tak app ka process " +
                    "zinda hai. Continuous sirf aapke Chalu dabane par chalta " +
                    "hai, har 10 sec ek frame, ROKO dabane tak. Har frame ek " +
                    "Gemini call hai, isliye gap rakha hai."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager
                launcher.launch(mgr.createScreenCaptureIntent())
            }) { Text("Screen permission do") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                ScreenCapture.LiveSessionHolder.clear()
                onScreenReset()
                note = "Screen session band kar diya. Dobara dekhne ke liye phir permission do."
            }) { Text("Screen session band karo") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { ask("Is screen par kya dikh raha hai, sab kuch detail me batao?") }) {
                Text("Is screen par kya hai?")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { ask("Is screen par kya likha hai, aur kaun se button/option dikh rahe hain?") }) {
                Text("Is screen par kya likha hai, kaun se button hain?")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                }
                try {
                    micLauncher.launch(intent)
                } catch (e: Exception) {
                    note = "Voice input nahi mila — Chat me bol kar bhi kar sakte ho."
                }
            }) { Text("🎤 Bolo app ka naam") }
            if (appName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("App: $appName")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { openAppAndLook() }) {
                Text("App kholo aur dekho")
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (continuousRunning) {
                Button(onClick = { continuousRunning = false }) { Text("Continuous ROKO") }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Continuous chal raha hai — frame $frameCount.")
            } else {
                Button(onClick = {
                    frameCount = 0
                    continuousRunning = true
                }) { Text("Continuous CHALU karo (ROKO dabane tak)") }
            }
            if (lastSummary.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Aakhri summary: $lastSummary")
            }
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
            if (answer.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(answer)
            }
        }
    }
}
