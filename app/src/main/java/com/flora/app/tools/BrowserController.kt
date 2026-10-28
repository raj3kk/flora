package com.flora.app.tools

import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

/**
 * In-app browser navigation (v2.22, Phase 5, G7) — Flora ke
 * andar ka browser, bahar ka browser nahi.
 *
 * BrowserNav: chat se browser tak kaam pahunchane ka raasta.
 * Voice branch pendingUrl/pendingAction set karke actionNonce
 * badhata hai; BrowserScreen ka LaunchedEffect use utha kar
 * chalata hai (screen pehle se khuli ho tab bhi).
 *
 * BrowserController: WebView par UI-thread-safe actions.
 * WebView null ho (browser screen khuli hi nahi) to har
 * action callback me failure deta hai — kabhi crash nahi.
 *
 * Phase 4 (load race fix): load() kabhi URL nahi khota —
 * WebView taiyaar na ho to URL queue me rehta hai, taiyaar
 * hote hi khul jata hai. "Browser kholo" par blank nahi —
 * default homepage khulta hai. Aakhri URL yaad rehta hai,
 * screen chhodne par bhi (state save).
 */
object BrowserNav {
    var pendingUrl: String? = null
    var pendingAction: String? = null
    var actionNonce by mutableStateOf(0)

    fun request(url: String? = null, action: String? = null) {
        pendingUrl = url
        pendingAction = action
        actionNonce++
    }
}

object BrowserController {
    var webView: WebView? = null

    /** Bola hua site naam/URL -> kholne layak URL. */
    fun normalize(input: String): String {
        val t = input.trim()
        if (t.startsWith("http://") || t.startsWith("https://")) return t
        // Space ho ya dot na ho to search query samjho.
        if (t.contains(" ") || !t.contains(".")) {
            return "https://www.google.com/search?q=" +
                java.net.URLEncoder.encode(t, "UTF-8")
        }
        return "https://$t"
    }

    /** URL jo WebView taiyaar hone par khulna hai — kabhi kho nahi sakta. */
    var queuedUrl: String? = null
        private set

    /** Aakhri khula URL — screen chhodne par bhi yaad rahe (state save). */
    var lastUrl: String? = null
        private set

    /** Default homepage — "browser kholo" par blank nahi dikhega. */
    const val HOMEPAGE = "https://www.google.com"

    /**
     * URL kholo — kabhi kho nahi sakta. WebView taiyaar na ho to
     * URL queue me jata hai (onResult(false)); taiyaar hote hi
     * flushQueued() use khol deta hai. Post ke dauraan WebView
     * hat jaye to bhi dobara queue hota hai, chupchaap blank nahi.
     */
    fun load(url: String, onResult: ((Boolean) -> Unit)? = null) {
        lastUrl = url
        val w = webView
        if (w == null) {
            queuedUrl = url
            onResult?.invoke(false)
            return
        }
        w.post {
            val current = webView
            if (current == null) {
                queuedUrl = url
                onResult?.invoke(false)
            } else {
                current.loadUrl(url)
                onResult?.invoke(true)
            }
        }
    }

    /** WebView taiyaar hote hi call karo — queued URL flush karta hai. */
    fun flushQueued(): String? {
        val q = queuedUrl ?: return null
        queuedUrl = null
        load(q)
        return q
    }

    /** Screen chhodne par aakhri URL save karo (khaali ho to chhodo). */
    fun saveState(url: String?) {
        if (!url.isNullOrEmpty()) lastUrl = url
    }

    fun goBack(onDone: (Boolean) -> Unit) {
        val w = webView
        if (w == null) {
            onDone(false)
            return
        }
        w.post {
            if (w.canGoBack()) {
                w.goBack()
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }

    fun goForward(onDone: (Boolean) -> Unit) {
        val w = webView
        if (w == null) {
            onDone(false)
            return
        }
        w.post {
            if (w.canGoForward()) {
                w.goForward()
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }

    fun reload() {
        webView?.post { webView?.reload() }
    }

    /** JS se wapas aaya quoted string decode karo. */
    private fun decodeJsString(raw: String?): String? {
        if (raw == null || raw == "null") return null
        return try {
            JSONArray("[$raw]").getString(0)
        } catch (e: Exception) {
            raw.trim().trim('"')
        }
    }

    /** Page ka title + body text (4000 chars tak). */
    fun readText(onDone: (String?) -> Unit) {
        val w = webView
        if (w == null) {
            onDone(null)
            return
        }
        w.post {
            w.evaluateJavascript(
                "(function(){var b=document.body;return (document.title+'\\n'+(b?b.innerText:''));})()"
            ) { raw ->
                val text = decodeJsString(raw)?.trim()?.take(4000)
                onDone(if (text.isNullOrEmpty()) null else text)
            }
        }
    }

    /** Page ke pehle 20 links: "text | url" ki list. */
    fun links(onDone: (List<String>?) -> Unit) {
        val w = webView
        if (w == null) {
            onDone(null)
            return
        }
        w.post {
            w.evaluateJavascript(
                "(function(){var out=[];var ls=document.links;for(var i=0;i<ls.length&&i<20;i++){var t=(ls[i].innerText||'').trim().replace(/\\s+/g,' ');out.push((t===''?'link':t)+' | '+ls[i].href);}return out.join('\\n');})()"
            ) { raw ->
                val text = decodeJsString(raw)?.trim()
                onDone(if (text.isNullOrEmpty()) null else text.split("\n"))
            }
        }
    }

    /** links() se mile index wala link kholo. */
    fun clickLink(index: Int) {
        webView?.post {
            webView?.evaluateJavascript(
                "(function(){var ls=document.links;if(ls.length>$index){ls[$index].click();return true;}return false;})()",
                null
            )
        }
    }
}
