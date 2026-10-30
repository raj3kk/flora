package com.flora.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import com.flora.app.assistant.VoiceEngine
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.launch

/**
 * Driving mode (v1.4): haath-free, bade button, awaaz pehle.
 * Gaadi mode ON karte hi call ka naam bolna bhi ON ho jaata hai
 * (CALL_ANNOUNCE), taaki phone chhooye bina pata chale kaun call
 * kar raha hai. Gaadi mode OFF karne par call-announce waisa hi
 * rehta hai jaisa user ne khud set kiya ho — chupke se koi setting
 * wapas nahi badli jaati. Koi message/call bina confirmation ke
 * driving mode me bhi nahi jaata.
 */
@Composable
fun DrivingScreen(voice: VoiceEngine, prefs: FloraPreferences, onMicRequest: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var driving by remember { mutableStateOf(false) }
    var callAnnounce by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    // Parity batch 2026-10-31, M11: driving-mode SMS — sirf
    // SMS app ka draft khulta hai, Send user khud dabayega.
    var smsNumber by remember { mutableStateOf("") }
    var smsText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        driving = prefs.drivingMode()
        callAnnounce = prefs.callAnnounce()
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Gaadi Mode — haath-free, awaaz se chalao.")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Gaadi mode ON hai to Flora bol kar batayegi, screen par " +
                    "padhna nahi padega. Call aane par naam bolna bhi ON ho jaata hai."
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = {
                val next = !driving
                scope.launch {
                    prefs.setDrivingMode(next)
                    if (next) prefs.setCallAnnounce(true)
                    driving = next
                    if (next) callAnnounce = true
                    val msg = if (next) "Gaadi mode chalu, call ka naam bolna bhi chalu."
                        else "Gaadi mode band."
                    note = msg
                    voice.speak(msg)
                }
            }) { Text(if (driving) "Gaadi Mode BAND karo" else "Gaadi Mode CHALU karo") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val next = !callAnnounce
                scope.launch {
                    prefs.setCallAnnounce(next)
                    callAnnounce = next
                    val msg = if (next) "Call ka naam bolna chalu." else "Call ka naam bolna band."
                    note = msg
                    voice.speak(msg)
                }
            }) { Text("Call ka naam bolna: " + if (callAnnounce) "ON" else "OFF") }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onMicRequest) { Text("🎤 Mic se bolo") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { voice.stop() }) { Text("Awaaz roko") }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Driving SMS — number + message likho, phir SMS app khulega; Send tum khud dabao ge. Flora kabhi chupke SMS nahi bhejegi.")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = smsNumber, onValueChange = { smsNumber = it },
                label = { Text("Mobile number") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = smsText, onValueChange = { smsText = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (smsNumber.isBlank() || smsText.isBlank()) {
                    note = "SMS ke liye number + message dono chahiye."
                    return@Button
                }
                try {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:" + smsNumber.trim())
                        putExtra("sms_body", smsText)
                    }
                    context.startActivity(intent)
                    note = "SMS app khul gaya — Send tum dabao ge."
                } catch (e: Exception) {
                    note = "SMS app kholne me fail hua."
                }
            }) { Text("SMS app me kholo") }
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
        }
    }
}
