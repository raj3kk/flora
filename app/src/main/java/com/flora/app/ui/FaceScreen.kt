package com.flora.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.flora.app.core.FaceMatcher
import com.flora.app.security.BiometricTemplates
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Face screen: v0.4 presence check + v2.3 face-match enrolment.
 *
 * Presence check (v0.4): front-camera preview + on-device ML Kit
 * face PRESENCE only — kitne chehre frame me hain.
 *
 * Face-match (v2.3, photo se): user apni photo chunta hai,
 * ML Kit se face box nikalta hai, MobileFaceNet embedding
 * banta hai, template sirf encrypted on-device save hota hai
 * (BiometricTemplates, KeyStore AES/GCM). Koi photo, koi
 * embedding, koi template kabhi server/backup par nahi jata,
 * kabhi log nahi hota. Match test sirf similarity score
 * dikhata hai, embedding value kabhi nahi.
 *
 * HONEST RULE (kabhi nahi tootega): face-match AKELA kabhi
 * PIN unlock nahi karega. Unlock hamesha PIN se hota hai.
 * Face-match sirf pehchaan ka test hai, unlock ka raasta nahi.
 * Voice-match is push me NAHI hai — wo v2.4 me aayega
 * (mel-frontend prototype abhi baaki hai).
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
    var enrolStatus by remember { mutableStateOf("") }
    var matchResult by remember { mutableStateOf("") }
    var pickMode by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val faceMatcher = remember { FaceMatcher(context) }
    val templates = remember { BiometricTemplates(context) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        granted = isGranted
        bound = false
    }
    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mode = pickMode
        enrolStatus = "Photo process ho rahi hai…"
        matchResult = ""
        try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
            if (bitmap == null) {
                enrolStatus = "Photo padh nahi payi, doosri photo chuno."
                return@rememberLauncherForActivityResult
            }
            val image = InputImage.fromBitmap(bitmap, 0)
            val detector = FaceDetection.getClient(
                FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .build()
            )
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isEmpty()) {
                        enrolStatus = "Photo me koi chehra nahi mila."
                        detector.close()
                        return@addOnSuccessListener
                    }
                    val box = faces[0].boundingBox
                    val left = box.left.coerceIn(0, bitmap.width - 1)
                    val top = box.top.coerceIn(0, bitmap.height - 1)
                    val width = box.width().coerceIn(1, bitmap.width - left)
                    val height = box.height().coerceIn(1, bitmap.height - top)
                    val crop = Bitmap.createBitmap(bitmap, left, top, width, height)
                    detector.close()
                    scope.launch(Dispatchers.Default) {
                        val embedding = faceMatcher.embedding(crop)
                        withContext(Dispatchers.Main) {
                            if (embedding == null) {
                                enrolStatus = "Face model nahi mila ya embedding nahi bani."
                                return@withContext
                            }
                            if (mode == "enrol") {
                                if (templates.storeFace(embedding)) {
                                    enrolStatus = "Enrol ho gaya — template sirf is phone par encrypted save hua hai."
                                } else {
                                    enrolStatus = "Enrol save nahi hua, dobara try karo."
                                }
                            } else {
                                val enrolled = templates.loadFace()
                                if (enrolled == null) {
                                    matchResult = "Pehle enrol karo, phir match test karo."
                                } else {
                                    val score = faceMatcher.cosine(embedding, enrolled)
                                    val ok = faceMatcher.matches(embedding, enrolled)
                                    matchResult = if (ok) {
                                        "Match hua (score ${String.format("%.2f", score)})."
                                    } else {
                                        "Match nahi hua (score ${String.format("%.2f", score)})."
                                    }
                                    enrolStatus = ""
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    enrolStatus = "Chehra pehchanne me fail hua, doosri photo try karo."
                    detector.close()
                }
        } catch (e: Exception) {
            enrolStatus = "Photo process me fail hua, dobara try karo."
        }
    }
    val detector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            detector.close()
            faceMatcher.close()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Face-match enrolment (v2.3, photo se)")
        Text("Photo sirf is phone par use hogi — save, send ya server par nahi jayegi. Face-match akela PIN unlock nahi karega.")
        if (!faceMatcher.isReady()) {
            Text("Face model nahi mila — enrolment kaam nahi karega.")
        }
        Button(onClick = {
            pickMode = "enrol"
            photoLauncher.launch("image/*")
        }) { Text("Enrol photo chuno") }
        Button(onClick = {
            pickMode = "match"
            photoLauncher.launch("image/*")
        }) { Text("Match test photo chuno") }
        Button(onClick = {
            templates.clearAll()
            enrolStatus = "Templates delete ho gaye."
            matchResult = ""
        }) { Text("Templates delete karo") }
        if (enrolStatus.isNotEmpty()) Text(enrolStatus)
        if (matchResult.isNotEmpty()) Text(matchResult)
        Spacer(modifier = Modifier.height(8.dp))
        if (!granted) {
            Text("Face check ke liye camera permission chahiye. Aap khud Allow karenge tabhi camera khulega.")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { cameraLauncher.launch(Manifest.permission.CAMERA) }) {
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
                modifier = Modifier.weight(1f).fillMaxWidth()
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
