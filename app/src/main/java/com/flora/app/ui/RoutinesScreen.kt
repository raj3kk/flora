package com.flora.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.flora.app.automation.JobScheduler
import com.flora.app.data.Job
import com.flora.app.data.JobStore
import com.flora.app.data.Routine
import com.flora.app.data.RoutineStore
import kotlinx.coroutines.launch

/**
 * Routines screen (v1.1, draft — FloraApp wiring v1.0 fix ke baad).
 * Routine banao: naam + commands (ek line me ek command, jaise
 * "open WhatsApp", "mausam batao"). Chalao: ek confirmation ke baad
 * commands baari-baari Chat ko bheji jayengi, har step bol kar.
 * Payment/delete step par dobara confirmation hoga — routine us
 * rule ko override nahi karti.
 *
 * v2.22 (chunk 4): routine ko roz ek waqt par bhi lagaya ja
 * sakta hai (JobScheduler alarm + CHAT khulte hi auto-run).
 * Auto Mode ON ho to waqt par khud chalegi, warna Flora
 * sirf batayegi. Reboot ke baad alarms BootReceiver dobara
 * lagata hai.
 */
@Composable
fun RoutinesScreen(voice: VoiceEngine, onRunRoutine: (Routine) -> Unit, onRunMacro: (com.flora.app.data.StoredMacro) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var routines by remember { mutableStateOf<List<Routine>>(emptyList()) }
    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var timeInputs by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var name by remember { mutableStateOf("") }
    var commandsText by remember { mutableStateOf("") }
    // Phase 6: Macros — naam + JSON steps. Save se pehle parse
    // validate hota hai; kharab JSON save nahi hoga.
    var macros by remember { mutableStateOf<List<com.flora.app.data.StoredMacro>>(emptyList()) }
    var macroName by remember { mutableStateOf("") }
    var macroJsonText by remember { mutableStateOf("") }
    var confirmMacro by remember { mutableStateOf<com.flora.app.data.StoredMacro?>(null) }
    var note by remember { mutableStateOf("") }
    var confirmRun by remember { mutableStateOf<Routine?>(null) }

    LaunchedEffect(Unit) {
        routines = RoutineStore.load(context)
        jobs = JobStore.load(context)
        macros = com.flora.app.data.MacroStore.load(context)
    }

    fun say(message: String) {
        note = message
        voice.speak(message)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Routines — ek baar banao, ek confirmation se poori chalegi.")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Routine ka naam (jaise Subah)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = commandsText, onValueChange = { commandsText = it },
                label = { Text("Commands — ek line me ek (jaise open WhatsApp)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val cmds = commandsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (name.isBlank() || cmds.isEmpty()) {
                    say("Naam aur kam se kam ek command dono chahiye.")
                } else {
                    val updated = routines + Routine(name.trim(), cmds)
                    scope.launch {
                        RoutineStore.save(context, updated)
                        routines = updated
                        name = ""
                        commandsText = ""
                        say("Routine '${updated.last().name}' save ho gayi.")
                    }
                }
            }) { Text("Routine save karo") }
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Meri routines")
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(routines) { routine ->
            Text("${routine.name} (${routine.commands.size} steps)")
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = { confirmRun = routine }) { Text("Chalao: ${routine.name}") }
            Spacer(modifier = Modifier.height(4.dp))
            val job = jobs.firstOrNull { it.routineName == routine.name }
            Text(
                if (job != null) "Roz ${String.format("%02d:%02d", job.hour, job.minute)} par chalegi."
                else "Roz ka waqt abhi nahi laga hai."
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = timeInputs[routine.name] ?: "",
                onValueChange = { timeInputs = timeInputs + (routine.name to it) },
                label = { Text("Waqt (HH:MM, jaise 08:00)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = {
                val parts = (timeInputs[routine.name] ?: "").split(":")
                val h = parts.getOrNull(0)?.toIntOrNull()
                val m = parts.getOrNull(1)?.toIntOrNull()
                if (h == null || m == null || h !in 0..23 || m !in 0..59) {
                    say("Waqt galat hai — HH:MM me likho, jaise 08:00.")
                } else {
                    val newJob = Job(routine.name, h, m, true)
                    scope.launch {
                        JobStore.upsert(context, newJob)
                        JobScheduler.schedule(context, newJob)
                        jobs = JobStore.load(context)
                        say("Routine '${routine.name}' roz ${String.format("%02d:%02d", h, m)} par chalegi.")
                    }
                }
            }) { Text("Roz is waqt chalao") }
            if (job != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = {
                    scope.launch {
                        JobScheduler.cancel(context, job)
                        JobStore.remove(context, routine.name)
                        jobs = JobStore.load(context)
                        say("Routine '${routine.name}' ka roz ka waqt hata diya.")
                    }
                }) { Text("Schedule hatao") }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            Text("Macros — JSON steps (open → tap → type → wait → waitText → scroll → back).")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = macroName, onValueChange = { macroName = it },
                label = { Text("Macro ka naam") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = macroJsonText, onValueChange = { macroJsonText = it },
                label = { Text("Steps JSON (jaise {\"name\":\"Subah\",\"steps\":[{\"type\":\"open\",\"app\":\"YouTube\"}]}") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (macroName.isBlank() || macroJsonText.isBlank()) {
                    say("Macro ka naam aur steps JSON dono chahiye.")
                } else {
                    val parsed = com.flora.app.automation.MacroJson.parse(macroJsonText.trim())
                    if (parsed == null) {
                        say("Macro JSON galat hai — steps ka format check karo, save nahi hua.")
                    } else {
                        val updated = macros.filter { it.name != macroName.trim() } +
                            com.flora.app.data.StoredMacro(macroName.trim(), macroJsonText.trim())
                        scope.launch {
                            com.flora.app.data.MacroStore.save(context, updated)
                            macros = updated
                            macroName = ""
                            macroJsonText = ""
                            say("Macro '${updated.last().name}' save ho gayi.")
                        }
                    }
                }
            }) { Text("Macro save karo") }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Mere macros")
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(macros) { macro ->
            Text("${macro.name}")
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = { confirmMacro = macro }) { Text("Chalao: ${macro.name}") }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    confirmMacro?.let { macro ->
        AlertDialog(
            onDismissRequest = { confirmMacro = null },
            title = { Text("Macro chalau?") },
            text = { Text("'${macro.name}' ke steps accessibility se chalenge. Sensitive step (password wali screen par pay/submit) par dobara poocha jayega.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmMacro = null
                    onRunMacro(macro)
                }) { Text("Haan, chalao") }
            },
            dismissButton = {
                TextButton(onClick = { confirmMacro = null }) { Text("Ruko") }
            }
        )
    }

    confirmRun?.let { routine ->
        AlertDialog(
            onDismissRequest = { confirmRun = null },
            title = { Text("Routine chalau?") },
            text = { Text("'${routine.name}' ke ${routine.commands.size} steps ek-ek karke chalenge. Call/SMS steps routine me nahi chalte — unke liye Chat se alag confirmation chahiye.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRun = null
                    onRunRoutine(routine)
                }) { Text("Haan, chalao") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRun = null }) { Text("Ruko") }
            }
        )
    }
}
