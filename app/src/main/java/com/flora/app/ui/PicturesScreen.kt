package com.flora.app.ui
import com.flora.app.data.CatcherHooks

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * My pictures screen (parity batch 2026-10-31, M4 — Maya
 * `my_pictures`/`photos_of` ka Flora version, apne code se).
 * Sirf Pictures/Flora ki photos dikhti hain — wahi folder jahan
 * Scan, Whiteboard aur ImageGen save karte hain. Read-only
 * listing hai: tap karne par system viewer khulta hai (Flora
 * photo ko edit/delete/share khud nahi karti). Koi photo
 * server par nahi jati, koi upload nahi hota.
 */
data class FloraPhoto(val uri: Uri, val name: String, val dateAdded: Long)

@Composable
fun PicturesScreen() {
    val context = LocalContext.current
    // Watcher-tick fix 2026-10-31: loadPhotos suspend hai — onClick
    // (non-suspend) se seedha call compile break tha (CI #322/#323/
    // #324 fail, PicturesScreen.kt:140). Scope se launch karo.
    val scope = rememberCoroutineScope()
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var photos by remember { mutableStateOf<List<FloraPhoto>>(emptyList()) }
    var note by remember { mutableStateOf("") }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        granted = isGranted
        if (!isGranted) note = "Photos dekhne ke liye gallery permission chahiye — aap khud Allow karenge tabhi list dikhegi."
    }

    suspend fun loadPhotos() {
        withContext(Dispatchers.IO) {
            try {
                val out = mutableListOf<FloraPhoto>()
                val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED
                )
                val (selection, args) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?" to
                        arrayOf("Pictures/Flora%")
                } else {
                    MediaStore.Images.Media.DATA + " LIKE ?" to
                        arrayOf("%/Pictures/Flora/%")
                }
                context.contentResolver.query(
                    uri, projection, selection, args,
                    MediaStore.Images.Media.DATE_ADDED + " DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    var count = 0
                    while (cursor.moveToNext() && count < 200) {
                        val id = cursor.getLong(idCol)
                        out.add(
                            FloraPhoto(
                                uri = ContentUris.withAppendedId(uri, id),
                                name = cursor.getString(nameCol) ?: "",
                                dateAdded = cursor.getLong(dateCol) * 1000L
                            )
                        )
                        count++
                    }
                }
                photos = out
                note = if (out.isEmpty()) "Pictures/Flora me abhi koi photo nahi hai — Scan, Whiteboard ya ImageGen se save karo."
                else "${out.size} photos mili hain."
            } catch (e: Exception) {
                CatcherHooks.featureError("pictures", "loadPhotos", "exception")
                note = "Photos padhne me fail hua, dobara try karo."
            }
        }
    }

    LaunchedEffect(granted) {
        if (granted) loadPhotos()
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Meri photos — sirf Pictures/Flora (Flora ki save ki hui).")
            Text("Tap karne par system viewer khulega. Flora photo edit/delete/share khud nahi karti.")
            Spacer(modifier = Modifier.height(8.dp))
            if (!granted) {
                Button(onClick = { permissionLauncher.launch(permission) }) {
                    Text("Gallery permission do")
                }
            } else {
                Button(onClick = { scope.launch { loadPhotos() } }) { Text("Refresh karo") }
            }
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        items(photos) { photo ->
            val thumb by produceState<Bitmap?>(initialValue = null, photo.uri) {
                value = try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.loadThumbnail(photo.uri, Size(256, 256), null)
                    }
                } catch (e: Exception) {
                    CatcherHooks.featureError("pictures", "loadPhotos", "exception")
                    null
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(photo.uri, "image/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            CatcherHooks.featureError("pictures", "loadPhotos", "exception")
                            note = "Photo kholne me fail hua."
                        }
                    }
            ) {
                if (thumb != null) {
                    Image(
                        bitmap = thumb!!.asImageBitmap(),
                        contentDescription = photo.name,
                        modifier = Modifier.size(64.dp).padding(end = 8.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(photo.name)
                    Text(
                        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                            .format(Date(photo.dateAdded))
                    )
                }
            }
        }
    }
}
