package com.flora.app.planner

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flora.app.data.MemoryCrypto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.taskListDataStore: DataStore<Preferences> by preferencesDataStore(name = "flora_tasks")

/**
 * SD-P1 Batch 3 (SD-6 task-list SURFACE, Lane A) — planner core
 * ke UPAR ki list, planner ka replacement nahi.
 *
 * CORE REWRITE FORBIDDEN ka paalan: ye store planner ke Room
 * tables ko chhoota hi nahi hai — tasks alag DataStore me
 * JSON roop me rehte hain (AutoLogStore pattern par,
 * MemoryCrypto se on-device encrypted). Planner se sirf ek
 * taraf ka READ link hai: task par planId likha ho to UI us
 * plan ka status + VERIFIED steps ki ginti marker ke roop me
 * dikhata hai; task ka complete/reopen planner ki state ko
 * kabhi nahi badalta, aur planner ka chalna task ko kabhi
 * auto-complete nahi karta.
 *
 * Maya parity detail UNVERIFIED hai (178 records me task-list
 * ka ek-ek vyavhaar verified nahi mila) — isliye is surface
 * me sirf wahi ops hain jo SD-P0 spec support karta hai:
 * create / list / complete / reopen + due + notes + planner
 * completion marker. Isse zyada parity ka daava nahi hai.
 */
data class UserTask(
    val id: String,
    val title: String,
    val notes: String,
    val dueText: String,
    val done: Boolean,
    val planId: String,
    val createdMs: Long,
)

class TaskListStore(private val context: Context) {

    private val key = stringPreferencesKey("tasks_json")

    private fun toJson(tasks: List<UserTask>): String {
        val arr = JSONArray()
        tasks.forEach { t ->
            arr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("title", t.title)
                    .put("notes", t.notes)
                    .put("due", t.dueText)
                    .put("done", t.done)
                    .put("planId", t.planId)
                    .put("created", t.createdMs),
            )
        }
        return arr.toString()
    }

    private fun fromJson(raw: String): List<UserTask> {
        val out = mutableListOf<UserTask>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                out.add(
                    UserTask(
                        id = o.optString("id", ""),
                        title = o.optString("title", ""),
                        notes = o.optString("notes", ""),
                        dueText = o.optString("due", ""),
                        done = o.optBoolean("done", false),
                        planId = o.optString("planId", ""),
                        createdMs = o.optLong("created", 0L),
                    ),
                )
            }
        } catch (e: Exception) {
        }
        return out.filter { it.id.isNotEmpty() && it.title.isNotEmpty() }
    }

    suspend fun list(): List<UserTask> {
        val raw = MemoryCrypto.decodeStored(
            context.taskListDataStore.data.map { it[key] ?: "[]" }.first(),
        )
        return fromJson(raw).sortedByDescending { it.createdMs }
    }

    suspend fun create(title: String, notes: String, dueText: String, planId: String): UserTask? {
        val clean = title.trim()
        if (clean.isEmpty()) return null
        val task = UserTask(
            id = System.currentTimeMillis().toString() + "-" + (0..9999).random(),
            title = clean,
            notes = notes.trim(),
            dueText = dueText.trim(),
            done = false,
            planId = planId.trim(),
            createdMs = System.currentTimeMillis(),
        )
        val updated = list() + task
        val enc = MemoryCrypto.encodeStored(toJson(updated)) ?: return null
        context.taskListDataStore.edit { it[key] = enc }
        return task
    }

    suspend fun setDone(id: String, done: Boolean): Boolean {
        val updated = list().map { if (it.id == id) it.copy(done = done) else it }
        if (updated.none { it.id == id }) return false
        val enc = MemoryCrypto.encodeStored(toJson(updated)) ?: return false
        context.taskListDataStore.edit { it[key] = enc }
        return true
    }
}
