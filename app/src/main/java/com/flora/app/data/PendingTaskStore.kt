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

private val Context.pendingTaskDataStore: DataStore<Preferences> by preferencesDataStore(name = "flora_pending_tasks")

/**
 * Pending kaam (Phase 2B, user order 2026-10-29 02:59) — user jo
 * "yaad rakhna" bol kar batata hai, wo yahan save hota hai.
 * Sirf is phone par, MemoryCrypto (KeyStore AES/GCM) se
 * encrypted; server par kabhi nahi jata.
 *
 * - "Aage kya karna hai" poochhne par isi list se jawab milta hai.
 * - "Agla kaam karo" par pehla kaam list se hat kar sendChat se
 *   wahi verified raaste par chalta hai (Auto Mode rules router
 *   me lagte hain) — task tabhi hata hai jab chalane ka faisla
 *   ho gaya ho, taaki dobara poochhne par wahi kaam dobara na chale.
 * - Is store par bhi 30-din prune NAHI chalta (pending kaam
 *   waqt se nahi, poora hone se khatam hote hain).
 */
class PendingTaskStore(private val context: Context) {

    private val key = stringPreferencesKey("pending_tasks_json")

    val tasks: Flow<List<String>> = context.pendingTaskDataStore.data.map { prefs ->
        decode(prefs[key] ?: "[]")
    }

    private fun decode(raw: String): List<String> {
        val plain = MemoryCrypto.decodeStored(raw)
        val out = mutableListOf<String>()
        try {
            val arr = JSONArray(plain)
            for (i in 0 until arr.length()) {
                val el = arr.optJSONObject(i) ?: continue
                val text = el.optString("t", "")
                if (text.isNotBlank()) out.add(text)
            }
        } catch (e: Exception) {
        }
        return out
    }

    private suspend fun save(list: List<String>) {
        val arr = JSONArray()
        list.forEach { text ->
            arr.put(JSONObject().put("t", text).put("ts", System.currentTimeMillis()))
        }
        context.pendingTaskDataStore.edit { it[key] = MemoryCrypto.encodeStored(arr.toString()) }
    }

    suspend fun loadAll(): List<String> {
        return try {
            tasks.first()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun add(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val current = loadAll().toMutableList()
        current.add(clean)
        save(current)
    }

    suspend fun removeAt(index: Int) {
        val current = loadAll().toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            save(current)
        }
    }

    suspend fun clearAll() {
        save(emptyList())
    }
}
