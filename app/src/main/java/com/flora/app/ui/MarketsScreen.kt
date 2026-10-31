package com.flora.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontWeight
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
 *
 * M6 educational section (2026-10-31, is file me neeche):
 * "Seekho" — share market ki basic jaankari, static text,
 * apne shabdon me. Sirf education hai: koi buy/sell, koi
 * tip, koi recommendation, koi portfolio advice nahi.
 * Disclaimer section ke shuru me screen par hai.
 */
/**
 * M6 educational topics — static text, apne shabdon me.
 * Koi price, koi tip, koi recommendation isme nahi hai.
 * Sirf samajhne ke liye basic jaankari hai.
 */
private data class LearnTopic(val title: String, val body: String)

private val learnTopics = listOf(
    LearnTopic(
        "Share market kya hai?",
        "Companies apne business ke liye paisa jutane ke liye apne hisse (shares) " +
            "aam logon ko bechti hain. Stock exchange (jaise NSE, BSE) wo regulated " +
            "jagah hai jahan ye shares kharide aur beche jaate hain. Koi share ka " +
            "price us waqt ke kharidne-beche walon ki demand aur supply se banta hai."
    ),
    LearnTopic(
        "Share kharidne ka matlab kya hai?",
        "Share kharidna matlab us company ke ownership ka ek chhota sa hissa lena. " +
            "Company munafa kamae aur baantna chahe to dividend mil sakta hai. " +
            "Share ka price badhe to fayda ho sakta hai, gire to nuksan. Koi bhi " +
            "share price badhega hi, iski koi guarantee nahi hoti."
    ),
    LearnTopic(
        "Zaroori shabd (glossary)",
        "Share/Stock: company ke ownership ka ek hissa.\n" +
            "Stock Exchange (NSE/BSE): jahan shares kharide-bechte hain.\n" +
            "Sensex/Nifty: index — badi companies ke shares ka average, jo market " +
            "ki overall haalat ka andaza deta hai.\n" +
            "Demat Account: shares ko electronic form me rakhne ka account.\n" +
            "Broker: exchange tak pahunchane wala licensed bichauliya.\n" +
            "IPO: jab company pehli baar aam logon ko shares beche.\n" +
            "Dividend: company ke munafe ka hissa jo shareholders ko milta hai.\n" +
            "Bull/Bear: badhta hua market (bull), girta hua market (bear).\n" +
            "Market Cap: company ke sabhi shares ki total keemat.\n" +
            "P/E Ratio: share price vs company ki kamai ka ratio — sirf ek andaza, " +
            "kisi faisle ki akeli wajah nahi."
    ),
    LearnTopic(
        "Price up-down kyun hota hai?",
        "Sabse bada kaaran demand-supply hai. Uske alawa company ke results, " +
            "sector ya economy ki khabar, interest rate, aur duniya bhar ki badi " +
            "ghatnaon se bhi price hilta hai. Afwah par bhi price hil sakta hai — " +
            "isliye short-term me price ka andaza lagana bahut mushkil hota hai."
    ),
    LearnTopic(
        "Risk kya-kya hain?",
        "Capital loss: lagaya hua paisa kam ya poora doob sakta hai.\n" +
            "Volatility: price tezi se upar-neeche ho sakta hai.\n" +
            "Udhaar lekar trading (leverage): nuksan ko kai guna bada deta hai.\n" +
            "'Guaranteed return' ya 'paisa double' wale daave aksar fraud hote hain.\n" +
            "Kisi ki tip par andha bharosa karke paisa lagana aksar nuksan karta hai."
    ),
    LearnTopic(
        "Shuru karne se pehle kya samajhna zaroori hai?",
        "Pehle emergency fund rakho. Udhaar ka paisa market me kabhi mat lagao. " +
            "Long-term nivesh aur roz ki speculation alag cheezein hain — dono ka " +
            "risk alag hai. Diversification ka matlab hai saara paisa ek jagah na " +
            "lagana. Faisla hamesha khud samajhkar lo, kisi ke kehne par nahi. " +
            "Chhota shuru karo aur seekhte raho."
    )
)

@Composable
private fun LearnTopicItem(topic: LearnTopic) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(topic.title, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Band karo" else "Padho")
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(topic.body)
        }
    }
}

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
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Flora watchlist", WatchlistStore.exportText(symbols)))
                    note = "Watchlist copy ho gayi — ${symbols.size} symbol."
                },
                enabled = symbols.isNotEmpty()
            ) { Text("Watchlist export karo (copy)") }
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
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Seekho — Share Market ki basic jaankari (sirf education)",
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Disclaimer: Ye section SIRF education ke liye hai. Ye investment " +
                    "advice, tip, ya buy/sell recommendation NAHI hai. Flora koi share " +
                    "recommend nahi karti aur koi portfolio advice nahi deti. Paisa " +
                    "lagane se pehle khud research karo aur zaroorat ho to " +
                    "SEBI-registered investment advisor se salah lo. Share market me " +
                    "paisa doobne ka risk hota hai — sirf wahi paisa lagana jo nuksan " +
                    "hone par bardaasht kar sako."
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(learnTopics) { topic ->
            LearnTopicItem(topic)
        }
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Flora kya NAHI karti: buy/sell/order nahi, koi share ya strategy " +
                    "recommend nahi, koi tip nahi, koi portfolio advice nahi. " +
                    "Is screen par sirf education aur read-only watchlist hai."
            )
        }
    }
}
