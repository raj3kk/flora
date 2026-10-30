package com.flora.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.flora.app.data.AutoLogStore
import com.flora.app.data.ChatMemoryStore
import com.flora.app.data.FloraPreferences
import com.flora.app.data.MemoriesStore
import com.flora.app.automation.AppAutomation
import com.flora.app.automation.VerifiedAppLauncher
import com.flora.app.planner.LearnedEngine
import com.flora.app.planner.LearnedPattern
import com.flora.app.tools.MusicControl
import com.flora.app.tools.SdResult
import kotlinx.coroutines.launch

/**
 * SD-P1 Batch 4 (SD-8 app batch, Lane A) — settings screen.
 *
 * Is screen par jo hai (har sub-item apne gate ke saath):
 * - 8.3 Languages: display-only. Baat-cheet ki bhasha Hindi hai
 *   (speech recognition + Deepgram Hindi, pehle se fixed) — baaki
 *   bhashayen DISABLED + imaandaar note ke saath hain, kyunki
 *   unka G2P/voice support records me UNVERIFIED hai. Koi bhasha
 *   yahan se badli nahi jati, isliye galat daava bhi nahi hai.
 * - 8.4 Naam/gender: device-only save (server par kabhi nahi,
 *   jab tak user opt-in na de — opt-in is batch me nahi hai).
 * - 8.5 Default role: display-only. Current persona + item 34
 *   ka default (Romantic, naye install par) dikhta hai — ye UI
 *   item 34 ke default ko kabhi override nahi karta; apna
 *   current role badalna Settings ke Persona section se hoga.
 * - 8.6 Incognito: ON ho to us session ke turns chat memory /
 *   history me save nahi honge (FloraApp save-effect me gate);
 *   safety stops Incognito me bhi lagu rahenge.
 * - 8.7 Auto-save rules: text + user-clear buttons (chat
 *   history / memories saaf karo — user ka tap hi consent hai).
 * - 8.12 Email/social consent: default OFF switches; bina
 *   consent ke Flora ki taraf se koi email/social action nahi
 *   hoga; consent badalne par Auto Log me entry jati hai.
 * - 8.14 Skills/thinking: skills toggle PlannerEngine me
 *   honored hai (OFF = learned shortcut skip, AI raasta);
 *   thinking toggle is screen par learned-pattern details
 *   dikhata/chhupata hai. Skill engine = learned patterns —
 *   LLM retraining ka daava kabhi nahi.
 *
 * - 8.8 Music-app control: MediaSession pehle (MusicControl
 *   helper), Spotify app-open fallback. Notification access
 *   OFF ho to imaandaar message, chhupke se fail nahi.
 *
 * Is screen par NAHI hai (held, wajah ke saath):
 * - 8.9 Marketing, 8.13 Touch Guard — SPEC-FIRST: records me
 *   inka vyavhaar sirf "PENDING" likha hai, detail nahi; spec
 *   freeze ke bina guess par build nahi hoga.
 * - 8.10 Whiteboard-full — SPEC-FIRST: WhiteboardScreen (canvas
 *   + voice) pehle se hai; "full" ka poora vyavhaar freeze
 *   hona baaki hai.
 * - 8.11 Drawer/widgets — drawer refactor + AppWidget receiver
 *   alag gate ke saath agle batch me.
 */
@Composable
fun Sd8SettingsScreen(autoLogStore: AutoLogStore, onBack: () -> Unit) {
    val appContext = LocalContext.current
    val prefs = remember { FloraPreferences(appContext) }
    val chatMemoryStore = remember { ChatMemoryStore(appContext) }
    val memoriesStore = remember { MemoriesStore(appContext) }
    val learnedEngine = remember { LearnedEngine(appContext) }
    val scope = rememberCoroutineScope()

    var note by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var userGender by remember { mutableStateOf("") }
    var persona by remember { mutableStateOf("") }
    var incognito by remember { mutableStateOf(false) }
    var skillsEnabled by remember { mutableStateOf(true) }
    var thinkingVisible by remember { mutableStateOf(true) }
    var emailConsent by remember { mutableStateOf(false) }
    var socialConsent by remember { mutableStateOf(false) }
    var patterns by remember { mutableStateOf(listOf<LearnedPattern>()) }
    var clearChatArmed by remember { mutableStateOf(false) }
    var clearMemArmed by remember { mutableStateOf(false) }
    var musicStatus by remember { mutableStateOf("Music status abhi check nahi hua.") }
    var musicPlaying by remember { mutableStateOf(false) }

    fun log(msg: String) {
        scope.launch {
            try {
                autoLogStore.add(msg)
            } catch (e: Exception) {
            }
        }
    }

    fun refresh() {
        scope.launch {
            try {
                userName = prefs.sd8UserName()
                userGender = prefs.sd8UserGender()
                persona = prefs.persona()
                incognito = prefs.sd8Incognito()
                skillsEnabled = prefs.sd8SkillsEnabled()
                thinkingVisible = prefs.sd8ThinkingVisible()
                emailConsent = prefs.sd8EmailConsent()
                socialConsent = prefs.sd8SocialConsent()
                patterns = learnedEngine.loadAll()
            } catch (e: Exception) {
                note = "Settings padhne me dikkat aayi — dobara kholo."
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Row {
            TextButton(onClick = onBack) { Text("Wapas") }
        }
        Text("Extra Settings — languages, profile, incognito, save, consent, skills")
        if (note.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(note)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("8.3 Languages")
        Text("Current: Hindi (speech recognition + Deepgram Hindi)")
        Text("Baaki bhashayen abhi disabled hain — unka G2P/voice support records me verified nahi hai, isliye yahan se koi bhasha badli nahi jati.")
        Button(onClick = {}, enabled = false) { Text("English (disabled)") }
        Button(onClick = {}, enabled = false) { Text("Baaki bhashayen (disabled)") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("8.4 Naam / gender (sirf isi phone par)")
        OutlinedTextField(
            value = userName, onValueChange = { userName = it },
            label = { Text("Tumhara naam") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = userGender, onValueChange = { userGender = it },
            label = { Text("Gender (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = {
            scope.launch {
                prefs.setSd8UserName(userName)
                prefs.setSd8UserGender(userGender)
                log("SD-8: profile save hua (naam/gender, device-only)")
                note = "Profile save ho gaya — sirf isi phone par rahega."
            }
        }) { Text("Profile save karo") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("8.5 Default assistant role")
        Text("Tumhara current role: ${if (persona.isBlank()) "flora" else persona}")
        Text("Default (naye install par): Romantic — item 34. Ye screen default ko override nahi karti; apna current role Settings ke Persona section se badlo.")

        Spacer(modifier = Modifier.height(16.dp))
        Text("8.6 Incognito")
        Row {
            Text("Incognito session", modifier = Modifier.weight(1f))
            Switch(checked = incognito, onCheckedChange = { value ->
                scope.launch {
                    prefs.setSd8Incognito(value)
                    incognito = value
                    log(if (value) "SD-8: Incognito ON" else "SD-8: Incognito OFF")
                    note = if (value) {
                        "Incognito ON hai — is session ki baat memory/history me save nahi hogi. Safety stops phir bhi lagu rahenge."
                    } else {
                        "Incognito OFF hai — baat dobara memory me save hogi."
                    }
                }
            })
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("8.7 Auto-save rules + user-clear")
        Text(
            "Rules: chat ke naye turns (greeting/khaali lines ko chhod kar) auto-save hote hain — sirf jab Incognito OFF ho. " +
                "Memories tumhare save karne par banti hain. Neeche ke buttons se tum khud history/memories saaf kar sakte ho.",
        )
        Row {
            Button(onClick = {
                if (!clearChatArmed) {
                    clearChatArmed = true
                    note = "Pakka chat history saaf karni hai? Ye wapas nahi aayegi — confirm karne ke liye dobara dabao."
                } else {
                    clearChatArmed = false
                    scope.launch {
                        try {
                            chatMemoryStore.clearAll()
                            log("SD-8: user ne chat history saaf ki")
                            note = "Chat history saaf kar di gayi."
                        } catch (e: Exception) {
                            note = "Chat history saaf nahi hui — dobara koshish karo."
                        }
                    }
                }
            }) { Text(if (clearChatArmed) "Pakka saaf karo" else "Chat history saaf karo") }
            TextButton(onClick = {
                if (!clearMemArmed) {
                    clearMemArmed = true
                    note = "Pakka memories saaf karni hain? Ye wapas nahi aayengi — confirm karne ke liye dobara dabao."
                } else {
                    clearMemArmed = false
                    scope.launch {
                        try {
                            memoriesStore.clearAll()
                            log("SD-8: user ne memories saaf ki")
                            note = "Memories saaf kar di gayi."
                        } catch (e: Exception) {
                            note = "Memories saaf nahi hui — dobara koshish karo."
                        }
                    }
                }
            }) { Text(if (clearMemArmed) "Pakka saaf karo" else "Memories saaf karo") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("8.12 Email / social consent (default OFF)")
        Text("Bina consent ke Flora tumhari taraf se koi email/social action nahi karegi.")
        Row {
            Text("Email actions ki consent", modifier = Modifier.weight(1f))
            Switch(checked = emailConsent, onCheckedChange = { value ->
                scope.launch {
                    prefs.setSd8EmailConsent(value)
                    emailConsent = value
                    log(if (value) "SD-8: email consent ON" else "SD-8: email consent OFF")
                }
            })
        }
        Row {
            Text("Social actions ki consent", modifier = Modifier.weight(1f))
            Switch(checked = socialConsent, onCheckedChange = { value ->
                scope.launch {
                    prefs.setSd8SocialConsent(value)
                    socialConsent = value
                    log(if (value) "SD-8: social consent ON" else "SD-8: social consent OFF")
                }
            })
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("8.14 Skills / thinking")
        Text("Skill engine = on-device learned patterns. LLM retraining ka daava kabhi nahi.")
        Row {
            Text("Learned skills ON (planner shortcut)", modifier = Modifier.weight(1f))
            Switch(checked = skillsEnabled, onCheckedChange = { value ->
                scope.launch {
                    prefs.setSd8SkillsEnabled(value)
                    skillsEnabled = value
                    log(if (value) "SD-8: skills ON" else "SD-8: skills OFF")
                    note = if (value) {
                        "Skills ON — repeat kaam par learned shortcut chalega."
                    } else {
                        "Skills OFF — planner learned shortcut skip karke AI raaste se plan banayega."
                    }
                }
            })
        }
        Row {
            Text("Thinking (learned patterns) dikhao", modifier = Modifier.weight(1f))
            Switch(checked = thinkingVisible, onCheckedChange = { value ->
                scope.launch {
                    prefs.setSd8ThinkingVisible(value)
                    thinkingVisible = value
                }
            })
        }
        if (thinkingVisible) {
            Text("Learned patterns (${patterns.size}):")
            patterns.forEach { pattern ->
                Text("- ${pattern.normalizedCommand.take(60)} • ${pattern.tool} • ✓${pattern.successCount} ✗${pattern.failCount}")
            }
            if (patterns.isEmpty()) Text("Abhi koi learned pattern nahi hai.")
        } else {
            Text("Thinking details chhupi hui hain.")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("8.8 Music-app control (MediaSession pehle, app-open fallback)")
        Text(musicStatus)
        Row {
            Button(onClick = {
                scope.launch {
                    when (val r = MusicControl.nowPlaying(appContext)) {
                        is SdResult.Ok -> {
                            val np = r.value
                            if (np == null) {
                                musicStatus = "Koi music session active nahi hai — pehle music app me gaana chalao, phir Refresh dabao."
                            } else {
                                musicPlaying = np.playing
                                musicStatus = "${if (np.playing) "Chal raha hai" else "Ruka hua"}: ${np.title.ifBlank { "(bina title)" }}${if (np.artist.isNotBlank()) " — ${np.artist}" else ""} (${np.pkg})"
                            }
                            log("SD-8: music status check hua")
                        }
                        is SdResult.Failure -> musicStatus = r.reason
                    }
                }
            }) { Text("Refresh") }
            TextButton(onClick = {
                scope.launch {
                    val action = if (musicPlaying) MusicControl.pause(appContext) else MusicControl.play(appContext)
                    when (action) {
                        is SdResult.Ok -> {
                            musicPlaying = !musicPlaying
                            log("SD-8: music ${if (musicPlaying) "play" else "pause"}")
                            musicStatus = if (musicPlaying) "Music chal rahi hai." else "Music ruk gayi hai."
                        }
                        is SdResult.Failure -> musicStatus = action.reason
                    }
                }
            }) { Text(if (musicPlaying) "Pause" else "Play") }
            TextButton(onClick = {
                scope.launch {
                    when (val r = MusicControl.previous(appContext)) {
                        is SdResult.Ok -> {
                            log("SD-8: music previous")
                            musicStatus = "Pichhla gaana."
                        }
                        is SdResult.Failure -> musicStatus = r.reason
                    }
                }
            }) { Text("Prev") }
            TextButton(onClick = {
                scope.launch {
                    when (val r = MusicControl.next(appContext)) {
                        is SdResult.Ok -> {
                            log("SD-8: music next")
                            musicStatus = "Agla gaana."
                        }
                        is SdResult.Failure -> musicStatus = r.reason
                    }
                }
            }) { Text("Next") }
        }
        TextButton(onClick = {
            scope.launch {
                if (!AppAutomation.isInstalled(appContext, "com.spotify.music")) {
                    musicStatus = "Spotify installed nahi hai — Play Store se install karke dobara koshish karo."
                } else {
                    val opened = AppAutomation.launch(appContext, "com.spotify.music")
                    musicStatus = if (opened == VerifiedAppLauncher.OpenResult.Opened ||
                        opened == VerifiedAppLauncher.OpenResult.AlreadyOpen
                    ) {
                        "Spotify khol diya hai — session active hote hi upar ke buttons se control karo."
                    } else {
                        "Spotify khul nahi paya — Accessibility service ON honi chahiye."
                    }
                }
            }
        }) { Text("Spotify kholo (fallback)") }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
