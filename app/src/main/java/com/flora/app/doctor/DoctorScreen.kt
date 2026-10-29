package com.flora.app.doctor

import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.flora.app.data.AutoLogStore
import com.flora.app.data.ProjectMeta
import com.flora.app.data.ProjectStore
import com.flora.app.planner.PlannerEngine
import com.flora.app.planner.StoredPlan
import com.flora.app.planner.StoredStep
import kotlinx.coroutines.launch

/**
 * B4-DOCTOR screen (DOC-P1-3 / DOC-P1-4 / DOC-P1-5 / DOC-P1-6 / DOC-P1-8).
 *
 * Alag route (FloraApp Screen.DOCTOR) — PlannerScreen se sirf
 * navigation button aata hai, fix wiring yahin hai.
 *
 * Triggers (DOC-P1-6): (1) project kholte hi health check
 * (read-only auto-scan), (2) on-demand "Scan karo" button,
 * (3) deploy se pehle mandatory scan = PlannerTools ka
 * DOC-P0-1 gate (yahan doosra deploy raasta nahi banaya gaya).
 *
 * Fix semantics (DOC-P1-3): preview informational hai; "Fix
 * karo" tap hi command hai. Us tap par us session ke
 * non-destructive fixes turant lagte + save hote hain;
 * destructive fix (line/block hatana) har baar ek confirm
 * dialog ke baad hi lagta hai. Planner stops iska substitute
 * NAHI hain — wo plan steps ke hain, ye doctor fixes ke.
 *
 * Verify (DOC-P1-4): fix ke baad dobara scan + preview run;
 * 0 issues tabhi "theek" likha jata hai. Planner ka VERIFIED
 * status isse alag hai — wo plan steps ka hai, doctor issues
 * ka nahi (dono ko confuse nahi karna).
 *
 * Rollback (DOC-P1-5): ProjectStore versions ka reuse —
 * saveProjectFile ka snapshot + restoreVersion; ek tap wapas.
 *
 * Boundary (NAHI chhedega): Flora ka apna code (sirf batana +
 * plan, fix button yahan hai hi nahi uske liye), server/web/
 * supabase/functions, admin actions, secure settings. Doctor
 * sirf builder projects (user ke apps) ko theek karta hai.
 * Ungated (B2 ungated ruling ke samakaksh): doctor par koi
 * licence-gate nahi — licence-gate enforcement B2 ka nahi,
 * doctor ka bhi nahi; self-map B5 ka, backup B6 ka hai.
 *
 * Secret rule: issue list me file + line + pattern naam hi
 * dikhta hai — file ki line ka content, secret value kabhi
 * nahi. Preview WebView user ki apni file ka render hai
 * (BuilderScreen jaisa), log/report nahi.
 */
@Composable
fun DoctorScreen(
    projectStore: ProjectStore,
    autoLogStore: AutoLogStore,
    engine: PlannerEngine,
) {
    val scope = rememberCoroutineScope()
    var projects by remember { mutableStateOf<List<ProjectMeta>>(emptyList()) }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var fileContents by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var fileIds by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var issues by remember { mutableStateOf<List<DoctorIssue>>(emptyList()) }
    var complianceState by remember { mutableStateOf(ComplianceState.PENDING_CORPUS) }
    var note by remember { mutableStateOf("") }
    var verifyNote by remember { mutableStateOf("") }
    var pendingFix by remember { mutableStateOf<DoctorIssue?>(null) }
    var versionsByFile by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var plans by remember { mutableStateOf<List<StoredPlan>>(emptyList()) }
    var selectedPlanId by remember { mutableStateOf<String?>(null) }
    var failedSteps by remember { mutableStateOf<List<StoredStep>>(emptyList()) }
    var linkedProjectNote by remember { mutableStateOf("") }

    val selectedProject = projects.firstOrNull { it.id == selectedProjectId }
    val firstHtmlFile = fileContents.keys.firstOrNull { it.lowercase().endsWith(".html") || it.lowercase().endsWith(".htm") }

    val doScan: (String) -> Unit = { reason ->
        val result = DoctorScanner.scanFiles(fileContents)
        issues = result.issues
        complianceState = result.complianceState
        val critical = result.issues.count { it.category == DoctorCategory.SECURITY && it.severity == DoctorSeverity.CRITICAL }
        note = "Scan ($reason): ${result.issues.size} issues mile, unme $critical critical security."
        val pid = selectedProjectId
        if (pid != null) {
            scope.launch {
                engine.logDoctorEvent(pid, "scan ($reason): ${result.issues.size} issues, $critical critical security")
            }
        }
    }

    val loadProject: (String) -> Unit = { pid ->
        scope.launch {
            val all = projectStore.listProjects()
            projects = all
            val project = all.firstOrNull { it.id == pid }
            if (project == null) {
                note = "Project nahi mila."
            } else {
                val contents = mutableMapOf<String, String>()
                val ids = mutableMapOf<String, String>()
                val versions = mutableMapOf<String, List<String>>()
                for (file in project.files) {
                    val read = projectStore.readProjectFile(pid, file.id)
                    if (read.isSuccess) {
                        contents[file.name] = read.getOrNull() ?: ""
                        ids[file.name] = file.id
                        versions[file.name] = projectStore.listFileVersions(pid, file.id)
                    }
                }
                fileContents = contents
                fileIds = ids
                versionsByFile = versions
                verifyNote = ""
                // Trigger (1): project open par health check (read-only scan).
                val result = DoctorScanner.scanFiles(contents)
                issues = result.issues
                complianceState = result.complianceState
                note = "Project khula — health check: ${result.issues.size} issues mile."
                engine.logDoctorEvent(pid, "open health check: ${result.issues.size} issues")
                plans = engine.listPlans()
                val linked = selectedPlanId
                if (linked != null) {
                    failedSteps = engine.failedSteps(linked)
                }
            }
        }
    }

    val doApplyFix: (DoctorIssue) -> Unit = { issue ->
        val pid = selectedProjectId
        if (pid == null) {
            note = "Pehle koi project chuno."
        } else {
            val fixed = DoctorScanner.applyFix(issue, fileContents)
            if (fixed == null) {
                note = "Is issue ka auto-fix nahi hai — insaan ko tay karna hoga."
            } else {
                scope.launch {
                    var savedOk = true
                    for ((fileName, content) in fixed) {
                        if (fileContents[fileName] != content) {
                            val saved = projectStore.saveProjectFile(pid, fileName, content)
                            if (!saved.isSuccess) savedOk = false
                        }
                    }
                    if (!savedOk) {
                        note = "Fix save nahi ho paya — dobara koshish karo."
                    } else {
                        // Verify (DOC-P1-4): dobara scan + preview run.
                        val result = DoctorScanner.scanFiles(fixed)
                        fileContents = fixed
                        issues = result.issues
                        complianceState = result.complianceState
                        verifyNote = if (result.issues.isEmpty()) {
                            "Verify: dobara scan me 0 issues — theek ho gaya."
                        } else {
                            "Verify: dobara scan me ${result.issues.size} issues bache hain."
                        }
                        engine.logDoctorEvent(pid, "fix (${issue.pattern}, ${issue.file}:${issue.line}): verify me ${result.issues.size} issues bache")
                        try {
                            autoLogStore.add("[doctor:$pid] fix lagaya (${issue.pattern})")
                        } catch (e: Exception) {
                        }
                        // Versions dobara load (fix ne naya snapshot banaya).
                        val project = projectStore.listProjects().firstOrNull { it.id == pid }
                        if (project != null) {
                            val versions = mutableMapOf<String, List<String>>()
                            for (file in project.files) {
                                versions[file.name] = projectStore.listFileVersions(pid, file.id)
                            }
                            versionsByFile = versions
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            projects = projectStore.listProjects()
        } catch (e: Exception) {
        }
        try {
            plans = engine.listPlans()
        } catch (e: Exception) {
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)
    ) {
        Text("Doctor — project ki jaanch, fix preview, verify, rollback")
        Spacer(modifier = Modifier.height(4.dp))
        Text("Doctor sirf builder projects ko theek karta hai — Flora ka apna code, server, admin ya secure settings kabhi nahi chhedega.")
        Spacer(modifier = Modifier.height(4.dp))
        Text("Pre-deploy gate: critical security issue (jaise secret leak) ho to deploy rukta hai — leak ke saath deploy kabhi nahi.")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Projects:")
        for (project in projects) {
            Row(modifier = Modifier.padding(top = 4.dp)) {
                Button(onClick = {
                    selectedProjectId = project.id
                    loadProject(project.id)
                }) { Text(if (selectedProjectId == project.id) "✓ ${project.name}" else project.name) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (selectedProject != null) {
            Row {
                Button(onClick = { doScan("on-demand") }) { Text("Scan karo") }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(note)
            if (verifyNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(verifyNote)
            }
            if (complianceState == ComplianceState.PENDING_CORPUS) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Compliance scan: corpus pending — scan nahi chal sakta (jhootha scan nahi dikhaya ja raha). Corpus mile to fix = hatana/replace, kabhi chhupana nahi.")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Issues (${issues.size}):")
            for (issue in issues) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("${issue.category} / ${issue.severity} — ${issue.file}:${issue.line}")
                Text("Kya: ${issue.title}")
                Text("Kyun: ${issue.why}")
                Text("Asar: ${issue.impact}")
                val preview = DoctorScanner.previewFix(issue)
                if (preview != null) {
                    Text("Fix preview — Badlega: ${preview.change}")
                    Text("NAHI chhedega: ${preview.notTouched}")
                    Text("Risk: ${preview.risk}")
                    Text("Wapas: ${preview.rollback}")
                    Row {
                        Button(onClick = {
                            if (preview.destructive) {
                                pendingFix = issue
                            } else {
                                doApplyFix(issue)
                            }
                        }) { Text("Fix karo") }
                    }
                } else {
                    Text("Fix preview: is issue ka auto-fix nahi hai — tum khud editor me theek karo, phir Scan karo.")
                }
            }
            val fixToConfirm = pendingFix
            if (fixToConfirm != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Destructive fix confirm karo: ${fixToConfirm.file}:${fixToConfirm.line} (pattern: ${fixToConfirm.pattern})")
                Text("Ye line/block hat jayegi — wapas rollback se hoga, par pehle confirm zaroori hai.")
                Row {
                    Button(onClick = {
                        pendingFix = null
                        doApplyFix(fixToConfirm)
                    }) { Text("Haan, fix karo") }
                    OutlinedButton(onClick = { pendingFix = null }) { Text("Nahi, rehne do") }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Rollback (ek tap wapas):")
            for ((fileName, versions) in versionsByFile) {
                if (versions.isNotEmpty()) {
                    Text("$fileName ke versions:")
                    for (version in versions) {
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            OutlinedButton(onClick = {
                                val pid = selectedProjectId
                                val fid = fileIds[fileName]
                                if (pid != null && fid != null) {
                                    scope.launch {
                                        val restored = projectStore.restoreVersion(pid, fid, version)
                                        if (restored.isSuccess) {
                                            loadProject(pid)
                                            engine.logDoctorEvent(pid, "rollback: $fileName -> $version")
                                            verifyNote = "Rollback ho gaya ($fileName -> $version) — dobara scan chal raha hai."
                                        } else {
                                            note = "Rollback nahi ho paya — dobara koshish karo."
                                        }
                                    }
                                }
                            }) { Text("$version par wapas lao") }
                        }
                    }
                }
            }
            if (firstHtmlFile != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Preview run (sirf is file ka): $firstHtmlFile")
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.allowFileAccessFromFileURLs = false
                            settings.allowUniversalAccessFromFileURLs = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.safeBrowsingEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    return true
                                }
                            }
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(null, fileContents[firstHtmlFile] ?: "", "text/html", "utf-8", null)
                    },
                    modifier = Modifier.fillMaxWidth().height(320.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Plan se jodo (plan <-> project link):")
            for (plan in plans) {
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    OutlinedButton(onClick = {
                        selectedPlanId = plan.planId
                        scope.launch {
                            val pid = selectedProjectId
                            if (pid != null) {
                                engine.linkPlanToProject(plan.planId, pid)
                                linkedProjectNote = "Plan '${plan.goal}' is project se jud gaya."
                                failedSteps = engine.failedSteps(plan.planId)
                            }
                        }
                    }) { Text(if (selectedPlanId == plan.planId) "✓ ${plan.goal}" else plan.goal) }
                }
            }
            if (linkedProjectNote.isNotBlank()) {
                Text(linkedProjectNote)
            }
            if (failedSteps.isNotEmpty()) {
                Text("Us plan ke fail/blocked steps (doctor reader):")
                for (step in failedSteps) {
                    val kind = selectedPlanId?.let { engine.lastFailureKind(it, step.stepIndex) }
                    Text("- ${step.title} [${step.status}] kind=${kind ?: "session me nahi"} — ${step.resultText}")
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Doctor ka log Auto Log me [doctor:<projectId> ...] se aur Room doctor_events me rehta hai — sirf AutoLog par nirbhar nahi.")
    }
}
