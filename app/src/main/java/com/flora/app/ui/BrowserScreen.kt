package com.flora.app.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.DownloadListener
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.SslError
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.flora.app.assistant.VoiceEngine
import com.flora.app.tools.BrowserController
import com.flora.app.tools.BrowserNav
import kotlinx.coroutines.delay

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
 */
@Composable
fun BrowserScreen(voice: VoiceEngine) {
    val context = LocalContext.current
    var urlBar by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var sslHandler by remember { mutableStateOf<SslErrorHandler?>(null) }
    var sslError by remember { mutableStateOf<SslError?>(null) }
    var pendingDownload by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    val sessionHistory = remember { mutableStateListOf<String>() }

    // Chat se aaya kaam (url kholo / wapas / aage / reload /
    // padho / link kholo) — screen khuli ho ya abhi khuli ho,
    // nonce badhte hi ye effect chalta hai.
    LaunchedEffect(BrowserNav.actionNonce) {
        val url = BrowserNav.pendingUrl
        val action = BrowserNav.pendingAction
        BrowserNav.pendingUrl = null
        BrowserNav.pendingAction = null
        if (url != null) {
            urlBar = url
            currentUrl = url
            pageError = null
            BrowserController.load(url)
            if (action == "read") {
                // Naya page load hone ka waqt do, phir padho.
                delay(3000)
                BrowserController.readText { text ->
                    if (text == null) {
                        voice.speak("Page abhi khula nahi hai, thodi der baad dobara bolo.")
                    } else {
                        voice.speak(text.take(600))
                    }
                }
                return@LaunchedEffect
            }
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
            "read" -> BrowserController.readText { text ->
                if (text == null) {
                    voice.speak("Pehle koi page kholo, phir dobara bolo.")
                } else {
                    voice.speak(text.take(600))
                }
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
        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (pageError != null) {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("Page nahi khula: $pageError")
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = {
                    pageError = null
                    BrowserController.reload()
                }) { Text("Dobara koshish karo") }
            }
        }
        if (showHistory) {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(0.3f)) {
                items(sessionHistory.reversed()) { h ->
                    TextButton(onClick = {
                        urlBar = h
                        currentUrl = h
                        pageError = null
                        BrowserController.load(h)
                    }) { Text(h) }
                }
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
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER
                    settings.safeBrowsingEnabled = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            if (url != null) {
                                currentUrl = url
                                urlBar = url
                            }
                            pageError = null
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
                }
            },
            onRelease = {
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
