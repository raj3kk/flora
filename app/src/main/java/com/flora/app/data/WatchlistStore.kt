package com.flora.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.watchlistStore by preferencesDataStore(name = "flora_watchlist")

/**
 * Markets watchlist (parity batch 2026-10-31, M7 — spec
 * FLORA_MARKETS_SPEC.md). Sirf symbols ki list hai — koi
 * price yahan save nahi hota (price har refresh par Yahoo
 * Finance lane se live aata hai). Koi trading state nahi,
 * koi order nahi — watchlist read-only hai. Phone par hi
 * save hoti hai, server par kabhi nahi.
 */
object WatchlistStore {
    private val KEY = stringPreferencesKey("watchlist_json")

    suspend fun load(context: Context): List<String> {
        val raw = MemoryCrypto.decodeStored(context.watchlistStore.data.map { it[KEY] ?: "[]" }.first())
        val out = mutableListOf<String>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val s = arr.optString(i)
                if (s.isNotBlank()) out.add(s)
            }
        } catch (e: Exception) {
        }
        return out
    }

    suspend fun save(context: Context, symbols: List<String>) {
        val arr = JSONArray()
        for (s in symbols) arr.put(s)
        context.watchlistStore.edit { val enc = MemoryCrypto.encodeStored(arr.toString()); if (enc != null) it[KEY] = enc }
    }

    suspend fun add(context: Context, symbol: String) {
        val clean = symbol.trim().uppercase()
        if (clean.isEmpty()) return
        val symbols = load(context).filter { it != clean } + clean
        save(context, symbols)
    }

    suspend fun remove(context: Context, symbol: String) {
        save(context, load(context).filter { it != symbol })
    }

    /** GAP-M8 (Part 1): watchlist export — ek symbol per line, plain text. Pure, koi IO nahi. */
    fun exportText(symbols: List<String>): String =
        symbols.filter { it.isNotBlank() }.joinToString("\n")
}
