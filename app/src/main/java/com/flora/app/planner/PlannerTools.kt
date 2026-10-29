package com.flora.app.planner

import com.flora.app.automation.Macro
import com.flora.app.automation.MacroJson
import com.flora.app.automation.MacroStep
import com.flora.app.core.PlanFeatures
import com.flora.app.data.BuilderService
import com.flora.app.data.FloraPreferences
import com.flora.app.data.ProjectStore
import com.flora.app.doctor.DoctorCategory
import com.flora.app.doctor.DoctorScanner
import com.flora.app.doctor.DoctorSeverity
import com.flora.app.tools.BuilderFailureKind
import com.flora.app.tools.BuilderHttp
import com.flora.app.tools.BuilderOutcome
import com.flora.app.tools.GitHubClient
import com.flora.app.tools.KeyInputGuard
import com.flora.app.tools.SupabaseProjectClient
import com.flora.app.tools.VercelClient
import org.json.JSONObject

/**
 * B4 tools registry / step-to-tool binding (F12).
 *
 * NAYA duplicate transport MANA hai — isliye har tool B3 ke
 * maujooda clients se judta hai (GitHubClient, VercelClient,
 * SupabaseProjectClient, BuilderHttp/Outcome/Redaction,
 * BuilderOAuth, BuilderTokens, KeyInputGuard) aur B2 ke
 * ProjectStore se. Retrofit par re-likhna ya naya
 * HttpURLConnection client duplicate hota — dono nahi kiye.
 *
 * Macro sirf tool-ADAPTER hai (UI steps ke liye) — executor
 * nahi. MacroStep sealed class ko planner steps ke liye reuse
 * karna conflict hota, isliye planner ke apne PlanStepDraft
 * hain; adapter sirf Macro JSON banata hai (MacroJson.toJson),
 * accessibility drive planner se nahi hota (F12 note).
 *
 * ConnectServicesScreen ka grant-once flow dobara NAHI banaya
 * gaya — planner sirf "connected hai ya nahi" (whoAmI) check
 * karta hai; not-connected = genuine block + ConnectServices
 * ka raasta (F12/B3 grant-once barkaraar).
 *
 * PASSWORD RULE barkaraar: password field kabhi type nahi;
 * login-like -> stop #3, submit-like -> stop #2. Supabase DB
 * password stored plan me kabhi nahi — sirf transient amend
 * payload se (engine ke paas, Room me nahi).
 */
class PlannerTools(
    prefs: FloraPreferences,
    private val projectStore: ProjectStore,
) {
    /**
     * B4-DOCTOR allowlist (DOC-P0-2): builder-project remote
     * targets ("github:owner/repo", "vercel:name") ka set.
     * Engine hi ise set karta hai — PlannerTools sirf engine
     * se banta hai (ek hi construction site), isliye provider
     * null = koi target allowed nahi (fail-closed), kabhi
     * "sab allowed" default nahi. Set Room plans ke VERIFIED
     * create-steps se banta hai (engine me) — naye repo/project
     * banane wale steps khud gate nahi hote, wahi registry ka
     * entry point hain.
     */
    var allowedTargetsProvider: (suspend () -> Set<String>)? = null

    /**
     * B5-07 registry audit + B5-06 quota gates — engine hi inhe
     * set karta hai (PlannerEngine init, single construction
     * site PlannerEngine:82). Null = gate/audit nahi (fail-open,
     * purana vyavhaar) — tools engine ke bina kabhi nahi chalte,
     * isliye production me ye hamesha set hote hain.
     */
    var auditSink: (suspend (String) -> Unit)? = null
    var buildQuotaGate: (suspend () -> String?)? = null
    var projectQuotaGate: (suspend (Int) -> String?)? = null

    /** Builder tools (B5-08 gate isi set par lagta hai). */
    private val builderTools: Set<PlanTool> = setOf(
        PlanTool.GITHUB_CREATE_REPO,
        PlanTool.GITHUB_PUSH_FILE,
        PlanTool.VERCEL_CREATE_PROJECT,
        PlanTool.VERCEL_DEPLOY,
        PlanTool.SUPABASE_CREATE_PROJECT,
        PlanTool.SUPABASE_RUN_SQL,
        PlanTool.PROJECT_SAVE_FILE,
        PlanTool.PROJECT_READ_FILE,
    )

    /** Registry descriptor list — ToolRegistry.all() hi ek source hai. */
    fun toolDescriptors(): List<ToolDescriptor> = ToolRegistry.all()

    private suspend fun audit(tool: String, action: String) {
        try {
            auditSink?.invoke("[tool:$tool] $action")
        } catch (e: Exception) {
        }
    }

    private suspend fun allowedTargets(): Set<String> =
        allowedTargetsProvider?.invoke() ?: emptySet()

    private val github = GitHubClient(prefs)
    private val vercel = VercelClient(prefs)
    private val supabase = SupabaseProjectClient(prefs)

    sealed class StepOutcome {
        data class Ok(val sideEffectId: String, val detail: String) : StepOutcome()
        data class Failure(val kind: BuilderFailureKind, val detail: String) : StepOutcome()
        data class Blocked(val reason: String, val stop: PlanStop) : StepOutcome()
        data class AdapterQueued(val macroJson: String, val note: String) : StepOutcome()
    }

    suspend fun isConnected(tool: PlanTool): Boolean {
        return when (tool) {
            PlanTool.GITHUB_CREATE_REPO, PlanTool.GITHUB_PUSH_FILE -> github.whoAmI() is BuilderOutcome.Ok
            PlanTool.VERCEL_CREATE_PROJECT, PlanTool.VERCEL_DEPLOY -> vercel.whoAmI() is BuilderOutcome.Ok
            PlanTool.SUPABASE_CREATE_PROJECT, PlanTool.SUPABASE_RUN_SQL -> supabase.whoAmI() is BuilderOutcome.Ok
            else -> true
        }
    }

    fun serviceFor(tool: PlanTool): BuilderService? = when (tool) {
        PlanTool.GITHUB_CREATE_REPO, PlanTool.GITHUB_PUSH_FILE -> BuilderService.GITHUB
        PlanTool.VERCEL_CREATE_PROJECT, PlanTool.VERCEL_DEPLOY -> BuilderService.VERCEL
        PlanTool.SUPABASE_CREATE_PROJECT, PlanTool.SUPABASE_RUN_SQL -> BuilderService.SUPABASE
        else -> null
    }

    /** Macro adapter: plan step -> Macro JSON (execute planner se nahi hota). */
    fun adaptMacro(input: Map<String, String>): StepOutcome {
        val action = (input["action"] ?: "").lowercase()
        val label = input["label"] ?: ""
        val text = input["text"] ?: ""
        if (text.contains("password", ignoreCase = true)) {
            return StepOutcome.Blocked(
                "Macro Type step me password jaisa text mana hai (PASSWORD RULE) — ye step kabhi type nahi hoga.",
                PlanStop.LOGIN_2FA,
            )
        }
        // CI fix (compileDebugKotlin fail tha): nullable declared type
        // par Elvis smart-cast nahi laga, isliye do-step binding —
        // stepOrNull (nullable) se step (non-null, ?: return se).
        val stepOrNull: MacroStep? = when (action) {
            "open" -> if (label.isNotBlank()) MacroStep.Open(label) else null
            "tap" -> if (label.isNotBlank()) MacroStep.Tap(label) else null
            "type" -> if (text.isNotBlank()) MacroStep.Type(text) else null
            "wait" -> MacroStep.Wait((input["ms"] ?: "1000").toLongOrNull() ?: 1000L)
            "scroll" -> MacroStep.Scroll((input["forward"] ?: "true") == "true")
            "back" -> MacroStep.Back
            else -> null
        }
        val step = stepOrNull ?: return StepOutcome.Blocked(
            "Macro action '$action' samajh nahi aaya (open/tap/type/wait/scroll/back hi allowed).",
            PlanStop.GENUINE_BLOCK,
        )
        val lowerLabel = label.lowercase()
        if (action == "tap" && (lowerLabel.contains("delete") || lowerLabel.contains("remove"))) {
            return StepOutcome.Blocked(
                "Macro tap '$label' delete jaisa lag raha hai — ye stop #2 (delete/irreversible) hai, confirm ke bina nahi chalega.",
                PlanStop.DELETE_IRREVERSIBLE,
            )
        }
        if (action == "tap" && (lowerLabel.contains("login") || lowerLabel.contains("sign in"))) {
            return StepOutcome.Blocked(
                "Macro tap '$label' login jaisa lag raha hai — ye stop #3 (login/2FA) hai, user khud login karega.",
                PlanStop.LOGIN_2FA,
            )
        }
        val macro = Macro(input["macroName"] ?: "planner-macro", listOf(step))
        val jsonText = try {
            MacroJson.toJson(macro)
        } catch (e: Exception) {
            ""
        }
        return StepOutcome.AdapterQueued(
            jsonText,
            // B4 follow-up P1-1: ye note sirf fallback hai — engine delivery text ise
            // overwrite karta hai. "Macro ho gaya" wali bina-proof attestation hata di gayi:
            // VERIFIED sirf Routines runner ke Done log se hoga.
            "Macro taiyaar hai — planner ise Routines ke macro list me save karega; " +
                "wahan Chalao + confirm se chalega, phir planner me runner-log confirm dabao. " +
                "Planner khud accessibility drive nahi karta.",
        )
    }

    suspend fun execute(step: StoredStep, transient: Map<String, String>): StepOutcome {
        val input = step.input + transient
        // B5-08 builder entitlement gate (PlanFeatures.kt mechanism
        // reuse — key pehli baar yahan use hua hai). Fail-open:
        // list na mile to isAllowed true deta hai (purana
        // vyavhaar). List mile aur "builder" na ho to imaandaar
        // message + upgrade hint, silent block nahi. NOTE/MACRO
        // par gate nahi (local/adapter raaste hain).
        if (step.tool in builderTools && !PlanFeatures.isAllowed("builder")) {
            return StepOutcome.Blocked(
                "Builder feature abhi is plan me nahi hai — Builder (Pro) chahiye. " +
                    "Plan-features me 'builder' key aane ke baad dobara koshish karo.",
                PlanStop.GENUINE_BLOCK,
            )
        }
        // B5-07: har tool ka log ek jagah se (AutoLogStore, engine ke auditSink se).
        audit(step.tool.name, "execute")
        return when (step.tool) {
            PlanTool.NOTE -> StepOutcome.Ok("", step.title)
            PlanTool.MACRO_UI_STEP -> adaptMacro(input)
            PlanTool.PROJECT_SAVE_FILE -> saveProjectFile(input)
            PlanTool.PROJECT_READ_FILE -> readProjectFile(input)
            PlanTool.GITHUB_CREATE_REPO -> {
                if (!isConnected(step.tool)) {
                    return StepOutcome.Blocked(
                        "GitHub connected nahi hai — pehle ConnectServices se ek baar connect karo (grant-once flow wahi hai).",
                        PlanStop.LOGIN_2FA,
                    )
                }
                val name = input["name"] ?: ""
                if (name.isBlank()) return StepOutcome.Failure(BuilderFailureKind.BAD_REQUEST, "repo name khali hai")
                when (val res = github.createRepo(name, (input["private"] ?: "true") == "true")) {
                    is BuilderOutcome.Ok -> StepOutcome.Ok(res.value.fullName, "Repo bana: ${res.value.fullName}")
                    is BuilderOutcome.Failure -> StepOutcome.Failure(res.kind, res.detail)
                }
            }
            PlanTool.GITHUB_PUSH_FILE -> {
                if (!isConnected(step.tool)) {
                    return StepOutcome.Blocked(
                        "GitHub connected nahi hai — pehle ConnectServices se ek baar connect karo.",
                        PlanStop.LOGIN_2FA,
                    )
                }
                val owner = input["owner"] ?: ""
                val repo = input["repo"] ?: ""
                val path = input["path"] ?: ""
                val content = input["content"] ?: ""
                if (owner.isBlank() || repo.isBlank() || path.isBlank()) {
                    return StepOutcome.Failure(BuilderFailureKind.BAD_REQUEST, "owner/repo/path me se koi khali hai")
                }
                // DOC-P1-7 + GitHubClient.pushFile caller-zimmedari:
                // planner caller hi KeyInputGuard se guzra content
                // bhejega — key jaisa content yahan refuse hota hai
                // (BuilderScreen saveCurrent :98-107 wala pattern).
                if (KeyInputGuard.looksKeyLike(content)) {
                    return StepOutcome.Failure(
                        BuilderFailureKind.BAD_REQUEST,
                        KeyInputGuard.PROJECT_FILE_REFUSAL_MESSAGE,
                    )
                }
                // DOC-P0-2 allowlist: sirf builder-project repo par push.
                if ("github:$owner/$repo" !in allowedTargets()) {
                    return StepOutcome.Failure(
                        BuilderFailureKind.BAD_REQUEST,
                        "Ye repo builder-project ka nahi lag raha — push refuse hua. " +
                            "Pehle isi builder se repo banao (planner create step se), phir push karo. " +
                            BuilderHttp.dashboardFallback(BuilderService.GITHUB),
                    )
                }
                when (val res = github.pushFile(owner, repo, path, content, input["message"] ?: "Flora planner update")) {
                    is BuilderOutcome.Ok -> StepOutcome.Ok(res.value, "File push hui: $path")
                    is BuilderOutcome.Failure -> StepOutcome.Failure(res.kind, res.detail)
                }
            }
            PlanTool.VERCEL_CREATE_PROJECT -> {
                if (!isConnected(step.tool)) {
                    return StepOutcome.Blocked(
                        "Vercel connected nahi hai — pehle ConnectServices se ek baar connect karo.",
                        PlanStop.LOGIN_2FA,
                    )
                }
                val name = input["name"] ?: ""
                if (name.isBlank()) return StepOutcome.Failure(BuilderFailureKind.BAD_REQUEST, "project name khali hai")
                when (val res = vercel.createProject(name)) {
                    is BuilderOutcome.Ok -> StepOutcome.Ok(res.value.id, "Vercel project bana: ${res.value.name}")
                    is BuilderOutcome.Failure -> StepOutcome.Failure(res.kind, res.detail)
                }
            }
            PlanTool.VERCEL_DEPLOY -> {
                if (!isConnected(step.tool)) {
                    return StepOutcome.Blocked(
                        "Vercel connected nahi hai — pehle ConnectServices se ek baar connect karo.",
                        PlanStop.LOGIN_2FA,
                    )
                }
                val projectName = input["projectName"] ?: ""
                val files = input.filterKeys { it.startsWith("file:") }
                    .mapKeys { it.key.removePrefix("file:") }
                if (projectName.isBlank() || files.isEmpty()) {
                    return StepOutcome.Failure(BuilderFailureKind.BAD_REQUEST, "projectName ya files (file:<path>) khali hain")
                }
                // DOC-P0-2 allowlist: sirf builder-project par deploy.
                if ("vercel:$projectName" !in allowedTargets()) {
                    return StepOutcome.Failure(
                        BuilderFailureKind.BAD_REQUEST,
                        "Ye project builder-project ka nahi lag raha — deploy refuse hua. " +
                            "Pehle isi builder se Vercel project banao (planner create step se), phir deploy karo. " +
                            BuilderHttp.dashboardFallback(BuilderService.VERCEL),
                    )
                }
                // DOC-P0-1 pre-deploy gate (DOC-P1-6 ka mandatory
                // trigger — yehi ek raasta hai, doosra nahi):
                // createDeployment se PEHLE files map par text-only
                // scan. Critical security issue mile to deploy RUKE
                // + wajah — leak ke saath deploy KABHI nahi. Ye gate
                // builder-project deploy par hai; Flora ki apni
                // CI/combined deploy par gate NAHI lagaya gaya.
                // Report me sirf file:line + pattern naam — koi
                // secret value kabhi nahi.
                val preScan = DoctorScanner.scanFiles(files)
                val critical = preScan.issues.filter { issue ->
                    issue.category == DoctorCategory.SECURITY && issue.severity == DoctorSeverity.CRITICAL
                }
                if (critical.isNotEmpty()) {
                    val listed = critical.take(5).joinToString("; ") { issue ->
                        "${issue.file}:${issue.line} (${issue.pattern})"
                    }
                    return StepOutcome.Failure(
                        BuilderFailureKind.BAD_REQUEST,
                        "Doctor pre-deploy gate: deploy RUKA — critical security issue mile: $listed. " +
                            "Leak ke saath deploy kabhi nahi hoga — pehle Doctor screen se fix karo, phir deploy karo.",
                    )
                }
                when (val res = vercel.createDeployment(projectName, files)) {
                    is BuilderOutcome.Ok -> StepOutcome.Ok(res.value.id, "Deploy hua: ${res.value.url}")
                    is BuilderOutcome.Failure -> StepOutcome.Failure(res.kind, res.detail)
                }
            }
            PlanTool.SUPABASE_CREATE_PROJECT -> {
                if (!isConnected(step.tool)) {
                    return StepOutcome.Blocked(
                        "Supabase connected nahi hai — pehle ConnectServices se ek baar connect karo.",
                        PlanStop.LOGIN_2FA,
                    )
                }
                val name = input["name"] ?: ""
                val org = input["organizationSlug"] ?: ""
                val dbPassword = transient["dbPassword"] ?: ""
                if (name.isBlank() || org.isBlank()) {
                    return StepOutcome.Failure(BuilderFailureKind.BAD_REQUEST, "name/organizationSlug khali hai")
                }
                if (dbPassword.isBlank()) {
                    return StepOutcome.Blocked(
                        "Supabase project ke liye database password chahiye — plan amend se ek baar do " +
                            "(sirf is call ke liye, Room me kabhi save nahi hoga).",
                        PlanStop.LOGIN_2FA,
                    )
                }
                when (val res = supabase.createProject(name, org, dbPassword)) {
                    is BuilderOutcome.Ok -> StepOutcome.Ok(res.value.ref, "Supabase project bana: ${res.value.name}")
                    is BuilderOutcome.Failure -> StepOutcome.Failure(res.kind, res.detail)
                }
            }
            PlanTool.SUPABASE_RUN_SQL -> {
                if (!isConnected(step.tool)) {
                    return StepOutcome.Blocked(
                        "Supabase connected nahi hai — pehle ConnectServices se ek baar connect karo.",
                        PlanStop.LOGIN_2FA,
                    )
                }
                val ref = input["projectRef"] ?: ""
                val query = input["query"] ?: ""
                if (ref.isBlank() || query.isBlank()) {
                    return StepOutcome.Failure(BuilderFailureKind.BAD_REQUEST, "projectRef/query khali hai")
                }
                when (val res = supabase.runSql(ref, query, (input["readOnly"] ?: "true") == "true")) {
                    is BuilderOutcome.Ok -> StepOutcome.Ok(ref, "SQL chala (ref $ref)")
                    is BuilderOutcome.Failure -> StepOutcome.Failure(res.kind, res.detail)
                }
            }
        }
    }

    private suspend fun saveProjectFile(input: Map<String, String>): StepOutcome {
        var projectId = input["projectId"] ?: ""
        if (projectId.isBlank()) {
            val projectName = input["projectName"] ?: ""
            if (projectName.isBlank()) {
                return StepOutcome.Failure(BuilderFailureKind.BAD_REQUEST, "projectId ya projectName chahiye")
            }
            // B5-06 project-count gate (Free 3 projects) — auto-create
            // path par bhi, wahi engine gate jo BuilderScreen button par hai.
            val projectGateMsg = try {
                projectQuotaGate?.invoke(projectStore.listProjects().size)
            } catch (e: Exception) {
                null
            }
            if (projectGateMsg != null) {
                return StepOutcome.Failure(BuilderFailureKind.BAD_REQUEST, projectGateMsg)
            }
            val created = try {
                projectStore.createProject(projectName)
            } catch (e: Exception) {
                null
            } ?: return StepOutcome.Failure(BuilderFailureKind.SERVER, "project ban nahi paya")
            projectId = created.id
        }
        val fileName = input["fileName"] ?: ""
        val content = input["content"] ?: ""
        if (fileName.isBlank()) return StepOutcome.Failure(BuilderFailureKind.BAD_REQUEST, "fileName khali hai")
        // DOC-P1-7: BuilderScreen saveCurrent jaisa KeyInputGuard
        // check planner-save path par bhi — key jaisa content refuse.
        if (KeyInputGuard.looksKeyLike(content)) {
            return StepOutcome.Failure(
                BuilderFailureKind.BAD_REQUEST,
                KeyInputGuard.PROJECT_FILE_REFUSAL_MESSAGE,
            )
        }
        val saved = try {
            projectStore.saveProjectFile(projectId, fileName, content)
        } catch (e: Exception) {
            return StepOutcome.Failure(BuilderFailureKind.SERVER, "project file save nahi hua")
        }
        return if (saved.isSuccess) {
            StepOutcome.Ok(saved.getOrNull()?.id ?: projectId, "Project file save hui: $fileName")
        } else {
            StepOutcome.Failure(BuilderFailureKind.SERVER, "project file save nahi hui")
        }
    }

    private suspend fun readProjectFile(input: Map<String, String>): StepOutcome {
        val projectId = input["projectId"] ?: ""
        val fileId = input["fileId"] ?: ""
        if (projectId.isBlank() || fileId.isBlank()) {
            return StepOutcome.Failure(BuilderFailureKind.BAD_REQUEST, "projectId/fileId khali hai")
        }
        val read = try {
            projectStore.readProjectFile(projectId, fileId)
        } catch (e: Exception) {
            return StepOutcome.Failure(BuilderFailureKind.SERVER, "project file padh nahi payi")
        }
        return if (read.isSuccess) {
            StepOutcome.Ok(fileId, "Project file padhi: ${(read.getOrNull() ?: "").length} chars")
        } else {
            StepOutcome.Failure(BuilderFailureKind.SERVER, "project file padh nahi payi")
        }
    }

    /** Side-effect preview (M8) + external names (M10) ka display text. */
    fun previewText(step: StoredStep): String {
        val namePart = if (step.externalName.isNotBlank()) " Naam: ${step.externalName}." else ""
        return step.sideEffectPreview + namePart
    }

    /** files input (file:<path>) ko JSON se bharne ka helper — plan amend ke liye. */
    fun filesFromJson(raw: String): Map<String, String> {
        return try {
            val obj = JSONObject(raw)
            val out = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                out["file:$key"] = obj.optString(key, "")
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
