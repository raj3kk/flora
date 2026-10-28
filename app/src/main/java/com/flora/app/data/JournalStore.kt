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

private val Context.journalDataStore: DataStore<Preferences> by preferencesDataStore(name = "flora_journal")

data class JournalEntry(val mood: String, val text: String, val ts: Long)

/**
 * Journal + mood (v1.6): on-device only, DataStore me JSON array.
 * Koi server, backup ya share nahi — MemoriesStore jaisa pattern.
 */
class JournalStore(private val context: Context) {

    private val key = stringPreferencesKey("journal_json")

    val entries: Flow<List<JournalEntry>> = context.journalDataStore.data.map { prefs ->
        val out = mutableListOf<JournalEntry>()
        try {
            val arr = JSONArray(prefs[key] ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                out.add(
                    JournalEntry(
                        o.optString("mood", ""),
                        o.optString("text", ""),
                        o.optLong("ts", 0L)
                    )
                )
            }
        } catch (e: Exception) {
        }
        out
    }

    private suspend fun save(list: List<JournalEntry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().put("mood", e.mood).put("text", e.text).put("ts", e.ts))
        }
        context.journalDataStore.edit { it[key] = arr.toString() }
    }

    suspend fun add(mood: String, text: String) {
        val current = entries.first().toMutableList()
        current.add(0, JournalEntry(mood, text, System.currentTimeMillis()))
        save(current)
    }

    suspend fun remove(index: Int) {
        val current = entries.first().toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            save(current)
        }
    }
}
