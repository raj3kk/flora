package com.flora.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

/**
 * Face screen (v0.4): front-camera preview + on-device ML Kit face
 * PRESENCE check only — it reports whether a face is in frame, and
 * how many. It does NOT identify whose face it is, does NOT match
 * against any stored face, and captures/saves/uploads NOTHING.
 * Identity-matching Face Lock is a separate later step (on-device
 * embedding model + user consent enrolment); this screen must never
 * be described as identity Face Lock. App unlock stays PIN-based.
 */
@Composable
fun FaceScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var bound by remember { mutableStateOf(false) }
    var faceCount by remember { mutableStateOf(-1) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        granted = isGranted
        bound = false
    }
    val detector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
        )
    }
    DisposableEffect(Unit) {
        onDispose { detector.close() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!granted) {
            Text("Face check ke liye camera permission chahiye. Aap khud Allow karenge tabhi camera khulega.")
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
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { proxy ->
                                    val mediaImage = proxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(
                                            mediaImage, proxy.imageInfo.rotationDegrees
                                        )
                                        detector.process(image)
                                            .addOnSuccessListener { faces ->
                                                faceCount = faces.size
                                            }
                                            .addOnFailureListener { faceCount = -1 }
                                            .addOnCompleteListener { proxy.close() }
                                    } else {
                                        proxy.close()
                                    }
                                }
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_FRONT_CAMERA,
                                    preview,
                                    analysis
                                )
                                bound = true
                            } catch (e: Exception) {
                                // Camera bind failed: preview stays blank, no crash.
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            Text(
                when {
                    faceCount < 0 -> "Face check chal raha hai…"
                    faceCount == 0 -> "Koi chehra frame me nahi hai."
                    else -> "Chehra mila: $faceCount (presence check only — pehchaan match nahi hota)"
                }
            )
            Text("Koi photo save, send ya match nahi hoti. Unlock PIN se hi hota hai.")
        }
    }
}
