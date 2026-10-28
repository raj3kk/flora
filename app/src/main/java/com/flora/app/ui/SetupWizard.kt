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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Setup wizard (v2.13, Flora ka apna design): app pehli baar khulte
 * hi 3 steps me setup hota hai — Welcome, Permissions, Background.
 *
 * Step 2 ka behaviour: "Sab allow karo" dabane par Android ke
 * system dialogs ek-ek karke aate hain (ye Android ka apna flow
 * hai — ek dialog ke baad doosra). Har permission ki row par tap
 * karke usko akela bhi maanga ja sakta hai, aur "App settings
 * kholo" se user khud Settings me jaa kar bhi de sakta hai.
 * Sirf Microphone zaroori hai — uske bina "Aage badho" nahi
 * khulega; baaki permissions ke bina sirf unke features band
 * rahenge, app nahi atakegi.
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
        "Taaki tum Flora se baat kar sako. Zaroori hai.",
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
fun SetupWizard(onDone: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) }
    var grants by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var attempted by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
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
    fun isGranted(permission: String): Boolean {
        @Suppress("UNUSED_EXPRESSION")
        refresh
        return grants[permission] ?: (ContextCompat.checkSelfPermission(
            context, permission
        ) == PackageManager.PERMISSION_GRANTED)
    }
    val micGranted = isGranted(Manifest.permission.RECORD_AUDIO)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Flora set up karo")
            Text("Step $step / 3")
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
                    "Ek tap — Android ek ek karke sab poochega. Sirf " +
                        "microphone zaroori hai; baaki se wo kaam ki banti hai."
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
                    Text("Microphone allow hote hi \"Aage badho\" khul jayega.")
                }
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
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("✓ Ho gaya")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Koi bhi step chhoda ja sakta hai — Ho gaya dabate hi Flora khul jayegi.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Shuru me hi keys: Settings > Keys me apni Gemini key (AI Studio se free) aur Deepgram key (Deepgram console se) daalo — har key ke neeche link se key banao, phir 'Bhare hue keys check karo' se sahi/galat dekho, phir Save dabao. Keys sirf is phone par rehti hain.")
            }
        }
    }
}
