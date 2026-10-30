package com.flora.app.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flora.app.data.DocsStore
import com.flora.app.data.FloraDoc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Documents: on-device title + body notes, add / edit / delete.
 *  Parity batch 2026-10-31, M8: user-tap export — CSV (Excel me
 *  khulta hai) + PDF, Downloads/Flora me save. Koi auto-share,
 *  koi server upload kabhi nahi; export sirf user ke tap par. */
@Composable
fun DocsScreen(store: DocsStore) {
    val context = LocalContext.current
    val docs by store.docs.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var newTitle by remember { mutableStateOf("") }
    var newBody by remember { mutableStateOf("") }
    var editIndex by remember { mutableStateOf(-1) }
    var editTitle by remember { mutableStateOf("") }
    var editBody by remember { mutableStateOf("") }
    var exportNote by remember { mutableStateOf("") }

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
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(onClick = {
                scope.launch {
                    exportNote = withContext(Dispatchers.IO) { exportDocsCsv(context, docs) }
                }
            }) { Text("CSV export karo") }
            Spacer(modifier = Modifier.padding(4.dp))
            Button(onClick = {
                scope.launch {
                    exportNote = withContext(Dispatchers.IO) { exportDocsPdf(context, docs) }
                }
            }) { Text("PDF export karo") }
        }
        if (exportNote.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(exportNote)
        }
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

/** M8 export helpers (user-tap only, Downloads/Flora me save). */

private fun csvEscape(value: String): String {
    return if (value.contains(',') || value.contains('\n') || value.contains('"')) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }
}

private fun saveToDownloads(context: Context, fileName: String, mime: String, bytes: ByteArray): String? {
    return try {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Flora")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return null
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        "Downloads/Flora/$fileName"
    } catch (e: Exception) {
        null
    }
}

private fun exportDocsCsv(context: Context, docs: List<FloraDoc>): String {
    if (docs.isEmpty()) return "Koi document nahi hai, export karne ko kuch nahi."
    val out = StringBuilder("Title,Body\n")
    for (doc in docs) {
        out.append(csvEscape(doc.title)).append(",").append(csvEscape(doc.body)).append("\n")
    }
    val path = saveToDownloads(
        context,
        "flora-docs-" + System.currentTimeMillis() + ".csv",
        "text/csv",
        out.toString().toByteArray(Charsets.UTF_8)
    )
    return if (path != null) "CSV save ho gaya: $path (Excel me khulta hai)."
    else "CSV save nahi ho paya, dobara try karo."
}

private fun wrapText(text: String, width: Int): List<String> {
    val lines = mutableListOf<String>()
    for (raw in text.split("\n")) {
        var rest = raw
        if (rest.isEmpty()) {
            lines.add("")
            continue
        }
        while (rest.length > width) {
            val cut = rest.substring(0, width).lastIndexOf(' ').takeIf { it > 0 } ?: width
            lines.add(rest.substring(0, cut))
            rest = rest.substring(cut).trimStart()
        }
        lines.add(rest)
    }
    return lines
}

private fun exportDocsPdf(context: Context, docs: List<FloraDoc>): String {
    if (docs.isEmpty()) return "Koi document nahi hai, export karne ko kuch nahi."
    return try {
        val pdf = PdfDocument()
        val titlePaint = Paint().apply { textSize = 16f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 12f }
        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        var y = 40f
        fun newPage() {
            pdf.finishPage(page)
            pageNumber++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            y = 40f
        }
        for (doc in docs) {
            if (y > 760f) newPage()
            page.canvas.drawText(doc.title.ifEmpty { "(bina title)" }, 40f, y, titlePaint)
            y += 24f
            for (line in wrapText(doc.body, 85)) {
                if (y > 800f) newPage()
                page.canvas.drawText(line, 40f, y, bodyPaint)
                y += 16f
            }
            y += 16f
        }
        pdf.finishPage(page)
        val stream = java.io.ByteArrayOutputStream()
        pdf.writeTo(stream)
        pdf.close()
        val path = saveToDownloads(
            context,
            "flora-docs-" + System.currentTimeMillis() + ".pdf",
            "application/pdf",
            stream.toByteArray()
        )
        if (path != null) "PDF save ho gaya: $path." else "PDF save nahi ho paya, dobara try karo."
    } catch (e: Exception) {
        "PDF banane me fail hua, dobara try karo."
    }
}
