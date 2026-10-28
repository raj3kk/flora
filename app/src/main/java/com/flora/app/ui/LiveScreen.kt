package com.flora.app.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import com.flora.app.automation.AppLauncher
import com.flora.app.automation.ScreenCapture
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Live screen (v2.22): user poochhe to Flora screen ka frame dekh
 * kar jawab deti hai — sirf text nahi, poori tasveer (icon, button,
 * colour, layout). Permission system dialog se milti hai.
 * Session app-level holder me zinda rehta hai — screen badalne par
 * bhi nahi marta, band sirf "session band karo" par ya projection
 * ke marne par hota hai. Continuous sirf user ke Chalu dabane par
 * chalta hai, har 10 sec ek frame, ROKO dabane tak (har frame ek
 * Gemini call = user key ka kharcha, isliye gap rakha hai).
 */
@Composable
fun LiveScreen(voice: VoiceEngine, prefs: FloraPreferences) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var granted by remember { mutableStateOf(false) }
    var resultCode by remember { mutableStateOf(0) }
    var resultData by remember { mutableStateOf<Intent?>(null) }
    var answer by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var continuousRunning by remember { mutableStateOf(false) }
    var frameCount by remember { mutableStateOf(0) }
    var lastSummary by remember { mutableStateOf("") }
    var appName by remember { mutableStateOf("") }
    val vision = remember { VisionClient() }
    // v2.22 fix — session ab composable ke `remember` me nahi,
    // ScreenCapture.LiveSessionHolder me rehta hai: screen
    // chhodne par bhi zinda, band sirf user ke kehne par ya
    // projection ke marne par. Isse "baar baar permission" wali
    // loop khatam hoti hai (process marne par Android ka niyam
    // phir bhi dobara permission maangega — wo OS ka niyam hai).

    fun openSession(): ScreenCapture.Session? {
        val act = activity
        val data = resultData
        if (act == null || data == null || !granted) return null
        val existing = ScreenCapture.LiveSessionHolder.session
        if (existing != null && existing.isOpen()) return existing
        val fresh = ScreenCapture.startSession(act, resultCode, data)
        ScreenCapture.LiveSessionHolder.session = fresh
        return fresh
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
        val act = activity
        val data = resultData
        if (act == null || data == null || !granted) {
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

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // Purana mara hua session hatao, naya agle sawaal par banega.
            ScreenCapture.LiveSessionHolder.clear()
            granted = true
            resultCode = result.resultCode
            resultData = result.data
            note = "Screen permission mil gayi. Ab sawaal poochhiye."
        } else {
            note = "Screen permission ke bina live screen nahi dekh sakti."
        }
    }

    fun ask(question: String) {
        if (!granted || resultData == null) {
            note = "Pehle screen permission dijiye."
            voice.speak("Pehle screen permission dijiye.")
            return
        }
        note = "Screen dekh rahi hun..."
        scope.launch {
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
            note = "Pehle app ka naam likho, jaise WhatsApp."
            voice.speak("Pehle app ka naam likho.")
            return
        }
        if (!granted || resultData == null) {
            note = "Pehle screen permission dijiye."
            voice.speak("Pehle screen permission dijiye.")
            return
        }
        scope.launch {
            val match = AppLauncher.findByName(context, name).firstOrNull()
            if (match == null || !AppLauncher.launch(context, match.packageName)) {
                note = "'$name' naam ki app nahi mili."
                voice.speak("'$name' naam ki app nahi mili, dobara naam bolo.")
                return@launch
            }
            note = "${match.label} khol diya, screen dekh rahi hun..."
            voice.speak("${match.label} khol diya, ab screen dekh rahi hun.")
            delay(3_000)
            ask("Is screen par kya dikh raha hai, sab kuch detail me batao?")
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
                granted = false
                resultData = null
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
            TextField(
                value = appName,
                onValueChange = { appName = it },
                label = { Text("App ka naam (jaise WhatsApp)") },
                modifier = Modifier.fillMaxWidth()
            )
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
