package com.flora.app.ui

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.flora.app.assistant.VoiceEngine
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Scan screen (v1.1 vision): photo capture + save ke saath ab
 * "dekh kar padhna" bhi — ML Kit Text Recognition SIRF phone par
 * chalti hai (Latin + Devanagari), photo kabhi server par nahi
 * jaati. Photo dikhake poocho to Flora usme likha text padh kar
 * bol degi — yehi wo "Maya dekhti thi" wala hissa tha jo v1.0
 * tak miss tha. Live camera se sawaal-jawaab (video samajhna)
 * abhi bhi agla step hai, is screen me uska daava nahi hai.
 *
 * Parity cycle 2026-10-31 (M1, Maya qr_scan route): QR / barcode
 * live scan bhi — ML Kit Barcode Scanning SIRF phone par chalti
 * hai, koi server call nahi, koi auto-open nahi. QR milte hi value
 * screen par ruk jaati hai (dobara scan tabhi jab user kahe);
 * URL ho to "Browser me kholo" sirf tab dikhta hai jab caller ne
 * onOpenUrl diya ho (FloraApp call site ab wired hai — M1
 * browser-open wiring: onOpenUrl QR URL ko BrowserNav.request
 * se BrowserScreen me kholta hai).
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun ScanScreen(voice: VoiceEngine, onOpenUrl: (String) -> Unit = {}) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboard = LocalClipboardManager.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var bound by remember { mutableStateOf(false) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var note by remember { mutableStateOf("") }
    var savedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var readText by remember { mutableStateOf("") }
    var qrMode by remember { mutableStateOf(false) }
    var qrValue by remember { mutableStateOf("") }
    var qrFrozen by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        granted = isGranted
        bound = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!granted) {
            Text("Scan ke liye camera permission chahiye. Aap khud Allow karenge tabhi camera khulega.")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Text("Camera permission do")
            }
        } else {
            Button(onClick = {
                qrMode = !qrMode
                bound = false
                capture = null
                qrValue = ""
                qrFrozen = false
                note = if (qrMode) "QR mode: camera ko QR / barcode par rakho." else "Photo mode par wapas."
            }) { Text(if (qrMode) "Photo mode par wapas jao" else "QR / Barcode scan karo") }
            Spacer(modifier = Modifier.height(8.dp))
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
                                provider.unbindAll()
                                if (qrMode) {
                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                    val scanner = BarcodeScanning.getClient()
                                    analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { proxy ->
                                        val media = proxy.image
                                        if (media == null || qrFrozen) {
                                            proxy.close()
                                            return@setAnalyzer
                                        }
                                        val image = InputImage.fromMediaImage(
                                            media,
                                            proxy.imageInfo.rotationDegrees
                                        )
                                        scanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                val first = barcodes.firstOrNull()?.rawValue
                                                if (!first.isNullOrBlank() && !qrFrozen) {
                                                    qrValue = first
                                                    qrFrozen = true
                                                    note = "QR / barcode mil gaya."
                                                    voice.speak("QR mil gaya.")
                                                }
                                            }
                                            .addOnCompleteListener { proxy.close() }
                                    }
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        analysis
                                    )
                                } else {
                                    val imageCapture = ImageCapture.Builder().build()
                                    capture = imageCapture
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageCapture
                                    )
                                }
                                bound = true
                            } catch (e: Exception) {
                                // Camera bind failed: preview stays blank, no crash.
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            if (qrMode) {
                Spacer(modifier = Modifier.height(8.dp))
                if (qrValue.isNotEmpty()) {
                    Text(qrValue)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        clipboard.setText(AnnotatedString(qrValue))
                        note = "Copy ho gaya."
                    }) { Text("Copy karo") }
                    val isUrl = qrValue.startsWith("http://") || qrValue.startsWith("https://")
                    if (isUrl) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { onOpenUrl(qrValue) }) { Text("Browser me kholo") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        qrValue = ""
                        qrFrozen = false
                        note = "Dobara scan karo — camera ko QR / barcode par rakho."
                    }) { Text("Dobara scan karo") }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Camera ko QR / barcode par rakho — milte hi yahan ruk jayega.")
                }
            }
            if (!qrMode) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val imageCapture = capture
                if (imageCapture == null) {
                    note = "Camera abhi taiyaar nahi hai, ek second ruk kar dobara dabaiye."
                    return@Button
                }
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "Flora_$stamp.jpg")
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
                            savedUri = output.savedUri
                            readText = ""
                            note = "Photo save ho gayi: Pictures/Flora me."
                            voice.speak("Photo save ho gayi.")
                        }

                        override fun onError(exception: ImageCaptureException) {
                            note = "Photo save nahi hui, dobara koshish kijiye."
                            voice.speak("Photo save nahi hui.")
                        }
                    }
                )
            }) { Text("Photo lo (capture + save)") }
            if (savedUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    val uri = savedUri ?: return@Button
                    note = "Photo padh rahi hun..."
                    try {
                        val image = InputImage.fromFilePath(context, uri)
                        val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        latin.process(image)
                            .addOnSuccessListener { visionText ->
                                if (visionText.text.isNotBlank()) {
                                    readText = visionText.text
                                    note = "Padh liya."
                                    voice.speak(visionText.text.take(500))
                                } else {
                                    // Devanagari (Hindi) try karo — Latin khaali ho to.
                                    val dev = TextRecognition.getClient(
                                        DevanagariTextRecognizerOptions.Builder().build()
                                    )
                                    dev.process(image)
                                        .addOnSuccessListener { devText ->
                                            if (devText.text.isNotBlank()) {
                                                readText = devText.text
                                                note = "Padh liya."
                                                voice.speak(devText.text.take(500))
                                            } else {
                                                note = "Is photo me koi padhne layak text nahi mila."
                                                voice.speak("Is photo me koi text nahi mila.")
                                            }
                                        }
                                        .addOnFailureListener {
                                            note = "Photo padhi nahi gayi, dobara koshish kijiye."
                                        }
                                }
                            }
                            .addOnFailureListener {
                                note = "Photo padhi nahi gayi, dobara koshish kijiye."
                            }
                    } catch (e: Exception) {
                        note = "Photo padhi nahi gayi, dobara koshish kijiye."
                    }
                }) { Text("Is photo me kya likha hai, padh kar batao") }
            }
            } // !qrMode: photo + OCR buttons sirf photo mode me
            if (readText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(readText)
            }
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
        }
    }
}
