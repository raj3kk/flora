package com.flora.app.automation

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.delay

/**
 * Phase 6B — Image-gen via external app + bring-back.
 *
 * Flora khud image generate NAHI karti. Wo Gemini/ChatGPT app
 * verified kholti hai → tree se prompt box dhoondhti hai →
 * user ke shabdon se prompt type karti hai → generate/send
 * dabati hai → intezar karti hai → image wapas laati hai
 * (accessibility screenshot → Gallery ke Flora folder me
 * save) → user ko batati hai. ChatGPT wala raasta v2.16 se
 * pehle se hai (runChatGptImage); ye file Gemini wala raasta
 * + dono ke liye gallery-save helper deti hai.
 *
 * Constraints (kabhi nahi tootenge): Gemini/ChatGPT app
 * installed honi chahiye (na ho to Play Store khulega, install
 * user khud karega); LOGIN user khud EK BAAR khud karega —
 * password Flora kabhi nahi bolegi, nahi mangegi, nahi save
 * karegi. Screenshot na mile to image app me hi dikhegi —
 * is baare me jhooth nahi bola jayega.
 */
sealed class ImageOutcome {
    data class Saved(val appName: String) : ImageOutcome()
    data class AppOnly(val appName: String) : ImageOutcome()
    data class Failed(val reason: String) : ImageOutcome()
}

object ImageGenFlow {

    fun saveJpegToGallery(context: Context, jpeg: ByteArray?): Boolean {
        if (jpeg == null || jpeg.isEmpty()) return false
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "flora-image-" + System.currentTimeMillis() + ".jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Flora")
                }
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { it.write(jpeg) } ?: return false
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun generateWithApp(
        context: Context,
        service: FloraAutomationService,
        appPkg: String,
        appName: String,
        promptBoxLabels: List<String>,
        sendLabels: List<String>,
        prompt: String,
        onProgress: (String) -> Unit,
    ): ImageOutcome {
        val opened = AppAutomation.launch(context, appPkg)
        if (opened != VerifiedAppLauncher.OpenResult.Opened &&
            opened != VerifiedAppLauncher.OpenResult.AlreadyOpen
        ) {
            val reason = when (opened) {
                VerifiedAppLauncher.OpenResult.NoVerifier ->
                    "app kholna verify nahi kar paya — Accessibility service ON karni hogi"
                VerifiedAppLauncher.OpenResult.NoLaunchIntent ->
                    "app '$appName' installed nahi mili"
                else -> "app '$appName' khul nahi payi"
            }
            return ImageOutcome.Failed(reason)
        }
        delay(3000)
        var typed = service.setTextOnFirstEditable(prompt)
        if (!typed) {
            service.clickAnyText(*promptBoxLabels.toTypedArray())
            delay(800)
            typed = service.setTextOnFirstEditable(prompt)
        }
        if (!typed) {
            return ImageOutcome.Failed(
                "prompt box nahi mila — shayad login chahiye; login tum khud ek baar kar lena"
            )
        }
        delay(500)
        if (!service.clickAnyText(*sendLabels.toTypedArray())) {
            return ImageOutcome.Failed(
                "prompt likh diya hai, par Send button nahi mila — wahan Send dabate hi image banegi"
            )
        }
        onProgress("Image ban rahi hai, thoda waqt lagega.")
        delay(20000)
        val jpeg = try {
            service.screenshotJpeg()
        } catch (e: Exception) {
            null
        }
        val saved = saveJpegToGallery(context, jpeg)
        return if (saved) ImageOutcome.Saved(appName) else ImageOutcome.AppOnly(appName)
    }
}
