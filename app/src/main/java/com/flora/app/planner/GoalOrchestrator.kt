package com.flora.app.planner

import com.flora.app.data.AutoLogStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

/**
 * G12 — goal orchestrator (Phase 9, A2).
 *
 * Layer PlannerEngine ke UPAR hai (goal -> sub-plans), uske
 * andar koi naya step type NAHI hai — ek raasta, dono nahi.
 *
 * Sub-agent = ek alag PlannerEngine plan (apna planId, apne
 * steps, apne stops). Parent goal id ke neeche grouped hain —
 * na sirf coroutine scope, na sirf step grouping.
 *
 * Independence structural hai: sub-plans alag Room plans
 * hain, aur depends_on sirf ek plan ke andar ke step
 * indices par lagta hai — cross-plan depends_on ka koi
 * raasta hi nahi hai. Isliye alag plans kabhi ek-doosre
 * par depends_on se nahi atakte; dependent kaam caller ko
 * ek hi plan (single plan, engine.createPlan) me rakhna hai.
 *
 * Fail = ISOLATE: failed sub-plan rukega, siblings chalte
 * rahenge, parent goal partial + failed part ki saaf report
 * dega (chupchaap fail kabhi nahi).
 *
 * Parent stop/pause = SAARE sub-plans rukenge: chal raha
 * sub-plan engine.stopPlan se rukega, shuru na hue subs
 * PARENT_STOPPED honge (WAITING_APPROVAL subs bhi — stop
 * ke baad koi approval UI nahi bachta). Ruka hua sub-plan
 * background me kuch nahi chalata — YE SIRF NORMAL STOP
 * PATH PAR SAHI HAI: stop ke baad approval-resume REFUSE
 * hota hai (stopRequested/PAUSED check), aur approval-resume
 * ke baad stop aaye to resumed plan ka poll job + planId
 * track karke engine.stopPlan lagta hai — isliye stopped
 * goal ka koi plan background me nahi chalta. Waiting
 * sub-plan (goal ruka na ho) user ki explicit approval
 * par hi dobara chalega.
 *
 * Replan per-sub-plan hai (engine ke runLoop ka max 2 replan
 * rule andar hi chalta hai); parent goal ka silent full-replan
 * kabhi nahi hota — dobara chalana ho to naya goal banao.
 *
 * Stops/approvals per-sub-plan, alag-alag: parent ki ek
 * approval sub-plans par carryover NAHI hoti. Har sub-plan
 * ka stop uske apne plan ke approveAndResume se khulta hai
 * — SIRF tab jab parent goal ruka na ho; parent stop ke
 * baad approveSubPlan REFUSE karta hai (koi resume nahi).
 *
 * Gating: orchestrator koi ToolRegistry/PlanTool entry NAHI
 * leta (wo plans ko compose karta hai, plan ke andar ka tool
 * nahi) aur koi gate ADD/BYPASS nahi karta. Sub-plan ke steps
 * jo builder tools call karenge, un par un tools ke apne
 * gates individually lagte rahenge — engine ke andar builder
 * entitlement gate, allowlist, quota gates aur stops, bilkul
 * wahi jo single plan par lagte hain. Execution sirf engine
 * ki machinery se hoti hai (koi doosra executor nahi — double
 * side-effect ka khatra nahi; M1/idempotency pre-check bhi
 * engine ka hi chalta hai).
 *
 * AutoLog format: "[goal:<goalId> subplan:<planId>] ..." —
 * engine ke "[plan:<id> step:<n>]" format se takrata nahi;
 * sub-plan ke steps apna wahi format rakhte hain.
 *
 * Cap: ek goal me max 5 sub-plans. Uss se zyada independent
 * parts hon to split mat karo — single plan + imaandaar note.
 *
 * Note: goal -> sub-plan mapping is version me memory me hai
 * (Room schema change is push me nahi). App restart par goal
 * runs UI se hat jayenge, par sub-plans Room me "Pichhle
 * plans" me dikhte rahenge aur AutoLog me goal entries
 * (30-din prune tak) record rahega.
 */
enum class GoalSubStatus {
    QUEUED,
    RUNNING,
    DONE,
    FAILED,
    BLOCKED,
    WAITING_APPROVAL,
    PARENT_STOPPED,
}

enum class GoalStatus {
    RUNNING,
    DONE,
    PARTIAL,
    PAUSED,
}

data class GoalSubPlan(
    val subGoal: String,
    val planId: String,
    val status: GoalSubStatus,
    val detail: String,
)

data class GoalRun(
    val goalId: String,
    val goal: String,
    val status: GoalStatus,
    val subPlans: List<GoalSubPlan>,
    val report: String,
)

sealed class GoalCreation {
    data class Created(val run: GoalRun) : GoalCreation()
    data class NeedsSinglePlan(val reason: String) : GoalCreation()
    data class Invalid(val reason: String) : GoalCreation()
}

class GoalOrchestrator(
    private val engine: PlannerEngine,
    private val autoLogStore: AutoLogStore,
) {
    companion object {
        const val MAX_SUB_PLANS = 5
    }

    private val orchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    private class MutableSub(val subGoal: String) {
        var planId: String = ""
        var status: GoalSubStatus = GoalSubStatus.QUEUED
        var detail: String = "banne ka intezar hai"
    }

    private class MutableGoal(
        val goalId: String,
        val goal: String,
        subGoals: List<String>,
    ) {
        val subs: List<MutableSub> = subGoals.map { MutableSub(it) }
        var status: GoalStatus = GoalStatus.RUNNING
        var report: String = "Goal shuru nahi hua hai."
        var started: Boolean = false
        @Volatile var stopRequested: Boolean = false
        @Volatile var currentPlanId: String? = null
    }

    private val goalsMap = mutableMapOf<String, MutableGoal>()
    private val runJobs = mutableMapOf<String, Job>()
    // Approval-resume poll jobs, planId se tracked — stopGoal
    // inhe cancel + inke planIds par engine.stopPlan karta hai.
    // Stale entries harmless hain (cancel no-op, planId unique).
    private val approvalJobs = mutableMapOf<String, Job>()
    private val goalsFlow = MutableStateFlow<List<GoalRun>>(emptyList())
    val goals: StateFlow<List<GoalRun>> = goalsFlow.asStateFlow()

    /** Goal banao (sirf register; chalana startGoal se). */
    fun createGoal(goal: String, subGoals: List<String>): GoalCreation {
        val cleanGoal = goal.trim()
        if (cleanGoal.isBlank()) return GoalCreation.Invalid("goal khali hai")
        val cleanSubs = subGoals.map { it.trim() }.filter { it.isNotBlank() }
        if (cleanSubs.isEmpty()) return GoalCreation.Invalid("koi sub-goal nahi diya")
        val dup = cleanSubs.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }
        if (dup != null) {
            return GoalCreation.Invalid("duplicate sub-goal hai: '${dup.key}' — ek hi kaam do plans me nahi chalega (double side-effect ka khatra)")
        }
        if (cleanSubs.size > MAX_SUB_PLANS) {
            return GoalCreation.NeedsSinglePlan(
                "$MAX_SUB_PLANS se zyada independent parts (${cleanSubs.size}) — split mat karo, " +
                    "single plan banao (Planner ka 'Plan banao' button). Cap isliye hai taaki goal " +
                    "an-ginat sub-plans me na bikhre."
            )
        }
        val goalId = UUID.randomUUID().toString().substring(0, 8)
        val mutable = MutableGoal(goalId, cleanGoal, cleanSubs)
        synchronized(lock) { goalsMap[goalId] = mutable }
        recompute(mutable)
        emit()
        orchScope.launch {
            logGoal(goalId, "-", "goal bana (${cleanSubs.size} sub-plans)")
        }
        return GoalCreation.Created(snapshot(mutable))
    }

    /** Goal chalao — sirf fresh goal; sub-plans ek-ek karke engine se chalte hain. */
    fun startGoal(goalId: String): Boolean {
        val goal = synchronized(lock) { goalsMap[goalId] } ?: return false
        synchronized(lock) {
            if (runJobs[goalId]?.isActive == true) return false
        }
        // BLOCK route (R-A2-12): sirf FRESH goal chalega. Jo goal
        // pehle chal chuka hai (DONE/PAUSED/PARTIAL koi bhi),
        // uska dobara-run block hai — dobara chalana ho to naya
        // goal banao (header comment ke mutabiq).
        if (goal.started) return false
        goal.stopRequested = false
        goal.started = true
        val job = orchScope.launch {
            try {
                executeGoal(goal)
            } catch (e: CancellationException) {
                // Parent stop ne job cancel ki — finalize phir bhi hoga.
                finalizeStopped(goal, "goal roka gaya")
                throw e
            }
        }
        synchronized(lock) { runJobs[goalId] = job }
        return true
    }

    /** Parent stop/pause — chal raha sub-plan rukega, baaki shuru nahi honge. */
    fun stopGoal(goalId: String) {
        val goal = synchronized(lock) { goalsMap[goalId] } ?: return
        goal.stopRequested = true
        synchronized(lock) { runJobs[goalId] }?.cancel()
        // Approval-poll jobs cancel + saare sub-planIds jama karo
        // (current + approval-resume wala — currentPlanId null hone
        // par bhi resumed plan stop se bach na sake).
        val planIds = mutableSetOf<String>()
        val current = goal.currentPlanId
        if (!current.isNullOrBlank()) planIds += current
        for (sub in goal.subs) {
            if (sub.planId.isNotBlank()) {
                synchronized(lock) { approvalJobs[sub.planId] }?.cancel()
                planIds += sub.planId
            }
        }
        orchScope.launch {
            // Room hi source of truth: DONE/FAILED/BLOCKED plan par
            // stopPlan NAHI (warna DONE plan PAUSED mark ho jayega).
            for (planId in planIds) {
                val fresh = try {
                    engine.loadPlan(planId)
                } catch (e: Exception) {
                    null
                } ?: continue
                if (fresh.status == PlanStatus.DONE || fresh.status == PlanStatus.FAILED ||
                    fresh.status == PlanStatus.BLOCKED
                ) {
                    continue
                }
                engine.stopPlan(planId, "goal roka gaya")
            }
            // Agar execute job pehle hi finalize kar chuka ho to ye
            // dobara kuch nahi badlega (finalize idempotent hai).
            finalizeStopped(goal, "goal roka gaya")
        }
    }

    /** Per-sub-plan approval — sirf isi sub-plan ka stop khulta hai, siblings par carryover nahi.
     *  Stopped goal par REFUSE (false): approval-resume stopped goal par kabhi nahi. */
    fun approveSubPlan(goalId: String, planId: String, stepIndex: Int): Boolean {
        val goal = synchronized(lock) { goalsMap[goalId] } ?: return false
        if (goal.stopRequested || goal.status == GoalStatus.PAUSED) return false
        val sub = goal.subs.firstOrNull { it.planId == planId } ?: return false
        engine.approveAndResume(planId, stepIndex)
        sub.status = GoalSubStatus.RUNNING
        sub.detail = "approval ke baad resume hua"
        emit()
        val pollJob = orchScope.launch {
            logGoal(goalId, planId, "sub-plan approval ke baad resume (step $stepIndex)")
            // respectParentStop = true: stop ke baad ye poll khud
            // plan rok kar lautega (job cancel ka backup).
            pollSub(goal, sub, respectParentStop = true)
            recompute(goal)
            emit()
        }
        synchronized(lock) { approvalJobs[planId] = pollJob }
        return true
    }

    /** Sub-plan statuses Room se dobara padho (waiting subs ka haal update karne ke liye). */
    fun refreshGoal(goalId: String) {
        val goal = synchronized(lock) { goalsMap[goalId] } ?: return
        orchScope.launch {
            for (sub in goal.subs) {
                if (sub.planId.isBlank()) continue
                // PARENT_STOPPED kabhi revive nahi hoga.
                if (sub.status == GoalSubStatus.PARENT_STOPPED) continue
                val fresh = try {
                    engine.loadPlan(sub.planId)
                } catch (e: Exception) {
                    null
                } ?: continue
                sub.status = mapPlanStatus(fresh.status)
                sub.detail = when (fresh.status) {
                    PlanStatus.DONE -> "poora ho gaya"
                    PlanStatus.FAILED -> fresh.lastError.ifBlank { "fail ho gaya" }
                    PlanStatus.BLOCKED -> fresh.lastError.ifBlank { "block ho gaya" }
                    PlanStatus.PAUSED -> "stop par ruka hai — approval/resume chahiye" +
                        (if (fresh.lastError.isNotBlank()) ": ${fresh.lastError}" else "")
                    PlanStatus.STOPPED -> "stop par ruka hai — approval/resume chahiye"
                    PlanStatus.RUNNING -> "chal raha hai (${fresh.steps.count { it.status == StepStatus.VERIFIED }}/${fresh.steps.size} steps)"
                    PlanStatus.DRAFT -> "queue me hai"
                }
            }
            recompute(goal)
            emit()
        }
    }

    private suspend fun executeGoal(goal: MutableGoal) {
        for (sub in goal.subs) {
            if (goal.stopRequested) {
                finalizeStopped(goal, "goal roka gaya")
                return
            }
            // Dobara-run guard (BLOCK route ki doosri deewar):
            // pehle se planned/DONE sub ka planId kabhi overwrite
            // nahi hoga, purana plan orphan nahi hoga. Fresh run
            // me ye kabhi trigger nahi hota (sab QUEUED + blank).
            if (sub.status == GoalSubStatus.DONE || sub.planId.isNotBlank()) {
                continue
            }
            sub.status = GoalSubStatus.RUNNING
            sub.detail = "plan ban raha hai"
            emit()
            val created = try {
                engine.createPlan(sub.subGoal)
            } catch (e: Exception) {
                Result.failure<StoredPlan>(e)
            }
            if (created.isFailure) {
                // ISOLATE: ye sub-plan fail, siblings aage chalenge.
                sub.status = GoalSubStatus.FAILED
                sub.detail = "plan nahi bana: ${created.exceptionOrNull()?.message ?: "unknown"}"
                emit()
                logGoal(goal.goalId, "-", "sub-plan fail (plan nahi bana): ${sub.subGoal}")
                continue
            }
            val plan = created.getOrNull()
            if (plan == null) {
                sub.status = GoalSubStatus.FAILED
                sub.detail = "plan nahi bana (khali result)"
                emit()
                continue
            }
            sub.planId = plan.planId
            goal.currentPlanId = plan.planId
            logGoal(goal.goalId, plan.planId, "sub-plan bana: ${sub.subGoal}")
            engine.startPlan(plan.planId)
            sub.detail = "chal raha hai"
            emit()
            pollSub(goal, sub, respectParentStop = true)
            goal.currentPlanId = null
            if (goal.stopRequested) {
                finalizeStopped(goal, "goal roka gaya")
                return
            }
        }
        recompute(goal)
        emit()
        logGoal(goal.goalId, "-", "goal khatm: ${goal.status} — ${goal.report}")
    }

    /** Ek sub-plan ko terminal status tak poll karo (engine ki Room status hi source of truth hai). */
    private suspend fun pollSub(goal: MutableGoal, sub: MutableSub, respectParentStop: Boolean) {
        while (true) {
            if (respectParentStop && goal.stopRequested) {
                if (sub.planId.isNotBlank()) {
                    engine.stopPlan(sub.planId, "goal roka gaya")
                }
                return
            }
            delay(1000L)
            val fresh = try {
                engine.loadPlan(sub.planId)
            } catch (e: Exception) {
                null
            }
            if (fresh == null) {
                sub.status = GoalSubStatus.FAILED
                sub.detail = "plan load nahi hua"
                emit()
                return
            }
            when (fresh.status) {
                PlanStatus.DONE -> {
                    sub.status = GoalSubStatus.DONE
                    sub.detail = "poora ho gaya"
                    emit()
                    logGoal(goal.goalId, sub.planId, "sub-plan DONE")
                    return
                }
                PlanStatus.FAILED -> {
                    sub.status = GoalSubStatus.FAILED
                    sub.detail = fresh.lastError.ifBlank { "fail ho gaya" }
                    emit()
                    logGoal(goal.goalId, sub.planId, "sub-plan FAILED: ${sub.detail}")
                    return
                }
                PlanStatus.BLOCKED -> {
                    sub.status = GoalSubStatus.BLOCKED
                    sub.detail = fresh.lastError.ifBlank { "block ho gaya" }
                    emit()
                    logGoal(goal.goalId, sub.planId, "sub-plan BLOCKED: ${sub.detail}")
                    return
                }
                PlanStatus.PAUSED, PlanStatus.STOPPED -> {
                    // Stop hit — approval ka intezar; siblings aage chalenge.
                    sub.status = GoalSubStatus.WAITING_APPROVAL
                    sub.detail = "stop par ruka hai — approval/resume chahiye" +
                        (if (fresh.lastError.isNotBlank()) ": ${fresh.lastError}" else "")
                    emit()
                    logGoal(goal.goalId, sub.planId, "sub-plan WAITING_APPROVAL: ${sub.detail}")
                    return
                }
                PlanStatus.DRAFT -> {
                    sub.status = GoalSubStatus.RUNNING
                    sub.detail = "queue me hai (pehla plan chal raha hai)"
                    emit()
                }
                PlanStatus.RUNNING -> {
                    sub.status = GoalSubStatus.RUNNING
                    sub.detail = "chal raha hai (${fresh.steps.count { it.status == StepStatus.VERIFIED }}/${fresh.steps.size} steps)"
                    emit()
                }
            }
        }
    }

    private fun mapPlanStatus(status: PlanStatus): GoalSubStatus = when (status) {
        PlanStatus.DONE -> GoalSubStatus.DONE
        PlanStatus.FAILED -> GoalSubStatus.FAILED
        PlanStatus.BLOCKED -> GoalSubStatus.BLOCKED
        PlanStatus.PAUSED, PlanStatus.STOPPED -> GoalSubStatus.WAITING_APPROVAL
        PlanStatus.RUNNING -> GoalSubStatus.RUNNING
        PlanStatus.DRAFT -> GoalSubStatus.QUEUED
    }

    /** Idempotent: QUEUED/RUNNING/WAITING_APPROVAL subs PARENT_STOPPED honge, DONE/FAILED/BLOCKED nahi chhedega. */
    private suspend fun finalizeStopped(goal: MutableGoal, reason: String) {
        var changed = false
        for (sub in goal.subs) {
            if (sub.status == GoalSubStatus.QUEUED || sub.status == GoalSubStatus.RUNNING ||
                sub.status == GoalSubStatus.WAITING_APPROVAL
            ) {
                sub.status = GoalSubStatus.PARENT_STOPPED
                sub.detail = reason
                changed = true
            }
        }
        if (goal.subs.all { it.status == GoalSubStatus.DONE }) {
            goal.status = GoalStatus.DONE
        } else {
            goal.status = GoalStatus.PAUSED
        }
        recompute(goal)
        emit()
        if (changed) {
            logGoal(goal.goalId, "-", "goal PAUSED: $reason")
        }
    }

    /** Goal status + partial report — failed/waiting part hamesha saaf dikhega. */
    private fun recompute(goal: MutableGoal) {
        val done = goal.subs.count { it.status == GoalSubStatus.DONE }
        val total = goal.subs.size
        val failed = goal.subs.filter {
            it.status == GoalSubStatus.FAILED || it.status == GoalSubStatus.BLOCKED
        }
        val waiting = goal.subs.filter { it.status == GoalSubStatus.WAITING_APPROVAL }
        val stopped = goal.subs.filter { it.status == GoalSubStatus.PARENT_STOPPED }
        goal.status = when {
            goal.subs.all { it.status == GoalSubStatus.DONE } -> GoalStatus.DONE
            goal.stopRequested -> GoalStatus.PAUSED
            failed.isNotEmpty() || waiting.isNotEmpty() ||
                goal.subs.all {
                    it.status == GoalSubStatus.DONE || it.status == GoalSubStatus.FAILED ||
                        it.status == GoalSubStatus.BLOCKED || it.status == GoalSubStatus.WAITING_APPROVAL ||
                        it.status == GoalSubStatus.PARENT_STOPPED
                } -> GoalStatus.PARTIAL
            else -> GoalStatus.RUNNING
        }
        val parts = mutableListOf<String>()
        parts += "$done/$total sub-plans done"
        if (failed.isNotEmpty()) {
            parts += "failed/blocked: " + failed.joinToString(", ") { "'${it.subGoal}' (${it.detail})" }
        }
        if (waiting.isNotEmpty()) {
            parts += "approval ka intezar: " + waiting.joinToString(", ") { "'${it.subGoal}'" }
        }
        if (stopped.isNotEmpty()) {
            parts += "goal rokne par ruke: " + stopped.joinToString(", ") { "'${it.subGoal}'" }
        }
        goal.report = when (goal.status) {
            GoalStatus.DONE -> "Goal poora ho gaya — " + parts.joinToString("; ")
            GoalStatus.PAUSED -> "Goal ruka hua hai — " + parts.joinToString("; ")
            GoalStatus.PARTIAL -> "Goal PARTIAL hai (poora nahi) — " + parts.joinToString("; ")
            GoalStatus.RUNNING -> "Goal chal raha hai — " + parts.joinToString("; ")
        }
        if (!goal.started && goal.subs.all { it.status == GoalSubStatus.QUEUED }) {
            goal.report = "Goal bana hua hai, abhi chala nahi — 'Goal chalao' dabane par sub-plans chalenge. " +
                parts.joinToString("; ")
        }
    }

    private fun snapshot(goal: MutableGoal): GoalRun = GoalRun(
        goalId = goal.goalId,
        goal = goal.goal,
        status = goal.status,
        subPlans = goal.subs.map {
            GoalSubPlan(it.subGoal, it.planId, it.status, it.detail)
        },
        report = goal.report,
    )

    private fun emit() {
        val snap = synchronized(lock) { goalsMap.values.map { snapshot(it) } }
        goalsFlow.value = snap
    }

    private suspend fun logGoal(goalId: String, planId: String, text: String) {
        try {
            autoLogStore.add("[goal:$goalId subplan:$planId] $text")
        } catch (e: Exception) {
        }
        try {
            Timber.tag("GoalOrchestrator").d("[goal:%s subplan:%s] %s", goalId, planId, text)
        } catch (e: Exception) {
        }
    }
}
