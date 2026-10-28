package com.swasthai.app.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.swasthai.app.pdf.ExtractedPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class PdfOcrResult(
    val pages: List<ExtractedPage>,
    val pageCount: Int,
    val failedPageNumbers: List<Int> = emptyList()
)

/**
 * High-speed 100% on-device OCR for medical reports, prescriptions, and lab tests
 * using Google ML Kit Vision.
 */
object ImageTextExtractor {
    private const val TAG = "ImageTextExtractor"

    suspend fun extractText(context: Context, imageUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            val visionText = Tasks.await(recognizer.process(image))
            val text = visionText.text.trim()
            Log.i(TAG, "Successfully extracted ${text.length} chars of text from image $imageUri")
            text
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to extract OCR text from image $imageUri: ${e.message}", e)
            ""
        }
    }

    suspend fun extractMissingPdfPages(
        file: File,
        existingPageNumbers: Set<Int>
    ): PdfOcrResult = withContext(Dispatchers.IO) {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)
        val recognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        try {
            val extracted = mutableListOf<ExtractedPage>()
            val failedPages = mutableListOf<Int>()
            for (index in 0 until renderer.pageCount) {
                val pageNumber = index + 1
                if (pageNumber in existingPageNumbers) continue
                val page = renderer.openPage(index)
                try {
                    val scale = minOf(2.5f, 1800f / maxOf(page.width, page.height))
                    val bitmap = Bitmap.createBitmap(
                        (page.width * scale).toInt().coerceAtLeast(1),
                        (page.height * scale).toInt().coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    try {
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val text = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0))).text.trim()
                        if (text.isNotBlank()) extracted += ExtractedPage(pageNumber, text)
                    } finally {
                        bitmap.recycle()
                    }
                } catch (error: Exception) {
                    failedPages += pageNumber
                    Log.w(TAG, "OCR failed on PDF page $pageNumber", error)
                } finally {
                    page.close()
                }
            }
            PdfOcrResult(extracted, renderer.pageCount, failedPages)
        } finally {
            recognizer.close()
            renderer.close()
            descriptor.close()
        }
    }
}
