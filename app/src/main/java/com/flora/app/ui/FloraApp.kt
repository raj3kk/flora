package com.flora.app.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.provider.MediaStore
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
import com.flora.app.automation.ScreenCapture
import com.flora.app.automation.ShizukuBridge
import com.flora.app.data.AutoLogStore
import com.flora.app.tools.ImageTools
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.flora.app.assistant.BrainClient
import com.flora.app.assistant.VisionClient
import com.flora.app.assistant.VoiceEngine
import com.google.firebase.messaging.FirebaseMessaging
import com.flora.app.automation.VerifiedAppLauncher
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
import com.flora.app.tools.WebsitePublisher
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

private enum class Screen { CHAT, SETTINGS, MEMORIES, SCAN, TOOLS, SOS, FACE, DOCS, CONNECT, PRIVACY, TERMS, AUTO, ROUTINES, LIVE, DRIVING, JOURNAL, PERMISSIONS, LICENCE, AUTO_LOG, MODELS, VOICE_ENROL, NOTIFICATIONS, WHITEBOARD, HOME, AVATAR, CAMERA_LOOK, BROWSER, LOCK, PC_LINK }

/** Dropdown menu + title ke liye har screen ka dikhne wala naam. */
private fun screenTitle(screen: Screen): String = when (screen) {
    Screen.CHAT -> "Chat"
    Screen.PERMISSIONS -> "Permissions"
    Screen.LICENCE -> "Licence"
    Screen.AUTO_LOG -> "Auto Log"
    Screen.SETTINGS -> "Settings"
    Screen.MEMORIES -> "Yaadein"
    Screen.SCAN -> "Scan"
    Screen.CAMERA_LOOK -> "Camera dekho"
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
    Screen.WHITEBOARD -> "Whiteboard"
    Screen.HOME -> "Home"
    Screen.AVATAR -> "Avatar"
    Screen.BROWSER -> "Browser"
    Screen.LOCK -> "Lock"
    Screen.PC_LINK -> "PC Link"
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
    // Voice-first Baat view (v2.15): default ON — baat-cheet me
    // likha hua transcript nahi dikhega, sirf orb + mic. Chat
    // sirf tab jab user khud "Chat kholo" dabaye.
    var voiceView by remember { mutableStateOf(true) }
    // Avatar style (v2.22, S2): orb ka rang, prefs se.
    var avatarStyle by remember { mutableStateOf("gulabi") }
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
    // Termux consent (v2.22, Phase 5, G11): command tabhi
    // chalega jab user dialog me "Haan" dabaye — kabhi silent nahi.
    var termuxConfirm by remember { mutableStateOf<String?>(null) }
    var voiceAppConfirm by remember { mutableStateOf<com.flora.app.automation.LaunchableApp?>(null) }
    // Phase 6: macro run state — pending = chal raha/ruka macro + agla step index.
    // macroNeedsConfirm true ho to sensitive step par dialog dikhega; Haan par wahi
    // step se resume, Ruko par macro ruk jayegi. Kabhi silent resume nahi.
    var macroPending by remember { mutableStateOf<Pair<com.flora.app.automation.Macro, Int>?>(null) }
    var macroNeedsConfirm by remember { mutableStateOf(false) }
    // Phase 6: form submit confirm — password field wale form par submit se pehle dialog.
    var formSubmitPending by remember { mutableStateOf(false) }
    // Phase 6: reply confirm — pkg + sender + draft; Haan par hi bhejne ka raasta chalega.
    var replyPending by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    // Auto mode (v2.17 switch; v2.18 user order se DEFAULT ON):
    // ON par call/SMS/app bina har baar poochhe chalenge, har kaam
    // Auto Log me dikhega.
    var autoMode by remember { mutableStateOf(false) }
    var pendingVoiceCallRequest by remember { mutableStateOf<String?>(null) }
    var pendingVoiceSmsRequest by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingContact by remember { mutableStateOf<PendingContact?>(null) }
    // Teach-by-doing state (v2.22, P4-F20): naam + darj steps.
    var teachingRoutine by remember { mutableStateOf<String?>(null) }
    var teachingSteps by remember { mutableStateOf<List<String>>(emptyList()) }
    // Number-guessing game state (v2.22, P5-F9): poora on-device.
    var gameNumber by remember { mutableStateOf<Int?>(null) }
    var gameTries by remember { mutableStateOf(0) }
    val appContext = LocalContext.current
    // Barge-in controller (v2.22, chunk 7): appContext ke BAAD hi
    // (order trap — pehle likhne par CI ne Unresolved reference
    // diya tha). TTS ke dauraan user beech me bole to Deepgram
    // live stream se suno; mic continuous listener ka ho to
    // shuru hi nahi hoga.
    val bargeIn = remember { com.flora.app.voice.BargeInController(appContext) }
    // appContext ke BAAD (declaration order — pehle wali galti se CI fail hua tha).
    val autoLogStore = remember { AutoLogStore(appContext) }
    // Long memory (Phase 2B, user order 2026-10-29 02:59): baat-cheet +
    // pending kaam phone par hi encrypted save rahenge — restart par
    // bhi khatam nahi honge (in-session-only history ki kami ka fix).
    // 30-din prune is store par NAHI chalta (long-lasting order);
    // baaki stores ka prune wahi rahega. Server par memory kabhi nahi jati.
    val chatMemoryStore = remember { com.flora.app.data.ChatMemoryStore(appContext) }
    val pendingTaskStore = remember { com.flora.app.data.PendingTaskStore(appContext) }
    var memoryReady by remember { mutableStateOf(false) }
    var persistedCount by remember { mutableStateOf(0) }
    var executeAfterReply by remember { mutableStateOf<String?>(null) }
    // App-start par purani baat-cheet wapas chat me dikhao (aakhri 60
    // turns; greeting pehle se hai, isliye append karte hain).
    LaunchedEffect(Unit) {
        try {
            val turns = chatMemoryStore.loadAll()
            if (turns.isNotEmpty()) {
                val shown = turns.takeLast(60).map { ChatMsg(it.text, it.fromUser) }
                messages = messages + shown
            }
        } catch (e: Exception) {
        }
        persistedCount = messages.size
        memoryReady = true
    }
    // Chat me jo naya judta hai, use encrypted store me save karo.
    // Greeting + khali lines save nahi hoti; "Flora: " prefix hat kar
    // save hota hai (recall bolte waqt saaf lage). Count tabhi badhta
    // hai jab save kamyab ho — fail ho to agle turn par dobara koshish.
    LaunchedEffect(messages) {
        if (!memoryReady) return@LaunchedEffect
        val current = messages
        if (current.size < persistedCount) {
            persistedCount = current.size
            return@LaunchedEffect
        }
        if (current.size == persistedCount) return@LaunchedEffect
        val fresh = current.drop(persistedCount).mapNotNull { m ->
            val t = m.text.removePrefix("Flora: ").trim()
            if (t.isEmpty() || t == "Namaste! Main Flora hun. Kya madad kar sakti hun?") null
            else t to m.fromUser
        }
        if (fresh.isEmpty()) {
            persistedCount = current.size
            return@LaunchedEffect
        }
        try {
            fresh.forEach { (t, u) -> chatMemoryStore.addTurn(u, t) }
            persistedCount = current.size
        } catch (e: Exception) {
        }
    }
    // Pending kaam execute: "agla kaam karo" par task yahan rakha
    // jata hai; busy khatam hote hi sendChat se wahi verified
    // raaste par chalega (Auto Mode rules usi router me lagte hain).
    LaunchedEffect(busy, executeAfterReply) {
        val task = executeAfterReply
        if (!busy && task != null) {
            executeAfterReply = null
            sendChatHolder[0]?.invoke(task)
        }
    }
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
    // Guardian PIN re-ask (Guardian enforcement build, 2026-10-29):
    // Guardian ON ho aur is session me PIN verify na hua ho, tab gated
    // kaam turant nahi chalega — PIN dialog khulega; sahi PIN par
    // pending kaam ek baar chalega, galat/cancel par kuch nahi chalega.
    // Guardian OFF ho ya session verified ho to kaam seedha chalega
    // (koi extra step nahi). Ek waqt me ek hi pending kaam rehta hai —
    // naya gated request purane pending ko badal deta hai (fail-safe:
    // bina PIN ke kuch nahi chalta). Har roka gaya kaam Auto Log me
    // likha jata hai; ongoing notification + "auto mode band" chhede nahi.
    var guardianPinInput by remember { mutableStateOf("") }
    var guardianPinNote by remember { mutableStateOf("") }
    var guardianPinPending by remember { mutableStateOf<(suspend () -> Unit)?>(null) }
    fun runGuardedVoice(action: suspend () -> Unit) {
        scope.launch {
            val gOn = try { prefs.guardianMode() } catch (e: Exception) { false }
            val pOn = try { prefs.pinEnabled() } catch (e: Exception) { false }
            if (guardian.canAccessSensitive(pOn, gOn)) {
                action()
            } else {
                guardianPinPending = action
                guardianPinInput = ""
                guardianPinNote = ""
                val msg = "Guardian mode ON hai — pehle PIN verify karo, phir ye kaam hoga."
                messages = messages + ChatMsg("Flora: $msg", false)
                voice.speak(msg)
                try { autoLogStore.add("Guardian ne roka: PIN verify ke bina kaam nahi chala") } catch (e: Exception) { }
            }
        }
    }

    // Screen share state (v2.22, voice-first autonomy): permission +
    // token FloraApp level par — Chat se bhi "screen dekho" bolne par
    // kaam kare, LiveScreen par jaane ki zaroorat nahi. Launcher
    // callback me session ban kar pending kaam aage badhega.
    var screenGranted by remember { mutableStateOf(false) }
    var screenResultCode by remember { mutableStateOf(0) }
    var screenResultData by remember { mutableStateOf<Intent?>(null) }
    // Pending-question auto-resume (v2.22, Phase 1): "screen
    // dekho" bolne par permission dialog khula ho to sawaal
    // yahan rakha jata hai; Allow milte hi launcher callback
    // ise voice pipeline se dobara chalata hai — user ko
    // dobara bolna nahi padta. Deny par clear ho jata hai.
    var pendingScreenQuestion by remember { mutableStateOf<String?>(null) }
    // Session re-ask guard (Phase 3): MediaProjection token marne par
    // dialog dobara kholna ek baar theek hai; uske baad bhi session na
    // bane to dialog loop mat chalao — imaandaar failure bolo.
    var screenReaskCount by remember { mutableStateOf(0) }
    val screenLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            com.flora.app.automation.ScreenCapture.LiveSessionHolder.clear()
            screenGranted = true
            screenResultCode = result.resultCode
            screenResultData = result.data
            val pending = pendingScreenQuestion
            pendingScreenQuestion = null
            if (pending != null) {
                voice.speak("Permission mil gayi, abhi dekh kar batati hun.")
                voiceCommand.value = pending
            } else {
                voice.speak("Screen permission mil gayi. Ab 'screen dekho' bolo, main dekh kar bata dungi.")
            }
        } else {
            pendingScreenQuestion = null
            screenReaskCount = 0
            voice.speak("Screen permission ke bina live screen nahi dekh sakti.")
        }
    }
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
            try { voice.voiceSample = prefs.voiceSample() } catch (e: Exception) { }
            try { voiceView = prefs.voiceView() } catch (e: Exception) { }
            try { avatarStyle = prefs.avatar() } catch (e: Exception) { }
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
            // Licence session renew (v2.22, Phase 1): linked
            // account ho to saved refresh token se session zinda
            // rakho. Fail ho to chupchaap local mode — app-start
            // kabhi nahi rukega. Naya token mile to save karo
            // (rotation). Access token kabhi save nahi hota.
            try {
                val rt = prefs.linkedRefreshToken()
                if (rt.isNotEmpty()) {
                    val next = licenceClient.refreshSession(rt)
                    if (!next.isNullOrEmpty() && next != rt) {
                        prefs.setLink(prefs.linkedUserId(), prefs.linkedEmail(), next)
                    }
                }
            } catch (e: Exception) {
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
                val matches = VerifiedAppLauncher.findByName(appContext, name)
                when {
                    matches.isEmpty() -> {
                        voice.speak("'$name' naam ki app nahi mili, step chhod diya.")
                    }
                    matches.size > 1 -> {
                        val names = matches.take(3).joinToString(", ") { it.label }
                        voice.speak("Ek se zyada app mili: $names. Kaunsi kholun, poora naam boliye — step chhod diya.")
                    }
                    else -> {
                        val app = matches[0]
                        runGuardedVoice {
                        when (VerifiedAppLauncher.openVerified(appContext, app)) {
                            VerifiedAppLauncher.OpenResult.Opened,
                            VerifiedAppLauncher.OpenResult.AlreadyOpen -> {
                                voice.speak("${app.label} khol diya.")
                            }
                            VerifiedAppLauncher.OpenResult.NoVerifier -> {
                                voice.speak("${app.label} kholna verify nahi kar paya — Accessibility service ON karna hoga, step chhod diya.")
                            }
                            else -> {
                                voice.speak("${app.label} nahi khul paya, step chhod diya.")
                            }
                        }
                        }
                    }
                }
            }
            lower.startsWith("chrome me ") || lower.startsWith("website ") -> {
                val site = cmd.substringAfter("kholo", "").trim()
                    .ifEmpty { cmd.substringAfter("me", "").trim() }
                if (site.isBlank()) {
                    voice.speak("Website nahi khul payi, step chhod diya.")
                } else {
                    runGuardedVoice {
                    when (VerifiedAppLauncher.openWebsiteVerified(appContext, site)) {
                        VerifiedAppLauncher.OpenResult.NotVisible,
                        VerifiedAppLauncher.OpenResult.NoLaunchIntent -> {
                            voice.speak("Website nahi khul payi, step chhod diya.")
                        }
                        else -> {
                            voice.speak("$site khol raha hun.")
                        }
                    }
                    }
                }
            }
            lower.startsWith("gaana bajao ") || lower.startsWith("gaana chalao ") -> {
                val song = cmd.substringAfter("bajao", "").trim()
                    .ifEmpty { cmd.substringAfter("chalao", "").trim() }
                if (song.isNotBlank()) {
                    VerifiedAppLauncher.openWebsiteVerified(
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
            lower == "camera me kya dikh raha hai" || lower == "camera dekho" ||
                lower == "camera se dekho" || lower == "camera me kya hai" -> {
                screen = Screen.CAMERA_LOOK
                voice.speak("Camera khol raha hun — 'Camera me kya dikh raha hai?' dabate hi dekh kar bata dungi.")
            }
            lower == "live update batao" || lower == "taaza khabar batao" ||
                lower == "realtime update batao" || lower == "aaj ki khabar batao" -> {
                voice.speak("Live update nikal rahi hun — mausam aur taaza khabar dekh rahi hun.")
                scope.launch {
                    try {
                        val weather = try { searchTools.weatherHere(appContext) } catch (e: Exception) { "" }
                        val news = try { searchTools.search("taaza khabar India today") } catch (e: Exception) { "" }
                        val msg = listOf(weather, news).filter { it.isNotBlank() }.joinToString(" • ")
                        if (msg.isBlank()) {
                            voice.speak("Is waqt live update nahi nikal payi, internet check kar ke dobara bolo.")
                        } else {
                            voice.speak(msg.take(600))
                        }
                    } catch (e: Exception) {
                        voice.speak("Live update me dikkat aayi, dobara koshish karo.")
                    }
                }
            }
            lower == "shizuku status batao" -> {
                voice.speak(ShizukuBridge.statusText())
            }
            lower == "shizuku se permission do" -> {
                if (ShizukuBridge.isReady() && ShizukuBridge.hasPermission()) {
                    val pkg = appContext.packageName
                    val done = listOf(
                        "Notification" to ShizukuBridge.grantPermission(pkg, Manifest.permission.POST_NOTIFICATIONS),
                        "Camera" to ShizukuBridge.grantPermission(pkg, Manifest.permission.CAMERA),
                        "Mic" to ShizukuBridge.grantPermission(pkg, Manifest.permission.RECORD_AUDIO)
                    )
                    val ok = done.filter { it.second }.map { it.first }
                    val fail = done.filter { !it.second }.map { it.first }
                    voice.speak(
                        if (fail.isEmpty()) "Shizuku se teeno permission mil gayi — ${ok.joinToString(", ")}."
                        else if (ok.isEmpty()) "Shizuku se permission nahi mili — Settings me Shizuku section se dobara koshish karo."
                        else "Mil gayi: ${ok.joinToString(", ")}. Nahi mili: ${fail.joinToString(", ")}."
                    )
                } else {
                    voice.speak("Shizuku taiyaar nahi hai. Pehle Shizuku app se chalao, phir Settings me 'Shizuku permission do' dabao, tab main permission le paungi.")
                }
            }
            lower == "whatsapp call uthao" -> {
                if (FloraAutomationService.isEnabled()) {
                    FloraAutomationService.instance?.swipeUp()
                    voice.speak("Swipe kar diya hai — agar WhatsApp call screen khuli thi to uth jayegi; har phone ka UI alag hota hai, guarantee nahi.")
                } else {
                    voice.speak("Accessibility band hai, isliye swipe nahi kar sakti. Settings me Flora ki accessibility ON karo, phir dobara bolo.")
                }
            }
            lower == "website publish karo" -> {
                voice.speak("Pichhli website ko public link par daal rahi hun.")
                scope.launch {
                    try {
                        val file = java.io.File(appContext.filesDir, "last-website.html")
                        if (!file.exists()) {
                            voice.speak("Pehle website banao — 'website banao' bol kar ek website banwao, phir main use publish kar dungi.")
                            return@launch
                        }
                        val html = file.readText()
                        val anonKey = try { prefs.supabaseAnonKey() } catch (e: Exception) { "" }
                        val uid = try { prefs.userId() } catch (e: Exception) { "" }
                        if (anonKey.isBlank() || uid.isBlank()) {
                            voice.speak("Publish ke liye app ki setting me website wali key chahiye — Settings me daal kar dobara bolo.")
                            return@launch
                        }
                        val slug = "site-" + uid.take(8)
                        val result = WebsitePublisher.publish(html, slug, anonKey)
                        if (result.isSuccess) {
                            val url = result.getOrNull() ?: ""
                            messages = messages + ChatMsg("Flora: Website publish ho gayi — $url", false)
                            voice.speak("Website publish ho gayi hai, link chat me de diya hai.")
                        } else {
                            voice.speak("Publish nahi ho paya. ${result.exceptionOrNull()?.message ?: "dobara koshish karo."}")
                        }
                    } catch (e: Exception) {
                        voice.speak("Publish me dikkat aayi, dobara koshish karo.")
                    }
                }
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
                        runGuardedVoice {
                        val ok = AutoActions.placeCall(appContext, number)
                        voice.speak(if (ok) "Routine call laga di gayi: $number" else "Routine call nahi lagi.")
                        scope.launch {
                            try { autoLogStore.add("Routine call (auto): $number -> ${if (ok) "ho gaya" else "fail"}") } catch (e: Exception) { }
                        }
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
                            runGuardedVoice {
                            val ok = AutoActions.sendSms(appContext, number, parts[1])
                            voice.speak(if (ok) "Routine SMS bhej diya gaya: $number" else "Routine SMS nahi gaya.")
                            scope.launch {
                                try { autoLogStore.add("Routine SMS (auto): $number -> ${if (ok) "ho gaya" else "fail"}") } catch (e: Exception) { }
                            }
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
                            // Echo guard (v2.22): Flora ki apni awaaz
                            // speaker se mic me aakar transcript ban
                            // jaye to use ignore karo — warna Flora
                            // apni hi baat par dobara bolegi (repeat
                            // loop). Aakhri Flora message se milao.
                            val spoken = transcript.trim().lowercase()
                            val lastFlora = messages.lastOrNull { !it.fromUser }
                                ?.text?.removePrefix("Flora:")?.trim()?.lowercase() ?: ""
                            val words = spoken.split(" ").filter { it.isNotBlank() }
                            val overlap = if (words.isEmpty()) 0 else
                                words.count { it in lastFlora.split(" ") }
                            val echo = spoken.isNotEmpty() && lastFlora.isNotEmpty() &&
                                (lastFlora.contains(spoken) || overlap * 2 >= words.size)
                            if (!echo) sendChatHolder[0]?.invoke(transcript)
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
    // user ko Bhejo dabana nahi padta. busy ho to baat queue me
    // rahegi (Phase 3: neeche wala effect busy khatam hote hi chalayega).
    // Voice-command queue (Phase 3): busy ho to command DROP mat karo.
    // Value tab tak rakhi rahegi jab tak busy khatam na ho; busy khatam
    // hote hi neeche wala effect use chalayega. Kabhi chupchaap drop
    // nahi — Phase 2B ke execute-effect pattern jaisa (LaunchedEffect
    // on busy). Dono effects Main par bina suspension ke consume karte
    // hain, isliye double-invoke nahi hoga (pehle wala clear kar dega).
    LaunchedEffect(voiceCommand.value) {
        val spoken = voiceCommand.value
        if (spoken.isNotEmpty() && !busy) {
            voiceCommand.value = ""
            sendChatHolder[0]?.invoke(spoken)
        }
    }
    LaunchedEffect(busy) {
        if (!busy) {
            val pending = voiceCommand.value
            if (pending.isNotEmpty()) {
                voiceCommand.value = ""
                sendChatHolder[0]?.invoke(pending)
            }
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
                            text = { Text("Camera dekho") },
                            onClick = { screen = Screen.CAMERA_LOOK; menuOpen = false },
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
                            text = { Text("Whiteboard") },
                            onClick = { screen = Screen.WHITEBOARD; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Home") },
                            onClick = { screen = Screen.HOME; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Avatar") },
                            onClick = { screen = Screen.AVATAR; menuOpen = false },
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
                            text = { Text("Browser") },
                            onClick = { screen = Screen.BROWSER; menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("PC Link") },
                            onClick = { screen = Screen.PC_LINK; menuOpen = false },
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
                            text = { Text("Lock") },
                            onClick = { screen = Screen.LOCK; menuOpen = false },
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
                    Text("Chat locked hai (Face Lock ON). PIN daal kar unlock kijiye. Face presence check se unlock nahi hota — PIN chahiye, aur awaaz enrolled ho to uske baad awaaz ka check bhi lagega.")
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
                FloraOrb(busy, avatarStyle)
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
                    val chatGptOpened = AppAutomation.launch(appContext, AppAutomation.CHATGPT_PKG)
                    if (chatGptOpened != VerifiedAppLauncher.OpenResult.Opened &&
                        chatGptOpened != VerifiedAppLauncher.OpenResult.AlreadyOpen
                    ) {
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
                    val savedBack = try {
                        com.flora.app.automation.ImageGenFlow.saveJpegToGallery(
                            appContext, service.screenshotJpeg()
                        )
                    } catch (e: Exception) {
                        false
                    }
                    val msg = if (savedBack) {
                        "Prompt ChatGPT ko bhej diya hai, image ban rahi hai — ChatGPT app me dekh lo, aur Gallery ke Flora folder me bhi save kar di hai. (ChatGPT me login/subscription manga to wo tumhe khud karna hoga.)"
                    } else {
                        "Prompt ChatGPT ko bhej diya hai, image ban rahi hai — ChatGPT app me dekh lo. Ban jaate hi wahi dikh jayegi. (ChatGPT me login/subscription manga to wo tumhe khud karna hoga.)"
                    }
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
                        val opened = AppAutomation.launch(appContext, pkg)
                        if (opened == VerifiedAppLauncher.OpenResult.NotVisible ||
                            opened == VerifiedAppLauncher.OpenResult.NoLaunchIntent
                        ) {
                            val msg = "$name khul nahi paya, app khud khol lo."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        } else if (opened == VerifiedAppLauncher.OpenResult.NoVerifier) {
                            val msg = "$name kholna verify nahi kar paya — Accessibility service ON karna hoga, Settings me."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        }
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
                        val opened = AppAutomation.launch(appContext, pkg)
                        if (opened == VerifiedAppLauncher.OpenResult.NotVisible ||
                            opened == VerifiedAppLauncher.OpenResult.NoLaunchIntent
                        ) {
                            val msg = "$name khul nahi paya, app khud khol lo."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        } else if (opened == VerifiedAppLauncher.OpenResult.NoVerifier) {
                            val msg = "$name kholna verify nahi kar paya — Accessibility service ON karna hoga, Settings me."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        }
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
                    runGuardedVoice {
                        val ok = AutoActions.placeCall(appContext, number)
                        val msg = if (ok) "Call laga di gayi: $number" else "Call nahi lagi, permission ya number check kijiye."
                        messages = messages + ChatMsg("Flora: $msg", false)
                        voice.speak(msg)
                        scope.launch {
                            try { autoLogStore.add("Call (auto): $number -> ${if (ok) "ho gaya" else "fail"}") } catch (e: Exception) { }
                        }
                    }
                }

                fun autoExecuteSms(number: String, text: String) {
                    runGuardedVoice {
                        val ok = AutoActions.sendSms(appContext, number, text)
                        val msg = if (ok) "SMS bhej diya gaya: $number" else "SMS nahi gaya, permission ya number check kijiye."
                        messages = messages + ChatMsg("Flora: $msg", false)
                        voice.speak(msg)
                        scope.launch {
                            try { autoLogStore.add("SMS (auto): $number -> ${if (ok) "ho gaya" else "fail"}") } catch (e: Exception) { }
                        }
                    }
                }

                fun autoExecuteOpen(
                    app: com.flora.app.automation.LaunchableApp,
                    onDone: ((VerifiedAppLauncher.OpenResult) -> Unit)? = null,
                ) {
                    runGuardedVoice {
                    scope.launch {
                        val result = VerifiedAppLauncher.openVerified(appContext, app)
                        val verified = result == VerifiedAppLauncher.OpenResult.Opened ||
                            result == VerifiedAppLauncher.OpenResult.AlreadyOpen
                        val msg = when (result) {
                            VerifiedAppLauncher.OpenResult.Opened -> "${app.label} khol diya."
                            VerifiedAppLauncher.OpenResult.AlreadyOpen -> "${app.label} pehle se khula hua hai."
                            VerifiedAppLauncher.OpenResult.NoVerifier -> "${app.label} kholna verify nahi kar paya — Accessibility service ON karna hoga, Settings me."
                            else -> "${app.label} nahi khul paya."
                        }
                        messages = messages + ChatMsg("Flora: $msg", false)
                        voice.speak(msg)
                        try { autoLogStore.add("App khola (auto): ${app.label} -> ${if (verified) "ho gaya" else "fail"}") } catch (e: Exception) { }
                        if (verified) {
                            com.flora.app.automation.ForegroundAppContext.noteOpened(app.packageName, app.label)
                        }
                        onDone?.invoke(result)
                    }
                    }
                }

                // Phase 6: macro chalao — step-by-step bol kar + Auto Log.
                // Sensitive step par runner NeedsConfirm dega to dialog
                // dikhega (macroNeedsConfirm); Haan par wahi step se
                // resume hoga. Kabhi silent macro nahi.
                fun runMacroFrom(macro: com.flora.app.automation.Macro, fromStep: Int) {
                    runGuardedVoice {
                        scope.launch {
                            val service = com.flora.app.automation.FloraAutomationService.instance
                            if (service == null) {
                                val msg = "Macro chalane ke liye Accessibility service ON karni hogi, Settings me."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                                return@launch
                            }
                            val result = com.flora.app.automation.MacroEngine.runMacro(
                                appContext, service, macro, fromStep
                            ) { stepMsg ->
                                messages = messages + ChatMsg("Flora: $stepMsg", false)
                                voice.speak(stepMsg)
                            }
                            when (result) {
                                is com.flora.app.automation.MacroOutcome.Done -> {
                                    val msg = "Macro '${macro.name}' poora ho gaya."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                    macroPending = null
                                    try { autoLogStore.add("Macro: ${macro.name} -> ho gaya") } catch (e: Exception) { }
                                }
                                is com.flora.app.automation.MacroOutcome.Failed -> {
                                    val msg = "Macro '${macro.name}' step ${result.stepIndex + 1} par ruk gaya: ${result.reason}"
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                    macroPending = null
                                    try { autoLogStore.add("Macro: ${macro.name} -> fail (${result.reason})") } catch (e: Exception) { }
                                }
                                is com.flora.app.automation.MacroOutcome.NeedsConfirm -> {
                                    macroPending = macro to result.stepIndex
                                    macroNeedsConfirm = true
                                    val msg = "Macro '${macro.name}' ka agla step sensitive hai (${result.reason}) — confirm karo to aage badhungi, warna ruk jayegi."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                }
                            }
                        }
                    }
                }

                // Phase 6: auto-reply draft propose karo — thread (aakhri
                // 6) + brain draft user ko sunao + chat me likho; bhejna
                // sirf replyPending dialog me Haan par hoga.
                fun proposeReply(pkg: String) {
                    scope.launch {
                        when (val d = com.flora.app.automation.ReplyDraft.buildDraft(appContext, pkg, brain)) {
                            is com.flora.app.automation.DraftResult.Empty -> {
                                voice.speak("Is app ki koi notification nahi mili — pehle Notifications se access ON karo.")
                            }
                            is com.flora.app.automation.DraftResult.Ready -> {
                                val summary = d.thread.lines.joinToString(" | ")
                                if (d.draft.isBlank()) {
                                    val msg = "Aakhri baatchit: $summary. Draft nahi bana payi — brain key check karo (Settings me Keys check karo)."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                } else {
                                    val msg = "Aakhri baatchit: $summary. Mera sujha hua jawab: ${d.draft}"
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak("Mera sujha hua jawab: ${d.draft}. Bheju to dialog me Haan dabao — bina confirm ke kuch nahi jayega.")
                                    replyPending = Triple(pkg, d.thread.senderTitle, d.draft)
                                }
                            }
                        }
                    }
                }

                // Clarification ke baad kaam chalana (v2.22): confirm
                // ya auto flow — call/sms branches jaisa hi.
                fun runContactAction(number: String, action: String, text: String) {
                    if (action == "whatsapp") {
                        // wa.me chat kholo, message bhara hua — Send
                        // user dabata hai (silent send kabhi nahi).
                        val digitsOnly = number.filter { it.isDigit() }
                        val waNumber = if (digitsOnly.length == 10) "91$digitsOnly" else digitsOnly
                        if (waNumber.length < 11) {
                            val msg = "WhatsApp ke liye poora number chahiye (10 digit ya country code ke saath)."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        } else {
                            try {
                                val uri = android.net.Uri.parse(
                                    "https://wa.me/$waNumber?text=" +
                                        android.net.Uri.encode(text)
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = uri
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                appContext.startActivity(intent)
                                val msg = "WhatsApp khol diya hai, message likha hua milega — Send dabate hi jayega."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            } catch (e: Exception) {
                                val msg = "WhatsApp nahi khul paya — kya WhatsApp installed hai?"
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
                        }
                    } else if (action == "call") {
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
                    // Teach-by-doing (v2.22, P4-F20): "routine
                    // sikhao <naam>" ke baad user jo commands bolta
                    // hai, wo execute NAHI hote — steps ke roop me
                    // darj hote hain. "routine khatam" par Routine
                    // save ho jaati hai (Routines screen wali).
                    val teachLower = question.lowercase()
                    if (teachingRoutine != null) {
                        input = ""
                        if (teachLower == "routine khatam") {
                            val tName = teachingRoutine!!
                            val tSteps = teachingSteps
                            teachingRoutine = null
                            teachingSteps = emptyList()
                            if (tSteps.isEmpty()) {
                                val msg = "Koi step darj nahi hua, routine save nahi ki."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            } else {
                                scope.launch {
                                    try {
                                        val existing = com.flora.app.data.RoutineStore.load(appContext)
                                        val updated = existing.filter { it.name != tName } +
                                            com.flora.app.data.Routine(tName, tSteps)
                                        com.flora.app.data.RoutineStore.save(appContext, updated)
                                        val msg = "Routine '$tName' seekh li — ${tSteps.size} steps. Routines screen se chala sakte ho."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    } catch (e: Exception) {
                                        val msg = "Routine save nahi ho payi."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    }
                                }
                            }
                        } else if (teachLower == "routine chhodo" || teachLower == "routine cancel") {
                            teachingRoutine = null
                            teachingSteps = emptyList()
                            val msg = "Theek hai, sikhana chhod diya — kuch save nahi hua."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        } else if (teachLower.startsWith("routine sikhao ")) {
                            val msg = "Pehle wali routine khatam karo ('routine khatam' bolo), phir nayi sikha dunga."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        } else {
                            teachingSteps = teachingSteps + question
                            val msg = "Step darj: $question"
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        }
                        return
                    }
                    if (teachLower.startsWith("routine sikhao ")) {
                        val tName = question.substring(15).trim()
                        input = ""
                        if (tName.isEmpty()) {
                            val msg = "Kis naam se routine sikhaun? Aise boliye: 'routine sikhao subah'."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        } else {
                            teachingRoutine = tName
                            teachingSteps = emptyList()
                            val msg = "Theek hai, '$tName' sikha raha hun — jo kaam karwana hai wo ek-ek karke bolo, khatam ho to 'routine khatam' bolo."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        }
                        return
                    }
                    // Clarification ka jawab (v2.22): naam ambiguous tha
                    // ya mila nahi tha — user ka jawab hi action hai.
                    // Naya command (call/sms/auto mode/romantic) aaye to
                    // purana pending khatam, naya command chalega.
                    val pending = pendingContact
                    if (pending != null) {
                        val ansLower = question.lowercase()
                        val isNewCommand = ansLower.startsWith("call ") ||
                            ansLower.startsWith("sms ") ||
                            ansLower.startsWith("whatsapp ") ||
                            ansLower.startsWith("game") ||
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
                    // Long memory (Phase 2B): pehle kya bola tha / aage kya
                    // karna hai / yaad rakhna / agla kaam karo — persisted
                    // store se jawab, brain ke andaze par nahi. Ye branches
                    // early hain (user turn append se pehle), isliye inke
                    // jawab persist effect se store me bhi save hote hain.
                    val memLower = lower.removePrefix("ye ").trim()
                    if (memLower == "pehle kya bola tha" || memLower == "pehle kya bole the" ||
                        memLower == "maine pehle kya bola" || memLower == "maine pehle kya bola tha" ||
                        memLower == "maine kya kaha tha" || memLower == "maine abhi kya bola" ||
                        memLower == "pichhli baar kya bola tha" || memLower == "what did i say before" ||
                        memLower == "what did i say" || memLower.startsWith("pehle maine kya")
                    ) {
                        scope.launch {
                            try {
                                val turns = chatMemoryStore.loadAll()
                                val mine = turns.filter { it.fromUser && it.text != question }
                                    .takeLast(3)
                                val msg = if (mine.isEmpty()) {
                                    "Abhi tak tumne mujhse koi purani baat save nahi hai — jo bologe, main yaad rakh lungi."
                                } else if (mine.size == 1) {
                                    "Tumne pehle ye bola tha: '${mine[0].text}'."
                                } else {
                                    val lines = mine.mapIndexed { i, t -> "${i + 1}. '${t.text}'" }
                                        .joinToString("; ")
                                    "Tumne pehle ye bola tha: $lines."
                                }
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            } catch (e: Exception) {
                                val msg = "Purani baat nikalne me dikkat aayi, dobara poochho na."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
                        }
                        return
                    }
                    if (memLower == "tumne kya bola tha" || memLower == "tumne pehle kya bola" ||
                        memLower == "tumne abhi kya kaha" || memLower == "tumne kya kaha tha" ||
                        memLower == "what did you say"
                    ) {
                        scope.launch {
                            try {
                                val turns = chatMemoryStore.loadAll()
                                val mine = turns.filter { !it.fromUser }.takeLast(2)
                                val msg = if (mine.isEmpty()) {
                                    "Maine abhi tak tumhe koi jawab save nahi kiya hai."
                                } else if (mine.size == 1) {
                                    "Maine ye bola tha: '${mine[0].text}'."
                                } else {
                                    "Maine pehle ye bola tha: '${mine[0].text}', phir ye: '${mine[1].text}'."
                                }
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            } catch (e: Exception) {
                                val msg = "Purani baat nikalne me dikkat aayi, dobara poochho na."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
                        }
                        return
                    }
                    if (memLower == "aage kya karna hai" || memLower == "aage kya karna h" ||
                        memLower == "mera agla kaam kya hai" || memLower == "agla kaam kya hai" ||
                        memLower == "pending kaam" || memLower == "pending kaam kya hain" ||
                        memLower == "mere pending kaam" || memLower == "mujhe kya karna chahiye" ||
                        memLower == "kya karna chahiye" || memLower == "what should i do next" ||
                        memLower == "what to do next" || memLower.startsWith("aage kya karna") ||
                        memLower.startsWith("pending kaam") || memLower.startsWith("mere pending kaam")
                    ) {
                        scope.launch {
                            try {
                                val tasks = pendingTaskStore.loadAll()
                                val msg = if (tasks.isEmpty()) {
                                    "Koi pending kaam save nahi hai. Koi kaam yaad rakhna ho to 'yaad rakhna' bol kar bata do."
                                } else {
                                    val lines = tasks.mapIndexed { i, t -> "${i + 1}. $t" }
                                        .joinToString("; ")
                                    "Aage ye kaam hain: $lines. 'Agla kaam karo' bologe to pehla kar dungi."
                                }
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            } catch (e: Exception) {
                                val msg = "Pending kaam nikalne me dikkat aayi, dobara poochho na."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
                        }
                        return
                    }
                    if (memLower.startsWith("yaad rakhna") || memLower.startsWith("remember to ") ||
                        memLower.startsWith("remember this")
                    ) {
                        val task = memLower.removePrefix("yaad rakhna")
                            .removePrefix("remember to").removePrefix("remember this")
                            .trim().trimStart(':', '-', ' ').trim()
                        if (task.isEmpty()) {
                            val msg = "Kya yaad rakhun? Kaam bol kar bata do — jaise 'yaad rakhna kal subah paani ka bill bharna hai'."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        } else {
                            scope.launch {
                                try {
                                    pendingTaskStore.add(task)
                                    val msg = "Yaad rakh liya: $task. 'Aage kya karna hai' poochhne par bata dungi."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                } catch (e: Exception) {
                                    val msg = "Yaad rakhne me dikkat aayi, dobara bolo na."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                }
                            }
                        }
                        return
                    }
                    if (memLower == "agla kaam karo" || memLower == "agla kaam kar do" ||
                        memLower == "pending kaam karo" || memLower == "pending kaam kar do" ||
                        memLower == "do the next task"
                    ) {
                        scope.launch {
                            try {
                                val tasks = pendingTaskStore.loadAll()
                                if (tasks.isEmpty()) {
                                    val msg = "Koi pending kaam save nahi hai — pehle 'yaad rakhna' bol kar koi kaam bata do."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                } else {
                                    val task = tasks[0]
                                    pendingTaskStore.removeAt(0)
                                    val msg = "Ye kar rahi hun: $task."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                    executeAfterReply = task
                                }
                            } catch (e: Exception) {
                                val msg = "Agla kaam chalane me dikkat aayi, dobara bolo na."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
                        }
                        return
                    }
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
                    // Notification voice query (v2.22, P2-F10 poora):
                    // user khud poochhe tabhi notifications bolkar
                    // sunao (auto-announce kabhi nahi — private
                    // content apne aap bolna leak ho sakta hai).
                    // Sirf aakhri 3, sirf title/text ka pehla hissa.
                    if (lower == "notification batao" ||
                        lower == "notifications batao" ||
                        lower == "koi notification aayi"
                    ) {
                        input = ""
                        scope.launch {
                            try {
                                val notifs = com.flora.app.data.NotificationStore
                                    .load(appContext).reversed().take(3)
                                if (notifs.isEmpty()) {
                                    val msg = "Koi notification save nahi hai. Pehle Menu me Notifications se access ON karo."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                } else {
                                    val parts = notifs.map { item ->
                                        val label = try {
                                            val info = appContext.packageManager
                                                .getApplicationInfo(item.pkg, 0)
                                            appContext.packageManager
                                                .getApplicationLabel(info).toString()
                                        } catch (e: Exception) {
                                            item.pkg
                                        }
                                        val t = if (item.title.isNotBlank()) item.title else item.text
                                        "$label se ${t.take(100)}"
                                    }
                                    val msg = "Aakhri notifications: " + parts.joinToString(" • ")
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                }
                            } catch (e: Exception) {
                                val msg = "Notifications nahi padh payi."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
                        }
                        return
                    }
                    // Home control ki koshish par imaandari wala
                    // jawab (v2.22, P5-F22): box juda ho ya na ho,
                    // server sync (S4) ke bina koi relay nahi chalega
                    // — chhupke se on/off kabhi nahi hoga.
                    if (teachLower.contains("ghar") &&
                        (teachLower.contains("light") || teachLower.contains("pankha") ||
                            teachLower.contains("fan") || teachLower.contains("batti"))
                    ) {
                        input = ""
                        scope.launch {
                            val code = try { prefs.homeBoxCode() } catch (e: Exception) { "" }
                            val msg = if (code.isEmpty()) {
                                "Ghar ka control abhi nahi chalega — pehle Menu me Home screen se box ka code jodo. Uske baad bhi server sync (S4) aane par hi on/off chalega."
                            } else {
                                "Box juda hua hai, par server sync abhi baaki hai — isliye light/pankha abhi awaaz se nahi chalega. Sync aate hi chalega."
                            }
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        }
                        return
                    }
                    // Number-guessing game (v2.22, P5-F9): poora
                    // on-device, koi key/network nahi. "game khelo"
                    // se shuru, number bolo, "game band karo" se band.
                    if (teachLower.startsWith("game khelo") ||
                        teachLower == "game" ||
                        teachLower == "khelo"
                    ) {
                        input = ""
                        gameNumber = (1..10).random()
                        gameTries = 0
                        val msg = "Game shuru! 1 se 10 ke beech ek number socha hai — guess karo."
                        messages = messages + ChatMsg("Flora: $msg", false)
                        voice.speak(msg)
                        return
                    }
                    if (gameNumber != null) {
                        val target = gameNumber!!
                        if (teachLower == "game band karo" || teachLower == "game chhodo") {
                            input = ""
                            gameNumber = null
                            val msg = "Theek hai, game band. Number $target tha."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                            return
                        }
                        val guess = question.filter { it.isDigit() }.toIntOrNull()
                        if (guess != null && guess in 1..10) {
                            input = ""
                            gameTries += 1
                            val msg = when {
                                guess == target -> {
                                    gameNumber = null
                                    "Sahi jawab! $gameTries baar me jeet gaye. Dobara khelna ho to 'game khelo' bolo."
                                }
                                guess < target -> "Chhota hai — bada number bolo."
                                else -> "Bada hai — chhota number bolo."
                            }
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                            return
                        }
                        // Number nahi hai to game background me rehta
                        // hai — baaki commands normal chalenge.
                    }
                    // Phase 6: foreground-app context carryover — khuli hui
                    // app ke andar search. Flora foreground me ho to plain
                    // form research branch par hi jayega (behaviour same).
                    // Explicit form ("is app me / is me / yahan") hamesha
                    // in-app: target saamne na ho to use dobara verified
                    // khol kar search hota hai.
                    val carryQuery: String? = when {
                        lower.startsWith("is app me ") && lower.endsWith(" search karo") ->
                            question.substring(10, (question.length - 12).coerceAtLeast(10)).trim()
                        lower.startsWith("is me ") && lower.endsWith(" search karo") ->
                            question.substring(6, (question.length - 12).coerceAtLeast(6)).trim()
                        lower.startsWith("yahan ") && lower.endsWith(" search karo") ->
                            question.substring(6, (question.length - 12).coerceAtLeast(6)).trim()
                        else -> null
                    }
                    val plainSearchQuery: String? = when {
                        lower.startsWith("search karo ") -> question.substring(12).trim()
                        lower.endsWith(" search karo") -> question.dropLast(12).trim()
                        else -> null
                    }
                    if (carryQuery != null) {
                        input = ""
                        val fgService = com.flora.app.automation.FloraAutomationService.instance
                        val fgPkg = try { fgService?.foregroundPackage() } catch (e: Exception) { null }
                        val targetPkg: String? = when {
                            fgPkg != null && fgPkg != appContext.packageName -> fgPkg
                            else -> com.flora.app.automation.ForegroundAppContext.lastOpenedPkg
                        }
                        if (carryQuery.isEmpty()) {
                            voice.speak("Kya search karu? Aise boliye: 'is app me chai search karo'.")
                        } else if (fgService == null || targetPkg == null) {
                            voice.speak("Koi app saamne nahi hai — pehle app kholo, phir 'is app me' bolo.")
                        } else {
                            scope.launch {
                                var svc: com.flora.app.automation.FloraAutomationService? = fgService
                                if (fgPkg != targetPkg) {
                                    val label = com.flora.app.automation.ForegroundAppContext.lastOpenedLabel ?: targetPkg
                                    val opened = com.flora.app.automation.VerifiedAppLauncher.openVerified(
                                        appContext,
                                        com.flora.app.automation.LaunchableApp(label, targetPkg)
                                    )
                                    if (opened != com.flora.app.automation.VerifiedAppLauncher.OpenResult.Opened &&
                                        opened != com.flora.app.automation.VerifiedAppLauncher.OpenResult.AlreadyOpen
                                    ) {
                                        val msg = "Wo app dobara khul nahi payi, isliye andar search nahi kar payi."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                        return@launch
                                    }
                                    svc = com.flora.app.automation.FloraAutomationService.instance
                                }
                                val serviceNow = svc
                                if (serviceNow == null) {
                                    voice.speak("App ke andar search ke liye Accessibility service ON karni hogi, Settings me.")
                                    return@launch
                                }
                                when (val res = com.flora.app.automation.InAppSearch.searchInApp(serviceNow, carryQuery)) {
                                    com.flora.app.automation.InAppSearchOutcome.Done -> {
                                        val msg = "Is app me '$carryQuery' search kar diya hai."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    }
                                    com.flora.app.automation.InAppSearchOutcome.TypedOnly -> {
                                        val msg = "Search box me '$carryQuery' likh diya hai, enter dabate hi search ho jayega."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    }
                                    else -> {
                                        val msg = "Is app me search box nahi mila, isliye andar search nahi kar payi."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    }
                                }
                            }
                        }
                        return
                    }
                    if (plainSearchQuery != null) {
                        val fgService = com.flora.app.automation.FloraAutomationService.instance
                        val fgPkg = try { fgService?.foregroundPackage() } catch (e: Exception) { null }
                        val inApp = fgService != null && fgPkg != null &&
                            fgPkg != appContext.packageName &&
                            com.flora.app.automation.ForegroundAppContext.matchesForeground(fgPkg)
                        if (inApp) {
                            input = ""
                            if (plainSearchQuery.isEmpty()) {
                                voice.speak("Kya search karu?")
                            } else {
                                scope.launch {
                                    when (val res = com.flora.app.automation.InAppSearch.searchInApp(fgService, plainSearchQuery)) {
                                        com.flora.app.automation.InAppSearchOutcome.Done -> {
                                            val msg = "Is app me '$plainSearchQuery' search kar diya hai."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        }
                                        com.flora.app.automation.InAppSearchOutcome.TypedOnly -> {
                                            val msg = "Search box me '$plainSearchQuery' likh diya hai, enter dabate hi search ho jayega."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        }
                                        else -> {
                                            val msg = "Is app me search box nahi mila, isliye andar search nahi kar payi."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        }
                                    }
                                }
                            }
                            return
                        }
                        // In-app nahi bana to neeche research branch chalegi.
                    }
                    // Phase 6: macros — "macro chalao <naam>" sirf tab
                    // chalta hai jab user ne saaf bola ho; sensitive
                    // step par confirm dialog aayega (kabhi silent nahi).
                    if (lower == "macros batao" || lower == "macro batao") {
                        input = ""
                        scope.launch {
                            try {
                                val macros = com.flora.app.automation.MacroStore.load(appContext)
                                val msg = if (macros.isEmpty()) {
                                    "Koi macro save nahi hai — Routines screen par Macros me banao."
                                } else {
                                    "Macros: " + macros.joinToString(", ") { it.name }
                                }
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            } catch (e: Exception) {
                                voice.speak("Macros nahi padh payi.")
                            }
                        }
                        return
                    }
                    if (lower.startsWith("macro chalao ")) {
                        input = ""
                        val mName = question.substring(13).trim()
                        if (mName.isEmpty()) {
                            voice.speak("Kaunsa macro chalau? Aise boliye: 'macro chalao subah'.")
                        } else {
                            scope.launch {
                                try {
                                    val macros = com.flora.app.automation.MacroStore.load(appContext)
                                    val stored = macros.firstOrNull { it.name.equals(mName, ignoreCase = true) }
                                    if (stored == null) {
                                        val msg = "Macro '$mName' nahi mila — 'macros batao' se list dekh lo."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    } else {
                                        val macro = com.flora.app.automation.MacroJson.parse(stored.json)
                                        if (macro == null) {
                                            voice.speak("Macro '$mName' ka format kharab hai, dobara save karo.")
                                        } else {
                                            val msg = "Macro '${macro.name}' chala rahi hun, har step bol kar bataungi."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                            macroPending = macro to 0
                                            runMacroFrom(macro, 0)
                                        }
                                    }
                                } catch (e: Exception) {
                                    voice.speak("Macro nahi chala payi.")
                                }
                            }
                        }
                        return
                    }
                    // Phase 6: form-fill — pehle "form bharo" se fields
                    // ki list, phir "field <naam> me <value> likho" se
                    // ek-ek field, phir "form submit karo". Password
                    // field me Flora kabhi nahi likhegi; password wale
                    // form ka submit confirm dialog ke baad hi hoga.
                    if (lower == "form bharo" || lower == "is form ko bharo") {
                        input = ""
                        scope.launch {
                            val service = com.flora.app.automation.FloraAutomationService.instance
                            if (service == null) {
                                voice.speak("Form bharne ke liye Accessibility service ON karni hogi, Settings me.")
                            } else {
                                val fields = com.flora.app.automation.FormFiller.findFields(service)
                                if (fields.isEmpty()) {
                                    voice.speak("Is screen par koi bharne layak field nahi mila.")
                                } else {
                                    val list = fields.joinToString(", ") { f ->
                                        f.label + if (f.isPassword) " (password — tum khud bharo)" else ""
                                    }
                                    val msg = "Form me ye fields mile: $list. Kisi field me likhne ke liye bolo: 'field <naam> me <value> likho'."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                }
                            }
                        }
                        return
                    }
                    if (lower.startsWith("field ") && lower.contains(" me ") && lower.endsWith(" likho")) {
                        input = ""
                        val body = question.substring(6, question.length - 6).trim()
                        val meIdx = body.lowercase().lastIndexOf(" me ")
                        if (meIdx < 0) {
                            voice.speak("Aise boliye: 'field naam me Priyadarshi likho'.")
                        } else {
                            val fLabel = body.substring(0, meIdx).trim()
                            val fValue = body.substring(meIdx + 4).trim()
                            if (fLabel.isEmpty() || fValue.isEmpty()) {
                                voice.speak("Aise boliye: 'field naam me Priyadarshi likho'.")
                            } else {
                                scope.launch {
                                    val service = com.flora.app.automation.FloraAutomationService.instance
                                    if (service == null) {
                                        voice.speak("Field bharne ke liye Accessibility service ON karni hogi, Settings me.")
                                    } else {
                                        when (val r = com.flora.app.automation.FormFiller.fillField(service, fLabel, fValue)) {
                                            is com.flora.app.automation.FillOutcome.Filled -> {
                                                val msg = "Field '${r.label}' me likh diya hai."
                                                messages = messages + ChatMsg("Flora: $msg", false)
                                                voice.speak(msg)
                                            }
                                            is com.flora.app.automation.FillOutcome.PasswordRefused -> {
                                                val msg = "Ye password field hai — isme main nahi likhungi, tum khud bharo."
                                                messages = messages + ChatMsg("Flora: $msg", false)
                                                voice.speak(msg)
                                            }
                                            is com.flora.app.automation.FillOutcome.NotFound -> {
                                                val msg = "Field '$fLabel' nahi mila — 'form bharo' se list dekh lo."
                                                messages = messages + ChatMsg("Flora: $msg", false)
                                                voice.speak(msg)
                                            }
                                            else -> {
                                                val msg = "Field '$fLabel' me likh nahi payi."
                                                messages = messages + ChatMsg("Flora: $msg", false)
                                                voice.speak(msg)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        return
                    }
                    if (lower == "form submit karo") {
                        input = ""
                        scope.launch {
                            val service = com.flora.app.automation.FloraAutomationService.instance
                            if (service == null) {
                                voice.speak("Form submit karne ke liye Accessibility service ON karni hogi, Settings me.")
                            } else if (com.flora.app.automation.FormFiller.hasPasswordField(service)) {
                                formSubmitPending = true
                                voice.speak("Ye form me password field hai, isliye submit se pehle confirm chahiye — dialog me Haan dabao to submit karungi.")
                            } else {
                                val ok = com.flora.app.automation.FormFiller.clickSubmit(service)
                                voice.speak(if (ok) "Form submit kar diya hai." else "Submit button nahi mila.")
                            }
                        }
                        return
                    }
                    // Phase 6B: image generate via external app — Gemini
                    // ya ChatGPT. "image banao" wala purana ChatGPT +
                    // Pollinations raasta chheda nahi hai; ye naya
                    // branch sirf "image generate karo" par chalta hai.
                    // Login user khud ek baar karega — password Flora
                    // kabhi nahi mangegi.
                    if (lower.startsWith("image generate karo") || lower.startsWith("gemini se image generate karo")) {
                        input = ""
                        var rest = when {
                            lower.startsWith("gemini se image generate karo ") ->
                                question.substring(30).trim()
                            lower.startsWith("image generate karo ") ->
                                question.substring(20).trim()
                            else -> ""
                        }
                        var appName: String? = null
                        val restLower = rest.lowercase()
                        if (restLower.endsWith(" gemini se")) {
                            appName = "Gemini"
                            rest = rest.dropLast(10).trim()
                        } else if (restLower.endsWith(" chatgpt pe")) {
                            appName = "ChatGPT"
                            rest = rest.dropLast(11).trim()
                        } else if (restLower.endsWith(" chat gpt pe")) {
                            appName = "ChatGPT"
                            rest = rest.dropLast(12).trim()
                        }
                        if (rest.isEmpty()) {
                            voice.speak("Kaisi image banau? Aise boliye: 'image generate karo ek sher, Gemini se'.")
                        } else if (appName == null) {
                            voice.speak("Kis app se image banau — Gemini ya ChatGPT? Aise boliye: 'image generate karo ek sher, Gemini se'.")
                        } else {
                            val prompt = rest
                            val app = appName
                            scope.launch {
                                val service = com.flora.app.automation.FloraAutomationService.instance
                                if (service == null) {
                                    voice.speak("Image banane ke liye Accessibility service ON karni hogi, Settings me.")
                                } else {
                                    voice.speak("'$app' se image bana rahi hun — pehle app kholegi, phir prompt likh kar generate dabayegi. Login tumne ek baar khud karna hoga.")
                                    val outcome = if (app == "Gemini") {
                                        val installed = com.flora.app.automation.AppAutomation.isInstalled(
                                            appContext, com.flora.app.automation.AppAutomation.GEMINI_PKG
                                        )
                                        if (!installed) {
                                            voice.speak("Gemini app installed nahi hai, Play Store khol raha hun — install poora hote hi dobara bol dena.")
                                            com.flora.app.automation.AppAutomation.openPlayStoreDetails(
                                                appContext, com.flora.app.automation.AppAutomation.GEMINI_PKG
                                            )
                                            null
                                        } else {
                                            com.flora.app.automation.ImageGenFlow.generateWithApp(
                                                appContext, service,
                                                com.flora.app.automation.AppAutomation.GEMINI_PKG, "Gemini",
                                                listOf("Ask Gemini", "Message", "Ask anything"),
                                                listOf("Send", "Send message"),
                                                prompt
                                            ) { p -> voice.speak(p) }
                                        }
                                    } else {
                                        // runChatGptImage khud screenshot-save nahi karta
                                        // (wo sirf app me banata hai) — isliye bring-back
                                        // yahan khud karo, warna "Saved" jhooth hota.
                                        val ok = runChatGptImage(prompt)
                                        if (!ok) {
                                            null
                                        } else {
                                            val jpeg = try { service.screenshotJpeg() } catch (e: Exception) { null }
                                            if (com.flora.app.automation.ImageGenFlow.saveJpegToGallery(appContext, jpeg)) {
                                                com.flora.app.automation.ImageOutcome.Saved("ChatGPT")
                                            } else {
                                                com.flora.app.automation.ImageOutcome.AppOnly("ChatGPT")
                                            }
                                        }
                                    }
                                    when (outcome) {
                                        is com.flora.app.automation.ImageOutcome.Saved -> {
                                            val msg = "Image ban gayi hai aur Gallery ke Flora folder me save kar di hai — Photos me dekh lo."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        }
                                        is com.flora.app.automation.ImageOutcome.AppOnly -> {
                                            val msg = "Image '$app' me ban gayi hai, par screenshot nahi le payi — app me hi dekh lo."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        }
                                        is com.flora.app.automation.ImageOutcome.Failed -> {
                                            val msg = "Image nahi bana payi: ${outcome.reason}"
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        }
                                        null -> {
                                        }
                                    }
                                }
                            }
                        }
                        return
                    }
                    // Phase 6: auto-reply design (ask-first) — notification
                    // ka message + pehle ke 5-6 messages padh kar draft
                    // banao, user ko sunao; bhejna sirf confirm dialog
                    // me Haan par hoga. Silent auto-reply kabhi nahi.
                    if (lower == "is notification ka jawab batao" ||
                        lower == "notification ka jawab batao" ||
                        lower == "is ka jawab batao"
                    ) {
                        input = ""
                        scope.launch {
                            try {
                                val all = com.flora.app.data.NotificationStore.load(appContext)
                                val latest = all.lastOrNull()
                                if (latest == null) {
                                    voice.speak("Koi notification save nahi hai — pehle Notifications se access ON karo.")
                                } else {
                                    proposeReply(latest.pkg)
                                }
                            } catch (e: Exception) {
                                voice.speak("Notifications nahi padh payi.")
                            }
                        }
                        return
                    }
                    if (lower.endsWith(" ka jawab batao")) {
                        input = ""
                        val appWord = question.dropLast(15).trim()
                        if (appWord.isEmpty()) {
                            voice.speak("Kis app ka jawab batau? Aise boliye: 'WhatsApp ka jawab batao'.")
                        } else {
                            scope.launch {
                                val matches = com.flora.app.automation.VerifiedAppLauncher.findByName(appContext, appWord)
                                if (matches.isEmpty()) {
                                    voice.speak("App '$appWord' nahi mili.")
                                } else if (matches.size > 1) {
                                    voice.speak("App '$appWord' ke liye ek se zyada app mili — naam poora bolo.")
                                } else {
                                    proposeReply(matches[0].packageName)
                                }
                            }
                        }
                        return
                    }
                    // Research voice commands (v2.22, P3-F12):
                    // ToolsScreen me ye tools pehle se the, awaaz
                    // se nahi chalte the. Sirf padhna hai, isliye
                    // Auto Mode gate nahi — bolna + chat me likhna.
                    if (lower.startsWith("search karo ") ||
                        lower.endsWith(" search karo") ||
                        lower.startsWith("matlab batao ") ||
                        lower.endsWith(" ka matlab") ||
                        lower.startsWith("stock price ") ||
                        lower.endsWith(" ka share price") ||
                        lower.startsWith("study karo ") ||
                        lower.endsWith(" par study")
                    ) {
                        input = ""
                        val (desc, job) = when {
                            lower.startsWith("search karo ") ->
                                "Search: ${question.substring(12).trim()}" to
                                    suspend { searchTools.search(question.substring(12).trim()) }
                            lower.endsWith(" search karo") ->
                                "Search: ${question.dropLast(12).trim()}" to
                                    suspend { searchTools.search(question.dropLast(12).trim()) }
                            lower.startsWith("matlab batao ") ->
                                "Matlab: ${question.substring(13).trim()}" to
                                    suspend { searchTools.defineWord(question.substring(13).trim()) }
                            lower.endsWith(" ka matlab") ->
                                "Matlab: ${question.dropLast(9).trim()}" to
                                    suspend { searchTools.defineWord(question.dropLast(9).trim()) }
                            lower.startsWith("stock price ") ->
                                "Stock: ${question.substring(12).trim()}" to
                                    suspend { searchTools.stockPrice(question.substring(12).trim()) }
                            lower.endsWith(" ka share price") ->
                                "Stock: ${question.dropLast(14).trim()}" to
                                    suspend { searchTools.stockPrice(question.dropLast(14).trim()) }
                            lower.startsWith("study karo ") ->
                                "Study: ${question.substring(11).trim()}" to
                                    suspend { searchTools.study(question.substring(11).trim()) }
                            else ->
                                "Study: ${question.dropLast(10).trim()}" to
                                    suspend { searchTools.study(question.dropLast(10).trim()) }
                        }
                        scope.launch {
                            try {
                                val result = job()
                                val spoken = result.take(500)
                                messages = messages + ChatMsg("Flora: $result", false)
                                voice.speak(spoken)
                            } catch (e: Exception) {
                                val msg = "$desc nahi mil paya, dobara bol dena."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
                        }
                        return
                    }
                    // Code-writer voice (v2.22, P3-F11): website
                    // starter pehle se ToolsScreen me tha, awaaz se
                    // nahi chalta tha. Imaandari: ye sirf starter
                    // HTML text hai (local template), poori website
                    // ya file save nahi — isliye bolkar bhi wahi
                    // bataya jaata hai, HTML sunaya nahi jaata.
                    if (lower.startsWith("website banao ") ||
                        lower.startsWith("website starter ")
                    ) {
                        input = ""
                        val idea = if (lower.startsWith("website banao "))
                            question.substring(14).trim()
                        else question.substring(16).trim()
                        if (idea.isEmpty()) {
                            val msg = "Kis cheez ki website banau? Aise boliye: 'website banao chai ki dukaan'."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                            return
                        }
                        val result = searchTools.websiteStarter(idea)
                        // Publish ke liye save (v2.22, G19): "website publish karo"
                        // isi file ko public link par daalta hai.
                        try {
                            java.io.File(appContext.filesDir, "last-website.html").writeText(result)
                        } catch (e: Exception) { }
                        messages = messages + ChatMsg("Flora: $result", false)
                        voice.speak("Website starter taiyaar hai — chat me dekh lo. Ye sirf shuruati HTML hai, poori website banani ho to detail bata dena.")
                        return
                    }
                    // Docs + email voice (v2.22, P3-F14): Docs
                    // screen pehle se thi, awaaz se nahi chalti thi.
                    // Docs sirf phone par save hote hain; email
                    // kabhi chhupke nahi bheja jaata — mail app
                    // khulta hai, bhejna user khud karta hai.
                    if (lower.startsWith("note save karo ") ||
                        lower == "documents dikhao" ||
                        lower == "mere documents dikhao" ||
                        lower.startsWith("email bhejo ") ||
                        lower == "email kholo"
                    ) {
                        input = ""
                        when {
                            lower.startsWith("note save karo ") -> {
                                val text = question.substring(15).trim()
                                if (text.isEmpty()) {
                                    val msg = "Kya save karun? Aise boliye: 'note save karo kal subah 9 baje meeting hai'."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                } else {
                                    scope.launch {
                                        try {
                                            docsStore.add(text.take(30), text)
                                            val msg = "Note save ho gaya."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        } catch (e: Exception) {
                                            val msg = "Note save nahi ho paya."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        }
                                    }
                                }
                            }
                            lower == "documents dikhao" || lower == "mere documents dikhao" -> {
                                scope.launch {
                                    try {
                                        val docs = docsStore.docs.first()
                                        if (docs.isEmpty()) {
                                            val msg = "Koi document save nahi hai."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        } else {
                                            val titles = docs.take(5).map { it.title }
                                            val msg = "Tumhare documents: " + titles.joinToString(" • ")
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        }
                                    } catch (e: Exception) {
                                        val msg = "Documents nahi padh payi."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    }
                                }
                            }
                            lower.startsWith("email bhejo ") -> {
                                val addr = question.substring(12).trim()
                                if (!addr.contains("@")) {
                                    val msg = "Email address samajh nahi aaya. Aise boliye: 'email bhejo kisi@example.com'."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                } else {
                                    try {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = android.net.Uri.parse("mailto:$addr")
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        appContext.startActivity(intent)
                                        val msg = "Mail app khol diya hai — subject aur message likh kar tum khud bhej dena."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    } catch (e: Exception) {
                                        val msg = "Mail app nahi khul paya."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    }
                                }
                            }
                            else -> {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = android.net.Uri.parse("mailto:")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    appContext.startActivity(intent)
                                    voice.speak("Mail app khol diya hai.")
                                } catch (e: Exception) {
                                    voice.speak("Mail app nahi khul paya.")
                                }
                            }
                        }
                        return
                    }
                    // Market overview voice (v2.22, P3-F15):
                    // Single stock F12 me tha; market ka haal ek
                    // saath — Nifty, Sensex, Bitcoin (Yahoo Finance,
                    // wahi stockPrice raasta; live ki pushti hamesha
                    // exchange/app se karne ko kaha jaata hai).
                    if (lower == "market kaisa hai" ||
                        lower == "market dikhao" ||
                        lower == "share market kaisa hai"
                    ) {
                        input = ""
                        scope.launch {
                            try {
                                val lines = listOf(
                                    "Nifty 50" to "^NSEI",
                                    "Sensex" to "^BSESN",
                                    "Bitcoin" to "BTC-USD"
                                ).map { (label, sym) ->
                                    "$label: " + searchTools.stockPrice(sym).substringBefore("\n")
                                }
                                val full = lines.joinToString("\n") +
                                    "\nNote: live price ki pushti exchange/app se karo. Source: Yahoo Finance"
                                messages = messages + ChatMsg("Flora: $full", false)
                                voice.speak(lines.joinToString(" • "))
                            } catch (e: Exception) {
                                val msg = "Market ka haal nahi mil paya, dobara bol dena."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
                        }
                        return
                    }
                    // Image share with confirm (v2.22, P3-F13):
                    // "image banao" se bani Gallery/Flora image ko
                    // share sheet se bhejo — app user khud chunta
                    // hai, post Flora kabhi chhupke nahi karti.
                    // Share sheet hi confirm hai.
                    if (lower == "image share karo" ||
                        lower == "aakhri image share karo"
                    ) {
                        input = ""
                        scope.launch {
                            try {
                                val uri = withContext(Dispatchers.IO) {
                                    var found: android.net.Uri? = null
                                    try {
                                        val cursor = appContext.contentResolver.query(
                                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                            arrayOf(MediaStore.Images.Media._ID),
                                            MediaStore.Images.Media.RELATIVE_PATH + "=?",
                                            arrayOf("Pictures/Flora"),
                                            MediaStore.Images.Media.DATE_ADDED + " DESC"
                                        )
                                        cursor?.use {
                                            if (it.moveToFirst()) {
                                                val id = it.getLong(0)
                                                found = android.content.ContentUris.withAppendedId(
                                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                                                )
                                            }
                                        }
                                    } catch (e: Exception) { }
                                    found
                                }
                                if (uri == null) {
                                    val msg = "Share karne layak koi image nahi mili. Pehle 'image banao' bolkar ek image banao."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                } else {
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/jpeg"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    appContext.startActivity(
                                        Intent.createChooser(send, "Image kahan share karni hai?")
                                    )
                                    val msg = "Share sheet khol diya hai — jis app par post karni hai, use chuno."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                }
                            } catch (e: Exception) {
                                val msg = "Image share nahi ho payi."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
                        }
                        return
                    }
                    // General tap/type/swipe voice commands (v2.22,
                    // P2-F4 poora): primitives FloraAutomationService
                    // me pehle se the, ab awaaz se bhi chalte hain.
                    // GATE: Accessibility OFF ho to guidance; Auto
                    // Mode OFF ho to execute NAHI (sirf batana) —
                    // har chalaya hua kaam Auto Log me darj hota hai.
                    if (lower.startsWith("tap karo ") ||
                        lower.startsWith("click karo ") ||
                        lower.endsWith(" par tap karo") ||
                        lower == "scroll up karo" ||
                        lower == "upar scroll karo" ||
                        lower == "scroll down karo" ||
                        lower == "neeche scroll karo" ||
                        lower.startsWith("type karo ") ||
                        lower == "back jao" ||
                        lower == "wapas jao" ||
                        lower == "home jao"
                    ) {
                        input = ""
                        val service = FloraAutomationService.instance
                        if (service == null) {
                            val msg = "Tap/type/scroll ke liye Accessibility ON chahiye. Permissions screen me Accessibility ON karo, phir dobara bol dena."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                            return
                        }
                        if (!autoMode) {
                            val msg = "Ye kaam Auto Mode me hota hai — Auto Mode ON karo, phir bol dena."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                            return
                        }
                        val (desc, ok) = when {
                            lower.startsWith("tap karo ") ->
                                "Tap: ${question.substring(9).trim()}" to
                                    service.clickText(question.substring(9).trim())
                            lower.startsWith("click karo ") ->
                                "Tap: ${question.substring(11).trim()}" to
                                    service.clickText(question.substring(11).trim())
                            lower.endsWith(" par tap karo") ->
                                "Tap: ${question.dropLast(13).trim()}" to
                                    service.clickText(question.dropLast(13).trim())
                            lower == "scroll up karo" || lower == "upar scroll karo" ->
                                "Scroll up" to service.swipeUp()
                            lower == "scroll down karo" || lower == "neeche scroll karo" ->
                                "Scroll down" to service.swipeDown()
                            lower.startsWith("type karo ") -> {
                                val t = question.substring(10).trim()
                                val typed = service.typeIntoFocused(t) ||
                                    service.setTextOnFirstEditable(t)
                                "Type: $t" to typed
                            }
                            lower == "back jao" || lower == "wapas jao" ->
                                "Back" to service.goBack()
                            else ->
                                "Home" to service.goHome()
                        }
                        val msg = if (ok) "$desc ho gaya." else "$desc nahi ho paya — screen par wo cheez dikh rahi hai ya nahi, dekh lo."
                        messages = messages + ChatMsg("Flora: $msg", false)
                        voice.speak(msg)
                        scope.launch {
                            try { autoLogStore.add("Voice $desc -> ${if (ok) "ho gaya" else "fail"}") } catch (e: Exception) { }
                        }
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
                    // Voice: "whatsapp karo <naam> <message>" (v2.22,
                    // P4-F3): SMS wale raaste par — naam resolve
                    // (seekha/exact/poochho), phir WhatsApp chat
                    // kholta hai, message pehle se bhara hua.
                    // Flora kabhi chhupke WhatsApp nahi bhejti —
                    // Send user khud dabata hai (koi official API
                    // nahi hai, isliye yehi imaandari wala raasta hai).
                    if (lower.startsWith("whatsapp karo ")) {
                        val rest = question.substring(14).trim()
                        val parts = rest.split(" ", limit = 2)
                        input = ""
                        if (parts.size < 2) {
                            val msg = "Aise boliye: 'whatsapp karo Ram kal milte hain'."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        } else {
                            val firstDigits = parts[0].filter { it.isDigit() || it == '+' }
                            val text = parts[1]
                            if (firstDigits.length >= 7) {
                                runContactAction(firstDigits, "whatsapp", text)
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
                                                runContactAction(foundDigits, "whatsapp", text)
                                            }
                                        }
                                        is ContactResolver.Result.Ambiguous -> {
                                            pendingContact = PendingContact(parts[0], res.candidates, "whatsapp", text)
                                            val names = res.candidates.joinToString(", ") { it.name }
                                            val msg = "'${parts[0]}' naam ke kai log mile — $names. Kisko WhatsApp karun? Naam bolo ya pehla/doosra bolo."
                                            messages = messages + ChatMsg("Flora: $msg", false)
                                            voice.speak(msg)
                                        }
                                        ContactResolver.Result.NotFound -> {
                                            pendingContact = PendingContact(parts[0], emptyList(), "whatsapp", text)
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
                    // Screen dekho helper (v2.22 autonomy): session
                    // dhoondho, na mile to dialog kholo, frame lekar
                    // vision se jawab bolo. Chat + app-kholo-dekho
                    // dono isi se chalte hain.
                    fun lookAtScreen(prompt: String) {
                        scope.launch {
                            // Key check PEHLE (Phase 3): bina key ke screenshot
                            // lena bekaar hai — pehle hi imaandaari se batao.
                            val key = prefs.geminiApiKey()
                            if (key.isEmpty()) {
                                voice.speak("Screen dekhne ke liye Settings me apni free Gemini key daaliye.")
                                return@launch
                            }
                            // One-time-setup raasta PEHLE: accessibility
                            // ON ho to bina dialog, bina Allow tap ke
                            // screenshot lo. Na mile to neeche wala
                            // MediaProjection raasta (dialog + Allow).
                            val accessJpeg = try {
                                FloraAutomationService.instance?.screenshotJpeg()
                            } catch (e: Exception) {
                                null
                            }
                            if (accessJpeg != null) {
                                screenReaskCount = 0
                                // Tree context (Maya jaisa): vision ko
                                // sirf photo nahi, saamne wali app +
                                // screen par likha text bhi batao —
                                // taaki jawab photo + tree dono par
                                // tika ho. Ye list kahin save nahi hoti.
                                val treeTexts = try {
                                    FloraAutomationService.instance?.visibleTexts(25)
                                } catch (e: Exception) {
                                    null
                                }
                                val richPrompt = if (!treeTexts.isNullOrEmpty()) {
                                    val fg = try {
                                        FloraAutomationService.instance?.foregroundPackage()
                                    } catch (e: Exception) {
                                        null
                                    }
                                    "Saamne wali app: ${fg ?: "pata nahi"}. " +
                                        "Screen par ye likha dikh raha hai: " +
                                        treeTexts.joinToString(" | ") + ". " + prompt
                                } else {
                                    prompt
                                }
                                val reply = vision.askAboutImage(richPrompt, accessJpeg, key)
                                if (reply == null) {
                                    voice.speak("Is baar dekh nahi payi — ek baar phir poochho na.")
                                } else {
                                    messages = messages + ChatMsg(reply, false)
                                    voice.speak(reply.take(500))
                                }
                                return@launch
                            }
                            val session = if (screenGranted) ScreenCapture.openShared(
                                appContext, screenResultCode, screenResultData
                            ) else null
                            if (session == null) {
                                // Re-ask guard (Phase 3): ek baar dialog
                                // dobara kholna theek hai; uske baad bhi
                                // session na bane to loop mat chalao —
                                // imaandaar failure bolo. MediaProjection
                                // token process ke saath khatam ho jaata
                                // hai (Android limit) — isliye purani
                                // permission ka dobara kaam na karna
                                // normal hai, chhupana nahi.
                                if (screenReaskCount >= 1) {
                                    screenReaskCount = 0
                                    screenGranted = false
                                    screenResultData = null
                                    pendingScreenQuestion = null
                                    voice.speak("Screen permission dobara dene ke baad bhi session nahi bana. Accessibility service ON karo — usse bina dialog ke dekh lungi, Settings me.")
                                    return@launch
                                }
                                screenReaskCount++
                                screenGranted = false
                                screenResultData = null
                                pendingScreenQuestion = "screen dekho"
                                val mgr = appContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                                    as MediaProjectionManager
                                screenLauncher.launch(mgr.createScreenCaptureIntent())
                                voice.speak("Pehle screen permission chahiye — dialog khol raha hun, Allow dabao, phir dobara bolo.")
                                return@launch
                            }
                            var jpeg: ByteArray? = null
                            for (attempt in 0 until 3) {
                                jpeg = session.captureFrame()
                                if (jpeg != null) break
                            }
                            if (jpeg == null) {
                                ScreenCapture.LiveSessionHolder.clear()
                                if (screenReaskCount >= 1) {
                                    screenReaskCount = 0
                                    screenGranted = false
                                    screenResultData = null
                                    pendingScreenQuestion = null
                                    voice.speak("Screen abhi bhi dikh nahi rahi — Accessibility service ON karo, usse bina dialog ke dekh lungi.")
                                    return@launch
                                }
                                screenReaskCount++
                                screenGranted = false
                                screenResultData = null
                                pendingScreenQuestion = "screen dekho"
                                val mgr = appContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                                    as MediaProjectionManager
                                screenLauncher.launch(mgr.createScreenCaptureIntent())
                                voice.speak("Screen abhi dikh nahi rahi — permission dobara de do, phir main dekh lungi.")
                                return@launch
                            }
                            screenReaskCount = 0
                            val reply = vision.askAboutImage(prompt, jpeg, key)
                            if (reply == null) {
                                voice.speak("Is baar dekh nahi payi — ek baar phir poochho na.")
                            } else {
                                messages = messages + ChatMsg(reply, false)
                                voice.speak(reply.take(500))
                            }
                        }
                    }
                    // Live screen dekho (Phase 3): accessibility frames
                    // refresh karke live jaisa — 3 frames, aakhri frame +
                    // tree vision ko. Sachha continuous video nahi hai
                    // (one-shot frames hain) — user ko "video" kabhi nahi bolna.
                    fun lookAtScreenLive() {
                        scope.launch {
                            val key = prefs.geminiApiKey()
                            if (key.isEmpty()) {
                                voice.speak("Screen dekhne ke liye Settings me apni free Gemini key daaliye.")
                                return@launch
                            }
                            // Accessibility-first: 3 frames, ~1.2s gap.
                            var lastJpeg: ByteArray? = null
                            var firstTree: List<String>? = null
                            var lastTree: List<String>? = null
                            var lastFg: String? = null
                            var gotFrame = false
                            for (i in 0 until 3) {
                                val jpeg = try {
                                    FloraAutomationService.instance?.screenshotJpeg()
                                } catch (e: Exception) {
                                    null
                                }
                                if (jpeg != null) {
                                    gotFrame = true
                                    lastJpeg = jpeg
                                    val tree = try {
                                        FloraAutomationService.instance?.visibleTexts(25)
                                    } catch (e: Exception) {
                                        null
                                    }
                                    if (firstTree == null) firstTree = tree
                                    lastTree = tree
                                    lastFg = try {
                                        FloraAutomationService.instance?.foregroundPackage()
                                    } catch (e: Exception) {
                                        null
                                    }
                                    screenReaskCount = 0
                                }
                                if (i < 2) delay(1200)
                            }
                            if (!gotFrame || lastJpeg == null) {
                                // Accessibility se na mila to MediaProjection
                                // fallback — wahi dialog flow (ek baar, loop nahi).
                                val session = if (screenGranted) ScreenCapture.openShared(
                                    appContext, screenResultCode, screenResultData
                                ) else null
                                if (session == null) {
                                    if (screenReaskCount >= 1) {
                                        screenReaskCount = 0
                                        screenGranted = false
                                        screenResultData = null
                                        pendingScreenQuestion = null
                                        voice.speak("Screen permission dobara dene ke baad bhi session nahi bana. Accessibility service ON karo — usse bina dialog ke dekh lungi, Settings me.")
                                    } else {
                                        screenReaskCount++
                                        screenGranted = false
                                        screenResultData = null
                                        pendingScreenQuestion = "live screen dekho"
                                        val mgr = appContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                                            as MediaProjectionManager
                                        screenLauncher.launch(mgr.createScreenCaptureIntent())
                                        voice.speak("Pehle screen permission chahiye — dialog khol raha hun, Allow dabao, phir dobara bolo.")
                                    }
                                    return@launch
                                }
                                var jpeg: ByteArray? = null
                                for (attempt in 0 until 3) {
                                    jpeg = session.captureFrame()
                                    if (jpeg != null) break
                                    delay(1200)
                                }
                                if (jpeg == null) {
                                    ScreenCapture.LiveSessionHolder.clear()
                                    screenGranted = false
                                    screenResultData = null
                                    pendingScreenQuestion = null
                                    voice.speak("Screen abhi dikh nahi rahi — Accessibility service ON karo, usse bina dialog ke dekh lungi.")
                                    return@launch
                                }
                                lastJpeg = jpeg
                                screenReaskCount = 0
                            }
                            val treeNote = if (!lastTree.isNullOrEmpty()) {
                                "Saamne wali app: ${lastFg ?: "pata nahi"}. Screen par ye likha dikh raha hai: " +
                                    lastTree.joinToString(" | ") + ". "
                            } else ""
                            val changeNote = if (firstTree != null && lastTree != null && firstTree != lastTree) {
                                "Pehle frame se ab tak screen par ye badla: pehle ye tha: " +
                                    firstTree.joinToString(" | ") + ". "
                            } else ""
                            // Nullable fix (CI fail 40775751163): lastJpeg ek
                            // mutable var hai — if-block ke andar assignment
                            // ke baad pehle wali null-check cast nahi rehti.
                            // Isliye naya val + explicit null-check (AGENTS.md
                            // wala `?: return` lesson, yahan if-form me taaki
                            // imaandaar message bhi bole).
                            val frame = lastJpeg
                            if (frame == null) {
                                voice.speak("Screen abhi dikh nahi rahi — Accessibility service ON karo, usse bina dialog ke dekh lungi.")
                                return@launch
                            }
                            val reply = vision.askAboutImage(
                                treeNote + changeNote +
                                    "Ye live dekhne ka aakhri frame hai (kuch second ke andar liye gaye frames me se). " +
                                    "Is screen par abhi kya chal raha hai, sab kuch detail me batao?",
                                frame, key
                            )
                            if (reply == null) {
                                voice.speak("Is baar dekh nahi payi — ek baar phir poochho na.")
                            } else {
                                messages = messages + ChatMsg(reply, false)
                                voice.speak(reply.take(500))
                            }
                        }
                    }
                    // Screen share + screen dekho — BOL KAR (v2.22).
                    // Permission dialog Flora khud kholegi; Allow ka ek
                    // tap Android ka niyam hai, uske baad session
                    // holder me zinda rehta hai, dobara tap nahi.
                    if (lower == "screen share chalu karo" || lower == "screen chalu karo" ||
                        lower == "screen share on karo"
                    ) {
                        input = ""
                        if (FloraAutomationService.isEnabled()) {
                            voice.speak("Screen dekhna taiyaar hai — ab 'screen dekho' bologe to bina kisi Allow ke dekh lungi.")
                        } else if (screenGranted && screenResultData != null &&
                            ScreenCapture.openShared(appContext, screenResultCode, screenResultData) != null
                        ) {
                            voice.speak("Screen share pehle se chalu hai. 'Screen dekho' bolo, main dekh kar bata dungi.")
                        } else {
                            screenGranted = false
                            screenResultData = null
                            val mgr = appContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                                as MediaProjectionManager
                            screenLauncher.launch(mgr.createScreenCaptureIntent())
                            voice.speak("Screen permission ka dialog khol raha hun — Allow dabao, phir 'screen dekho' bolna.")
                        }
                        return
                    }
                    if (lower == "screen share band karo" || lower == "screen band karo" ||
                        lower == "screen share off karo"
                    ) {
                        input = ""
                        ScreenCapture.LiveSessionHolder.clear()
                        screenGranted = false
                        screenResultData = null
                        screenReaskCount = 0
                        voice.speak("Screen share band kar diya.")
                        return
                    }
                    // Live screen dekho (Phase 3): fuzzy screen branch se
                    // PEHLE (warna "live screen dekho" wahi gir jaata).
                    if (lower == "live screen dekho" || lower == "screen live dekho" ||
                        lower == "live screen dikhao"
                    ) {
                        input = ""
                        lookAtScreenLive()
                        return
                    }
                    // Screen dekho — FUZZY (Phase 3): sirf 4 exact strings
                    // nahi. "pe"/"par" dono, trailing "batao"/"dikhao"
                    // ignore — contains se. Share branches upar return kar
                    // chuke hain, isliye "screen share..." yahan nahi pahunchega.
                    val screenLook = lower == "screen dekho" ||
                        lower.startsWith("screen dekho ") ||
                        (lower.contains("screen") && (lower.contains("kya") ||
                            lower.contains("dikhao") || lower.contains("batao") ||
                            lower.contains("chal raha")))
                    if (screenLook) {
                        input = ""
                        lookAtScreen("Is screen par kya dikh raha hai, sab kuch detail me batao?")
                        return
                    }
                    // App kholo aur dekho — BOL KAR (v2.22). Compound
                    // block se PEHLE (warna "kholo aur dekho" compound
                    // me galat app nikalta).
                    if (lower.startsWith("app kholo aur dekho ") || lower.endsWith(" kholo aur dekho")) {
                        val name = if (lower.startsWith("app kholo aur dekho ")) {
                            lower.substringAfter("dekho", "").trim()
                        } else {
                            lower.substringBefore(" kholo aur dekho").trim()
                        }
                        input = ""
                        if (name.isEmpty()) {
                            voice.speak("Kaun si app khol kar dekhun? Naam boliye, jaise 'WhatsApp kholo aur dekho'.")
                        } else {
                            val matches = VerifiedAppLauncher.findByName(appContext, name)
                            if (matches.isEmpty()) {
                                voice.speak("'$name' naam ki app nahi mili, naam dobara boliye.")
                            } else if (matches.size > 1) {
                                val names = matches.take(3).joinToString(", ") { it.label }
                                voice.speak("Ek se zyada app mili: $names. Poora naam boliye.")
                            } else {
                                val app = matches[0]
                                runGuardedVoice { scope.launch {
                                    when (VerifiedAppLauncher.openVerified(appContext, app)) {
                                        VerifiedAppLauncher.OpenResult.Opened,
                                        VerifiedAppLauncher.OpenResult.AlreadyOpen -> {
                                            voice.speak("${app.label} khol diya, screen dekh rahi hun.")
                                            delay(1500)
                                            lookAtScreen("Is screen par kya dikh raha hai, sab kuch detail me batao?")
                                        }
                                        VerifiedAppLauncher.OpenResult.NoVerifier -> {
                                            voice.speak("${app.label} kholna verify nahi kar paya — Accessibility service ON karna hoga, Settings me.")
                                        }
                                        else -> {
                                            voice.speak("${app.label} nahi khul paya.")
                                        }
                                    }
                                }
                                }
                            }
                        }
                        return
                    }
                    // Continuous mic — BOL KAR (v2.22).
                    if (lower == "continuous mic chalu karo" || lower == "mic chalu karo" ||
                        lower == "sunna chalu karo"
                    ) {
                        input = ""
                        continuousOn = true
                        onContinuousStart()
                        voice.speak("Sunna chalu ho gaya hai — ab jo bologe, main sunungi.")
                        return
                    }
                    if (lower == "continuous mic band karo" || lower == "mic band karo" ||
                        lower == "sunna band karo" || lower == "mute karo"
                    ) {
                        input = ""
                        continuousOn = false
                        onContinuousStop()
                        voice.speak("Sunna band kar diya hai.")
                        return
                    }
                    // Compound voice (v2.22): "X kholo aur <kaam>" /
                    // "kholo X aur <kaam>" / "... phir <kaam>" — pehla
                    // hissa app kholega, doosra hissa usi sendChat se
                    // chalega, taaki ek command par ek kaam karke
                    // rukna band ho. App kholna direct + logged hai
                    // (wapas liya ja sakta hai); call/sms ka confirm
                    // niyam doosre hisse par phir bhi lagu rahega.
                    val sep = when {
                        lower.contains(" aur ") -> " aur "
                        lower.contains(" phir ") -> " phir "
                        else -> null
                    }
                    if (sep != null) {
                        val first = lower.substringBefore(sep).trim()
                        val rest = lower.substringAfter(sep).trim()
                        val appName = when {
                            first.startsWith("open ") || first.startsWith("kholo ") ->
                                first.substringAfter(" ").trim()
                            first.endsWith(" kholo") &&
                                first !in setOf("camera kholo", "yaadein kholo", "memories kholo") &&
                                !first.startsWith("chrome me ") && !first.startsWith("website ") ->
                                first.removeSuffix(" kholo").trim()
                            else -> ""
                        }
                        if (appName.isNotEmpty() && rest.isNotEmpty()) {
                            input = ""
                            val matches = VerifiedAppLauncher.findByName(appContext, appName)
                            when {
                                matches.isEmpty() -> {
                                    val msg = "'$appName' naam ki app nahi mili, naam dobara boliye."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                }
                                matches.size > 1 -> {
                                    val names = matches.take(3).joinToString(", ") { it.label }
                                    val msg = "Ek se zyada app mili: $names. Poora naam boliye."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                }
                                else -> {
                                    autoExecuteOpen(matches[0]) { result ->
                                        val verified = result == VerifiedAppLauncher.OpenResult.Opened ||
                                            result == VerifiedAppLauncher.OpenResult.AlreadyOpen
                                        if (verified) {
                                            scope.launch {
                                                delay(1000)
                                                sendChatHolder[0]?.invoke(rest)
                                            }
                                        }
                                    }
                                }
                            }
                            return
                        }
                    }
                    // Voice: "open <app>" / "kholo <app>" — ek confirmation, phir app khul jayegi.
                    if (lower.startsWith("open ") || lower.startsWith("kholo ")) {
                        val name = question.substringAfter(" ").trim()
                        input = ""
                        val matches = VerifiedAppLauncher.findByName(appContext, name)
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
                        } else {
                            scope.launch {
                                val result = VerifiedAppLauncher.openWebsiteVerified(appContext, site)
                                val msg = if (result == VerifiedAppLauncher.OpenResult.NotVisible ||
                                    result == VerifiedAppLauncher.OpenResult.NoLaunchIntent
                                ) {
                                    "Website nahi khul payi."
                                } else {
                                    "$site khol raha hun."
                                }
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
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
                            scope.launch {
                                VerifiedAppLauncher.openWebsiteVerified(
                                    appContext,
                                    "https://www.youtube.com/results?search_query=" +
                                        android.net.Uri.encode(song)
                                )
                            }
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
                    // Code likho (v2.22, Phase 1, G15): bola hua
                    // description -> brain se sirf code -> phone par
                    // file save + chat me dikhao. Code sunaya nahi
                    // jaata (lamba hota hai), sirf confirmation boli
                    // jaati hai. Publish ke liye "website publish karo"
                    // wala raasta alag hai (sirf website HTML ke liye).
                    if (lower.startsWith("code likho ")) {
                        input = ""
                        val desc = question.substring(11).trim()
                        if (desc.isEmpty()) {
                            val msg = "Kaisa code likhun? Aise boliye: 'code likho ek calculator ka HTML'."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        } else {
                            scope.launch {
                                try {
                                    val code = brain.ask(
                                        "Sirf code likho, koi explanation ya markdown ke bahar ki baat nahi. Task: $desc"
                                    )
                                    val name = "code-" + System.currentTimeMillis() + ".txt"
                                    try {
                                        java.io.File(appContext.filesDir, name).writeText(code)
                                    } catch (e: Exception) { }
                                    messages = messages + ChatMsg("Flora: Code ban gaya ($name) —\n$code", false)
                                    voice.speak("Code ban gaya hai, chat me dekh lo.")
                                } catch (e: Exception) {
                                    val msg = "Code nahi ban paya, dobara bol dena."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                }
                            }
                        }
                        return
                    }
                    // Hisaab (v2.22, Phase 1): ab tak kya hua + aage
                    // kya karna hai — yaadein, routines, jobs se.
                    // Sirf padhna hai, koi action nahi hota.
                    if (lower == "hisaab batao" || lower == "mera hisaab batao" ||
                        lower == "kya kiya" || lower == "kya karna hai" ||
                        lower == "ab tak kya hua"
                    ) {
                        input = ""
                        scope.launch {
                            try {
                                val mems = try { memoriesStore.items.first().takeLast(5) } catch (e: Exception) { emptyList() }
                                val jobs = try { com.flora.app.data.JobStore.load(appContext) } catch (e: Exception) { emptyList() }
                                val routines = try { com.flora.app.data.RoutineStore.load(appContext) } catch (e: Exception) { emptyList() }
                                val pending = jobs.filter { it.enabled }.joinToString(", ") {
                                    it.routineName + " (" + it.hour + ":" + it.minute.toString().padStart(2, '0') + ")"
                                }
                                val detail = StringBuilder()
                                detail.append("Ab tak: ${mems.size} aakhri yaadein")
                                if (mems.isNotEmpty()) detail.append(" — " + mems.joinToString("; "))
                                detail.append("\nRoutines: ${routines.size} (${routines.joinToString(", ") { it.name }})")
                                detail.append("\nJobs: ${jobs.size}")
                                if (pending.isNotEmpty()) detail.append(" — aage karna hai: $pending") else detail.append(" — koi pending job nahi")
                                messages = messages + ChatMsg("Flora: $detail", false)
                                val spoken = if (pending.isNotEmpty()) "Hisaab ye raha — aage karna hai: $pending." else "Sab kaam poora hai, koi pending job nahi hai."
                                voice.speak(spoken)
                            } catch (e: Exception) {
                                val msg = "Hisaab nahi nikal paya, dobara bol dena."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
                        }
                        return
                    }
                    // Overlay mic (v2.22, Phase 1): har screen par
                    // Flora ka mic button. Permission Android ka
                    // niyam hai (ek baar user khud dega); bina
                    // permission ke overlay nahi dikhega.
                    if (lower == "overlay mic chalu karo" || lower == "overlay button chalu karo") {
                        input = ""
                        if (!android.provider.Settings.canDrawOverlays(appContext)) {
                            try {
                                val intent = android.content.Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:" + appContext.packageName)
                                )
                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                appContext.startActivity(intent)
                            } catch (e: Exception) { }
                            voice.speak("Overlay ke liye ek baar permission chahiye — settings khol raha hun, Flora ko doosri app ke upar dikhne ki permission do, phir dobara bolo.")
                        } else {
                            try {
                                appContext.startForegroundService(
                                    android.content.Intent(appContext, com.flora.app.automation.OverlayMicService::class.java)
                                )
                                voice.speak("Overlay mic chalu ho gaya — ab kisi bhi screen par Flora ka mic button dikhega, dabate hi main sunungi.")
                            } catch (e: Exception) {
                                voice.speak("Overlay mic chalu nahi ho paya, dobara koshish karo.")
                            }
                        }
                        return
                    }
                    if (lower == "overlay mic band karo" || lower == "overlay button band karo") {
                        input = ""
                        try {
                            appContext.stopService(
                                android.content.Intent(appContext, com.flora.app.automation.OverlayMicService::class.java)
                            )
                            voice.speak("Overlay mic band kar diya.")
                        } catch (e: Exception) {
                            voice.speak("Overlay mic band nahi ho paya.")
                        }
                        return
                    }
                    // In-app browser (v2.22, Phase 5, G7): Flora ke
                    // andar ka browser. "browser me <site> kholo"
                    // se site khulti hai; wapas/aage/reload/padho/
                    // link sab BrowserScreen ke through chalte hain.
                    if (lower == "browser kholo") {
                        input = ""
                        screen = Screen.BROWSER
                        voice.speak("Browser khol raha hun.")
                        return
                    }
                    if (lower.startsWith("browser me ") && lower.endsWith(" kholo")) {
                        input = ""
                        val site = question.substring(11, question.length - 6).trim()
                        if (site.isEmpty()) {
                            screen = Screen.BROWSER
                            voice.speak("Browser khol raha hun.")
                        } else {
                            val url = com.flora.app.tools.BrowserController.normalize(site)
                            com.flora.app.tools.BrowserNav.request(url = url)
                            screen = Screen.BROWSER
                            voice.speak("Browser me khol raha hun.")
                        }
                        return
                    }
                    if (lower == "browser me wapas jao") {
                        input = ""
                        com.flora.app.tools.BrowserNav.request(action = "back")
                        screen = Screen.BROWSER
                        voice.speak("Browser me wapas ja raha hun.")
                        return
                    }
                    if (lower == "browser me aage jao") {
                        input = ""
                        com.flora.app.tools.BrowserNav.request(action = "forward")
                        screen = Screen.BROWSER
                        voice.speak("Browser me aage ja raha hun.")
                        return
                    }
                    if (lower == "browser me reload karo") {
                        input = ""
                        com.flora.app.tools.BrowserNav.request(action = "reload")
                        screen = Screen.BROWSER
                        voice.speak("Browser me page dobara load kar raha hun.")
                        return
                    }
                    if (lower == "ye page padh kar sunao" || lower == "is page par kya hai") {
                        input = ""
                        com.flora.app.tools.BrowserNav.request(action = "read")
                        screen = Screen.BROWSER
                        voice.speak("Page padh kar sunata hun.")
                        return
                    }
                    if (lower == "links dikhao") {
                        input = ""
                        com.flora.app.tools.BrowserNav.request(action = "links")
                        screen = Screen.BROWSER
                        voice.speak("Is page ke links nikal raha hun.")
                        return
                    }
                    if (lower.endsWith(" link kholo")) {
                        val word = lower.removeSuffix(" link kholo").trim()
                        val idx = when (word) {
                            "pehla" -> 0
                            "doosra" -> 1
                            "teesra" -> 2
                            "chautha" -> 3
                            "panchva" -> 4
                            else -> -1
                        }
                        if (idx >= 0) {
                            input = ""
                            com.flora.app.tools.BrowserNav.request(action = "link:$idx")
                            screen = Screen.BROWSER
                            voice.speak("Link khol raha hun.")
                            return
                        }
                    }
                    // Generic screen map (v2.22, Phase 5, route
                    // parity): Flora ki apni screens "<naam>
                    // kholo" se khulti hain. Ye branch app-kholo
                    // branch ke baad aati hai (wo sirf "kholo "
                    // se SHURU hone wale commands pakadti hai),
                    // aur camera/yaadein/sos jaise pehle wale
                    // exact branches isse pehle chal chuke hote
                    // hain — isliye yahan sirf bachi screens hain.
                    if (lower.endsWith(" kholo")) {
                        val name = lower.removeSuffix(" kholo").trim()
                        val target = when (name) {
                            "chat" -> Screen.CHAT
                            "settings" -> Screen.SETTINGS
                            "tools" -> Screen.TOOLS
                            "docs", "documents" -> Screen.DOCS
                            "whiteboard" -> Screen.WHITEBOARD
                            "home" -> Screen.HOME
                            "avatar" -> Screen.AVATAR
                            "live screen" -> Screen.LIVE
                            "journal" -> Screen.JOURNAL
                            "permissions" -> Screen.PERMISSIONS
                            "licence", "license" -> Screen.LICENCE
                            "auto log" -> Screen.AUTO_LOG
                            "models" -> Screen.MODELS
                            "awaaz sikhao", "voice enrol" -> Screen.VOICE_ENROL
                            "notifications" -> Screen.NOTIFICATIONS
                            "face", "chehra" -> Screen.FACE
                            "connect" -> Screen.CONNECT
                            "privacy" -> Screen.PRIVACY
                            "terms" -> Screen.TERMS
                            "auto" -> Screen.AUTO
                            "routines" -> Screen.ROUTINES
                            "scan" -> Screen.SCAN
                            "gaadi", "driving" -> Screen.DRIVING
                            "sos" -> Screen.SOS
                            "lock", "screen lock" -> Screen.LOCK
                            "pc link", "pc" -> Screen.PC_LINK
                            else -> null
                        }
                        if (target != null) {
                            input = ""
                            screen = target
                            voice.speak("Khol raha hun.")
                            return
                        }
                    }
                    // Voice: "<app> kholo" (suffix, natural Hindi) —
                    // prefix branch jaisa hi behaviour: ek match →
                    // confirm/auto, zyada → ambiguity, zero →
                    // not-found. Flora ki apni screens upar wale
                    // map me pakdi ja chuki hain; G16 (chrome /
                    // github / vercel) neeche aata hai, isliye ye
                    // teen + "link" wale naam yahan nahi pakde jaate.
                    if (lower.endsWith(" kholo")) {
                        val name = lower.removeSuffix(" kholo").trim()
                        if (name.isNotEmpty() && name !in setOf("chrome", "github", "vercel") &&
                            !name.endsWith(" link")
                        ) {
                            input = ""
                            val matches = VerifiedAppLauncher.findByName(appContext, name)
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
                    }
                    // Live wallpaper (v2.22, Phase 5, G9): system
                    // chooser khulta hai, user khud Flora
                    // wallpaper chunta hai — chhupke nahi lagta.
                    if (lower == "live wallpaper lagao") {
                        input = ""
                        try {
                            val intent = android.content.Intent(
                                android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
                            ).apply {
                                putExtra(
                                    android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                    android.content.ComponentName(
                                        appContext,
                                        com.flora.app.automation.FloraWallpaperService::class.java
                                    )
                                )
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            appContext.startActivity(intent)
                            voice.speak("Live wallpaper chooser khol raha hun — wahan Flora wallpaper chun lo.")
                        } catch (e: Exception) {
                            voice.speak("Live wallpaper chooser nahi khul paya.")
                        }
                        return
                    }
                    // Job chains (v2.22, Phase 5, G12): ek naam
                    // ke neeche kai routines. Chalana Auto Mode
                    // ON par hi bina poochhe hota hai, warna
                    // Routines screen khulti hai.
                    if (lower.startsWith("chain banao ")) {
                        input = ""
                        val rest = question.substring(12).trim()
                        val parts = rest.split(":", limit = 2)
                        if (parts.size < 2 || parts[0].isBlank()) {
                            val msg = "Aise boliye: 'chain banao subah: photo lo, yaadein kholo'."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        } else {
                            val chainName = parts[0].trim()
                            val routineNames = parts[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            scope.launch {
                                try {
                                    com.flora.app.data.JobChainStore.upsert(
                                        appContext,
                                        com.flora.app.data.JobChain(chainName, routineNames)
                                    )
                                    val msg = "Chain '$chainName' ban gayi hai (${routineNames.size} routines)."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                } catch (e: Exception) {
                                    val msg = "Chain nahi ban payi, dobara bol dena."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                }
                            }
                        }
                        return
                    }
                    if (lower.startsWith("chain chalao ")) {
                        input = ""
                        val chainName = question.substring(13).trim()
                        scope.launch {
                            try {
                                val chains = com.flora.app.data.JobChainStore.load(appContext)
                                val chain = chains.firstOrNull { it.name.equals(chainName, ignoreCase = true) }
                                if (chain == null) {
                                    val msg = "Chain '$chainName' nahi mili — pehle 'chain banao' se banao."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                } else {
                                    val routines = com.flora.app.data.RoutineStore.load(appContext)
                                    val cmds = chain.routineNames.flatMap { rn ->
                                        routines.firstOrNull { it.name.equals(rn, ignoreCase = true) }?.commands ?: emptyList()
                                    }
                                    if (cmds.isEmpty()) {
                                        val msg = "Chain '$chainName' me koi chalne layak routine nahi mili — routine ke naam sahi hain ya nahi, check karo."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    } else if (autoMode) {
                                        routineQueue = cmds
                                        voice.speak("Chain '$chainName' chala raha hun.")
                                    } else {
                                        screen = Screen.ROUTINES
                                        val msg = "Chain '$chainName' Routines screen par khol raha hun — Auto Mode ON ho to ye khud chalegi."
                                        messages = messages + ChatMsg("Flora: $msg", false)
                                        voice.speak(msg)
                                    }
                                }
                            } catch (e: Exception) {
                                val msg = "Chain nahi chala paya, dobara bol dena."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
                        }
                        return
                    }
                    if (lower == "chain dikhao" || lower == "chains dikhao") {
                        input = ""
                        scope.launch {
                            try {
                                val chains = com.flora.app.data.JobChainStore.load(appContext)
                                val msg = if (chains.isEmpty()) {
                                    "Abhi koi chain nahi bani hai."
                                } else {
                                    "Chains: " + chains.joinToString(", ") { it.name + " (" + it.routineNames.size + ")" }
                                }
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            } catch (e: Exception) {
                                val msg = "Chains nahi dikha paya."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
                        }
                        return
                    }
                    // G16 voice ops (v2.22, Phase 5): Chrome /
                    // GitHub / Vercel kholo — app ho to app, warna
                    // website. Login user khud ek baar karega;
                    // password kabhi bola/store nahi hota.
                    if (lower == "chrome kholo" || lower == "github kholo" || lower == "vercel kholo") {
                        input = ""
                        val appName = when (lower) {
                            "chrome kholo" -> "chrome"
                            "github kholo" -> "github"
                            else -> "vercel"
                        }
                        val fallback = when (lower) {
                            "chrome kholo" -> "https://www.google.com"
                            "github kholo" -> "https://github.com"
                            else -> "https://vercel.com"
                        }
                        runGuardedVoice { scope.launch {
                            try {
                                val found = VerifiedAppLauncher.findByName(appContext, appName)
                                val msg = when {
                                    found.size > 1 -> {
                                        val names = found.take(3).joinToString(", ") { it.label }
                                        "Ek se zyada app mili: $names. Poora naam boliye."
                                    }
                                    found.size == 1 -> {
                                        when (VerifiedAppLauncher.openVerified(appContext, found[0])) {
                                            VerifiedAppLauncher.OpenResult.Opened,
                                            VerifiedAppLauncher.OpenResult.AlreadyOpen -> "$appName khol diya."
                                            VerifiedAppLauncher.OpenResult.NoVerifier -> "$appName kholna verify nahi kar paya — Accessibility service ON karna hoga, Settings me."
                                            else -> "$appName nahi khul paya."
                                        }
                                    }
                                    else -> {
                                        when (VerifiedAppLauncher.openWebsiteVerified(appContext, fallback)) {
                                            VerifiedAppLauncher.OpenResult.Opened,
                                            VerifiedAppLauncher.OpenResult.AlreadyOpen -> "$appName khol diya."
                                            VerifiedAppLauncher.OpenResult.FiredUnverified -> "$appName khol raha hun — browser me khul raha hai."
                                            else -> "$appName nahi khul paya."
                                        }
                                    }
                                }
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            } catch (e: Exception) {
                                val msg = "$appName nahi khul paya."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
                        }
                        }
                        return
                    }
                    // Termux bridge (v2.22, Phase 5, G11): har
                    // command se pehle consent dialog — kabhi
                    // silent nahi chalega.
                    if (lower.startsWith("termux me ") && lower.endsWith(" chalao")) {
                        input = ""
                        val cmd = question.substring(9, question.length - 7).trim()
                        if (cmd.isEmpty()) {
                            val msg = "Aise boliye: 'termux me ls chalao'."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                        } else {
                            termuxConfirm = cmd
                            voice.speak("Termux me ye command chalau? Confirm karne ke liye dialog me Haan dabao.")
                        }
                        return
                    }
                    // WhatsApp read (v2.22, Phase 5): notification
                    // list se aakhri WhatsApp messages sunao.
                    // Bhejna wa.me se hi hota hai (user Send dabata
                    // hai) — yahan sirf padhna hai.
                    if (lower == "whatsapp padho") {
                        input = ""
                        scope.launch {
                            try {
                                val all = com.flora.app.data.NotificationStore.load(appContext)
                                val wa = all.filter { it.pkg == "com.whatsapp" }.takeLast(3)
                                if (wa.isEmpty()) {
                                    val msg = "WhatsApp ki koi notification nahi mili — notification permission ON honi chahiye."
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg)
                                } else {
                                    val text = wa.joinToString(" | ") { it.title + ": " + it.text }
                                    messages = messages + ChatMsg("Flora: $text", false)
                                    voice.speak(text.take(600))
                                }
                            } catch (e: Exception) {
                                val msg = "WhatsApp messages nahi padh paya."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
                        }
                        return
                    }
                    // Website publish, chat se (v2.22, Phase 5,
                    // G15 code loop poora): code-likho + website
                    // starter + publish — teeno chat se chalte hain.
                    if (lower == "website publish karo") {
                        input = ""
                        voice.speak("Pichhli website ko public link par daal rahi hun.")
                        scope.launch {
                            try {
                                val file = java.io.File(appContext.filesDir, "last-website.html")
                                if (!file.exists()) {
                                    voice.speak("Pehle website banao — 'website banao' bol kar ek website banwao, phir main use publish kar dungi.")
                                    return@launch
                                }
                                val html = file.readText()
                                val anonKey = try { prefs.supabaseAnonKey() } catch (e: Exception) { "" }
                                val uid = try { prefs.userId() } catch (e: Exception) { "" }
                                if (anonKey.isBlank() || uid.isBlank()) {
                                    voice.speak("Publish ke liye app ki setting me website wali key chahiye — Settings me daal kar dobara bolo.")
                                    return@launch
                                }
                                val slug = "site-" + uid.take(8)
                                val result = WebsitePublisher.publish(html, slug, anonKey)
                                if (result.isSuccess) {
                                    val url = result.getOrNull() ?: ""
                                    messages = messages + ChatMsg("Flora: Website publish ho gayi — $url", false)
                                    voice.speak("Website publish ho gayi hai, link chat me de diya hai.")
                                } else {
                                    voice.speak("Publish nahi ho paya. ${result.exceptionOrNull()?.message ?: "dobara koshish karo."}")
                                }
                            } catch (e: Exception) {
                                voice.speak("Publish me dikkat aayi, dobara koshish karo.")
                            }
                        }
                        return
                    }
                    // Live update, chat se (v2.22, Phase 6 fix):
                    // ye branch sirf routine executor me thi —
                    // chat/voice se "live update batao" bolne par
                    // brain fallback par ja raha tha, taaza
                    // mausam+khabar wala raasta nahi mil raha tha.
                    // Ab dono raaste ek hi kaam karte hain.
                    if (lower == "live update batao" || lower == "taaza khabar batao" ||
                        lower == "realtime update batao" || lower == "aaj ki khabar batao"
                    ) {
                        input = ""
                        voice.speak("Live update nikal rahi hun — mausam aur taaza khabar dekh rahi hun.")
                        scope.launch {
                            try {
                                val weather = try { searchTools.weatherHere(appContext) } catch (e: Exception) { "" }
                                val news = try { searchTools.search("taaza khabar India today") } catch (e: Exception) { "" }
                                val msg = listOf(weather, news).filter { it.isNotBlank() }.joinToString(" • ")
                                if (msg.isBlank()) {
                                    val fail = "Is waqt live update nahi nikal payi, internet check kar ke dobara bolo."
                                    messages = messages + ChatMsg("Flora: $fail", false)
                                    voice.speak(fail)
                                } else {
                                    messages = messages + ChatMsg("Flora: $msg", false)
                                    voice.speak(msg.take(600))
                                }
                            } catch (e: Exception) {
                                val fail = "Live update me dikkat aayi, dobara koshish karo."
                                messages = messages + ChatMsg("Flora: $fail", false)
                                voice.speak(fail)
                            }
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
                        // Phase 2B long memory: brain history persisted store
                        // se — in-session + restart se pehle ki baat-cheet
                        // dono (cap BrainClient.capHistory me: 12 turns /
                        // ~1500 chars). Current sawaal history me nahi (wo
                        // prompt hai); agar persist effect use pehle hi
                        // save kar chuka ho to aakhri turn hata do.
                        val storedTurns = try {
                            chatMemoryStore.loadAll()
                        } catch (e: Exception) {
                            emptyList()
                        }
                        val historyBase = storedTurns.map { BrainClient.ChatTurn(it.fromUser, it.text) }
                        val history = if (historyBase.lastOrNull()?.let { it.fromUser && it.text == question } == true) {
                            historyBase.dropLast(1)
                        } else {
                            historyBase
                        }
                        val reply = brain.ask(personaPrefix(prefs.persona()) + question, memCtx, history)
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
            } else if (screen == Screen.CAMERA_LOOK) {
                CameraLookScreen(voice, prefs)
            } else if (screen == Screen.SOS) {
                SosScreen(sosStore)
            } else if (screen == Screen.FACE) {
                FaceScreen()
            } else if (screen == Screen.DOCS) {
                DocsScreen(docsStore)
            } else if (screen == Screen.WHITEBOARD) {
                WhiteboardScreen()
            } else if (screen == Screen.HOME) {
                HomeScreen(prefs)
            } else if (screen == Screen.AVATAR) {
                AvatarScreen(prefs) { avatarStyle = it }
            } else if (screen == Screen.CONNECT) {
                ConnectScreen(voice, prefs)
            } else if (screen == Screen.PRIVACY) {
                PrivacyScreen()
            } else if (screen == Screen.TERMS) {
                TermsScreen()
            } else if (screen == Screen.AUTO) {
                AutoScreen(voice)
            } else if (screen == Screen.ROUTINES) {
                RoutinesScreen(voice, onRunMacro = { stored ->
                    screen = Screen.CHAT
                    val macro = com.flora.app.automation.MacroJson.parse(stored.json)
                    if (macro == null) {
                        voice.speak("Macro '${stored.name}' ka format kharab hai, dobara save karo.")
                    } else {
                        messages = messages + ChatMsg("Flora: Macro '${macro.name}' chala rahi hun, har step bol kar bataungi.", false)
                        voice.speak("Macro ${macro.name} chala rahi hun.")
                        macroPending = macro to 0
                        runMacroFrom(macro, 0)
                    }
                }) { routine ->
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
                LiveScreen(
                    voice, prefs,
                    screenGranted = screenGranted,
                    screenResultCode = screenResultCode,
                    screenResultData = screenResultData,
                    onScreenResult = { code, data ->
                        com.flora.app.automation.ScreenCapture.LiveSessionHolder.clear()
                        screenGranted = true
                        screenResultCode = code
                        screenResultData = data
                    },
                    onScreenReset = {
                        screenGranted = false
                        screenResultData = null
                    },
                    screenLauncher = screenLauncher,
                )
            } else if (screen == Screen.DRIVING) {
                DrivingScreen(voice, prefs, onMicRequest)
            } else if (screen == Screen.JOURNAL) {
                JournalScreen(journalStore)
            } else if (screen == Screen.BROWSER) {
                BrowserScreen(voice)
            } else if (screen == Screen.LOCK) {
                LockScreen(pinVault, onUnlock = { screen = Screen.CHAT }, onBack = { screen = Screen.CHAT })
            } else if (screen == Screen.PC_LINK) {
                PcLinkScreen(prefs)
            } else {
                ToolsScreen(searchTools)
            }

            // Phase 6: macro sensitive-step confirm — Haan par wahi
            // step se resume, Ruko par macro ruk jayegi.
            if (macroNeedsConfirm) {
                val pending = macroPending
                AlertDialog(
                    onDismissRequest = { macroNeedsConfirm = false },
                    title = { Text("Macro ka sensitive step?") },
                    text = { Text("Macro '${pending?.first?.name}' ka agla step sensitive hai — Haan dabao to wahi step se aage badhungi, Ruko par macro ruk jayegi.") },
                    confirmButton = {
                        TextButton(onClick = {
                            macroNeedsConfirm = false
                            val p = macroPending
                            if (p != null) runMacroFrom(p.first, p.second)
                        }) { Text("Haan, aage badho") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            macroNeedsConfirm = false
                            macroPending = null
                            voice.speak("Macro rok diya.")
                        }) { Text("Ruko") }
                    }
                )
            }
            // Phase 6: form submit confirm (password field wale form par).
            if (formSubmitPending) {
                AlertDialog(
                    onDismissRequest = { formSubmitPending = false },
                    title = { Text("Form submit karu?") },
                    text = { Text("Ye form me password field hai — Haan dabao to submit karungi, Ruko par nahi karungi.") },
                    confirmButton = {
                        TextButton(onClick = {
                            formSubmitPending = false
                            scope.launch {
                                val service = com.flora.app.automation.FloraAutomationService.instance
                                if (service == null) {
                                    voice.speak("Submit karne ke liye Accessibility service ON karni hogi, Settings me.")
                                } else {
                                    val ok = com.flora.app.automation.FormFiller.clickSubmit(service)
                                    voice.speak(if (ok) "Form submit kar diya hai." else "Submit button nahi mila.")
                                }
                            }
                        }) { Text("Haan, submit karo") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            formSubmitPending = false
                            voice.speak("Form submit nahi kiya.")
                        }) { Text("Ruko") }
                    }
                )
            }
            // Phase 6: auto-reply confirm — Haan par hi bhejne ka
            // raasta chalega; WhatsApp par aakhri Send user khud
            // dabata hai (wa.me prefilled). Baaki apps me sirf draft.
            replyPending?.let { (pkg, sender, draft) ->
                AlertDialog(
                    onDismissRequest = { replyPending = null },
                    title = { Text("Ye jawab bheju?") },
                    text = { Text("'$sender' ko: $draft") },
                    confirmButton = {
                        TextButton(onClick = {
                            replyPending = null
                            scope.launch {
                                if (pkg == "com.whatsapp") {
                                    when (val res = com.flora.app.automation.ContactResolver.resolve(appContext, prefs, sender)) {
                                        is com.flora.app.automation.ContactResolver.Result.Resolved -> {
                                            runContactAction(res.number, "whatsapp", draft)
                                        }
                                        is com.flora.app.automation.ContactResolver.Result.Ambiguous -> {
                                            voice.speak("'$sender' ke liye ek se zyada contact mile — naam poora bolo, tabhi bhej paungi.")
                                        }
                                        else -> {
                                            voice.speak("Number nahi mila — draft chat me hai, tum khud bhej do.")
                                        }
                                    }
                                } else {
                                    voice.speak("Is app me Flora direct reply nahi bhej sakti — draft chat me hai, app khol kar bhej do.")
                                }
                            }
                        }) { Text("Haan, bhejo") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            replyPending = null
                            voice.speak("Jawab nahi bheja.")
                        }) { Text("Ruko") }
                    }
                )
            }
            voiceCallConfirm?.let { number ->
                AlertDialog(
                    onDismissRequest = { voiceCallConfirm = null },
                    title = { Text("Call lagau?") },
                    text = { Text("$number par seedha call lagega.") },
                    confirmButton = {
                        TextButton(onClick = {
                            voiceCallConfirm = null
                            runGuardedVoice {
                            val ok = AutoActions.placeCall(appContext, number)
                            val msg = if (ok) "Call laga di gayi: $number" else "Call nahi lagi, permission ya number check kijiye."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                            }
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
                            runGuardedVoice {
                            val ok = AutoActions.sendSms(appContext, number, text)
                            val msg = if (ok) "SMS bhej diya gaya: $number" else "SMS nahi gaya, permission ya number check kijiye."
                            messages = messages + ChatMsg("Flora: $msg", false)
                            voice.speak(msg)
                            }
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
                            runGuardedVoice { scope.launch {
                                val msg = when (VerifiedAppLauncher.openVerified(appContext, app)) {
                                    VerifiedAppLauncher.OpenResult.Opened -> "${app.label} khol diya."
                                    VerifiedAppLauncher.OpenResult.AlreadyOpen -> "${app.label} pehle se khula hua hai."
                                    VerifiedAppLauncher.OpenResult.NoVerifier -> "${app.label} kholna verify nahi kar paya — Accessibility service ON karna hoga, Settings me."
                                    else -> "${app.label} nahi khul paya."
                                }
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
                            }
                        }) { Text("Haan, kholo") }
                    },
                    dismissButton = {
                        TextButton(onClick = { voiceAppConfirm = null }) { Text("Ruko") }
                    }
                )
            }
            termuxConfirm?.let { cmd ->
                AlertDialog(
                    onDismissRequest = { termuxConfirm = null },
                    title = { Text("Termux me chalau?") },
                    text = {
                        Text(
                            "Ye command Termux me chalegi:\n$cmd\n\n" +
                                "Termux app khuli honi chahiye aur usme Flora ko " +
                                "permission deni hogi. Galat command phone ko " +
                                "nuksaan pahuncha sakti hai — sirf wahi chalao jo tum samajhte ho."
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            termuxConfirm = null
                            try {
                                val intent = android.content.Intent("com.termux.RUN_COMMAND").apply {
                                    setClassName("com.termux", "com.termux.app.RunCommandService")
                                    putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/sh")
                                    putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", cmd))
                                    putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
                                }
                                appContext.startService(intent)
                                val msg = "Termux me command bhej di gayi hai."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            } catch (e: Exception) {
                                val msg = "Termux me command nahi bheji ja saki — Termux app install hai ya nahi, check karo."
                                messages = messages + ChatMsg("Flora: $msg", false)
                                voice.speak(msg)
                            }
                        }) { Text("Haan, chalao") }
                    },
                    dismissButton = {
                        TextButton(onClick = { termuxConfirm = null }) { Text("Ruko") }
                    },
                )
            }
            guardianPinPending?.let { pending ->
                AlertDialog(
                    onDismissRequest = { guardianPinPending = null; guardianPinInput = "" },
                    title = { Text("Guardian PIN verify karo") },
                    text = {
                        Column {
                            Text("Guardian mode ON hai — ye kaam PIN verify ke baad hi hoga. PIN sahi ho to kaam turant chalega.")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = guardianPinInput, onValueChange = { guardianPinInput = it },
                                label = { Text("PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (guardianPinNote.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(guardianPinNote)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (pinVault.verify(guardianPinInput)) {
                                guardian.markPinVerified()
                                guardianPinPending = null
                                guardianPinInput = ""
                                guardianPinNote = ""
                                scope.launch { pending() }
                            } else {
                                guardianPinNote = "Galat PIN."
                                guardianPinInput = ""
                            }
                        }) { Text("Verify karo") }
                    },
                    dismissButton = {
                        TextButton(onClick = { guardianPinPending = null; guardianPinInput = "" }) { Text("Ruko") }
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
    // Voice sample state (v2.22, user selection): persona ke saath,
    // launcher se pehle. Default "lily" (Sample 2 fixed).
    var voiceSample by remember { mutableStateOf("lily") }
    // Theme state (v1.9): persona ke saath, launcher se pehle.
    var theme by remember { mutableStateOf("light") }
    // Shizuku state (v2.22, G19): launcher se PEHLE declare (order trap).
    var shizukuStatus by remember { mutableStateOf("Shizuku: jaanch ho rahi hai...") }
    var shizukuReady by remember { mutableStateOf(false) }
    var shizukuGranted by remember { mutableStateOf(false) }
    // FCM status (v2.2): token value KABHI screen par nahi dikhegi.
    var fcmStatus by remember { mutableStateOf("Push: jaanch ho rahi hai...") }
    // Wake Word state (v2.4): launcher se PEHLE declare (order trap).
    var wakeEnabled by remember { mutableStateOf(false) }
    var wakeEnrolled by remember { mutableStateOf(false) }
    var wakeStatus by remember { mutableStateOf("Wake Word: band hai") }
    var wakeThreshold by remember { mutableStateOf(0.60f) }
    // Voice-match threshold (v2.22, Phase 3): PIN unlock par
    // awaaz check kitna sakht ho. Default 0.50 starting guess
    // hai (same-speaker min 0.558, noise max 0.380) — phone
    // par validate hone ke baad hi lock hoga (Phase 8).
    var matchThreshold by remember { mutableStateOf(0.50f) }
    // App-link state (v2.8, PREP — push v2.7 PASS ke baad): launcher
    // se PEHLE declare (order trap). Password state screen par hi
    // rahega, kabhi save/log nahi hoga.
    var linkEmail by remember { mutableStateOf("") }
    var linkPassword by remember { mutableStateOf("") }
    var linkedEmailDisplay by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    // Guardian PIN re-ask (Guardian enforcement build, 2026-10-29):
    // Guardian ON ho aur is session me PIN verify na hua ho, tab
    // sensitive Settings kaam turant nahi chalega — PIN dialog khulega;
    // sahi PIN par pending kaam ek baar chalega, galat/cancel par kuch
    // nahi badlega. Guardian OFF ho ya session verified ho to kaam
    // seedha chalega (koi extra step nahi). Ek waqt me ek hi pending
    // kaam rehta hai (fail-safe: bina PIN ke kuch nahi chalta).
    var guardianPinInput by remember { mutableStateOf("") }
    var guardianPinNote by remember { mutableStateOf("") }
    var guardianPinPending by remember { mutableStateOf<(suspend () -> Unit)?>(null) }
    fun runGuardedSettings(action: suspend () -> Unit) {
        scope.launch {
            val gOn = try { prefs.guardianMode() } catch (e: Exception) { false }
            val pOn = try { prefs.pinEnabled() } catch (e: Exception) { false }
            if (guardian.canAccessSensitive(pOn, gOn)) {
                action()
            } else {
                guardianPinPending = action
                guardianPinInput = ""
                guardianPinNote = ""
                note = "Guardian mode ON hai — pehle PIN verify karo, phir ye kaam hoga."
            }
        }
    }

    // Keys-check + background status (v2.12): launcher se PEHLE.
    var keyCheckNote by remember { mutableStateOf("") }
    var bgNote by remember { mutableStateOf("") }
    var notifGranted by remember { mutableStateOf(false) }
    var batteryIgnored by remember { mutableStateOf(false) }
    // Resume par status auto-refresh (v2.23, Phase 5): settings se
    // wapas aate hi battery/notification status khud update ho —
    // manual "Status refresh karo" dabana zaroori nahi (button phir
    // bhi hai). Ye effect states ke BAAD hai (order trap se bacha).
    val settingsLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(settingsLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
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
        }
        settingsLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { settingsLifecycleOwner.lifecycle.removeObserver(observer) }
    }
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
    // Google sign-in launcher (v2.22, Phase 5, G14): sirf
    // id_token lene ke liye; token Supabase ko jata hai,
    // password kabhi nahi. Client ID khali ho to button
    // dikhta hi nahi (neeche gated hai).
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getSignedInAccountFromIntent(result.data)
                val account = task.getResult(
                    com.google.android.gms.common.api.ApiException::class.java
                )
                val idToken = account?.idToken
                if (idToken.isNullOrEmpty()) {
                    note = "Google se token nahi mila."
                } else {
                    scope.launch {
                        val res = licenceClient.linkWithGoogleIdToken(idToken)
                        if (res.error.isNotEmpty()) {
                            note = "Google link fail: ${res.error}"
                        } else {
                            prefs.setLink(res.uid, account?.email ?: "", res.refreshToken)
                            linkedEmailDisplay = account?.email ?: ""
                            note = "Google se account link ho gaya."
                        }
                    }
                }
            } catch (e: Exception) {
                note = "Google sign-in nahi ho paya."
            }
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
        voiceSample = prefs.voiceSample()
        theme = prefs.theme()
        // Shizuku status (v2.22, G19): system se live padha jata hai.
        shizukuStatus = ShizukuBridge.statusText()
        shizukuReady = ShizukuBridge.isReady()
        shizukuGranted = ShizukuBridge.hasPermission()
        fcmStatus = if (prefs.fcmToken().isNotEmpty()) {
            "Push: chalu (token sirf is phone par save hai)"
        } else {
            "Push: token abhi nahi mila — app dobara kholo ya internet check karo"
        }
        wakeEnabled = prefs.wakeEnabled()
        wakeEnrolled = BiometricTemplates(context).hasWake()
        wakeStatus = if (wakeEnabled) "Wake Word ON hai — sun raha hun." else "Wake Word: band hai"
        wakeThreshold = prefs.wakeThreshold()
        matchThreshold = prefs.voiceMatchThreshold()
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
            Text("Awaaz pehchan (PIN unlock) — galat awaaz se khul jaye to 'Sakht' karo, apni awaaz na pehchane to 'Dheela' karo.")
            Row {
                listOf(0.60f to "Sakht", 0.50f to "Medium", 0.40f to "Dheela").forEach { (value, label) ->
                    Button(onClick = {
                        matchThreshold = value
                        scope.launch { prefs.setVoiceMatchThreshold(value) }
                    }) { Text(if (matchThreshold == value) "✓ $label" else label) }
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
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://aistudio.google.com/apikey")
                }
                context.startActivity(intent)
            }) { Text("Gemini key kahan se milegi? — AI Studio kholo (free)") }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = groqKey, onValueChange = { groqKey = it },
                label = { Text("Groq API key (backup brain, free tier)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://console.groq.com/keys")
                }
                context.startActivity(intent)
            }) { Text("Groq key kahan se milegi? — Groq console kholo (free)") }
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
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://console.deepgram.com/")
                }
                context.startActivity(intent)
            }) { Text("Deepgram key kahan se milegi? — Deepgram console kholo (signup ke baad API Keys page)") }
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
                    runGuardedSettings {
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
            Row {
                Button(onClick = {
                    scope.launch {
                        keyCheckNote = "Bhare hue keys check ho rahi hain..."
                        val results = try {
                            brain.checkKeysFor(
                                geminiKey.trim(), geminiKey2.trim(),
                                groqKey.trim(), groqKey2.trim(), deepgramKey.trim(),
                            )
                        } catch (e: Exception) {
                            emptyList()
                        }
                        keyCheckNote = if (results.isEmpty()) {
                            "Check nahi ho paya — internet dekh kar dobara karo."
                        } else {
                            results.joinToString(" • ") { "${it.name}: ${it.note}" }
                        }
                    }
                }) { Text("Bhare hue keys check karo (bina save)") }
            }
            Text("Save tumhare haath me hai — sahi ho ya galat, Save dabane par wahi save hogi. 'Keys check karo' saved keys ko check karta hai; 'Bhare hue keys check karo' bina save ke field wali keys ko check karta hai. Check sirf validity batata hai, key ki value kabhi screen par nahi aati. Keys sirf is phone par save hoti hain — har user ki apni key, usi user ke liye istemal hoti hai (koi shared key nahi).")
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
                    runGuardedSettings {
                    scope.launch {
                        prefs.clearLink()
                        linkedEmailDisplay = ""
                        note = "Website account unlink ho gaya — ab app wapas random-UUID mode me hai."
                    }
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
                    runGuardedSettings {
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
                    }
                }) { Text("Website account link karo") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Google link (v2.22, Phase 5, G14): Web OAuth
            // client ID dashboard par banana baaki hai — tab
            // tak button nahi, sirf setup guidance dikhegi.
            if (com.flora.app.core.FloraConfig.GOOGLE_WEB_CLIENT_ID.isEmpty()) {
                Text("Google se link karna abhi setup me hai — Web OAuth client ID dashboard par banana baaki hai, tab tak email + password wala link use karo.")
            } else {
                Button(onClick = {
                    try {
                        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                        ).requestIdToken(com.flora.app.core.FloraConfig.GOOGLE_WEB_CLIENT_ID)
                            .requestEmail()
                            .build()
                        val client = com.google.android.gms.auth.api.signin.GoogleSignIn
                            .getClient(context, gso)
                        googleLauncher.launch(client.signInIntent)
                    } catch (e: Exception) {
                        note = "Google sign-in shuru nahi ho paya."
                    }
                }) { Text("Google se link karo") }
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
            Text("Flora ki awaaz — Sample 2 Lily fixed hai (gahri, dheemi, mellow)")
            Row {
                listOf("isabella" to "Sample 1 Isabella", "lily" to "Sample 2 Lily", "myrtle" to "Sample 3 Myrtle").forEach { (id, label) ->
                    Button(onClick = {
                        voiceSample = id
                        voice.voiceSample = id
                        scope.launch { prefs.setVoiceSample(id) }
                    }) { Text(if (voiceSample == id) "✓ $label" else label) }
                    Spacer(modifier = Modifier.padding(2.dp))
                }
            }
            Text("Ye setting Flora ki har awaaz par lagti hai — chat, background, Auto Mode, sab jagah — kyunki sab ek hi VoiceEngine se bolte hain. System TTS par ye pitch/rate profile lagti hai; Deepgram aane par isi style ki voice dhoondhi jayegi.")
            Spacer(modifier = Modifier.height(12.dp))
            Text("Roles — ek baar tumhare tap se (koi app ye khud nahi kar sakti)")
            Row {
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }) { Text("Default assistant banao") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }) { Text("Default dialer banao") }
            }
            Text("Default assistant: system settings me Flora chuno — phir Flora har jagah se bulayi ja sakegi. Default dialer: default apps me Flora chuno — phir call UI Flora ka chalega.")
            Spacer(modifier = Modifier.height(12.dp))
            Text("Shizuku — system permission ek tap me (bina root ke, tumhare control me)")
            Text(shizukuStatus)
            Row {
                Button(onClick = {
                    ShizukuBridge.requestPermission(1001)
                    shizukuStatus = ShizukuBridge.statusText()
                    shizukuReady = ShizukuBridge.isReady()
                    shizukuGranted = ShizukuBridge.hasPermission()
                }) { Text("Shizuku permission do") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = {
                    shizukuStatus = ShizukuBridge.statusText()
                    shizukuReady = ShizukuBridge.isReady()
                    shizukuGranted = ShizukuBridge.hasPermission()
                }) { Text("Status refresh karo") }
            }
            if (shizukuReady && shizukuGranted) {
                Text("Taiyaar hai — neeche wali permission ek tap me mil jayegi.")
                Row {
                    Button(onClick = {
                        val ok = ShizukuBridge.grantPermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
                        shizukuStatus = if (ok) "Notification permission mil gaya." else "Notification permission nahi mila — dobara koshish karo."
                    }) { Text("Notification do") }
                    Spacer(modifier = Modifier.padding(4.dp))
                    Button(onClick = {
                        val ok = ShizukuBridge.grantPermission(context.packageName, Manifest.permission.CAMERA)
                        shizukuStatus = if (ok) "Camera permission mil gaya." else "Camera permission nahi mila — dobara koshish karo."
                    }) { Text("Camera do") }
                    Spacer(modifier = Modifier.padding(4.dp))
                    Button(onClick = {
                        val ok = ShizukuBridge.grantPermission(context.packageName, Manifest.permission.RECORD_AUDIO)
                        shizukuStatus = if (ok) "Mic permission mil gaya." else "Mic permission nahi mila — dobara koshish karo."
                    }) { Text("Mic do") }
                }
            }
            Row {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }) { Text("Shizuku app kholo") }
            }
            Text("Setup: Shizuku app install karo, usme wireless debugging se chalao (Shizuku app khud steps batata hai). Reboot par Shizuku band ho jata hai — dobara chalana padta hai. Tum jab chaho Shizuku band kar sakte ho, tab Flora wapas normal settings wale raaste par aa jayegi.")
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
                runGuardedSettings {
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
                }
            }) { Text("PIN set karo") }
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Text("Guardian mode (ON ho to sensitive kaam par PIN dobara maanga jayega)")
                Spacer(modifier = Modifier.padding(4.dp))
                Switch(checked = guardianMode, onCheckedChange = { value ->
                    if (value) {
                        scope.launch {
                            if (!prefs.pinEnabled()) {
                                note = "Guardian mode ke liye pehle PIN set karo."
                            } else {
                                guardianMode = true
                                prefs.setGuardianMode(true)
                                note = "Guardian mode ON ho gaya."
                            }
                        }
                    } else {
                        runGuardedSettings {
                            scope.launch {
                                guardianMode = false
                                prefs.setGuardianMode(false)
                                note = "Guardian mode OFF ho gaya."
                            }
                        }
                    }
                })
            }
            Text("Guardian ON ho to ye kaam PIN verify ke baad hi honge: Keys save, PIN change, Guardian/Face Lock band karna, website link/unlink, VPA/UTR save, aur Auto Mode ke call/SMS/app-kholo. Ek session me ek baar PIN verify ho jaye to baaki kaam seedha chalenge. Chat lock ka PIN + awaaz check alag se chalta hai.")
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
                    if (value) {
                        scope.launch {
                            if (!prefs.pinEnabled()) {
                                note = "Face Lock ke liye pehle PIN set karo."
                            } else {
                                faceLock = true
                                prefs.setFaceLock(true)
                                note = "Face Lock ON ho gaya."
                            }
                        }
                    } else {
                        runGuardedSettings {
                            scope.launch {
                                faceLock = false
                                prefs.setFaceLock(false)
                                note = "Face Lock OFF ho gaya."
                            }
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
                runGuardedSettings {
                scope.launch {
                    prefs.setAdminUpiVpa(adminVpa.trim())
                    note = "Admin UPI VPA save ho gaya."
                }
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
                runGuardedSettings {
                scope.launch {
                    prefs.setLastUtr(utr.trim())
                    note = "UTR save ho gaya (local note). Admin verify karke hi licence active karega — server verification alag step hai."
                }
                }
            }) { Text("UTR save karo") }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Data retention: Journal 30 din baad auto-delete; Memories me date wali entries 30 din baad auto-delete, bina date wali manually. Licence/payment record alag se rakha jata hai.")

            guardianPinPending?.let { pending ->
                AlertDialog(
                    onDismissRequest = { guardianPinPending = null; guardianPinInput = "" },
                    title = { Text("Guardian PIN verify karo") },
                    text = {
                        Column {
                            Text("Guardian mode ON hai — ye sensitive kaam PIN verify ke baad hi hoga. PIN sahi ho to kaam turant chalega.")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = guardianPinInput, onValueChange = { guardianPinInput = it },
                                label = { Text("PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (guardianPinNote.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(guardianPinNote)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (pinVault.verify(guardianPinInput)) {
                                guardian.markPinVerified()
                                guardianPinPending = null
                                guardianPinInput = ""
                                guardianPinNote = ""
                                scope.launch { pending() }
                            } else {
                                guardianPinNote = "Galat PIN."
                                guardianPinInput = ""
                            }
                        }) { Text("Verify karo") }
                    },
                    dismissButton = {
                        TextButton(onClick = { guardianPinPending = null; guardianPinInput = "" }) { Text("Ruko") }
                    }
                )
            }
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
        }
    }
}
