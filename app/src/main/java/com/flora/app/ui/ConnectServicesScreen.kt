package com.flora.app.ui

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.flora.app.data.AutoLogStore
import com.flora.app.data.BuilderAuthKind
import com.flora.app.data.BuilderService
import com.flora.app.data.FloraPreferences
import com.flora.app.tools.BuilderHttp
import com.flora.app.tools.BuilderHosts
import com.flora.app.tools.BuilderOAuth
import com.flora.app.tools.BuilderOutcome
import com.flora.app.tools.ClipboardCapture
import com.flora.app.tools.GitHubClient
import com.flora.app.tools.KeyInputGuard
import com.flora.app.tools.SupabaseProjectClient
import com.flora.app.tools.VercelClient
import kotlinx.coroutines.launch

/**
 * B3 connect UI (R-B3-5: NAYA screen file, BuilderScreen se
 * khulta hai; FloraApp me iska koi bada wiring nahi hai).
 *
 * Teen raaste (plan ke auth options):
 * - (A) GUIDED LOGIN: GitHub + Supabase ke liye dedicated
 *   WebView (B1 shared controller kabhi nahi — apna instance,
 *   B1 hardening settings mirror). User apne haathon se login
 *   karta hai; Flora sirf redirect URL se OAuth code pakadti
 *   hai — page content, password field, cookie kabhi nahi
 *   padhe jate (WebView se koi content/cookie read nahi hota).
 *   Client ID + redirect URI user apne OAuth app se
 *   daalta hai (dashboard step, Phase 9/web track) — koi
 *   redirect URI fabricate nahi hota, bina asli grant ke live
 *   OAuth proof ka daava nahi (R-B3-1, Phase 10).
 * - (B) MANUAL INTEGRATION: secure paste field (masked) —
 *   voice/chat se key kabhi nahi li jati (KeyInputGuard).
 * - (D) CLIPBOARD CAPTURE: "clipboard se key loon?" prompt ke
 *   baad hi foreground read (system read-toast dikhega),
 *   shape-validate, encrypted save, auto-clear (opt-out ke
 *   saath). Cookies kabhi nahi.
 * Vercel: scope-string/OAuth authorize model hi nahi hai —
 * dashboard Full Access token hi raasta hai (guide + manual /
 * clipboard); yahan koi Vercel authorize endpoint invent nahi
 * hua hai.
 *
 * Chat/TTS rule (R-8): is screen par bhi full key kabhi nahi
 * dikhti — save ke baad paste field turant khaali, status me
 * sirf "connected" hota hai. Detailed wajah (HTTP kind) sirf
 * isi Builder auth UI me hoti hai, chat/voice me sirf warm
 * line jati hai.
 *
 * Reinstall note (R-B3-6): reinstall/data-clear ke baad
 * dobara grant karna expected hai — ye grant-once ka fail nahi.
 */
@Composable
fun ConnectServicesScreen(autoLogStore: AutoLogStore, onBack: () -> Unit) {
    val appContext = LocalContext.current
    val prefs = remember { FloraPreferences(appContext) }
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var connectedGithub by remember { mutableStateOf(false) }
    var connectedVercel by remember { mutableStateOf(false) }
    var connectedSupabase by remember { mutableStateOf(false) }
    var autoClear by remember { mutableStateOf(true) }
    var pasteOpenGithub by remember { mutableStateOf(false) }
    var pasteOpenVercel by remember { mutableStateOf(false) }
    var pasteOpenSupabase by remember { mutableStateOf(false) }
    var pasteGithub by remember { mutableStateOf("") }
    var pasteVercel by remember { mutableStateOf("") }
    var pasteSupabase by remember { mutableStateOf("") }
    var supabaseRef by remember { mutableStateOf("") }
    var clipAskGithub by remember { mutableStateOf(false) }
    var clipAskVercel by remember { mutableStateOf(false) }
    var clipAskSupabase by remember { mutableStateOf(false) }
    var oauthClientId by remember { mutableStateOf("") }
    var oauthRedirect by remember { mutableStateOf("") }
    var oauthUrl by remember { mutableStateOf<String?>(null) }
    var oauthService by remember { mutableStateOf<BuilderService?>(null) }
    var oauthVerifier by remember { mutableStateOf("") }
    var oauthCode by remember { mutableStateOf<String?>(null) }

    fun log(msg: String) {
        scope.launch {
            try {
                autoLogStore.add(msg)
            } catch (e: Exception) {
            }
        }
    }

    fun refreshFlags() {
        scope.launch {
            try {
                connectedGithub = prefs.builderConnected(BuilderService.GITHUB)
                connectedVercel = prefs.builderConnected(BuilderService.VERCEL)
                connectedSupabase = prefs.builderConnected(BuilderService.SUPABASE)
                autoClear = prefs.builderClipboardAutoClear()
            } catch (e: Exception) {
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshFlags()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)
    ) {
        Button(onClick = { onBack() }) { Text("← Builder wapas") }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Services jodo — GitHub, Vercel, Supabase")
        Spacer(modifier = Modifier.height(4.dp))
        Text("Ek baar grant ke baad dobara prompt nahi aayega (grant-once). Token sirf is phone par encrypted rehta hai, kabhi log/chat me nahi jata.")
        Spacer(modifier = Modifier.height(4.dp))
        Text("Reinstall ya data clear ke baad dobara grant karna expected hai — ye koi bug nahi hai.")
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            Text("Clipboard save ke baad auto-clear ho: ")
            Switch(
                checked = autoClear,
                onCheckedChange = { value ->
                    autoClear = value
                    scope.launch {
                        try {
                            prefs.setBuilderClipboardAutoClear(value)
                        } catch (e: Exception) {
                        }
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (note.isNotEmpty()) {
            Text(note)
            Spacer(modifier = Modifier.height(4.dp))
        }
        if (detail.isNotEmpty()) {
            Text(detail)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---- GitHub ----
        Text("GitHub — full repo scope (repo)")
        Text("Guide: GitHub dashboard par apna OAuth app banao (ya classic/fine-grained token banao), phir yahan jodo. Scope `repo` = public + private repos par full access.")
        Text(if (connectedGithub) "Status: connected" else "Status: not connected")
        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = { pasteOpenGithub = !pasteOpenGithub }) { Text("Manual paste se jodo") }
        if (pasteOpenGithub) {
            OutlinedTextField(
                value = pasteGithub, onValueChange = { pasteGithub = it },
                label = { Text("GitHub key/token (paste karo)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                val value = pasteGithub.trim()
                val error = KeyInputGuard.validateForService(BuilderService.GITHUB, value)
                if (error != null) {
                    note = error
                    detail = "Dobara copy karke phir se koshish karo."
                } else {
                    pasteGithub = ""
                    pasteOpenGithub = false
                    scope.launch {
                        val saved = try {
                            prefs.setBuilderToken(BuilderService.GITHUB, value, BuilderAuthKind.PAT)
                        } catch (e: Exception) {
                            false
                        }
                        if (saved) {
                            log("Builder services: GitHub connected (manual)")
                            note = "GitHub jud gaya."
                            detail = ""
                        } else {
                            note = "Save nahi ho paya — dobara koshish karo."
                            detail = ""
                        }
                        refreshFlags()
                    }
                }
            }) { Text("Save karo") }
        }
        Button(onClick = { clipAskGithub = true }) { Text("Clipboard se lo") }
        if (clipAskGithub) {
            Text("Clipboard se key loon?")
            Row {
                Button(onClick = {
                    clipAskGithub = false
                    scope.launch {
                        val result = ClipboardCapture.captureFromClipboard(
                            appContext, prefs, BuilderService.GITHUB, true
                        )
                        when (result) {
                            is ClipboardCapture.Result.Saved -> {
                                log("Builder services: GitHub connected (clipboard)")
                                note = "GitHub jud gaya — clipboard clear ho gaya hai."
                                detail = ""
                            }
                            is ClipboardCapture.Result.Invalid -> {
                                note = result.message
                                detail = "Dobara copy karke phir se koshish karo."
                            }
                            is ClipboardCapture.Result.Empty -> {
                                note = "Clipboard khaali hai — pehle key copy karo."
                                detail = ""
                            }
                            else -> {
                                note = "Save nahi ho paya — dobara koshish karo."
                                detail = ""
                            }
                        }
                        refreshFlags()
                    }
                }, modifier = Modifier.padding(end = 4.dp)) { Text("Haan, lo") }
                Button(onClick = { clipAskGithub = false }) { Text("Nahi") }
            }
            Text("Note: padhte waqt Android ka clipboard-read toast dikhega — kuch chhupa hua nahi hota.")
        }
        Button(onClick = {
            oauthService = BuilderService.GITHUB
            oauthCode = null
            oauthUrl = null
        }) { Text("Guided login (OAuth)") }
        Button(onClick = {
            scope.launch {
                val client = GitHubClient(prefs)
                when (val res = client.whoAmI()) {
                    is BuilderOutcome.Ok -> {
                        note = "GitHub connection theek hai."
                        detail = "User: " + res.value
                    }
                    is BuilderOutcome.Failure -> {
                        note = BuilderHttp.warmMessage(res.kind)
                        detail = BuilderHttp.dashboardFallback(BuilderService.GITHUB)
                    }
                }
            }
        }) { Text("Connection check karo") }
        if (connectedGithub) {
            Button(onClick = {
                scope.launch {
                    try {
                        prefs.disconnectBuilder(BuilderService.GITHUB)
                    } catch (e: Exception) {
                    }
                    log("Builder services: GitHub disconnected")
                    note = "GitHub disconnect ho gaya — agli baar phir se jodna hoga."
                    detail = ""
                    refreshFlags()
                }
            }) { Text("Disconnect karo") }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // ---- Vercel ----
        Text("Vercel — full project scope (Full Access token)")
        Text("Guide: Vercel dashboard (Account Settings → Tokens) par Full Access token banao, phir yahan manual paste ya clipboard se jodo. Vercel ka OAuth authorize raasta nahi hai — token hi credential hai.")
        Text(if (connectedVercel) "Status: connected" else "Status: not connected")
        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = { pasteOpenVercel = !pasteOpenVercel }) { Text("Manual paste se jodo") }
        if (pasteOpenVercel) {
            OutlinedTextField(
                value = pasteVercel, onValueChange = { pasteVercel = it },
                label = { Text("Vercel token (paste karo)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                val value = pasteVercel.trim()
                val error = KeyInputGuard.validateForService(BuilderService.VERCEL, value)
                if (error != null) {
                    note = error
                    detail = "Dobara copy karke phir se koshish karo."
                } else {
                    pasteVercel = ""
                    pasteOpenVercel = false
                    scope.launch {
                        val saved = try {
                            prefs.setBuilderToken(BuilderService.VERCEL, value, BuilderAuthKind.PAT)
                        } catch (e: Exception) {
                            false
                        }
                        if (saved) {
                            log("Builder services: Vercel connected (manual)")
                            note = "Vercel jud gaya."
                            detail = ""
                        } else {
                            note = "Save nahi ho paya — dobara koshish karo."
                            detail = ""
                        }
                        refreshFlags()
                    }
                }
            }) { Text("Save karo") }
        }
        Button(onClick = { clipAskVercel = true }) { Text("Clipboard se lo") }
        if (clipAskVercel) {
            Text("Clipboard se key loon?")
            Row {
                Button(onClick = {
                    clipAskVercel = false
                    scope.launch {
                        val result = ClipboardCapture.captureFromClipboard(
                            appContext, prefs, BuilderService.VERCEL, true
                        )
                        when (result) {
                            is ClipboardCapture.Result.Saved -> {
                                log("Builder services: Vercel connected (clipboard)")
                                note = "Vercel jud gaya — clipboard clear ho gaya hai."
                                detail = ""
                            }
                            is ClipboardCapture.Result.Invalid -> {
                                note = result.message
                                detail = "Dobara copy karke phir se koshish karo."
                            }
                            is ClipboardCapture.Result.Empty -> {
                                note = "Clipboard khaali hai — pehle token copy karo."
                                detail = ""
                            }
                            else -> {
                                note = "Save nahi ho paya — dobara koshish karo."
                                detail = ""
                            }
                        }
                        refreshFlags()
                    }
                }, modifier = Modifier.padding(end = 4.dp)) { Text("Haan, lo") }
                Button(onClick = { clipAskVercel = false }) { Text("Nahi") }
            }
        }
        Button(onClick = {
            scope.launch {
                val client = VercelClient(prefs)
                when (val res = client.whoAmI()) {
                    is BuilderOutcome.Ok -> {
                        note = "Vercel connection theek hai."
                        detail = "User: " + res.value
                    }
                    is BuilderOutcome.Failure -> {
                        note = BuilderHttp.warmMessage(res.kind)
                        detail = BuilderHttp.dashboardFallback(BuilderService.VERCEL)
                    }
                }
            }
        }) { Text("Connection check karo") }
        if (connectedVercel) {
            Button(onClick = {
                scope.launch {
                    try {
                        prefs.disconnectBuilder(BuilderService.VERCEL)
                    } catch (e: Exception) {
                    }
                    log("Builder services: Vercel disconnected")
                    note = "Vercel disconnect ho gaya — agli baar phir se jodna hoga."
                    detail = ""
                    refreshFlags()
                }
            }) { Text("Disconnect karo") }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // ---- Supabase (user ka apna project) ----
        Text("Supabase — user ka apna project (Flora ke project se alag)")
        Text("Guide: Supabase dashboard par apna OAuth app banao (scopes: projects + database manage), ya Management API token/PAT banao. Project ref bhi daalo — Flora ke apne project ka URL yahan kabhi use nahi hota.")
        Text(if (connectedSupabase) "Status: connected" else "Status: not connected")
        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = { pasteOpenSupabase = !pasteOpenSupabase }) { Text("Manual paste se jodo") }
        if (pasteOpenSupabase) {
            OutlinedTextField(
                value = pasteSupabase, onValueChange = { pasteSupabase = it },
                label = { Text("Supabase token (paste karo)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = supabaseRef, onValueChange = { supabaseRef = it },
                label = { Text("Project ref (jaise apne project ka ref)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                val value = pasteSupabase.trim()
                val ref = supabaseRef.trim()
                val error = KeyInputGuard.validateForService(BuilderService.SUPABASE, value)
                if (error != null) {
                    note = error
                    detail = "Dobara copy karke phir se koshish karo."
                } else {
                    pasteSupabase = ""
                    pasteOpenSupabase = false
                    scope.launch {
                        val baseUrl = if (ref.isEmpty()) "" else BuilderHosts.userProjectBaseUrl(ref)
                        val saved = try {
                            prefs.setBuilderToken(
                                BuilderService.SUPABASE, value, BuilderAuthKind.PAT,
                                baseUrl = baseUrl, projectRef = ref
                            )
                        } catch (e: Exception) {
                            false
                        }
                        if (saved) {
                            log("Builder services: Supabase connected (manual)")
                            note = "Supabase jud gaya."
                            detail = ""
                        } else {
                            note = "Save nahi ho paya — dobara koshish karo."
                            detail = ""
                        }
                        refreshFlags()
                    }
                }
            }) { Text("Save karo") }
        }
        Button(onClick = { clipAskSupabase = true }) { Text("Clipboard se lo") }
        if (clipAskSupabase) {
            Text("Clipboard se key loon?")
            Row {
                Button(onClick = {
                    clipAskSupabase = false
                    scope.launch {
                        val result = ClipboardCapture.captureFromClipboard(
                            appContext, prefs, BuilderService.SUPABASE, true
                        )
                        when (result) {
                            is ClipboardCapture.Result.Saved -> {
                                log("Builder services: Supabase connected (clipboard)")
                                note = "Supabase jud gaya — clipboard clear ho gaya hai."
                                detail = ""
                            }
                            is ClipboardCapture.Result.Invalid -> {
                                note = result.message
                                detail = "Dobara copy karke phir se koshish karo."
                            }
                            is ClipboardCapture.Result.Empty -> {
                                note = "Clipboard khaali hai — pehle token copy karo."
                                detail = ""
                            }
                            else -> {
                                note = "Save nahi ho paya — dobara koshish karo."
                                detail = ""
                            }
                        }
                        refreshFlags()
                    }
                }, modifier = Modifier.padding(end = 4.dp)) { Text("Haan, lo") }
                Button(onClick = { clipAskSupabase = false }) { Text("Nahi") }
            }
        }
        Button(onClick = {
            oauthService = BuilderService.SUPABASE
            oauthCode = null
            oauthUrl = null
        }) { Text("Guided login (OAuth)") }
        Button(onClick = {
            scope.launch {
                val client = SupabaseProjectClient(prefs)
                when (val res = client.whoAmI()) {
                    is BuilderOutcome.Ok -> {
                        note = "Supabase connection theek hai."
                        detail = "Projects mile: " + res.value
                    }
                    is BuilderOutcome.Failure -> {
                        note = BuilderHttp.warmMessage(res.kind)
                        detail = BuilderHttp.dashboardFallback(BuilderService.SUPABASE)
                    }
                }
            }
        }) { Text("Connection check karo") }
        if (connectedSupabase) {
            Button(onClick = {
                scope.launch {
                    try {
                        prefs.disconnectBuilder(BuilderService.SUPABASE)
                    } catch (e: Exception) {
                    }
                    log("Builder services: Supabase disconnected")
                    note = "Supabase disconnect ho gaya — agli baar phir se jodna hoga."
                    detail = ""
                    refreshFlags()
                }
            }) { Text("Disconnect karo") }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // ---- Guided login (GitHub / Supabase) ----
        if (oauthService != null) {
            val service = oauthService!!
            Text("Guided login — " + if (service == BuilderService.GITHUB) "GitHub" else "Supabase")
            Text("Apne OAuth app ki client ID aur redirect URI daalo (dashboard se mile hain). Flora sirf redirect se code legi — tumhara password Flora kabhi nahi dekhegi, na page ka content padhegi.")
            OutlinedTextField(
                value = oauthClientId, onValueChange = { oauthClientId = it },
                label = { Text("OAuth client ID") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = oauthRedirect, onValueChange = { oauthRedirect = it },
                label = { Text("Redirect URI (apne OAuth app wala)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                val pair = BuilderOAuth.newPkcePair()
                oauthVerifier = pair.verifier
                val endpoint = if (service == BuilderService.GITHUB) {
                    BuilderOAuth.GITHUB_AUTHORIZE_URL
                } else {
                    BuilderOAuth.SUPABASE_AUTHORIZE_URL
                }
                val scopeText = if (service == BuilderService.GITHUB) {
                    BuilderOAuth.GITHUB_SCOPE
                } else {
                    BuilderOAuth.SUPABASE_SCOPE
                }
                oauthUrl = BuilderOAuth.buildAuthorizeUrl(
                    endpoint, oauthClientId.trim(), oauthRedirect.trim(),
                    scopeText, pair.challenge, "flora-b3"
                )
                oauthCode = null
                note = "Apne haathon se login karo — Flora sirf wapas aane wale code ko pakdegi."
                detail = ""
            }) { Text("Login kholo") }
            TextButton(onClick = {
                oauthService = null
                oauthUrl = null
                oauthCode = null
            }) { Text("Guided login band karo") }
        }
        if (oauthUrl != null && oauthService != null) {
            val redirect = oauthRedirect.trim()
            val targetUrl = oauthUrl!!
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
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
                                val url = request?.url?.toString() ?: return false
                                val code = BuilderOAuth.parseCodeFromRedirect(url, redirect)
                                if (code != null) {
                                    oauthCode = code
                                    oauthUrl = null
                                    return true
                                }
                                return false
                            }
                        }
                        loadUrl(targetUrl)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(420.dp)
            )
        }
        if (oauthCode != null && oauthService != null) {
            val service = oauthService!!
            val code = oauthCode!!
            Text("Login code mil gaya — ab token lo (code screen par kabhi poora nahi dikhaya jata).")
            Button(onClick = {
                scope.launch {
                    val outcome = if (service == BuilderService.GITHUB) {
                        when (val res = GitHubClient(prefs).exchangeCodeForToken(
                            oauthClientId.trim(), code, oauthVerifier, oauthRedirect.trim()
                        )) {
                            is BuilderOutcome.Ok -> {
                                val saved = try {
                                    prefs.setBuilderToken(
                                        BuilderService.GITHUB, res.value.accessToken,
                                        BuilderAuthKind.OAUTH, res.value.refreshToken
                                    )
                                } catch (e: Exception) {
                                    false
                                }
                                if (saved) "ok" else "save-fail"
                            }
                            is BuilderOutcome.Failure -> "fail:" + res.kind.name
                        }
                    } else {
                        when (val res = SupabaseProjectClient(prefs).exchangeCodeForToken(
                            oauthClientId.trim(), code, oauthVerifier, oauthRedirect.trim()
                        )) {
                            is BuilderOutcome.Ok -> {
                                val saved = try {
                                    prefs.setBuilderToken(
                                        BuilderService.SUPABASE, res.value.accessToken,
                                        BuilderAuthKind.OAUTH, res.value.refreshToken
                                    )
                                } catch (e: Exception) {
                                    false
                                }
                                if (saved) "ok" else "save-fail"
                            }
                            is BuilderOutcome.Failure -> "fail:" + res.kind.name
                        }
                    }
                    if (outcome == "ok") {
                        log("Builder services: connected via guided login")
                        note = "Guided login se jud gaya."
                        detail = ""
                        oauthCode = null
                        oauthService = null
                    } else if (outcome == "save-fail") {
                        note = "Token save nahi ho paya — dobara koshish karo."
                        detail = ""
                    } else {
                        note = "Login poora nahi ho paya — manual paste ya clipboard se jodo."
                        detail = "Wajah: " + outcome +
                            ". Dashboard se token bana kar manual raasta bhi chalega."
                    }
                    refreshFlags()
                }
            }) { Text("Code se token lo aur save karo") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Dashboard fallback: kisi bhi API block / rate-limit / scope-kami par upar wali service ka dashboard hi raasta hai — Flora kabhi jhootha 'ho gaya' nahi bolegi.")
    }
}
