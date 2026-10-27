package com.flora.app.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * No-key tools: DuckDuckGo instant answer + Open-Meteo weather.
 * No API key is needed or used for either. A failed call returns an
 * honest error string, never an invented answer.
 */
class SearchTools {

    private fun get(urlText: String): String? {
        val conn = URL(urlText).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("User-Agent", "Flora/0.2")
            if (conn.responseCode !in 200..299) return null
            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).readText()
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    suspend fun search(query: String): String = withContext(Dispatchers.IO) {
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
}
