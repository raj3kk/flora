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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import android.provider.Settings
import com.flora.app.automation.AppAutomation
import com.flora.app.automation.AutoModeNotifier
import com.flora.app.automation.FloraAutomationService
import com.flora.app.data.AutoLogStore
import com.flora.app.tools.ImageTools
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
import com.flora.app.automation.ContactLookup
import com.flora.app.automation.ContactResolver
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
import android.content.ComponentName
import android.os.PowerManager
import androidx.core.content.ContextCompat

private enum class Screen { CHAT, SETTINGS, MEMORIES, SCAN, TOOLS, SOS, FACE, DOCS, CONNECT, PRIVACY, TERMS, AUTO, ROUTINES, LIVE, DRIVING, JOURNAL, PERMISSIONS, LICENCE, AUTO_LOG, MODELS, VOICE_ENROL, NOTIFICATIONS }

/** Dropdown menu + title ke liye har screen ka dikhne wala naam. */
private fun screenTitle(screen: Screen): String = when (screen) {
    Screen.CHAT -> "Chat"
    Screen.PERMISSIONS -> "Permissions"
    Screen.LICENCE -> "Licence"
    Screen.AUTO_LOG -> "Auto Log"
    Screen.SETTINGS -> "Settings"
    Screen.MEMORIES -> "Yaadein"
    Screen.SCAN -> "Scan"
    Screen.TOOLS -> "Tools"
    Screen.SOS -> "SOS"
    Screen.FACE -> "Face"
    Screen.DOCS -> "Docs"
    Screen.CONNECT -> "Connect"
    Screen.PRIVACY -> "Privacy"
    Screen.TERMS -> "Terms"
    Screen.AUTO -> "Auto"
    Screen.ROUTINES -> "Routines"
    Screen.LIVE -> "Live Screen"
    Screen.DRIVING -> "Gaadi"
    Screen.JOURNAL -> "Journal"
    Screen.MODELS -> "Models"
    Screen.VOICE_ENROL -> "Awaaz sikhao"
    Screen.NOTIFICATIONS -> "Notifications"
}

private data class ChatMsg(val text: String, val fromUser: Boolean)

/** Clarification pending: naam ambiguous tha ya mila nahi, user
 *  ke jawab ka intezar hai. learnName = jawab milne par jo
 *  naam seekhna hai (aksar poochha gaya naam hi). */
private data class PendingContact(
    val name: String,
    val candidates: List<com.flora.app.automation.ContactLookup.ContactMatch>,
    val action: String, // "call" ya "sms"
    val text: String, // sms ka text, call par ""
)

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
    // Romantic (v2.17, user order): pyaar bhara, caring saathi ka tone.
    // Halki flirt theek hai; koi ashleel ya explicit baat nahi.
    "romantic" -> "Pyaar bhare, romantic aur caring tone me jawab do — jaise koi kareebi saathi baat karta hai. Pyaar bhare shabd theek hain, par koi ashleel ya explicit baat nahi karni. "
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
    // Continuous mic (v2.14): MainActivity ka listener + state.
    voiceCommand: MutableState<String>,
    listeningState: MutableState<Boolean>,
    speakingState: MutableState<Boolean>,
    onContinuousStart: () -> Unit,
    onContinuousStop: () -> Unit,
) {
    var screen by remember { mutableStateOf(Screen.CHAT) }
    // Continuous mic (v2.14): toggle state + sendChat holder (local
    // fun baad me declare hota hai, isliye effect holder se call
    // karta hai — hook order ke liye effect yahan, call holder se).
    var continuousOn by remember { mutableStateOf(false) }
    // Barge-in controller (v2.22, chunk 7): TTS ke dauraan user
    // beech me bole to Deepgram live stream se suno. Mic
    // continuous listener ka ho to shuru hi nahi hoga.
    val bargeIn = remember { com.flora.app.voice.BargeInController(appContext) }
    // Voice-first Baat view (v2.15): default ON — baat-cheet me
    // likha hua transcript nahi dikhega, sirf orb + mic. Chat
    // sirf tab jab user khud "Chat kholo" dabaye.
    var voiceView by remember { mutableStateOf(true) }
    val sendChatHolder = remember { arrayOfNulls<((String) -> Unit)>(1) }
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
    var pendingVoiceUnlock by remember { mutableStateOf(false) }
    var voiceUnlockNote by remember { mutableStateOf("") }
    var voiceCallConfirm by remember { mutableStateOf<String?>(null) }
    var voiceSmsConfirm by remember { mutableStateOf<Pair<String, String>?>(null) }
    var voiceAppConfirm by remember { mutableStateOf<com.flora.app.automation.LaunchableApp?>(null) }
    // Auto mode (v2.17 switch; v2.18 user order se DEFAULT ON):
    // ON par call/SMS/app bina har baar poochhe chalenge, har kaam
    // Auto Log me dikhega.
    var autoMode by remember { mutableStateOf(false) }
    var pendingVoiceCallRequest by remember { mutableStateOf<String?>(null) }
    var pendingVoiceSmsRequest by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingContact by remember { mutableStateOf<PendingContact?>(null) }
    val appContext = LocalContext.current
    // appContext ke BAAD (declaration order — pehle wali galti se CI fail hua tha).
    val autoLogStore = remember { AutoLogStore(appContext) }
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
    // Setup wizard (v2.13): states — baaki states ke saath, upar
    // declare (wizard UI neeche inhe use karega).
    var showGate by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
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
        // Setup wizard (v2.13): pehli baar app khulte hi wizard
        // dikhega; wizard poora hone ke baad dobara nahi.
        try { showGate = !prefs.permissionsGateDone() } catch (e: Exception) { }
        try {
            theme = prefs.theme()
            try { voice.persona = prefs.persona() } catch (e: Exception) { }
            try { voiceView = prefs.voiceView() } catch (e: Exception) { }
            // Auto mode (v2.17): ON tha to notification wapas dikhao.
            try {
                autoMode = prefs.autoMode()
                if (autoMode) AutoModeNotifier.show(appContext)
            } catch (e: Exception) { }
            if (prefs.supabaseAnonKey().isNotEmpty()) {
                val userId = prefs.effectiveUserId()
                val androidId = Settings.Secure.getString(
                    appContext.contentResolver, Settings.Secure.ANDROID_ID
                ) ?: "flora-device"
                val deviceId = prefs.deviceId(androidId)
                licence = licenceClient.verify(userId, deviceId)
            }
            // 30-din retention (v2.9): app-start par journal +
            // memories ki purani date-wali entries auto-delete.
            // Fail ho to app-start nahi rukega; bina date wali
            // entries isse kabhi delete nahi hoti.
            try {
                val cutoff = System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L
                memoriesStore.pruneOlderThan(cutoff)
                journalStore.pruneOlderThan(cutoff)
                autoLogStore.pruneOlderThan(cutoff)
            } catch (e: Exception) {
            }
            // FCM token (v2.2 local save, v2.5 server sync): app khulte            // hi token lo, is phone par save karo, aur server ko bhi
            // bhejo taaki send-push dhoondh sake. Sync fail ho to
            // local token phir bhi rehta hai, agle open par retry.
            try {
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        scope.launch {
                            try {
                                prefs.setFcmToken(token)
                                val uid = prefs.effectiveUserId()
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
    // Call/SMS steps: Auto Mode ON (v2.18 default) ho to seedha
    // chalenge + log honge; OFF ho to pehle wala skip rule.
    // Payment ka koi step routine me hai hi nahi.
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
                // Auto Mode (v2.18, default ON): routine ke call/SMS
                // steps bhi seedha chalenge + log honge. Auto Mode OFF
                // ho to pehle wala skip rule wapas (Chat se alag
                // confirmation chahiye).
                if (!autoMode) {
                    voice.speak("Call ya SMS step routine me nahi chalta, iske liye Chat se alag confirmation chahiye. Step chhod diya.")
                } else if (lower.startsWith("call ")) {
                    val number = cmd.substring(5).trim().filter { it.isDigit() || it == '+' }
                    if (number.length >= 7 && AutoActions.hasCallPermission(appContext)) {
                        val ok = AutoActions.placeCall(appContext, number)
                        voice.speak(if (ok) "Routine call laga di gayi: $number" else "Routine call nahi lagi.")
                        scope.launch {
                            try { autoLogStore.add("Routine call (auto): $number -> ${if (ok) "ho gaya" else "fail"}") } catch (e: Exception) { }
                        }
                    } else {
                        voice.speak("Call permission ya number nahi mila, step chhod diya.")
                    }
                } else {
                    val rest = cmd.substring(4).trim()
                    val parts = rest.split(" ", limit = 2)
                    if (parts.size < 2) {
                        voice.speak("SMS ka format samajh nahi aaya, step chhod diya.")
                    } else {
                        val number = parts[0].filter { it.isDigit() || it == '+' }
                        if (number.length >= 7 && AutoActions.hasSmsPermission(appContext)) {
                            val ok = AutoActions.sendSms(appContext, number, parts[1])
                            voice.speak(if (ok) "Routine SMS bhej diya gaya: $number" else "Routine SMS nahi gaya.")
                            scope.launch {
                                try { autoLogStore.add("Routine SMS (auto): $number -> ${if (ok) "ho gaya" else "fail"}") } catch (e: Exception) { }
                            }
                        } else {
                            voice.speak("SMS permission ya number nahi mila, step chhod diya.")
                        }
                    }
                }
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
        // Continuous mic toggle ki shuruati haalat prefs se.
        try { continuousOn = prefs.continuousMic() } catch (e: Exception) { }
        // Barge-in hooks (v2.22, chunk 7): MainActivity ke hooks
        // ko OVERWRITE nahi karna — chain karo (pehle unka kaam,
        // phir barge-in). TTS shuru hote hi arm, khatam hote hi
        // disarm. Continuous mic ON ho to mic uska hai, isliye
        // barge-in shuru hi nahi hota.
        val prevStart = voice.onSpeechStart
        val prevDone = voice.onSpeechDone
        voice.onSpeechStart = {
            try { prevStart?.invoke() } catch (e: Exception) { }
            scope.launch {
                try {
                    if (continuousOn) return@launch
                    val key = prefs.deepgramApiKey()
                    if (key.isEmpty()) return@launch
                    bargeIn.start(key) { transcript ->
                        scope.launch {
                            bargeIn.stop()
                            voice.stop()
                            sendChatHolder[0]?.invoke(transcript)
                        }
                    }
                } catch (e: Exception) {
                }
            }
        }
        voice.onSpeechDone = {
            try { prevDone?.invoke() } catch (e: Exception) { }
            try { bargeIn.stop() } catch (e: Exception) { }
        }
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

    // Continuous mic (v2.14): suni hui baat seedha sendChat ko —
    // user ko Bhejo dabana nahi padta. busy ho to us baat ko chhod
    // do (dobara sunna TTS-done par phir shuru hoga).
    LaunchedEffect(voiceCommand.value) {
        val spoken = voiceCommand.value
        if (spoken.isNotEmpty()) {
            voiceCommand.value = ""
            if (!busy) sendChatHolder[0]?.invoke(spoken)
        }
    }

    // Setup wizard (v2.13): gate khula ho to sirf wizard dikhega;
    // baaki UI tabhi banega jab wizard poora ho. Saare remember /
    // launcher / effect is return se PEHLE hain, isliye hook order
    // nahi tootega.
    if (showGate) {
        androidx.compose.material3.MaterialTheme(colorScheme = floraColorScheme(theme)) {
            SetupWizard(
                onDone = {
                    scope.launch {
                        try { prefs.setPermissionsGateDone(true) } catch (e: Exception) { }
                        showGate = false
                        // Continuous mic (v2.14): wizard poora hote hi,
                        // mic mila ho to sunna apne aap shuru — mute
                        // dabane tak. Mute ki pasand prefs me rehti hai.
                        try {
                            val micOk = ContextCompat.checkSelfPermission(
                                appContext, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (prefs.continuousMic() && micOk) {
                                continuousOn = true
                                onContinuousStart()
                            }
                        } catch (e: Exception) { }
                    }
                },
            )
        }
        return
    }

    androidx.compose.material3.MaterialTheme(colorScheme = floraColorScheme(theme)) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (screen == Screen.CHAT && voiceView) "Flora" else "Flora — " + screenTitle(screen)) },
                actions = {
                    TextButton(onClick = { menuOpen = true }) { Text("Menu ▾") }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            enabled = false,
                            text = { Text("— Baat-cheet —") },
                            onClick = { },
                        )
                        DropdownMenuItem(
                            text = { Text("Chat") },
                            onClick = { screen = Screen.CHAT; menuOpen = false },
                        )
                        DropdownMenuItem(
                            enabled = false,
                            text = { Text("— Tools —") },
                            onClick = { },
                        )
                        DropdownMenuItem(
                            text = { Text("Scan") },
                            onClick = { screen = Screen.SCAN; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Tools") },
                            onClick = { screen = Screen.TOOLS; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Docs") },
                            onClick = { screen = Screen.DOCS; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Live Screen") },
                            onClick = { screen = Screen.LIVE; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Models") },
                            onClick = { screen = Screen.MODELS; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Gaadi") },
                            onClick = { screen = Screen.DRIVING; menuOpen = false },
                        )
                        DropdownMenuItem(
                            enabled = false,
                            text = { Text("— Meri cheezein —") },
                            onClick = { },
                        )
                        DropdownMenuItem(
                            text = { Text("Yaadein") },
                            onClick = { screen = Screen.MEMORIES; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Journal") },
                            onClick = { screen = Screen.JOURNAL; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("SOS") },
                            onClick = { screen = Screen.SOS; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Face") },
                            onClick = { screen = Screen.FACE; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Awaaz sikhao") },
                            onClick = { screen = Screen.VOICE_ENROL; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Notifications") },
                            onClick = { screen = Screen.NOTIFICATIONS; menuOpen = false },
                        )
                        DropdownMenuItem(
                            enabled = false,
                            text = { Text("— Setup —") },
                            onClick = { },
                        )
                        DropdownMenuItem(
                            text = { Text("Permissions") },
                            onClick = { screen = Screen.PERMISSIONS; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Licence / Pro") },
                            onClick = { screen = Screen.LICENCE; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Auto Log") },
                            onClick = { screen = Screen.AUTO_LOG; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = { screen = Screen.SETTINGS; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Connect") },
                            onClick = { screen = Screen.CONNECT; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Auto") },
                            onClick = { screen = Screen.AUTO; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Routines") },
                            onClick = { screen = Screen.ROUTINES; menuOpen = false },
                        )
                        DropdownMenuItem(
                            enabled = false,
                            text = { Text("— Kanooni —") },
                            onClick = { },
                        )
                        DropdownMenuItem(
                            text = { Text("Privacy") },
                            onClick = { screen = Screen.PRIVACY; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Terms") },
                            onClick = { screen = Screen.TERMS; menuOpen = false },
                        )
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            if (!(screen == Screen.CHAT && voiceView)) {
                Text(
                    "Free: 10 min/day, app me maujood saare tools  •  Pro: ${FloraConfig.PRO_PRICE_TEXT}  •  " +
                        "Support: ${FloraConfig.SUPPORT_EMAIL}"
                )
                licence?.let {
                    Text("Licence: ${it.plan} (${it.status})" + if (it.serverChecked) " — server se verified" else " — local mode")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (screen == Screen.CHAT) {
                // Due jobs (v2.22, chunk 4): alarm par flag laga tha —
                // CHAT khulte hi routine chalegi (Auto Mode ON ho to
                // khud, warna sirf batayegi). Ek se zyada due hon to
                // sabke steps ek queue me jud jate hain.
                LaunchedEffect(screen) {
                    try {
                        val duePrefs = appContext.getSharedPreferences("flora_job_due", android.content.Context.MODE_PRIVATE)
                        val due = duePrefs.getStringSet("due", emptySet())?.toList() ?: emptyList()
                        if (due.isNotEmpty()) {
                            duePrefs.edit().putStringSet("due", emptySet()).apply()
                            val routines = com.flora.app.data.RoutineStore.load(appContext)
                            val found = due.mapNotNull { name -> routines.firstOrNull { it.name == name } }
                            if (found.isNotEmpty()) {
                                if (autoMode) {
                                    routineName = found.joinToString(", ") { it.name }
                                    routineQueue = found.flatMap { it.commands }
                                    routineRunning = true
                                    val msg = "Waqt ho gaya — routine '${routineName}' chalu kar raha hun."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                } else {
                                    val msg = "Routine '${found.joinToString(", ") { it.name }}' ka waqt ho gaya hai — chalane ke liye Routines screen se chalao (Auto Mode OFF hai)."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                }
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
                if (faceLockOn && !chatUnlocked) {
                    Text("Chat locked hai (Face Lock ON). PIN daal kar unlock kijiye. Face presence check se unlock nahi hota — sirf PIN se.")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (pendingVoiceUnlock) {
                        // PIN sahi ho gaya; ab awaaz ka check (v2.22).
                        // Enrolled awaaz na ho to ye screen kabhi nahi aati.
                        Text("PIN sahi hai. Ab apni awaaz sunao (3 second) — awaaz milne par hi chat khulegi.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            voiceUnlockNote = "Sun raha hun, 3 second bolo..."
                            scope.launch {
                                val pcm = com.flora.app.voice.VoiceRecorder.recordSeconds(appContext, 3)
                                if (pcm == null) {
                                    voiceUnlockNote = "Recording nahi ho payi — Mic permission Permissions screen se do."
                                } else {
                                    val result = com.flora.app.voice.VoiceMatcher.match(appContext, pcm, prefs)
                                    if (result.matched) {
                                        guardian.markVoiceVerified()
                                        pendingVoiceUnlock = false
                                        chatUnlocked = true
                                        lockNote = ""
                                        voiceUnlockNote = ""
                                        voice.speak("Awaaz mil gayi, chat khol raha hun.")
                                    } else if (!result.available) {
                                        voiceUnlockNote = "Voice model abhi taiyaar nahi hai — Models screen se voice-encoder download karo."
                                    } else if (!result.enrolled) {
                                        voiceUnlockNote = "Koi awaaz enrolled nahi hai — Menu se 'Awaaz sikhao' me enrol karo."
                                    } else {
                                        voiceUnlockNote = "Awaaz nahi mili. Dobara koshish karo."
                                    }
                                }
                            }
                        }) { Text("Awaaz sunao (3 second)") }
                        if (voiceUnlockNote.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(voiceUnlockNote)
                        }
                    } else {
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
                                // Voice enrolled + matcher available ho to
                                // PIN ke baad awaaz ka check bhi lagega.
                                val voiceNeeded = try {
                                    com.flora.app.voice.VoiceVault(appContext).hasEnrolled() &&
                                        com.flora.app.voice.VoiceMatcher.isAvailable(appContext)
                                } catch (e: Exception) {
                                    false
                                }
                                if (voiceNeeded) {
                                    pendingVoiceUnlock = true
                                    voiceUnlockNote = ""
                                    lockNote = "PIN sahi hai, ab awaaz se confirm karo."
                                } else {
                                    chatUnlocked = true
                                    lockNote = ""
                                }
                            } else {
                                lockNote = "Galat PIN."
                            }
                            pinInput = ""
                        }) { Text("Unlock karo") }
                    }
                    if (lockNote.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(lockNote)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("PIN bhool gaye ho to Settings me naya PIN set kar sakte ho (Chat ke alawa baaki screens khuli hain).")
                } else {
                FloraOrb(busy)
                Spacer(modifier = Modifier.height(8.dp))
                // Baat view (v2.15): transcript nahi dikhega — sirf
                // orb + mic, taaki baat-cheet lage, chat nahi.
                if (!voiceView) {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(messages) { msg ->
                        Text((if (msg.fromUser) "Aap: " else "Flora: ") + msg.text)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
                }
                if (!voiceView) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Message likhiye ya mic dabaiye") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                }
                // App automation flows (v2.16, user order):
                // "image banao X" → ChatGPT app kholo → prompt bharo →
                // generate. "X install karo" → Play Store se install →
                // kholo. Dono me Flora bol kar batayega; "ruk" par
                // Stop dabana kaafi hai. Paid app ka payment step
                // hamesha user khud Play Store me karega.
                suspend fun runChatGptImage(promptRaw: String): Boolean {
                    val prompt = promptRaw.trim()
                    val fillText = if (prompt.contains("image") ||
                        prompt.contains("photo")
                    ) prompt else "$prompt ki image banao"
                    val service = FloraAutomationService.instance
                    if (service == null) {
                        val msg = "ChatGPT me prompt bharne ke liye Accessibility ON chahiye. Permissions screen me Accessibility ON karo, phir dobara bol dena."
                        messages = messages + ChatMsg("Flora: $msg", false)
                        voice.speak(msg)
                        try {
                            appContext.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        } catch (e: Exception) { }
                        return false
                    }
                    if (!AppAutomation.isInstalled(appContext, AppAutomation.CHATGPT_PKG)) {
                        voice.speak("ChatGPT app installed nahi hai, Play Store se install kar raha hun.")
                        AppAutomation.openPlayStoreDetails(appContext, AppAutomation.CHATGPT_PKG)
                        val installed = AppAutomation.waitForInstalled(
                            appContext, AppAutomation.CHATGPT_PKG, 180_000L
                        )
                        if (!installed) {
                            val msg = "ChatGPT install poora nahi hua. Play Store khula hua hai — install poora hote hi dobara 'image banao' bol dena."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                            return false
                        }
                    }
                    voice.speak("ChatGPT khol raha hun, $fillText ki image bana raha hun.")
                    if (!AppAutomation.launch(appContext, AppAutomation.CHATGPT_PKG)) {
                        val msg = "ChatGPT khol nahi paya. App khud khol kar dobara bol dena."
                        messages = messages + ChatMsg("Flora: $msg", false)
                        voice.speak(msg)
                        return false
                    }
                    delay(3000)
                    var typed = service.setTextOnFirstEditable(fillText)
                    if (!typed) {
                        service.clickAnyText("Message", "Ask anything")
                        delay(800)
                        typed = service.setTextOnFirstEditable(fillText)
                    }
                    if (!typed) {
                        val msg = "ChatGPT khul gaya hai, par prompt box nahi mila. Prompt wahan likha hua dikhe to khud bhej dena, ya dobara bol dena."
                        messages = messages + ChatMsg("Flora: $msg", false)
                        voice.speak(msg)
                        return false
                    }
                    delay(500)
                    if (!service.clickAnyText("Send", "Send message")) {
                        val msg = "Prompt ChatGPT me bhar diya hai, par Send button nahi mila — wahan Send dabate hi image banegi."
                        messages = messages + ChatMsg("Flora: $msg", false)
                        voice.speak(msg)
                        return false
                    }
                    delay(20000)
                    val msg = "Prompt ChatGPT ko bhej diya hai, image ban rahi hai — ChatGPT app me dekh lo. Ban jaate hi wahi dikh jayegi. (ChatGPT me login/subscription manga to wo tumhe khud karna hoga.)"
                    messages = messages + ChatMsg("Flora: $msg", false)
                    voice.speak(msg)
                    return true
                }

                suspend fun runInstallFlow(appNameRaw: String) {
                    val name = appNameRaw.trim()
                    if (name.isEmpty()) {
                        val msg = "Kaunsi app install karun? Aise boliye: 'WhatsApp install karo'."
                        messages = messages + ChatMsg("Flora: $msg", false)
                        voice.speak(msg)
                        return
                    }
                    val pkg = AppAutomation.resolvePackage(name)
                    if (pkg == null) {
                        voice.speak("Play Store me $name search kar raha hun — jo app chahiye us par Install dabao.")
                        AppAutomation.openPlayStoreSearch(appContext, name)
                        return
                    }
                    if (AppAutomation.isInstalled(appContext, pkg)) {
                        voice.speak("$name pehle se installed hai, khol raha hun.")
                        AppAutomation.launch(appContext, pkg)
                        return
                    }
                    val service = FloraAutomationService.instance
                    voice.speak("Play Store se $name install kar raha hun. Agar ye paid app hai to payment tumhe khud confirm karni hogi.")
                    AppAutomation.openPlayStoreDetails(appContext, pkg)
                    delay(3000)
                    if (service != null) {
                        service.clickAnyText("Install")
                        delay(2000)
                    }
                    val installed = AppAutomation.waitForInstalled(appContext, pkg, 180_000L)
                    if (installed) {
                        voice.speak("$name install ho gaya hai, khol raha hun.")
                        AppAutomation.launch(appContext, pkg)
                    } else {
                        val msg = "Play Store khula hua hai — $name par Install dabate hi install shuru ho jayega. (Install nahi dikh raha to app ka naam dobara bol dena.)"
                        messages = messages + ChatMsg("Flora: $msg", false)
                        voice.speak(msg)
                    }
                }

                fun parseInstallRequest(lower: String, question: String): String? {
                    val prefixes = listOf(
                        "install karo ", "install kar do ",
                        "download karo ", "install "
                    )
                    for (p in prefixes) {
                        if (lower.startsWith(p)) return question.substring(p.length).trim()
                    }
                    val suffixes = listOf(
                        " install karo", " install kar do", " install kar"
                    )
                    for (s in suffixes) {
                        if (lower.endsWith(s)) {
                            return question.substring(0, question.length - s.length).trim()
                        }
                    }
                    return null
                }

                // Auto Mode execute helpers (v2.17): confirm dialog ke
                // bajay seedha execute + Auto Log me entry + bol kar.
                fun autoExecuteCall(number: String) {
                    val ok = AutoActions.placeCall(appContext, number)
                    val msg = if (ok) "Call laga di gayi: $number" else "Call nahi lagi, permission ya number check kijiye."
                    messages = messages + ChatMsg("Flora: $msg", false)
                    voice.speak(msg)
                    scope.launch {
                        try { autoLogStore.add("Call (auto): $number -> ${if (ok) "ho gaya" else "fail"}") } catch (e: Exception) { }
                    }
                }

                fun autoExecuteSms(number: String, text: String) {
                    val ok = AutoActions.sendSms(appContext, number, text)
                    val msg = if (ok) "SMS bhej diya gaya: $number" else "SMS nahi gaya, permission ya number check kijiye."
                    messages = messages + ChatMsg("Flora: $msg", false)
                    voice.speak(msg)
                    scope.launch {
                        try { autoLogStore.add("SMS (auto): $number -> ${if (ok) "ho gaya" else "fail"}") } catch (e: Exception) { }
                    }
                }

                fun autoExecuteOpen(app: com.flora.app.automation.LaunchableApp) {
                    val ok = AppLauncher.launch(appContext, app.packageName)
                    val msg = if (ok) "${app.label} khol diya." else "${app.label} nahi khul paya."
                    messages = messages + ChatMsg("Flora: $msg", false)
                    voice.speak(msg)
                    scope.launch {
                        try { autoLogStore.add("App khola (auto): ${app.label} -> ${if (ok) "ho gaya" else "fail"}") } catch (e: Exception) { }
                    }
                }

                // Clarification ke baad kaam chalana (v2.22): confirm
                // ya auto flow — call/sms branches jaisa hi.
                fun runContactAction(number: String, action: String, text: String) {
                    if (action == "call") {
                        if (autoMode && AutoActions.hasCallPermission(appContext)) {
                            autoExecuteCall(number)
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
                    } else {
                        if (autoMode && AutoActions.hasSmsPermission(appContext)) {
                            autoExecuteSms(number, text)
                        } else {
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
                    }
                }

                // sendChat (v2.14): Bhejo button ka pipeline, ab continuous
                // mic bhi isi se auto-send karta hai. Holder ke zariye
                // upar ke LaunchedEffect se call hota hai.
                fun sendChat(questionRaw: String) {
                    val question = questionRaw.trim()
                    if (question.isEmpty() || busy) return
                    // Clarification ka jawab (v2.22): naam ambiguous tha
                    // ya mila nahi tha — user ka jawab hi action hai.
                    // Naya command (call/sms/auto mode/romantic) aaye to
                    // purana pending khatam, naya command chalega.
                    val pending = pendingContact
                    if (pending != null) {
                        val ansLower = question.lowercase()
                        val isNewCommand = ansLower.startsWith("call ") ||
                            ansLower.startsWith("sms ") ||
                            ansLower.startsWith("auto mode") ||
                            ansLower == "romantic mode" ||
                            ansLower == "normal mode"
                        if (!isNewCommand) {
                            input = ""
                            if (ansLower == "cancel" || ansLower == "chhodo" ||
                                ansLower == "rehne do" || ansLower == "chhod do"
                            ) {
                                pendingContact = null
                                val msg = "Theek hai, chhod diya."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                                return
                            }
                            scope.launch {
                                val digits = question.filter { it.isDigit() || it == '+' }
                                if (pending.candidates.isNotEmpty()) {
                                    val ordinals = mapOf(
                                        "pehla" to 0, "pehle wala" to 0, "1" to 0,
                                        "doosra" to 1, "doosre wala" to 1, "2" to 1,
                                        "teesra" to 2, "teesre wala" to 2, "3" to 2,
                                    )
                                    val byOrdinal = ordinals[ansLower]?.let { idx ->
                                        pending.candidates.getOrNull(idx)
                                    }
                                    val byName = pending.candidates.firstOrNull {
                                        it.name.equals(question, ignoreCase = true)
                                    }
                                    val chosen = byOrdinal ?: byName
                                    if (chosen != null) {
                                        pendingContact = null
                                        try { prefs.learnContact(pending.name, chosen.number) } catch (e: Exception) { }
                                        val foundDigits = chosen.number.filter { it.isDigit() || it == '+' }
                                        runContactAction(foundDigits, pending.action, pending.text)
                                    } else if (digits.length >= 7) {
                                        pendingContact = null
                                        try { prefs.learnContact(pending.name, digits) } catch (e: Exception) { }
                                        runContactAction(digits, pending.action, pending.text)
                                    } else {
                                        val names = pending.candidates.joinToString(", ") { it.name }
                                        val msg = "Samajh nahi aaya — $names me se kisko? Naam bolo ya pehla/doosra bolo."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    }
                                } else if (digits.length >= 7) {
                                    pendingContact = null
                                    try { prefs.learnContact(pending.name, digits) } catch (e: Exception) { }
                                    runContactAction(digits, pending.action, pending.text)
                                } else {
                                    when (val res = ContactResolver.resolve(appContext, prefs, question)) {
                                        is ContactResolver.Result.Resolved -> {
                                            pendingContact = null
                                            try { prefs.learnContact(pending.name, res.number) } catch (e: Exception) { }
                                            val foundDigits = res.number.filter { it.isDigit() || it == '+' }
                                            runContactAction(foundDigits, pending.action, pending.text)
                                        }
                                        is ContactResolver.Result.Ambiguous -> {
                                            pendingContact = pending.copy(candidates = res.candidates)
                                            val names = res.candidates.joinToString(", ") { it.name }
                                            val msg = "'$question' naam ke kai log mile — $names. Kisko?"
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        }
                                        else -> {
                                            val msg = "Ye naam bhi nahi mila — poora number de do, ya sahi naam batao."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        }
                                    }
                                }
                            }
                            return
                        } else {
                            pendingContact = null
                        }
                    }
                    // Voice command, one confirmation, full execution, spoken
                    // commentary — no stopping at intermediate steps to ask again.
                    // "call <number>" / "call <naam>" / "sms <number|naam> <message>".
                    // AutoMemory (v2.22): user apne baare me jo bataye, wo
                    // chupchaap yaad rakho (Memories screen me dikhega).
                    scope.launch {
                        try {
                            com.flora.app.assistant.AutoMemory.learn(memoriesStore, question)
                        } catch (e: Exception) {
                        }
                    }
                    val lower = question.lowercase()
                    // Auto Mode + persona voice commands (v2.17, user order).
                    if (lower == "auto mode on" || lower == "auto mode chalu" ||
                        lower == "auto mode chalu karo"
                    ) {
                        input = ""
                        autoMode = true
                        scope.launch {
                            try {
                                prefs.setAutoMode(true)
                                autoLogStore.add("Auto mode ON")
                            } catch (e: Exception) { }
                            AutoModeNotifier.show(appContext)
                        }
                        voice.speak("Auto mode ON ho gaya hai. Ab call, SMS aur app kholna bina har baar poochhe hoga, aur har kaam Auto Log me dikhega. Band karne ke liye 'auto mode band' bolo.")
                        return
                    }
                    if (lower == "auto mode band" || lower == "auto mode off" ||
                        lower == "auto mode band karo"
                    ) {
                        input = ""
                        autoMode = false
                        scope.launch {
                            try {
                                prefs.setAutoMode(false)
                                autoLogStore.add("Auto mode OFF")
                            } catch (e: Exception) { }
                            AutoModeNotifier.cancel(appContext)
                        }
                        voice.speak("Auto mode band ho gaya hai. Ab har kaam ke liye phir se confirmation poochhungi.")
                        return
                    }
                    if (lower == "romantic mode" || lower == "romantic persona" ||
                        lower == "romantic ho jao"
                    ) {
                        input = ""
                        scope.launch {
                            try { prefs.setPersona("romantic") } catch (e: Exception) { }
                        }
                        voice.persona = "romantic"
                        voice.speak("Romantic mode ON ho gaya hai.")
                        return
                    }
                    if (lower == "normal mode" || lower == "flora mode") {
                        input = ""
                        scope.launch {
                            try { prefs.setPersona("flora") } catch (e: Exception) { }
                        }
                        voice.persona = "flora"
                        voice.speak("Normal mode wapas aa gaya hai.")
                        return
                    }
                    if (lower.startsWith("call ")) {
                        val raw = question.substring(5).trim()
                        val number = raw.filter { it.isDigit() || it == '+' }
                        input = ""
                        if (number.length >= 7) {
                            if (autoMode && AutoActions.hasCallPermission(appContext)) {
                                autoExecuteCall(number)
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
                        } else if (raw.isNotEmpty()) {
                            // Naam se kaun (v2.22): resolver — seekha hua,
                            // exact, ek, ya poochho (kabhi chupke se pehla nahi).
                            scope.launch {
                                when (val res = ContactResolver.resolve(appContext, prefs, raw)) {
                                    is ContactResolver.Result.Resolved -> {
                                        val foundDigits = res.number.filter { it.isDigit() || it == '+' }
                                        if (foundDigits.length < 7) {
                                            val msg = "Contacts me '$raw' ka number poora nahi mila — poora number likhiye."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        } else {
                                            runContactAction(foundDigits, "call", "")
                                        }
                                    }
                                    is ContactResolver.Result.Ambiguous -> {
                                        pendingContact = PendingContact(raw, res.candidates, "call", "")
                                        val names = res.candidates.joinToString(", ") { it.name }
                                        val msg = "'$raw' naam ke kai log mile — $names. Kisko call karun? Naam bolo ya pehla/doosra bolo."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    }
                                    ContactResolver.Result.NotFound -> {
                                        pendingContact = PendingContact(raw, emptyList(), "call", "")
                                        val msg = "Contacts me '$raw' nahi mila — poora number de do, ya sahi naam batao, main yaad rakh lungi."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    }
                                    ContactResolver.Result.NoPermission -> {
                                        val msg = "Contacts permission nahi mili — Settings se Contacts permission do, ya poora number likho."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    }
                                }
                            }
                        } else {
                            val msg = "Number poora likhiye, jaise 'call 9876543210'."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        }
                        return
                    }
                    if (lower.startsWith("sms ")) {
                        val rest = question.substring(4).trim()
                        val parts = rest.split(" ", limit = 2)
                        input = ""
                        if (parts.size < 2) {
                            val msg = "Aise likhiye: 'sms 9876543210 aapka message'."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        } else {
                            val firstDigits = parts[0].filter { it.isDigit() || it == '+' }
                            val text = parts[1]
                            if (firstDigits.length >= 7) {
                                if (autoMode && AutoActions.hasSmsPermission(appContext)) {
                                    autoExecuteSms(firstDigits, text)
                                } else {
                                    messages = messages + ChatMsg(
                                        "Flora: Theek hai, $firstDigits par SMS bhej raha hun — ek baar confirm kijiye.", false
                                    )
                                    voice.speak("Theek hai, SMS bhej raha hun, ek baar confirm kijiye.")
                                    if (AutoActions.hasSmsPermission(appContext)) {
                                        voiceSmsConfirm = firstDigits to text
                                    } else {
                                        pendingVoiceSmsRequest = firstDigits to text
                                        voicePermissionLauncher.launch(
                                            arrayOf(Manifest.permission.SEND_SMS)
                                        )
                                    }
                                }
                            } else {
                                scope.launch {
                                    when (val res = ContactResolver.resolve(appContext, prefs, parts[0])) {
                                        is ContactResolver.Result.Resolved -> {
                                            val foundDigits = res.number.filter { it.isDigit() || it == '+' }
                                            if (foundDigits.length < 7) {
                                                val msg = "Contacts me '${parts[0]}' ka number poora nahi mila — poora number likhiye."
                                                messages = messages + ChatMsg("Flora: $msg", false)
                                                voice.speak(msg)
                                            } else {
                                                runContactAction(foundDigits, "sms", text)
                                            }
                                        }
                                        is ContactResolver.Result.Ambiguous -> {
                                            pendingContact = PendingContact(parts[0], res.candidates, "sms", text)
                                            val names = res.candidates.joinToString(", ") { it.name }
                                            val msg = "'${parts[0]}' naam ke kai log mile — $names. Kisko SMS karun? Naam bolo ya pehla/doosra bolo."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        }
                                        ContactResolver.Result.NotFound -> {
                                            pendingContact = PendingContact(parts[0], emptyList(), "sms", text)
                                            val msg = "Contacts me '${parts[0]}' nahi mila — poora number de do, ya sahi naam batao, main yaad rakh lungi."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        }
                                        ContactResolver.Result.NoPermission -> {
                                            val msg = "Contacts permission nahi mili — Settings se Contacts permission do, ya poora number likho."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        }
                                    }
                                }
                            }
                        }
                        return
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
                                if (autoMode) {
                                    autoExecuteOpen(app)
                                } else {
                                    messages = messages + ChatMsg(
                                        "Flora: Theek hai, ${app.label} khol raha hun — ek baar confirm kijiye.", false
                                    )
                                    voice.speak("Theek hai, ${app.label} khol raha hun, ek baar confirm kijiye.")
                                    voiceAppConfirm = app
                                }
                            }
                            else -> {
                                val names = matches.take(3).joinToString(", ") { it.label }
                                val msg = "Ek se zyada app mili: $names. Poora naam boliye ya Auto screen se chuniye."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
                        }
                        return
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
                        return
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
                        return
                    }
                    // Voice navigation: bolte hi screen khul jayegi, beech me koi
                    // step nahi rukega. "photo lo" / "sos bhejo" / "yaadein kholo".
                    if (lower == "photo lo" || lower == "scan karo" || lower == "camera kholo") {
                        input = ""
                        screen = Screen.SCAN
                        val msg = "Scan khol raha hun, Photo lo dabate hi photo save ho jayegi."
                        messages = messages + ChatMsg("Flora: $msg", false)
                        voice.speak(msg)
                        return
                    }
                    if (lower == "sos" || lower == "sos bhejo" || lower == "madad") {
                        input = ""
                        screen = Screen.SOS
                        val msg = "SOS khol raha hun, Auto SOS se ek confirmation me call aur SMS dono ho jayenge."
                        messages = messages + ChatMsg("Flora: $msg", false)
                        voice.speak(msg)
                        return
                    }
                    if (lower == "yaadein kholo" || lower == "memories kholo") {
                        input = ""
                        screen = Screen.MEMORIES
                        val msg = "Yaadein khol raha hun."
                        messages = messages + ChatMsg("Flora: $msg", false)
                        voice.speak(msg)
                        return
                    }
                    // Attachments (v1.7): file + sawaal ek saath.
                    // Image -> vision, text/md -> brain. PDF abhi
                    // supported nahi (koi PDF parser dependency nahi).
                    // Talk-limit yahan bhi lagu hota hai (free cap).
                    if (attachedUri != null) {
                        val uri = attachedUri ?: return
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
                        return
                    }
                    messages = messages + ChatMsg(question, true)
                    input = ""
                    busy = true
                    scope.launch {
                        // Image via ChatGPT (v2.16, user order):
                        // "image banao <prompt>" → ChatGPT app kholo →
                        // prompt bharo → generate. ChatGPT flow fail ho
                        // to bata kar Gallery wala tareeka (Pollinations)
                        // fallback hai — chhupke nahi.
                        if (lower.startsWith("image banao ") || lower.startsWith("photo banao ")) {
                            val prompt = question.substringAfter("banao", "").trim()
                            if (prompt.isBlank()) {
                                val msg = "Kaisi image banau? Aise boliye: 'image banao ek sher'."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            } else {
                                val ok = runChatGptImage(prompt)
                                if (!ok) {
                                    voice.speak("ChatGPT se nahi bana to Gallery wale tareeke se bana raha hun.")
                                    val saved = ImageTools.generateImage(appContext, prompt)
                                    val msg = if (saved != null) {
                                        "Image ban gayi, Gallery ke Flora folder me save ho gayi."
                                    } else {
                                        "Image nahi ban payi, internet check karke dobara boliye."
                                    }
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                }
                            }
                            busy = false
                            return@launch
                        }
                        // App install via Play Store (v2.16, user order):
                        // "X install karo" / "install karo X".
                        val installName = parseInstallRequest(lower, question)
                        if (installName != null) {
                            runInstallFlow(installName)
                            busy = false
                            return@launch
                        }
                        // Yahan ka mausam (v2.13): bina brain key ke,
                        // phone ki location se seedha Open-Meteo.
                        val weatherHere = lower == "yahan ka mausam" ||
                            lower == "mausam yahan ka" ||
                            lower.startsWith("yahan ka mausam ") ||
                            lower == "weather here"
                        if (weatherHere) {
                            val reply = searchTools.weatherHere(appContext)
                            messages = messages + ChatMsg("Flora: $reply", false)
                            voice.speak(reply)
                            busy = false
                            return@launch
                        }
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
                        // Brain-context memories (v2.22, chunk 5):
                        // user ne jo bataya tha, wo brain ko context
                        // ke roop me milega (phone par hi padha jata
                        // hai, server par save nahi hota). Khali ho to
                        // kuch nahi judta — prompt pehle jaisa hi.
                        val mems = try {
                            memoriesStore.items.first().takeLast(20)
                        } catch (e: Exception) {
                            emptyList()
                        }
                        val memCtx = if (mems.isEmpty()) "" else mems.joinToString("; ")
                        val reply = brain.ask(personaPrefix(prefs.persona()) + question, memCtx)
                        messages = messages + ChatMsg(reply, false)
                        voice.speak(reply)
                        busy = false
                    }
                }
                sendChatHolder[0] = { q -> sendChat(q) }
                Row {
                    if (!voiceView) {
                    Button(
                        onClick = {
                            val q = input.trim()
                            if (q.isEmpty() || busy) return@Button
                            sendChat(q)
                        },
                        enabled = !busy
                    ) { Text(if (busy) "Soch rahi hun…" else "Bhejo") }
                    Spacer(modifier = Modifier.padding(4.dp))
                    Button(onClick = onMicRequest) { Text("Mic") }
                    Spacer(modifier = Modifier.padding(4.dp))
                    }
                    // Continuous mic toggle (v2.14): ON = mute dabane
                    // tak sunta rahe; OFF = mic band. Mic permission
                    // na ho to wizard/settings se pehle deni hogi.
                    // Baat view me sirf emoji (koi likha hua nahi).
                    Button(onClick = {
                        if (continuousOn) {
                            continuousOn = false
                            onContinuousStop()
                        } else {
                            val micOk = ContextCompat.checkSelfPermission(
                                appContext, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (micOk) {
                                continuousOn = true
                                onContinuousStart()
                            }
                        }
                    }) { Text(if (voiceView) (if (continuousOn) "🔇" else "🎤") else (if (continuousOn) "🔇 Mute" else "🎤 Mic ON karo")) }
                    Spacer(modifier = Modifier.padding(4.dp))
                    if (!voiceView) {
                    Button(onClick = { attachLauncher.launch("*/*") }) { Text("Attach") }
                    Spacer(modifier = Modifier.padding(4.dp))
                    }
                    Button(onClick = {
                        voice.stop()
                        try { bargeIn.stop() } catch (e: Exception) { }
                        // Interrupt (v2.14): continuous mode me Stop
                        // dabana matlab user kuch kehna chahta hai —
                        // casually poochho, phir sunna apne aap shuru
                        // hoga (TTS-done par). Manual mode me sirf roko.
                        if (continuousOn) {
                            val msg = "Ruk, kuch bol rahe the?"
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        }
                    }) { Text(if (voiceView) "⏹" else "Stop") }
                    Spacer(modifier = Modifier.padding(4.dp))
                    Button(onClick = {
                        val next = !voiceView
                        voiceView = next
                        scope.launch {
                            try { prefs.setVoiceView(next) } catch (e: Exception) { }
                        }
                    }) { Text(if (voiceView) "💬" else "🎤 Baat") }
                }
                if (!voiceView) {
                if (continuousOn && listeningState.value) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("🎤 Sun raha hun… (Mute dabate hi band ho jayega)")
                }
                if (speakingState.value) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("🔊 Bol rahi hun… (rokne ke liye Stop dabao)")
                }
                if (busy && !speakingState.value) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("💭 Soch rahi hun…")
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
                }
            } else if (screen == Screen.SETTINGS) {
                SettingsScreen(prefs, licenceClient, pinVault, guardian, brain, voice, { showGate = true }, { status ->
                    licence = status
                }, autoMode, { value ->
                    autoMode = value
                    scope.launch {
                        try {
                            prefs.setAutoMode(value)
                            autoLogStore.add(if (value) "Auto mode ON" else "Auto mode OFF")
                        } catch (e: Exception) { }
                        if (value) AutoModeNotifier.show(appContext) else AutoModeNotifier.cancel(appContext)
                    }
                }, { screen = Screen.AUTO_LOG })
            } else if (screen == Screen.PERMISSIONS) {
                PermissionsScreen(onShowWizard = { showGate = true })
            } else if (screen == Screen.LICENCE) {
                LicenceScreen(prefs, licenceClient, licence, { status ->
                    licence = status
                }, onGoSettings = { screen = Screen.SETTINGS })
            } else if (screen == Screen.AUTO_LOG) {
                AutoLogScreen(autoLogStore)
            } else if (screen == Screen.MODELS) {
                ModelsScreen(voice, prefs)
            } else if (screen == Screen.VOICE_ENROL) {
                VoiceEnrolScreen(voice)
            } else if (screen == Screen.NOTIFICATIONS) {
                NotificationsScreen(voice)
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
    brain: BrainClient,
    voice: VoiceEngine,
    onShowPermissions: () -> Unit,
    onLicence: (LicenceStatus) -> Unit,
    autoMode: Boolean,
    onAutoModeChange: (Boolean) -> Unit,
    onShowAutoLog: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var anonKey by remember { mutableStateOf("") }
    var geminiKey by remember { mutableStateOf("") }
    var groqKey by remember { mutableStateOf("") }
    var deepgramKey by remember { mutableStateOf("") }
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
    var wakeThreshold by remember { mutableStateOf(0.60f) }
    // App-link state (v2.8, PREP — push v2.7 PASS ke baad): launcher
    // se PEHLE declare (order trap). Password state screen par hi
    // rahega, kabhi save/log nahi hoga.
    var linkEmail by remember { mutableStateOf("") }
    var linkPassword by remember { mutableStateOf("") }
    var linkedEmailDisplay by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    // Keys-check + background status (v2.12): launcher se PEHLE.
    var keyCheckNote by remember { mutableStateOf("") }
    var bgNote by remember { mutableStateOf("") }
    var notifGranted by remember { mutableStateOf(false) }
    var batteryIgnored by remember { mutableStateOf(false) }
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
    // Notification permission launcher (v2.12): bg states upar hain.
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
        bgNote = if (granted) {
            "Notification permission mil gaya — ab push dikhega."
        } else {
            "Notification permission nahi mila — tab tak koi notification nahi aayega."
        }
    }

    LaunchedEffect(Unit) {
        anonKey = prefs.supabaseAnonKey()
        geminiKey = prefs.geminiApiKey()
        groqKey = prefs.groqApiKey()
        deepgramKey = prefs.deepgramApiKey()
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
        wakeThreshold = prefs.wakeThreshold()
        linkedEmailDisplay = prefs.linkedEmail()
        // Background status (v2.12): system se live padha jata hai.
        notifGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        batteryIgnored = try {
            context.getSystemService(PowerManager::class.java)
                ?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } catch (e: Exception) {
            false
        }
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
            Text("Wake sensitivity — galat trigger zyada ho to 'Kam' karo, sunna mushkil ho to 'Zyada' karo.")
            Row {
                listOf(0.70f to "Kam", 0.60f to "Medium", 0.50f to "Zyada").forEach { (value, label) ->
                    Button(onClick = {
                        wakeThreshold = value
                        scope.launch { prefs.setWakeThreshold(value) }
                    }) { Text(if (wakeThreshold == value) "✓ $label" else label) }
                    Spacer(modifier = Modifier.padding(2.dp))
                }
            }
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
                value = deepgramKey, onValueChange = { deepgramKey = it },
                label = { Text("Deepgram key (optional, sachhi barge-in/STT ke liye)") },
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
            Row {
                Button(onClick = {
                    scope.launch {
                        prefs.setSupabaseAnonKey(anonKey.trim())
                        prefs.setGeminiApiKey(geminiKey.trim())
                        prefs.setGroqApiKey(groqKey.trim())
                        prefs.setDeepgramApiKey(deepgramKey.trim())
                        prefs.setGeminiApiKey2(geminiKey2.trim())
                        prefs.setGroqApiKey2(groqKey2.trim())
                        prefs.setTavilyApiKey(tavilyKey.trim())
                        prefs.setBraveApiKey(braveKey.trim())
                        prefs.setSerpApiKey(serpKey.trim())
                        prefs.setMapsApiKey(mapsKey.trim())
                        note = "Keys is phone par save ho gayi."
                    }
                }) { Text("Keys save karo") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = {
                    scope.launch {
                        keyCheckNote = "Keys check ho rahi hain..."
                        val results = try {
                            brain.checkKeys()
                        } catch (e: Exception) {
                            emptyList()
                        }
                        keyCheckNote = if (results.isEmpty()) {
                            "Check nahi ho paya — internet dekh kar dobara karo."
                        } else {
                            results.joinToString(" • ") { "${it.name}: ${it.note}" }
                        }
                    }
                }) { Text("Keys check karo") }
            }
            if (keyCheckNote.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(keyCheckNote)
            }
            Text("Save dabaye bina key kaam nahi karegi — pehle Save, phir Check. Check sirf key ki validity batata hai, key ki value kabhi screen par nahi aati.")
            Spacer(modifier = Modifier.height(12.dp))
            Text("Background aur notifications — app band hone par bhi chale")
            Text(
                "Notification permission: " + if (notifGranted) "mila hua ✓" else "nahi mila — bina iske koi push nahi dikhega"
            )
            Text(
                "Battery optimization: " + if (batteryIgnored) {
                    "band hai ✓ (background chalega)"
                } else {
                    "on hai — phone background me app rok sakta hai"
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = {
                    if (notifGranted) {
                        bgNote = "Notification permission pehle se mila hua hai."
                    } else {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) { Text("Notification permission do") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = {
                    try {
                        val pm = context.getSystemService(PowerManager::class.java)
                        if (pm != null && pm.isIgnoringBatteryOptimizations(context.packageName)) {
                            batteryIgnored = true
                            bgNote = "Battery optimization pehle se band hai."
                        } else {
                            val intent = Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:" + context.packageName),
                            )
                            context.startActivity(intent)
                            bgNote = "System dialog me 'Allow' dabao — phir wapas aakar Status refresh karo."
                        }
                    } catch (e: Exception) {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            )
                            bgNote = "Battery settings khul rahi hain — wahan Flora ko 'Don't optimize' karo."
                        } catch (e2: Exception) {
                            bgNote = "Battery settings nahi khul payi — phone ki Settings > Apps > Flora > Battery se karo."
                        }
                    }
                }) { Text("Battery optimization band karo") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = {
                    try {
                        val vivoIntents = listOf(
                            Intent().setClassName(
                                "com.vivo.permissionmanager",
                                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
                            ),
                            Intent().setClassName(
                                "com.iqoo.secure",
                                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
                            ),
                        )
                        var opened = false
                        for (intent in vivoIntents) {
                            try {
                                context.startActivity(intent)
                                opened = true
                                break
                            } catch (e: Exception) {
                            }
                        }
                        if (!opened) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + context.packageName),
                                )
                            )
                        }
                        bgNote = "Auto-start/app-info khul rahi hai — wahan Flora ka auto-start ON karo."
                    } catch (e: Exception) {
                        bgNote = "Auto-start settings nahi khul payi — phone ki Settings > Apps > Flora se karo."
                    }
                }) { Text("Auto-start settings kholo (Vivo/iQOO)") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = {
                    notifGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    batteryIgnored = try {
                        context.getSystemService(PowerManager::class.java)
                            ?.isIgnoringBatteryOptimizations(context.packageName) ?: false
                    } catch (e: Exception) {
                        false
                    }
                    bgNote = "Status refresh ho gaya."
                }) { Text("Status refresh karo") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { onShowPermissions() }) { Text("Permissions dobara dekho") }
            if (bgNote.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(bgNote)
            }
            Text("Reboot ke baad Wake Word apne aap wapas chalu ho jayega (agar pehle ON tha). Accessibility aur dialer role hamesha tumhari marzi se, alag se milenge.")
            Spacer(modifier = Modifier.height(12.dp))
            Text("Website account link — website se khareeda Pro isi app me dikhane ke liye")
            Text("Link karne se pehle jaan lo: purane random-UUID wali koi licence (agar hoti) orphan ho jayegi; refresh token sirf is phone par save hoga; password change par link tootega aur dobara login karna hoga.")
            if (linkedEmailDisplay.isNotEmpty()) {
                Text("Linked: $linkedEmailDisplay")
                Button(onClick = {
                    scope.launch {
                        prefs.clearLink()
                        linkedEmailDisplay = ""
                        note = "Website account unlink ho gaya — ab app wapas random-UUID mode me hai."
                    }
                }) { Text("Unlink karo") }
            } else {
                OutlinedTextField(
                    value = linkEmail, onValueChange = { linkEmail = it },
                    label = { Text("Website account email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = linkPassword, onValueChange = { linkPassword = it },
                    label = { Text("Website account password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    scope.launch {
                        if (linkEmail.isBlank() || linkPassword.isBlank()) {
                            note = "Email aur password dono daalo."
                        } else {
                            val result = licenceClient.linkWebsiteAccount(
                                linkEmail.trim(), linkPassword
                            )
                            linkPassword = ""
                            if (result.error.isNotEmpty()) {
                                note = "Link fail: ${result.error}"
                            } else {
                                prefs.setLink(result.uid, linkEmail.trim(), result.refreshToken)
                                linkedEmailDisplay = linkEmail.trim()
                                linkEmail = ""
                                val aid = Settings.Secure.getString(
                                    context.contentResolver, Settings.Secure.ANDROID_ID
                                )
                                val token = prefs.fcmToken()
                                if (token.isNotEmpty()) {
                                    licenceClient.syncPushToken(result.uid, prefs.deviceId(aid), token)
                                }
                                note = "Website account link ho gaya — ab website wala Pro isi app me dikhega."
                            }
                        }
                    }
                }) { Text("Website account link karo") }
            }
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
                listOf("flora" to "Flora", "saathi" to "Saathi", "guru" to "Guru", "romantic" to "Romantic").forEach { (id, label) ->
                    Button(onClick = {
                        persona = id
                        voice.persona = id
                        scope.launch { prefs.setPersona(id) }
                    }) { Text(if (persona == id) "✓ $label" else label) }
                    Spacer(modifier = Modifier.padding(2.dp))
                }
            }
            Text("Flora: friendly + professional. Saathi: dostana, chhote jawab. Guru: step-by-step, to-the-point. Romantic: pyaar bhara, caring tone (user ke kehne par ON hua hai).")
            Spacer(modifier = Modifier.height(12.dp))
            Text("Auto mode — bina har baar poochhe kaam (default ON, user order)")
            Row {
                Text("Auto mode")
                Spacer(modifier = Modifier.padding(4.dp))
                Switch(checked = autoMode, onCheckedChange = { value -> onAutoModeChange(value) })
            }
            Text("ON hone par Flora call, SMS, app-kholo aur routine ke call/SMS steps bina har baar poochhe karegi, aur har kaam bol kar batayegi. Har kaam Auto Log me dikhega aur notification me 'Auto mode ON' dikhega. 'Auto mode band' bolte hi band. Ek confirmation abhi bhi lagega: payment, delete-all, ek se zyada app milne par chunna, pehli baar permission, aur SOS emergency.")
            Button(onClick = { onShowAutoLog() }) { Text("Auto Log dekho") }
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
                    val userId = prefs.effectiveUserId()
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
                        val userId = prefs.effectiveUserId()
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
            Text("Data retention: Journal 30 din baad auto-delete; Memories me date wali entries 30 din baad auto-delete, bina date wali manually. Licence/payment record alag se rakha jata hai.")
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
        }
    }
}
