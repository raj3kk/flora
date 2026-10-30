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
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
fun PlannerScreen(
    engine: PlannerEngine,
    orchestrator: GoalOrchestrator? = null,
    onOpenDoctor: () -> Unit = {},
    // Phase 0, Item 2 — ek-tap Routines raasta. Default khali taaki
    // purane call sites bina badle compile hon; FloraApp call site
    // named args se Screen.ROUTINES deta hai. Trailing-lambda trap
    // se bachne ke liye call site par dono args NAMED hain.
    onOpenRoutines: () -> Unit = {},
) {
    val state by engine.state.collectAsState()
    val pendingGoal by engine.pendingGoal.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var goal by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var plans by remember { mutableStateOf(listOf<StoredPlan>()) }
    // Phase 9, A2 (G12): orchestrator section ke states — launcher/
    // callback vars use se pehle declare (order trap se bacha).
    val goalsFlow = remember(orchestrator) {
        orchestrator?.goals ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<GoalRun>())
    }
    val goals by goalsFlow.collectAsState()
    var orchGoal by remember { mutableStateOf("") }
    var orchSubs by remember { mutableStateOf("") }
    var orchApproveSteps by remember { mutableStateOf(mapOf<String, Int>()) }
    // B4 follow-up P1-2: har Supabase step ka transient password field (stepIndex -> text).
    // Ye state sirf UI me hai — amend ke baad turant saaf hoti hai, Room me kabhi nahi jati.
    var dbPasswords by remember { mutableStateOf(mapOf<Int, String>()) }

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
        // B4-DOCTOR: sirf navigation — fix wiring DoctorScreen me hi.
        Row {
            OutlinedButton(onClick = { onOpenDoctor() }) {
                Text(text = "Doctor kholo (project scan/fix)")
            }
        }
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
            // A3 (f, R-A3-A5): sirf LABEL badla — promptCount ka
            // matlab stops/block count hai (bump sirf Engine
            // ke Stopped aur genuine-block paths par hota hai).
            // Bump sites chhede nahi gaye (metric ka arth wahi).
            Text(text = "Version: v${plan.version} | Status: ${plan.status} | Prompts (stops/blocks): ${plan.promptCount}")
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
                    // B4 follow-up P1-1: purana "Macro ho gaya" attestation button HATA DIYA.
                    // Ye button sirf tab VERIFIED karta hai jab Routines runner ka Done log
                    // mile — bina log ke step BLOCKED hi rehta hai (engine me fail-closed).
                    Text(text = "Macro Routines me save hai ('${engine.plannerMacroName(plan.planId, step.stepIndex)}') — neeche wale button se ek tap me Routines kholo, wahan Mere macros se Chalao + confirm se chalao, phir Planner me wapas aakar confirm button dabao.")
                    Row {
                        OutlinedButton(
                            onClick = { onOpenRoutines() }
                        ) {
                            Text(text = "Routines me macro chalao (ek tap)")
                        }
                    }
                    Row {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val confirmed = engine.confirmMacroExecuted(plan.planId, step.stepIndex)
                                    message = confirmed.fold(
                                        { ok -> ok },
                                        { err -> "Confirm nahi hua: ${err.message ?: "runner log nahi mila"}" },
                                    )
                                    plans = engine.listPlans()
                                }
                            }
                        ) {
                            Text(text = "Macro chal gaya — runner log se confirm karo")
                        }
                    }
                    // Phase 0, Item 2 NOTE: upar wala confirm button + engine ka
                    // fail-closed proof (AutoLog me delivery ke baad ka "Macro: <name>
                    // -> ho gaya" entry) CHHEDA NAHI gaya — bina runner log ke
                    // "ho gaya" attestation dobara nahi laya gaya (B4 P1-1 wahi rehta hai).
                }
                if (step.tool == PlanTool.SUPABASE_CREATE_PROJECT && step.status == StepStatus.BLOCKED) {
                    // B4 follow-up P1-2: Blocked message "plan amend se ek baar do" kehta tha,
                    // par amend ka sirf skip button tha — ab input-change UI amendPlan se wired hai.
                    // Password yahan se seedha transient amend payload me jata hai (memory only,
                    // Room me kabhi nahi); amend BLOCKED step ko PENDING karta hai (engine P2(c)
                    // reset), phir Resume/Chalao se dobara try hoga.
                    val dbPassword = dbPasswords[step.stepIndex] ?: ""
                    OutlinedTextField(
                        value = dbPassword,
                        onValueChange = { typed -> dbPasswords = dbPasswords + (step.stepIndex to typed) },
                        label = { Text(text = "Database password (sirf is call ke liye)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(text = "Ye password sirf memory me rahega, Room me kabhi save nahi hoga; app restart par dobara dena hoga. Amend ke baad step PENDING hoga — phir Resume/Chalao dabao.")
                    Row {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    if (dbPassword.isBlank()) {
                                        message = "Database password khali hai — pehle password likho."
                                    } else {
                                        val amended = engine.amendPlan(
                                            plan.planId,
                                            inputChanges = mapOf(step.stepIndex to mapOf("dbPassword" to dbPassword)),
                                        )
                                        if (amended.isSuccess) {
                                            dbPasswords = dbPasswords - step.stepIndex
                                            plans = engine.listPlans()
                                            message = "Database password transient diya gaya — Room me save nahi hua. Ab Resume/Chalao dabao."
                                        } else {
                                            message = "Amend nahi hua: ${amended.exceptionOrNull()?.message ?: "unknown"}"
                                        }
                                    }
                                }
                            }
                        ) {
                            Text(text = "Password dekar amend karo")
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
        // Phase 9, A2 (G12) — goal orchestrator: ek goal ke
        // INDEPENDENT sub-goals alag-alag sub-plans me chalte
        // hain (har sub-plan apna planId, apne steps, apne
        // stops). Dependent kaam yahan mat todo — wo ek hi
        // single plan me rahega (upar wala 'Plan banao').
        // Fail = isolate (siblings chalte rahenge), goal roko =
        // sab rukenge, approval per-sub-plan alag hai.
        if (orchestrator != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Goal orchestrator (G12) — goal ko sub-plans me chalao")
            Text(text = "Sirf independent sub-goals yahan do (ek-doosre par nirbhar kaam nahi). Max 5 sub-plans; usse zyada hon to single plan banao.")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = orchGoal,
                onValueChange = { newGoal -> orchGoal = newGoal },
                label = { Text(text = "Goal (poora kaam, ek line)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = orchSubs,
                onValueChange = { newSubs -> orchSubs = newSubs },
                label = { Text(text = "Sub-goals (comma se alag karo, max 5)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(
                    onClick = {
                        val subs = orchSubs.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        when (val res = orchestrator.createGoal(orchGoal.trim(), subs)) {
                            is GoalCreation.Created -> {
                                message = "Goal ban gaya (${res.run.subPlans.size} sub-plans) — ab 'Goal chalao' dabao."
                            }
                            is GoalCreation.NeedsSinglePlan -> {
                                message = res.reason
                            }
                            is GoalCreation.Invalid -> {
                                message = "Goal nahi bana: ${res.reason}"
                            }
                        }
                    }
                ) {
                    Text(text = "Goal banao")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            for (run in goals) {
                Text(text = "Goal: ${run.goal} [${run.status}]")
                Text(text = run.report)
                for (sub in run.subPlans) {
                    val planPart = if (sub.planId.isNotBlank()) " [plan ${sub.planId}]" else ""
                    Text(text = "- ${sub.subGoal} [${sub.status}]$planPart")
                    if (sub.detail.isNotBlank()) {
                        Text(text = "   ${sub.detail}")
                    }
                    // R-A2-11(iv): ruka hua (PAUSED) goal par approval offer hi nahi.
                    if (sub.status == GoalSubStatus.WAITING_APPROVAL && sub.planId.isNotBlank() &&
                        run.status != GoalStatus.PAUSED
                    ) {
                        val apStep = orchApproveSteps[sub.planId]
                        if (apStep == null) {
                            Row {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val p = engine.loadPlan(sub.planId)
                                            val st = p?.steps?.firstOrNull {
                                                it.status == StepStatus.PENDING && (it.spend || it.irreversible)
                                            }
                                            if (st != null) {
                                                orchApproveSteps = orchApproveSteps + (sub.planId to st.stepIndex)
                                            } else {
                                                message = "Is sub-plan ka ruka hua step spend/irreversible approval wala nahi lag raha — plan kholo aur Resume dekho."
                                            }
                                        }
                                    }
                                ) {
                                    Text(text = "Approval step dhoondo")
                                }
                            }
                        } else {
                            Row {
                                OutlinedButton(
                                    onClick = {
                                        if (orchestrator.approveSubPlan(run.goalId, sub.planId, apStep)) {
                                            orchApproveSteps = orchApproveSteps - sub.planId
                                            message = "Sub-plan approval di gayi — sirf ye sub-plan aage badhega."
                                        } else {
                                            message = "Goal ruka hua hai — ruke hue goal ka sub-plan approval se nahi chalega."
                                        }
                                    }
                                ) {
                                    Text(text = "Confirm karke ye sub-plan aage badhao (step ${apStep + 1})")
                                }
                            }
                        }
                    }
                }
                Row {
                    Button(
                        onClick = {
                            if (!orchestrator.startGoal(run.goalId)) {
                                message = "Ye goal pehle se chal chuka hai (ya chal raha hai) — dobara chalana ho to naya goal banao."
                            } else {
                                message = "Goal chal raha hai — sub-plans ek-ek karke chalenge."
                            }
                        }
                    ) {
                        Text(text = "Goal chalao")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            orchestrator.stopGoal(run.goalId)
                            message = "Goal roka gaya — chal raha sub-plan rukega, baaki shuru nahi honge."
                        }
                    ) {
                        Text(text = "Goal roko")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            orchestrator.refreshGoal(run.goalId)
                        }
                    ) {
                        Text(text = "Refresh")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
