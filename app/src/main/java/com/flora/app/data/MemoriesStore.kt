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

private val Context.memoriesDataStore: DataStore<Preferences> by preferencesDataStore(name = "flora_memories")

/**
 * Yaadein (Memories): on-device only, stored as a JSON array in
 * DataStore on this phone. Nothing is sent to any server, backup, or
 * message. Edit = replace at index; delete = remove at index.
 */
class MemoriesStore(private val context: Context) {

    private val key = stringPreferencesKey("memories_json")

    val items: Flow<List<String>> = context.memoriesDataStore.data.map { prefs ->
        val raw = prefs[key] ?: "[]"
        val out = mutableListOf<String>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) out.add(arr.optString(i))
        } catch (e: Exception) {
            // Corrupt stored JSON: treat as empty rather than crashing.
        }
        out
    }

    private suspend fun save(list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        context.memoriesDataStore.edit { it[key] = arr.toString() }
    }

    suspend fun add(text: String) {
        val current = items.first().toMutableList()
        current.add(text)
        save(current)
    }

    suspend fun update(index: Int, text: String) {
        val current = items.first().toMutableList()
        if (index in current.indices) {
            current[index] = text
            save(current)
        }
    }

    suspend fun remove(index: Int) {
        val current = items.first().toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            save(current)
        }
    }

    /** Sab kuch delete (user ke clear confirmation ke baad hi call hota hai). */
    suspend fun clearAll() {
        save(emptyList())
    }
}
