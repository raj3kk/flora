package com.flora.app.planner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * B4 PLAN DIKHAO UI (F04/F14/F17).
 *
 * Plan user ko dikhe: goal + ordered steps + har step ka tool
 * + success-check + stops + no-do + rollback + side-effect
 * preview (M8: kya banega, kahan, kis naam se — M10 external
 * names ke saath) + progress + estimate (F17, sirf jaankari).
 * Preview approval-gate NAHI hai (M9) — plan dikhte hi
 * execution shuru ho sakti hai, user kabhi bhi "ruko/badlo"
 * bol sakta hai. Ruko = stopPlan, Badlo = amend (skip).
 *
 * Har composable ke params me jo chahiye wahi hai (FloraApp
 * wali voice-scope galti dobara nahi — SettingsScreen wali
 * seekh: signature + call site dono check).
 */
@Composable
fun PlannerScreen(engine: PlannerEngine) {
    val state by engine.state.collectAsState()
    val pendingGoal by engine.pendingGoal.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var goal by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var plans by remember { mutableStateOf(listOf<StoredPlan>()) }

    LaunchedEffect(pendingGoal) {
        if (pendingGoal.isNotBlank()) goal = pendingGoal
    }
    LaunchedEffect(Unit) {
        plans = engine.listPlans()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp)
    ) {
        Text(text = "Planner — plan banao, dekho, chalao")
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = goal,
            onValueChange = { newGoal -> goal = newGoal },
            label = { Text(text = "Kya karna hai? (ek baar bolo)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(
                onClick = {
                    scope.launch {
                        message = "Plan ban raha hai..."
                        val created = engine.createPlan(goal.trim())
                        if (created.isSuccess) {
                            plans = engine.listPlans()
                            message = "Plan taiyaar hai — neeche dekho, phir Chalao dabao."
                        } else {
                            message = "Plan nahi bana: ${created.exceptionOrNull()?.message ?: "unknown"}"
                        }
                    }
                }
            ) {
                Text(text = "Plan banao")
            }
        }
        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message)
        }
        val plan = state.plan
        if (plan != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Goal: ${plan.goal}")
            Text(text = "Version: v${plan.version} | Status: ${plan.status} | Prompts (stops ke alawa): ${plan.promptCount}")
            Text(text = "Estimate: ${plan.estimate.freeLimitNote}")
            if (plan.stops.isNotEmpty()) {
                Text(text = "Stops: ${plan.stops.joinToString(", ")}")
            }
            if (plan.noDo.isNotEmpty()) {
                Text(text = "No-do (ye nahi chhedega): ${plan.noDo.joinToString(", ")}")
            }
            if (plan.rollback.isNotBlank()) {
                Text(text = "Rollback: ${plan.rollback}")
            }
            if (plan.history.isNotEmpty()) {
                Text(text = "History: ${plan.history.joinToString(" | ")}")
            }
            if (plan.lastError.isNotBlank()) {
                Text(text = "Aakhri rukawat: ${plan.lastError}")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(
                    onClick = {
                        scope.launch {
                            engine.startPlan(plan.planId)
                            plans = engine.listPlans()
                        }
                    }
                ) {
                    Text(text = "Chalao")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        engine.stopPlan(plan.planId)
                    }
                ) {
                    Text(text = "Ruko")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        engine.resumePlan(plan.planId)
                    }
                ) {
                    Text(text = "Resume")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            for (step in plan.steps) {
                Text(text = "${step.stepIndex + 1}. ${step.title} [${step.tool}] — ${step.status}")
                Text(text = "   Success-check: ${step.successCheck}")
                if (step.sideEffectPreview.isNotBlank()) {
                    Text(text = "   Side-effect preview: ${step.sideEffectPreview}")
                }
                if (step.externalName.isNotBlank()) {
                    Text(text = "   Bahar dikhne wala naam: ${step.externalName}")
                }
                if (step.resultText.isNotBlank()) {
                    Text(text = "   Result: ${step.resultText}")
                }
                if (step.status == StepStatus.PENDING) {
                    Row {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    engine.amendPlan(plan.planId, skipIndices = setOf(step.stepIndex))
                                    plans = engine.listPlans()
                                }
                            }
                        ) {
                            Text(text = "Ye step mat karo (skip)")
                        }
                    }
                }
                if (step.status == StepStatus.BLOCKED && step.tool == PlanTool.MACRO_UI_STEP) {
                    Row {
                        OutlinedButton(
                            onClick = {
                                engine.confirmMacroStep(plan.planId, step.stepIndex)
                            }
                        ) {
                            Text(text = "Macro ho gaya, aage badho")
                        }
                    }
                }
                if ((step.spend || step.irreversible) && step.status == StepStatus.PENDING) {
                    Row {
                        OutlinedButton(
                            onClick = {
                                engine.approveAndResume(plan.planId, step.stepIndex)
                            }
                        ) {
                            Text(text = "Confirm karke aage badho (stop approval)")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val report = engine.compensationReport(plan.planId)
                            message = report.joinToString("\n")
                        }
                    }
                ) {
                    Text(text = "Compensation dekho")
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Pichhle plans:")
        for (old in plans) {
            Text(text = "- ${old.goal} (v${old.version}, ${old.status})")
        }
    }
}
