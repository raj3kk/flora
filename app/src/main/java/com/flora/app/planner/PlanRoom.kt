package com.flora.app.planner

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * B4 plan/task state (F02) — Room hi source of truth hai.
 * DataStore iska substitute nahi; ProjectStore (B2) project
 * files ka store hai, plan state ka nahi — dono alag hain.
 * MacroStore/RoutineStore/JobStore ko plan state NAHI banaya
 * gaya (wo apne-apne kaam ke stores hain).
 *
 * Completion marker = StepEntity.status VERIFIED + verifiedAtMs
 * + sideEffectId. Retry/resume hamesha marker se aage badhega
 * (M1/M15) — verified step ka side-effect dobara kabhi nahi.
 * Plan history Room me rahegi (historyJson: version notes) —
 * AutoLog me nahi, kyunki AutoLog 30-din prune hota hai (F11).
 */
@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey val planId: String,
    val goal: String,
    val version: Int,
    val status: String,
    val stopsJson: String,
    val noDoJson: String,
    val rollback: String,
    val estimateJson: String,
    val currentStepIndex: Int,
    val promptCount: Int,
    val lastError: String,
    val historyJson: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

@Entity(
    tableName = "plan_steps",
    indices = [Index(value = ["planId", "stepIndex"], unique = true)],
)
data class StepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val planId: String,
    val stepIndex: Int,
    val title: String,
    val tool: String,
    val inputJson: String,
    val successCheck: String,
    val fallback: String,
    val dependsOnJson: String,
    val sideEffectPreview: String,
    val externalName: String,
    val spend: Boolean,
    val irreversible: Boolean,
    val status: String,
    val resultText: String,
    val sideEffectId: String,
    val verifiedAtMs: Long,
    val attempts: Int,
)

@Dao
interface PlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity)

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Query("SELECT * FROM plans WHERE planId = :planId LIMIT 1")
    suspend fun getPlan(planId: String): PlanEntity?

    @Query("SELECT * FROM plans ORDER BY updatedAtMs DESC LIMIT 50")
    suspend fun listPlans(): List<PlanEntity>

    @Query("SELECT * FROM plans WHERE status IN (:statuses) ORDER BY updatedAtMs DESC")
    suspend fun plansWithStatus(statuses: List<String>): List<PlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<StepEntity>)

    @Update
    suspend fun updateStep(step: StepEntity)

    @Query("SELECT * FROM plan_steps WHERE planId = :planId ORDER BY stepIndex ASC")
    suspend fun getSteps(planId: String): List<StepEntity>

    @Query("DELETE FROM plan_steps WHERE planId = :planId")
    suspend fun deleteSteps(planId: String)
}

@Database(entities = [PlanEntity::class, StepEntity::class], version = 1, exportSchema = false)
abstract class PlanDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao
}

/** Stored plan view — engine + UI isi se padhte hain. */
data class StoredPlan(
    val planId: String,
    val goal: String,
    val version: Int,
    val status: PlanStatus,
    val stops: List<String>,
    val noDo: List<String>,
    val rollback: String,
    val estimate: PlanEstimate,
    val promptCount: Int,
    val lastError: String,
    val history: List<String>,
    val steps: List<StoredStep>,
)

data class StoredStep(
    val stepIndex: Int,
    val title: String,
    val tool: PlanTool,
    val input: Map<String, String>,
    val successCheck: String,
    val fallback: String,
    val dependsOn: List<Int>,
    val sideEffectPreview: String,
    val externalName: String,
    val spend: Boolean,
    val irreversible: Boolean,
    val status: StepStatus,
    val resultText: String,
    val sideEffectId: String,
    val verifiedAtMs: Long,
    val attempts: Int,
)

class PlanRepository(context: Context) {
    private val db: PlanDatabase = Room.databaseBuilder(
        context.applicationContext,
        PlanDatabase::class.java,
        "flora_plans.db",
    ).build()
    private val dao: PlanDao = db.planDao()
    private val json = Json { ignoreUnknownKeys = false }

    fun encodeStrings(values: List<String>): String = json.encodeToString(values)
    fun encodeInts(values: List<Int>): String = json.encodeToString(values)
    fun encodeMap(values: Map<String, String>): String = json.encodeToString(values)
    fun decodeStrings(raw: String): List<String> = try {
        json.decodeFromString<List<String>>(raw)
    } catch (e: Exception) {
        emptyList()
    }
    fun decodeInts(raw: String): List<Int> = try {
        json.decodeFromString<List<Int>>(raw)
    } catch (e: Exception) {
        emptyList()
    }
    fun decodeMap(raw: String): Map<String, String> = try {
        json.decodeFromString<Map<String, String>>(raw)
    } catch (e: Exception) {
        emptyMap()
    }

    suspend fun saveNewPlan(planId: String, draft: PlanDraft, estimate: PlanEstimate): StoredPlan {
        val now = System.currentTimeMillis()
        val plan = PlanEntity(
            planId = planId,
            goal = draft.goal,
            version = 1,
            status = PlanStatus.DRAFT.name,
            stopsJson = encodeStrings(draft.stops),
            noDoJson = encodeStrings(draft.noDo),
            rollback = draft.rollback,
            estimateJson = estimate.display(),
            currentStepIndex = 0,
            promptCount = 0,
            lastError = "",
            historyJson = encodeStrings(listOf("v1: plan bana")),
            createdAtMs = now,
            updatedAtMs = now,
        )
        dao.insertPlan(plan)
        val steps = draft.steps.mapIndexed { index, step ->
            StepEntity(
                planId = planId,
                stepIndex = index,
                title = step.title,
                tool = step.tool.trim().uppercase(),
                inputJson = encodeMap(step.input),
                successCheck = step.successCheck,
                fallback = step.fallback,
                dependsOnJson = encodeInts(step.dependsOn),
                sideEffectPreview = step.sideEffectPreview,
                externalName = step.externalName,
                spend = step.spend,
                irreversible = step.irreversible,
                status = StepStatus.PENDING.name,
                resultText = "",
                sideEffectId = "",
                verifiedAtMs = 0L,
                attempts = 0,
            )
        }
        dao.insertSteps(steps)
        return loadPlan(planId) ?: throw IllegalStateException("plan save ke baad load nahi hua")
    }

    suspend fun loadPlan(planId: String): StoredPlan? {
        val plan = dao.getPlan(planId) ?: return null
        val steps = dao.getSteps(planId).map { e -> toStoredStep(e) }
        return StoredPlan(
            planId = plan.planId,
            goal = plan.goal,
            version = plan.version,
            status = PlanStatus.valueOf(plan.status),
            stops = decodeStrings(plan.stopsJson),
            noDo = decodeStrings(plan.noDoJson),
            rollback = plan.rollback,
            estimate = PlanEstimate(0, 0, plan.estimateJson),
            promptCount = plan.promptCount,
            lastError = plan.lastError,
            history = decodeStrings(plan.historyJson),
            steps = steps,
        )
    }

    suspend fun listPlans(): List<StoredPlan> {
        val out = mutableListOf<StoredPlan>()
        for (plan in dao.listPlans()) {
            val loaded = loadPlan(plan.planId)
            if (loaded != null) out += loaded
        }
        return out
    }

    suspend fun interruptedPlans(): List<StoredPlan> {
        val out = mutableListOf<StoredPlan>()
        for (plan in dao.plansWithStatus(listOf(PlanStatus.RUNNING.name, PlanStatus.PAUSED.name))) {
            val loaded = loadPlan(plan.planId)
            if (loaded != null) out += loaded
        }
        return out
    }

    suspend fun updatePlanMeta(
        planId: String,
        status: PlanStatus? = null,
        promptCount: Int? = null,
        lastError: String? = null,
        historyNote: String? = null,
        version: Int? = null,
    ) {
        val plan = dao.getPlan(planId) ?: return
        val history = decodeStrings(plan.historyJson).toMutableList()
        if (historyNote != null) history += historyNote
        dao.updatePlan(
            plan.copy(
                status = (status ?: PlanStatus.valueOf(plan.status)).name,
                promptCount = promptCount ?: plan.promptCount,
                lastError = lastError ?: plan.lastError,
                historyJson = encodeStrings(history),
                version = version ?: plan.version,
                updatedAtMs = System.currentTimeMillis(),
            )
        )
    }

    suspend fun updateStep(step: StoredStep, planId: String) {
        val existing = dao.getSteps(planId).firstOrNull { it.stepIndex == step.stepIndex } ?: return
        dao.updateStep(
            existing.copy(
                title = step.title,
                tool = step.tool.name,
                inputJson = encodeMap(step.input),
                successCheck = step.successCheck,
                fallback = step.fallback,
                dependsOnJson = encodeInts(step.dependsOn),
                sideEffectPreview = step.sideEffectPreview,
                externalName = step.externalName,
                spend = step.spend,
                irreversible = step.irreversible,
                status = step.status.name,
                resultText = step.resultText,
                sideEffectId = step.sideEffectId,
                verifiedAtMs = step.verifiedAtMs,
                attempts = step.attempts,
            )
        )
    }

    /** Replan/amend: baaki steps naye version se badlo, verified steps chhuo mat.
     *  Verified steps apne original index par rehte hain; naye steps unke
     *  baad (maxIndex+1 se) judte hain taaki index takrao kabhi na ho.
     *  Naye draft ke dependsOn usi draft ke andar ke index hain, isliye
     *  base se map hote hain. */
    suspend fun replacePendingSteps(planId: String, newDraft: PlanDraft, newVersion: Int, note: String) {
        val plan = dao.getPlan(planId) ?: return
        val existing = dao.getSteps(planId)
        val verified = existing.filter { it.status == StepStatus.VERIFIED.name }
        val base = (existing.maxOfOrNull { it.stepIndex } ?: -1) + 1
        dao.deleteSteps(planId)
        dao.insertSteps(verified)
        val fresh = newDraft.steps.mapIndexed { index, step ->
            StepEntity(
                planId = planId,
                stepIndex = base + index,
                title = step.title,
                tool = step.tool.trim().uppercase(),
                inputJson = encodeMap(step.input),
                successCheck = step.successCheck,
                fallback = step.fallback,
                dependsOnJson = encodeInts(step.dependsOn.map { it + base }),
                sideEffectPreview = step.sideEffectPreview,
                externalName = step.externalName,
                spend = step.spend,
                irreversible = step.irreversible,
                status = StepStatus.PENDING.name,
                resultText = "",
                sideEffectId = "",
                verifiedAtMs = 0L,
                attempts = 0,
            )
        }
        dao.insertSteps(fresh)
        val history = decodeStrings(plan.historyJson).toMutableList()
        history += note
        dao.updatePlan(
            plan.copy(
                version = newVersion,
                historyJson = encodeStrings(history),
                updatedAtMs = System.currentTimeMillis(),
            )
        )
    }

    private fun toStoredStep(e: StepEntity): StoredStep {
        return StoredStep(
            stepIndex = e.stepIndex,
            title = e.title,
            tool = PlanTool.fromString(e.tool) ?: PlanTool.NOTE,
            input = decodeMap(e.inputJson),
            successCheck = e.successCheck,
            fallback = e.fallback,
            dependsOn = decodeInts(e.dependsOnJson),
            sideEffectPreview = e.sideEffectPreview,
            externalName = e.externalName,
            spend = e.spend,
            irreversible = e.irreversible,
            status = StepStatus.valueOf(e.status),
            resultText = e.resultText,
            sideEffectId = e.sideEffectId,
            verifiedAtMs = e.verifiedAtMs,
            attempts = e.attempts,
        )
    }
}
