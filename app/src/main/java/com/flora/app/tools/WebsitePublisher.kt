package com.flora.app.tools

import com.flora.app.core.FloraConfig
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Website publish (v2.22, G19 "impossible → possible").
 *
 * "Website banao" se bani HTML ko Flora ke apne Supabase bucket
 * flora-sites me upload karta hai, taaki ek public link ban jaye.
 * Bucket public-read + upload policy dashboard se lagti hai (docs me);
 * bucket/policy na ho to failure reason wapas aata hai, crash nahi.
 * Koi key/password yahan hardcode nahi — anon key caller (prefs) se aati hai.
 */
object WebsitePublisher {

    /**
     * @return Result.success(publicUrl) ya Result.failure(reason ke saath).
     */
    suspend fun publish(html: String, slug: String, anonKey: String): Result<String> {
        return try {
            val path = "/storage/v1/object/flora-sites/$slug/index.html?upsert=true"
            val conn = (URL(FloraConfig.SUPABASE_URL + path).openConnection()
                as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 20000
                readTimeout = 30000
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
                setRequestProperty("Content-Type", "text/html")
            }
            val bytes = html.toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(bytes.size)
            var out: OutputStream? = null
            try {
                out = conn.outputStream
                out.write(bytes)
                out.flush()
            } finally {
                try { out?.close() } catch (e: Exception) { }
            }
            val code = conn.responseCode
            if (code in 200..299) {
                Result.success(FloraConfig.SUPABASE_URL + "/storage/v1/object/public/flora-sites/$slug/index.html")
            } else {
                val body = try {
                    conn.errorStream?.bufferedReader()?.readText()?.take(200) ?: ""
                } catch (e: Exception) { "" }
                Result.failure(Exception("Upload nahi hua (HTTP $code) $body — bucket flora-sites ya uski policy dashboard se lagani baaki ho sakti hai."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Upload me dikkat: ${e.message ?: "internet check karo"}"))
        }
    }
}
