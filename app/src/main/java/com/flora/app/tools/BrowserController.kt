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

    /**
     * B1 G9 — /pay lock ka persistent hissa (composition ke
     * saath nahi marta): BrowserScreen is flag ko padh kar
     * lock wapas lagati hai, taaki screen dispose + restore par
     * unlocked page na dikhe. Clear sirf lock UI ke "Payment ho
     * gaya — browser unlock karo" button se hota hai (ya naya
     * pay entry lock dobara lagne par overwrite). upi:// jaisa
     * non-http scheme exception barkaraar hai (BrowserScreen
     * shouldOverrideUrlLoading me).
     */
    var payLocked: Boolean = false
    var payEntryUrl: String? = null
        private set

    fun setPayLock(url: String) {
        payLocked = true
        payEntryUrl = url
    }

    fun clearPayLock() {
        payLocked = false
        payEntryUrl = null
    }

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
     *
     * B1 G12: lastUrl sirf tab set hota hai jab loadUrl asal me
     * chala (ya queuedUrl ke through flush hone par) — kabhi na
     * khula URL agle open par lastUrl restore se nahi dikhega.
     * Queued URL ka restore queuedUrl se hota hai, lastUrl se nahi.
     */
    fun load(url: String, onResult: ((Boolean) -> Unit)? = null) {
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
                lastUrl = url
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

    /** B1 G6 — page ka varg: wall page ko content ki tarah kabhi mat dikhao/sunao. */
    enum class PageClass { Content, LoginWall, Captcha, Unsupported }

    fun classifyPageText(text: String?): PageClass {
        val t = text?.lowercase() ?: return PageClass.Content
        return when {
            t.contains("captcha") || t.contains("verify you are human") ||
                t.contains("verify you're human") || t.contains("not a robot") ||
                t.contains("human verification") -> PageClass.Captcha
            t.contains("sign in to continue") || t.contains("log in to continue") ||
                t.contains("login to continue") || t.contains("sign in to view") ||
                t.contains("log in to view") || t.contains("create an account to continue") ||
                t.contains("sign up to continue") || t.contains("please sign in") ||
                t.contains("please log in") -> PageClass.LoginWall
            t.contains("browser not supported") || t.contains("unsupported browser") ||
                t.contains("this browser is not supported") ||
                t.contains("please update your browser") -> PageClass.Unsupported
            else -> PageClass.Content
        }
    }

    fun wallReason(cls: PageClass): String {
        return when (cls) {
            PageClass.Captcha -> "captcha/human-verification par ruka hai"
            PageClass.LoginWall -> "login wall par ruka hai (aage badhne ke liye user khud login kare)"
            PageClass.Unsupported -> "unsupported page hai (ye browser is page ko nahi chala sakta)"
            PageClass.Content -> ""
        }
    }

    /** B1 G1/G8 — WebView DOM ka ek form field (label/type/name/id + password flag). */
    data class WebFormField(
        val label: String,
        val type: String,
        val name: String,
        val id: String,
        val isPassword: Boolean,
    )

    sealed class WebFillOutcome {
        data class Filled(val label: String) : WebFillOutcome()
        data class NotFound(val label: String) : WebFillOutcome()
        data class PasswordRefused(val label: String) : WebFillOutcome()
        object NoWebView : WebFillOutcome()
    }

    private fun webFieldLabel(
        lab: String, placeholder: String, name: String, id: String, index: Int,
    ): String {
        return when {
            lab.isNotBlank() -> lab.trim().take(40)
            placeholder.isNotBlank() -> placeholder.trim().take(40)
            name.isNotBlank() -> name.trim().take(40)
            id.isNotBlank() -> id.trim().take(40)
            else -> "field ${index + 1}"
        }
    }

    /** Page ke pehle 30 form fields detect karo (DOM par, WebView JS se). */
    fun detectFields(onDone: (List<WebFormField>?) -> Unit) {
        val w = webView
        if (w == null) {
            onDone(null)
            return
        }
        val js = "(function(){var out=[];var els=document.querySelectorAll('input,textarea,select');" +
            "for(var i=0;i<els.length&&i<30;i++){var el=els[i];" +
            "var type=((el.type||el.tagName)||'').toLowerCase();" +
            "var id=el.id||'';var name=el.name||'';var ph=el.placeholder||'';var lab='';" +
            "if(id){var l=document.querySelector('label[for=\"'+id+'\"]');if(l)lab=l.innerText||'';}" +
            "if(!lab){var p=el.closest('label');if(p)lab=p.innerText||'';}" +
            "var ac=(el.getAttribute('autocomplete')||'').toLowerCase();" +
            "var isPw=(type==='password'||ac.indexOf('password')>=0);" +
            "out.push({label:lab,type:type,name:name,id:id,placeholder:ph,isPassword:isPw});}" +
            "return JSON.stringify(out);})()"
        w.post {
            w.evaluateJavascript(js) { raw ->
                val text = decodeJsString(raw) ?: run {
                    onDone(null)
                    return@evaluateJavascript
                }
                try {
                    val arr = JSONArray(text)
                    val out = mutableListOf<WebFormField>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val lab = o.optString("label", "")
                        val ph = o.optString("placeholder", "")
                        val nm = o.optString("name", "")
                        val idv = o.optString("id", "")
                        out.add(
                            WebFormField(
                                webFieldLabel(lab, ph, nm, idv, i),
                                o.optString("type", ""),
                                nm,
                                idv,
                                o.optBoolean("isPassword", false),
                            )
                        )
                    }
                    onDone(out)
                } catch (e: Exception) {
                    onDone(null)
                }
            }
        }
    }

    /**
     * B1 G1 — DOM field me value bharo. Guards (FormFiller
     * jaisi, WebView DOM par): type=password YA autocomplete
     * me password ho to PasswordRefused — password field me
     * Flora kabhi type nahi karegi. Sirf Android InputType
     * mask variation par bharosa nahi (B1 G8: DOM-level
     * type=password check hi asli pehchan hai; code-review +
     * CI se verify, phone-test ka daava nahi).
     */
    fun fillWebField(labelOrName: String, value: String, onDone: (WebFillOutcome) -> Unit) {
        val w = webView
        if (w == null) {
            onDone(WebFillOutcome.NoWebView)
            return
        }
        val wanted = org.json.JSONObject.quote(labelOrName.trim().lowercase())
        val jsValue = org.json.JSONObject.quote(value)
        val js = "(function(){var want=$wanted;var val=$jsValue;" +
            "var els=document.querySelectorAll('input,textarea,select');" +
            "for(var i=0;i<els.length;i++){var el=els[i];" +
            "var type=((el.type||el.tagName)||'').toLowerCase();" +
            "var id=el.id||'';var name=el.name||'';var ph=el.placeholder||'';var lab='';" +
            "if(id){var l=document.querySelector('label[for=\"'+id+'\"]');if(l)lab=l.innerText||'';}" +
            "if(!lab){var p=el.closest('label');if(p)lab=p.innerText||'';}" +
            "var hay=(lab+' '+ph+' '+name+' '+id).toLowerCase();" +
            "if(hay.indexOf(want)<0)continue;" +
            "var ac=(el.getAttribute('autocomplete')||'').toLowerCase();" +
            "if(type==='password'||ac.indexOf('password')>=0)return 'PASSWORD_REFUSED';" +
            "try{el.focus();el.value=val;" +
            "el.dispatchEvent(new Event('input',{bubbles:true}));" +
            "el.dispatchEvent(new Event('change',{bubbles:true}));" +
            "return 'FILLED:'+(lab||ph||name||id);}catch(e){return 'SET_FAILED';}}" +
            "return 'NOT_FOUND';})()"
        w.post {
            w.evaluateJavascript(js) { raw ->
                val text = decodeJsString(raw) ?: ""
                when {
                    text.startsWith("FILLED:") -> onDone(
                        WebFillOutcome.Filled(text.removePrefix("FILLED:").ifEmpty { labelOrName })
                    )
                    text == "PASSWORD_REFUSED" -> onDone(WebFillOutcome.PasswordRefused(labelOrName))
                    else -> onDone(WebFillOutcome.NotFound(labelOrName))
                }
            }
        }
    }
}
