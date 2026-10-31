package com.flora.app.planner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.flora.app.assistant.BrainClient
import com.flora.app.automation.MacroJson
import com.flora.app.data.AutoLogStore
import com.flora.app.data.ChatMemoryStore
import com.flora.app.data.FloraPreferences
import com.flora.app.data.LicenceClient
import com.flora.app.data.MacroStore
import com.flora.app.data.ProjectMemoryStore
import com.flora.app.data.ProjectStore
import com.flora.app.data.StoredMacro
import com.flora.app.tools.BuilderFailureKind
import timber.log.Timber
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
import kotlinx.coroutines.flow.first
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

    /**
     * B4-DOCTOR (DOC-P1-8): step-run ka failure kind — private
     * StepRunResult se bahar nikalne ka public accessor. Kind
     * Room step row me persist NAHI hota (schema change se
     * bacha gaya), isliye ye map session-scoped hai: runOneStep
     * ke Failed branch me bharta hai, VERIFIED par saaf hota
     * hai. Durable reader failedSteps() hai (Room se).
     */
    private val lastFailureKindByStep = mutableMapOf<String, BuilderFailureKind>()

    fun lastFailureKind(planId: String, stepIndex: Int): BuilderFailureKind? =
        lastFailureKindByStep["$planId:$stepIndex"]

    init {
        // DOC-P0-2 registry: VERIFIED create-steps se allowed targets.
        tools.allowedTargetsProvider = { allowedRemoteTargets() }
    }

    private suspend fun allowedRemoteTargets(): Set<String> {
        val out = mutableSetOf<String>()
        return try {
            for (plan in repository.listPlans()) {
                for (step in plan.steps) {
                    if (step.status != StepStatus.VERIFIED) continue
                    if (step.tool == PlanTool.GITHUB_CREATE_REPO && step.sideEffectId.isNotBlank()) {
                        out += "github:${step.sideEffectId}"
                    }
                    if (step.tool == PlanTool.VERCEL_CREATE_PROJECT) {
                        val createdName = step.input["name"] ?: ""
                        if (createdName.isNotBlank()) out += "vercel:$createdName"
                    }
                }
            }
            out
        } catch (e: Exception) {
            out
        }
    }
    private val plannerBrain = PlannerBrain(prefs, licenceClient, brain, chatMemoryStore)

    /** Apna scope — lifecycleScope kabhi nahi (F03). */
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var runJob: Job? = null

    /**
     * B5 stores — sab yahan (engine) bante hain, PlannerEngineFactory
     * shared instance se ek hi baar. Order: appContext / prefs /
     * licenceClient / androidId / chatMemoryStore / autoLogStore
     * sab constructor se pehle maujood hain, isliye init order safe
     * hai. Koi B5 store PlannerTools ya PlannerBrain ke andar
     * dobara nahi banta (single construction site barkaraar).
     */
    private val projectMemoryStore = ProjectMemoryStore(appContext)
    private val learnedEngine = LearnedEngine(appContext)
    private val aiBuildQuota = AiBuildQuota(appContext, prefs, licenceClient, androidId)
    private val selfMapClient = SelfMapClient(appContext)
    // B5-04 decision: MemoryAppSearchIndex removed hai -
    // unifiedSearch store-scan-only hai (koi AppSearch dep,
    // index build, ya stack bump nahi).
    private val unifiedSearch = UnifiedSearch(
        chatMemoryStore, projectMemoryStore, autoLogStore,
    )

    /** Learned shortcut se bane plans — in par dobara learned record nahi hoga (feedback loop nahi). */
    private val learnedCreatedPlanIds = mutableSetOf<String>()

    init {
        // B5-07 audit sink + B5-06 quota gates — tools ko engine hi deta hai.
        tools.auditSink = { text ->
            try {
                autoLogStore.add(text)
            } catch (e: Exception) {
            }
        }
        tools.buildQuotaGate = { aiBuildQuota.buildGateMessage() }
        tools.projectQuotaGate = { count -> aiBuildQuota.projectGateMessage(count) }
    }
    private sealed class QueueItem {
        data class Plan(val planId: String) : QueueItem()
        data class Goal(val goal: String) : QueueItem()
    }
    private val queue = ArrayDeque<QueueItem>()
    private val approvedSteps = mutableSetOf<String>()
    /** Transient inputs (jaise Supabase dbPassword) — sirf memory me, Room me kabhi nahi. */
    private val transientInputs = mutableMapOf<Int, Map<String, String>>()
    /** Planner-macro delivery ka waqt (memory) — restart par resultText ke marker se recover hota hai. */
    private val macroQueuedAtMs = mutableMapOf<String, Long>()

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

    /** B4-DOCTOR step-results reader (DOC-P1-8): FAILED + BLOCKED steps, Room se. */
    suspend fun failedSteps(planId: String): List<StoredStep> {
        val plan = repository.loadPlan(planId) ?: return emptyList()
        return plan.steps.filter { it.status == StepStatus.FAILED || it.status == StepStatus.BLOCKED }
    }

    /** B4-DOCTOR plan <-> project link (DOC-P1-8, Room link table). */
    suspend fun linkPlanToProject(planId: String, projectId: String) {
        repository.linkPlanToProject(planId, projectId)
    }

    suspend fun linkedProjectForPlan(planId: String): String? =
        repository.linkedProjectForPlan(planId)

    /**
     * B4-DOCTOR log (DOC-P2-5): "[doctor:<projectId> ...]" prefix
     * (planner "[plan:<id> step:<n>]" pattern par) + durable proof
     * Room doctor_events me — sirf AutoLog (30-din prune + 500 cap)
     * par nirbhar nahi. Text me kabhi file content/secret nahi.
     */
    suspend fun logDoctorEvent(projectId: String, text: String) {
        try {
            repository.insertDoctorEvent(projectId, text)
        } catch (e: Exception) {
        }
        try {
            autoLogStore.add("[doctor:$projectId] $text")
        } catch (e: Exception) {
        }
    }

    suspend fun doctorEvents(projectId: String): List<String> {
        return try {
            repository.doctorEvents(projectId).map { it.text }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun interruptedPlans(): List<StoredPlan> = repository.interruptedPlans()

    /** Plan banao (brain se) + Room me save. Licence Blocked par koi AI call nahi (PlannerBrain me hook). */
    suspend fun createPlan(goal: String): Result<StoredPlan> {
        // B5-05 learned shortcut — repeat kaam par ZERO AI call.
        // Sirf SAFE tools (NOTE / MACRO_UI_STEP): NOTE ka koi
        // side-effect nahi; MACRO sirf adapter-queued hota hai
        // (Routines confirm ke bina execute kabhi nahi). Side-effect
        // tools ka pattern kabhi offline nahi chalta (AI chain hi).
        // SD-P1 Batch 4 (8.14): skills toggle OFF ho to learned
        // shortcut skip — AI raasta. Default ON hai; prefs fail
        // ho to fail-open ON (pehle jaisa vyavhaar).
        val skillsOn = try { prefs.sd8SkillsEnabled() } catch (e: Exception) { true }
        if (skillsOn) when (val learned = learnedEngine.decide(goal, null)) {
            is LearnedEngine.Route.Offline -> {
                val pattern = learned.pattern
                val draft = PlanDraft(
                    goal = goal,
                    steps = listOf(
                        PlanStepDraft(
                            title = pattern.actionLabel.ifBlank { goal },
                            tool = pattern.tool,
                            input = pattern.input,
                            successCheck = "Learned pattern se step poora hua",
                            fallback = "Dobara AI se plan banao",
                            sideEffectPreview = if (pattern.tool == PlanTool.MACRO_UI_STEP.name) {
                                "Macro adapter me jayega (Routines confirm ke baad hi chalega)"
                            } else {
                                ""
                            },
                        )
                    ),
                )
                val planId = UUID.randomUUID().toString()
                val saved = repository.saveNewPlan(planId, draft, PlanEstimateCalc.forPlan(1, 0))
                learnedCreatedPlanIds += planId
                log(planId, -1, "learned pattern se bina AI call ke plan bana (tool=${pattern.tool})")
                stateFlow.value = EngineState(saved, PlanStatus.DRAFT, "Plan taiyaar hai (learned pattern, bina AI call).", 0, queue.size, false)
                return Result.success(saved)
            }
            else -> {}
        }
        val deviceId = deviceId()
        return when (val gen = plannerBrain.generate(goal, deviceId)) {
            is PlannerBrain.Generation.Blocked -> Result.failure(IllegalStateException(gen.message))
            is PlannerBrain.Generation.Failed -> Result.failure(IllegalStateException(gen.reason))
            is PlannerBrain.Generation.Ok -> {
                val estimate = PlanEstimateCalc.forPlan(gen.draft.steps.size, gen.calls)
                val planId = UUID.randomUUID().toString()
                val saved = repository.saveNewPlan(planId, gen.draft, estimate)
                // B5-06: plan-generation calls ka count sirf hisaab
                // ke liye — builds me KABHI nahi ginega (CEO faisla c).
                try {
                    aiBuildQuota.recordPlanGeneration(gen.calls)
                } catch (e: Exception) {
                }
                log(planId, -1, "plan bana (v1, source=${gen.source}, calls=${gen.calls}; plan calls builds me nahi gine)")
                stateFlow.value = EngineState(saved, PlanStatus.DRAFT, "Plan taiyaar hai.", 0, queue.size, false)
                Result.success(saved)
            }
        }
    }

    /** B5 public APIs — PlannerScreen / BuilderScreen / chat se call honge (engine shared instance se). */
    suspend fun searchMemory(query: String): List<UnifiedSearchResult> {
        return try {
            unifiedSearch.search(query)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun selfMap(): SelfMap? {
        return try {
            selfMapClient.load()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun aiBuildStatus(): String {
        return try {
            aiBuildQuota.statusText()
        } catch (e: Exception) {
            "AI build status nahi mila."
        }
    }

    suspend fun projectGateMessage(count: Int): String? {
        return try {
            aiBuildQuota.projectGateMessage(count)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun learnedRoute(query: String): LearnedEngine.Route =
        learnedEngine.decide(query, null)

    /** B5-05: plan DONE hone par safe single-step plan se seekho (Macro/Routine/Job se kabhi nahi). */
    private suspend fun maybeRecordLearned(plan: StoredPlan) {
        if (plan.planId in learnedCreatedPlanIds) return
        if (plan.steps.size != 1) return
        val step = plan.steps[0]
        if (step.status != StepStatus.VERIFIED) return
        if (step.tool != PlanTool.NOTE && step.tool != PlanTool.MACRO_UI_STEP) return
        try {
            learnedEngine.record(plan.goal, step.tool.name, step.title, step.input, fromExplicitAutomation = false)
        } catch (e: Exception) {
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

    // A3 (a, R-A3-A2): queueGoal HATA DIYA GAYA (dead helper —
    // koi caller nahi tha; caller jodna naya feature hota, isliye
    // wiring nahi ki gayi). QueueItem.Goal + launchRun ka Goal
    // branch waisa hi hai (sealed when exhaustive rahe).
    private fun launchRun(planId: String) {
        runJob = engineScope.launch {
            // A3 (g): start ka result ab engine tak pahunchta hai
            // — fail ho to Auto Log me signal, runLoop phir bhi
            // chalega (service bina foreground ke bhi plan rukega
            // nahi; engine ab andha nahi hai).
            if (!PlannerService.start(appContext)) {
                log(planId, -1, "PlannerService start fail — service foreground nahi hui, plan phir bhi chal raha hai")
            }
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

    /**
     * B4 follow-up P1-1 — planner-macro ka deterministic naam.
     * Routines ke MacroStore me isi naam se save hota hai, aur
     * FloraApp ka maujooda macro runner (runMacroFrom) Done par
     * AutoLog me "Macro: <naam> -> ho gaya" likhta hai — wahi
     * runner log confirm ka proof hai, user ka kehna nahi.
     */
    fun plannerMacroName(planId: String, stepIndex: Int): String =
        "planner-$planId-step$stepIndex"

    /**
     * B4 follow-up P1-1 — macro step ka confirm, SIRF runner-log proof par.
     * Purana confirmMacroStep (bina proof ke VERIFIED) HATA DIYA GAYA hai:
     * is function ke bina proof ke VERIFIED kabhi nahi hoga — proof na mile to
     * step BLOCKED hi rehta hai aur failure wapas milta hai (Screen dikhata hai).
     * Proof = AutoLog me isi macro naam ka Done entry, delivery waqt ke baad ka.
     * Delivery waqt memory map se, ya restart ke baad step ke resultText me
     * maujood [macro-queued-at:<ms>] marker se milta hai.
     */
    suspend fun confirmMacroExecuted(planId: String, stepIndex: Int): Result<String> {
        val plan = repository.loadPlan(planId) ?: return Result.failure(IllegalStateException("plan nahi mila"))
        val step = plan.steps.firstOrNull { it.stepIndex == stepIndex }
            ?: return Result.failure(IllegalStateException("step nahi mila"))
        if (step.status != StepStatus.BLOCKED || step.tool != PlanTool.MACRO_UI_STEP) {
            return Result.failure(IllegalStateException("ye step BLOCKED macro step nahi hai — is button se VERIFIED nahi hoga"))
        }
        val macroName = plannerMacroName(planId, stepIndex)
        val queuedAt = macroQueuedAtMs["$planId:$stepIndex"] ?: queuedAtFromResult(step.resultText)
        if (queuedAt == null) {
            return Result.failure(IllegalStateException("macro delivery ka waqt nahi mila — macro Routines se dobara chalao, phir confirm dabao"))
        }
        val proofFound = try {
            autoLogStore.items.first().any { (text, ts) ->
                text == "Macro: $macroName -> ho gaya" && ts >= queuedAt
            }
        } catch (e: Exception) {
            false
        }
        if (!proofFound) {
            log(planId, stepIndex, "macro confirm roka gaya: runner log nahi mila ($macroName)")
            return Result.failure(IllegalStateException("Macro runner log nahi mila — pehle Routines > Mere macros se '$macroName' Chalao + confirm se chalao, phir ye button dabao"))
        }
        repository.updateStep(
            step.copy(
                status = StepStatus.VERIFIED,
                resultText = "Macro runner log se VERIFIED (Routines runner Done: $macroName).",
                sideEffectId = "macro-run-log:$macroName",
                verifiedAtMs = System.currentTimeMillis(),
            ),
            planId,
        )
        log(planId, stepIndex, "macro runner log se VERIFIED ($macroName)")
        if (runJob?.isActive != true) launchRun(planId)
        return Result.success("Macro runner log se VERIFIED — plan aage badh raha hai.")
    }

    private fun queuedAtFromResult(resultText: String): Long? {
        val marker = "macro-queued-at="
        val start = resultText.indexOf(marker)
        if (start < 0) return null
        val digits = resultText.substring(start + marker.length).takeWhile { it.isDigit() }
        return digits.toLongOrNull()
    }

    /**
     * B4 follow-up P1-1 — AdapterQueued macro JSON ko maujooda runner raaste tak
     * pahunchana: MacroStore (REUSE, edit nahi) me Routines wale naam se save +
     * reload se verify. Koi naya accessibility-drive path nahi banaya gaya —
     * chalana Routines screen ke confirm dialog + FloraApp ke runMacroFrom se hi hoga.
     * Save ya reload-verify fail ho to false (step BLOCKED hi rahega, VERIFIED nahi).
     */
    private suspend fun deliverPlannerMacro(planId: String, step: StoredStep, outcome: PlannerTools.StepOutcome.AdapterQueued): Boolean {
        if (outcome.macroJson.isBlank()) return false
        val parsed = try {
            MacroJson.parse(outcome.macroJson)
        } catch (e: Exception) {
            null
        } ?: return false
        if (parsed.steps.isEmpty()) return false
        val macroName = plannerMacroName(planId, step.stepIndex)
        return try {
            val existing = MacroStore.load(appContext)
            val updated = existing.filter { it.name != macroName } + StoredMacro(macroName, outcome.macroJson)
            MacroStore.save(appContext, updated)
            val reloaded = MacroStore.load(appContext)
            val ok = reloaded.any { it.name == macroName && it.json == outcome.macroJson }
            if (ok) macroQueuedAtMs["$planId:${step.stepIndex}"] = System.currentTimeMillis()
            ok
        } catch (e: Exception) {
            false
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
            // A3 (h): apiKey class bhi transient hi hai — denylist
            // (PlannerModels) + scanner regex ke barabar. Stored
            // plan me apiKey kabhi nahi jayega.
            val transientPart = changes.filterKeys { key ->
                key.lowercase().contains("password") || key.lowercase().contains("db_pass") ||
                    key.lowercase().contains("secret") || key.lowercase().contains("token") ||
                    key.lowercase().contains("api_key") || key.lowercase().contains("apikey") ||
                    key.lowercase().contains("api-key")
            }
            if (transientPart.isNotEmpty()) transientInputs[index] = transientPart
            val storedPart = changes.filterKeys { key -> key !in transientPart.keys }
            // B4 follow-up P1-2 / P2(c): input badla to purana status nahi rahega.
            // VERIFIED step ka input chupchaap badal kar VERIFIED nahi rehne diya jayega,
            // aur BLOCKED/FAILED step dobara try ke liye PENDING hoga — warna
            // (a) VERIFIED + sideEffectId wala step M1 pre-check se dobara kabhi nahi chalta,
            // (b) BLOCKED step runLoop ke PENDING filter me kabhi wapas nahi aata.
            // Status reset par sideEffectId + verifiedAtMs bhi saaf (nahi to M1 Done maan lega).
            // SKIPPED/PENDING/RUNNING par ye reset nahi lagta (skip user ka faisla hai).
            steps = steps.map { step ->
                if (step.stepIndex == index) {
                    val reset = if (step.status == StepStatus.VERIFIED ||
                        step.status == StepStatus.BLOCKED || step.status == StepStatus.FAILED
                    ) {
                        step.copy(
                            status = StepStatus.PENDING,
                            resultText = "input amend hua — dobara verify hoga (purana status hata)",
                            sideEffectId = "",
                            verifiedAtMs = 0L,
                        )
                    } else {
                        step
                    }
                    reset.copy(input = reset.input + storedPart)
                } else {
                    step
                }
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
            // A3 (b) se juda index-keyed state: transient inputs
            // bhi stepIndex se key hote hain — reorder par unki
            // keys bhi remap hon, warna purana transient naye
            // step par lag jayega (wahi stale-index class ka bug).
            val remappedTransient = transientInputs.mapKeys { (oldIndex, _) -> remap[oldIndex] ?: oldIndex }
            transientInputs.clear()
            transientInputs.putAll(remappedTransient)
        }
        for (step in steps) repository.updateStep(step, planId)
        val newVersion = plan.version + 1
        repository.updatePlanMeta(
            planId, version = newVersion,
            // History me sirf step index (koi input value / password kabhi nahi).
            historyNote = "v$newVersion: amend (skip=$skipIndices, inputSteps=${inputChanges.keys.sorted()}, reorder=${newOrder != null})",
        )
        // A3 (b, R-A3-A1, P1 barkaraar): amend (skip / input-change /
        // reorder — koi bhi) par is plan ki approvals CLEAR. StepIndex
        // se key hone ki wajah se reorder/input-change ke baad purani
        // approval doosre step par lag sakti thi (spend step ki approval
        // ka carryover = stop-gate bypass). Session-memory design
        // waisa hi hai — sirf is plan ki approvals hat ti hain.
        approvedSteps.removeAll { it.startsWith("$planId:") }
        log(planId, -1, "plan amend hua -> v$newVersion (is plan ki approvals saaf ki gayi)")
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
        // B5-09: Timber = planner-step logging ka ek standard raasta.
        // Yahan sirf pattern-level text jata hai (planId/step/status) —
        // free-text detail (input, file content, key) Timber par kabhi nahi.
        try {
            Timber.tag("Planner").d("[plan:%s step:%d] %s", planId, stepIndex, text)
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
            // D-06 R4-1: plan-level stops = HARD GATES. Parser/model se aaye
            // stops sirf display (PlannerScreen) par nahi rukte — koi step
            // chalne se pehle plan PAUSED hota hai, wajah ke saath. Ye
            // gate har loop iteration par dobara check hota hai (replan
            // ke baad aaye stops bhi isi se rukenge).
            if (fresh.stops.isNotEmpty()) {
                repository.updatePlanMeta(
                    planId, status = PlanStatus.PAUSED,
                    lastError = "Plan stops par ruka: ${fresh.stops.joinToString(", ")}",
                )
                log(planId, -1, "plan stops hard gate: ${fresh.stops.joinToString(", ")}")
                refreshState(planId, PlanStatus.PAUSED, "Ruka: stops — ${fresh.stops.joinToString(", ")}")
                return
            }
            // D-06 R4-1: plan-level noDo enforcement (pending steps par hi,
            // prospective — VERIFIED steps ko retroactively gate nahi karta).
            // Kisi pending step ka title kisi noDo phrase se match kare to
            // plan PAUSED, wajah me noDo entry + step ka naam.
            val noDoHit = fresh.steps.filter { it.status == StepStatus.PENDING }.firstNotNullOfOrNull { step ->
                fresh.noDo.firstOrNull { entry ->
                    entry.isNotBlank() && step.title.lowercase().contains(entry.trim().lowercase())
                }?.let { entry -> entry to step.title }
            }
            if (noDoHit != null) {
                repository.updatePlanMeta(
                    planId, status = PlanStatus.PAUSED,
                    lastError = "No-do gate: '${noDoHit.second}' step noDo '${noDoHit.first}' se takrata hai",
                )
                log(planId, -1, "plan noDo hard gate: ${noDoHit.first} -> ${noDoHit.second}")
                refreshState(planId, PlanStatus.PAUSED, "Ruka: no-do — ${noDoHit.first}")
                return
            }
            val pending = fresh.steps.filter { it.status == StepStatus.PENDING }
            if (pending.isEmpty()) {
                val allDone = fresh.steps.all { it.status == StepStatus.VERIFIED || it.status == StepStatus.SKIPPED }
                val finalStatus = if (allDone) PlanStatus.DONE else PlanStatus.BLOCKED
                repository.updatePlanMeta(planId, status = finalStatus)
                // B5-05: DONE plan se safe single-step pattern seekho.
                if (finalStatus == PlanStatus.DONE) {
                    try {
                        maybeRecordLearned(fresh)
                    } catch (e: Exception) {
                    }
                }
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
        // B5-06 build-quota gate — sirf code-gen tools par, execute
        // se PEHLE. Plan-generation calls is gate se kabhi nahi
        // gine (wo createPlan me alag counter me hain).
        if (step.tool in AiBuildQuotaLogic.CODE_GEN_TOOLS) {
            val gateMsg = try {
                aiBuildQuota.buildGateMessage()
            } catch (e: Exception) {
                null
            }
            if (gateMsg != null) {
                return StepRunResult.Stopped(PlanStop.GENUINE_BLOCK, gateMsg)
            }
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
                lastFailureKindByStep["$planId:${step.stepIndex}"] = BuilderFailureKind.NETWORK
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
                    lastFailureKindByStep.remove("$planId:${step.stepIndex}")
                    if (step.stepIndex in transientInputs) transientInputs.remove(step.stepIndex)
                    log(planId, step.stepIndex, "VERIFIED: ${outcome.detail}")
                    // B5-06: AI build sirf yahan gina jata hai —
                    // code-gen execution VERIFIED hone par, ek baar
                    // (M1: VERIFIED step dobara kabhi nahi chalega,
                    // isliye double-count impossible hai).
                    if (step.tool in AiBuildQuotaLogic.CODE_GEN_TOOLS) {
                        try {
                            aiBuildQuota.recordCodeGeneration()
                        } catch (e: Exception) {
                        }
                        try {
                            log(planId, step.stepIndex, "ai-build gina (aaj ${aiBuildQuota.buildsToday()}/5)")
                        } catch (e: Exception) {
                        }
                    }
                    return StepRunResult.Done(done)
                }
                is PlannerTools.StepOutcome.Blocked -> {
                    repository.updateStep(step.copy(status = StepStatus.BLOCKED, attempts = attempts, resultText = outcome.reason), planId)
                    log(planId, step.stepIndex, "BLOCKED: ${outcome.reason}")
                    return StepRunResult.Stopped(outcome.stop, outcome.reason)
                }
                is PlannerTools.StepOutcome.AdapterQueued -> {
                    // B4 follow-up P1-1: macro JSON ko Routines runner raaste tak pahunchayo;
                    // deliver na ho to bhi step BLOCKED hi rahega — VERIFIED kabhi nahi.
                    val delivered = deliverPlannerMacro(planId, step, outcome)
                    val macroName = plannerMacroName(planId, step.stepIndex)
                    val queuedAt = macroQueuedAtMs["$planId:${step.stepIndex}"] ?: System.currentTimeMillis()
                    val deliveryText = if (delivered) {
                        "Macro Routines me save hua '$macroName' — Routines > Mere macros se Chalao + confirm se chalao, " +
                            "phir yahan runner-log confirm dabao. Planner ne khud accessibility nahi chalaya. [macro-queued-at:$queuedAt]"
                    } else {
                        "Macro deliver nahi hua (Routines save/verify fail) — step BLOCKED hai, VERIFIED nahi. Dobara Chalao par phir try hoga."
                    }
                    repository.updateStep(step.copy(status = StepStatus.BLOCKED, attempts = attempts, resultText = deliveryText), planId)
                    log(planId, step.stepIndex, "macro adapter ${if (delivered) "delivered" else "delivery-failed"}: $macroName")
                    return StepRunResult.Stopped(PlanStop.GENUINE_BLOCK, deliveryText)
                }
                is PlannerTools.StepOutcome.Failure -> {
                    lastKind = outcome.kind
                    lastDetail = outcome.detail
                    lastFailureKindByStep["$planId:${step.stepIndex}"] = outcome.kind
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
        lastFailureKindByStep["$planId:${step.stepIndex}"] = lastKind
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
