package com.flora.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.notifStore by preferencesDataStore(name = "flora_notifications")

/**
 * Notification list (v2.22, P2-F10, READ ONLY) — Flora sirf
 * notifications padhti hai, kabhi dismiss/reply/action nahi
 * karti. List sirf is phone par rehti hai (DataStore), server
 * par kabhi nahi jati. Zyada se zyada 20, uske baad purani
 * apne aap hat jati hai. Text 200 chars par kata jata hai.
 * Flora ki apni notifications isme kabhi nahi aati.
 */
data class NotifItem(
    val pkg: String,
    val title: String,
    val text: String,
    val timeMs: Long,
)

object NotificationStore {
    private val KEY = stringPreferencesKey("notifs_json")
    private const val MAX = 20


    suspend fun load(context: Context): List<NotifItem> {
        val raw = MemoryCrypto.decodeStored(context.notifStore.data.map { it[KEY] ?: "[]" }.first())
        val out = mutableListOf<NotifItem>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                out.add(
                    NotifItem(
                        pkg = obj.optString("pkg"),
                        title = obj.optString("title"),
                        text = obj.optString("text"),
                        timeMs = obj.optLong("time"),
                    )
                )
            }
        } catch (e: Exception) {
        }
        return out
    }

    suspend fun add(context: Context, item: NotifItem) {
        val items = (load(context) + item).takeLast(MAX)
        val arr = JSONArray()
        for (it in items) {
            arr.put(
                JSONObject()
                    .put("pkg", it.pkg)
                    .put("title", it.title)
                    .put("text", it.text)
                    .put("time", it.timeMs)
            )
        }
        try {
            context.notifStore.edit { prefs -> prefs[KEY] = MemoryCrypto.encodeStored(arr.toString()) }
        } catch (e: Exception) {
        }
    }

    suspend fun clear(context: Context) {
        try {
            context.notifStore.edit { prefs -> prefs[KEY] = "[]" }
        } catch (e: Exception) {
        }
    }
}
