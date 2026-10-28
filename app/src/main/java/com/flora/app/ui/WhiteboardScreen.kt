package com.flora.app.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Whiteboard (v2.22, P3-F16): ungli se draw karo — samjhane,
 * rough kaam ya bachon ke liye. Drawing sirf is phone par
 * banti hai; Save dabane par gallery (Pictures/Flora) me
 * jaati hai, kahin upload nahi hoti.
 */
@Composable
fun WhiteboardScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var paths by remember { mutableStateOf(listOf<Pair<Color, List<Offset>>>()) }
    var current by remember { mutableStateOf<Pair<Color, List<Offset>>?>(null) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var note by remember { mutableStateOf("") }

    fun toPath(points: List<Offset>): Path {
        val p = Path()
        if (points.isNotEmpty()) {
            p.moveTo(points[0].x, points[0].y)
            for (pt in points.drop(1)) p.lineTo(pt.x, pt.y)
        }
        return p
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
            Button(
                onClick = { if (paths.isNotEmpty()) paths = paths.dropLast(1) },
                modifier = Modifier.padding(end = 4.dp)
            ) { Text("Undo") }
            Button(
                onClick = { paths = emptyList(); current = null },
                modifier = Modifier.padding(end = 4.dp)
            ) { Text("Saaf karo") }
            Button(onClick = {
                val snapshot = paths
                val canvasSize = size
                if (snapshot.isEmpty() || canvasSize.width == 0) {
                    note = "Pehle kuch draw karo, phir Save dabao."
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
                            for ((color, pts) in snapshot) {
                                if (pts.size < 2) continue
                                val paint = android.graphics.Paint().apply {
                                    this.color = color.toArgb()
                                    strokeWidth = 8f
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
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White)
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            current = selectedColor to listOf(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val cur = current
                            if (cur != null) {
                                current = cur.first to (cur.second + change.position)
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
            for ((color, pts) in paths) {
                drawPath(
                    toPath(pts), color,
                    style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            val cur = current
            if (cur != null) {
                drawPath(
                    toPath(cur.second), cur.first,
                    style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
