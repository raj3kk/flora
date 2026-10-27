package com.flora.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.flora.app.assistant.BrainClient
import com.flora.app.assistant.VoiceEngine
import com.flora.app.core.FloraConfig
import com.flora.app.data.FloraPreferences
import com.flora.app.data.LicenceClient
import com.flora.app.data.LicenceStatus
import com.flora.app.security.GuardianGate
import com.flora.app.security.PinVault
import kotlinx.coroutines.launch

private enum class Screen { CHAT, SETTINGS }

private data class ChatMsg(val text: String, val fromUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloraApp(
    prefs: FloraPreferences,
    brain: BrainClient,
    voice: VoiceEngine,
    licenceClient: LicenceClient,
    pinVault: PinVault,
    guardian: GuardianGate,
    voiceResult: MutableState<String>,
    onMicRequest: () -> Unit,
) {
    var screen by remember { mutableStateOf(Screen.CHAT) }
    var messages by remember { mutableStateOf(listOf(ChatMsg("Namaste! Main Flora hun. Kya madad kar sakti hun?", false))) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var licence by remember { mutableStateOf<LicenceStatus?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(voiceResult.value) {
        val spoken = voiceResult.value
        if (spoken.isNotEmpty()) {
            input = spoken
            voiceResult.value = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Flora") })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Row {
                Button(onClick = { screen = Screen.CHAT }) { Text("Chat") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = { screen = Screen.SETTINGS }) { Text("Settings") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Free: 10 min/day, ~15 tools  •  Pro: ${FloraConfig.PRO_PRICE_TEXT}  •  " +
                    "Support: ${FloraConfig.SUPPORT_EMAIL}"
            )
            licence?.let {
                Text("Licence: ${it.plan} (${it.status})" + if (it.serverChecked) " — server se verified" else " — local mode")
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (screen == Screen.CHAT) {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(messages) { msg ->
                        Text((if (msg.fromUser) "Aap: " else "Flora: ") + msg.text)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Message likhiye ya mic dabaiye") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(
                        onClick = {
                            val question = input.trim()
                            if (question.isEmpty() || busy) return@Button
                            messages = messages + ChatMsg(question, true)
                            input = ""
                            busy = true
                            scope.launch {
                                val reply = brain.ask(question)
                                messages = messages + ChatMsg(reply, false)
                                voice.speak(reply)
                                busy = false
                            }
                        },
                        enabled = !busy
                    ) { Text(if (busy) "Soch rahi hun…" else "Bhejo") }
                    Spacer(modifier = Modifier.padding(4.dp))
                    Button(onClick = onMicRequest) { Text("Mic") }
                    Spacer(modifier = Modifier.padding(4.dp))
                    Button(onClick = { voice.stop() }) { Text("Stop") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Note: screen, camera aur calls sirf tab jab aap khud kahein — " +
                        "kabhi chupke se record nahi hota. Automation ke liye Settings me consent ON karna hoga."
                )
            } else {
                SettingsScreen(prefs, licenceClient, pinVault, guardian) { status ->
                    licence = status
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    prefs: FloraPreferences,
    licenceClient: LicenceClient,
    pinVault: PinVault,
    guardian: GuardianGate,
    onLicence: (LicenceStatus) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var anonKey by remember { mutableStateOf("") }
    var geminiKey by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        anonKey = prefs.supabaseAnonKey()
        geminiKey = prefs.geminiApiKey()
        consent = prefs.automationConsent()
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Keys — sirf is phone par save hoti hain, kabhi server par nahi bheji jaati (licence check ke alawa).")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = anonKey, onValueChange = { anonKey = it },
                label = { Text("Supabase anon key (apni project ki)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = geminiKey, onValueChange = { geminiKey = it },
                label = { Text("Gemini API key (free tier)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    prefs.setSupabaseAnonKey(anonKey.trim())
                    prefs.setGeminiApiKey(geminiKey.trim())
                    note = "Keys is phone par save ho gayi."
                }
            }) { Text("Keys save karo") }
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Text("Automation consent (manual, visible stop ke saath)")
                Spacer(modifier = Modifier.padding(4.dp))
                Switch(checked = consent, onCheckedChange = { value ->
                    consent = value
                    scope.launch { prefs.setAutomationConsent(value) }
                })
            }
            Text("Consent OFF ho to koi app khud nahi kholegi, kuch auto-post nahi hoga.")
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = pin, onValueChange = { pin = it },
                label = { Text("PIN (encrypted on-device store me hi rahega)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    if (pin.length < 4) {
                        note = "PIN kam se kam 4 digit ka rakho."
                    } else if (pinVault.store(pin)) {
                        prefs.setPinEnabled(true)
                        guardian.markPinVerified()
                        note = "PIN encrypted store me save ho gaya. Default OFF tha, ab aapne ON kiya hai."
                    } else {
                        note = "PIN save nahi hua, dobara koshish kijiye."
                    }
                    pin = ""
                }
            }) { Text("PIN set karo") }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = {
                scope.launch {
                    val userId = prefs.userId()
                    val androidId = Settings.Secure.getString(
                        context.contentResolver, Settings.Secure.ANDROID_ID
                    ) ?: "flora-device"
                    val deviceId = prefs.deviceId(androidId)
                    onLicence(licenceClient.verify(userId, deviceId))
                    note = "Licence check ho gaya (upar status dekho)."
                }
            }) { Text("Licence check karo") }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Pro activation (₹199/month): UPI deep link se payment karne ke baad " +
                    "UTR admin ko verify karwana hoga — tabhi licence active hogi. " +
                    "App khud kabhi payment approve nahi karti."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Data retention: sync/memory 30 din. Licence/payment record alag se rakha jata hai.")
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
        }
    }
}
