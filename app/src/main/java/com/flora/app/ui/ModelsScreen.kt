package com.flora.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
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
import com.flora.app.assistant.VoiceEngine
import com.flora.app.data.FloraPreferences
import com.flora.app.models.FloraModel
import com.flora.app.models.ModelDownloader
import com.flora.app.models.ModelStatus
import kotlinx.coroutines.launch

/**
 * Models screen (v2.21, CEO plan M1): Flora ke models ka
 * status — kaunsa phone me maujood hai, kaunsa download
 * karna hai. Koi technical baat nahi, sirf pyar se status.
 */
@Composable
fun ModelsScreen(voice: VoiceEngine, prefs: FloraPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var models by remember { mutableStateOf<List<FloraModel>>(emptyList()) }
    var statuses by remember { mutableStateOf<Map<String, ModelStatus>>(emptyMap()) }
    var note by remember { mutableStateOf("") }

    fun refresh() {
        scope.launch {
            val list = ModelDownloader.fetchManifest(context, prefs)
            models = list
            statuses = list.associate { it.id to ModelDownloader.status(context, it) }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    fun statusText(model: FloraModel): String = when (statuses[model.id]) {
        ModelStatus.READY -> "Phone me maujood, kaam karega."
        ModelStatus.DOWNLOADING -> "Download ho raha hai, thoda ruk jao."
        ModelStatus.NOT_DOWNLOADED -> "Abhi phone me nahi hai."
        ModelStatus.SERVER_PENDING, null -> "Server par abhi nahi rakha gaya hai."
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Models — Flora ki samajh ka saamaan.")
            Text(
                "Bundled models phone me pehle se hain. Neural models " +
                    "(jaise nayi wake word) jab taiyaar honge, tab yahan " +
                    "se download honge — Maya ke models kabhi nahi."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { refresh() }) { Text("Refresh karo") }
        }
        items(models.size) { index ->
            val model = models[index]
            Spacer(modifier = Modifier.height(12.dp))
            Text("${model.name} (v${model.version})")
            Text(statusText(model))
            if (!model.bundled && statuses[model.id] == ModelStatus.NOT_DOWNLOADED) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = {
                    scope.launch {
                        statuses = statuses + (model.id to ModelStatus.DOWNLOADING)
                        val ok = ModelDownloader.download(context, model)
                        statuses = statuses + (model.id to ModelDownloader.status(context, model))
                        if (ok) {
                            note = "${model.name} download ho gaya, ab kaam karega."
                            voice.speak("${model.name} download ho gaya, ab kaam karega.")
                        } else {
                            note = "${model.name} download nahi ho paya, thodi der me phir koshish karo."
                            voice.speak("${model.name} download nahi ho paya, thodi der me phir koshish karo.")
                        }
                    }
                }) { Text("Download karo") }
            }
        }
        if (note.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(note)
            }
        }
    }
}
