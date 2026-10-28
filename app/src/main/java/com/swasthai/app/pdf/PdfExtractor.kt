package com.swasthai.app.pdf

import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

data class ExtractedPage(
    val pageNumber: Int,
    val text: String
)

data class ExtractionResult(
    val pages: List<ExtractedPage>,
    val reportDate: Long?,
    val isScannedOnly: Boolean,
    val failureReason: String? = null,
    val pageCount: Int? = null
)

object PdfExtractor {

    /** Extracts real PDF text per source page. Scanned/textless pages are left for local OCR. */
    fun extractTextFromPdf(file: File): ExtractionResult {
        if (!file.exists() || file.length() == 0L) {
            return ExtractionResult(emptyList(), null, false, "File does not exist or is empty (0 bytes).")
        }

        return try {
            val pages = mutableListOf<ExtractedPage>()
            var pageText = StringBuilder()
            var pageCount: Int? = null
            PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly()).use { document ->
                pageCount = document.numberOfPages
                val stripper = object : PDFTextStripper() {
                    override fun startPage(page: PDPage) {
                        super.startPage(page)
                        pageText = StringBuilder()
                    }

                    override fun writeString(text: String) {
                        pageText.append(text).append('\n')
                    }

                    override fun endPage(page: PDPage) {
                        val text = pageText.toString().trim()
                        if (text.isNotEmpty()) pages += ExtractedPage(getCurrentPageNo(), text)
                        super.endPage(page)
                    }
                }
                stripper.setSortByPosition(true)
                stripper.getText(document)
            }

            val text = pages.joinToString("\n") { it.text }
            ExtractionResult(
                pages = pages,
                reportDate = detectReportDate(text),
                isScannedOnly = pages.isEmpty(),
                failureReason = if (pages.isEmpty()) "No text layer found; local OCR is required." else null,
                pageCount = pageCount
            )
        } catch (error: Exception) {
            ExtractionResult(
                pages = emptyList(),
                reportDate = null,
                isScannedOnly = false,
                failureReason = "PDF text extraction failed: ${error.localizedMessage ?: error.javaClass.simpleName}"
            )
        }
    }

    fun detectReportDate(text: String): Long? {
        val datePatterns = listOf(
            Regex("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})"),
            Regex("(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})"),
            Regex("(\\d{1,2})\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+(\\d{4})", RegexOption.IGNORE_CASE)
        )

        for (pattern in datePatterns) {
            val match = pattern.find(text) ?: continue
            val raw = match.value
            for (format in listOf("dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "d MMM yyyy", "dd MMM yyyy")) {
                runCatching {
                    SimpleDateFormat(format, Locale.US).parse(raw)?.time
                }.getOrNull()?.let { return it }
            }
        }
        return null
    }
}
