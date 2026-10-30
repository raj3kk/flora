package com.flora.app.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.flora.app.automation.OneTapFix
import com.flora.app.data.AutoLogStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Permissions screen (v2.15) — user ki shikayat "battery
 * unrestricted kahan hai, accessibility kahan hai" ka seedha
 * jawab: sab kuch ek hi screen par, live status ke saath.
 *
 * - Runtime permissions: har row tap = usi permission ka dialog.
 * - Battery unrestricted: live status + system dialog.
 * - Auto-start (Vivo/iQOO): brand intents, na khule to app-info.
 * - Accessibility: live status (FloraAutomationService ON hai ya
 *   nahi) + Accessibility settings ka button.
 * - Dialer role: call-talk ke liye, system dialog se.
 * Koi permission chhupke nahi li jati — har ek user ke tap par.
 */
@Composable
fun PermissionsScreen(onShowWizard: () -> Unit) {
    val context = LocalContext.current
    var note by remember { mutableStateOf("") }
    var refreshTick by remember { mutableStateOf(0) }
    // Lane A — A1 One-Tap Fix state (sab upar, LazyColumn se pehle):
    // note/refreshTick ke saath hi declare — section inhe neeche use karta hai.
    var oneTapNote by remember { mutableStateOf("") }
    var oneTapBusy by remember { mutableStateOf(false) }
    val oneTapScope = rememberCoroutineScope()
    val autoLogStore = remember { AutoLogStore(context) }
    // Resume par status auto-refresh (v2.23, Phase 5): settings se
    // wapas aate hi refreshTick badhega, jisse battery/accessibility
    // status dobara compute hoga (tick padhne ka pattern upar hai).
    val permLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(permLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTick++
            }
        }
        permLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { permLifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var pendingPerm by remember { mutableStateOf<String?>(null) }

    val singlePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        note = if (granted) {
            "Permission mil gayi ✓"
        } else {
            "Permission nahi mili — wo feature band rahega, poora app nahi."
        }
        pendingPerm = null
        refreshTick++
    }
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        note = "Dialer role check karne ke liye Status refresh karo."
        refreshTick++
    }

    fun granted(perm: String): Boolean {
        return ContextCompat.checkSelfPermission(context, perm) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun batteryIgnored(): Boolean {
        return try {
            context.getSystemService(PowerManager::class.java)
                ?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun accessibilityOn(): Boolean {
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
                as? AccessibilityManager ?: return false
            am.getEnabledAccessibilityServiceList(
                android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            ).any { it.resolveInfo.serviceInfo.packageName == context.packageName }
        } catch (e: Exception) {
            false
        }
    }

    fun dialerHeld(): Boolean {
        return try {
            val rm = context.getSystemService(RoleManager::class.java)
            rm?.isRoleHeld(RoleManager.ROLE_DIALER) == true
        } catch (e: Exception) {
            false
        }
    }

    // refreshTick padhne se status dobara compute hota hai.
    @Suppress("UNUSED_VARIABLE")
    val tick = refreshTick

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Permissions — sab kuch ek jagah, live status ke saath")
            Text(
                "Background mic ke liye Battery unrestricted + Auto-start " +
                    "zaroori hain; automation ke liye Accessibility; call-talk " +
                    "ke liye Dialer role. Koi bhi permission deny ho to sirf " +
                    "uska feature band hoga, poora app nahi."
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            Text("App permissions (row dabao to dialog khulega)")
            val rows = listOf(
                Manifest.permission.RECORD_AUDIO to "Mic — baat karne aur sunne ke liye",
                Manifest.permission.CAMERA to "Camera — Scan/photo/torch ke liye",
                Manifest.permission.READ_CONTACTS to "Contacts — naam se call/SMS ke liye",
                Manifest.permission.CALL_PHONE to "Call — confirmed call ke liye",
                Manifest.permission.SEND_SMS to "SMS — confirmed SMS ke liye",
                Manifest.permission.POST_NOTIFICATIONS to "Notifications — push aur background notice ke liye",
                Manifest.permission.READ_PHONE_STATE to "Phone state — call announce ke liye",
                Manifest.permission.READ_CALL_LOG to "Call log — caller number padhne ke liye (na ho to announce nahi chalega)",
                Manifest.permission.ACCESS_FINE_LOCATION to "Location — yahan ka mausam ke liye",
            )
            for ((perm, label) in rows) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        label + " — " + if (granted(perm)) "Mil gaya ✓" else "Baaki hai",
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        pendingPerm = perm
                        singlePermLauncher.launch(perm)
                    }) { Text(if (granted(perm)) "Mila hua" else "Do") }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            Text("Background — app back hone par bhi chale")
            Text(
                "Battery unrestricted: " + if (batteryIgnored()) {
                    "ON hai ✓ (background chalega)"
                } else {
                    "OFF hai — phone background me app rok sakta hai"
                }
            )
            Row {
                Button(onClick = {
                    try {
                        val pm = context.getSystemService(PowerManager::class.java)
                        if (pm != null && pm.isIgnoringBatteryOptimizations(context.packageName)) {
                            note = "Battery optimization pehle se band hai."
                        } else {
                            val intent = Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:" + context.packageName),
                            )
                            context.startActivity(intent)
                            note = "System dialog me 'Allow' dabao — phir wapas aakar Status refresh karo."
                        }
                    } catch (e: Exception) {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            )
                            note = "Battery settings khul rahi hain — wahan Flora ko 'Don't optimize' karo."
                        } catch (e2: Exception) {
                            note = "Battery settings nahi khul payi — phone ki Settings > Apps > Flora > Battery se karo."
                        }
                    }
                }) { Text("Battery unrestricted karo") }
                Spacer(modifier = Modifier.padding(4.dp))
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
                        note = "Auto-start/app-info khul rahi hai — wahan Flora ka auto-start ON karo."
                    } catch (e: Exception) {
                        note = "Auto-start settings nahi khul payi — phone ki Settings > Apps > Flora se karo."
                    }
                }) { Text("Auto-start kholo (Vivo/iQOO)") }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            Text("Automation — Accessibility")
            Text(
                "Accessibility (FloraAutomationService): " + if (accessibilityOn()) {
                    "ON hai ✓"
                } else {
                    "OFF hai — automation (doosri app chalana) iske bina nahi hoga"
                }
            )
            Button(onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    note = "Accessibility settings khul rahi hain — wahan Flora ko ON karo. Ye sirf tumhari marzi se hoga."
                } catch (e: Exception) {
                    note = "Accessibility settings nahi khul payi — phone ki Settings > Accessibility se karo."
                }
            }) { Text("Accessibility settings kholo") }
            Spacer(modifier = Modifier.height(12.dp))
        }
        // Lane A — A1 One-Tap Fix (R-A1-6/R-A1-7): accessibility section
        // ke paas hi — naya Screen entry nahi. Sirf Flora ki apni
        // service restore hoti hai (append-only, doosri services
        // barkaraar); battery-optimisation/auto-start isme nahi.
        // DOCTOR CARVE-OUT: secure-settings write sirf isi path
        // (OneTapFix.restore) se hota hai — DoctorScreen/planner
        // isko kabhi call nahi karte.
        item {
            Text("One-Tap Fix — Accessibility wapas lao")
            Text(
                "Phone (khaas kar Vivo) ya Android 13 kabhi-kabhi Flora ki " +
                    "accessibility service rok deta hai. Fix ek tap me use " +
                    "wapas ON karne ki koshish karta hai — sirf Flora ki " +
                    "service, kuch aur nahi. Status: " + if (accessibilityOn()) {
                    "ON hai ✓ — fix ki zaroorat nahi"
                } else {
                    "OFF hai — neeche Fix dabao"
                }
            )
            Text(
                "Android 13+ par sideload app ke liye pehle ye zaroori hai: " +
                    "Settings > Apps > Flora > upar ke 3 dots > " +
                    "'Allow restricted settings' ON karo — iske bina " +
                    "Accessibility ON hi nahi hoga. Reboot ke baad Shizuku " +
                    "band ho jata hai — Shizuku app se dobara chalao, phir Fix dabao."
            )
            Row {
                Button(
                    enabled = !oneTapBusy,
                    onClick = {
                        oneTapBusy = true
                        oneTapNote = "Fix chal raha hai…"
                        oneTapScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                OneTapFix.restore(context)
                            }
                            val msg = when (result) {
                                is OneTapFix.Result.AlreadyOn ->
                                    "Accessibility pehle se ON hai — kuch badla nahi."
                                is OneTapFix.Result.Restored ->
                                    "Ho gaya ✓ — Flora ki accessibility service wapas ON ho gayi." +
                                        if (result.enabledFlagChanged) " (Accessibility bhi ON kiya gaya.)" else ""
                                // A1 follow-up F1: aadha restore — "pehle se
                                // ON hai, kuch badla nahi" jaisa message
                                // yahan kabhi nahi; automation abhi poori
                                // tarah ON nahi hai, dobara Fix / adb guide.
                                is OneTapFix.Result.PartiallyRestored ->
                                    "Aadha hua: service list me jud gayi, par accessibility flag ON nahi ho paya (${result.reason}). Dobara Fix dabao ya adb guide se grant do — abhi automation poori tarah ON nahi hai."
                                is OneTapFix.Result.GrantMissing ->
                                    "Fix nahi ho paya: WRITE_SECURE_SETTINGS grant nahi mila. " +
                                        if (result.shizukuReady) {
                                            "Shizuku chal raha hai par grant nahi mila — neeche adb guide se grant do, phir dobara Fix dabao."
                                        } else {
                                            "Shizuku chalu karo ya adb guide se grant do, phir dobara Fix dabao."
                                        }
                                is OneTapFix.Result.Failed ->
                                    "Fix nahi ho paya: ${result.reason}. adb guide se grant do, phir dobara Fix dabao."
                            }
                            try {
                                autoLogStore.add("One-Tap Fix: $msg")
                            } catch (e: Exception) {
                            }
                            oneTapNote = msg
                            oneTapBusy = false
                            refreshTick++
                        }
                    }
                ) { Text(if (oneTapBusy) "Fix chal raha…" else "One-Tap Fix chalao") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = {
                    try {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as? ClipboardManager
                        cm?.setPrimaryClip(
                            ClipData.newPlainText("Flora adb command", OneTapFix.ADB_COMMAND)
                        )
                        oneTapNote = "adb command copy ho gaya — computer par terminal me chalao, phir dobara Fix dabao."
                    } catch (e: Exception) {
                        oneTapNote = "Copy nahi ho paya — command ye hai: " + OneTapFix.ADB_COMMAND
                    }
                }) { Text("adb command copy karo") }
            }
            Text(
                "adb guide: phone ko computer se jodo, USB debugging ON karo, " +
                    "phir computer par ye command chalao: " + OneTapFix.ADB_COMMAND +
                    " — isse Flora ko sirf secure-settings likhne ki permission " +
                    "milti hai, root nahi maanga jata."
            )
            if (oneTapNote.isNotEmpty()) {
                Text(oneTapNote)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            Text("Call-talk — Dialer role")
            Text(
                "Default dialer: " + if (dialerHeld()) {
                    "Flora hai ✓ (call uthake baat ho sakti hai)"
                } else {
                    "Flora nahi hai — call-talk ke liye zaroori hai"
                }
            )
            Button(onClick = {
                try {
                    val rm = context.getSystemService(RoleManager::class.java)
                    if (rm == null || !rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                        note = "Is phone par dialer role available nahi hai."
                    } else if (rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                        note = "Flora pehle se default dialer hai."
                    } else {
                        roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
                    }
                } catch (e: Exception) {
                    note = "Dialer role dialog nahi khul paya."
                }
            }) { Text("Default dialer banao") }
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            // Batch 2 (F7): overlay mic ka UI path — ab sirf voice
            // command se hi nahi, yahan se bhi milta hai. Voice
            // commands barkaraar hain. Permission na ho to sirf
            // imaandaar guidance, crash nahi.
            Text("Overlay mic — kisi bhi screen par Flora ka mic button")
            val overlayAllowed = try {
                android.provider.Settings.canDrawOverlays(context)
            } catch (e: Exception) {
                false
            }
            Text(
                "Overlay permission: " + if (overlayAllowed) {
                    "mila hua ✓"
                } else {
                    "nahi mila — bina iske overlay nahi dikhega"
                }
            )
            Row {
                Button(onClick = {
                    try {
                        context.startActivity(
                            Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + context.packageName),
                            )
                        )
                        note = "Overlay settings khul rahi hain — wahan Flora ko 'Display over other apps' do."
                    } catch (e: Exception) {
                        note = "Overlay settings nahi khul payi — phone ki Settings > Apps > Flora > 'Display over other apps' se do."
                    }
                }) { Text("Overlay permission do") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = {
                    if (!overlayAllowed) {
                        note = "Pehle overlay permission do, phir overlay mic chalu hoga."
                    } else {
                        try {
                            context.startForegroundService(
                                Intent(context, com.flora.app.automation.OverlayMicService::class.java)
                            )
                            note = "Overlay mic chalu ho gaya — ab kisi bhi screen par Flora ka mic button dikhega."
                        } catch (e: Exception) {
                            note = "Overlay mic chalu nahi ho paya, dobara koshish karo."
                        }
                    }
                }) { Text("Overlay mic chalu karo") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = {
                    try {
                        context.stopService(
                            Intent(context, com.flora.app.automation.OverlayMicService::class.java)
                        )
                        note = "Overlay mic band kar diya."
                    } catch (e: Exception) {
                        note = "Overlay mic band nahi ho paya."
                    }
                }) { Text("Overlay mic band karo") }
            }
            Text("Voice command se bhi chalta hai: 'overlay mic chalu karo' / 'overlay mic band karo'.")
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            Text("Mic beep: system recognizer ki 'tu-ta' beep ko sunne ke dauraan mute kar diya jata hai (v2.15). Kuch phones ki apni beep isse bhi nahi rukti — wo device ki limit hai.")
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = { refreshTick++ }) { Text("Status refresh karo") }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(onClick = onShowWizard) { Text("Setup wizard dobara dekho") }
            }
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(note)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Note: Vivo/iQOO jaise phones battery saver se background service phir bhi rok sakte hain — upar ke teeno (battery unrestricted + auto-start + notification) ON rakhna zaroori hai.")
        }
    }
}
