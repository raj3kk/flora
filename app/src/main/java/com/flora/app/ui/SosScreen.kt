package com.flora.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
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
import com.flora.app.data.SosStore
import kotlinx.coroutines.launch

/**
 * SOS screen: up to 3 emergency contacts, stored on-device only.
 * SOS button asks for confirmation, then opens the dialer for the
 * first contact AND an SMS draft with a help message — the user
 * presses Call / Send themselves. The app never calls, texts,
 * records, or tracks silently. Face Lock / Guardian voice-match
 * are separate later steps; nothing here claims them.
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
            ) { Text("SOS — Madad mangao") }
            Text(
                "SOS dabane par pehle confirmation poochi jayegi, phir dialer aur " +
                    "SMS draft khulenge — Call / Send aap khud dabayenge. App chupke " +
                    "se call ya message kabhi nahi karti."
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
}
