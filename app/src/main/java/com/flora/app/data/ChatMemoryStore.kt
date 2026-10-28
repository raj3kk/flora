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

private val Context.chatMemoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "flora_chat_memory")

/**
 * Long memory (Phase 2B, user order 2026-10-29 02:59) — baat-cheet
 * ka persisted record, sirf is phone par, MemoryCrypto (KeyStore
 * AES/GCM) se encrypted. Server, backup ya kisi message par kabhi
 * nahi jata; sirf Flora hi padh sakti hai.
 *
 * - Har user turn + Flora ka jawab yahan save hota hai (FloraApp
 *   ka persist effect), taaki app restart ke baad bhi purani
 *   baat-cheet wapas dikhe aur brain ko history mile.
 * - Is store par 30-din prune NAHI chalta — user ka "long
 *   lasting" order hai. Baaki stores (memories/journal/autolog)
 *   ka prune wahi rahega. Bloat se bachne ke liye sirf aakhri
 *   500 turns rakhe jate hain (time-based delete nahi — ye cap
 *   hai, prune nahi).
 * - Corrupt JSON mile to khali samjha jata hai, crash nahi hota.
 * - Greeting ("Namaste! Main Flora hun...") kabhi save nahi hoti
 *   (FloraApp persist effect use chhod deta hai).
 */
class ChatMemoryStore(private val context: Context) {

    data class StoredTurn(val fromUser: Boolean, val text: String, val ts: Long)

    private val key = stringPreferencesKey("chat_memory_json")

    companion object {
        private const val MAX_TURNS = 500
    }

    val turns: Flow<List<StoredTurn>> = context.chatMemoryDataStore.data.map { prefs ->
        decode(prefs[key] ?: "[]")
    }

    private fun decode(raw: String): List<StoredTurn> {
        val plain = MemoryCrypto.decodeStored(raw)
        val out = mutableListOf<StoredTurn>()
        try {
            val arr = JSONArray(plain)
            for (i in 0 until arr.length()) {
                val el = arr.optJSONObject(i) ?: continue
                val text = el.optString("t", "")
                if (text.isBlank()) continue
                out.add(
                    StoredTurn(
                        fromUser = el.optBoolean("u", false),
                        text = text,
                        ts = el.optLong("ts", 0L)
                    )
                )
            }
        } catch (e: Exception) {
        }
        return out
    }

    private suspend fun save(list: List<StoredTurn>) {
        val arr = JSONArray()
        list.forEach { turn ->
            arr.put(
                JSONObject()
                    .put("u", turn.fromUser)
                    .put("t", turn.text)
                    .put("ts", turn.ts)
            )
        }
        context.chatMemoryDataStore.edit { it[key] = MemoryCrypto.encodeStored(arr.toString()) }
    }

    suspend fun loadAll(): List<StoredTurn> {
        return try {
            turns.first()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addTurn(fromUser: Boolean, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val current = loadAll().toMutableList()
        current.add(StoredTurn(fromUser, clean, System.currentTimeMillis()))
        val kept = if (current.size > MAX_TURNS) {
            current.drop(current.size - MAX_TURNS)
        } else {
            current
        }
        save(kept)
    }

    /** Poori chat memory delete — abhi kisi UI se wired nahi hai;
     *  future me user ke clear confirmation ke baad hi call hoga. */
    suspend fun clearAll() {
        save(emptyList())
    }
}
