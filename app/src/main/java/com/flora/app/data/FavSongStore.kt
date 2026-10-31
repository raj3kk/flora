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

private val Context.favSongDataStore: DataStore<Preferences> by preferencesDataStore(name = "flora_fav_songs")

/**
 * P3 favourite-song storage/selection (on-device only).
 *
 * Sirf storage + selection hai — koi naya player nahi
 * banaya gaya hai. Play hamesha maujooda raaste se hoga
 * (YouTube search via VerifiedAppLauncher, jaise "gaana
 * bajao" command karta hai); play/pause/next control SD-8
 * 8.8 (MediaSession) ka hai, yahan duplicate nahi hua hai.
 *
 * MUSIC-APP PAIRING HELD (audit C7 NEEDS-DEFINITION):
 * kaunsa gaana kis music app me bajega, ye pairing abhi
 * define nahi hui hai — isliye is store me koi app-pairing
 * field invent nahi ki gayi hai. Jab tak C7 define na ho,
 * favourites hamesha YouTube-search raaste se bajenge.
 * On-device encrypted (MemoryCrypto), server par kabhi nahi.
 */
data class FavSong(
    val id: String,
    val title: String,
    val addedMs: Long,
    val lastPlayedMs: Long,
)

object FavSongStore {

    private val key = stringPreferencesKey("fav_songs_json")

    private fun toJson(songs: List<FavSong>): String {
        val arr = JSONArray()
        songs.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("title", s.title)
                    .put("added", s.addedMs)
                    .put("lastPlayed", s.lastPlayedMs),
            )
        }
        return arr.toString()
    }

    private fun fromJson(raw: String): List<FavSong> {
        val out = mutableListOf<FavSong>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                out.add(
                    FavSong(
                        id = o.optString("id", ""),
                        title = o.optString("title", ""),
                        addedMs = o.optLong("added", 0L),
                        lastPlayedMs = o.optLong("lastPlayed", 0L),
                    ),
                )
            }
        } catch (e: Exception) {
            CatcherHooks.dbError("fav-song", "fromJson", "exception")
        }
        return out.filter { it.id.isNotEmpty() && it.title.isNotEmpty() }
    }

    suspend fun list(context: Context): List<FavSong> {
        val raw = MemoryCrypto.decodeStored(
            context.favSongDataStore.data.map { it[key] ?: "[]" }.first(),
        )
        return fromJson(raw).sortedByDescending { it.addedMs }
    }

    suspend fun add(context: Context, title: String): FavSong? {
        val clean = title.trim()
        if (clean.isEmpty()) return null
        val existing = list(context)
        if (existing.any { it.title.equals(clean, ignoreCase = true) }) return null
        val song = FavSong(
            id = System.currentTimeMillis().toString() + "-" + (0..9999).random(),
            title = clean,
            addedMs = System.currentTimeMillis(),
            lastPlayedMs = 0L,
        )
        val updated = (existing + song).takeLast(200)
        val enc = MemoryCrypto.encodeStored(toJson(updated)) ?: return null
        context.favSongDataStore.edit { it[key] = enc }
        return song
    }

    suspend fun remove(context: Context, id: String): Boolean {
        val current = list(context)
        val updated = current.filter { it.id != id }
        if (updated.size == current.size) return false
        val enc = MemoryCrypto.encodeStored(toJson(updated)) ?: return false
        context.favSongDataStore.edit { it[key] = enc }
        return true
    }

    suspend fun markPlayed(context: Context, id: String) {
        val updated = list(context).map {
            if (it.id == id) it.copy(lastPlayedMs = System.currentTimeMillis()) else it
        }
        val enc = MemoryCrypto.encodeStored(toJson(updated)) ?: return
        context.favSongDataStore.edit { it[key] = enc }
    }
}
