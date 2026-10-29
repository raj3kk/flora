package com.flora.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.flora.app.data.AutoLogStore
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
import androidx.core.content.ContextCompat
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.launch

/**
 * Setup wizard (v2.13, Flora ka apna design; Batch 2 F1: 9 steps):
 * app pehli baar khulte hi setup hota hai — Welcome, Permissions,
 * Background, phir 6 dedicated steps (Keys, Website link, Licence,
 * Default assistant, Overlay, Free-mode banner).
 *
 * Har naya step SKIP karne layak hai (defaults maujood hain),
 * par step ka hona zaroori hai. "Section kholo" dabane par user
 * Settings/Licence screen par jata hai aur wahan se "Setup
 * wizard me wapas jao" se isi wizard me wapas aata hai — Settings
 * me atak kar nahi reh jata (FloraApp me wizardBack banner).
 * Settings se dobara kholne ka raasta barkaraar hai (Permissions
 * screen > "Setup wizard dobara dekho" + pehli-baar gate).
 *
 * Step 2 ka behaviour: "Sab allow karo" dabane par Android ke
 * system dialogs ek-ek karke aate hain (ye Android ka apna flow
 * hai — ek dialog ke baad doosra). Har permission ki row par tap
 * karke usko akela bhi maanga ja sakta hai, aur "App settings
 * kholo" se user khud Settings me jaa kar bhi de sakta hai.
 * Microphone bolne ke liye chahiye — par uske bina bhi aage
 * badha ja sakta hai (Cycle 1, APP-C1-001): mic deny karne
 * wala user "Mic ke bina aage badho" se step 3 par ja sakta
 * hai, tab sirf likh kar baat hogi, bol kar nahi. Baaki
 * permissions ke bina sirf unke features band rahenge, app
 * nahi atakegi.
 *
 * Step 3 ke saare kaam user ki marzi se hain — koi bhi step
 * chhoda ja sakta hai, "Ho gaya" dabate hi Flora khul jayegi.
 * Accessibility/dialer role yahan bhi zabardasti nahi — sirf
 * settings kholne ka raasta diya jata hai.
 */
private data class WizardPermission(val permission: String, val label: String, val why: String)

private val WIZARD_PERMISSIONS = listOf(
    WizardPermission(
        Manifest.permission.RECORD_AUDIO,
        "Microphone",
        "Taaki tum Flora se baat kar sako. Bina iske sirf likh kar baat hogi.",
    ),
    WizardPermission(
        Manifest.permission.POST_NOTIFICATIONS,
        "Notifications",
        "Flora ki apni notification, jo use background me chalu rakhti hai.",
    ),
    WizardPermission(
        Manifest.permission.CAMERA,
        "Camera",
        "Taaki wo photo le sake ya jo tum dikhao wo dekh sake.",
    ),
    WizardPermission(
        Manifest.permission.CALL_PHONE,
        "Phone calls",
        "Taaki wo tumhare liye call laga sake — sirf tumhare confirmation ke baad.",
    ),
    WizardPermission(
        Manifest.permission.READ_CONTACTS,
        "Contacts",
        "Taaki sirf naam kaafi ho — number wo khud dhoondh legi.",
    ),
    WizardPermission(
        Manifest.permission.SEND_SMS,
        "SMS",
        "Taaki wo text message bhej sake — sirf tumhare confirmation ke baad.",
    ),
    WizardPermission(
        Manifest.permission.READ_PHONE_STATE,
        "Phone state",
        "Taaki wo bata sake kaun call kar raha hai.",
    ),
    WizardPermission(
        Manifest.permission.ACCESS_FINE_LOCATION,
        "Location",
        "Mausam, raasta, aur yahan ka mausam batane ke liye.",
    ),
)

@Composable
fun SetupWizard(
    onDone: () -> Unit,
    prefs: FloraPreferences? = null,
    onOpenSection: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val wizardScope = rememberCoroutineScope()
    var step by remember { mutableStateOf(1) }
    var grants by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var attempted by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    // Batch 2 (F1) wizard state — launchers/callbacks se pehle.
    var linkedEmail by remember { mutableStateOf("") }
    var personaSel by remember { mutableStateOf("flora") }
    var voiceSel by remember { mutableStateOf("lily") }
    var wizardNote by remember { mutableStateOf("") }
    // Item 34: naye install par hi romantic-default gate-card;
    // dialog state callbacks se PEHLE declare (order trap).
    var ageGatePending by remember { mutableStateOf(false) }
    var ageGateNote by remember { mutableStateOf("") }
    var gateCardDismissed by remember { mutableStateOf(false) }
    var freshInstall by remember { mutableStateOf(false) }
    var ageConfirmedWiz by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        try { linkedEmail = prefs?.linkedEmail() ?: "" } catch (e: Exception) { }
        try { personaSel = prefs?.persona() ?: "flora" } catch (e: Exception) { }
        try { voiceSel = prefs?.voiceSample() ?: "lily" } catch (e: Exception) { }
        try { freshInstall = prefs?.let { !(it.hasStoredPersona()) } ?: false } catch (e: Exception) { }
        try { ageConfirmedWiz = prefs?.ageConfirmed() ?: false } catch (e: Exception) { }
    }
    // Launcher ke callback se PEHLE declare (order trap).
    var lastRequested by remember { mutableStateOf("") }
    val singleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        attempted = true
        grants = grants + (lastRequested to granted)
    }
    val multiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        attempted = true
        grants = grants + result
    }
    // Cycle 1, APP-C1-001: system check HI sach hai. Pehle
    // grants map ka purana denial (false) system check ko
    // daba deta tha — Settings se mic grant karke wapas aane
    // par bhi "Aage badho" stale-disabled rehta tha, har launch
    // par gate atak jata tha. Ab system ki maujooda haalat hi
    // dikhegi; grants map sirf callback record ke liye rehta hai.
    fun isGranted(permission: String): Boolean {
        @Suppress("UNUSED_EXPRESSION")
        refresh
        return ContextCompat.checkSelfPermission(
            context, permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    val micGranted = isGranted(Manifest.permission.RECORD_AUDIO)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Flora set up karo")
            Text("Step $step / 9")
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (step == 1) {
            item {
                Text("Namaste! Main Flora hun.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Main tumhari awaaz se baat kar sakti hun, apps khol " +
                        "sakti hun, yaadein rakh sakti hun, aur 'Hey Flora' " +
                        "bolne par sun sakti hun. Tumhara data isi phone " +
                        "par rehta hai — screen kabhi server par nahi jati, " +
                        "aur koi kaam tumhare confirmation ke bina nahi hota."
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { step = 2 }, modifier = Modifier.fillMaxWidth()) {
                    Text("Shuru karo")
                }
            }
        }
        if (step == 2) {
            item {
                Text("Flora ko apna phone use karne do")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Ek tap — Android ek ek karke sab poochega. " +
                        "Microphone bolne ke liye chahiye; bina mic ke " +
                        "bhi aage badh sakte ho, tab sirf likh kar baat hogi."
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            items(WIZARD_PERMISSIONS.size) { index ->
                val item = WIZARD_PERMISSIONS[index]
                val granted = isGranted(item.permission)
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            lastRequested = item.permission
                            singleLauncher.launch(item.permission)
                        }
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.label)
                        Text(item.why)
                    }
                    Text(if (granted) "Mil gaya ✓" else "Baaki hai")
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                if (!micGranted && attempted) {
                    Text(
                        "Microphone ke bina Flora tumhe sun nahi sakti. " +
                            "Agar Android ne poochna band kar diya hai, to " +
                            "App settings → Permissions → Microphone kholo " +
                            "aur Allow chuno."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = {
                        multiLauncher.launch(
                            WIZARD_PERMISSIONS.map { it.permission }.toTypedArray()
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("✓ Sab allow karo") }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + context.packageName),
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("App settings kholo") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { if (micGranted) step = 3 },
                    enabled = micGranted,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("→ Aage badho") }
                Spacer(modifier = Modifier.height(4.dp))
                if (!micGranted) {
                    Text(
                        "Microphone allow hote hi \"Aage badho\" khul " +
                            "jayega — ya mic ke bina bhi aage badh sakte " +
                            "ho, tab sirf likh kar baat hogi, bol kar nahi. " +
                            "Baad me Settings se mic de sakte ho."
                    )
                }
                // Cycle 1, APP-C1-001: mic deny karne wala user
                // bhi aage badh sake — text-only raasta. Isse gate
                // kabhi mic par atak kar har launch par nahi khulega.
                TextButton(
                    onClick = { step = 3 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Mic ke bina aage badho (sirf likh kar)") }
                Text("Har row par tap karke us permission ko akela bhi maang sakte ho.")
            }
        }
        if (step == 3) {
            item {
                Text("Flora ko background me zinda rakho")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Battery zaroori hai — isi se screen off hone par wo " +
                        "chup nahi hoti. Baaki optional hain par unse wo " +
                        "kahin zyada kaam ki ho jaati hai."
                )
                Spacer(modifier = Modifier.height(12.dp))
                val batteryIgnored = try {
                    context.getSystemService(PowerManager::class.java)
                        ?.isIgnoringBatteryOptimizations(context.packageName) ?: false
                } catch (e: Exception) {
                    false
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            try {
                                if (!batteryIgnored) {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                            Uri.parse("package:" + context.packageName),
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    )
                                } catch (e2: Exception) {
                                }
                            }
                        }
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Battery — optimisation band")
                        Text(
                            "Zaroori hai. Iske bina phone background me Flora " +
                                "ko band kar deta hai aur wo chup ho jaati hai. " +
                                "Tap karo, phir Allow chuno."
                        )
                    }
                    Text(if (batteryIgnored) "✓" else "Baaki hai")
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            try {
                                var opened = false
                                val candidates = listOf(
                                    Intent().setClassName(
                                        "com.vivo.permissionmanager",
                                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
                                    ),
                                    Intent().setClassName(
                                        "com.iqoo.secure",
                                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
                                    ),
                                )
                                for (intent in candidates) {
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
                            } catch (e: Exception) {
                            }
                        }
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-start (Vivo/iQOO)")
                        Text("Taaki reboot ke baad bhi Flora apne aap chalu ho jaye.")
                    }
                    Text("Settings me karo")
                }
                val accessibilityOn = try {
                    val manager = context.getSystemService(
                        AccessibilityManager::class.java
                    )
                    manager?.getEnabledAccessibilityServiceList(
                        android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
                    )?.any { it.id.contains("FloraAutomationService") } ?: false
                } catch (e: Exception) {
                    false
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            try {
                                context.startActivity(
                                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                )
                            } catch (e: Exception) {
                            }
                        }
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Accessibility service")
                        Text(
                            "Taaki wo apps khol sake, tap kar sake aur tumhare " +
                                "liye screen padh sake. List me Flora ON karo — " +
                                "ye tumhari marzi se hi hoga."
                        )
                    }
                    Text(if (accessibilityOn) "ON hai ✓" else "Baaki hai")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { refresh += 1 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Status refresh karo") }
                Spacer(modifier = Modifier.height(8.dp))
                // Batch 2 (F1): step 3 se seedha step 4 (keys) — Ho gaya
                // dabana zaroori nahi, baaki steps skip bhi ho sakte hain.
                Button(
                    onClick = { step = 4 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Agla step: Keys →") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("✓ Ho gaya")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Koi bhi step chhoda ja sakta hai — Ho gaya dabate hi Flora khul jayegi.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Agla step (4) keys ka hai — wahan se Keys section khologe. Har step skip kar sakte ho.")
            }
        }
        // Batch 2 (F1) step 4 — key setup: keys tak le jao, wapas wizard me lao.
        if (step == 4) {
            item {
                Text("Apni keys jodo")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Flora tumhari apni keys se chalti hai — Gemini key " +
                        "(AI Studio se free) sochne ke liye, Deepgram key " +
                        "awaaz sunne ke liye, aur Brave/SerpAPI/Maps keys " +
                        "search ke liye. Keys sirf is phone par rehti hain, " +
                        "kabhi server par nahi jati."
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Settings > Keys me har key ke neeche link se key " +
                        "banao, phir 'Bhare hue keys check karo' se sahi/galat " +
                        "dekho, phir Save dabao. Bina key ke bhi aage badh " +
                        "sakte ho — tab sirf halki local baat hogi."
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onOpenSection("keys") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Keys section kholo (Settings)") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { step = 3 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("← Peeche") }
                TextButton(
                    onClick = { step = 5 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Skip karo → Agla step") }
                Text("Settings se 'Setup wizard me wapas jao' dabate hi yahin wapas aaoge.")
            }
        }
        // Batch 2 (F1) step 5 — website link: status + dobara sync ka raasta.
        if (step == 5) {
            item {
                Text("Website account link karo")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (linkedEmail.isNotEmpty()) "Linked: $linkedEmail — website wala Pro isi app me dikhega."
                    else "Abhi koi website account link nahi hai — website se khareeda Pro isi app me dikhane ke liye link karo."
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Link pending ho to Settings me 'Server sync dobara karo' " +
                        "dabao. Link karne se pehle jaan lo: purani random-UUID " +
                        "wali licence orphan ho jayegi; refresh token sirf is " +
                        "phone par save hoga; password kabhi save nahi hota."
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onOpenSection("link") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Website link section kholo (Settings)") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { step = 4 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("← Peeche") }
                TextButton(
                    onClick = { step = 6 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Skip karo → Agla step") }
            }
        }
        // Batch 2 (F1) step 6 — licence activation: LicenceScreen ka raasta.
        if (step == 6) {
            item {
                Text("Licence / Pro")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Free me roz 10 minute baat + app me maujood saare " +
                        "tools. Pro (₹199/month) me unlimited baat + FCM push. " +
                        "Licence screen par status dekho, refresh karo, ya " +
                        "admin se mili key verify karo."
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onOpenSection("licence") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Licence screen kholo") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { step = 5 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("← Peeche") }
                TextButton(
                    onClick = { step = 7 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Skip karo → Agla step") }
            }
        }
        // Batch 2 (F1) step 7 — default assistant: persona + voice sample, yahin se.
        if (step == 7) {
            item {
                Text("Flora kaisi lagegi?")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Persona (tone) chuno — awaaz Sample 2 Lily default hai (gahri, dheemi, mellow).")
                Spacer(modifier = Modifier.height(8.dp))
                // Item 34 (D-I34-1): NAYE install par hi romantic
                // default ka gate-card — purane installs ko kabhi
                // nag nahi (unka stored persona hi rehta hai).
                // Confirm = romantic default; Nahi = friendly mode.
                if (freshInstall && !ageConfirmedWiz && !gateCardDismissed) {
                    Text("Flora ka default nature Romantic 18+ hai — pyaar bhara, caring saathi ka tone. Flora ek AI hai, koi asli partner nahi. Sirf 18+ ke liye.")
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        Button(
                            onClick = {
                                ageConfirmedWiz = true
                                personaSel = "romantic"
                                wizardScope.launch {
                                    try { prefs?.setAgeConfirmed(true) } catch (e: Exception) { }
                                    try { prefs?.setPersona("romantic") } catch (e: Exception) { }
                                    try { AutoLogStore(context).add("Romantic mode ON (wizard gate) — 18+ confirm hua, naya install default") } catch (e: Exception) { }
                                }
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        ) { Text("Haan, main 18+ hun — Romantic default") }
                        Button(
                            onClick = {
                                gateCardDismissed = true
                                personaSel = "flora"
                                wizardScope.launch { try { prefs?.setPersona("flora") } catch (e: Exception) { } }
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        ) { Text("Nahi, friendly mode") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (ageGateNote.isNotEmpty()) {
                    Text(ageGateNote)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Row {
                    listOf("flora" to "Flora", "saathi" to "Saathi", "guru" to "Guru", "romantic" to "Romantic").forEach { (id, label) ->
                        Button(
                            onClick = {
                                // Item 34 gate: romantic seedha kabhi
                                // ON nahi — bina confirm ke dialog.
                                if (id == "romantic" && !ageConfirmedWiz) {
                                    ageGatePending = true
                                } else {
                                    val wasRomantic = personaSel == "romantic"
                                    personaSel = id
                                    wizardScope.launch {
                                        try { prefs?.setPersona(id) } catch (e: Exception) { }
                                        if (id == "romantic" || wasRomantic) {
                                            try { AutoLogStore(context).add(if (id == "romantic") "Romantic mode ON (wizard) — 18+ confirm pehle se tha" else "Romantic mode OFF (wizard) — $id mode") } catch (e: Exception) { }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        ) { Text(if (personaSel == id) "✓ $label" else label) }
                    }
                }
                // Item 34 gate dialog (wizard): Haan = confirm + ON;
                // Nahi = friendly Flora mode, romantic kabhi ON nahi.
                if (ageGatePending) {
                    AlertDialog(
                        onDismissRequest = { ageGatePending = false },
                        title = { Text("Romantic mode — 18+ confirm?") },
                        text = { Text("Romantic mode sirf 18 saal ya usse zyada umar ke liye hai. Flora ek AI saathi hai — ye roleplay hai, koi asli insaan ya partner hone ka daava nahi. Kya tum 18+ ho aur Romantic mode ON karna chahte ho?") },
                        confirmButton = {
                            TextButton(onClick = {
                                ageGatePending = false
                                ageConfirmedWiz = true
                                personaSel = "romantic"
                                wizardScope.launch {
                                    try { prefs?.setAgeConfirmed(true) } catch (e: Exception) { }
                                    try { prefs?.setPersona("romantic") } catch (e: Exception) { }
                                    try { AutoLogStore(context).add("Romantic mode ON (wizard gate) — 18+ confirm hua") } catch (e: Exception) { }
                                }
                            }) { Text("Haan, main 18+ hun") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                ageGatePending = false
                                personaSel = "flora"
                                ageGateNote = "18+ confirm nahi hua, isliye friendly Flora mode rakha hai — Romantic kabhi bhi Settings se 18+ confirm karke ON kar sakte ho."
                                wizardScope.launch {
                                    try { prefs?.setPersona("flora") } catch (e: Exception) { }
                                    try { AutoLogStore(context).add("Romantic mode OFF — 18+ confirm nahi hua, friendly Flora mode") } catch (e: Exception) { }
                                }
                            }) { Text("Nahi, friendly mode") }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    listOf("isabella" to "Sample 1", "lily" to "Sample 2 Lily", "myrtle" to "Sample 3").forEach { (id, label) ->
                        Button(
                            onClick = {
                                voiceSel = id
                                wizardScope.launch { try { prefs?.setVoiceSample(id) } catch (e: Exception) { } }
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        ) { Text(if (voiceSel == id) "✓ $label" else label) }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Baad me Settings se bhi badal sakte ho.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { step = 6 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("← Peeche") }
                TextButton(
                    onClick = { step = 8 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Skip karo → Agla step") }
            }
        }
        // Batch 2 (F1) step 8 — overlay: mic permission + setup, imaandaar guidance.
        if (step == 8) {
            item {
                val overlayAllowed = try {
                    android.provider.Settings.canDrawOverlays(context)
                } catch (e: Exception) {
                    false
                }
                Text("Overlay mic")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Overlay mic se kisi bhi screen par Flora ka mic button " +
                        "dikhta hai — dabate hi wo sunegi. Iske liye ek baar " +
                        "Android ki 'doosri app ke upar dikhne' ki permission " +
                        "chahiye, jo sirf tum khud de sakte ho."
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Overlay permission: " + if (overlayAllowed) "mila hua ✓" else "nahi mila — bina iske overlay nahi dikhega"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + context.packageName),
                                )
                            )
                        } catch (e: Exception) {
                            wizardNote = "Overlay settings nahi khul payi — phone ki Settings > Apps > Flora > 'Display over other apps' se do."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Overlay permission do") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (!overlayAllowed) {
                            wizardNote = "Pehle upar se overlay permission do, phir overlay mic chalu hoga."
                        } else {
                            try {
                                context.startForegroundService(
                                    Intent(context, com.flora.app.automation.OverlayMicService::class.java)
                                )
                                wizardNote = "Overlay mic chalu ho gaya."
                            } catch (e: Exception) {
                                wizardNote = "Overlay mic chalu nahi ho paya — dobara koshish karo."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Overlay mic chalu karo") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        try {
                            context.stopService(
                                Intent(context, com.flora.app.automation.OverlayMicService::class.java)
                            )
                            wizardNote = "Overlay mic band kar diya."
                        } catch (e: Exception) {
                            wizardNote = "Overlay mic band nahi ho paya."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Overlay mic band karo") }
                if (wizardNote.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(wizardNote)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Baad me chat me bhi bol sakte ho: 'overlay mic chalu karo' / 'overlay mic band karo'.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { step = 7 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("← Peeche") }
                TextButton(
                    onClick = { step = 9 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Skip karo → Agla step") }
            }
        }
        // Batch 2 (F1) step 9 — free-mode banner samjhao.
        if (step == 9) {
            item {
                Text("Free mode samajh lo")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Free mode me roz 10 minute baat milti hai, par app me " +
                        "maujood saare tools milte hain. Limit khatam hone " +
                        "par Flora imaandaari se bata degi — chupchaap nahi " +
                        "rokegi. Pro me unlimited baat + FCM push milta hai."
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Chat aur baat wali screen par hamesha ye banner dikhega:")
                Spacer(modifier = Modifier.height(4.dp))
                if (prefs != null) {
                    FreeRemainingText(prefs, null)
                } else {
                    Text("AAP FREE MODE ME HO — aaj 10 min baat milti hai.")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { step = 8 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("← Peeche") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("✓ Ho gaya — Flora kholo")
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = { step = 1 },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Shuru se dobara dekho") }
                Text("Koi bhi step chhoda ja sakta hai — Ho gaya dabate hi Flora khul jayegi.")
            }
        }
    }
}
