package com.flora.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.chainStore by preferencesDataStore(name = "flora_chains")

/**
 * Job chain (v2.22, Phase 5) — ek naam
 * ke neeche kai routines ka kram. (Pehle is comment par
 * galat G12 label tha — G12 number ab sub-agent
 * orchestrator ka hai, Phase 9 A2. Behaviour wahi hai.)
 * Chain "chain chalao
 * <naam>" se chalti hai (Auto Mode ON ho tabhi bina
 * poochhe; warna Routines screen par khulti hai).
 * Data encrypted hai (MemoriesStore pattern) — routines
 * ke naam user ka kaam batate hain, isliye plain nahi.
 */
data class JobChain(
    val name: String,
    val routineNames: List<String>,
)

object JobChainStore {
    private val KEY = stringPreferencesKey("chains_json")


    suspend fun load(context: Context): List<JobChain> {
        val raw = MemoryCrypto.decodeStored(context.chainStore.data.map { it[KEY] ?: "[]" }.first())
        val out = mutableListOf<JobChain>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val names = mutableListOf<String>()
                val nameArr = obj.optJSONArray("routines") ?: JSONArray()
                for (j in 0 until nameArr.length()) names.add(nameArr.getString(j))
                out.add(JobChain(obj.optString("name"), names))
            }
        } catch (e: Exception) {
        }
        return out
    }

    suspend fun save(context: Context, chains: List<JobChain>) {
        val arr = JSONArray()
        for (c in chains) {
            val obj = JSONObject()
            obj.put("name", c.name)
            val nameArr = JSONArray()
            for (n in c.routineNames) nameArr.put(n)
            obj.put("routines", nameArr)
            arr.put(obj)
        }
        context.chainStore.edit { val enc = MemoryCrypto.encodeStored(arr.toString()); if (enc != null) it[KEY] = enc }
    }

    suspend fun upsert(context: Context, chain: JobChain) {
        val list = load(context).toMutableList()
        list.removeAll { it.name.equals(chain.name, ignoreCase = true) }
        list.add(chain)
        save(context, list)
    }
}
