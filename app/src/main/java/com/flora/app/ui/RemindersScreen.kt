package com.flora.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.flora.app.automation.ReminderScheduler
import com.flora.app.data.Reminder
import com.flora.app.data.ReminderStore
import kotlinx.coroutines.launch

/**
 * Reminders screen (parity batch 2026-10-31, M2 — Maya
 * ReminderReceiver ka Flora version, apne code se).
 * Reminder = roz ek waqt par notification + message. Routine
 * jobs se alag store hai (title kabhi routine schedule se
 * takrayega nahi). Fire hone par sirf notification aata hai —
 * koi routine, koi action, koi SMS/call kabhi nahi chalta.
 * Notification permission na ho to alarm chupchaap skip hoga
 * (crash nahi) — permission Settings se di ja sakti hai.
 * Reboot ke baad alarms BootReceiver dobara lagata hai.
 */
@Composable
fun RemindersScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reminders by remember { mutableStateOf<List<Reminder>>(emptyList()) }
    var title by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        reminders = ReminderStore.load(context)
    }

    fun refresh() {
        scope.launch { reminders = ReminderStore.load(context) }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Reminders — roz ek waqt par sirf yaad dilayega, kuch chalega nahi.")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = timeText, onValueChange = { timeText = it },
                label = { Text("Waqt (HH:MM, jaise 08:30)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = message, onValueChange = { message = it },
                label = { Text("Message (notification me dikhega)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val parts = timeText.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull()
                val minute = parts.getOrNull(1)?.toIntOrNull()
                if (title.isBlank() || hour == null || minute == null ||
                    hour !in 0..23 || minute !in 0..59
                ) {
                    note = "Title + sahi waqt (HH:MM) dono chahiye."
                    return@Button
                }
                val reminder = Reminder(title.trim(), hour, minute, message.trim(), true)
                scope.launch {
                    ReminderStore.upsert(context, reminder)
                    ReminderScheduler.schedule(context, reminder)
                    title = ""
                    timeText = ""
                    message = ""
                    note = "Reminder lag gaya — roz ${String.format("%02d:%02d", hour, minute)} par notification aayega."
                    refresh()
                }
            }) { Text("Reminder lagao") }
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        items(reminders) { reminder ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    "${reminder.title} — roz ${String.format("%02d:%02d", reminder.hour, reminder.minute)}" +
                        (if (reminder.message.isNotBlank()) " — ${reminder.message}" else "") +
                        (if (!reminder.enabled) " (band)" else ""),
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    scope.launch {
                        val updated = reminder.copy(enabled = !reminder.enabled)
                        ReminderStore.upsert(context, updated)
                        if (updated.enabled) ReminderScheduler.schedule(context, updated)
                        else ReminderScheduler.cancel(context, reminder)
                        refresh()
                    }
                }) { Text(if (reminder.enabled) "Band" else "Chalu") }
                Button(onClick = {
                    scope.launch {
                        ReminderScheduler.cancel(context, reminder)
                        ReminderStore.remove(context, reminder.title)
                        refresh()
                    }
                }) { Text("Hatao") }
            }
        }
    }
}
