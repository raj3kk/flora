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

    /** B1-POLISH N5 — readText + login-form indicator (ek JS call me). */
    data class PageInfo(
        val text: String?,
        val title: String,
        val hasLoginForm: Boolean,
    )

    fun readPageInfo(onDone: (PageInfo) -> Unit) {
        val w = webView
        if (w == null) {
            onDone(PageInfo(null, "", false))
            return
        }
        val js = "(function(){var hasPw=!!document.querySelector('input[type=\"password\"]');" +
            "var hasLoginBtn=false;" +
            "var btns=document.querySelectorAll('button,input[type=\"submit\"],input[type=\"button\"],a');" +
            "for(var i=0;i<btns.length;i++){var t=((btns[i].innerText||btns[i].value||'')+'').toLowerCase();" +
            "if(t.indexOf('sign in')>=0||t.indexOf('log in')>=0||t.indexOf('login')>=0||t.indexOf('sign up')>=0){hasLoginBtn=true;break;}}" +
            "var b=document.body;" +
            "return JSON.stringify({text:(document.title+'\\n'+(b?b.innerText:'')),title:document.title,hasLoginForm:(hasPw||hasLoginBtn)});})()"
        w.post {
            w.evaluateJavascript(js) { raw ->
                val text = decodeJsString(raw)
                if (text == null) {
                    onDone(PageInfo(null, "", false))
                    return@evaluateJavascript
                }
                try {
                    val o = org.json.JSONObject(text)
                    val body = o.optString("text", "").trim().take(4000)
                    onDone(
                        PageInfo(
                            if (body.isEmpty()) null else body,
                            o.optString("title", ""),
                            o.optBoolean("hasLoginForm", false),
                        )
                    )
                } catch (e: Exception) {
                    onDone(PageInfo(null, "", false))
                }
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

    /**
     * B1-POLISH N5 — login-wall ke liye keyword AKELA kaafi nahi:
     * login/login-wall shabd + DOM me login-form indicator
     * (password input ya login/sign-in button, readPageInfo se)
     * dono chahiye. Bina form ke "please log in" jaisa zikr
     * karne wala article/help text ab galat wall nahi banega.
     * Captcha keyword akele par bhi Captcha hi rahega
     * (fail-closed — captcha kabhi content nahi banega).
     * Unsupported keyword-only hi rehta hai (browser capability
     * ka sandesh, galat-wall ka khatra kam hai).
     * Ek-arg overload sirf tab jab DOM jaankari uplabdh na ho —
     * tab login-wall nahi mana jata; read path hamesha
     * readPageInfo se flag deta hai.
     */
    fun classifyPageText(text: String?, hasLoginForm: Boolean): PageClass {
        val t = text?.lowercase() ?: return PageClass.Content
        return when {
            t.contains("captcha") || t.contains("verify you are human") ||
                t.contains("verify you're human") || t.contains("not a robot") ||
                t.contains("human verification") -> PageClass.Captcha
            hasLoginForm && (
                t.contains("sign in to continue") || t.contains("log in to continue") ||
                    t.contains("login to continue") || t.contains("sign in to view") ||
                    t.contains("log in to view") || t.contains("create an account to continue") ||
                    t.contains("sign up to continue") || t.contains("please sign in") ||
                    t.contains("please log in")
                ) -> PageClass.LoginWall
            t.contains("browser not supported") || t.contains("unsupported browser") ||
                t.contains("this browser is not supported") ||
                t.contains("please update your browser") -> PageClass.Unsupported
            else -> PageClass.Content
        }
    }

    fun classifyPageText(text: String?): PageClass {
        return classifyPageText(text, false)
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
        /** B1-POLISH N2 — ek se zyada match: chupchaap pehla kabhi nahi. */
        data class Ambiguous(val label: String, val candidates: List<String>) : WebFillOutcome()
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
     *
     * B1-POLISH N2 — substring-first-match HATAYA: pehle
     * exact match (label/name/id barabar) dhoondho, phir
     * substring. Ek se zyada match hon to Ambiguous wapas
     * do (candidates ke saath) — chupchaap pehla match
     * kabhi mat bharo. Ek match ho to Filled me wahi label
     * bataya jata hai jo bhara gaya. Matching detectFields
     * ki DOM-order list par hoti hai, isliye set bhi usi
     * index par hota hai; set ke waqt password check
     * dobara hota hai (defence in depth).
     */
    fun fillWebField(labelOrName: String, value: String, onDone: (WebFillOutcome) -> Unit) {
        if (webView == null) {
            onDone(WebFillOutcome.NoWebView)
            return
        }
        val want = labelOrName.trim().lowercase()
        if (want.isEmpty()) {
            onDone(WebFillOutcome.NotFound(labelOrName))
            return
        }
        detectFields { fields ->
            if (fields == null) {
                if (webView == null) onDone(WebFillOutcome.NoWebView)
                else onDone(WebFillOutcome.NotFound(labelOrName))
                return@detectFields
            }
            val exact = fields.indices.filter { i ->
                val f = fields[i]
                f.label.lowercase() == want || f.name.lowercase() == want || f.id.lowercase() == want
            }
            if (exact.size > 1) {
                onDone(
                    WebFillOutcome.Ambiguous(
                        labelOrName,
                        exact.map { fields[it].label }.distinct().take(5),
                    )
                )
                return@detectFields
            }
            if (exact.size == 1) {
                fillFieldAt(fields, exact[0], labelOrName, value, onDone)
                return@detectFields
            }
            val sub = fields.indices.filter { i ->
                val f = fields[i]
                (f.label + " " + f.name + " " + f.id).lowercase().contains(want)
            }
            when {
                sub.isEmpty() -> onDone(WebFillOutcome.NotFound(labelOrName))
                sub.size > 1 -> onDone(
                    WebFillOutcome.Ambiguous(
                        labelOrName,
                        sub.map { fields[it].label }.distinct().take(5),
                    )
                )
                else -> fillFieldAt(fields, sub[0], labelOrName, value, onDone)
            }
        }
    }

    private fun fillFieldAt(
        fields: List<WebFormField>,
        index: Int,
        labelOrName: String,
        value: String,
        onDone: (WebFillOutcome) -> Unit,
    ) {
        val field = fields[index]
        if (field.isPassword) {
            onDone(WebFillOutcome.PasswordRefused(labelOrName))
            return
        }
        val w = webView
        if (w == null) {
            onDone(WebFillOutcome.NoWebView)
            return
        }
        val jsValue = org.json.JSONObject.quote(value)
        val js = "(function(){var idx=$index;var val=$jsValue;" +
            "var els=document.querySelectorAll('input,textarea,select');" +
            "if(idx>=els.length)return 'NOT_FOUND';" +
            "var el=els[idx];" +
            "var type=((el.type||el.tagName)||'').toLowerCase();" +
            "var ac=(el.getAttribute('autocomplete')||'').toLowerCase();" +
            "if(type==='password'||ac.indexOf('password')>=0)return 'PASSWORD_REFUSED';" +
            "try{el.focus();el.value=val;" +
            "el.dispatchEvent(new Event('input',{bubbles:true}));" +
            "el.dispatchEvent(new Event('change',{bubbles:true}));" +
            "return 'FILLED';}catch(e){return 'SET_FAILED';}})()"
        w.post {
            w.evaluateJavascript(js) { raw ->
                val text = decodeJsString(raw) ?: ""
                when {
                    text == "FILLED" -> onDone(WebFillOutcome.Filled(field.label))
                    text == "PASSWORD_REFUSED" -> onDone(WebFillOutcome.PasswordRefused(labelOrName))
                    else -> onDone(WebFillOutcome.NotFound(labelOrName))
                }
            }
        }
    }
}
