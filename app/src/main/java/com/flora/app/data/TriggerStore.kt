package com.flora.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.triggerStore by preferencesDataStore(name = "flora_triggers")

/**
 * Event trigger (parity batch 2026-10-31, M5 — spec
 * FLORA_EVENT_TRIGGERS_SPEC.md ke mutaabiq). v1 me sirf TIME
 * trigger hai: roz ek waqt par fire.
 *
 * actionType "routine" = routine ka naam due-set me jayega
 * (app khulte hi maujooda due-rule: Auto Mode ON ho to routine
 * khud chalegi, warna sirf bataya jayega — trigger khud koi
 * command nahi chalata).
 * actionType "message" = sirf notification, koi action nahi.
 *
 * Call/SMS/payment/delete trigger kabhi nahi — ye design
 * hard rule hai, store isey rokta nahi, receiver kabhi aisa
 * action fire nahi karta (receiver me sirf ye do raaste hain).
 * Phone par hi save hota hai, server par kabhi nahi.
 */
data class EventTrigger(
    val name: String,
    val hour: Int,
    val minute: Int,
    val actionType: String,
    val actionValue: String,
    val enabled: Boolean = true,
)

object TriggerStore {
    private val KEY = stringPreferencesKey("triggers_json")

    suspend fun load(context: Context): List<EventTrigger> {
        val raw = MemoryCrypto.decodeStored(context.triggerStore.data.map { it[KEY] ?: "[]" }.first())
        val out = mutableListOf<EventTrigger>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                out.add(
                    EventTrigger(
                        name = obj.optString("name"),
                        hour = obj.optInt("hour"),
                        minute = obj.optInt("minute"),
                        actionType = obj.optString("actionType", "message"),
                        actionValue = obj.optString("actionValue", ""),
                        enabled = obj.optBoolean("enabled", true),
                    )
                )
            }
        } catch (e: Exception) {
            CatcherHooks.dbError("trigger", "load", "exception")
        }
        return out
    }

    suspend fun save(context: Context, triggers: List<EventTrigger>) {
        val arr = JSONArray()
        for (t in triggers) {
            arr.put(
                JSONObject()
                    .put("name", t.name)
                    .put("hour", t.hour)
                    .put("minute", t.minute)
                    .put("actionType", t.actionType)
                    .put("actionValue", t.actionValue)
                    .put("enabled", t.enabled)
            )
        }
        context.triggerStore.edit { val enc = MemoryCrypto.encodeStored(arr.toString()); if (enc != null) it[KEY] = enc }
    }

    suspend fun upsert(context: Context, trigger: EventTrigger) {
        val triggers = load(context).filter { it.name != trigger.name } + trigger
        save(context, triggers)
    }

    suspend fun remove(context: Context, name: String) {
        save(context, load(context).filter { it.name != name })
    }
}
