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
import androidx.compose.runtime.DisposableEffect
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
 * Live screen (v1.2): user poochhe to Flora screen ka frame dekh
 * kar jawab deti hai — sirf text nahi, poori tasveer (icon, button,
 * colour, layout). Permission system dialog se milti hai, frame
 * ke baad projection turant band hoti hai, continuous dekhna nahi
 * hota. Frame sirf user ke sawaal ke liye vision ko jaata hai.
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
    // v2.20 fix — "ek baar permission, phir dobara frame nahi
    // mila" ki jad: purana one-shot capture har baar projection
    // STOP kar deta tha, aur wahi permission dobara kaam nahi
    // karti (MediaProjection token ek baar ka hota hai). Ab ek
    // hi session poori Live screen ke liye zinda rehta hai —
    // har sawaal usi se frame leta hai, band sirf screen
    // chhodne par hota hai. Dobara permission tabhi chahiye
    // jab session mar jaye.
    var liveSession by remember { mutableStateOf<ScreenCapture.Session?>(null) }
    DisposableEffect(Unit) {
        onDispose { try { liveSession?.stop() } catch (e: Exception) { } }
    }

    fun openSession(): ScreenCapture.Session? {
        val act = activity
        val data = resultData
        if (act == null || data == null || !granted) return null
        val existing = liveSession
        if (existing != null && existing.isOpen()) return existing
        val fresh = ScreenCapture.startSession(act, resultCode, data)
        liveSession = fresh
        return fresh
    }

    /**
     * Continuous live (v1.4): sirf user ke Chalu dabane par, har 10 sec
     * ek frame, max 10 frame per session (har frame ek Gemini call =
     * user key ka kharcha, isliye cap). Roko dabate hi projection band.
     * Effect cancel hote hi session.stop() chalta hai — chhup kar
     * chalta rehna possible nahi.
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
        while (count < 10) {
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
            if (count < 10) delay(10_000)
        }
        note = "10 frame poore, continuous band. Session zinda hai, sawaal phir poochh sakte ho."
        continuousRunning = false
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // Purana mara hua session hatao, naya agle sawaal par banega.
            try { liveSession?.stop() } catch (e: Exception) { }
            liveSession = null
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
                try { liveSession?.stop() } catch (e: Exception) { }
                liveSession = null
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
                "Permission aap system dialog se denge. Frame milte hi " +
                    "capture band ho jaata hai — chhup kar continuous " +
                    "screen dekhna kabhi nahi hota. Continuous sirf aapke " +
                    "Chalu dabane par chalta hai, har 10 sec ek frame, " +
                    "max 10 frame, phir khud band."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager
                launcher.launch(mgr.createScreenCaptureIntent())
            }) { Text("Screen permission do") }
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
                Text("Continuous chal raha hai — frame $frameCount / 10.")
            } else {
                Button(onClick = {
                    frameCount = 0
                    continuousRunning = true
                }) { Text("Continuous CHALU karo (10 frame tak)") }
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
