package com.flora.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.flora.app.data.AutoLogStore
import com.flora.app.planner.PlannerEngine
import com.flora.app.planner.StepStatus
import com.flora.app.planner.StoredPlan
import com.flora.app.planner.TaskListStore
import com.flora.app.planner.UserTask
import kotlinx.coroutines.launch

/**
 * SD-P1 Batch 3 (SD-6 task-list surface, Lane A).
 *
 * Sirf surface: create / list / complete / reopen + due +
 * notes. Planner core (M1–M15) ko ye screen kabhi nahi
 * chhooti — na Room, na engine state, na step status.
 * Planner completion marker READ-ONLY hai: task se juda plan
 * mile to uska status + VERIFIED steps ki ginti dikhti hai.
 * Task complete karna plan ko complete nahi karta, aur plan
 * DONE hona task ko auto-complete nahi karta — dono alag
 * rahenge, warna surface chhupke se core ban jata.
 */
@Composable
fun TaskListScreen(
    engine: PlannerEngine,
    autoLogStore: AutoLogStore,
    onBack: () -> Unit,
) {
    val appContext = LocalContext.current
    val store = remember { TaskListStore(appContext) }
    val scope = rememberCoroutineScope()

    var note by remember { mutableStateOf("") }
    var tasks by remember { mutableStateOf(listOf<UserTask>()) }
    var plans by remember { mutableStateOf(listOf<StoredPlan>()) }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }
    var planId by remember { mutableStateOf("") }

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
                tasks = store.list()
            } catch (e: Exception) {
                note = "Tasks padhne me dikkat aayi."
            }
            try {
                plans = engine.listPlans()
            } catch (e: Exception) {
                plans = emptyList()
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    fun markerFor(task: UserTask): String {
        if (task.planId.isBlank()) return "Kisi plan se juda nahi"
        val plan = plans.firstOrNull { it.planId == task.planId }
            ?: return "Plan nahi mila (id: ${task.planId.take(12)})"
        val verified = plan.steps.count { it.status == StepStatus.VERIFIED }
        return "Plan: ${plan.status} • ${verified}/${plan.steps.size} steps verified"
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Row {
            TextButton(onClick = onBack) { Text("Wapas") }
        }
        Text("Task list — planner ke upar ki surface")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Task banao, poora karo, dobara kholo. Due + notes ke saath. " +
                "Plan se joda task us plan ka status marker dikhata hai — " +
                "par task aur plan alag-alag rahenge.",
        )
        if (note.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(note)
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text("Task kya hai?") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = notes, onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = due, onValueChange = { due = it },
            label = { Text("Due (jaise: kal, 31 Oct)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = planId, onValueChange = { planId = it },
            label = { Text("Plan id (optional — marker ke liye)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = {
            scope.launch {
                val created = store.create(title, notes, due, planId)
                if (created == null) {
                    note = "Task khaali hai — pehle naam likho."
                } else {
                    title = ""
                    notes = ""
                    due = ""
                    planId = ""
                    log("SD-6: task banaya: ${created.title.take(60)}")
                    note = "Task ban gaya."
                    refresh()
                }
            }
        }) { Text("Task banao") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Tasks (${tasks.size})")
        tasks.forEach { task ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(if (task.done) "✓ ${task.title}" else "○ ${task.title}")
            if (task.notes.isNotBlank()) Text("Notes: ${task.notes.take(200)}")
            if (task.dueText.isNotBlank()) Text("Due: ${task.dueText.take(80)}")
            Text(markerFor(task))
            Row {
                Button(onClick = {
                    scope.launch {
                        val next = !task.done
                        if (store.setDone(task.id, next)) {
                            log(if (next) "SD-6: task complete: ${task.title.take(60)}" else "SD-6: task dobara khola: ${task.title.take(60)}")
                            note = if (next) "Task complete ho gaya." else "Task dobara khol diya gaya."
                            refresh()
                        } else {
                            note = "Task update nahi hua — dobara koshish karo."
                        }
                    }
                }) { Text(if (task.done) "Dobara kholo" else "Complete karo") }
            }
        }
        if (tasks.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Abhi koi task nahi hai — upar se pehla task banao.")
        }
    }
}
