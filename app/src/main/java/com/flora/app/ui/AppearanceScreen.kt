package com.flora.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.flora.app.automation.FloraCharmService
import com.flora.app.data.AutoLogStore
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.launch

/**
 * P1 appearance bundle (SPEC-FREEZE, audit line 48 — names
 * ke alaawa records me koi behaviour detail nahi thi, isliye
 * neeche ka vyavhaar frozen design hai, guess par chhupa
 * hua nahi):
 *
 * - 3D-style orb: FloraOrbDepth (shaded 2D depth, koi OpenGL
 *   engine nahi). Toggle ON → chat header me wahi orb
 *   depth roop me dikhega; OFF → purana FloraOrb. Preview
 *   neeche dono roop dikhte hain.
 * - Floating charm: draggable overlay orb (FloraCharmService).
 *   Tap → sirf Flora chat khulti hai, koi action execute
 *   nahi hota. Toggle OFF / notification "Band karo" /
 *   permission revoke → charm hat jata hai (visible stop).
 *   Overlay permission user khud Settings se dega.
 * - Edge glow: chat-area ke kinaron par animated border,
 *   sirf jab toggle ON ho AUR Flora bol/sun rahi ho
 *   (SPEAKING/LISTENING). System-wide edge lighting is
 *   scope me nahi hai.
 *
 * Orb canvas (FloraOrb), themes (avatar rang), aur
 * live-wallpaper (item 2, FloraWallpaperService) is bundle
 * ke duplicate NAHI hain — orb wahi file me badha hai, rang
 * wahi hain, wallpaper ko chheda hi nahi gaya hai. Overlay
 * mic service bhi alag hai (mic vs charm-navigation).
 * Teenon toggles default OFF hain.
 */
@Composable
fun AppearanceScreen(autoLogStore: AutoLogStore, onBack: () -> Unit) {
    val appContext = LocalContext.current
    val prefs = remember { FloraPreferences(appContext) }
    val scope = rememberCoroutineScope()

    var note by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf("gulabi") }
    var orbDepth by remember { mutableStateOf(false) }
    var charmOn by remember { mutableStateOf(false) }
    var edgeGlow by remember { mutableStateOf(false) }

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
                avatar = prefs.avatar()
                orbDepth = prefs.p1OrbDepth()
                charmOn = prefs.p1Charm()
                edgeGlow = prefs.p1EdgeGlow()
            } catch (e: Exception) {
                note = "Appearance settings padhne me dikkat aayi — dobara kholo."
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Row {
            TextButton(onClick = onBack) { Text("Wapas") }
        }
        Text("Appearance — orb, charm, edge glow")
        if (note.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(note)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("3D-style orb preview (bayen: purana, dayen: depth)")
        Row {
            FloraOrb(busy = false, avatar = avatar)
            FloraOrbDepth(busy = false, avatar = avatar)
        }
        Text("Depth roop shaded 2D hai — koi alag 3D engine nahi. Rang wahi avatar rang hain.")
        Row {
            Text("Chat header me depth orb", modifier = Modifier.weight(1f))
            Switch(checked = orbDepth, onCheckedChange = { value ->
                scope.launch {
                    prefs.setP1OrbDepth(value)
                    orbDepth = value
                    log(if (value) "P1: depth orb ON" else "P1: depth orb OFF")
                    note = if (value) "Depth orb ON — chat header me depth roop dikhega." else "Depth orb OFF — purana orb wapas."
                }
            })
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Floating charm (tairta orb)")
        Text("Tap par sirf Flora chat khulti hai — koi kaam execute nahi hota. Drag karke kahin bhi le jao.")
        Row {
            Text("Charm dikhao", modifier = Modifier.weight(1f))
            Switch(checked = charmOn, onCheckedChange = { value ->
                if (value) {
                    if (!Settings.canDrawOverlays(appContext)) {
                        note = "Charm ke liye overlay permission chahiye — Settings khol raha hun, wahan Flora ko 'Display over other apps' do, phir toggle dobara karo."
                        try {
                            appContext.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + appContext.packageName)
                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            )
                        } catch (e: Exception) {
                            note = "Overlay Settings khul nahi payi — phone ki Settings me Flora ko overlay permission do."
                        }
                    } else {
                        scope.launch {
                            prefs.setP1Charm(true)
                            charmOn = true
                            log("P1: floating charm ON")
                            note = "Charm ON — tairta orb dikhega, tap par chat khulegi."
                        }
                        try {
                            appContext.startService(Intent(appContext, FloraCharmService::class.java))
                        } catch (e: Exception) {
                            note = "Charm service start nahi hui — dobara koshish karo."
                        }
                    }
                } else {
                    scope.launch {
                        prefs.setP1Charm(false)
                        charmOn = false
                        log("P1: floating charm OFF")
                        note = "Charm OFF — orb hata diya gaya."
                    }
                    try {
                        appContext.stopService(Intent(appContext, FloraCharmService::class.java))
                    } catch (e: Exception) {
                    }
                }
            })
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Edge glow (chat-area kinare)")
        Text("Sirf jab Flora bol/sun rahi ho tab kinaron par animated glow dikhega. Baaki waqt kuch nahi — battery bachane ke liye.")
        Row {
            Text("Edge glow ON", modifier = Modifier.weight(1f))
            Switch(checked = edgeGlow, onCheckedChange = { value ->
                scope.launch {
                    prefs.setP1EdgeGlow(value)
                    edgeGlow = value
                    log(if (value) "P1: edge glow ON" else "P1: edge glow OFF")
                    note = if (value) "Edge glow ON — chat me bolne/sunne par kinare chamkenge." else "Edge glow OFF."
                }
            })
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
