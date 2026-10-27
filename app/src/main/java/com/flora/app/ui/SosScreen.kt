package com.flora.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flora.app.automation.AutoActions
import com.flora.app.data.SosStore
import kotlinx.coroutines.launch

/**
 * SOS screen: up to 3 emergency contacts, stored on-device only.
 * Manual SOS asks confirmation, then opens dialer + SMS draft.
 * Auto SOS (v0.7, confirmed automation) asks ONE confirmation,
 * then places a direct call AND sends a direct SMS — no draft,
 * no manual step. Nothing runs without that confirmation, and
 * Auto SOS needs CALL_PHONE + SEND_SMS granted once by the user.
 */
@Composable
fun SosScreen(sosStore: SosStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contacts by sosStore.contacts.collectAsState(initial = emptyList())
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var confirmSos by remember { mutableStateOf(false) }
    var confirmAutoSos by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.CALL_PHONE] == true &&
            grants[Manifest.permission.SEND_SMS] == true
        ) {
            confirmAutoSos = true
        } else {
            note = "Auto SOS ke liye call + SMS permission chahiye — manual SOS button use kijiye."
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("SOS — Emergency contacts (zyada se zyada 3, sirf is phone par)")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Naam") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = number, onValueChange = { number = it },
                label = { Text("Mobile number") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    if (sosStore.add(name, number)) {
                        name = ""
                        number = ""
                        note = "Contact save ho gaya."
                    } else {
                        note = "3 contacts poore hain, ya naam/number khaali hai."
                    }
                }
            }) { Text("Contact jodo") }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { confirmSos = true },
                enabled = contacts.isNotEmpty()
            ) { Text("SOS — Madad mangao (manual dialer/SMS draft)") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (!AutoActions.hasCallPermission(context) ||
                        !AutoActions.hasSmsPermission(context)
                    ) {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.SEND_SMS)
                        )
                    } else {
                        confirmAutoSos = true
                    }
                },
                enabled = contacts.isNotEmpty()
            ) { Text("Auto SOS — ek confirmation, seedha call + SMS") }
            Text(
                "Auto SOS: confirm karne par pehle contact ko seedha call lagegi aur " +
                    "SMS bhi seedha jayega — koi draft nahi. Manual SOS me dialer/SMS draft khulta hai."
            )
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        itemsIndexed(contacts) { index, contact ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("${contact.name} — ${contact.number}", modifier = Modifier.weight(1f))
                Button(onClick = {
                    scope.launch { sosStore.remove(index) }
                }) { Text("Hatao") }
            }
        }
    }

    if (confirmSos && contacts.isNotEmpty()) {
        val first = contacts[0]
        AlertDialog(
            onDismissRequest = { confirmSos = false },
            title = { Text("SOS bhejna hai?") },
            text = {
                Text(
                    "Dialer ${first.name} (${first.number}) ke liye khulega aur " +
                        "SMS draft taiyaar hoga. Call aur Send aap khud dabayenge."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmSos = false
                    context.startActivity(
                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${first.number}"))
                    )
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${first.number}")).apply {
                            putExtra("sms_body", "Flora SOS: mujhe madad chahiye, kripya call kijiye.")
                        }
                    )
                }) { Text("Haan, kholo") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSos = false }) { Text("Ruko") }
            }
        )
    }

    if (confirmAutoSos && contacts.isNotEmpty()) {
        val first = contacts[0]
        AlertDialog(
            onDismissRequest = { confirmAutoSos = false },
            title = { Text("Auto SOS bhejna hai?") },
            text = {
                Text(
                    "${first.name} (${first.number}) ko seedha call lagegi aur " +
                        "SMS bhi seedha jayega. Confirm karne par dono ho jayenge."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmAutoSos = false
                    val called = AutoActions.placeCall(context, first.number)
                    val texted = AutoActions.sendSms(
                        context, first.number,
                        "Flora SOS: mujhe madad chahiye, kripya call kijiye."
                    )
                    note = "Auto SOS: call ${if (called) "lag gayi" else "nahi lagi"}, " +
                        "SMS ${if (texted) "bhej diya gaya" else "nahi gaya"}."
                }) { Text("Haan, bhejo") }
            },
            dismissButton = {
                TextButton(onClick = { confirmAutoSos = false }) { Text("Ruko") }
            }
        )
    }
}
