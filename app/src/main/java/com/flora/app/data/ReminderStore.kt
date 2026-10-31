package com.flora.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.reminderStore by preferencesDataStore(name = "flora_reminders")

/**
 * Reminder (parity batch 2026-10-31, M2 — Maya ReminderReceiver ka
 * Flora version, apne code se). Ek reminder = title + roz ka waqt
 * + ek chhota message. Routine jobs se ALAG store hai, taaki
 * title kabhi kisi routine ke schedule se takra na jaye (JobStore
 * upsert routineName se hota hai — wahi store use karte to ek
 * reminder kisi routine ka alarm overwrite kar sakta tha).
 *
 * Alarm pattern JobScheduler jaisa hi hai (AlarmManager, exact
 * mile to exact warna inexact fallback, WorkManager nahi) — naya
 * alarm infra nahi, wahi pattern doosre action par. Fire hone
 * par sirf notification + agle din ka alarm; koi routine, koi
 * action, koi SMS/call kabhi nahi chalta — reminder sirf yaad
 * dilata hai. Phone par hi save hota hai, server par kabhi nahi.
 */
data class Reminder(
    val title: String,
    val hour: Int,
    val minute: Int,
    val message: String = "",
    val enabled: Boolean = true,
)

object ReminderStore {
    private val KEY = stringPreferencesKey("reminders_json")

    suspend fun load(context: Context): List<Reminder> {
        val raw = MemoryCrypto.decodeStored(context.reminderStore.data.map { it[KEY] ?: "[]" }.first())
        val out = mutableListOf<Reminder>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                out.add(
                    Reminder(
                        title = obj.optString("title"),
                        hour = obj.optInt("hour"),
                        minute = obj.optInt("minute"),
                        message = obj.optString("message", ""),
                        enabled = obj.optBoolean("enabled", true),
                    )
                )
            }
        } catch (e: Exception) {
            CatcherHooks.dbError("reminder", "load", "exception")
        }
        return out
    }

    suspend fun save(context: Context, reminders: List<Reminder>) {
        val arr = JSONArray()
        for (r in reminders) {
            arr.put(
                JSONObject()
                    .put("title", r.title)
                    .put("hour", r.hour)
                    .put("minute", r.minute)
                    .put("message", r.message)
                    .put("enabled", r.enabled)
            )
        }
        context.reminderStore.edit { val enc = MemoryCrypto.encodeStored(arr.toString()); if (enc != null) it[KEY] = enc }
    }

    suspend fun upsert(context: Context, reminder: Reminder) {
        val reminders = load(context).filter { it.title != reminder.title } + reminder
        save(context, reminders)
    }

    suspend fun remove(context: Context, title: String) {
        save(context, load(context).filter { it.title != title })
    }
}
