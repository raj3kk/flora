package com.flora.app.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
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
    val vision = remember { VisionClient() }

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
        val session = ScreenCapture.startSession(act, resultCode, data)
        if (session == null) {
            note = "Screen session nahi khuli, dobara permission dijiye."
            continuousRunning = false
            return@LaunchedEffect
        }
        try {
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
            note = "10 frame poore, continuous band. Dobara chalu karna ho to button dabaiye."
        } finally {
            session.stop()
            continuousRunning = false
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            granted = true
            resultCode = result.resultCode
            resultData = result.data
            note = "Screen permission mil gayi. Ab sawaal poochhiye."
        } else {
            note = "Screen permission ke bina live screen nahi dekh sakti."
        }
    }

    fun ask(question: String) {
        val act = activity
        val data = resultData
        if (act == null || data == null || !granted) {
            note = "Pehle screen permission dijiye."
            voice.speak("Pehle screen permission dijiye.")
            return
        }
        note = "Screen dekh rahi hun..."
        scope.launch {
            val jpeg = ScreenCapture.captureFrame(act, resultCode, data)
            if (jpeg == null) {
                note = "Screen frame nahi mila, dobara koshish kijiye."
                voice.speak("Screen frame nahi mila.")
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
                voice.speak("Vision jawab nahi de paya.")
            } else {
                answer = reply
                note = "Dekh liya."
                voice.speak(reply.take(500))
            }
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
