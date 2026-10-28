package com.flora.app.ui

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.flora.app.assistant.VisionClient
import com.flora.app.assistant.VoiceEngine
import com.flora.app.data.FloraPreferences
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Camera dekhna (v2.22, user order 2026-10-28: "camera me kya
 * dikh raha hai") — ScanScreen wala CameraX pattern, par photo
 * save karne ke baad Gemini vision se poochha jata hai ki camera
 * me kya dikh raha hai. Photo Pictures/Flora me save hoti hai
 * (ScanScreen jaisa hi), vision ke liye sirf uske JPEG bytes
 * istemal hote hain; photo kabhi server par upload nahi hoti,
 * sirf user ki apni key se vision model ko frame milta hai.
 * Camera permission user khud Allow karega tabhi preview khulega.
 */
@Composable
fun CameraLookScreen(voice: VoiceEngine, prefs: FloraPreferences) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val vision = remember { VisionClient() }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var bound by remember { mutableStateOf(false) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var note by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        granted = isGranted
        bound = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!granted) {
            Text("Camera dekhne ke liye camera permission chahiye. Aap khud Allow karenge tabhi camera khulega.")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Text("Camera permission do")
            }
        } else {
            AndroidView(
                factory = { ctx -> PreviewView(ctx) },
                update = { view ->
                    if (!bound) {
                        val providerFuture = ProcessCameraProvider.getInstance(context)
                        providerFuture.addListener({
                            try {
                                val provider = providerFuture.get()
                                val preview = Preview.Builder().build()
                                preview.setSurfaceProvider(view.surfaceProvider)
                                val imageCapture = ImageCapture.Builder().build()
                                capture = imageCapture
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                                bound = true
                            } catch (e: Exception) {
                                // Camera bind failed: preview stays blank, no crash.
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val imageCapture = capture
                if (imageCapture == null || busy) {
                    note = "Camera abhi taiyaar nahi hai, ek second ruk kar dobara dabaiye."
                    return@Button
                }
                busy = true
                answer = ""
                note = "Photo le rahi hun, phir dekh kar bataungi..."
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "Flora_look_$stamp.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Flora")
                    }
                }
                val options = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                ).build()
                imageCapture.takePicture(
                    options,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val uri = output.savedUri
                            if (uri == null) {
                                busy = false
                                note = "Photo save nahi hui, dobara koshish kijiye."
                                return
                            }
                            scope.launch {
                                try {
                                    val stream = context.contentResolver.openInputStream(uri)
                                    val bitmap = BitmapFactory.decodeStream(stream)
                                    stream?.close()
                                    if (bitmap == null) {
                                        busy = false
                                        note = "Photo padhi nahi gayi, dobara koshish kijiye."
                                        return@launch
                                    }
                                    val out = ByteArrayOutputStream()
                                    bitmap.compress(
                                        android.graphics.Bitmap.CompressFormat.JPEG,
                                        80,
                                        out
                                    )
                                    val jpeg = out.toByteArray()
                                    val key = prefs.geminiApiKey()
                                    if (key.isEmpty()) {
                                        busy = false
                                        note = "Camera dekhne ke liye Settings me apni free Gemini key daaliye."
                                        voice.speak("Camera dekhne ke liye Gemini key chahiye.")
                                        return@launch
                                    }
                                    note = "Dekh rahi hun..."
                                    val reply = vision.askAboutImage(
                                        "Is camera me kya dikh raha hai, detail me batao.",
                                        jpeg,
                                        key
                                    )
                                    busy = false
                                    if (reply == null) {
                                        note = "Is baar dekh nahi payi — dobara dabaiye."
                                        voice.speak("Is baar dekh nahi payi.")
                                    } else {
                                        answer = reply
                                        note = "Dekh liya."
                                        voice.speak(reply.take(500))
                                    }
                                } catch (e: Exception) {
                                    busy = false
                                    note = "Photo dekh nahi payi, dobara koshish kijiye."
                                }
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            busy = false
                            note = "Photo save nahi hui, dobara koshish kijiye."
                            voice.speak("Photo save nahi hui.")
                        }
                    }
                )
            }) { Text("Camera me kya dikh raha hai?") }
            if (answer.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(answer)
            }
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
        }
    }
}
