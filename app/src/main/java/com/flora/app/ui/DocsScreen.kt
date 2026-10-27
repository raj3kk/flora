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
import com.flora.app.data.DocsStore
import kotlinx.coroutines.launch

/** Documents: on-device title + body notes, add / edit / delete. */
@Composable
fun DocsScreen(store: DocsStore) {
    val docs by store.docs.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var newTitle by remember { mutableStateOf("") }
    var newBody by remember { mutableStateOf("") }
    var editIndex by remember { mutableStateOf(-1) }
    var editTitle by remember { mutableStateOf("") }
    var editBody by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Documents — sirf is phone par save hote hain, kahin share nahi hote.")
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = newTitle, onValueChange = { newTitle = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = newBody, onValueChange = { newBody = it },
            label = { Text("Likhaai") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            if (newTitle.isNotBlank()) {
                scope.launch { store.add(newTitle, newBody) }
                newTitle = ""
                newBody = ""
            }
        }) { Text("Document save karo") }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            itemsIndexed(docs) { index, doc ->
                if (editIndex == index) {
                    OutlinedTextField(
                        value = editTitle, onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editBody, onValueChange = { editBody = it },
                        label = { Text("Likhaai") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row {
                        Button(onClick = {
                            scope.launch { store.update(index, editTitle, editBody) }
                            editIndex = -1
                        }) { Text("Save") }
                        Spacer(modifier = Modifier.padding(4.dp))
                        Button(onClick = { editIndex = -1 }) { Text("Cancel") }
                    }
                } else {
                    Text(doc.title)
                    Text(doc.body)
                    Row {
                        Button(onClick = {
                            editIndex = index
                            editTitle = doc.title
                            editBody = doc.body
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
