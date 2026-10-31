package com.flora.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.flora.app.assistant.BrainClient
import com.flora.app.data.CatcherHooks
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.launch

/**
 * Provider keys screen (parity batch 2026-10-31 — brain
 * inventory §I missing lanes). 9 providers: OpenAI, Anthropic,
 * Mistral, Together, xAI, DeepSeek, DeepInfra, NVIDIA, OpenRouter.
 * Keys KeyStore-encrypted save hoti hain (FloraPreferences
 * readSecret/writeSecret). Key values screen par kabhi plain
 * nahi dikhti (password-masked fields) aur check result me
 * sirf saved/valid status aata hai, value kabhi nahi.
 * Chain order: Gemini → Groq → ye lanes → BasicTalk.
 * Notion aur ElevenLabs is screen me NAHI hain — wo gated
 * hain (user token/decision), force nahi kiye gaye.
 */
@Composable
fun ProviderKeysScreen(prefs: FloraPreferences, brain: BrainClient) {
    val scope = rememberCoroutineScope()
    var openai by remember { mutableStateOf("") }
    var anthropic by remember { mutableStateOf("") }
    var mistral by remember { mutableStateOf("") }
    var together by remember { mutableStateOf("") }
    var xai by remember { mutableStateOf("") }
    var deepseek by remember { mutableStateOf("") }
    var deepinfra by remember { mutableStateOf("") }
    var nvidia by remember { mutableStateOf("") }
    var openrouter by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<BrainClient.LaneCheck>>(emptyList()) }
    var note by remember { mutableStateOf("") }

    val providerNames = setOf(
        "OpenAI", "Anthropic", "Mistral", "Together", "xAI",
        "DeepSeek", "DeepInfra", "NVIDIA", "OpenRouter"
    )

    LaunchedEffect(Unit) {
        openai = prefs.openAiApiKey()
        anthropic = prefs.anthropicApiKey()
        mistral = prefs.mistralApiKey()
        together = prefs.togetherApiKey()
        xai = prefs.xaiApiKey()
        deepseek = prefs.deepseekApiKey()
        deepinfra = prefs.deepinfraApiKey()
        nvidia = prefs.nvidiaApiKey()
        openrouter = prefs.openrouterApiKey()
    }

    fun saveAll() {
        scope.launch {
            prefs.setOpenAiApiKey(openai)
            prefs.setAnthropicApiKey(anthropic)
            prefs.setMistralApiKey(mistral)
            prefs.setTogetherApiKey(together)
            prefs.setXaiApiKey(xai)
            prefs.setDeepseekApiKey(deepseek)
            prefs.setDeepinfraApiKey(deepinfra)
            prefs.setNvidiaApiKey(nvidia)
            prefs.setOpenrouterApiKey(openrouter)
            note = "Provider keys save ho gayi hain."
        }
    }

    fun checkSaved() {
        scope.launch {
            val checked = brain.checkKeys().filter { it.name in providerNames }
            results = checked
            // P3-01: invalid key check sirf count-signal ke saath —
            // key value ya note text Catcher me kabhi nahi jata.
            if (checked.any { it.saved && !it.valid }) {
                CatcherHooks.featureError("keys", "checkSaved", "invalid")
            }
            note = ""
        }
    }

    fun checkTyped() {
        scope.launch {
            val checked = brain.checkKeysFor(
                gemini = "", gemini2 = "", groq = "", groq2 = "", deepgram = "",
                openai = openai, anthropic = anthropic, mistral = mistral,
                together = together, xai = xai, deepseek = deepseek,
                deepinfra = deepinfra, nvidia = nvidia, openrouter = openrouter,
            ).filter { it.name in providerNames }
            results = checked
            if (checked.any { it.saved && !it.valid }) {
                CatcherHooks.featureError("keys", "checkTyped", "invalid")
            }
            note = ""
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Provider keys — khali lane band rehta hai, koi key dena zaroori nahi.")
            Text("Chain: Gemini → Groq → ye lanes → BasicTalk. Check sirf /models GET karta hai, koi token kharch nahi hota.")
            Spacer(modifier = Modifier.height(8.dp))
        }
        val fields = listOf(
            "OpenAI" to { v: String -> openai = v },
            "Anthropic" to { v: String -> anthropic = v },
            "Mistral" to { v: String -> mistral = v },
            "Together" to { v: String -> together = v },
            "xAI" to { v: String -> xai = v },
            "DeepSeek" to { v: String -> deepseek = v },
            "DeepInfra" to { v: String -> deepinfra = v },
            "NVIDIA" to { v: String -> nvidia = v },
            "OpenRouter" to { v: String -> openrouter = v },
        )
        val values = listOf(openai, anthropic, mistral, together, xai, deepseek, deepinfra, nvidia, openrouter)
        for (i in fields.indices) {
            item {
                OutlinedTextField(
                    value = values[i], onValueChange = fields[i].second,
                    label = { Text(fields[i].first + " key") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        item {
            Button(onClick = { saveAll() }) { Text("Sab keys save karo") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { checkSaved() }) { Text("Saved keys check karo") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { checkTyped() }) { Text("Bhare hue keys check karo") }
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        for (result in results) {
            item {
                Text(
                    result.name + ": " +
                        if (!result.saved) "saved nahi hai"
                        else if (result.valid) "sahi hai, kaam karegi"
                        else result.note
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
