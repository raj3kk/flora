package com.flora.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flora.app.data.WatchlistStore
import com.flora.app.tools.SearchTools
import kotlinx.coroutines.launch

/**
 * Markets screen (parity batch 2026-10-31, M7 — spec
 * FLORA_MARKETS_SPEC.md). READ-ONLY watchlist: Yahoo Finance
 * lane se price/change dikhta hai. Koi trading nahi — buy/sell/
 * order/advice is screen me kabhi nahi hoga. Price indicative
 * hai; live price ki pushti exchange/app se karo (ye line
 * screen par bhi hai). Symbols sirf is phone par save hote
 * hain. Network fail ho to imaandaar error, invented price
 * kabhi nahi.
 */
@Composable
fun MarketsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val searchTools = remember { SearchTools() }
    var symbols by remember { mutableStateOf<List<String>>(emptyList()) }
    var quotes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var newSymbol by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        symbols = WatchlistStore.load(context)
    }

    fun refreshAll(list: List<String>) {
        if (list.isEmpty()) return
        refreshing = true
        scope.launch {
            val out = mutableMapOf<String, String>()
            for (symbol in list) {
                out[symbol] = searchTools.stockPrice(symbol)
            }
            quotes = out
            refreshing = false
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Markets — read-only watchlist (koi trading nahi).")
            Text("Price indicative hai — live price ki pushti exchange/app se karo. Source: Yahoo Finance.")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = newSymbol, onValueChange = { newSymbol = it },
                label = { Text("Symbol (jaise RELIANCE.NS, AAPL)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val clean = newSymbol.trim().uppercase()
                if (clean.isEmpty()) {
                    note = "Symbol khaali hai."
                    return@Button
                }
                scope.launch {
                    WatchlistStore.add(context, clean)
                    val list = WatchlistStore.load(context)
                    symbols = list
                    newSymbol = ""
                    refreshAll(list)
                }
            }) { Text("Watchlist me jodo") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { refreshAll(symbols) },
                enabled = symbols.isNotEmpty() && !refreshing
            ) { Text(if (refreshing) "Refresh ho raha hai…" else "Prices refresh karo") }
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        items(symbols) { symbol ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    quotes[symbol] ?: "$symbol — refresh karo.",
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    scope.launch {
                        WatchlistStore.remove(context, symbol)
                        symbols = WatchlistStore.load(context)
                    }
                }) { Text("Hatao") }
            }
        }
    }
}
