package com.flora.app.tools

import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * No-key tools: DuckDuckGo instant answer, Open-Meteo weather,
 * Frankfurter currency rates, free Dictionary API.
 * No API key is needed or used for any of them. A failed call
 * returns an honest error string, never an invented answer.
 * v2.1: optional Tavily lane (key ho to pehle, warna DuckDuckGo).
 */
class SearchTools(private val prefs: FloraPreferences? = null) {

    private fun get(urlText: String): String? {
        val conn = URL(urlText).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("User-Agent", "Flora/0.5")
            if (conn.responseCode !in 200..299) return null
            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).readText()
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    suspend fun search(query: String): String = withContext(Dispatchers.IO) {
        // Tavily lane (v2.1): key ho to pehle try karo, fail ho to DuckDuckGo.
        val tavilyKey = prefs?.tavilyApiKey() ?: ""
        if (tavilyKey.isNotEmpty()) {
            val tavilyResult = tavilySearch(query, tavilyKey)
            if (tavilyResult != null) return@withContext tavilyResult
        }
        val encoded = URLEncoder.encode(query, "UTF-8")
        val text = get("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1")
            ?: return@withContext "Search nahi ho paya — network ya server ki dikkat hai."
        try {
            val json = JSONObject(text)
            val abstract = json.optString("AbstractText", "")
            if (abstract.isNotBlank()) return@withContext abstract
            val answer = json.optString("Answer", "")
            if (answer.isNotBlank()) return@withContext answer
            val topics = json.optJSONArray("RelatedTopics")
            if (topics != null) {
                for (i in 0 until topics.length()) {
                    val t = topics.optJSONObject(i)?.optString("Text", "") ?: ""
                    if (t.isNotBlank()) return@withContext t
                }
            }
            "Is sawal ka seedha jawab nahi mila."
        } catch (e: Exception) {
            "Search ka jawab samajh nahi aaya, dobara koshish kijiye."
        }
    }

    private fun tavilySearch(query: String, key: String): String? {
        val body = JSONObject()
            .put("api_key", key)
            .put("query", query)
            .put("max_results", 3)
            .put("include_answer", true)
        val text = postJson("https://api.tavily.com/search", body) ?: return null
        return try {
            val json = JSONObject(text)
            val answer = json.optString("answer", "")
            if (answer.isNotBlank()) return answer + "\n\nSource: Tavily"
            val results = json.optJSONArray("results")
            if (results != null && results.length() > 0) {
                val first = results.optJSONObject(0)?.optString("content", "") ?: ""
                if (first.isNotBlank()) return first + "\n\nSource: Tavily"
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun postJson(urlText: String, body: JSONObject): String? {
        val conn = URL(urlText).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            if (conn.responseCode !in 200..299) return null
            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).readText()
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    suspend fun weather(city: String): String = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(city, "UTF-8")
        val geoText = get("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json")
            ?: return@withContext "Mausam nahi mil paya — network ya server ki dikkat hai."
        try {
            val results = JSONObject(geoText).optJSONArray("results")
            val first = results?.optJSONObject(0)
                ?: return@withContext "Ye jagah nahi mili, naam check karke dobara likhiye."
            val lat = first.optDouble("latitude")
            val lon = first.optDouble("longitude")
            val name = first.optString("name", city)
            val country = first.optString("country", "")
            val fcText = get(
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,wind_speed_10m"
            ) ?: return@withContext "Mausam ka data nahi mila."
            val current = JSONObject(fcText).optJSONObject("current")
            val temp = current?.optDouble("temperature_2m", Double.NaN) ?: Double.NaN
            val humidity = current?.optInt("relative_humidity_2m", -1)
            val wind = current?.optDouble("wind_speed_10m", Double.NaN)
            if (temp.isNaN()) return@withContext "Mausam ka data nahi mila."
            "$name, $country — abhi $temp°C, humidity $humidity%, hawa $wind km/h."
        } catch (e: Exception) {
            "Mausam ka jawab samajh nahi aaya, dobara koshish kijiye."
        }
    }

    suspend fun convertCurrency(amount: Double, from: String, to: String): String =
        withContext(Dispatchers.IO) {
            val fromCode = from.trim().uppercase()
            val toCode = to.trim().uppercase()
            if (fromCode.isEmpty() || toCode.isEmpty() || fromCode.length != 3 || toCode.length != 3) {
                return@withContext "Currency code 3 akshar ka likhiye, jaise USD, INR, EUR."
            }
            val text = get("https://api.frankfurter.app/latest?amount=$amount&from=$fromCode&to=$toCode")
                ?: return@withContext "Currency rate nahi mil paya — network ya server ki dikkat hai."
            try {
                val rates = JSONObject(text).optJSONObject("rates")
                val converted = rates?.optDouble(toCode, Double.NaN) ?: Double.NaN
                if (converted.isNaN()) return@withContext "Ye currency pair nahi mila, code check kijiye."
                "$amount $fromCode = $converted $toCode (aaj ka rate, Frankfurter API)"
            } catch (e: Exception) {
                "Currency ka jawab samajh nahi aaya, dobara koshish kijiye."
            }
        }

    suspend fun defineWord(word: String): String = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(word.trim(), "UTF-8")
        val text = get("https://api.dictionaryapi.dev/api/v2/entries/en/$encoded")
            ?: return@withContext "Shabd ka matlab nahi mil paya — network ya server ki dikkat hai."
        try {
            val entry = JSONArray(text).optJSONObject(0)
                ?: return@withContext "Ye shabd dictionary me nahi mila."
            val meanings = entry.optJSONArray("meanings")
            val firstMeaning = meanings?.optJSONObject(0)
            val defs = firstMeaning?.optJSONArray("definitions")
            val definition = defs?.optJSONObject(0)?.optString("definition", "")
            if (definition.isNullOrBlank()) return@withContext "Is shabd ka matlab nahi mila."
            val part = firstMeaning?.optString("partOfSpeech", "") ?: ""
            "${word.trim()} ($part): $definition"
        } catch (e: Exception) {
            "Ye shabd dictionary me nahi mila, spelling check kijiye."
        }
    }

    /** Study (v2.0): topic par 5 tak sources ka short summary, source ke naam ke saath. */
    suspend fun study(topic: String): String = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(topic.trim(), "UTF-8")
        val text = get("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1")
            ?: return@withContext "Study search nahi ho paya — network ya server ki dikkat hai."
        try {
            val json = JSONObject(text)
            val parts = mutableListOf<String>()
            val abstract = json.optString("AbstractText", "")
            if (abstract.isNotBlank()) parts.add(abstract)
            val topics = json.optJSONArray("RelatedTopics")
            if (topics != null) {
                for (i in 0 until topics.length()) {
                    if (parts.size >= 5) break
                    val t = topics.optJSONObject(i)?.optString("Text", "") ?: ""
                    if (t.isNotBlank()) parts.add(t)
                }
            }
            if (parts.isEmpty()) return@withContext "Is topic par kuch nahi mila, topic badal kar koshish kijiye."
            parts.joinToString("\n\n") + "\n\nSource: DuckDuckGo"
        } catch (e: Exception) {
            "Study ka jawab samajh nahi aaya, dobara koshish kijiye."
        }
    }

    /** Market (v2.0): Yahoo Finance se price/change. Bina key. Price ki pushti exchange/app se karo. */
    suspend fun stockPrice(symbol: String): String = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(symbol.trim().uppercase(), "UTF-8")
        val text = get("https://query1.finance.yahoo.com/v8/finance/chart/$encoded")
            ?: return@withContext "Market price nahi mil paya — network ya server ki dikkat hai."
        try {
            val meta = JSONObject(text)
                .optJSONObject("chart")?.optJSONArray("result")?.optJSONObject(0)?.optJSONObject("meta")
                ?: return@withContext "Ye symbol nahi mila, symbol check kijiye (jaise RELIANCE.NS, AAPL)."
            val price = meta.optDouble("regularMarketPrice", Double.NaN)
            if (price.isNaN()) return@withContext "Is symbol ka price nahi mila."
            val currency = meta.optString("currency", "")
            val change = meta.optDouble("regularMarketChangePercent", Double.NaN)
            val changeText = if (change.isNaN()) "" else " (${String.format("%.2f", change)}%)"
            "${symbol.trim().uppercase()}: $price $currency$changeText\nNote: live price ki pushti exchange/app se karo. Source: Yahoo Finance"
        } catch (e: Exception) {
            "Market ka jawab samajh nahi aaya, dobara koshish kijiye."
        }
    }

    /** Website starter (v2.0): sirf local text — pages ka structure + Home ka HTML skeleton. */
    fun websiteStarter(idea: String): String {
        val safeIdea = idea.trim()
        return "Website idea: $safeIdea\n\nPages: Home, About, Services, Contact\n\n" +
            "Home ka starter HTML:\n<html>\n<head><title>$safeIdea</title></head>\n" +
            "<body>\n<h1>$safeIdea</h1>\n<p>Yahan apne business ke baare me 2-3 line likhiye.</p>\n" +
            "<h2>Services</h2>\n<p>Apni 3 main services yahan likhiye.</p>\n" +
            "<h2>Contact</h2>\n<p>Phone/WhatsApp/email yahan likhiye.</p>\n</body>\n</html>\n\n" +
            "Note: ye sirf starter text hai, file save nahi hui hai."
    }
}
