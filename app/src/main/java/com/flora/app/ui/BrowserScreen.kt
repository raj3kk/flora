package com.flora.app.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.net.http.SslError
import android.webkit.DownloadListener
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.flora.app.assistant.VoiceEngine
import com.flora.app.core.FloraConfig
import com.flora.app.tools.BrowserController
import com.flora.app.tools.BrowserNav
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * In-app browser screen (v2.22, Phase 5, G7) — Maya ke
 * in-app browser jaisa, Flora ke andar.
 *
 * Suraksha (kabhi kam nahi hogi):
 * - JS on, DOM storage on; file access OFF, universal file
 *   access OFF, mixed content NEVER, Safe Browsing on.
 * - SSL error par kabhi chupchaap proceed nahi — warning
 *   dialog + user ka faisla (aage badho / wapas jao).
 * - Download har baar confirm dialog ke baad hi hota hai,
 *   kabhi silent nahi.
 * - Password WebView me kabhi save nahi hota (modern
 *   WebView me password save ka option hi nahi hai; yahan
 *   koi save-password setting on nahi ki gayi).
 * - History sirf is phone par, is session ki list me.
 * - Phase 4 (load race fix): load() kabhi URL nahi khota
 *   (queue + flush); "browser kholo" par default homepage;
 *   aakhri URL screen chhodne par bhi yaad rehta hai;
 *   load-fail par visible error/notice, chupchaap blank nahi.
 */
@Composable
fun BrowserScreen(voice: VoiceEngine, searchTools: com.flora.app.tools.SearchTools? = null) {
    val context = LocalContext.current
    val browserScope = rememberCoroutineScope()
    var urlBar by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0) }
    var pageError by remember { mutableStateOf<String?>(null) }
    // Load queue notice (Phase 4): URL queue me ho to user ko
    // chupchaap blank nahi — ye line dikhegi.
    var queuedNotice by remember { mutableStateOf(false) }
    var sslHandler by remember { mutableStateOf<SslErrorHandler?>(null) }
    var sslError by remember { mutableStateOf<SslError?>(null) }
    var pendingDownload by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    val sessionHistory = remember { mutableStateListOf<String>() }
    // B1-POLISH N1 — detectFields se mili list yahan dikhti hai
    // (chat se "browser fields dikhao" par), taaki user field
    // ka exact label dekh kar phir fill bol sake.
    var detectedFields by remember { mutableStateOf<List<BrowserController.WebFormField>?>(null) }
    // Cycle 2, APP-C1-021 (/pay entry lock): LicenceScreen se
    // website-submit payment ke liye /pay par aaye to ye browser
    // sirf pay-host (Flora website) tak lock rehta hai — /pay
    // EXCEPTION BARKARAAR hai (payment path par koi gate nahi),
    // sirf uske baad ka escape band hai: URL bar, back/forward/
    // reload buttons aur history-tap chhup jate hain, voice se
    // aaya bahar ka URL load nahi hota, aur page ke andar ke
    // link bhi bahar ke host par nahi khulte.
    // B1 G9: lock ab screen dispose par reset NAHI hota —
    // BrowserController.payLocked me persist hota hai aur screen
    // dobara khulne par pay entry page hi restore hota hai.
    // Lock sirf "Payment ho gaya — browser unlock karo" button
    // se hat-ta hai. upi:// jaisa non-http scheme exception
    // barkaraar hai (payment flow ka designed exception).
    // B1 G9: lock ab composition ke saath nahi marta — BrowserController
    // me persist hota hai (payLocked/payEntryUrl); screen dobara khule
    // to wahi lock wapas lagta hai, lastUrl restore unlocked page nahi dikhata.
    val payLockedState = remember { mutableStateOf(BrowserController.payLocked) }
    val payHost = remember { Uri.parse(FloraConfig.WEBSITE_URL).host }
    fun isPayEntry(url: String): Boolean {
        val u = try { Uri.parse(url) } catch (e: Exception) { return false }
        // B1 G9: "/pay/" trailing slash bhi pay entry hai.
        return u.host == payHost && u.path?.trimEnd('/') == "/pay"
    }
    fun isPayHostUrl(url: String): Boolean {
        return try { Uri.parse(url).host == payHost } catch (e: Exception) { false }
    }
    // B1 G9: lock lagana/hatana — state + persistent flag dono saath.
    fun lockPay(url: String) {
        payLockedState.value = true
        BrowserController.setPayLock(url)
    }
    fun unlockPay() {
        payLockedState.value = false
        BrowserController.clearPayLock()
    }

    /**
     * B1 G6/G7 — browser-fail se research-lane fallback. Wall page
     * ka content kabhi sunaya nahi jata; lane fail ho to imaandaar
     * message (brain-proxy lane FloraApp fallback me hai).
     * B1-POLISH N6 — lane jawab ke saath source bhi bataya jata
     * hai (Tavily jab lane me "Source: Tavily" ho, warna
     * DuckDuckGo) — FloraApp wale research path ki tarah.
     */
    fun researchFallback(query: String, failMsg: String) {
        val tools = searchTools
        if (tools == null || query.isBlank()) {
            voice.speak(failMsg)
            return
        }
        browserScope.launch {
            val res = try { tools.search(query) } catch (e: Exception) { "" }
            if (res.isBlank() || com.flora.app.tools.IntentRouter.isLaneFailure(res)) {
                voice.speak("$failMsg Live source se jawab nahi mila.")
            } else {
                val source = if (res.contains("Source: Tavily")) "Tavily" else "DuckDuckGo"
                val clean = res.substringBefore("\n\nSource:").trim()
                voice.speak("Browser se page nahi mila, research lane ($source) se: ${clean.take(600)}")
            }
        }
    }

    /**
     * B1-POLISH N6 — wall research query: wall page ki pehli
     * line (aksar sirf title/"Login") NAHI — page title +
     * URL (host + path) se saarthak query banao, login/captcha
     * jaise shabd hatakar. Khali pade to host, phir poora URL.
     */
    fun buildWallQuery(title: String, url: String): String {
        val stop = setOf(
            "login", "log", "in", "sign", "signin", "signup", "captcha",
            "verify", "verification", "human", "please", "account",
            "continue", "to", "view", "page",
        )
        val words = title.split(Regex("[^A-Za-z0-9]+"))
            .filter { it.isNotBlank() && it.lowercase() !in stop }
        val host = try { Uri.parse(url).host.orEmpty().removePrefix("www.") } catch (e: Exception) { "" }
        val hostWords = host.split(".").filter { it.isNotBlank() && it.lowercase() !in setOf("com", "org", "net", "in", "www") }
        val pathWords = try {
            Uri.parse(url).path.orEmpty().split(Regex("[^A-Za-z0-9]+"))
                .filter { it.isNotBlank() && it.lowercase() !in stop }
        } catch (e: Exception) { emptyList() }
        val q = (words + hostWords + pathWords).joinToString(" ").take(120).trim()
        return q.ifEmpty { host.ifEmpty { url } }
    }

    fun handleReadText(text: String?, pageTitle: String, pageUrl: String, hasLoginForm: Boolean) {
        if (text == null) {
            researchFallback(buildWallQuery(pageTitle, pageUrl), "Page abhi khula nahi hai, thodi der baad dobara bolo.")
            return
        }
        val cls = BrowserController.classifyPageText(text, hasLoginForm)
        if (cls != BrowserController.PageClass.Content) {
            // Wall ko content ki tarah mat dikhao/sunao — sirf wajah + research lane.
            voice.speak("Ye page ${BrowserController.wallReason(cls)} — isliye use content ki tarah nahi dikha raha.")
            researchFallback(buildWallQuery(pageTitle, pageUrl), "Research lane se bhi jawab nahi mila.")
            return
        }
        voice.speak(text.take(600))
    }

    // Chat se aaya kaam (url kholo / wapas / aage / reload /
    // padho / link kholo) — screen khuli ho ya abhi khuli ho,
    // nonce badhte hi ye effect chalta hai.
    LaunchedEffect(BrowserNav.actionNonce) {
        val url = BrowserNav.pendingUrl
        val action = BrowserNav.pendingAction
        BrowserNav.pendingUrl = null
        BrowserNav.pendingAction = null
        if (url != null) {
            // APP-C1-021: /pay entry par lock lagao; lock ke
            // baad bahar ke host ka URL (voice se bhi) nahi khulega.
            // B1 G9: lock entry-route se azaad hai — ye branch,
            // URL-bar "Jao", history-tap aur onPageStarted sab
            // lock lagate hain.
            if (isPayEntry(url)) lockPay(url)
            if (payLockedState.value && !isPayHostUrl(url)) {
                voice.speak("Payment page se bahar nahi ja sakte — sirf Flora ki site khuli hai.")
                return@LaunchedEffect
            }
            urlBar = url
            currentUrl = url
            pageError = null
            queuedNotice = false
            // WebView isi composition me taiyaar hota hai (factory
            // effect se pehle chalti hai) — ready check se read
            // galat page par nahi chalega.
            val webViewReady = BrowserController.webView != null
            BrowserController.load(url) { ok ->
                queuedNotice = !ok
            }
            if (action == "read") {
                if (!webViewReady) {
                    voice.speak("Page abhi khul raha hai, khulne ke baad dobara bolo.")
                    return@LaunchedEffect
                }
                // Naya page load hone ka waqt do, phir padho.
                delay(3000)
                BrowserController.readPageInfo { info ->
                    handleReadText(info.text, info.title, url, info.hasLoginForm)
                }
                return@LaunchedEffect
            }
        }
        // APP-C1-021: lock me voice se back/forward/link se bhi
        // pay-host se bahar nahi nikla ja sakta (history me
        // lock se pehle ka page ho sakta hai). Read/links
        // read-only hain, isliye wahi rehte hain.
        // B1 G9: Reload bhi lock me blocked actions me hai.
        if (payLockedState.value &&
            (action == "back" || action == "forward" || action == "reload" ||
                (action != null && action.startsWith("link:")))
        ) {
            voice.speak("Payment page se bahar nahi ja sakte — sirf Flora ki site khuli hai.")
            return@LaunchedEffect
        }
        // B1 G1: browser form-fill action ("fill:<label>\u0001<value>").
        // Password guard BrowserController.fillWebField me hai.
        if (action != null && action.startsWith("fill:")) {
            val parts = action.removePrefix("fill:").split("\u0001")
            if (parts.size == 2) {
                BrowserController.fillWebField(parts[0], parts[1]) { outcome ->
                    val msg = when (outcome) {
                        is BrowserController.WebFillOutcome.Filled ->
                            "Field '${outcome.label}' me likh diya hai."
                        is BrowserController.WebFillOutcome.PasswordRefused ->
                            "Ye password field hai — isme main nahi likhungi, tum khud bharo."
                        is BrowserController.WebFillOutcome.NotFound ->
                            "Field '${parts[0]}' is page par nahi mila."
                        // B1-POLISH N2 — ambiguous: candidates dikhao,
                        // exact label dobara bolne ko kaho, andaza nahi.
                        is BrowserController.WebFillOutcome.Ambiguous ->
                            "Ek se zyada field mile: ${outcome.candidates.joinToString(", ")}. " +
                                "Poora exact label boliye, jaise field ka poora naam."
                        BrowserController.WebFillOutcome.NoWebView ->
                            "Browser khula nahi hai — pehle page kholo."
                    }
                    voice.speak(msg)
                }
            } else {
                voice.speak("Browser fill ka format samajh nahi aaya.")
            }
            return@LaunchedEffect
        }
        // B1-POLISH N1 — fields discovery ab user tak pahunchti
        // hai: chat se "browser fields dikhao" par ye action
        // list sunata + screen par dikhata hai (read-only, isliye
        // pay-lock me bhi chalta hai — fill ki tarah). Password
        // field sirf naam se bataya jata hai, kabhi bhara nahi.
        if (action == "fields") {
            BrowserController.detectFields { list ->
                if (list.isNullOrEmpty()) {
                    voice.speak("Is page par koi form field nahi mila.")
                } else {
                    detectedFields = list
                    val shown = list.take(8)
                    val desc = shown.mapIndexed { i, f ->
                        "${i + 1}. ${f.label}" +
                            if (f.isPassword) " (password field — khud bharna)" else ""
                    }.joinToString(", ")
                    val extra = if (list.size > 8) " Aur ${list.size - 8} field hain." else ""
                    voice.speak(
                        "Is page par ${list.size} field hain: $desc.$extra " +
                            "Bharne ke liye bolo: 'browser me field <naam> me <value> likho'."
                    )
                }
            }
            return@LaunchedEffect
        }
        when (action) {
            "back" -> BrowserController.goBack { ok ->
                voice.speak(if (ok) "Wapas ja raha hun." else "Peechhe jaane ke liye koi page nahi hai.")
            }
            "forward" -> BrowserController.goForward { ok ->
                voice.speak(if (ok) "Aage ja raha hun." else "Aage jaane ke liye koi page nahi hai.")
            }
            "reload" -> {
                BrowserController.reload()
                voice.speak("Page dobara load kar raha hun.")
            }
            "read" -> BrowserController.readPageInfo { info ->
                handleReadText(info.text, info.title, currentUrl, info.hasLoginForm)
            }
            "links" -> BrowserController.links { list ->
                if (list.isNullOrEmpty()) {
                    voice.speak("Is page par koi link nahi mila.")
                } else {
                    voice.speak("Links: " + list.take(5).joinToString(", ") { it.substringBefore(" | ") })
                }
            }
            else -> {
                if (action != null && action.startsWith("link:")) {
                    val idx = action.removePrefix("link:").toIntOrNull() ?: 0
                    BrowserController.clickLink(idx)
                    voice.speak("Link khol raha hun.")
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // APP-C1-021: pay-lock me URL bar + nav + history nahi —
        // entry-mode ke hisaab se ye controls band hain.
        if (payLockedState.value) {
            Text(
                "Payment page — sirf Flora ki site khuli hai. URL bar aur bahar ke links band hain.",
                modifier = Modifier.fillMaxWidth().padding(8.dp),
            )
            // B1 G9: lock se bahar nikalne ka ek saaf raasta —
            // payment poora hone par user khud unlock karega.
            Button(onClick = {
                unlockPay()
                voice.speak("Browser unlock kar diya hai.")
            }) { Text("Payment ho gaya — browser unlock karo") }
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = urlBar,
                    onValueChange = { urlBar = it },
                    label = { Text("Site ya khoj likho") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Button(onClick = {
                    val url = BrowserController.normalize(urlBar)
                    // B1 G9: manual URL-bar "Jao" path par bhi /pay lock lage.
                    if (isPayEntry(url)) lockPay(url)
                    urlBar = url
                    currentUrl = url
                    pageError = null
                    BrowserController.load(url)
                }) { Text("Jao") }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = {
                    BrowserController.goBack { }
                }) { Text("◀") }
                OutlinedButton(onClick = {
                    BrowserController.goForward { }
                }) { Text("▶") }
                OutlinedButton(onClick = { BrowserController.reload() }) { Text("Reload") }
                OutlinedButton(onClick = { showHistory = !showHistory }) {
                    Text(if (showHistory) "History band" else "History")
                }
            }
        }
        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (queuedNotice) {
            Text(
                "Page khul raha hai — browser taiyaar hote hi dikh jayega.",
                modifier = Modifier.fillMaxWidth().padding(8.dp),
            )
        }
        if (pageError != null) {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("Page nahi khula: $pageError")
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = {
                    // B1 G9: lock me error-page reload bhi blocked hai
                    // (wahi page dobara load karna bhi lock ka hissa
                    // nahi — unlock karke hi dobara koshish hogi).
                    if (payLockedState.value) {
                        voice.speak("Payment page lock me reload band hai — payment poora ho to unlock karo.")
                    } else {
                        pageError = null
                        BrowserController.reload()
                    }
                }) { Text("Dobara koshish karo") }
                // B1 G6 (page-error path): load-fail par bhi
                // research-lane fallback ka raasta — wall/read
                // path ke alawa, error page par bhi lane se
                // live jawab maanga ja sakta hai (query = fail
                // hua URL; lane fail ho to imaandaar message).
                if (searchTools != null && currentUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = {
                        researchFallback(currentUrl, "Page nahi khula.")
                    }) { Text("Research lane se dhoondho") }
                }
            }
        }
        if (showHistory && !payLockedState.value) {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(0.3f)) {
                items(sessionHistory.reversed()) { h ->
                    TextButton(onClick = {
                        // B1 G9: history se /pay khule to bhi lock lage.
                        if (isPayEntry(h)) lockPay(h)
                        urlBar = h
                        currentUrl = h
                        pageError = null
                        BrowserController.load(h)
                    }) { Text(h) }
                }
            }
        }
        // B1-POLISH N1 — fields list screen par bhi dikhe.
        val fieldsList = detectedFields
        if (fieldsList != null) {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("Is page ke fields (${fieldsList.size}):")
                fieldsList.take(10).forEach { f ->
                    Text(
                        "• ${f.label}" +
                            if (f.isPassword) " (password — khud bharna)" else ""
                    )
                }
                TextButton(onClick = { detectedFields = null }) { Text("Fields list band karo") }
            }
        }
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
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    webViewClient = object : WebViewClient() {
                        // APP-C1-021: pay-lock me page ke andar ke
                        // link/redirect bhi bahar ke host par nahi
                        // khulenge (same-host pay flow waisa hi).
                        // Non-http scheme (jaise upi://) ka purana
                        // vyavhaar waisa hi rehta hai.
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val target = request?.url?.toString() ?: return false
                            if (!payLockedState.value) return false
                            val scheme = request.url?.scheme
                            if (scheme != "http" && scheme != "https") return false
                            return !isPayHostUrl(target)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            // B1 G9: redirect/link se /pay par pahunche
                            // to bhi lock lage (entry-route se azaad).
                            if (url != null && isPayEntry(url)) lockPay(url)
                            // Defence-in-depth: koi navigation lock
                            // ko chhupa kar bahar le jaye to use roko
                            // aur pay page par wapas lao.
                            if (payLockedState.value && url != null &&
                                !isPayHostUrl(url) &&
                                (url.startsWith("http://") || url.startsWith("https://"))
                            ) {
                                view?.stopLoading()
                                view?.loadUrl(FloraConfig.WEBSITE_URL + "/pay")
                                return
                            }
                            if (url != null) {
                                currentUrl = url
                                urlBar = url
                                // B1-POLISH N7: lock me saveState NAHI —
                                // warna unlock ke baad restore aakhri
                                // pay-host page dikhata, pre-lock page
                                // nahi. Lock escape nahi hai — sirf
                                // state ki asangat theek ki gayi hai;
                                // lock ka koi aur vyavhaar nahi badla.
                                if (!payLockedState.value) BrowserController.saveState(url)
                            }
                            pageError = null
                            queuedNotice = false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            progress = 100
                            if (url != null && (sessionHistory.isEmpty() || sessionHistory.last() != url)) {
                                sessionHistory.add(url)
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                pageError = error?.description?.toString() ?: "Unknown error"
                            }
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: SslErrorHandler?,
                            error: SslError?
                        ) {
                            // Kabhi chupchaap proceed nahi — user faisla karega.
                            sslHandler = handler
                            sslError = error
                        }
                    }
                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            progress = newProgress
                        }
                    }
                    setDownloadListener(DownloadListener { url, userAgent, contentDisposition, _, _ ->
                        pendingDownload = Triple(url, userAgent, contentDisposition)
                    })
                    BrowserController.webView = this
                    // WebView taiyaar: B1 G9 — lock persist ho to
                    // pay entry page hi restore hoga, kabhi
                    // unlocked page nahi (queued/restore iske baad).
                    if (BrowserController.payLocked && BrowserController.payEntryUrl != null) {
                        loadUrl(BrowserController.payEntryUrl!!)
                    } else {
                        // WebView taiyaar: queued URL ho to kholo; pending
                        // voice URL ho to effect use kholega (isliye skip);
                        // warna aakhri page restore karo; warna pehli baar
                        // homepage — blank kabhi nahi.
                        val queued = BrowserController.flushQueued()
                        if (queued == null && BrowserNav.pendingUrl == null) {
                            val restore = BrowserController.lastUrl
                            if (restore != null) {
                                loadUrl(restore)
                            } else {
                                loadUrl(BrowserController.HOMEPAGE)
                            }
                        }
                    }
                }
            },
            onRelease = {
                // B1 G9: lock me currentUrl save nahi hoga, taaki
                // lastUrl restore kabhi unlocked page na dikhaye.
                if (!payLockedState.value) BrowserController.saveState(currentUrl)
                if (BrowserController.webView == it) BrowserController.webView = null
                it.destroy()
            },
            modifier = Modifier.fillMaxSize().weight(1f),
        )
    }

    // SSL warning dialog — user ke faisle ke bina kuch nahi.
    if (sslHandler != null) {
        AlertDialog(
            onDismissRequest = {
                sslHandler?.cancel()
                sslHandler = null
                sslError = null
            },
            title = { Text("Suraksha chetavni") },
            text = {
                Text(
                    "Is site ka certificate theek nahi lag raha " +
                        "(galti: ${sslError?.primaryError ?: "?"}). Aage badhna " +
                        "khatarnak ho sakta hai — password ya payment yahan mat daalo."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    sslHandler?.proceed()
                    sslHandler = null
                    sslError = null
                }) { Text("Phir bhi kholo") }
            },
            dismissButton = {
                TextButton(onClick = {
                    sslHandler?.cancel()
                    sslHandler = null
                    sslError = null
                }) { Text("Wapas jao") }
            },
        )
    }

    // Download confirm dialog — har download se pehle poochha jayega.
    val dl = pendingDownload
    if (dl != null) {
        val fileName = try {
            android.webkit.URLUtil.guessFileName(dl.first, dl.third, null)
        } catch (e: Exception) {
            "flora-download"
        }
        AlertDialog(
            onDismissRequest = { pendingDownload = null },
            title = { Text("Download karo?") },
            text = { Text("File: $fileName\nSource: ${dl.first}") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDownload = null
                    try {
                        val req = DownloadManager.Request(Uri.parse(dl.first))
                            .setTitle(fileName)
                            .setDescription("Flora browser se download")
                            .setNotificationVisibility(
                                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                            )
                            .setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS, fileName
                            )
                        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        dm.enqueue(req)
                        voice.speak("Download shuru kar diya hai.")
                    } catch (e: Exception) {
                        voice.speak("Download shuru nahi ho paya.")
                    }
                }) { Text("Download karo") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDownload = null }) { Text("Ruko") }
            },
        )
    }
}
