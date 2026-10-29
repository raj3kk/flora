package com.flora.app.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.flora.app.core.PlanFeatures
import com.flora.app.data.AutoLogStore
import com.flora.app.data.ProjectMeta
import com.flora.app.tools.KeyInputGuard
import com.flora.app.data.ProjectStore
import com.flora.app.data.extractFirstTextEntry
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * B2 Builder core — project editor + single-file preview + export + pick (2026-10-29).
 *
 * Coordinator ruling (binding) — preview option (a) ONLY: ek file ka
 * HTML preview isi screen ke apne WebView me (loadDataWithBaseURL).
 * Koi local server nahi, koi NanoHTTPD/Ktor nahi. Multi-file preview
 * (poora project ek saath) is build me NAHI hai — ye limit screen
 * par saaf likhi hai, aur multi-file preview ka faisla baad me
 * (licence-verify-first NanoHTTPD route ko apni approval chahiye).
 *
 * B1 browser se alag: ye WebView apna hai — BrowserController ka
 * singleton, uska pay lock, aur B1 ke read/fill/classify raaste
 * yahan kabhi nahi chhuaye jate (B2-09).
 *
 * Voice (B2-05): naya STT library nahi — system speech recognizer
 * (hi-IN), WhiteboardScreen wale pattern par apna launcher. Bola
 * hua text focused file ke editor me judta hai, phir wahi save
 * raasta chalta hai jo typed text ka hai (wahi encryption, wahi
 * version snapshot, wahi Auto Log). MainActivity ka mic launcher
 * chat-bound hai (uske result ko FloraApp chat me leta hai), isliye
 * builder ka apna launcher hai — wahi system recognizer, wahi bhasha.
 *
 * Errors (B2-12): chat/voice jaisi satah par sirf saaf-saadhi baat
 * (kya hua, aage kya karein); technical detail sirf Auto Log me,
 * aur usme bhi kabhi file content, path, key ya token nahi.
 * Ungated (coordinator ruling, B2-11): builder local-only hai —
 * koi server cost/network content nahi — isliye kisi invented
 * plan-features key/quota se gate nahi kiya gaya. Entitlement
 * B5 (F2) me aayega.
 */
@Composable
fun BuilderScreen(
    projectStore: ProjectStore,
    autoLogStore: AutoLogStore,
    quotaGate: (suspend (Int) -> String?)? = null,
) {
    val appContext = LocalContext.current
    val scope = rememberCoroutineScope()
    var projects by remember { mutableStateOf<List<ProjectMeta>>(emptyList()) }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var editorText by remember { mutableStateOf("") }
    var newProjectName by remember { mutableStateOf("") }
    var newFileName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    // B3: Services screen toggle — baaki saare states + saveCurrent
    // + launchers isse PEHLE declare hain (declaration-order trap
    // se bachne ke liye); screen swap LaunchedEffect ke baad hota hai.
    var showServices by remember { mutableStateOf(false) }

    val selectedProject = projects.firstOrNull { it.id == selectedProjectId }
    val selectedFile = selectedProject?.files?.firstOrNull { it.name == selectedFileName }
    val isHtmlFile = (selectedFileName ?: "").lowercase().endsWith(".html") ||
        (selectedFileName ?: "").lowercase().endsWith(".htm")

    // A3 P2-7: Auto Log fail par chupchaap catch nahi — har site
    // ko Boolean milta hai aur note me signal judta hai (doosra
    // log path nahi hai, isliye user-visible note hi signal hai).
    // Ye helper saveCurrent se PEHLE hai (saare uses iske baad).
    val logBuilder: suspend (String) -> Boolean = { entry ->
        try {
            autoLogStore.add(entry)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Save raasta — typed text ho ya bola hua text, yahi ek raasta
    // hai (wahi encryption, version snapshot, Auto Log).
    val saveCurrent: () -> Unit = {
        val pid = selectedProjectId
        val fname = selectedFileName
        if (pid == null || fname == null) {
            note = "Pehle koi project aur file chuno, phir save karo."
        } else if (KeyInputGuard.looksKeyLike(editorText)) {
            // B3-11: builder token/key kabhi project file me nahi
            // likha jata — save rok do, key hata kar dobara save
            // karne ko bolo (Services screen se jodna sahi raasta hai).
            note = KeyInputGuard.PROJECT_FILE_REFUSAL_MESSAGE
        } else {
            val textToSave = editorText
            scope.launch {
                val result = projectStore.saveProjectFile(pid, fname, textToSave)
                if (result.isSuccess) {
                    projects = projectStore.listProjects()
                    val logOk = logBuilder("Builder: file save hui ($fname)")
                    note = "Save ho gaya." + if (!logOk) " (Auto Log me entry nahi likh payi.)" else ""
                } else {
                    val kind = result.exceptionOrNull()?.javaClass?.simpleName ?: "error"
                    val logOk = logBuilder("Builder technical: save failed ($kind)")
                    note = "Save nahi ho paya — dobara koshish karo." + if (!logOk) " (Auto Log me entry nahi likh payi.)" else ""
                }
            }
        }
    }

    // Voice launcher — saare states + saveCurrent isse PEHLE declare
    // hain (declaration-order trap se bachne ke liye).
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull() ?: ""
            if (spoken.isBlank()) {
                note = "Kuch sunai nahi diya — dobara bolo."
            } else if (KeyInputGuard.looksKeyLike(spoken)) {
                // B3-06: boli hui key kabhi editor me nahi judti,
                // kabhi project file me save nahi hoti, kabhi
                // memory/brain prompt me nahi jati.
                note = KeyInputGuard.REFUSAL_MESSAGE
            } else if (selectedFileName == null || selectedProjectId == null) {
                note = "Pehle koi project aur file chuno, phir bolo."
            } else {
                editorText = if (editorText.isBlank()) spoken else editorText + "\n" + spoken
                saveCurrent()
                // A3 P2-2: save async hai — success ka daava tabhi
                // jab saveCurrent ka apna note aaye. Interim me
                // sirf neutral note (fail case me pehle success
                // note dikhna band).
                note = "Bola hua text jud gaya — save ho raha hai…"
            }
        } else {
            note = "Voice sun nahi paya — dobara koshish karo."
        }
    }

    // Single-file pick (B2-10, E2): GetContent — persistable URI
    // permission isme nahi hai (wo B5 ka kaam hai, yahan nahi).
    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            note = "Koi file chuni nahi gayi."
        } else {
            scope.launch {
                try {
                    val pickedName = uri.lastPathSegment
                        ?.substringAfterLast('/')
                        ?.take(100)
                        ?.ifBlank { null } ?: "picked.txt"
                    val stream = appContext.contentResolver.openInputStream(uri)
                    if (stream == null) {
                        note = "Ye file khul nahi payi — doosri file chuno."
                    } else {
                        // A3 P2-3: size check poora read KE BAAD nahi —
                        // bounded read (1 MB cap tak, usse aage ka
                        // chunk padha hi nahi jata) se badi pick
                        // memory me aane se pehle hi reject hoti hai.
                        // Limit wahi 1 MB hai, badla nahi.
                        val buf = ByteArrayOutputStream()
                        val chunk = ByteArray(8192)
                        var overLimit = false
                        stream.use { input ->
                            while (true) {
                                val n = input.read(chunk)
                                if (n < 0) break
                                if (buf.size() + n > 1_000_000) {
                                    overLimit = true
                                    break
                                }
                                buf.write(chunk, 0, n)
                            }
                        }
                        val bytes = buf.toByteArray()
                        // A3 P2-4: zip pehchan content se (magic bytes
                        // PK\x03\x04 / PK\x05\x06 / PK\x07\x08), sirf
                        // naam ke suffix se nahi — bina .zip wali
                        // content URI bhi extract hogi. Naam wala
                        // suffix fallback ke roop me barkaraar hai.
                        val looksZip = bytes.size >= 4 &&
                            bytes[0] == 'P'.code.toByte() &&
                            bytes[1] == 'K'.code.toByte() &&
                            ((bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()) ||
                                (bytes[2] == 0x05.toByte() && bytes[3] == 0x06.toByte()) ||
                                (bytes[2] == 0x07.toByte() && bytes[3] == 0x08.toByte()))
                        if (overLimit) {
                            note = "Ye file bahut badi hai — chhoti file chuno."
                        } else if (looksZip || pickedName.lowercase().endsWith(".zip")) {
                            val extracted = extractFirstTextEntry(bytes)
                            if (extracted == null) {
                                note = "Is zip me koi text file nahi mili."
                            } else if (KeyInputGuard.looksKeyLike(extracted.content)) {
                                // B3-11: chuni hui file me key jaisi
                                // cheez ho to project file me save nahi hoti.
                                note = KeyInputGuard.PROJECT_FILE_REFUSAL_MESSAGE
                            } else {
                                val pid = selectedProjectId ?: projectStore.createProject("Mera project").id
                                val saved = projectStore.saveProjectFile(pid, extracted.name, extracted.content)
                                if (saved.isSuccess) {
                                    projects = projectStore.listProjects()
                                    selectedProjectId = pid
                                    selectedFileName = extracted.name
                                    editorText = extracted.content
                                    val logOk = logBuilder("Builder: zip se file li gayi (${extracted.name})")
                                    note = "Zip se file mil gayi aur editor me khul gayi." + if (!logOk) " (Auto Log me entry nahi likh payi.)" else ""
                                } else {
                                    note = "Zip wali file save nahi ho payi — dobara koshish karo."
                                }
                            }
                        } else if (bytes.contains(0.toByte())) {
                            note = "Ye file text jaisi nahi lagi — text ya zip file chuno."
                        } else if (KeyInputGuard.looksKeyLike(String(bytes, Charsets.UTF_8))) {
                            // B3-11: chuni hui file me key jaisi
                            // cheez ho to project file me save nahi hoti.
                            note = KeyInputGuard.PROJECT_FILE_REFUSAL_MESSAGE
                        } else {
                            val content = String(bytes, Charsets.UTF_8)
                            val pid = selectedProjectId ?: projectStore.createProject("Mera project").id
                            val saved = projectStore.saveProjectFile(pid, pickedName, content)
                            if (saved.isSuccess) {
                                projects = projectStore.listProjects()
                                selectedProjectId = pid
                                selectedFileName = pickedName
                                editorText = content
                                val logOk = logBuilder("Builder: file chuni gayi ($pickedName)")
                                note = "File mil gayi aur editor me khul gayi." + if (!logOk) " (Auto Log me entry nahi likh payi.)" else ""
                            } else {
                                note = "Chuni hui file save nahi ho payi — dobara koshish karo."
                            }
                        }
                    }
                } catch (e: Exception) {
                    note = "File padhte waqt kuch gadbad hui — dobara koshish karo."
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        // A3 P2-7: load fail par khaali screen bina note ke nahi —
        // imaandaar note aayega. A3 P2-8: corrupt index par bhi
        // note (projects chhupenge, par overwrite nahi hoga).
        try {
            projects = projectStore.listProjects()
            if (projectStore.isIndexCorrupt()) {
                note = "Project index kharab lag raha hai — purane projects dikh nahi rahe; naya project/file save roka jayega taaki purana data overwrite na ho. Backup se restore karo."
            }
        } catch (e: Exception) {
            note = "Project list load nahi ho payi — dobara kholo."
        }
    }

    // B3 (R-B3-5): Services screen BuilderScreen se khulti hai —
    // saare hooks upar unconditional chal chuke hain, isliye ye
    // swap hook order nahi todta. FloraApp me iske liye koi
    // bada wiring nahi hai.
    if (showServices) {
        ConnectServicesScreen(autoLogStore) { showServices = false }
        return
    }

    // B5-08 builder entitlement gate (PlanFeatures mechanism reuse —
    // key pehli baar B5 me use hua hai; B2 ungated ruling isi se
    // supersede hoti hai). Fail-open: list na mile to isAllowed
    // true deta hai (purana vyavhaar). List mile aur "builder" na
    // ho to imaandaar message — silent block nahi. Ye block saare
    // hooks (states + launchers + LaunchedEffect) ke BAAD hai,
    // isliye hook order kabhi nahi toot-ta (showServices swap
    // wala pattern hi hai). Server hissa (F2: seed 14->15,
    // allowlist 14->15, panel editor) pending hai — key aane ke
    // baad test dobara hoga (audit log me limit darj hai).
    if (!PlanFeatures.isAllowed("builder")) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            Text("Builder abhi is plan me nahi hai.")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Builder (Pro) chahiye — plan-features me 'builder' key aane ke baad ye screen khulegi. Agar licence list abhi mili hi nahi hai to purana vyavhaar hi rahega (fail-open).")
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)
    ) {
        Text("Builder — project banao, code likho ya bolo, preview dekho.")
        Spacer(modifier = Modifier.height(4.dp))
        Text("Projects sirf is phone par encrypted save hote hain — file manager se nahi khulenge.")
        Spacer(modifier = Modifier.height(4.dp))
        Text("Preview abhi sirf ek file ka dikhta hai — poora project ek saath preview karna baad me aayega.")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { showServices = true }) { Text("Services jodo (GitHub / Vercel / Supabase)") }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = newProjectName, onValueChange = { newProjectName = it },
            label = { Text("Naye project ka naam") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                // B5-06 project-count gate (Free 3 projects) — engine
                // ka wahi gate jo planner auto-create path par hai.
                val gateMsg = try {
                    quotaGate?.invoke(projects.size)
                } catch (e: Exception) {
                    null
                }
                if (gateMsg != null) {
                    note = gateMsg
                } else {
                    // A3 P2-8: corrupt index par createProject throw
                    // karta hai — bina catch ke coroutine crash hota,
                    // isliye imaandaar note ke saath pakdo.
                    try {
                        val created = projectStore.createProject(newProjectName)
                        projects = projectStore.listProjects()
                        selectedProjectId = created.id
                        selectedFileName = null
                        editorText = ""
                        newProjectName = ""
                        val logOk = logBuilder("Builder: project bana (${created.name})")
                        note = "Project ban gaya." + if (!logOk) " (Auto Log me entry nahi likh payi.)" else ""
                    } catch (e: Exception) {
                        note = e.message ?: "Project ban nahi paya — dobara koshish karo."
                    }
                }
            }
        }) { Text("Naya project banao") }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Projects:")
        for (project in projects) {
            Row(modifier = Modifier.padding(top = 4.dp)) {
                Button(onClick = {
                    selectedProjectId = project.id
                    selectedFileName = null
                    editorText = ""
                }) { Text(if (selectedProjectId == project.id) "✓ ${project.name}" else project.name) }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (selectedProject != null) {
            OutlinedTextField(
                value = newFileName, onValueChange = { newFileName = it },
                label = { Text("Nayi file ka naam (jaise index.html)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val fname = newFileName.trim()
                if (fname.isBlank()) {
                    note = "Pehle file ka naam likho."
                } else {
                    scope.launch {
                        val saved = projectStore.saveProjectFile(selectedProject.id, fname, "")
                        if (saved.isSuccess) {
                            projects = projectStore.listProjects()
                            selectedFileName = fname
                            editorText = ""
                            newFileName = ""
                            val logOk = logBuilder("Builder: nayi file bani ($fname)")
                            note = "File ban gayi — ab isme likho ya bolo." + if (!logOk) " (Auto Log me entry nahi likh payi.)" else ""
                        } else {
                            note = "File ban nahi payi — dobara koshish karo."
                        }
                    }
                }
            }) { Text("Nayi file banao") }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Files:")
            for (file in selectedProject.files) {
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Button(onClick = {
                        scope.launch {
                            val content = projectStore.readProjectFile(selectedProject.id, file.id)
                            if (content.isSuccess) {
                                selectedFileName = file.name
                                editorText = content.getOrNull() ?: ""
                                note = "File khul gayi."
                            } else {
                                note = "Ye file khul nahi payi — shayad isme kuch gadbad hai."
                            }
                        }
                    }) { Text(if (selectedFileName == file.name) "✓ ${file.name}" else file.name) }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { pickLauncher.launch("*/*") }) { Text("File chuno (text ya zip)") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (selectedFile != null) {
            OutlinedTextField(
                value = editorText, onValueChange = { editorText = it },
                label = { Text(selectedFile.name) },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth().height(280.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = { saveCurrent() }, modifier = Modifier.padding(end = 4.dp)) { Text("Save karo") }
                Button(onClick = {
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bolo — jo code me jodna hai")
                        }
                        speechLauncher.launch(intent)
                    } catch (e: Exception) {
                        note = "Voice recognition is phone par nahi khul paya — text likh kar save karo."
                    }
                }, modifier = Modifier.padding(end = 4.dp)) { Text("🎤 Bolo aur jodo") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Preview (sirf is file ka):")
            Spacer(modifier = Modifier.height(4.dp))
            if (isHtmlFile) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            // B4-DOCTOR DOC-P2-1: BrowserScreen
                            // :453-460 hardened baseline ke barabar —
                            // explicit settings (pehle sirf JS on tha).
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
                                    // Local-only preview: bahar ka link kabhi nahi khulta.
                                    return true
                                }

                                // A3 P2-1 (R-A3-A3, network-block): JS ON
                                // rahega (interactive preview), par koi
                                // network subresource load nahi hoga —
                                // top-level block akela kaafi nahi tha
                                // (absolute https image/script load ho
                                // jata tha). Sirf main frame ka data
                                // URL guzarta hai, baaki sab block.
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    if (request?.isForMainFrame == true) {
                                        return super.shouldInterceptRequest(view, request)
                                    }
                                    return WebResourceResponse(
                                        "text/plain", "utf-8", ByteArray(0).inputStream()
                                    )
                                }
                            }
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(null, editorText, "text/html", "utf-8", null)
                    },
                    modifier = Modifier.fillMaxWidth().height(320.dp)
                )
            } else {
                Text(editorText)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = {
                val proj = selectedProject ?: return@Button
                scope.launch {
                    try {
                        val dir = File(appContext.cacheDir, "builder_exports")
                        if (!dir.exists()) dir.mkdirs()
                        // A3 P2-5 (D4): naam sanitize karo, kabhi
                        // drop mat karo — "/" wala naam builder_exports
                        // ke bahar subpath banata tha aur FileProvider
                        // share fail hota tha. Log me bhi sirf
                        // sanitized naam jayega (koi token nahi).
                        val safeName = proj.name
                            .map { ch -> if (ch == '/' || ch == '\\') '_' else ch }
                            .joinToString("").trim().ifBlank { "project" }
                        val outFile = File(dir, "$safeName-${System.currentTimeMillis()}.zip")
                        val result = projectStore.exportProjectZip(proj.id, outFile)
                        if (result.isSuccess) {
                            val uri = FileProvider.getUriForFile(
                                appContext,
                                appContext.packageName + ".fileprovider",
                                outFile
                            )
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            appContext.startActivity(Intent.createChooser(send, "Project share karo"))
                            val logOk = logBuilder("Builder: project export share sheet khola ($safeName)")
                            note = "Share sheet khul gaya — tum chuno kahan bhejna hai." + if (!logOk) " (Auto Log me entry nahi likh payi.)" else ""
                        } else {
                            note = "Export nahi ho paya — dobara koshish karo."
                        }
                    } catch (e: Exception) {
                        note = "Export nahi ho paya — dobara koshish karo."
                    }
                }
            }) { Text("Share karo (ZIP)") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(note)
    }
}
