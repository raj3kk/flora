package com.flora.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.macroStore by preferencesDataStore(name = "flora_macros")

/**
 * Phase 6 — Saved JSON macros. Sirf on-device (DataStore,
 * MemoryCrypto se encrypted), server par kabhi nahi. JSON
 * raw save hota hai; parse chalane/save-validate karte waqt
 * hota hai — kharab JSON wala macro list me dikhega par
 * chalega nahi (imaandaar message ke saath).
 */
data class StoredMacro(val name: String, val json: String)

object MacroStore {
    private val KEY = stringPreferencesKey("macros_json")

    suspend fun load(context: Context): List<StoredMacro> {
        val raw = MemoryCrypto.decodeStored(context.macroStore.data.map { it[KEY] ?: "[]" }.first())
        val out = mutableListOf<StoredMacro>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name", "")
                val json = obj.optString("json", "")
                if (name.isNotBlank() && json.isNotBlank()) {
                    out.add(StoredMacro(name, json))
                }
            }
        } catch (e: Exception) {
            CatcherHooks.dbError("macro", "load", "exception")
        }
        return out
    }

    suspend fun save(context: Context, macros: List<StoredMacro>) {
        val arr = JSONArray()
        for (m in macros) {
            val obj = JSONObject()
            obj.put("name", m.name)
            obj.put("json", m.json)
            arr.put(obj)
        }
        context.macroStore.edit { val enc = MemoryCrypto.encodeStored(arr.toString()); if (enc != null) it[KEY] = enc }
    }
}
