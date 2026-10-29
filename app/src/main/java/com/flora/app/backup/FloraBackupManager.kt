package com.flora.app.backup

import android.content.Context
import com.flora.app.data.BuilderAuthKind
import com.flora.app.data.BuilderService
import com.flora.app.data.ChatMemoryStore
import com.flora.app.data.DocsStore
import com.flora.app.data.FloraDoc
import com.flora.app.data.FloraPreferences
import com.flora.app.data.JournalEntry
import com.flora.app.data.JournalStore
import com.flora.app.data.LicenceClient
import com.flora.app.data.MacroStore
import com.flora.app.data.MemoriesStore
import com.flora.app.data.ProjectMemoryStore
import com.flora.app.data.ProjectMeta
import com.flora.app.data.ProjectStore
import com.flora.app.data.RoutineStore
import com.flora.app.data.StoredMacro
import com.flora.app.data.Routine
import com.flora.app.planner.PlanValidator
import com.flora.app.tools.KeyInputGuard
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/**
 * B6 Backup/Import pipeline (B6-01…B6-09, rulings R-B6-1…R-B6-11).
 *
 * SCOPE (R-B6-6, final — isse bahar kuch nahi):
 * IN: chats, memories, journal, docs, B2 projects + project
 * memory, settings (allowlisted keys ONLY), routines/macros,
 * secrets (F4 re-wrap section: app-link refresh token +
 * builder tokens + API keys — sirf encrypted blob ke andar).
 * OUT: JobStore/JobChainStore/NotificationStore/PendingTaskStore/
 * SosStore, AutoLog CONTENT (sirf backup/import ACTION log hoti
 * hai, content kabhi backup nahi hota), VoiceVault/biometric
 * templates, model files, quota counters (R-B6-7), usage
 * counters, planner plans (Room flora_plans.db — B6 v1 OUT,
 * user decision ke liye open question hai), licence key slot +
 * payment/UTR slots (R-B6-8 — server hi sach hai).
 *
 * SETTINGS ALLOWLIST (R-B6-8): sirf ye keys — PIN/Guardian/
 * face-lock toggles, automation consent, auto mode, licence
 * key, UTR, UPI, talk/usage counters, permissions-gate flag
 * kabhi backup nahi hote (naye phone par dobara consent /
 * dobara PIN set karna hi sahi hai; Guardian vault waise bhi
 * device-bound hai, uska toggle restore karna lockout banata).
 *
 * RESTORE (R-B6-9, R-B6-11): REPLACE per section, kabhi merge
 * nahi. Pehle POORA blob verify hota hai (GCM tag + har section
 * ka hash + guard checks) — uske baad hi koi write chalta hai.
 * Galat PIN / corrupt / guard fail = poora import refuse, koi
 * partial state nahi. Import ke aakhir me MANDATORY app-link
 * re-sync + LicenceClient.verify() re-verify; server reject
 * kare to link unlinked + imaandaar re-link guidance (koi
 * locally-invented link state kabhi nahi).
 */
class BackupFailure(message: String) : Exception(message)

data class RestoreReport(
    val sectionsRestored: List<String>,
    val linkRestored: Boolean,
    val linkLost: Boolean,
    val licenceMessage: String?,
    val licenceKeyNote: String,
)

class FloraBackupManager(
    private val context: Context,
    private val prefs: FloraPreferences,
    private val licenceClient: LicenceClient,
) {

    private val appContext = context.applicationContext

    // ---------- COLLECT (backup banane ke liye) ----------

    /**
     * Saare IN-scope sections padh kar naam -> JSON map deta hai.
     * Project file ka strict decrypt fail ho to poora collect
     * fail (koi aadha blob nahi banta).
     */
    suspend fun collectSections(): Map<String, String> {
        val out = linkedMapOf<String, String>()

        val chats = ChatMemoryStore(appContext).loadAll()
        val chatArr = JSONArray()
        for (turn in chats) {
            chatArr.put(
                JSONObject()
                    .put("u", turn.fromUser)
                    .put("t", turn.text)
                    .put("ts", turn.ts)
            )
        }
        out[FloraBackupCrypto.Sections.CHATS] = chatArr.toString()

        val memories = MemoriesStore(appContext).items.first()
        val memArr = JSONArray()
        for (text in memories) memArr.put(text)
        out[FloraBackupCrypto.Sections.MEMORIES] = memArr.toString()

        val journal = JournalStore(appContext).entries.first()
        val journalArr = JSONArray()
        for (entry in journal) {
            journalArr.put(
                JSONObject()
                    .put("mood", entry.mood)
                    .put("text", entry.text)
                    .put("ts", entry.ts)
            )
        }
        out[FloraBackupCrypto.Sections.JOURNAL] = journalArr.toString()

        val docs = DocsStore(appContext).docs.first()
        val docsArr = JSONArray()
        for (doc in docs) {
            docsArr.put(JSONObject().put("title", doc.title).put("body", doc.body))
        }
        out[FloraBackupCrypto.Sections.DOCS] = docsArr.toString()

        val projectStore = ProjectStore(appContext)
        val projects = projectStore.listProjects()
        val projectsArr = JSONArray()
        for (project in projects) {
            val filesArr = JSONArray()
            for (file in project.files) {
                val read = projectStore.readProjectFile(project.id, file.id)
                val content = read.getOrNull()
                    ?: throw BackupFailure(
                        "Project '${project.name}' ki ek file khuli nahi — backup roka gaya hai, kuch bhi save nahi hua."
                    )
                filesArr.put(
                    JSONObject()
                        .put("id", file.id)
                        .put("name", file.name)
                        .put("content", content)
                )
            }
            projectsArr.put(
                JSONObject()
                    .put("id", project.id)
                    .put("name", project.name)
                    .put("createdAtMs", project.createdAtMs)
                    .put("files", filesArr)
            )
        }
        out[FloraBackupCrypto.Sections.PROJECTS] = projectsArr.toString()

        val projectMemory = ProjectMemoryStore(appContext).loadAll()
        val pmArr = JSONArray()
        for (entry in projectMemory) {
            pmArr.put(
                JSONObject()
                    .put("p", entry.projectId)
                    .put("k", entry.kind)
                    .put("t", entry.text)
                    .put("ts", entry.ts)
            )
        }
        out[FloraBackupCrypto.Sections.PROJECT_MEMORY] = pmArr.toString()

        out[FloraBackupCrypto.Sections.SETTINGS] = collectSettings().toString()

        val routines = RoutineStore.load(appContext)
        val routinesArr = JSONArray()
        for (routine in routines) {
            val cmds = JSONArray()
            for (command in routine.commands) cmds.put(command)
            routinesArr.put(JSONObject().put("name", routine.name).put("commands", cmds))
        }
        out[FloraBackupCrypto.Sections.ROUTINES] = routinesArr.toString()

        val macros = MacroStore.load(appContext)
        val macrosArr = JSONArray()
        for (macro in macros) {
            macrosArr.put(JSONObject().put("name", macro.name).put("json", macro.json))
        }
        out[FloraBackupCrypto.Sections.MACROS] = macrosArr.toString()

        out[FloraBackupCrypto.Sections.SECRETS] = collectSecrets().toString()

        return out
    }

    private suspend fun collectSettings(): JSONObject {
        val settings = JSONObject()
        settings.put("avatar", prefs.avatar())
        settings.put("persona", prefs.persona())
        settings.put("voiceSample", prefs.voiceSample())
        settings.put("theme", prefs.theme())
        settings.put("wakeEnabled", prefs.wakeEnabled())
        settings.put("wakeThreshold", prefs.wakeThreshold().toDouble())
        settings.put("voiceMatchThreshold", prefs.voiceMatchThreshold().toDouble())
        settings.put("callAnnounce", prefs.callAnnounce())
        settings.put("eventAnnounce", prefs.eventAnnounce())
        settings.put("drivingMode", prefs.drivingMode())
        settings.put("callTalk", prefs.callTalk())
        settings.put("continuousMic", prefs.continuousMic())
        settings.put("voiceView", prefs.voiceView())
        return settings
    }

    private suspend fun collectSecrets(): JSONObject {
        val secrets = JSONObject()
        val link = JSONObject()
        val uid = prefs.linkedUserId()
        val email = prefs.linkedEmail()
        val refresh = prefs.linkedRefreshToken()
        if (uid.isNotEmpty() && refresh.isNotEmpty()) {
            link.put("uid", uid)
            link.put("email", email)
            link.put("refresh", refresh)
        }
        secrets.put("link", link)

        val builders = JSONObject()
        for (service in BuilderService.values()) {
            val connection = prefs.builderConnection(service)
            if (!connection.connected) continue
            val token = prefs.builderToken(service)
            if (token.isEmpty()) continue
            val entry = JSONObject()
            entry.put("token", token)
            entry.put("refresh", prefs.builderRefreshToken(service))
            entry.put("expires", connection.expiresAtMs)
            entry.put(
                "authKind",
                if (connection.authKind == BuilderAuthKind.OAUTH) "oauth" else "pat"
            )
            entry.put("baseUrl", connection.baseUrl)
            entry.put("projectRef", connection.projectRef)
            builders.put(service.name, entry)
        }
        secrets.put("builders", builders)

        val keys = JSONObject()
        putSecretKey(keys, "gemini", prefs.geminiApiKey())
        putSecretKey(keys, "gemini2", prefs.geminiApiKey2())
        putSecretKey(keys, "groq", prefs.groqApiKey())
        putSecretKey(keys, "groq2", prefs.groqApiKey2())
        putSecretKey(keys, "tavily", prefs.tavilyApiKey())
        putSecretKey(keys, "brave", prefs.braveApiKey())
        putSecretKey(keys, "serp", prefs.serpApiKey())
        putSecretKey(keys, "maps", prefs.mapsApiKey())
        putSecretKey(keys, "deepgram", prefs.deepgramApiKey())
        putSecretKey(keys, "supabaseAnon", prefs.supabaseAnonKey())
        secrets.put("keys", keys)
        return secrets
    }

    private fun putSecretKey(keys: JSONObject, name: String, value: String) {
        if (value.isNotEmpty()) keys.put(name, value)
    }

    /** Collect + PIN-blob encrypt — backup file ke bytes. */
    suspend fun buildBackupBlob(pin: String): ByteArray {
        val sections = collectSections()
        return FloraBackupCrypto.encryptBlob(pin, sections)
    }

    // ---------- RESTORE ----------

    /**
     * Blob kholo -> sab validate karo -> REPLACE writes ->
     * app-link re-sync + licence re-verify. Koi bhi validation
     * fail ho to BackupFailure aur koi write nahi chalta.
     */
    suspend fun restoreFromBlob(pin: String, bytes: ByteArray, deviceId: String): RestoreReport {
        val sections = try {
            FloraBackupCrypto.decryptBlob(pin, bytes)
        } catch (e: BackupFormatException) {
            throw BackupFailure(e.message ?: "Backup nahi khula.")
        }
        val parsed = validateAll(sections)
        writeAll(parsed)
        val linkResult = resyncLinkAndLicence(parsed, deviceId)
        return RestoreReport(
            sectionsRestored = FloraBackupCrypto.Sections.ALL.filter { sections.containsKey(it) },
            linkRestored = linkResult.first,
            linkLost = linkResult.second,
            licenceMessage = linkResult.third,
            licenceKeyNote = "Licence key backup me nahi rakhi jaati — key se licence " +
                "chalane wale users ko import ke baad key dobara daalni hogi.",
        )
    }

    private data class ParsedBackup(
        val chats: List<ChatMemoryStore.StoredTurn>,
        val memories: List<String>,
        val journal: List<JournalEntry>,
        val docs: List<FloraDoc>,
        val projects: List<ProjectMeta>,
        val projectContents: Map<String, Map<String, String>>,
        val projectMemory: List<ProjectMemoryStore.ProjectMemoryEntry>,
        val settings: JSONObject,
        val routines: List<Routine>,
        val macros: List<StoredMacro>,
        val secrets: JSONObject,
    )

    /** Sirf parse + guard validation — yahan koi write nahi. */
    private fun validateAll(sections: Map<String, String>): ParsedBackup {
        val chats = mutableListOf<ChatMemoryStore.StoredTurn>()
        val chatArr = parseArray(sections, FloraBackupCrypto.Sections.CHATS)
        for (i in 0 until chatArr.length()) {
            val item = chatArr.optJSONObject(i) ?: throw BackupFailure("Chats ka hissa toota hua hai.")
            val text = item.optString("t", "")
            if (text.isBlank()) continue
            chats.add(
                ChatMemoryStore.StoredTurn(
                    item.optBoolean("u", false), text, item.optLong("ts", 0L)
                )
            )
        }

        val memories = mutableListOf<String>()
        val memArr = parseArray(sections, FloraBackupCrypto.Sections.MEMORIES)
        for (i in 0 until memArr.length()) {
            val text = memArr.optString(i, "")
            if (text.isNotBlank()) memories.add(text)
        }

        val journal = mutableListOf<JournalEntry>()
        val journalArr = parseArray(sections, FloraBackupCrypto.Sections.JOURNAL)
        for (i in 0 until journalArr.length()) {
            val item = journalArr.optJSONObject(i) ?: continue
            journal.add(
                JournalEntry(
                    item.optString("mood", ""), item.optString("text", ""), item.optLong("ts", 0L)
                )
            )
        }

        val docs = mutableListOf<FloraDoc>()
        val docsArr = parseArray(sections, FloraBackupCrypto.Sections.DOCS)
        for (i in 0 until docsArr.length()) {
            val item = docsArr.optJSONObject(i) ?: continue
            docs.add(FloraDoc(item.optString("title", ""), item.optString("body", "")))
        }

        val projects = mutableListOf<ProjectMeta>()
        val projectContents = mutableMapOf<String, Map<String, String>>()
        val projectsArr = parseArray(sections, FloraBackupCrypto.Sections.PROJECTS)
        val idOk = Regex("^[A-Za-z0-9_-]{1,64}$")
        for (i in 0 until projectsArr.length()) {
            val item = projectsArr.optJSONObject(i)
                ?: throw BackupFailure("Projects ka hissa toota hua hai.")
            val projectId = item.optString("id", "")
            if (!idOk.matches(projectId)) throw BackupFailure("Project ki pehchan galat hai — restore roka gaya.")
            val filesArr = item.optJSONArray("files") ?: JSONArray()
            val files = mutableListOf<com.flora.app.data.ProjectFileMeta>()
            val contents = mutableMapOf<String, String>()
            for (j in 0 until filesArr.length()) {
                val file = filesArr.optJSONObject(j) ?: continue
                val fileId = file.optString("id", "")
                val fileName = file.optString("name", "")
                val content = file.optString("content", "")
                if (!idOk.matches(fileId) || fileName.isBlank()) {
                    throw BackupFailure("Project file ki pehchan galat hai — restore roka gaya.")
                }
                files.add(
                    com.flora.app.data.ProjectFileMeta(
                        id = fileId, name = fileName, updatedAtMs = System.currentTimeMillis()
                    )
                )
                contents[fileName] = content
            }
            projects.add(
                ProjectMeta(
                    id = projectId,
                    name = item.optString("name", "Mera project"),
                    createdAtMs = item.optLong("createdAtMs", 0L),
                    files = files,
                )
            )
            projectContents[projectId] = contents
        }

        val projectMemory = mutableListOf<ProjectMemoryStore.ProjectMemoryEntry>()
        val pmArr = parseArray(sections, FloraBackupCrypto.Sections.PROJECT_MEMORY)
        for (i in 0 until pmArr.length()) {
            val item = pmArr.optJSONObject(i) ?: continue
            val text = item.optString("t", "")
            val projectId = item.optString("p", "")
            if (text.isBlank() || projectId.isBlank()) continue
            // R-B6-11 restore-time guard — fail ho to poora import refuse.
            if (KeyInputGuard.looksKeyLike(text)) {
                throw BackupFailure("Project memory me key jaisi cheez mili — poora restore roka gaya hai.")
            }
            if (PlanValidator.containsForbiddenAssignment(text)) {
                throw BackupFailure("Project memory me mana hua text mila — poora restore roka gaya hai.")
            }
            projectMemory.add(
                ProjectMemoryStore.ProjectMemoryEntry(
                    projectId = projectId,
                    kind = item.optString("k", "context"),
                    text = text,
                    ts = item.optLong("ts", 0L),
                )
            )
        }

        val settings = parseObject(sections, FloraBackupCrypto.Sections.SETTINGS)

        val routines = mutableListOf<Routine>()
        val routinesArr = parseArray(sections, FloraBackupCrypto.Sections.ROUTINES)
        for (i in 0 until routinesArr.length()) {
            val item = routinesArr.optJSONObject(i) ?: continue
            val name = item.optString("name", "")
            if (name.isBlank()) continue
            val cmds = mutableListOf<String>()
            val cmdArr = item.optJSONArray("commands") ?: JSONArray()
            for (j in 0 until cmdArr.length()) cmds.add(cmdArr.optString(j, ""))
            routines.add(Routine(name, cmds))
        }

        val macros = mutableListOf<StoredMacro>()
        val macrosArr = parseArray(sections, FloraBackupCrypto.Sections.MACROS)
        for (i in 0 until macrosArr.length()) {
            val item = macrosArr.optJSONObject(i) ?: continue
            val name = item.optString("name", "")
            val json = item.optString("json", "")
            if (name.isNotBlank() && json.isNotBlank()) macros.add(StoredMacro(name, json))
        }

        val secrets = parseObject(sections, FloraBackupCrypto.Sections.SECRETS)

        return ParsedBackup(
            chats = chats,
            memories = memories,
            journal = journal,
            docs = docs,
            projects = projects,
            projectContents = projectContents,
            projectMemory = projectMemory,
            settings = settings,
            routines = routines,
            macros = macros,
            secrets = secrets,
        )
    }

    private fun parseArray(sections: Map<String, String>, name: String): JSONArray {
        val raw = sections[name] ?: return JSONArray()
        return try {
            JSONArray(raw)
        } catch (e: Exception) {
            throw BackupFailure("Backup ka '$name' hissa padha nahi gaya.")
        }
    }

    private fun parseObject(sections: Map<String, String>, name: String): JSONObject {
        val raw = sections[name] ?: return JSONObject()
        return try {
            JSONObject(raw)
        } catch (e: Exception) {
            throw BackupFailure("Backup ka '$name' hissa padha nahi gaya.")
        }
    }

    /** REPLACE writes — validateAll ke baad hi chalta hai. */
    private suspend fun writeAll(parsed: ParsedBackup) {
        val chatStore = ChatMemoryStore(appContext)
        chatStore.clearAll()
        for (turn in parsed.chats) chatStore.addTurn(turn.fromUser, turn.text)

        val memoriesStore = MemoriesStore(appContext)
        memoriesStore.clearAll()
        for (text in parsed.memories) memoriesStore.add(text)

        JournalStore(appContext).replaceAll(parsed.journal)
        DocsStore(appContext).replaceAll(parsed.docs)

        val projectMemoryOk = ProjectMemoryStore(appContext).replaceAll(parsed.projectMemory)
        if (!projectMemoryOk) {
            throw BackupFailure("Project memory save nahi hui — restore adhoora raha, dobara koshish karo.")
        }

        val projectsOk = ProjectStore(appContext).replaceAllProjects(
            parsed.projects, parsed.projectContents
        )
        if (!projectsOk) {
            throw BackupFailure("Projects save nahi hue — restore adhoora raha, dobara koshish karo.")
        }

        RoutineStore.save(appContext, parsed.routines)
        MacroStore.save(appContext, parsed.macros)

        applySettings(parsed.settings)
        applySecrets(parsed.secrets)
    }

    private suspend fun applySettings(settings: JSONObject) {
        if (settings.has("avatar")) prefs.setAvatar(settings.optString("avatar", ""))
        if (settings.has("persona")) prefs.setPersona(settings.optString("persona", ""))
        if (settings.has("voiceSample")) prefs.setVoiceSample(settings.optString("voiceSample", ""))
        if (settings.has("theme")) prefs.setTheme(settings.optString("theme", ""))
        if (settings.has("wakeEnabled")) prefs.setWakeEnabled(settings.optBoolean("wakeEnabled", false))
        if (settings.has("wakeThreshold")) {
            prefs.setWakeThreshold(settings.optDouble("wakeThreshold", 0.60).toFloat())
        }
        if (settings.has("voiceMatchThreshold")) {
            prefs.setVoiceMatchThreshold(settings.optDouble("voiceMatchThreshold", 0.50).toFloat())
        }
        if (settings.has("callAnnounce")) prefs.setCallAnnounce(settings.optBoolean("callAnnounce", false))
        if (settings.has("eventAnnounce")) prefs.setEventAnnounce(settings.optBoolean("eventAnnounce", false))
        if (settings.has("drivingMode")) prefs.setDrivingMode(settings.optBoolean("drivingMode", false))
        if (settings.has("callTalk")) prefs.setCallTalk(settings.optBoolean("callTalk", false))
        if (settings.has("continuousMic")) prefs.setContinuousMic(settings.optBoolean("continuousMic", false))
        if (settings.has("voiceView")) prefs.setVoiceView(settings.optBoolean("voiceView", false))
    }

    /**
     * F4 re-wrap: backup ke andar ke plaintext secrets yahan
     * writeSecret/setLink/setBuilderToken se guzarte hain — naye
     * phone ke KeyStore se dobara encrypt hote hain. Purana
     * KeyStore ciphertext blob me kabhi nahi aata (wahi
     * foreign-ciphertext rule jo verbatim restore ko khaali
     * kar deta — isliye re-wrap, na ki copy).
     */
    private suspend fun applySecrets(secrets: JSONObject) {
        val link = secrets.optJSONObject("link") ?: JSONObject()
        val uid = link.optString("uid", "")
        val email = link.optString("email", "")
        val refresh = link.optString("refresh", "")
        if (uid.isNotEmpty() && refresh.isNotEmpty()) {
            prefs.setLink(uid, email, refresh)
        }

        val builders = secrets.optJSONObject("builders") ?: JSONObject()
        for (service in BuilderService.values()) {
            val entry = builders.optJSONObject(service.name) ?: continue
            val token = entry.optString("token", "")
            if (token.isEmpty()) continue
            prefs.setBuilderToken(
                service = service,
                token = token,
                authKind = if (entry.optString("authKind", "pat") == "oauth") {
                    BuilderAuthKind.OAUTH
                } else {
                    BuilderAuthKind.PAT
                },
                refreshToken = entry.optString("refresh", ""),
                expiresAtMs = entry.optLong("expires", 0L),
                baseUrl = entry.optString("baseUrl", ""),
                projectRef = entry.optString("projectRef", ""),
            )
        }

        val keys = secrets.optJSONObject("keys") ?: JSONObject()
        if (keys.has("gemini")) prefs.setGeminiApiKey(keys.optString("gemini", ""))
        if (keys.has("gemini2")) prefs.setGeminiApiKey2(keys.optString("gemini2", ""))
        if (keys.has("groq")) prefs.setGroqApiKey(keys.optString("groq", ""))
        if (keys.has("groq2")) prefs.setGroqApiKey2(keys.optString("groq2", ""))
        if (keys.has("tavily")) prefs.setTavilyApiKey(keys.optString("tavily", ""))
        if (keys.has("brave")) prefs.setBraveApiKey(keys.optString("brave", ""))
        if (keys.has("serp")) prefs.setSerpApiKey(keys.optString("serp", ""))
        if (keys.has("maps")) prefs.setMapsApiKey(keys.optString("maps", ""))
        if (keys.has("deepgram")) prefs.setDeepgramApiKey(keys.optString("deepgram", ""))
        if (keys.has("supabaseAnon")) prefs.setSupabaseAnonKey(keys.optString("supabaseAnon", ""))
    }

    /**
     * Mandatory post-import re-sync + re-verify (R-B6-8).
     * Returns (linkRestored, linkLost, licenceMessage).
     */
    private suspend fun resyncLinkAndLicence(
        parsed: ParsedBackup,
        deviceId: String,
    ): Triple<Boolean, Boolean, String?> {
        var linkRestored = false
        var linkLost = false
        val link = parsed.secrets.optJSONObject("link") ?: JSONObject()
        val refresh = link.optString("refresh", "")
        val uid = link.optString("uid", "")
        val email = link.optString("email", "")
        if (uid.isNotEmpty() && refresh.isNotEmpty()) {
            val rotated = try {
                licenceClient.refreshSession(refresh)
            } catch (e: Exception) {
                null
            }
            if (rotated != null) {
                prefs.setLink(uid, email, rotated)
                prefs.setLinkServerConfirmed(true)
                linkRestored = true
            } else {
                prefs.clearLink()
                prefs.setLinkServerConfirmed(false)
                linkLost = true
            }
        } else {
            prefs.setLinkServerConfirmed(false)
        }
        val effectiveUid = try {
            prefs.effectiveUserId()
        } catch (e: Exception) {
            ""
        }
        val status = try {
            licenceClient.verify(effectiveUid, deviceId)
        } catch (e: Exception) {
            null
        }
        val message = status?.honestMessage()
        return Triple(linkRestored, linkLost, message)
    }
}
