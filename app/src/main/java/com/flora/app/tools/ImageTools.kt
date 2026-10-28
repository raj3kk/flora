package com.flora.app.tools

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Image generation (v2.15) — Pollinations, bina key ke.
 *
 * Chat me "image banao <prompt>" bolte hi ye image banata hai
 * aur phone ki Gallery > Flora folder me save karta hai. Koi key
 * nahi chahiye, koi payment nahi. Fail ho to null — chat me
 * jhoothi "ban gayi" kabhi nahi bola jayega.
 */
object ImageTools {
    suspend fun generateImage(context: Context, prompt: String): String? =
        withContext(Dispatchers.IO) {
            val clean = prompt.trim()
            if (clean.isEmpty()) return@withContext null
            val urlText = "https://image.pollinations.ai/prompt/" +
                Uri.encode(clean) + "?width=1024&height=1024&nologo=true"
            val conn = URL(urlText).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = 20_000
                conn.readTimeout = 120_000
                if (conn.responseCode !in 200..299) return@withContext null
                val bytes = conn.inputStream.readBytes()
                if (bytes.size < 10_000) return@withContext null
                val name = "flora_img_" + System.currentTimeMillis() + ".jpg"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Flora")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ) ?: return@withContext null
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                name
            } catch (e: Exception) {
                null
            } finally {
                try { conn.disconnect() } catch (e: Exception) { }
            }
        }
}
