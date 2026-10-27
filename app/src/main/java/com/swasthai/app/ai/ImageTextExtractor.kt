package com.swasthai.app.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-speed 100% on-device OCR for medical reports, prescriptions, and lab tests
 * using Google ML Kit Vision.
 */
object ImageTextExtractor {
    private const val TAG = "ImageTextExtractor"

    suspend fun extractText(context: Context, imageUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val visionText = Tasks.await(recognizer.process(image))
            val text = visionText.text.trim()
            Log.i(TAG, "Successfully extracted ${text.length} chars of text from image $imageUri")
            text
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to extract OCR text from image $imageUri: ${e.message}", e)
            ""
        }
    }
}
