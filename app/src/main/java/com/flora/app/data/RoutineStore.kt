package com.flora.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.routineStore by preferencesDataStore(name = "flora_routines")

/**
 * Routines (v1.1, draft — FloraApp wiring v1.0 fix ke baad).
 * Ek routine = naam + commands ki list. Execution: ek confirmation
 * ke baad har command baari-baari chalega, har step bol kar.
 * Routines sirf on-device save hoti hain, server par kabhi nahi.
 * Payment/delete wale step par dobara confirmation hoga — ye rule
 * routine se bhi upar hai.
 */
data class Routine(val name: String, val commands: List<String>)

object RoutineStore {
    private val KEY = stringPreferencesKey("routines_json")

    /** Ciphertext ko plain me badlo; fail ho to raw (purana plain). */
    private fun decode(raw: String): String = MemoryCrypto.decrypt(raw) ?: raw

    /** Plain ko ciphertext me badlo; fail ho to plain (data safe). */
    private fun encode(json: String): String = MemoryCrypto.encrypt(json) ?: json

    suspend fun load(context: Context): List<Routine> {
        val raw = decode(context.routineStore.data.map { it[KEY] ?: "[]" }.first())
        val out = mutableListOf<Routine>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val cmds = mutableListOf<String>()
                val cmdArr = obj.optJSONArray("commands") ?: JSONArray()
                for (j in 0 until cmdArr.length()) cmds.add(cmdArr.getString(j))
                out.add(Routine(obj.optString("name"), cmds))
            }
        } catch (e: Exception) {
        }
        return out
    }

    suspend fun save(context: Context, routines: List<Routine>) {
        val arr = JSONArray()
        for (r in routines) {
            val obj = JSONObject()
            obj.put("name", r.name)
            val cmdArr = JSONArray()
            for (c in r.commands) cmdArr.put(c)
            obj.put("commands", cmdArr)
            arr.put(obj)
        }
        context.routineStore.edit { it[KEY] = encode(arr.toString()) }
    }
}
