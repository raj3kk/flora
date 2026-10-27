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
import com.flora.app.data.DocsStore
import com.flora.app.data.MemoriesStore
import com.flora.app.data.SosStore
import com.flora.app.tools.SearchTools
import com.flora.app.security.GuardianGate
import com.flora.app.security.PinVault
import kotlinx.coroutines.launch

private enum class Screen { CHAT, SETTINGS, MEMORIES, SCAN, TOOLS, SOS, FACE, DOCS }

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
    memoriesStore: MemoriesStore,
    searchTools: SearchTools,
    sosStore: SosStore,
    docsStore: DocsStore,
    voiceResult: MutableState<String>,
    onMicRequest: () -> Unit,
) {
    var screen by remember { mutableStateOf(Screen.CHAT) }
    var messages by remember { mutableStateOf(listOf(ChatMsg("Namaste! Main Flora hun. Kya madad kar sakti hun?", false))) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var licence by remember { mutableStateOf<LicenceStatus?>(null) }
    var faceLockOn by remember { mutableStateOf(false) }
    var chatUnlocked by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var lockNote by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        faceLockOn = prefs.faceLock()
    }
    LaunchedEffect(screen) {
        if (screen == Screen.CHAT) faceLockOn = prefs.faceLock()
    }

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
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = { screen = Screen.MEMORIES }) { Text("Yaadein") }
            }
            Row {
                Button(onClick = { screen = Screen.SCAN }) { Text("Scan") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = { screen = Screen.TOOLS }) { Text("Tools") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = { screen = Screen.SOS }) { Text("SOS") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = { screen = Screen.FACE }) { Text("Face") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = { screen = Screen.DOCS }) { Text("Docs") }
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
                if (faceLockOn && !chatUnlocked) {
                    Text("Chat locked hai (Face Lock ON). PIN daal kar unlock kijiye. Face presence check se unlock nahi hota — sirf PIN se.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput, onValueChange = { pinInput = it },
                        label = { Text("PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        if (pinVault.verify(pinInput)) {
                            guardian.markPinVerified()
                            chatUnlocked = true
                            lockNote = ""
                        } else {
                            lockNote = "Galat PIN."
                        }
                        pinInput = ""
                    }) { Text("Unlock karo") }
                    if (lockNote.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(lockNote)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("PIN bhool gaye ho to Settings me naya PIN set kar sakte ho (Chat ke alawa baaki screens khuli hain).")
                } else {
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
                }
            } else if (screen == Screen.SETTINGS) {
                SettingsScreen(prefs, licenceClient, pinVault, guardian) { status ->
                    licence = status
                }
            } else if (screen == Screen.MEMORIES) {
                MemoriesScreen(memoriesStore)
            } else if (screen == Screen.SCAN) {
                ScanScreen()
            } else if (screen == Screen.SOS) {
                SosScreen(sosStore)
            } else if (screen == Screen.FACE) {
                FaceScreen()
            } else if (screen == Screen.DOCS) {
                DocsScreen(docsStore)
            } else {
                ToolsScreen(searchTools)
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
    var groqKey by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var guardianMode by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        anonKey = prefs.supabaseAnonKey()
        geminiKey = prefs.geminiApiKey()
        groqKey = prefs.groqApiKey()
        consent = prefs.automationConsent()
        guardianMode = prefs.guardianMode()
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
            OutlinedTextField(
                value = groqKey, onValueChange = { groqKey = it },
                label = { Text("Groq API key (backup brain, free tier)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    prefs.setSupabaseAnonKey(anonKey.trim())
                    prefs.setGeminiApiKey(geminiKey.trim())
                    prefs.setGroqApiKey(groqKey.trim())
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
            Row {
                Text("Guardian mode (ON = sensitive kaam ke liye PIN dobara chahiye)")
                Spacer(modifier = Modifier.padding(4.dp))
                Switch(checked = guardianMode, onCheckedChange = { value ->
                    scope.launch {
                        if (value && !prefs.pinEnabled()) {
                            note = "Guardian mode ke liye pehle PIN set karo."
                        } else {
                            guardianMode = value
                            prefs.setGuardianMode(value)
                            note = if (value) "Guardian mode ON ho gaya." else "Guardian mode OFF ho gaya."
                        }
                    }
                })
            }
            Text("Voice-match abhi is gate me nahi hai — jab tak aapke consent samples aur model step poora nahi hota, PIN hi gate hai.")
            Spacer(modifier = Modifier.height(12.dp))
            var verifyPin by remember { mutableStateOf("") }
            var faceLock by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { faceLock = prefs.faceLock() }
            OutlinedTextField(
                value = verifyPin, onValueChange = { verifyPin = it },
                label = { Text("PIN verify karo (session unlock ke liye)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (pinVault.verify(verifyPin)) {
                    guardian.markPinVerified()
                    note = "PIN sahi hai — is session ke liye unlock ho gaya."
                } else {
                    note = "Galat PIN."
                }
                verifyPin = ""
            }) { Text("PIN verify karo") }
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Text("Face Lock (ON = Chat PIN verify ke bina nahi khulega)")
                Spacer(modifier = Modifier.padding(4.dp))
                Switch(checked = faceLock, onCheckedChange = { value ->
                    scope.launch {
                        if (value && !prefs.pinEnabled()) {
                            note = "Face Lock ke liye pehle PIN set karo."
                        } else {
                            faceLock = value
                            prefs.setFaceLock(value)
                            note = if (value) "Face Lock ON ho gaya." else "Face Lock OFF ho gaya."
                        }
                    }
                })
            }
            Text("Face screen sirf presence check karti hai — unlock hamesha PIN se hota hai, chehre se nahi.")
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
