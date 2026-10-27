package com.flora.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flora.app.data.MemoriesStore
import com.flora.app.tools.SearchTools
import kotlinx.coroutines.launch

/** Yaadein: on-device list, add / edit / delete. No server involved. */
@Composable
fun MemoriesScreen(store: MemoriesStore) {
    val items by store.items.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var newText by remember { mutableStateOf("") }
    var editIndex by remember { mutableStateOf(-1) }
    var editText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Yaadein — sirf is phone par save hoti hain, kahin nahi bheji jaati.")
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = newText, onValueChange = { newText = it },
            label = { Text("Nayi yaad likhiye") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val text = newText.trim()
            if (text.isNotEmpty()) {
                scope.launch { store.add(text) }
                newText = ""
            }
        }) { Text("Yaad save karo") }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            itemsIndexed(items) { index, item ->
                if (editIndex == index) {
                    OutlinedTextField(
                        value = editText, onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row {
                        Button(onClick = {
                            scope.launch { store.update(index, editText.trim()) }
                            editIndex = -1
                        }) { Text("Save") }
                        Spacer(modifier = Modifier.padding(4.dp))
                        Button(onClick = { editIndex = -1 }) { Text("Cancel") }
                    }
                } else {
                    Text(item)
                    Row {
                        Button(onClick = {
                            editIndex = index
                            editText = item
                        }) { Text("Edit") }
                        Spacer(modifier = Modifier.padding(4.dp))
                        Button(onClick = {
                            scope.launch { store.remove(index) }
                        }) { Text("Delete") }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/** Tools: no-key search + weather. Honest errors, never invented data. */
@Composable
fun ToolsScreen(tools: SearchTools) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var weatherResult by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Search (DuckDuckGo, bina key)")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                label = { Text("Kya dhoondhna hai?") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (query.isNotBlank() && !busy) {
                        busy = true
                        scope.launch {
                            searchResult = tools.search(query.trim())
                            busy = false
                        }
                    }
                },
                enabled = !busy
            ) { Text("Search karo") }
            if (searchResult.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(searchResult)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Mausam (Open-Meteo, bina key)")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = city, onValueChange = { city = it },
                label = { Text("Sheher ka naam") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (city.isNotBlank() && !busy) {
                        busy = true
                        scope.launch {
                            weatherResult = tools.weather(city.trim())
                            busy = false
                        }
                    }
                },
                enabled = !busy
            ) { Text("Mausam dekho") }
            if (weatherResult.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(weatherResult)
            }
        }
    }
}
