package com.flora.app.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import com.flora.app.assistant.VoiceEngine
import com.flora.app.automation.AutoActions
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.launch

/**
 * Connect screen (v0.7, confirmed automation): user says it once,
 * ONE confirmation dialog, then the action executes fully.
 * - Auto Call: direct call (CALL_PHONE permission, user-granted once).
 * - Auto SMS: direct SMS (SEND_SMS permission, user-granted once).
 * - Share: system share sheet only — social apps do not allow any
 *   app to post silently on the user's behalf without their own
 *   login/SDK, so the user still picks the app and presses Post.
 * If a permission is denied, the screen falls back to the manual
 * dialer / SMS draft and says so — it never pretends an auto
 * action happened when it did not.
 */
@Composable
fun ConnectScreen(voice: VoiceEngine, prefs: FloraPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var callTalk by remember { mutableStateOf(false) }
    var dialerHeld by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        callTalk = prefs.callTalk()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            dialerHeld = rm?.isRoleHeld(RoleManager.ROLE_DIALER) == true
        }
    }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            dialerHeld = rm?.isRoleHeld(RoleManager.ROLE_DIALER) == true
            note = if (dialerHeld) "Flora ab default dialer hai."
                else "Default dialer nahi bana — bina iske Flora call nahi utha sakti."
        }
    }
    // note yahan upar hai kyunki roleLauncher ka callback isko use karta
    // hai — neeche declare karne par compile error aata (use-before-declaration).
    var note by remember { mutableStateOf("") }
    var shareText by remember { mutableStateOf("") }
    var callNumber by remember { mutableStateOf("") }
    var smsNumber by remember { mutableStateOf("") }
    var smsText by remember { mutableStateOf("") }
    var pendingCall by remember { mutableStateOf<String?>(null) }
    var pendingSms by remember { mutableStateOf<Pair<String, String>?>(null) }
    var wantCallPermission by remember { mutableStateOf(false) }
    var wantSmsPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (wantCallPermission && grants[Manifest.permission.CALL_PHONE] == true) {
            wantCallPermission = false
            pendingCall = callNumber.trim()
        } else if (wantSmsPermission && grants[Manifest.permission.SEND_SMS] == true) {
            wantSmsPermission = false
            pendingSms = smsNumber.trim() to smsText
        } else {
            wantCallPermission = false
            wantSmsPermission = false
            note = "Permission nahi mili — manual dialer / SMS draft khol raha hun."
            if (callNumber.isNotBlank()) {
                context.startActivity(
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${callNumber.trim()}"))
                )
            }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Auto Call — ek confirmation ke baad seedha call lagega (draft nahi)")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = callNumber, onValueChange = { callNumber = it },
                label = { Text("Mobile number") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (callNumber.isBlank()) {
                    note = "Pehle number likhiye."
                } else if (!AutoActions.hasCallPermission(context)) {
                    wantCallPermission = true
                    permissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                } else {
                    pendingCall = callNumber.trim()
                }
            }) { Text("Call lagao") }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Auto SMS — ek confirmation ke baad seedha SMS jayega (draft nahi)")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = smsNumber, onValueChange = { smsNumber = it },
                label = { Text("Mobile number") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = smsText, onValueChange = { smsText = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (smsNumber.isBlank()) {
                    note = "Pehle number likhiye."
                } else if (!AutoActions.hasSmsPermission(context)) {
                    wantSmsPermission = true
                    permissionLauncher.launch(arrayOf(Manifest.permission.SEND_SMS))
                } else {
                    pendingSms = smsNumber.trim() to smsText
                }
            }) { Text("SMS bhejo") }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Share — app tum chunoge, Post tum dabaaoge (social apps silent post allow nahi karti)")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = shareText, onValueChange = { shareText = it },
                label = { Text("Share karne wala text") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (shareText.isBlank()) {
                    note = "Pehle share karne wala text likhiye."
                } else {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Kahan share karna hai?"))
                }
            }) { Text("Share sheet kholo") }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Call-talk (dialer role) — Flora call utha kar baat karegi, sirf tab jab aap khud ise default dialer banayenge.")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Default dialer: " + if (dialerHeld) "Flora hai" else "Flora nahi hai")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    note = "Dialer role ke liye Android 10+ chahiye."
                } else {
                    val rm = context.getSystemService(RoleManager::class.java)
                    if (rm == null || !rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                        note = "Is phone par dialer role available nahi hai."
                    } else if (rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                        note = "Flora pehle se default dialer hai."
                    } else {
                        roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
                    }
                }
            }) { Text("Flora ko default dialer banao") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val next = !callTalk
                scope.launch {
                    prefs.setCallTalk(next)
                    callTalk = next
                    val msg = if (next) "Call-talk ON: default dialer hote hi Flora call uthayegi."
                        else "Call-talk OFF."
                    note = msg
                    voice.speak(msg)
                }
            }) { Text("Call-talk: " + if (callTalk) "ON" else "OFF") }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Limit imaandari se: TTS ka doosri taraf saaf sunai dena phone test ke baad hi kaha ja sakta hai — pehle se guarantee nahi hai.")
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
        }
    }

    pendingCall?.let { number ->
        AlertDialog(
            onDismissRequest = { pendingCall = null },
            title = { Text("Call lagau?") },
            text = { Text("$number par seedha call lagega. Confirm karne par call lag jayegi.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingCall = null
                    note = if (AutoActions.placeCall(context, number)) {
                        "Call laga di gayi: $number"
                    } else {
                        "Call nahi lagi — permission ya number check kijiye."
                    }
                }) { Text("Haan, call lagao") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCall = null }) { Text("Ruko") }
            }
        )
    }

    pendingSms?.let { (number, text) ->
        AlertDialog(
            onDismissRequest = { pendingSms = null },
            title = { Text("SMS bheju?") },
            text = { Text("$number par ye SMS seedha jayega: \"$text\"") },
            confirmButton = {
                TextButton(onClick = {
                    pendingSms = null
                    note = if (AutoActions.sendSms(context, number, text)) {
                        "SMS bhej diya gaya: $number"
                    } else {
                        "SMS nahi gaya — permission ya number check kijiye."
                    }
                }) { Text("Haan, bhejo") }
            },
            dismissButton = {
                TextButton(onClick = { pendingSms = null }) { Text("Ruko") }
            }
        )
    }
}
