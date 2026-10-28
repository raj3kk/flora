package com.flora.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.jobStore by preferencesDataStore(name = "flora_jobs")

/**
 * Job (v2.22, chunk 4): ek routine ko roz ek waqt par chalane
 * ka schedule. Ek routine ka ek hi schedule. Phone par hi
 * save hota hai — server par kabhi nahi.
 */
data class Job(val routineName: String, val hour: Int, val minute: Int, val enabled: Boolean = true)

object JobStore {
    private val KEY = stringPreferencesKey("jobs_json")

    /** Ciphertext ko plain me badlo; fail ho to raw (purana plain). */
    private fun decode(raw: String): String = MemoryCrypto.decrypt(raw) ?: raw

    /** Plain ko ciphertext me badlo; fail ho to plain (data safe). */
    private fun encode(json: String): String = MemoryCrypto.encrypt(json) ?: json

    suspend fun load(context: Context): List<Job> {
        val raw = decode(context.jobStore.data.map { it[KEY] ?: "[]" }.first())
        val out = mutableListOf<Job>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                out.add(
                    Job(
                        routineName = obj.optString("routine"),
                        hour = obj.optInt("hour"),
                        minute = obj.optInt("minute"),
                        enabled = obj.optBoolean("enabled", true)
                    )
                )
            }
        } catch (e: Exception) {
        }
        return out
    }

    suspend fun save(context: Context, jobs: List<Job>) {
        val arr = JSONArray()
        for (j in jobs) {
            arr.put(
                JSONObject()
                    .put("routine", j.routineName)
                    .put("hour", j.hour)
                    .put("minute", j.minute)
                    .put("enabled", j.enabled)
            )
        }
        context.jobStore.edit { it[KEY] = encode(arr.toString()) }
    }

    suspend fun upsert(context: Context, job: Job) {
        val jobs = load(context).filter { it.routineName != job.routineName } + job
        save(context, jobs)
    }

    suspend fun remove(context: Context, routineName: String) {
        save(context, load(context).filter { it.routineName != routineName })
    }
}
