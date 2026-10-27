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
    val vision = remember { VisionClient() }

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
                    "screen dekhna kabhi nahi hota."
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
