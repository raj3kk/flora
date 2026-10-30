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
import com.flora.app.automation.TriggerScheduler
import com.flora.app.data.EventTrigger
import com.flora.app.data.TriggerStore
import kotlinx.coroutines.launch

/**
 * Event triggers screen (parity batch 2026-10-31, M5 — spec
 * FLORA_EVENT_TRIGGERS_SPEC.md). v1 me sirf TIME trigger:
 * roz ek waqt par fire. Action do tarah ka — routine (due-set
 * me naam, app khulte hi maujooda due-rule chalega) ya message
 * (sirf notification). Call/SMS/payment/delete trigger kabhi
 * nahi — ye design hard rule hai.
 */
@Composable
fun TriggersScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var triggers by remember { mutableStateOf<List<EventTrigger>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf("") }
    var actionType by remember { mutableStateOf("message") }
    var actionValue by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        triggers = TriggerStore.load(context)
    }

    fun refresh() {
        scope.launch { triggers = TriggerStore.load(context) }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Event triggers — roz ek waqt par fire honge (sirf time trigger).")
            Text("Routine action = routine due hogi (Auto Mode ON ho to khud chalegi, warna sirf bataya jayega). Message action = sirf notification.")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Trigger ka naam") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = timeText, onValueChange = { timeText = it },
                label = { Text("Waqt (HH:MM, jaise 09:00)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = { actionType = "message" }) {
                    Text(if (actionType == "message") "Message ✓" else "Message")
                }
                Button(onClick = { actionType = "routine" }) {
                    Text(if (actionType == "routine") "Routine ✓" else "Routine")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = actionValue, onValueChange = { actionValue = it },
                label = { Text(if (actionType == "routine") "Routine ka naam" else "Notification message") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val parts = timeText.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull()
                val minute = parts.getOrNull(1)?.toIntOrNull()
                if (name.isBlank() || actionValue.isBlank() || hour == null || minute == null ||
                    hour !in 0..23 || minute !in 0..59
                ) {
                    note = "Naam + sahi waqt (HH:MM) + action value teenon chahiye."
                    return@Button
                }
                val trigger = EventTrigger(name.trim(), hour, minute, actionType, actionValue.trim(), true)
                scope.launch {
                    TriggerStore.upsert(context, trigger)
                    TriggerScheduler.schedule(context, trigger)
                    name = ""
                    timeText = ""
                    actionValue = ""
                    note = "Trigger lag gaya — roz ${String.format("%02d:%02d", hour, minute)} par fire hoga."
                    refresh()
                }
            }) { Text("Trigger lagao") }
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        items(triggers) { trigger ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    "${trigger.name} — roz ${String.format("%02d:%02d", trigger.hour, trigger.minute)} — " +
                        "${trigger.actionType}: ${trigger.actionValue}" +
                        (if (!trigger.enabled) " (band)" else ""),
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    scope.launch {
                        val updated = trigger.copy(enabled = !trigger.enabled)
                        TriggerStore.upsert(context, updated)
                        if (updated.enabled) TriggerScheduler.schedule(context, updated)
                        else TriggerScheduler.cancel(context, trigger)
                        refresh()
                    }
                }) { Text(if (trigger.enabled) "Band" else "Chalu") }
                Button(onClick = {
                    scope.launch {
                        TriggerScheduler.cancel(context, trigger)
                        TriggerStore.remove(context, trigger.name)
                        refresh()
                    }
                }) { Text("Hatao") }
            }
        }
    }
}
