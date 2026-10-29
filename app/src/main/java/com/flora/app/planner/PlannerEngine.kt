package com.flora.app.planner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.flora.app.assistant.BrainClient
import com.flora.app.data.AutoLogStore
import com.flora.app.data.ChatMemoryStore
import com.flora.app.data.FloraPreferences
import com.flora.app.data.LicenceClient
import com.flora.app.data.ProjectStore
import com.flora.app.tools.BuilderFailureKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * B4 planner executor (F03/F06/F07/F08/F09/F10/F11/F16).
 *
 * Live execution foreground-service + coroutines se chalegi
 * (PlannerService) — WorkManager live executor NAHI hai, sirf
 * durable scheduling/resume guarantee (PlanResumeWorker, M13).
 * JobScheduler routine-alarm hai, executor nahi — usse koi
 * lena-dena nahi.
 *
 * Apna CoroutineScope pattern (lifecycle-runtime-ktx
 * DELIBERATELY EXCLUDED hai — v1.3 CI fail ke baad dobara
 * nahi lana): CoroutineScope(SupervisorJob() + IO).
 *
 * Stops (F10, unified): paisa / delete-irreversible /
 * login-2FA / genuine block. Messaging ask-first checkpoint
 * ALAG checkpoint hai — stops ki jagah nahi lega, aur B4 ke
 * toolset me message-send tool hai hi nahi, isliye wo checkpoint
 * FloraApp ke maujooda flow me barkaraar hai, yahan replace
 * nahi hua.
 *
 * Auto Log (F11): har step ka log planId/stepId/status ke
 * saath — AutoLogStore sirf add(text) hai, isliye format
 * "[plan:<id> step:<n>] <status> ..." yahan banaya gaya hai;
 * AutoLog ka storage/format nahi badla. Plan history Room me
 * rahegi (AutoLog 30-din prune hota hai, isliye history wahan
 * nahi).
 */
data class EngineState(
    val plan: StoredPlan?,
    val status: PlanStatus,
    val message: String,
    val promptCount: Int,
    val queueSize: Int,
    val running: Boolean,
)

class PlannerEngine(
    context: Context,
    private val prefs: FloraPreferences,
    licenceClient: LicenceClient,
    brain: BrainClient,
    projectStore: ProjectStore,
    private val autoLogStore: AutoLogStore,
    chatMemoryStore: ChatMemoryStore?,
    private val androidId: String,
) {
    private val appContext: Context = context.applicationContext
    private val repository = PlanRepository(appContext)
    private val tools = PlannerTools(prefs, projectStore)
    private val plannerBrain = PlannerBrain(prefs, licenceClient, brain, chatMemoryStore)

    /** Apna scope — lifecycleScope kabhi nahi (F03). */
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var runJob: Job? = null
    private sealed class QueueItem {
        data class Plan(val planId: String) : QueueItem()
        data class Goal(val goal: String) : QueueItem()
    }
    private val queue = ArrayDeque<QueueItem>()
    private val approvedSteps = mutableSetOf<String>()
    /** Transient inputs (jaise Supabase dbPassword) — sirf memory me, Room me kabhi nahi. */
    private val transientInputs = mutableMapOf<Int, Map<String, String>>()

    private val stateFlow = MutableStateFlow(
        EngineState(null, PlanStatus.DRAFT, "", 0, 0, false)
    )
    val state: StateFlow<EngineState> = stateFlow.asStateFlow()
    private val pendingGoalFlow = MutableStateFlow("")
    val pendingGoal: StateFlow<String> = pendingGoalFlow.asStateFlow()

    fun setPendingGoal(goal: String) {
        pendingGoalFlow.value = goal
    }

    suspend fun loadPlan(planId: String): StoredPlan? = repository.loadPlan(planId)

    suspend fun listPlans(): List<StoredPlan> = repository.listPlans()

    suspend fun interruptedPlans(): List<StoredPlan> = repository.interruptedPlans()

    /** Plan banao (brain se) + Room me save. Licence Blocked par koi AI call nahi (PlannerBrain me hook). */
    suspend fun createPlan(goal: String): Result<StoredPlan> {
        val deviceId = deviceId()
        return when (val gen = plannerBrain.generate(goal, deviceId)) {
            is PlannerBrain.Generation.Blocked -> Result.failure(IllegalStateException(gen.message))
            is PlannerBrain.Generation.Failed -> Result.failure(IllegalStateException(gen.reason))
            is PlannerBrain.Generation.Ok -> {
                val estimate = PlanEstimateCalc.forPlan(gen.draft.steps.size, gen.calls)
                val planId = UUID.randomUUID().toString().substring(0, 8)
                val saved = repository.saveNewPlan(planId, gen.draft, estimate)
                log(planId, -1, "plan bana (v1, source=${gen.source}, calls=${gen.calls})")
                stateFlow.value = EngineState(saved, PlanStatus.DRAFT, "Plan taiyaar hai.", 0, queue.size, false)
                Result.success(saved)
            }
        }
    }

    private suspend fun deviceId(): String {
        return try {
            prefs.deviceId(androidId)
        } catch (e: Exception) {
            androidId
        }
    }

    /** M2/G7 queue: plan chal raha ho to nayi intention queue me. */
    fun startPlan(planId: String, replaceCurrent: Boolean = false) {
        if (runJob?.isActive == true) {
            if (replaceCurrent) {
                engineScope.launch {
                    stopCurrent()
                    launchRun(planId)
                }
            } else {
                engineScope.launch {
                    val plan = repository.loadPlan(planId)
                    if (plan != null) {
                        queue += QueueItem.Plan(planId)
                        log(planId, -1, "queue me gaya (pehla plan chal raha hai, G7)")
                        refreshState(planId, PlanStatus.PAUSED, "Queue me hai — pehla plan khatm hone ke baad shuru hoga.")
                    }
                }
            }
            return
        }
        launchRun(planId)
    }

    fun queueGoal(goal: String) {
        engineScope.launch {
            if (runJob?.isActive == true) {
                queue += QueueItem.Goal(goal)
                refreshState(null, PlanStatus.PAUSED, "Goal queue me gaya.")
            } else {
                val created = createPlan(goal)
                if (created.isSuccess) {
                    launchRun(created.getOrNull()?.planId ?: return@launch)
                }
            }
        }
    }

    private fun launchRun(planId: String) {
        runJob = engineScope.launch {
            PlannerService.start(appContext)
            runLoop(planId)
            val next = queue.removeFirstOrNull()
            when (next) {
                is QueueItem.Plan -> launchRun(next.planId)
                is QueueItem.Goal -> {
                    val created = createPlan(next.goal)
                    if (created.isSuccess) launchRun(created.getOrNull()?.planId ?: return@launch)
                }
                null -> PlannerService.stop(appContext)
            }
        }
    }

    /** User interrupt "ruko" (F09) — safe stop + state save. */
    fun stopPlan(planId: String, reason: String = "user ne roka") {
        engineScope.launch {
            stopCurrent()
            repository.updatePlanMeta(planId, status = PlanStatus.PAUSED, lastError = reason)
            log(planId, -1, "ruka: $reason (state save ho gaya)")
            PlanNotifier.showProgress(appContext, planId, "Plan ruka hua hai", 0, 0)
            PlannerService.stop(appContext)
            refreshState(planId, PlanStatus.PAUSED, "Plan ruka hua hai: $reason")
        }
    }

    private suspend fun stopCurrent() {
        runJob?.cancelAndJoin()
        runJob = null
    }

    /** Stop approval ke baad resume — approved step par stop dobara nahi lagega. */
    fun approveAndResume(planId: String, stepIndex: Int) {
        approvedSteps += "$planId:$stepIndex"
        engineScope.launch {
            repository.loadPlan(planId) ?: return@launch
            repository.updatePlanMeta(planId, status = PlanStatus.RUNNING, lastError = "")
            launchRun(planId)
        }
    }

    fun resumePlan(planId: String) {
        if (runJob?.isActive == true) return
        launchRun(planId)
    }

    /** Macro adapter stop ke baad user confirm — UI step ho gaya maan kar aage badho. */
    fun confirmMacroStep(planId: String, stepIndex: Int) {
        engineScope.launch {
            val plan = repository.loadPlan(planId) ?: return@launch
            val step = plan.steps.firstOrNull { it.stepIndex == stepIndex } ?: return@launch
            repository.updateStep(
                step.copy(
                    status = StepStatus.VERIFIED,
                    resultText = "Macro user-confirm se poora mana gaya.",
                    sideEffectId = "macro-confirmed-by-user",
                    verifiedAtMs = System.currentTimeMillis(),
                ),
                planId,
            )
            log(planId, stepIndex, "macro user-confirm se VERIFIED")
            launchRun(planId)
        }
    }

    /** F09/M5 amend: skip / input-change / reorder — naya version, poora plan dobara shuru nahi. */
    suspend fun amendPlan(
        planId: String,
        skipIndices: Set<Int> = emptySet(),
        inputChanges: Map<Int, Map<String, String>> = emptyMap(),
        newOrder: List<Int>? = null,
    ): Result<StoredPlan> {
        val plan = repository.loadPlan(planId) ?: return Result.failure(IllegalStateException("plan nahi mila"))
        if (runJob?.isActive == true) {
            return Result.failure(IllegalStateException("plan chal raha hai — pehle roko, phir badlo"))
        }
        var steps = plan.steps
        // Transient keys (password-ish) Room me kabhi nahi — sirf memory map me.
        for ((index, changes) in inputChanges) {
            val transientPart = changes.filterKeys { key ->
                key.lowercase().contains("password") || key.lowercase().contains("db_pass") ||
                    key.lowercase().contains("secret") || key.lowercase().contains("token")
            }
            if (transientPart.isNotEmpty()) transientInputs[index] = transientPart
            val storedPart = changes.filterKeys { key -> key !in transientPart.keys }
            steps = steps.map { step ->
                if (step.stepIndex == index) step.copy(input = step.input + storedPart) else step
            }
        }
        if (skipIndices.isNotEmpty()) {
            steps = steps.map { step ->
                if (step.stepIndex in skipIndices && step.status == StepStatus.PENDING) {
                    step.copy(status = StepStatus.SKIPPED)
                } else {
                    step
                }
            }
        }
        if (newOrder != null) {
            val byIndex = steps.associateBy { it.stepIndex }
            if (newOrder.toSet() != steps.map { it.stepIndex }.toSet()) {
                return Result.failure(IllegalStateException("reorder me saare step index hone chahiye"))
            }
            val remap = mutableMapOf<Int, Int>()
            for ((newPos, oldIndex) in newOrder.withIndex()) remap[oldIndex] = newPos
            val reordered = newOrder.mapIndexed { newPos, oldIndex ->
                val step = byIndex[oldIndex] ?: return Result.failure(IllegalStateException("step nahi mila"))
                step.copy(
                    stepIndex = newPos,
                    dependsOn = step.dependsOn.mapNotNull { remap[it] }.filter { it < newPos },
                )
            }
            steps = reordered
        }
        for (step in steps) repository.updateStep(step, planId)
        val newVersion = plan.version + 1
        repository.updatePlanMeta(
            planId, version = newVersion,
            historyNote = "v$newVersion: amend (skip=$skipIndices, reorder=${newOrder != null})",
        )
        log(planId, -1, "plan amend hua -> v$newVersion")
        val updated = repository.loadPlan(planId) ?: return Result.failure(IllegalStateException("plan load nahi hua"))
        stateFlow.value = EngineState(updated, updated.status, "Plan badal gaya (v$newVersion).", updated.promptCount, queue.size, false)
        return Result.success(updated)
    }

    /** F08/M7 compensation — Flora khud delete nahi karegi (koi delete API nahi, delete = stop #2). */
    suspend fun compensationReport(planId: String): List<String> {
        val plan = repository.loadPlan(planId) ?: return listOf("plan nahi mila")
        val done = plan.steps.filter { it.status == StepStatus.VERIFIED && it.sideEffectId.isNotBlank() }
        if (done.isEmpty()) return listOf("koi side-effect poora nahi hua — compensation ki zaroorat nahi")
        val out = mutableListOf<String>()
        for (step in done.asReversed()) {
            out += "Undo (manual, user-confirm ke baad hi): ${step.title} — ${step.sideEffectPreview} (id: ${step.sideEffectId})"
        }
        out += "Note: delete/irreversible undo sirf tumhare ek baar haan bolne par, Flora chupchaap kuch delete nahi karegi."
        return out
    }

    private suspend fun refreshState(planId: String?, status: PlanStatus, message: String) {
        val plan = if (planId != null) repository.loadPlan(planId) else stateFlow.value.plan
        stateFlow.value = EngineState(plan, status, message, plan?.promptCount ?: 0, queue.size, runJob?.isActive == true)
    }

    private suspend fun log(planId: String, stepIndex: Int, text: String) {
        try {
            autoLogStore.add("[plan:$planId step:$stepIndex] $text")
        } catch (e: Exception) {
        }
    }

    private suspend fun bumpPrompt(planId: String) {
        val plan = repository.loadPlan(planId) ?: return
        repository.updatePlanMeta(planId, promptCount = plan.promptCount + 1)
    }

    private sealed class StepRunResult {
        data class Done(val step: StoredStep) : StepRunResult()
        data class Failed(val reason: String, val kind: BuilderFailureKind) : StepRunResult()
        data class Stopped(val stop: PlanStop, val reason: String) : StepRunResult()
    }

    private suspend fun runLoop(planId: String) {
        var replansUsed = 0
        repository.updatePlanMeta(planId, status = PlanStatus.RUNNING, lastError = "")
        while (true) {
            val plan = repository.loadPlan(planId) ?: return
            // M15: RUNNING step — sideEffectId hai to side-effect ho chuka (VERIFIED),
            // nahi hai to blind retry nahi, PENDING par wapas (phir idempotency pre-check).
            for (step in plan.steps) {
                if (step.status != StepStatus.RUNNING) continue
                if (step.sideEffectId.isNotBlank()) {
                    repository.updateStep(step.copy(status = StepStatus.VERIFIED, verifiedAtMs = System.currentTimeMillis()), planId)
                } else {
                    repository.updateStep(step.copy(status = StepStatus.PENDING), planId)
                }
            }
            val fresh = repository.loadPlan(planId) ?: return
            val pending = fresh.steps.filter { it.status == StepStatus.PENDING }
            if (pending.isEmpty()) {
                val allDone = fresh.steps.all { it.status == StepStatus.VERIFIED || it.status == StepStatus.SKIPPED }
                val finalStatus = if (allDone) PlanStatus.DONE else PlanStatus.BLOCKED
                repository.updatePlanMeta(planId, status = finalStatus)
                log(planId, -1, "plan khatm: $finalStatus")
                PlanNotifier.showProgress(appContext, planId, "Plan khatm: $finalStatus", 0, 0, ongoing = false)
                refreshState(planId, finalStatus, if (allDone) "Plan poora ho gaya." else "Plan atka hua hai — baaki steps dekho.")
                return
            }
            val levels = levelsForPending(pending)
            var needReplan: String? = null
            var stopped: StepRunResult.Stopped? = null
            var failed: StepRunResult.Failed? = null
            for (level in levels) {
                if (stopped != null || failed != null) break
                val levelSteps = level.mapNotNull { pos -> pending.getOrNull(pos) }
                val results: List<StepRunResult> = coroutineScope {
                    levelSteps.map { step ->
                        async { runOneStep(planId, step) }
                    }.map { it.await() }
                }
                for (result in results) {
                    when (result) {
                        is StepRunResult.Done -> {}
                        is StepRunResult.Stopped -> stopped = result
                        is StepRunResult.Failed -> failed = result
                    }
                }
                val doneCount = (repository.loadPlan(planId)?.steps?.count { it.status == StepStatus.VERIFIED } ?: 0)
                val total = fresh.steps.size
                PlanNotifier.showProgress(appContext, planId, "Plan chal raha hai", doneCount, total)
            }
            if (stopped != null) {
                val stop = stopped as StepRunResult.Stopped
                bumpPrompt(planId)
                repository.updatePlanMeta(planId, status = PlanStatus.PAUSED, lastError = stop.reason)
                log(planId, -1, "stop (${stop.stop}): ${stop.reason}")
                PlanNotifier.showProgress(appContext, planId, "Plan ruka: ${stop.reason}", 0, 0)
                refreshState(planId, PlanStatus.PAUSED, "Ruka: ${stop.reason}")
                return
            }
            if (failed != null) {
                val fail = failed as StepRunResult.Failed
                if (fail.kind == BuilderFailureKind.AUTH_EXPIRED || fail.kind == BuilderFailureKind.SCOPE_SHORT) {
                    bumpPrompt(planId)
                    repository.updatePlanMeta(planId, status = PlanStatus.BLOCKED, lastError = fail.reason)
                    log(planId, -1, "genuine block (auth/scope): ${fail.reason}")
                    refreshState(planId, PlanStatus.BLOCKED, "Block: ${fail.reason} — ConnectServices se dobara connect karo.")
                    return
                }
                if (replansUsed >= 2) {
                    repository.updatePlanMeta(planId, status = PlanStatus.FAILED, lastError = fail.reason)
                    log(planId, -1, "fail (replan khatm): ${fail.reason}")
                    refreshState(planId, PlanStatus.FAILED, "Fail ho gaya: ${fail.reason}")
                    return
                }
                replansUsed += 1
                val deviceId = deviceId()
                val remainingGoal = "Baaki kaam poora karo. Original goal: ${fresh.goal}. Fail wajah: ${fail.reason}."
                when (val gen = plannerBrain.generate(remainingGoal, deviceId, fail.reason)) {
                    is PlannerBrain.Generation.Blocked -> {
                        repository.updatePlanMeta(planId, status = PlanStatus.BLOCKED, lastError = gen.message)
                        refreshState(planId, PlanStatus.BLOCKED, gen.message)
                        return
                    }
                    is PlannerBrain.Generation.Failed -> {
                        repository.updatePlanMeta(planId, status = PlanStatus.FAILED, lastError = gen.reason)
                        refreshState(planId, PlanStatus.FAILED, gen.reason)
                        return
                    }
                    is PlannerBrain.Generation.Ok -> {
                        val newVersion = fresh.version + 1
                        repository.replacePendingSteps(planId, gen.draft, newVersion, "v$newVersion: replan (wajah: ${fail.reason})")
                        log(planId, -1, "replan hua -> v$newVersion (source=${gen.source})")
                    }
                }
            }
        }
    }

    /** Pending steps ke levels — verified deps satisfied maane jate hain. */
    private fun levelsForPending(pending: List<StoredStep>): List<List<Int>> {
        val pendingIndices = pending.map { it.stepIndex }.toSet()
        val depends = pending.map { step ->
            step.dependsOn.filter { it in pendingIndices }
                .map { dep -> pending.indexOfFirst { it.stepIndex == dep } }
        }
        return PlanValidator.levels(pending.size, depends)
    }

    private suspend fun runOneStep(planId: String, step: StoredStep): StepRunResult {
        // M1 idempotency pre-check: VERIFIED ya sideEffectId wala step dobara kabhi nahi.
        if (step.status == StepStatus.VERIFIED || step.sideEffectId.isNotBlank()) {
            return StepRunResult.Done(step)
        }
        // F10 unified stops — approval ke bina spend/irreversible step nahi chalega.
        if (step.spend && "$planId:${step.stepIndex}" !in approvedSteps) {
            return StepRunResult.Stopped(PlanStop.MONEY, "Step '${step.title}' par paisa kharch ho sakta hai — approve karo tabhi aage badhega (stop #1).")
        }
        if (step.irreversible && "$planId:${step.stepIndex}" !in approvedSteps) {
            return StepRunResult.Stopped(PlanStop.DELETE_IRREVERSIBLE, "Step '${step.title}' irreversible lag raha hai — ek baar confirm karo tabhi aage badhega (stop #2).")
        }
        repository.updateStep(step.copy(status = StepStatus.RUNNING), planId)
        log(planId, step.stepIndex, "shuru: ${step.title} [${step.tool}]")
        var attempts = 0
        var lastKind = BuilderFailureKind.SERVER
        var lastDetail = "unknown"
        while (attempts < 3) {
            attempts += 1
            val transient = transientInputs[step.stepIndex] ?: emptyMap()
            val outcome: PlannerTools.StepOutcome? = withTimeoutOrNull(120_000L) {
                try {
                    tools.execute(step, transient)
                } catch (e: Exception) {
                    PlannerTools.StepOutcome.Failure(BuilderFailureKind.NETWORK, "exception")
                }
            }
            if (outcome == null) {
                // F09 step timeout — ek retry, phir replan/block path.
                if (attempts < 2) {
                    delay(2000L)
                    continue
                }
                repository.updateStep(step.copy(status = StepStatus.FAILED, attempts = attempts, resultText = "timeout (120s)"), planId)
                return StepRunResult.Failed("step timeout: ${step.title}", BuilderFailureKind.NETWORK)
            }
            when (outcome) {
                is PlannerTools.StepOutcome.Ok -> {
                    // Side-effect preview ke baad bhi: successCheck = client Ok + detail.
                    val done = step.copy(
                        status = StepStatus.VERIFIED,
                        resultText = outcome.detail,
                        sideEffectId = outcome.sideEffectId,
                        verifiedAtMs = System.currentTimeMillis(),
                        attempts = attempts,
                    )
                    repository.updateStep(done, planId)
                    if (step.stepIndex in transientInputs) transientInputs.remove(step.stepIndex)
                    log(planId, step.stepIndex, "VERIFIED: ${outcome.detail}")
                    return StepRunResult.Done(done)
                }
                is PlannerTools.StepOutcome.Blocked -> {
                    repository.updateStep(step.copy(status = StepStatus.BLOCKED, attempts = attempts, resultText = outcome.reason), planId)
                    log(planId, step.stepIndex, "BLOCKED: ${outcome.reason}")
                    return StepRunResult.Stopped(outcome.stop, outcome.reason)
                }
                is PlannerTools.StepOutcome.AdapterQueued -> {
                    repository.updateStep(step.copy(status = StepStatus.BLOCKED, attempts = attempts, resultText = outcome.note), planId)
                    log(planId, step.stepIndex, "macro adapter queued: ${outcome.note}")
                    return StepRunResult.Stopped(PlanStop.GENUINE_BLOCK, outcome.note)
                }
                is PlannerTools.StepOutcome.Failure -> {
                    lastKind = outcome.kind
                    lastDetail = outcome.detail
                    // M11 backoff: rate-limit par exponential backoff + retry.
                    if (outcome.kind == BuilderFailureKind.RATE_LIMITED && attempts < 3) {
                        delay(2000L * attempts)
                        continue
                    }
                    repository.updateStep(step.copy(status = StepStatus.FAILED, attempts = attempts, resultText = outcome.detail), planId)
                    log(planId, step.stepIndex, "FAILED (${outcome.kind}): ${outcome.detail}")
                    return StepRunResult.Failed("${step.title}: ${outcome.detail}", outcome.kind)
                }
            }
        }
        repository.updateStep(step.copy(status = StepStatus.FAILED, attempts = attempts, resultText = lastDetail), planId)
        return StepRunResult.Failed(lastDetail, lastKind)
    }
}

/** F11 notification progress wiring — AutoModeNotifier wale pattern par. */
object PlanNotifier {
    private const val CHANNEL_ID = "flora_plan"
    private const val NOTIF_ID = 2004

    fun showProgress(context: Context, planId: String, text: String, done: Int, total: Int, ongoing: Boolean = true) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Flora Plan", NotificationManager.IMPORTANCE_LOW)
                )
            }
            val content = if (total > 0) "$text ($done/$total steps)" else text
            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Flora Plan $planId")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(ongoing)
                .build()
            nm.notify(NOTIF_ID, notif)
        } catch (e: Exception) {
        }
    }

    fun cancel(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIF_ID)
        } catch (e: Exception) {
        }
    }
}

/** Manual-DI factory (F15 fallback pattern — Hilt nahi, constructor DI hi hai). */
object PlannerEngineFactory {
    @Volatile
    private var instance: PlannerEngine? = null

    fun get(
        context: Context,
        prefs: FloraPreferences,
        licenceClient: LicenceClient,
        brain: BrainClient,
        projectStore: ProjectStore,
        autoLogStore: AutoLogStore,
        chatMemoryStore: ChatMemoryStore?,
        androidId: String,
    ): PlannerEngine {
        val existing = instance
        if (existing != null) return existing
        synchronized(this) {
            val again = instance
            if (again != null) return again
            val created = PlannerEngine(
                context, prefs, licenceClient, brain, projectStore,
                autoLogStore, chatMemoryStore, androidId,
            )
            instance = created
            return created
        }
    }
}
