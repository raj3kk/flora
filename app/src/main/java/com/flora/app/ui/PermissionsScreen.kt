package com.flora.app.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
                Manifest.permission.CAMERA to "Camera — Scan/photo ke liye",
                Manifest.permission.READ_CONTACTS to "Contacts — naam se call/SMS ke liye",
                Manifest.permission.CALL_PHONE to "Call — confirmed call ke liye",
                Manifest.permission.SEND_SMS to "SMS — confirmed SMS ke liye",
                Manifest.permission.POST_NOTIFICATIONS to "Notifications — push aur background notice ke liye",
                Manifest.permission.READ_PHONE_STATE to "Phone state — call announce ke liye",
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
