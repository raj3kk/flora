package com.flora.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.postHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "flora_post_history")

/**
 * P2 consent-gated communication bundle — Post history.
 *
 * Har P2 action (WhatsApp alert, group report, email reply,
 * social post, email read) ka ek entry yahan aata hai —
 * AutoLog se alag, sirf outbound communication ka hisaab.
 * Sirf snippet (pehle 80 akshar) save hota hai, poora
 * private text nahi. On-device encrypted (MemoryCrypto),
 * server par kabhi nahi. Aakhri 200 entries rakhi jati hain.
 *
 * Status ki imaandaari: P2 koi message khud nahi bhejta —
 * hamesha user ke app (WhatsApp / email app / Instagram) me
 * prefilled composer khulta hai aur Send user dabata hai.
 * Isliye status kabhi "bhej diya" nahi kehta — sirf
 * "bhejne ke liye khola gaya" (user ne app me confirm
 * kiya hoga, Flora verify nahi kar sakti).
 */
data class PostEntry(
    val id: String,
    val action: String,
    val target: String,
    val snippet: String,
    val status: String,
    val ts: Long,
)

object PostHistoryStore {

    private val key = stringPreferencesKey("posts_json")

    private fun toJson(entries: List<PostEntry>): String {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("action", e.action)
                    .put("target", e.target)
                    .put("snippet", e.snippet)
                    .put("status", e.status)
                    .put("ts", e.ts),
            )
        }
        return arr.toString()
    }

    private fun fromJson(raw: String): List<PostEntry> {
        val out = mutableListOf<PostEntry>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                out.add(
                    PostEntry(
                        id = o.optString("id", ""),
                        action = o.optString("action", ""),
                        target = o.optString("target", ""),
                        snippet = o.optString("snippet", ""),
                        status = o.optString("status", ""),
                        ts = o.optLong("ts", 0L),
                    ),
                )
            }
        } catch (e: Exception) {
            CatcherHooks.dbError("post-history", "fromJson", "exception")
        }
        return out.filter { it.id.isNotEmpty() }
    }

    suspend fun list(context: Context): List<PostEntry> {
        val raw = MemoryCrypto.decodeStored(
            context.postHistoryDataStore.data.map { it[key] ?: "[]" }.first(),
        )
        return fromJson(raw).sortedByDescending { it.ts }
    }

    suspend fun add(
        context: Context,
        action: String,
        target: String,
        snippet: String,
        status: String,
    ) {
        val entry = PostEntry(
            id = System.currentTimeMillis().toString() + "-" + (0..9999).random(),
            action = action,
            target = target,
            snippet = snippet.take(80),
            status = status,
            ts = System.currentTimeMillis(),
        )
        val updated = (list(context) + entry).takeLast(200)
        val enc = MemoryCrypto.encodeStored(toJson(updated)) ?: return
        context.postHistoryDataStore.edit { it[key] = enc }
    }
}
