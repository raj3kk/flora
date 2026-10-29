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

private val Context.memoriesDataStore: DataStore<Preferences> by preferencesDataStore(name = "flora_memories")

/**
 * Yaadein (Memories): on-device only, stored as a JSON array in
 * DataStore on this phone, ENCRYPTED with MemoryCrypto (KeyStore
 * AES/GCM) — file haath lag bhi jaye to sirf Flora hi padh
 * sakti hai. Nothing is sent to any server, backup, or message.
 * Purana plain data migration: decrypt fail ho to plain samajh
 * kar padha jata hai, agle save par encrypted ho jata hai.
 * Edit = replace at index; delete = remove at index.
 */
class MemoriesStore(private val context: Context) {

    private val key = stringPreferencesKey("memories_json")


    val items: Flow<List<String>> = context.memoriesDataStore.data.map { prefs ->
        val raw = MemoryCrypto.decodeStored(prefs[key] ?: "[]")
        val out = mutableListOf<String>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val el = arr.opt(i)
                if (el is org.json.JSONObject && el.has("text")) out.add(el.optString("text", ""))
                else out.add(arr.optString(i))
            }
        } catch (e: Exception) {
            // Corrupt stored JSON: treat as empty rather than crashing.
        }
        out
    }

    // Internal pair model: (text, ts-ms or null). Bina date wali
    // purani entries (plain strings) ka ts null rehta hai — unko
    // prune kabhi delete nahi karega (date pata hi nahi hai).
    private suspend fun loadPairs(): MutableList<Pair<String, Long?>> {
        val raw = MemoryCrypto.decodeStored(context.memoriesDataStore.data.map { it[key] ?: "[]" }.first())
        val out = mutableListOf<Pair<String, Long?>>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val el = arr.opt(i)
                if (el is org.json.JSONObject && el.has("text")) {
                    val ts = el.optLong("ts", -1L)
                    out.add(el.optString("text", "") to if (ts > 0) ts else null)
                } else {
                    out.add(arr.optString(i) to null)
                }
            }
        } catch (e: Exception) {
        }
        return out
    }

    private suspend fun savePairs(list: List<Pair<String, Long?>>) {
        val arr = JSONArray()
        list.forEach { (text, ts) ->
            if (ts != null) arr.put(JSONObject().put("text", text).put("ts", ts))
            else arr.put(text)
        }
        context.memoriesDataStore.edit { val enc = MemoryCrypto.encodeStored(arr.toString()); if (enc != null) it[key] = enc }
    }

    private suspend fun save(list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        context.memoriesDataStore.edit { val enc = MemoryCrypto.encodeStored(arr.toString()); if (enc != null) it[key] = enc }
    }

    suspend fun add(text: String) {
        val current = loadPairs()
        current.add(text to System.currentTimeMillis())
        savePairs(current)
    }

    suspend fun update(index: Int, text: String) {
        val current = loadPairs()
        if (index in current.indices) {
            current[index] = text to current[index].second
            savePairs(current)
        }
    }

    suspend fun remove(index: Int) {
        val current = loadPairs()
        if (index in current.indices) {
            current.removeAt(index)
            savePairs(current)
        }
    }

    /** 30-din retention (v2.9): date wali entries jo cutoff se
     * purani hain, delete hoti hain. Bina date wali entries
     * kabhi auto-delete nahi hoti (unko user manually delete
     * karega). Ye sirf app-start par chalta hai, chupchaap. */
    suspend fun pruneOlderThan(cutoffMs: Long) {
        val current = loadPairs()
        val kept = current.filter { (_, ts) -> ts == null || ts >= cutoffMs }
        if (kept.size != current.size) savePairs(kept)
    }

    /** Sab kuch delete (user ke clear confirmation ke baad hi call hota hai). */
    suspend fun clearAll() {
        save(emptyList())
    }
}
