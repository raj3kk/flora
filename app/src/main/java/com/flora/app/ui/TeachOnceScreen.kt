package com.flora.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.flora.app.data.AutoLogStore
import com.flora.app.data.MacroStore
import com.flora.app.data.StoredMacro
import com.flora.app.planner.LearnedEngine
import com.flora.app.planner.LearnedPattern
import kotlinx.coroutines.launch

/**
 * Item 36 §2 — Teach-once core (surface, Lane A app part).
 *
 * "Silent" ka matlab yahan NO-REPEATED-EFFORT hai, chhup kar
 * karna kabhi nahi: har execution Auto Log me hogi, aur
 * "auto mode band" kehte hi sab rukega (Auto Mode ka visible
 * stop barkaraar hai).
 *
 * Sikhana = Routines screen par macro steps save karo (macro
 * JSON), ya planner ke learned patterns — ye screen un
 * sikhe hue kaamon ko trigger karti hai, dobara sikhana nahi
 * padta. Execute planner/macro primitives par hota hai
 * (SD-5 general automation ka intezar nahi karta); har
 * execution ke baad verify → report hoti hai (macro outcome
 * chat me + Auto Log me).
 *
 * 4 stops har taught skill me built-in hain, bypass kabhi
 * nahi: money, delete/irreversible, login/2FA, genuine block.
 * Koi skill in stops ko override nahi kar sakti — MacroEngine
 * ka password-field guard + sensitive-confirm isi ka hissa
 * hai. Teesre vyakti ko Flora ki taraf se silent reply kabhi
 * nahi — outbound-to-others ke liye Phase 6 ask-first hi
 * approved design hai.
 */
@Composable
fun TeachOnceScreen(
    autoLogStore: AutoLogStore,
    onRunMacro: (StoredMacro) -> Unit,
    onBack: () -> Unit,
) {
    val appContext = LocalContext.current
    val learnedEngine = remember { LearnedEngine(appContext) }
    val scope = rememberCoroutineScope()

    var note by remember { mutableStateOf("") }
    var macros by remember { mutableStateOf(listOf<StoredMacro>()) }
    var patterns by remember { mutableStateOf(listOf<LearnedPattern>()) }

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
                macros = MacroStore.load(appContext)
            } catch (e: Exception) {
                macros = emptyList()
            }
            try {
                patterns = learnedEngine.loadAll()
            } catch (e: Exception) {
                patterns = emptyList()
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Row {
            TextButton(onClick = onBack) { Text("Wapas") }
        }
        Text("Teach-once — ek baar sikhao, phir Flora khud kare")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Sikhana: Routines screen par macro steps save karo — phir yahan se Chalao dabao, " +
                "dobara steps batane nahi padenge. Har execution Auto Log me hogi; " +
                "\"auto mode band\" kehte hi sab rukega.",
        )
        if (note.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(note)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Sikhe hue macros (${macros.size}):")
        macros.forEach { macro ->
            Row {
                Text(macro.name, modifier = Modifier.weight(1f))
                Button(onClick = {
                    log("Teach-once: macro chalaya: ${macro.name.take(60)}")
                    note = "Macro '${macro.name}' chala rahi hun — har step bol kar bataungi, result chat me aayega."
                    onRunMacro(macro)
                }) { Text("Chalao") }
            }
        }
        if (macros.isEmpty()) {
            Text("Abhi koi macro save nahi hai — pehle Routines screen par macro banao.")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Learned skills (${patterns.size}) — skill engine = learned patterns, LLM retraining ka daava kabhi nahi:")
        patterns.forEach { pattern ->
            Text("- ${pattern.normalizedCommand.take(60)} • ${pattern.tool}")
        }
        if (patterns.isEmpty()) Text("Abhi koi learned pattern nahi hai.")
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "4 stops (bypass kabhi nahi): money par ruko + explicit approval; delete/irreversible par ruko; " +
                "login/2FA par ruko (vault se ek baar imaandaari se dobara maangna); genuine block par ruko + imaandaar report. " +
                "Verify fail ho to skill pause + report — chupchaap galat execute kabhi nahi.",
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
