package com.flora.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.docsDataStore: DataStore<Preferences> by preferencesDataStore(name = "flora_docs")

data class FloraDoc(val title: String, val body: String)

/**
 * Documents: title + body notes, on-device only (DataStore JSON).
 * No server, no backup, no sharing from this store. Export/share
 * is a separate, user-confirmed step and is NOT in v0.5.
 */
class DocsStore(private val context: Context) {

    private val key = stringPreferencesKey("docs_json")


    val docs: Flow<List<FloraDoc>> = context.docsDataStore.data.map { prefs ->
        val raw = MemoryCrypto.decodeStored(prefs[key] ?: "[]")
        val out = mutableListOf<FloraDoc>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                out.add(FloraDoc(obj.optString("title"), obj.optString("body")))
            }
        } catch (e: Exception) {
            // Corrupt stored JSON: treat as empty rather than crashing.
        }
        out
    }

    private suspend fun save(list: List<FloraDoc>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("title", it.title).put("body", it.body)) }
        context.docsDataStore.edit { val enc = MemoryCrypto.encodeStored(arr.toString()); if (enc != null) it[key] = enc }
    }

    /**
     * B6 restore (R-B6-9): REPLACE, kabhi merge nahi. Backup se
     * aayi poori list ek hi save me purani list ki jagah leti
     * hai. Sirf FloraBackupManager bulata hai, restore confirm
     * ke baad — koi UI isko seedha nahi bulata.
     */
    suspend fun replaceAll(newDocs: List<FloraDoc>) {
        save(newDocs)
    }

    suspend fun add(title: String, body: String) {
        val current = docs.first().toMutableList()
        current.add(FloraDoc(title.trim(), body))
        save(current)
    }

    suspend fun update(index: Int, title: String, body: String) {
        val current = docs.first().toMutableList()
        if (index in current.indices) {
            current[index] = FloraDoc(title.trim(), body)
            save(current)
        }
    }

    suspend fun remove(index: Int) {
        val current = docs.first().toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            save(current)
        }
    }
}
