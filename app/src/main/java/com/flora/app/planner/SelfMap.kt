package com.flora.app.planner

import android.content.Context
import com.flora.app.tools.KeyInputGuard
import org.json.JSONArray
import org.json.JSONObject

/**
 * B5-01 self-map client + B5-02 map generator + B5-12 safety rule.
 *
 * Self-map = Flora ka living map (screens / features / services /
 * data flows / permissions / dependencies). Device par sirf READ /
 * USE hota hai — Flora kabhi khud apna map update NAHI karegi.
 * Naya map sirf naye version ke saath publish route se aayega
 * (isliye is file me koi save/update API hai hi nahi — map update
 * != code self-edit boundary, plan ka Self-knowledge map section).
 *
 * B5-12 SAFETY (generator + client + search me, sirf comment me nahi):
 * SelfMapSafety hi ek enforcement point hai — generator isse
 * filter karke JSON banata hai, client isse filter karke padhta
 * hai, aur UnifiedSearch isse filter karke snippet banata hai
 * (B5-04 decision: AppSearch removed hai, isliye map me
 * appsearch dependency entry bhi nahi hai). Map me KABHI nahi
 * aayega: koi secret /
 * token / key VALUE, koi token-shape pattern wali line, koi
 * path-with-secret. Sirf pattern-level + structural info
 * (screen / feature / service / data-flow / permission naam —
 * value nahi).
 */
data class SelfMapScreen(val name: String, val title: String, val route: String)
data class SelfMapFeature(val name: String, val detail: String)
data class SelfMapService(val name: String, val detail: String)
data class SelfMapDataFlow(val from: String, val to: String, val detail: String)
data class SelfMapPermission(val name: String, val detail: String)
data class SelfMapDependency(val name: String, val detail: String)

data class SelfMap(
    val screens: List<SelfMapScreen>,
    val features: List<SelfMapFeature>,
    val services: List<SelfMapService>,
    val dataFlows: List<SelfMapDataFlow>,
    val permissions: List<SelfMapPermission>,
    val dependencies: List<SelfMapDependency>,
)

object SelfMapSafety {

    /** Sensitive key NAAM (value nahi) — assignment shape pakadne ke liye. */
    private val SENSITIVE_NAMES = listOf(
        "password", "passwd", "secret", "token", "apikey", "api_key", "auth",
    )

    private val ASSIGNMENT = Regex(
        "(?i)\\b(password|passwd|secret|token|apikey|api_key|auth)\\b\\s*[:=]"
    )

    /**
     * Kya is text me secret jaisi cheez hai? (a) koi token-shape
     * token (KeyInputGuard shape check — value kabhi yahan print
     * nahi hoti, sirf true/false), (b) sensitive naam ke saath
     * assignment shape (name=value / name: value), (c) Bearer
     * ke saath lamba token. Structural naam (jaise
     * "BuilderTokens") isme NAHI aate — unme na shape hai, na
     * assignment hai.
     */
    fun containsSecretValue(text: String): Boolean {
        if (text.isBlank()) return false
        if (ASSIGNMENT.containsMatchIn(text)) return true
        val lower = text.lowercase()
        if (lower.contains("bearer ") && text.length > 30) return true
        for (raw in text.split(Regex("\\s+"))) {
            val token = raw.trim('.', ',', ';', ':', '!', '?', '"', '\'', '(', ')', '[', ']', '{', '}', '<', '>')
            if (token.isEmpty()) continue
            if (KeyInputGuard.looksKeyLike(token)) return true
        }
        return false
    }

    fun isSensitiveName(name: String): Boolean {
        val lower = name.lowercase()
        return SENSITIVE_NAMES.any { lower == it || lower.endsWith("_$it") || lower.startsWith("${it}_") }
    }

    private fun clean(text: String): String? {
        val t = text.trim()
        if (t.isEmpty()) return null
        if (containsSecretValue(t)) return null
        return t
    }

    fun screen(s: SelfMapScreen): SelfMapScreen? {
        val name = clean(s.name) ?: return null
        if (isSensitiveName(name)) return null
        val title = clean(s.title) ?: ""
        val route = clean(s.route) ?: ""
        return SelfMapScreen(name, title, route)
    }

    fun feature(f: SelfMapFeature): SelfMapFeature? {
        val name = clean(f.name) ?: return null
        if (isSensitiveName(name)) return null
        val detail = clean(f.detail) ?: return null
        return SelfMapFeature(name, detail)
    }

    fun service(s: SelfMapService): SelfMapService? {
        val name = clean(s.name) ?: return null
        if (isSensitiveName(name)) return null
        val detail = clean(s.detail) ?: return null
        return SelfMapService(name, detail)
    }

    fun dataFlow(d: SelfMapDataFlow): SelfMapDataFlow? {
        val from = clean(d.from) ?: return null
        val to = clean(d.to) ?: return null
        val detail = clean(d.detail) ?: return null
        return SelfMapDataFlow(from, to, detail)
    }

    fun permission(p: SelfMapPermission): SelfMapPermission? {
        val name = clean(p.name) ?: return null
        if (isSensitiveName(name)) return null
        val detail = clean(p.detail) ?: ""
        return SelfMapPermission(name, detail)
    }

    fun dependency(d: SelfMapDependency): SelfMapDependency? {
        val name = clean(d.name) ?: return null
        if (isSensitiveName(name)) return null
        val detail = clean(d.detail) ?: return null
        return SelfMapDependency(name, detail)
    }

    /** Poora map sanitize karo — sensitive entry poori drop hoti hai, kabhi mask nahi. */
    fun sanitize(map: SelfMap): SelfMap = SelfMap(
        screens = map.screens.mapNotNull { screen(it) },
        features = map.features.mapNotNull { feature(it) },
        services = map.services.mapNotNull { service(it) },
        dataFlows = map.dataFlows.mapNotNull { dataFlow(it) },
        permissions = map.permissions.mapNotNull { permission(it) },
        dependencies = map.dependencies.mapNotNull { dependency(it) },
    )
}

/**
 * B5-02 map generator code. Repo docs + code index se map JSON
 * banane wala canonical generator — CI drift-check step (pending
 * web-UI item, docs/FLORA_B5_CI_PENDING.md) isi ke rules ka
 * host-side port chalayega aur shipped asset se milayega.
 * Generator sirf pattern-level + structural entries leta hai;
 * SelfMapSafety har entry par lagta hai, isliye sensitive entry
 * JSON me kabhi nahi pahunchegi.
 */
object SelfMapGenerator {

    fun generate(map: SelfMap): String {
        val safe = SelfMapSafety.sanitize(map)
        val root = JSONObject()
        val screens = JSONArray()
        for (s in safe.screens) {
            screens.put(JSONObject().put("name", s.name).put("title", s.title).put("route", s.route))
        }
        val features = JSONArray()
        for (f in safe.features) {
            features.put(JSONObject().put("name", f.name).put("detail", f.detail))
        }
        val services = JSONArray()
        for (s in safe.services) {
            services.put(JSONObject().put("name", s.name).put("detail", s.detail))
        }
        val flows = JSONArray()
        for (d in safe.dataFlows) {
            flows.put(JSONObject().put("from", d.from).put("to", d.to).put("detail", d.detail))
        }
        val permissions = JSONArray()
        for (p in safe.permissions) {
            permissions.put(JSONObject().put("name", p.name).put("detail", p.detail))
        }
        val dependencies = JSONArray()
        for (d in safe.dependencies) {
            dependencies.put(JSONObject().put("name", d.name).put("detail", d.detail))
        }
        root.put("screens", screens)
        root.put("features", features)
        root.put("services", services)
        root.put("dataFlows", flows)
        root.put("permissions", permissions)
        root.put("dependencies", dependencies)
        return root.toString()
    }

    /**
     * INITIAL map — shipped asset
     * app/src/main/assets/flora_self_map.json ka canonical source.
     * Screen naam FloraApp Screen enum se, title screenTitle() se;
     * baaki entries maujooda code ke structural naam hi hain
     * (koi value, path-with-secret ya token-shape nahi).
     */
    fun initialMap(): SelfMap {
        val screens = listOf(
            SelfMapScreen("CHAT", "Chat", "chat"),
            SelfMapScreen("SETTINGS", "Settings", "settings"),
            SelfMapScreen("MEMORIES", "Yaadein", "memories"),
            SelfMapScreen("SCAN", "Scan", "scan"),
            SelfMapScreen("TOOLS", "Tools", "tools"),
            SelfMapScreen("SOS", "SOS", "sos"),
            SelfMapScreen("FACE", "Face", "face"),
            SelfMapScreen("DOCS", "Docs", "docs"),
            SelfMapScreen("CONNECT", "Connect", "connect"),
            SelfMapScreen("PRIVACY", "Privacy", "privacy"),
            SelfMapScreen("TERMS", "Terms", "terms"),
            SelfMapScreen("AUTO", "Auto", "auto"),
            SelfMapScreen("ROUTINES", "Routines", "routines"),
            SelfMapScreen("LIVE", "Live Screen", "live"),
            SelfMapScreen("DRIVING", "Gaadi", "driving"),
            SelfMapScreen("JOURNAL", "Journal", "journal"),
            SelfMapScreen("PERMISSIONS", "Permissions", "permissions"),
            SelfMapScreen("LICENCE", "Licence", "licence"),
            SelfMapScreen("AUTO_LOG", "Auto Log", "auto_log"),
            SelfMapScreen("MODELS", "Models", "models"),
            SelfMapScreen("VOICE_ENROL", "Awaaz sikhao", "voice_enrol"),
            SelfMapScreen("NOTIFICATIONS", "Notifications", "notifications"),
            SelfMapScreen("WHITEBOARD", "Whiteboard", "whiteboard"),
            SelfMapScreen("HOME", "Home", "home"),
            SelfMapScreen("AVATAR", "Avatar", "avatar"),
            SelfMapScreen("CAMERA_LOOK", "Camera dekho", "camera_look"),
            SelfMapScreen("BROWSER", "Browser", "browser"),
            SelfMapScreen("LOCK", "PIN gate", "lock"),
            SelfMapScreen("PC_LINK", "PC Link", "pc_link"),
            SelfMapScreen("BUILDER", "Builder", "builder"),
            SelfMapScreen("PLANNER", "Planner", "planner"),
            SelfMapScreen("DOCTOR", "Doctor", "doctor"),
        )
        val features = listOf(
            SelfMapFeature("planner", "User intention se plan banao, step-by-step execute karo, verify karo (PlannerEngine)."),
            SelfMapFeature("builder", "Project banao, code likho, preview dekho, export karo (BuilderScreen + ProjectStore)."),
            SelfMapFeature("browser", "Flora browser: page padho, form bharo, classify karo (BrowserController singleton)."),
            SelfMapFeature("voice", "Voice chat, mic, wake, TTS (voice surfaces; planner migration audit alag doc me)."),
            SelfMapFeature("chat_memory", "Baat-cheet ka long-lasting encrypted record (ChatMemoryStore)."),
            SelfMapFeature("project_memory", "Project-scoped decisions/context ka encrypted record (ProjectMemoryStore)."),
            SelfMapFeature("learned_engine", "Repeat kaam ka pattern store + decision engine (LearnedEngine)."),
            SelfMapFeature("unified_search", "Chat + project + task log ki ek search, dedup + ranking ke saath (UnifiedSearch)."),
            SelfMapFeature("doctor", "Builder projects ka scan + fix preview + fix + rollback + pre-deploy gate (DoctorScanner)."),
            SelfMapFeature("licence_gate", "Har gated use se pehle fresh licence verify + free-cap fallback (LicenceGate)."),
            SelfMapFeature("auto_log", "Har action ka on-device hisaab, 30-din prune (AutoLogStore)."),
        )
        val services = listOf(
            SelfMapService("github", "Builder-project repo create + file push (GitHubClient, B3)."),
            SelfMapService("vercel", "Builder-project create + deploy, pre-deploy gate ke saath (VercelClient, B3)."),
            SelfMapService("supabase_projects", "Builder-project create + SQL run (SupabaseProjectClient, B3)."),
            SelfMapService("brain_proxy", "Planning LLM server lane (PlannerBrainProxyClient, B4)."),
            SelfMapService("licence_verify", "Licence status verify server se (LicenceClient)."),
        )
        val dataFlows = listOf(
            SelfMapDataFlow("chat", "brain", "User baat -> brain reply; history chat memory se milti hai."),
            SelfMapDataFlow("planner", "tools", "Plan steps -> PlannerTools registry -> B3 clients / ProjectStore."),
            SelfMapDataFlow("builder", "project_files", "Editor text -> encrypted project files (device par hi)."),
            SelfMapDataFlow("memory", "encrypted_stores", "Chat/project/learned/quota sab device-encrypted stores me."),
            SelfMapDataFlow("doctor", "deploy_gate", "Deploy se pehle files ka text-only scan; critical security par deploy rukta hai."),
        )
        val permissions = listOf(
            SelfMapPermission("INTERNET", "API calls ke liye (browser, clients, brain-proxy)."),
            SelfMapPermission("CAMERA", "Scan / camera surfaces ke liye."),
            SelfMapPermission("RECORD_AUDIO", "Voice / mic surfaces ke liye."),
            SelfMapPermission("CALL_PHONE", "Confirmed automation calls ke liye."),
            SelfMapPermission("SEND_SMS", "Confirmed automation SMS ke liye."),
            SelfMapPermission("POST_NOTIFICATIONS", "Plan / foreground-service notifications ke liye."),
            SelfMapPermission("FOREGROUND_SERVICE", "PlannerService / mic service ke liye."),
            SelfMapPermission("SYSTEM_ALERT_WINDOW", "Overlay surfaces ke liye."),
            SelfMapPermission("RECEIVE_BOOT_COMPLETED", "Plan resume scheduling ke liye."),
        )
        val dependencies = listOf(
            SelfMapDependency("okhttp", "API steps ke liye HTTP client (B3 clients isi par hain)."),
            SelfMapDependency("room", "Plan/task state ka source of truth (PlanDatabase)."),
            SelfMapDependency("workmanager", "Durable scheduling / resume guarantee hi (live executor nahi)."),
            SelfMapDependency("datastore", "Chhoti settings + memory stores (chat/project/learned/quota)."),
            SelfMapDependency("kotlinx_serialization", "Structured plan schema + strict parsing."),
            SelfMapDependency("timber", "Planner-step logging ka ek standard raasta (B5-09)."),
            SelfMapDependency("app_startup", "Tools registry init ka single point (B5-09)."),
        )
        return SelfMap(screens, features, services, dataFlows, permissions, dependencies)
    }
}

/**
 * B5-01 self-map client — device par sirf READ / USE.
 * Shipped asset (assets/flora_self_map.json) padhta hai, sanitize
 * karke deta hai. Corrupt / missing asset par null (crash nahi) —
 * caller "map uplabdh nahi" batayega, jhootha map nahi banayega.
 * Is class me koi save/update function NAHI hai — yehi boundary
 * hai (naya map naye version ke saath publish route se aayega).
 */
class SelfMapClient(private val context: Context) {

    fun load(): SelfMap? {
        return try {
            val raw = context.assets.open("flora_self_map.json").bufferedReader().readText()
            parse(raw)
        } catch (e: Exception) {
            null
        }
    }

    fun parse(raw: String): SelfMap? {
        return try {
            val root = JSONObject(raw)
            val screens = mutableListOf<SelfMapScreen>()
            val sArr = root.optJSONArray("screens") ?: JSONArray()
            for (i in 0 until sArr.length()) {
                val o = sArr.optJSONObject(i) ?: continue
                screens.add(
                    SelfMapScreen(
                        o.optString("name", ""),
                        o.optString("title", ""),
                        o.optString("route", ""),
                    )
                )
            }
            val features = mutableListOf<SelfMapFeature>()
            val fArr = root.optJSONArray("features") ?: JSONArray()
            for (i in 0 until fArr.length()) {
                val o = fArr.optJSONObject(i) ?: continue
                features.add(SelfMapFeature(o.optString("name", ""), o.optString("detail", "")))
            }
            val services = mutableListOf<SelfMapService>()
            val svArr = root.optJSONArray("services") ?: JSONArray()
            for (i in 0 until svArr.length()) {
                val o = svArr.optJSONObject(i) ?: continue
                services.add(SelfMapService(o.optString("name", ""), o.optString("detail", "")))
            }
            val flows = mutableListOf<SelfMapDataFlow>()
            val dArr = root.optJSONArray("dataFlows") ?: JSONArray()
            for (i in 0 until dArr.length()) {
                val o = dArr.optJSONObject(i) ?: continue
                flows.add(SelfMapDataFlow(o.optString("from", ""), o.optString("to", ""), o.optString("detail", "")))
            }
            val permissions = mutableListOf<SelfMapPermission>()
            val pArr = root.optJSONArray("permissions") ?: JSONArray()
            for (i in 0 until pArr.length()) {
                val o = pArr.optJSONObject(i) ?: continue
                permissions.add(SelfMapPermission(o.optString("name", ""), o.optString("detail", "")))
            }
            val dependencies = mutableListOf<SelfMapDependency>()
            val depArr = root.optJSONArray("dependencies") ?: JSONArray()
            for (i in 0 until depArr.length()) {
                val o = depArr.optJSONObject(i) ?: continue
                dependencies.add(SelfMapDependency(o.optString("name", ""), o.optString("detail", "")))
            }
            SelfMapSafety.sanitize(
                SelfMap(screens, features, services, flows, permissions, dependencies)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun screenByRoute(map: SelfMap, route: String): SelfMapScreen? =
        map.screens.firstOrNull { it.route == route }

    fun serviceByName(map: SelfMap, name: String): SelfMapService? =
        map.services.firstOrNull { it.name == name }
}
