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

private val Context.sosDataStore: DataStore<Preferences> by preferencesDataStore(name = "flora_sos")

data class SosContact(val name: String, val number: String)

/**
 * SOS contacts: on-device only, max 3. SOS action never calls or
 * texts silently — it opens the phone's dialer / SMS app with the
 * message pre-filled, and the user presses Call / Send themselves.
 * Nothing is sent, recorded, or uploaded by this store.
 */
class SosStore(private val context: Context) {

    private val key = stringPreferencesKey("sos_contacts_json")


    val contacts: Flow<List<SosContact>> = context.sosDataStore.data.map { prefs ->
        val raw = MemoryCrypto.decodeStored(prefs[key] ?: "[]")
        val out = mutableListOf<SosContact>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                out.add(SosContact(obj.optString("name"), obj.optString("number")))
            }
        } catch (e: Exception) {
            // Corrupt stored JSON: treat as empty rather than crashing.
        }
        out
    }

    private suspend fun save(list: List<SosContact>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("name", it.name).put("number", it.number))
        }
        context.sosDataStore.edit { val enc = MemoryCrypto.encodeStored(arr.toString()); if (enc != null) it[key] = enc }
    }

    /** Returns false when the list is already full (max 3) or input is blank. */
    suspend fun add(name: String, number: String): Boolean {
        val current = contacts.first().toMutableList()
        if (current.size >= 3 || name.isBlank() || number.isBlank()) return false
        current.add(SosContact(name.trim(), number.trim()))
        save(current)
        return true
    }

    suspend fun remove(index: Int) {
        val current = contacts.first().toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            save(current)
        }
    }
}
