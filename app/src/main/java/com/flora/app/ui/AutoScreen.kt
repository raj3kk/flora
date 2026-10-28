package com.flora.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.flora.app.automation.VerifiedAppLauncher
import com.flora.app.automation.FloraAutomationService
import com.flora.app.automation.LaunchableApp
import kotlinx.coroutines.launch

/**
 * Auto screen (v1.0): universal automation control.
 * - Accessibility service status + button that opens the system
 *   Accessibility settings — the USER turns it ON himself, with a
 *   visible stop (turning it OFF stops everything).
 * - Installed launchable apps list: tap → one confirmation → open.
 * - Service actions (need service ON): click by text, type into
 *   focused field, scroll, back, home — each result spoken + shown.
 * Screen content is never sent anywhere; actions run on-device only.
 */
@Composable
fun AutoScreen(voice: VoiceEngine) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var confirmApp by remember { mutableStateOf<LaunchableApp?>(null) }
    var clickText by remember { mutableStateOf("") }
    var typeText by remember { mutableStateOf("") }
    val apps = remember { VerifiedAppLauncher.listApps(context) }
    val shown = if (query.isBlank()) apps.take(50)
    else apps.filter { it.label.lowercase().contains(query.lowercase()) }.take(50)
    val serviceOn = FloraAutomationService.isEnabled()
    val scope = rememberCoroutineScope()

    fun say(message: String) {
        note = message
        voice.speak(message)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                if (serviceOn) "Automation service: ON hai."
                else "Automation service: OFF hai — click/scroll/type ke liye ON karna hoga."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }) { Text("Accessibility settings kholo (aap khud ON karenge)") }
            Text(
                "ON karne par Flora button dabana, scroll aur field bharna kar " +
                    "sakegi — sirf aapke bolne/confirm karne par, sirf is phone par. " +
                    "OFF karte hi sab ruk jayega."
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Service actions (service ON hone par)")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = clickText, onValueChange = { clickText = it },
                label = { Text("Button ka text (jaise Submit, Login)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val service = FloraAutomationService.instance
                if (service == null) {
                    say("Automation service OFF hai, pehle ON kijiye.")
                } else if (service.clickText(clickText)) {
                    say("'${clickText}' dab gaya.")
                } else {
                    say("'${clickText}' screen par nahi mila, guess-click nahi kiya.")
                }
            }) { Text("Text wala button dabao") }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = typeText, onValueChange = { typeText = it },
                label = { Text("Focused field me type karne wala text") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val service = FloraAutomationService.instance
                if (service == null) {
                    say("Automation service OFF hai, pehle ON kijiye.")
                } else if (service.typeIntoFocused(typeText)) {
                    say("Text bhar diya gaya.")
                } else {
                    say("Koi editable field focused nahi hai, pehle field par tap kijiye.")
                }
            }) { Text("Field me type karo") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val service = FloraAutomationService.instance
                if (service == null) say("Automation service OFF hai, pehle ON kijiye.")
                else if (service.scroll(true)) say("Neeche scroll ho gaya.")
                else say("Scroll nahi hua.")
            }) { Text("Scroll down") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val service = FloraAutomationService.instance
                if (service == null) say("Automation service OFF hai, pehle ON kijiye.")
                else if (service.goBack()) say("Back ho gaya.") else say("Back nahi hua.")
            }) { Text("Back") }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Koi bhi app kholo (tap → ek confirmation → open)")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                label = { Text("App ka naam dhoondhiye") },
                modifier = Modifier.fillMaxWidth()
            )
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(shown) { app ->
            Button(onClick = { confirmApp = app }) { Text(app.label) }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    confirmApp?.let { app ->
        AlertDialog(
            onDismissRequest = { confirmApp = null },
            title = { Text("App kholu?") },
            text = { Text("${app.label} kholna hai?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmApp = null
                    scope.launch {
                        when (VerifiedAppLauncher.openVerified(context, app)) {
                            VerifiedAppLauncher.OpenResult.Opened -> say("${app.label} khol diya.")
                            VerifiedAppLauncher.OpenResult.AlreadyOpen -> say("${app.label} pehle se khula hua hai.")
                            VerifiedAppLauncher.OpenResult.NoVerifier -> say("${app.label} kholna verify nahi kar paya — Accessibility service ON karna hoga, Settings me.")
                            else -> say("${app.label} nahi khul paya.")
                        }
                    }
                }) { Text("Haan, kholo") }
            },
            dismissButton = {
                TextButton(onClick = { confirmApp = null }) { Text("Ruko") }
            }
        )
    }
}
