package com.flora.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.flora.app.automation.VerifiedAppLauncher
import com.flora.app.data.AutoLogStore
import com.flora.app.data.FavSong
import com.flora.app.data.FavSongStore
import com.flora.app.tools.MusicControl
import com.flora.app.tools.SdResult
import kotlinx.coroutines.launch

/**
 * P3 favourite-song storage/selection (on-device only).
 *
 * - Add: naam type karke save, ya "abhi baj raha gaana"
 *   (8.8 MediaSession now-playing) se save.
 * - Select + Play: favourite par Bajao dabao → maujooda
 *   YouTube-search raasta khulta hai (koi naya player nahi).
 * - Remove: user ka tap + confirm (armed button).
 * - Control (play/pause/next) yahan NAHI hai — wo SD-8
 *   8.8 music control ka hai, duplicate nahi hua.
 * - Music-app pairing HELD hai (audit C7 NEEDS-DEFINITION):
 *   pairing define hone tak favourites hamesha YouTube
 *   search se bajenge; koi pairing invent nahi ki gayi.
 */
@Composable
fun FavSongsScreen(autoLogStore: AutoLogStore, onBack: () -> Unit) {
    val appContext = LocalContext.current
    val scope = rememberCoroutineScope()

    var note by remember { mutableStateOf("") }
    var songs by remember { mutableStateOf(listOf<FavSong>()) }
    var newTitle by remember { mutableStateOf("") }
    var armedRemoveId by remember { mutableStateOf("") }

    fun log(msg: String) {
        scope.launch {
            try {
                autoLogStore.add(msg)
            } catch (e: Exception) {
            }
        }
    }

    fun refresh() {
        scope.launch {
            try {
                songs = FavSongStore.list(appContext)
            } catch (e: Exception) {
                note = "Favourites padhne me dikkat aayi — dobara kholo."
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    fun playSong(song: FavSong) {
        scope.launch {
            try {
                VerifiedAppLauncher.openWebsiteVerified(
                    appContext,
                    "https://www.youtube.com/results?search_query=" + Uri.encode(song.title)
                )
                FavSongStore.markPlayed(appContext, song.id)
                log("P3: favourite bajaya: ${song.title.take(60)}")
                note = "'${song.title}' YouTube par khol raha hun. Play/pause ke liye SD-8 Settings > Music dekho."
                refresh()
            } catch (e: Exception) {
                note = "Gaana khul nahi paya — dobara koshish karo."
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Row {
            TextButton(onClick = onBack) { Text("Wapas") }
        }
        Text("Favourite gaane — sirf isi phone par")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Bajana hamesha maujooda YouTube-search raaste se hoga (koi naya player nahi). " +
                "Music-app pairing abhi define nahi hui hai (C7) — isliye koi app-pairing yahan invent nahi ki gayi.",
        )
        if (note.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(note)
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = newTitle, onValueChange = { newTitle = it },
            label = { Text("Gaane ka naam") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row {
            Button(onClick = {
                scope.launch {
                    val added = FavSongStore.add(appContext, newTitle)
                    if (added == null) {
                        note = if (newTitle.isBlank()) "Gaane ka naam khaali hai." else "Ye gaana pehle se favourite me hai."
                    } else {
                        newTitle = ""
                        log("P3: favourite save hua: ${added.title.take(60)}")
                        note = "Favourite me save ho gaya."
                        refresh()
                    }
                }
            }) { Text("Favourite me daalo") }
            TextButton(onClick = {
                scope.launch {
                    when (val r = MusicControl.nowPlaying(appContext)) {
                        is SdResult.Ok -> {
                            val np = r.value
                            if (np == null || np.title.isBlank()) {
                                note = "Abhi koi gaana session me nahi hai — pehle music app me gaana chalao."
                            } else {
                                val added = FavSongStore.add(appContext, np.title)
                                if (added == null) {
                                    note = "Ye gaana pehle se favourite me hai."
                                } else {
                                    log("P3: abhi baj raha gaana favourite hua: ${np.title.take(60)}")
                                    note = "Abhi baj raha gaana favourite me save ho gaya."
                                    refresh()
                                }
                            }
                        }
                        is SdResult.Failure -> note = r.reason
                    }
                }
            }) { Text("Abhi baj raha gaana favourite karo") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Favourites (${songs.size}):")
        songs.forEach { song ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(song.title)
            Row {
                Button(onClick = { playSong(song) }) { Text("Bajao") }
                TextButton(onClick = {
                    if (armedRemoveId != song.id) {
                        armedRemoveId = song.id
                        note = "Pakka '${song.title}' favourite se hatana hai? Confirm karne ke liye dobara dabao."
                    } else {
                        armedRemoveId = ""
                        scope.launch {
                            if (FavSongStore.remove(appContext, song.id)) {
                                log("P3: favourite hataya gaya: ${song.title.take(60)}")
                                note = "Favourite se hata diya gaya."
                                refresh()
                            } else {
                                note = "Hataya nahi ja saka — dobara koshish karo."
                            }
                        }
                    }
                }) { Text(if (armedRemoveId == song.id) "Pakka hatao" else "Hatao") }
            }
        }
        if (songs.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Abhi koi favourite nahi hai — upar se pehla gaana daalo.")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
