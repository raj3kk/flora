package com.flora.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flora.app.planner.PlanValidator
import com.flora.app.tools.KeyInputGuard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.projectMemoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "flora_project_memory")

/**
 * B5-03 flora_project_memory namespace (F6).
 *
 * Project-scoped, device-encrypted, long-lasting memory —
 * project ke decisions / context (recall pollute nahi hoga).
 * ChatMemoryStore pattern par hi bana hai (ALAG DataStore naam
 * "flora_project_memory" — flora_chat_memory se alag), MemoryCrypto
 * KeyStore AES/GCM reuse, corrupt JSON = khali (crash nahi).
 *
 * NAMESPACE RULE (F6): project memory = project-scoped
 * decisions/context; chat memory = baat-cheet; task log =
 * execution proof. Project memory ko ProjectStore (project
 * FILES) me kabhi mat ghusana — file store aur recall store
 * alag hain. Is store me sirf chhote text entries hain, file
 * content kabhi nahi.
 *
 * SAVE-PATH GUARDS: KeyInputGuard + PlanValidator
 * FORBIDDEN_INPUT_KEYS reject — memory me key / password /
 * token kabhi store nahi hoga. Reject par save() false deta
 * hai (silent drop nahi — caller ko pata chalta hai).
 */
class ProjectMemoryStore(private val context: Context) {

    data class ProjectMemoryEntry(
        val projectId: String,
        val kind: String,
        val text: String,
        val ts: Long,
    )

    private val key = stringPreferencesKey("project_memory_json")

    companion object {
        private const val MAX_ENTRIES = 1000
    }

    private fun decode(raw: String): List<ProjectMemoryEntry> {
        val plain = MemoryCrypto.decodeStored(raw)
        val out = mutableListOf<ProjectMemoryEntry>()
        try {
            val arr = JSONArray(plain)
            for (i in 0 until arr.length()) {
                val el = arr.optJSONObject(i) ?: continue
                val text = el.optString("t", "")
                val projectId = el.optString("p", "")
                if (text.isBlank() || projectId.isBlank()) continue
                out.add(
                    ProjectMemoryEntry(
                        projectId = projectId,
                        kind = el.optString("k", "context"),
                        text = text,
                        ts = el.optLong("ts", 0L),
                    )
                )
            }
        } catch (e: Exception) {
        }
        return out
    }

    private suspend fun saveAll(list: List<ProjectMemoryEntry>) {
        val arr = JSONArray()
        list.forEach { entry ->
            arr.put(
                JSONObject()
                    .put("p", entry.projectId)
                    .put("k", entry.kind)
                    .put("t", entry.text)
                    .put("ts", entry.ts)
            )
        }
        context.projectMemoryDataStore.edit { prefs ->
            val enc = MemoryCrypto.encodeStored(arr.toString())
            if (enc != null) prefs[key] = enc
        }
    }

    suspend fun loadAll(): List<ProjectMemoryEntry> {
        return try {
            val raw = context.projectMemoryDataStore.data.map { it[key] ?: "[]" }.first()
            decode(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadForProject(projectId: String): List<ProjectMemoryEntry> {
        return loadAll().filter { it.projectId == projectId }
    }

    /**
     * Save — false = reject hua (guard) ya encrypt fail.
     * Guard: key jaisi value, ya forbidden key assignment
     * (password/secret/token jaisa naam ke saath value).
     */
    suspend fun save(projectId: String, kind: String, text: String): Boolean {
        val clean = text.trim()
        if (clean.isEmpty() || projectId.isBlank()) return false
        if (KeyInputGuard.looksKeyLike(clean)) return false
        if (PlanValidator.containsForbiddenAssignment(clean)) return false
        val current = loadAll().toMutableList()
        current.add(
            ProjectMemoryEntry(
                projectId = projectId,
                kind = kind.ifBlank { "context" },
                text = clean,
                ts = System.currentTimeMillis(),
            )
        )
        val kept = if (current.size > MAX_ENTRIES) {
            current.drop(current.size - MAX_ENTRIES)
        } else {
            current
        }
        return try {
            saveAll(kept)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * B6 restore (R-B6-9 + R-B6-11): REPLACE, kabhi merge nahi.
     * Har entry restore-time guard se guzarti hai (KeyInputGuard
     * + forbidden assignment) — ek bhi entry guard fail kare to
     * false aur KUCH NAHI likha jaata (caller poora import
     * fail-closed rok deta hai). Backup wale ts waisa hi rehte
     * hain (save() wali tarah ab ka time nahi lagta).
     */
    suspend fun replaceAll(entries: List<ProjectMemoryEntry>): Boolean {
        for (entry in entries) {
            if (entry.projectId.isBlank() || entry.text.isBlank()) return false
            if (KeyInputGuard.looksKeyLike(entry.text)) return false
            if (PlanValidator.containsForbiddenAssignment(entry.text)) return false
        }
        return try {
            saveAll(entries.take(MAX_ENTRIES))
            true
        } catch (e: Exception) {
            false
        }
    }
}
