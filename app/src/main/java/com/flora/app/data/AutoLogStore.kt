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

private val Context.autoLogDataStore: DataStore<Preferences> by preferencesDataStore(name = "flora_autolog")

/**
 * Auto Log (v2.17): Auto mode me Flora ne bina poochhe jo bhi kiya,
 * uska on-device hisaab. Sirf is phone par, koi server/backup nahi.
 * 30 din se purani entries app-start par auto-delete hoti hain.
 * Ye log hi "silent" ko user-authorized banata hai — jo hua, wo
 * yahan dikhega, chhupke kuch nahi hoga.
 */
class AutoLogStore(private val context: Context) {

    private val key = stringPreferencesKey("autolog_json")

    val items: Flow<List<Pair<String, Long>>> = context.autoLogDataStore.data.map { prefs ->
        val raw = prefs[key] ?: "[]"
        val out = mutableListOf<Pair<String, Long>>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val el = arr.opt(i)
                if (el is JSONObject) {
                    out.add(el.optString("text", "") to el.optLong("ts", 0L))
                }
            }
        } catch (e: Exception) {
        }
        out
    }

    suspend fun add(text: String) {
        val raw = context.autoLogDataStore.data.map { it[key] ?: "[]" }.first()
        val list = mutableListOf<Pair<String, Long>>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val el = arr.opt(i)
                if (el is JSONObject) list.add(el.optString("text", "") to el.optLong("ts", 0L))
            }
        } catch (e: Exception) {
        }
        list.add(text to System.currentTimeMillis())
        val arr = JSONArray()
        list.takeLast(500).forEach { (t, ts) ->
            arr.put(JSONObject().put("text", t).put("ts", ts))
        }
        context.autoLogDataStore.edit { it[key] = arr.toString() }
    }

    suspend fun clear() {
        context.autoLogDataStore.edit { it[key] = "[]" }
    }

    suspend fun pruneOlderThan(cutoffMs: Long) {
        val raw = context.autoLogDataStore.data.map { it[key] ?: "[]" }.first()
        val kept = JSONArray()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val el = arr.opt(i)
                if (el is JSONObject && el.optLong("ts", 0L) >= cutoffMs) kept.put(el)
            }
        } catch (e: Exception) {
        }
        context.autoLogDataStore.edit { it[key] = kept.toString() }
    }
}
