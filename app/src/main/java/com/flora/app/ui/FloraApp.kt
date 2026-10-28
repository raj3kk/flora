package com.flora.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.flora.app.assistant.VisionClient
import com.flora.app.assistant.VoiceEngine
import com.google.firebase.messaging.FirebaseMessaging
import com.flora.app.automation.AppLauncher
import com.flora.app.automation.AutoActions
import com.flora.app.core.FloraConfig
import com.flora.app.data.FloraPreferences
import com.flora.app.data.LicenceClient
import com.flora.app.data.LicenceStatus
import com.flora.app.data.DocsStore
import com.flora.app.data.MemoriesStore
import com.flora.app.data.JournalStore
import com.flora.app.data.SosStore
import com.flora.app.tools.SearchTools
import com.flora.app.security.BiometricTemplates
import com.flora.app.security.GuardianGate
import com.flora.app.security.PinVault
import com.flora.app.wakeword.WakeWordMFCC
import com.flora.app.wakeword.WakeWordRecorder
import com.flora.app.wakeword.WakeWordService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

private enum class Screen { CHAT, SETTINGS, MEMORIES, SCAN, TOOLS, SOS, FACE, DOCS, CONNECT, PRIVACY, TERMS, AUTO, ROUTINES, LIVE, DRIVING, JOURNAL }

private data class ChatMsg(val text: String, val fromUser: Boolean)

/** Flora theme colors (v1.9): Flora ke apne 3 themes, sirf colors — koi asset copy nahi. */
private fun floraColorScheme(theme: String): androidx.compose.material3.ColorScheme = when (theme) {
    "dark" -> androidx.compose.material3.darkColorScheme()
    "warm" -> androidx.compose.material3.lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF7C4A03),
        secondary = androidx.compose.ui.graphics.Color(0xFF6B5A2E),
        tertiary = androidx.compose.ui.graphics.Color(0xFF8D6E00),
        background = androidx.compose.ui.graphics.Color(0xFFFFF8E7),
        surface = androidx.compose.ui.graphics.Color(0xFFFFF3D6)
    )
    else -> androidx.compose.material3.lightColorScheme()
}
private fun personaPrefix(persona: String): String = when (persona) {
    "saathi" -> "Dostana, chhote aur hausla dene wale tone me jawab do. "
    "guru" -> "Step-by-step, to-the-point, padhai/kaam par focus karke jawab do. "
    else -> ""
}

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
    journalStore: JournalStore,
    searchTools: SearchTools,
    sosStore: SosStore,
    docsStore: DocsStore,
    voiceResult: MutableState<String>,
    onMicRequest: () -> Unit,
) {
    var screen by remember { mutableStateOf(Screen.CHAT) }
    // Theme state (v1.9): states block me, launcher se pehle.
    var theme by remember { mutableStateOf("light") }
    var messages by remember { mutableStateOf(listOf(ChatMsg("Namaste! Main Flora hun. Kya madad kar sakti hun?", false))) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    // Attachments (v1.7, local — journal PASS ke baad push): ye states
    // launcher se PEHLE declare hain (order trap se bachne ke liye).
    var attachedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var attachedName by remember { mutableStateOf("") }
    var attachedMime by remember { mutableStateOf("") }
    var licence by remember { mutableStateOf<LicenceStatus?>(null) }
    var faceLockOn by remember { mutableStateOf(false) }
    var chatUnlocked by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var lockNote by remember { mutableStateOf("") }
    var voiceCallConfirm by remember { mutableStateOf<String?>(null) }
    var voiceSmsConfirm by remember { mutableStateOf<Pair<String, String>?>(null) }
    var voiceAppConfirm by remember { mutableStateOf<com.flora.app.automation.LaunchableApp?>(null) }
    var pendingVoiceCallRequest by remember { mutableStateOf<String?>(null) }
    var pendingVoiceSmsRequest by remember { mutableStateOf<Pair<String, String>?>(null) }
    val appContext = LocalContext.current
    val voicePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.CALL_PHONE] == true) {
            pendingVoiceCallRequest?.let { voiceCallConfirm = it }
            pendingVoiceCallRequest = null
        }
        if (grants[Manifest.permission.SEND_SMS] == true) {
            pendingVoiceSmsRequest?.let { voiceSmsConfirm = it }
            pendingVoiceSmsRequest = null
        }
    }
    val scope = rememberCoroutineScope()
    val vision = remember { VisionClient() }
    val attachLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            attachedUri = uri
            attachedMime = try {
                appContext.contentResolver.getType(uri) ?: ""
            } catch (e: Exception) {
                ""
            }
            attachedName = uri.lastPathSegment ?: "file"
            messages = messages + ChatMsg(
                "Flora: File attach ho gayi: $attachedName. Ab sawaal likh kar Bhejo dabaiye.", false
            )
        }
    }
    /**
     * Licence auto-refresh (v1.4 gating, local-only abhi — push v1.4
     * verdict ke baad hoga): app khulte hi, agar Supabase anon key
     * set hai to server se plan verify karo. Fail ho to licence null
     * hi rehta hai = free mana jaata hai (safe default); talk-limit
     * tab bhi enforce hota hai. Manual Settings check waisa hi hai.
     */
    LaunchedEffect(Unit) {
        try {
            theme = prefs.theme()
            if (prefs.supabaseAnonKey().isNotEmpty()) {
                val userId = prefs.userId()
                val androidId = Settings.Secure.getString(
                    appContext.contentResolver, Settings.Secure.ANDROID_ID
                ) ?: "flora-device"
                val deviceId = prefs.deviceId(androidId)
                licence = licenceClient.verify(userId, deviceId)
            }
            // FCM token (v2.2 local save, v2.5 server sync): app khulte
            // hi token lo, is phone par save karo, aur server ko bhi
            // bhejo taaki send-push dhoondh sake. Sync fail ho to
            // local token phir bhi rehta hai, agle open par retry.
            try {
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        scope.launch {
                            try {
                                prefs.setFcmToken(token)
                                val uid = prefs.userId()
                                val aid = Settings.Secure.getString(
                                    appContext.contentResolver,
                                    Settings.Secure.ANDROID_ID
                                ) ?: "flora-device"
                                licenceClient.syncPushToken(
                                    uid, prefs.deviceId(aid), token
                                )
                            } catch (e: Exception) {
                            }
                        }
                    }
            } catch (e: Exception) {
            }
        } catch (e: Exception) {
            // Offline/server fail: licence null = free, kaam rukta nahi.
        }
    }
    // Routine auto-run (v1.1): Routines screen se ek confirmation ke
    // baad queue yahan chalti hai — har step bol kar, bina ruke.
    // Call/SMS steps queue me nahi chalte (unke liye Chat se alag
    // confirmation chahiye); payment ka koi step routine me hai hi nahi.
    var routineQueue by remember { mutableStateOf<List<String>>(emptyList()) }
    var routineName by remember { mutableStateOf("") }
    var routineRunning by remember { mutableStateOf(false) }
    LaunchedEffect(routineQueue) {
        if (routineQueue.isEmpty()) {
            if (routineRunning) {
                routineRunning = false
                val done = "Routine '$routineName' poori ho gayi."
                messages = messages + ChatMsg("Flora: $done", false)
                voice.speak(done)
            }
            return@LaunchedEffect
        }
        val cmd = routineQueue.first()
        val lower = cmd.lowercase().trim()
        messages = messages + ChatMsg("Flora: Routine step: $cmd", false)
        voice.speak("Step: $cmd")
        when {
            lower.startsWith("open ") || lower.startsWith("kholo ") -> {
                val name = cmd.substringAfter(" ").trim()
                val match = AppLauncher.findByName(appContext, name).firstOrNull()
                if (match != null && AppLauncher.launch(appContext, match.packageName)) {
                    voice.speak("${match.label} khol diya.")
                } else {
                    voice.speak("'$name' naam ki app nahi mili, step chhod diya.")
                }
            }
            lower.startsWith("chrome me ") || lower.startsWith("website ") -> {
                val site = cmd.substringAfter("kholo", "").trim()
                    .ifEmpty { cmd.substringAfter("me", "").trim() }
                if (site.isNotBlank() && AppLauncher.openWebsite(appContext, site)) {
                    voice.speak("$site khol raha hun.")
                } else {
                    voice.speak("Website nahi khul payi, step chhod diya.")
                }
            }
            lower.startsWith("gaana bajao ") || lower.startsWith("gaana chalao ") -> {
                val song = cmd.substringAfter("bajao", "").trim()
                    .ifEmpty { cmd.substringAfter("chalao", "").trim() }
                if (song.isNotBlank()) {
                    AppLauncher.openWebsite(
                        appContext,
                        "https://www.youtube.com/results?search_query=" +
                            android.net.Uri.encode(song)
                    )
                    voice.speak("$song YouTube par khol raha hun.")
                }
            }
            lower == "photo lo" || lower == "scan karo" || lower == "camera kholo" -> {
                screen = Screen.SCAN
                voice.speak("Scan khol raha hun.")
            }
            lower == "sos" || lower == "sos bhejo" || lower == "madad" -> {
                screen = Screen.SOS
                voice.speak("SOS screen khol raha hun.")
            }
            lower == "yaadein kholo" -> {
                screen = Screen.MEMORIES
                voice.speak("Yaadein khol raha hun.")
            }
            lower.startsWith("call ") || lower.startsWith("sms ") -> {
                voice.speak("Call ya SMS step routine me nahi chalta, iske liye Chat se alag confirmation chahiye. Step chhod diya.")
            }
            else -> {
                val today = java.text.SimpleDateFormat(
                    "yyyy-MM-dd", java.util.Locale.US
                ).format(java.util.Date())
                val used = prefs.talkSecondsUsed(today)
                if (licence?.plan != "pro" && used >= 600) {
                    voice.speak("Free talk time khatam ho gaya hai, isliye ye step chhod diya.")
                } else {
                    val reply = brain.ask(cmd)
                    messages = messages + ChatMsg("Flora: $reply", false)
                    voice.speak(reply.take(500))
                }
            }
        }
        kotlinx.coroutines.delay(1500)
        routineQueue = routineQueue.drop(1)
    }


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

    androidx.compose.material3.MaterialTheme(colorScheme = floraColorScheme(theme)) {
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
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = { screen = Screen.CONNECT }) { Text("Connect") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = { screen = Screen.PRIVACY }) { Text("Privacy") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = { screen = Screen.TERMS }) { Text("Terms") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = { screen = Screen.AUTO }) { Text("Auto") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = { screen = Screen.ROUTINES }) { Text("Routines") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = { screen = Screen.LIVE }) { Text("Live Screen") }
                Button(onClick = { screen = Screen.DRIVING }) { Text("Gaadi") }
                Button(onClick = { screen = Screen.JOURNAL }) { Text("Journal") }
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
                FloraOrb(busy)
                Spacer(modifier = Modifier.height(8.dp))
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
                            // Voice command, one confirmation, full execution, spoken
                            // commentary — no stopping at intermediate steps to ask again.
                            // "call <number>" / "sms <number> <message>".
                            val lower = question.lowercase()
                            if (lower.startsWith("call ")) {
                                val number = question.substring(5).trim()
                                    .filter { it.isDigit() || it == '+' }
                                input = ""
                                if (number.length < 7) {
                                    val msg = "Number poora likhiye, jaise 'call 9876543210'."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                } else {
                                    messages = messages + ChatMsg(
                                        "Flora: Theek hai, $number par call laga raha hun — ek baar confirm kijiye.", false
                                    )
                                    voice.speak("Theek hai, call laga raha hun, ek baar confirm kijiye.")
                                    if (AutoActions.hasCallPermission(appContext)) {
                                        voiceCallConfirm = number
                                    } else {
                                        pendingVoiceCallRequest = number
                                        voicePermissionLauncher.launch(
                                            arrayOf(Manifest.permission.CALL_PHONE)
                                        )
                                    }
                                }
                                return@Button
                            }
                            if (lower.startsWith("sms ")) {
                                val rest = question.substring(4).trim()
                                val parts = rest.split(" ", limit = 2)
                                input = ""
                                if (parts.size < 2 ||
                                    parts[0].filter { it.isDigit() || it == '+' }.length < 7
                                ) {
                                    val msg = "Aise likhiye: 'sms 9876543210 aapka message'."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                } else {
                                    val number = parts[0].filter { it.isDigit() || it == '+' }
                                    val text = parts[1]
                                    messages = messages + ChatMsg(
                                        "Flora: Theek hai, $number par SMS bhej raha hun — ek baar confirm kijiye.", false
                                    )
                                    voice.speak("Theek hai, SMS bhej raha hun, ek baar confirm kijiye.")
                                    if (AutoActions.hasSmsPermission(appContext)) {
                                        voiceSmsConfirm = number to text
                                    } else {
                                        pendingVoiceSmsRequest = number to text
                                        voicePermissionLauncher.launch(
                                            arrayOf(Manifest.permission.SEND_SMS)
                                        )
                                    }
                                }
                                return@Button
                            }
                            // Voice: "open <app>" / "kholo <app>" — ek confirmation, phir app khul jayegi.
                            if (lower.startsWith("open ") || lower.startsWith("kholo ")) {
                                val name = question.substringAfter(" ").trim()
                                input = ""
                                val matches = AppLauncher.findByName(appContext, name)
                                when {
                                    matches.isEmpty() -> {
                                        val msg = "'$name' naam ki app nahi mili, naam dobara boliye."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    }
                                    matches.size == 1 -> {
                                        val app = matches[0]
                                        messages = messages + ChatMsg(
                                            "Flora: Theek hai, ${app.label} khol raha hun — ek baar confirm kijiye.", false
                                        )
                                        voice.speak("Theek hai, ${app.label} khol raha hun, ek baar confirm kijiye.")
                                        voiceAppConfirm = app
                                    }
                                    else -> {
                                        val names = matches.take(3).joinToString(", ") { it.label }
                                        val msg = "Ek se zyada app mili: $names. Poora naam boliye ya Auto screen se chuniye."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    }
                                }
                                return@Button
                            }
                            // Voice: "chrome me <site> kholo" / "website kholo <site>".
                            if (lower.startsWith("chrome me ") || lower.startsWith("website kholo ") ||
                                lower.startsWith("website ")
                            ) {
                                val site = question.substringAfter("kholo", "")
                                    .trim().ifEmpty { question.substringAfter("me", "").trim() }
                                input = ""
                                if (site.isBlank()) {
                                    val msg = "Kaun si website kholu? Naam boliye, jaise 'chrome me example.com kholo'."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                } else if (AppLauncher.openWebsite(appContext, site)) {
                                    val msg = "$site khol raha hun."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                } else {
                                    val msg = "Website nahi khul payi."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                }
                                return@Button
                            }
                            // Voice: "gaana bajao <song>" — YouTube search kholo.
                            if (lower.startsWith("gaana bajao ") || lower.startsWith("gaana chalao ")) {
                                val song = question.substringAfter("bajao", "").trim()
                                    .ifEmpty { question.substringAfter("chalao", "").trim() }
                                input = ""
                                if (song.isBlank()) {
                                    val msg = "Kaun sa gaana bajau? Naam boliye."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                } else {
                                    AppLauncher.openWebsite(
                                        appContext,
                                        "https://www.youtube.com/results?search_query=" +
                                            android.net.Uri.encode(song)
                                    )
                                    val msg = "$song YouTube par khol raha hun."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                }
                                return@Button
                            }
                            // Voice navigation: bolte hi screen khul jayegi, beech me koi
                            // step nahi rukega. "photo lo" / "sos bhejo" / "yaadein kholo".
                            if (lower == "photo lo" || lower == "scan karo" || lower == "camera kholo") {
                                input = ""
                                screen = Screen.SCAN
                                val msg = "Scan khol raha hun, Photo lo dabate hi photo save ho jayegi."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                                return@Button
                            }
                            if (lower == "sos" || lower == "sos bhejo" || lower == "madad") {
                                input = ""
                                screen = Screen.SOS
                                val msg = "SOS khol raha hun, Auto SOS se ek confirmation me call aur SMS dono ho jayenge."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                                return@Button
                            }
                            if (lower == "yaadein kholo" || lower == "memories kholo") {
                                input = ""
                                screen = Screen.MEMORIES
                                val msg = "Yaadein khol raha hun."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                                return@Button
                            }
                            // Attachments (v1.7): file + sawaal ek saath.
                            // Image -> vision, text/md -> brain. PDF abhi
                            // supported nahi (koi PDF parser dependency nahi).
                            // Talk-limit yahan bhi lagu hota hai (free cap).
                            if (attachedUri != null) {
                                val uri = attachedUri ?: return@Button
                                val mime = attachedMime
                                val fname = attachedName
                                val q = question
                                attachedUri = null
                                attachedMime = ""
                                attachedName = ""
                                messages = messages + ChatMsg(question, true)
                                input = ""
                                busy = true
                                scope.launch {
                                    val today = java.text.SimpleDateFormat(
                                        "yyyy-MM-dd", java.util.Locale.US
                                    ).format(java.util.Date())
                                    val used = prefs.talkSecondsUsed(today)
                                    if (licence?.plan != "pro" && used >= 600) {
                                        val msg = "Aaj ka free talk time (10 minute) khatam ho gaya hai. Kal phir milega, ya Pro (₹199/month) lijiye."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                        busy = false
                                        return@launch
                                    }
                                    try {
                                        val bytes = kotlinx.coroutines.withContext(
                                            kotlinx.coroutines.Dispatchers.IO
                                        ) {
                                            appContext.contentResolver.openInputStream(uri)?.readBytes()
                                        }
                                        if (bytes == null || bytes.isEmpty()) {
                                            val msg = "File padh nahi payi, dobara attach kijiye."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        } else if (bytes.size > 5 * 1024 * 1024) {
                                            val msg = "File 5 MB se badi hai, chhoti file bhejo."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        } else if (mime.startsWith("image/")) {
                                            val key = prefs.geminiApiKey()
                                            if (key.isEmpty()) {
                                                val msg = "Image dekhne ke liye Settings me apni free Gemini key daaliye."
                                                messages = messages + ChatMsg("Flora: $msg", false)
                                                voice.speak(msg)
                                            } else {
                                                val reply = vision.askAboutImage(
                                                    if (q.isBlank()) "Is image me kya dikh raha hai, detail me batao?" else q,
                                                    bytes, key
                                                )
                                                if (reply == null) {
                                                    val msg = "Image ka jawab nahi de payi, dobara koshish kijiye."
                                                    messages = messages + ChatMsg("Flora: $msg", false)
                                                    voice.speak(msg)
                                                } else {
                                                    messages = messages + ChatMsg(reply, false)
                                                    voice.speak(reply.take(500))
                                                }
                                            }
                                        } else if (mime.startsWith("text/") || fname.endsWith(".md") || fname.endsWith(".txt")) {
                                            val text = String(bytes, Charsets.UTF_8).take(8000)
                                            val reply = brain.ask(
                                                personaPrefix(prefs.persona()) +
                                                    "File ka text:\n$text\n\nUser ka sawaal: " +
                                                    if (q.isBlank()) "Is file ke baare me batao." else q
                                            )
                                            messages = messages + ChatMsg(reply, false)
                                            voice.speak(reply.take(500))
                                        } else {
                                            val msg = "Ye file type abhi nahi padh sakti — image ya text/md file bhejo. PDF abhi supported nahi hai."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        }
                                    } catch (e: Exception) {
                                        val msg = "File padhte waqt error aaya, dobara koshish kijiye."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    }
                                    busy = false
                                }
                                return@Button
                            }
                            messages = messages + ChatMsg(question, true)
                            input = ""
                            busy = true
                            scope.launch {
                                // Free talk limit (v1.3 audit gap fix): Pro nahi
                                // to 600 sec/day ke baad brain jawab nahi dega.
                                // Licence null = free mana jaata hai (safe default).
                                val today = java.text.SimpleDateFormat(
                                    "yyyy-MM-dd", java.util.Locale.US
                                ).format(java.util.Date())
                                val used = prefs.talkSecondsUsed(today)
                                if (licence?.plan != "pro" && used >= 600) {
                                    val msg = "Aaj ka free talk time (10 minute) khatam ho gaya hai. Kal phir milega, ya Pro (₹199/month) lijiye."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                    busy = false
                                    return@launch
                                }
                                val reply = brain.ask(personaPrefix(prefs.persona()) + question)
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
                    Button(onClick = { attachLauncher.launch("*/*") }) { Text("Attach") }
                    Spacer(modifier = Modifier.padding(4.dp))
                    Button(onClick = { voice.stop() }) { Text("Stop") }
                }
                if (attachedUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Attach hai: $attachedName")
                    Button(onClick = {
                        attachedUri = null
                        attachedMime = ""
                        attachedName = ""
                    }) { Text("Attach hatao") }
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
                ScanScreen(voice)
            } else if (screen == Screen.SOS) {
                SosScreen(sosStore)
            } else if (screen == Screen.FACE) {
                FaceScreen()
            } else if (screen == Screen.DOCS) {
                DocsScreen(docsStore)
            } else if (screen == Screen.CONNECT) {
                ConnectScreen(voice, prefs)
            } else if (screen == Screen.PRIVACY) {
                PrivacyScreen()
            } else if (screen == Screen.TERMS) {
                TermsScreen()
            } else if (screen == Screen.AUTO) {
                AutoScreen(voice)
            } else if (screen == Screen.ROUTINES) {
                RoutinesScreen(voice) { routine ->
                    screen = Screen.CHAT
                    routineName = routine.name
                    routineRunning = true
                    messages = messages + ChatMsg(
                        "Flora: Routine '${routine.name}' chalu — ${routine.commands.size} steps, ek-ek karke chalenge.", false
                    )
                    voice.speak("Routine ${routine.name} chalu kar raha hun.")
                    routineQueue = routine.commands
                }
            } else if (screen == Screen.LIVE) {
                LiveScreen(voice, prefs)
            } else if (screen == Screen.DRIVING) {
                DrivingScreen(voice, prefs, onMicRequest)
            } else if (screen == Screen.JOURNAL) {
                JournalScreen(journalStore)
            } else {
                ToolsScreen(searchTools)
            }

            voiceCallConfirm?.let { number ->
                AlertDialog(
                    onDismissRequest = { voiceCallConfirm = null },
                    title = { Text("Call lagau?") },
                    text = { Text("$number par seedha call lagega.") },
                    confirmButton = {
                        TextButton(onClick = {
                            voiceCallConfirm = null
                            val ok = AutoActions.placeCall(appContext, number)
                            val msg = if (ok) "Call laga di gayi: $number" else "Call nahi lagi, permission ya number check kijiye."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        }) { Text("Haan, call lagao") }
                    },
                    dismissButton = {
                        TextButton(onClick = { voiceCallConfirm = null }) { Text("Ruko") }
                    }
                )
            }
            voiceSmsConfirm?.let { (number, text) ->
                AlertDialog(
                    onDismissRequest = { voiceSmsConfirm = null },
                    title = { Text("SMS bheju?") },
                    text = { Text("$number par ye SMS jayega: \"$text\"") },
                    confirmButton = {
                        TextButton(onClick = {
                            voiceSmsConfirm = null
                            val ok = AutoActions.sendSms(appContext, number, text)
                            val msg = if (ok) "SMS bhej diya gaya: $number" else "SMS nahi gaya, permission ya number check kijiye."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        }) { Text("Haan, bhejo") }
                    },
                    dismissButton = {
                        TextButton(onClick = { voiceSmsConfirm = null }) { Text("Ruko") }
                    }
                )
            }
            voiceAppConfirm?.let { app ->
                AlertDialog(
                    onDismissRequest = { voiceAppConfirm = null },
                    title = { Text("App kholu?") },
                    text = { Text("${app.label} kholna hai?") },
                    confirmButton = {
                        TextButton(onClick = {
                            voiceAppConfirm = null
                            val ok = AppLauncher.launch(appContext, app.packageName)
                            val msg = if (ok) "${app.label} khol diya." else "${app.label} nahi khul paya."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        }) { Text("Haan, kholo") }
                    },
                    dismissButton = {
                        TextButton(onClick = { voiceAppConfirm = null }) { Text("Ruko") }
                    }
                )
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
    var groqKey by remember { mutableStateOf("") }
    // Optional keys (v2.1, local): rotation ke second keys + optional search keys.
    var geminiKey2 by remember { mutableStateOf("") }
    var groqKey2 by remember { mutableStateOf("") }
    var tavilyKey by remember { mutableStateOf("") }
    var braveKey by remember { mutableStateOf("") }
    var serpKey by remember { mutableStateOf("") }
    var mapsKey by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var guardianMode by remember { mutableStateOf(false) }
    var adminVpa by remember { mutableStateOf("") }
    var utr by remember { mutableStateOf("") }
    var callAnnounce by remember { mutableStateOf(false) }
    var eventAnnounce by remember { mutableStateOf(false) }
    // Persona state (v1.8, local): launcher se PEHLE declare (order trap).
    var persona by remember { mutableStateOf("flora") }
    // Theme state (v1.9): persona ke saath, launcher se pehle.
    var theme by remember { mutableStateOf("light") }
    // FCM status (v2.2): token value KABHI screen par nahi dikhegi.
    var fcmStatus by remember { mutableStateOf("Push: jaanch ho rahi hai...") }
    // Wake Word state (v2.4): launcher se PEHLE declare (order trap).
    var wakeEnabled by remember { mutableStateOf(false) }
    var wakeEnrolled by remember { mutableStateOf(false) }
    var wakeStatus by remember { mutableStateOf("Wake Word: band hai") }
    var note by remember { mutableStateOf("") }
    val phonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            callAnnounce = true
            scope.launch { prefs.setCallAnnounce(true) }
        } else {
            note = "Phone permission ke bina call announce nahi chalega."
        }
    }
    // Mic permission launcher (v2.4): wake states upar declare hain.
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                wakeEnabled = true
                prefs.setWakeEnabled(true)
                context.startForegroundService(Intent(context, WakeWordService::class.java))
                wakeStatus = "Wake Word ON ho gaya — sun raha hun."
            }
        } else {
            wakeStatus = "Mic permission ke bina Wake Word nahi chalega."
        }
    }

    LaunchedEffect(Unit) {
        anonKey = prefs.supabaseAnonKey()
        geminiKey = prefs.geminiApiKey()
        groqKey = prefs.groqApiKey()
        geminiKey2 = prefs.geminiApiKey2()
        groqKey2 = prefs.groqApiKey2()
        tavilyKey = prefs.tavilyApiKey()
        braveKey = prefs.braveApiKey()
        serpKey = prefs.serpApiKey()
        mapsKey = prefs.mapsApiKey()
        consent = prefs.automationConsent()
        guardianMode = prefs.guardianMode()
        adminVpa = prefs.adminUpiVpa()
        utr = prefs.lastUtr()
        callAnnounce = prefs.callAnnounce()
        eventAnnounce = prefs.eventAnnounce()
        persona = prefs.persona()
        theme = prefs.theme()
        fcmStatus = if (prefs.fcmToken().isNotEmpty()) {
            "Push: chalu (token sirf is phone par save hai)"
        } else {
            "Push: token abhi nahi mila — app dobara kholo ya internet check karo"
        }
        wakeEnabled = prefs.wakeEnabled()
        wakeEnrolled = BiometricTemplates(context).hasWake()
        wakeStatus = if (wakeEnabled) "Wake Word ON hai — sun raha hun." else "Wake Word: band hai"
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Keys — sirf is phone par save hoti hain, kabhi server par nahi bheji jaati (licence check ke alawa).")
            Spacer(modifier = Modifier.height(8.dp))
            Text(fcmStatus)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Wake Word — Hey Flora (default OFF; ON par foreground service + notification hamesha dikhegi; audio kabhi save/server par nahi jata)")
            Row {
                Text("Wake Word")
                Spacer(modifier = Modifier.padding(4.dp))
                Switch(checked = wakeEnabled, onCheckedChange = { value ->
                    scope.launch {
                        if (value) {
                            if (!BiometricTemplates(context).hasWake()) {
                                wakeStatus = "Pehle enrol karo — neeche wale button se 3 baar 'Hey Flora' bolo."
                            } else if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                wakeEnabled = true
                                prefs.setWakeEnabled(true)
                                context.startForegroundService(Intent(context, WakeWordService::class.java))
                                wakeStatus = "Wake Word ON ho gaya — sun raha hun."
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        } else {
                            wakeEnabled = false
                            prefs.setWakeEnabled(false)
                            context.stopService(Intent(context, WakeWordService::class.java))
                            wakeStatus = "Wake Word: band hai"
                        }
                    }
                })
            }
            Text(wakeStatus)
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = {
                scope.launch {
                    wakeStatus = "Enrol ho raha hai: mic ke paas 'Hey Flora' bolo (sample 1/3)..."
                    val means = mutableListOf<DoubleArray>()
                    for (i in 1..3) {
                        val audio = withContext(Dispatchers.Default) { WakeWordRecorder.recordMillis(2500) }
                        val mean = audio?.let { WakeWordMFCC.mfccMean(it) }
                        if (mean == null) {
                            wakeStatus = "Enrol fail — mic check karo aur dobara karo."
                            return@launch
                        }
                        means.add(mean)
                        if (i < 3) wakeStatus = "Sample ${i + 1}/3 ke liye 'Hey Flora' bolo..."
                    }
                    val avg = DoubleArray(12) { idx -> means.sumOf { it[idx] } / means.size }
                    val stored = BiometricTemplates(context).storeWake(FloatArray(12) { avg[it].toFloat() })
                    wakeEnrolled = stored
                    wakeStatus = if (stored) "Enrol ho gaya — ab Wake Word ON kar sakte ho." else "Enrol save fail — dobara karo."
                }
            }) { Text("Enrol: 3 baar 'Hey Flora' bolo") }
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
            OutlinedTextField(
                value = geminiKey2, onValueChange = { geminiKey2 = it },
                label = { Text("Gemini key 2 (optional, rotation ke liye)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = groqKey2, onValueChange = { groqKey2 = it },
                label = { Text("Groq key 2 (optional, rotation ke liye)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = tavilyKey, onValueChange = { tavilyKey = it },
                label = { Text("Tavily key (optional, search lane)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = braveKey, onValueChange = { braveKey = it },
                label = { Text("Brave key (optional, future lane)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = serpKey, onValueChange = { serpKey = it },
                label = { Text("SerpAPI key (optional, future lane)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = mapsKey, onValueChange = { mapsKey = it },
                label = { Text("Maps key (optional, future lane)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Rotation order: Gemini 1, Gemini 2, Groq 1, Groq 2. Key values kabhi log nahi hoti. Brave/SerpAPI/Maps keys sirf save hoti hain, unka lane abhi nahi bana hai.")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    prefs.setSupabaseAnonKey(anonKey.trim())
                    prefs.setGeminiApiKey(geminiKey.trim())
                    prefs.setGroqApiKey(groqKey.trim())
                    prefs.setGeminiApiKey2(geminiKey2.trim())
                    prefs.setGroqApiKey2(groqKey2.trim())
                    prefs.setTavilyApiKey(tavilyKey.trim())
                    prefs.setBraveApiKey(braveKey.trim())
                    prefs.setSerpApiKey(serpKey.trim())
                    prefs.setMapsApiKey(mapsKey.trim())
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
            Row {
                Text("Call announce (aane wali call ka number bol kar batana)")
                Spacer(modifier = Modifier.padding(4.dp))
                Switch(checked = callAnnounce, onCheckedChange = { value ->
                    if (value) {
                        phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                    } else {
                        callAnnounce = false
                        scope.launch { prefs.setCallAnnounce(false) }
                    }
                })
            }
            Text("Default OFF. Naam batana aur bol kar receive/reject v1.3 me (CallScreening/dialer role ke saath). Naye Android par background broadcast rok diya ja sakta hai.")
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Text("Event announce (charger lagna/nikalna, battery low)")
                Spacer(modifier = Modifier.padding(4.dp))
                Switch(checked = eventAnnounce, onCheckedChange = { value ->
                    eventAnnounce = value
                    scope.launch { prefs.setEventAnnounce(value) }
                })
            }
            Text("Default OFF.")
            Spacer(modifier = Modifier.height(12.dp))
            Text("Persona — Flora ka tone (sirf tone badlega, awaaz nahi)")
            Row {
                listOf("flora" to "Flora", "saathi" to "Saathi", "guru" to "Guru").forEach { (id, label) ->
                    Button(onClick = {
                        persona = id
                        scope.launch { prefs.setPersona(id) }
                    }) { Text(if (persona == id) "✓ $label" else label) }
                    Spacer(modifier = Modifier.padding(2.dp))
                }
            }
            Text("Flora: friendly + professional. Saathi: dostana, chhote jawab. Guru: step-by-step, to-the-point.")
            Spacer(modifier = Modifier.height(12.dp))
            Text("Theme — Flora ka rang (sirf colors, koi asset nahi)")
            Row {
                listOf("light" to "Light", "dark" to "Dark", "warm" to "Warm").forEach { (id, label) ->
                    Button(onClick = {
                        theme = id
                        scope.launch { prefs.setTheme(id) }
                    }) { Text(if (theme == id) "✓ $label" else label) }
                    Spacer(modifier = Modifier.padding(2.dp))
                }
            }
            Text("Theme badalte hi poora app us rang me dikhega. Default Light hai.")
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
            OutlinedTextField(
                value = adminVpa, onValueChange = { adminVpa = it },
                label = { Text("Admin UPI VPA (owner yahan apna VPA set karega)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    prefs.setAdminUpiVpa(adminVpa.trim())
                    note = "Admin UPI VPA save ho gaya."
                }
            }) { Text("VPA save karo") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    val vpa = prefs.adminUpiVpa()
                    if (vpa.isBlank()) {
                        note = "Pehle admin UPI VPA set kijiye, tabhi UPI payment khulega."
                    } else {
                        val userId = prefs.userId()
                        val uri = Uri.parse(
                            "upi://pay?pa=$vpa&pn=Flora&am=199&cu=INR&tn=FloraPro_$userId"
                        )
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        note = "UPI app khul rahi hai — payment ke baad UTR yahan save kijiye."
                    }
                }
            }) { Text("₹199 UPI se pay karo") }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = utr, onValueChange = { utr = it },
                label = { Text("UTR number (payment ke baad)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    prefs.setLastUtr(utr.trim())
                    note = "UTR save ho gaya (local note). Admin verify karke hi licence active karega — server verification alag step hai."
                }
            }) { Text("UTR save karo") }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Data retention: sync/memory 30 din. Licence/payment record alag se rakha jata hai.")
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
        }
    }
}
