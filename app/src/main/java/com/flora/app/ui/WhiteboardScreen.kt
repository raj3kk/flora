package com.flora.app.ui

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Whiteboard (v2.22, P3-F16; Batch 2 F5 voice + size):
 * ungli se draw karo — samjhane, rough kaam ya bachon ke
 * liye. Drawing sirf is phone par banti hai; Save dabane
 * par gallery (Pictures/Flora) me jaati hai, kahin upload
 * nahi hoti. Batch 2: maujooda system STT (RecognizerIntent,
 * mic permission pehle se step-2 me milti hai) se bola hua
 * text board par text ban kar aata hai, aur bola hua shape
 * ("gol banao", "line kheecho", "trikon", "chaukor") canvas
 * path ban kar aata hai. Drag-draw + rang + Undo/Saaf/Save
 * waisa hi hai; stroke width ab fixed 8f nahi — user badal
 * sakta hai (Patla/Normal/Mota/Bahut mota).
 */
@Composable
fun WhiteboardScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Path ab (rang, points, strokeWidth) — purani drag-draw
    // isi shape me rehti hai, sirf width bhi save hota hai.
    var paths by remember { mutableStateOf(listOf<Triple<Color, List<Offset>, Float>>()) }
    var current by remember { mutableStateOf<Triple<Color, List<Offset>, Float>?>(null) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(8f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var note by remember { mutableStateOf("") }
    var voiceText by remember { mutableStateOf("") }
    var texts by remember { mutableStateOf(listOf<Triple<String, Color, Offset>>()) }

    fun toPath(points: List<Offset>): Path {
        val p = Path()
        if (points.isNotEmpty()) {
            p.moveTo(points[0].x, points[0].y)
            for (pt in points.drop(1)) p.lineTo(pt.x, pt.y)
        }
        return p
    }

    // Shape helpers — launcher callback se PEHLE declare (order trap).
    fun shapePoints(kind: String): List<Offset>? {
        val w = size.width.toFloat()
        val h = size.height.toFloat()
        if (w <= 0f || h <= 0f) return null
        val cx = w / 2f
        val cy = h / 2f
        return when (kind) {
            "circle" -> {
                val r = min(w, h) * 0.3f
                (0..64).map { i ->
                    val a = 2 * PI.toFloat() * i / 64f
                    Offset(cx + r * cos(a), cy + r * sin(a))
                }
            }
            "line" -> listOf(Offset(w * 0.15f, cy), Offset(w * 0.85f, cy))
            "triangle" -> listOf(
                Offset(cx, h * 0.2f), Offset(w * 0.8f, h * 0.8f),
                Offset(w * 0.2f, h * 0.8f), Offset(cx, h * 0.2f)
            )
            "rectangle" -> listOf(
                Offset(w * 0.2f, h * 0.25f), Offset(w * 0.8f, h * 0.25f),
                Offset(w * 0.8f, h * 0.75f), Offset(w * 0.2f, h * 0.75f),
                Offset(w * 0.2f, h * 0.25f)
            )
            else -> null
        }
    }

    fun addVoiceText(text: String) {
        val y = 60f + texts.size * 56f
        texts = texts + Triple(text, selectedColor, Offset(40f, y))
        note = "Board par likh diya: $text"
    }

    // Bola hua text: shape-shabd ho to shape path, warna text.
    // Hindi + English dono shabd pehchane jate hain.
    fun handleVoiceResult(spoken: String) {
        val lower = spoken.lowercase()
        val kind = when {
            lower.contains("gol") || lower.contains("circle") -> "circle"
            lower.contains("trikon") || lower.contains("triangle") -> "triangle"
            lower.contains("chaukor") || lower.contains("rectangle") ||
                lower.contains("square") || lower.contains("aayat") -> "rectangle"
            lower.contains("line") || lower.contains("rekha") ||
                lower.contains("seedhi") -> "line"
            else -> null
        }
        if (kind != null) {
            val pts = shapePoints(kind)
            if (pts == null) {
                note = "Pehle board dikhne do, phir shape bolo."
            } else {
                paths = paths + Triple(selectedColor, pts, strokeWidth)
                note = "Shape bana diya: $spoken"
            }
        } else {
            addVoiceText(spoken)
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull() ?: ""
            if (spoken.isNotBlank()) {
                voiceText = spoken
                handleVoiceResult(spoken)
            } else {
                note = "Kuch sunai nahi diya — dobara bolo."
            }
        } else {
            note = "Voice sun nahi paya — dobara koshish karo."
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Whiteboard — ungli se draw karo. Save sirf gallery me jaata hai.")
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            val colors = listOf(
                "Kala" to Color.Black, "Laal" to Color.Red,
                "Neela" to Color.Blue, "Hara" to Color(0xFF2E7D32)
            )
            for ((name, c) in colors) {
                Button(
                    onClick = { selectedColor = c },
                    modifier = Modifier.padding(end = 4.dp)
                ) { Text(if (selectedColor == c) "✓ $name" else name) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            // Batch 2 (F5): size control — stroke width fixed 8f nahi raha.
            listOf(4f to "Patla", 8f to "Normal", 16f to "Mota", 24f to "Bahut mota").forEach { (w, label) ->
                Button(
                    onClick = { strokeWidth = w },
                    modifier = Modifier.padding(end = 4.dp)
                ) { Text(if (strokeWidth == w) "✓ $label" else label) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(
                onClick = {
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                            putExtra(
                                RecognizerIntent.EXTRA_PROMPT,
                                "Bolo — text ya shape, jaise 'gol banao'"
                            )
                        }
                        speechLauncher.launch(intent)
                    } catch (e: Exception) {
                        note = "Voice recognition is phone par nahi khul paya — text field me likh kar 'Board par likho' dabao."
                    }
                },
                modifier = Modifier.padding(end = 4.dp)
            ) { Text("🎤 Bolo aur likho") }
            Button(
                onClick = {
                    val t = voiceText.trim()
                    if (t.isEmpty()) {
                        note = "Pehle bolo ya text field me likho, phir ye dabao."
                    } else {
                        addVoiceText(t)
                    }
                },
                modifier = Modifier.padding(end = 4.dp)
            ) { Text("Board par likho") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = voiceText, onValueChange = { voiceText = it },
            label = { Text("Bola hua / likha hua text (shape-shabd bolne par shape banega)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(
                onClick = {
                    if (paths.isNotEmpty()) paths = paths.dropLast(1)
                    else if (texts.isNotEmpty()) texts = texts.dropLast(1)
                },
                modifier = Modifier.padding(end = 4.dp)
            ) { Text("Undo") }
            Button(
                onClick = { paths = emptyList(); texts = emptyList(); current = null },
                modifier = Modifier.padding(end = 4.dp)
            ) { Text("Saaf karo") }
            Button(onClick = {
                val snapshot = paths
                val snapshotTexts = texts
                val canvasSize = size
                if ((snapshot.isEmpty() && snapshotTexts.isEmpty()) || canvasSize.width == 0) {
                    note = "Pehle kuch draw karo ya likho, phir Save dabao."
                    return@Button
                }
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        try {
                            val bmp = Bitmap.createBitmap(
                                canvasSize.width, canvasSize.height,
                                Bitmap.Config.ARGB_8888
                            )
                            val canvas = android.graphics.Canvas(bmp)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            for ((color, pts, width) in snapshot) {
                                if (pts.size < 2) continue
                                val paint = android.graphics.Paint().apply {
                                    this.color = color.toArgb()
                                    strokeWidth = width
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeCap = android.graphics.Paint.Cap.ROUND
                                    strokeJoin = android.graphics.Paint.Join.ROUND
                                    isAntiAlias = true
                                }
                                val p = android.graphics.Path()
                                p.moveTo(pts[0].x, pts[0].y)
                                for (pt in pts.drop(1)) p.lineTo(pt.x, pt.y)
                                canvas.drawPath(p, paint)
                            }
                            // Batch 2 (F5): voice/text items bhi gallery image me aayein.
                            for ((text, color, pos) in snapshotTexts) {
                                val paint = android.graphics.Paint().apply {
                                    this.color = color.toArgb()
                                    textSize = 48f
                                    isAntiAlias = true
                                }
                                canvas.drawText(text, pos.x, pos.y, paint)
                            }
                            val values = ContentValues().apply {
                                put(
                                    MediaStore.Images.Media.DISPLAY_NAME,
                                    "flora-whiteboard-${System.currentTimeMillis()}.png"
                                )
                                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Flora")
                            }
                            val uri = context.contentResolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                            ) ?: return@withContext false
                            context.contentResolver.openOutputStream(uri)?.use {
                                bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
                            }
                            true
                        } catch (e: Exception) {
                            false
                        }
                    }
                    note = if (ok) "Gallery me save ho gaya (Pictures/Flora)."
                    else "Save nahi ho paya."
                }
            }) { Text("Save") }
        }
        if (note.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(note)
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Batch 2 (F5): Canvas + voice-text items ek Box me — text
        // items canvas coordinates par hi dikhte hain.
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .onSizeChanged { size = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                current = Triple(selectedColor, listOf(offset), strokeWidth)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val cur = current
                                if (cur != null) {
                                    current = Triple(cur.first, cur.second + change.position, cur.third)
                                }
                            },
                            onDragEnd = {
                                val cur = current
                                if (cur != null && cur.second.size >= 2) {
                                    paths = paths + cur
                                }
                                current = null
                            }
                        )
                    }
            ) {
                for ((color, pts, width) in paths) {
                    drawPath(
                        toPath(pts), color,
                        style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                val cur = current
                if (cur != null) {
                    drawPath(
                        toPath(cur.second), cur.first,
                        style = Stroke(width = cur.third, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
            for ((text, color, pos) in texts) {
                Text(
                    text, color = color,
                    modifier = Modifier.offset { IntOffset(pos.x.toInt(), pos.y.toInt()) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
