package com.swasthai.app.pdf

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

data class ExtractedPage(
    val pageNumber: Int,
    val text: String
)

data class ExtractionResult(
    val pages: List<ExtractedPage>,
    val reportDate: Long?,
    val isScannedOnly: Boolean,
    val failureReason: String? = null
)

object PdfExtractor {

    /**
     * Extracts text streams from a PDF file preserving page breaks.
     * Parses standard PDF text objects (BT ... ET) and stream blocks.
     */
    fun extractTextFromPdf(file: File): ExtractionResult {
        if (!file.exists() || file.length() == 0L) {
            return ExtractionResult(
                pages = emptyList(),
                reportDate = null,
                isScannedOnly = false,
                failureReason = "File does not exist or is empty (0 bytes)."
            )
        }

        return try {
            val bytes = file.readBytes()
            val pages = parsePdfTextPages(bytes)

            val totalText = pages.joinToString("\n") { it.text }.trim()
            if (totalText.isBlank()) {
                ExtractionResult(
                    pages = emptyList(),
                    reportDate = null,
                    isScannedOnly = true,
                    failureReason = "No extractable text found in PDF. Scanned images or rasterized PDFs require OCR, which is not yet supported in this version."
                )
            } else {
                val detectedDate = detectReportDate(totalText)
                ExtractionResult(
                    pages = pages,
                    reportDate = detectedDate,
                    isScannedOnly = false
                )
            }
        } catch (e: Exception) {
            ExtractionResult(
                pages = emptyList(),
                reportDate = null,
                isScannedOnly = false,
                failureReason = "Error parsing PDF: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    /**
     * Core PDF text stream decoder:
     * Scans for /Type /Page objects and extracts text operators (Tj, TJ, ') between BT and ET.
     */
    private fun parsePdfTextPages(bytes: ByteArray): List<ExtractedPage> {
        val content = String(bytes, Charsets.ISO_8859_1)
        val pages = mutableListOf<ExtractedPage>()

        // Split by /Page objects or form feeds / stream blocks
        val pageMarkers = listOf("/Type /Page", "/Type/Page")
        var pageCount = 0
        for (marker in pageMarkers) {
            val count = countOccurrences(content, marker)
            if (count > pageCount) pageCount = count
        }
        if (pageCount == 0) pageCount = 1

        // Extract all text inside BT ... ET blocks
        val btPattern = Pattern.compile("BT[\\s\\r\\n]+(.*?)[\\s\\r\\n]+ET", Pattern.DOTALL)
        val matcher = btPattern.matcher(content)
        val textBlocks = mutableListOf<String>()

        while (matcher.find()) {
            val block = matcher.group(1) ?: continue
            val decoded = decodePdfTextBlock(block)
            if (decoded.isNotBlank()) {
                textBlocks.add(decoded)
            }
        }

        if (textBlocks.isEmpty()) {
            // Fallback: search for readable ASCII text chunks directly in streams
            val fallbackText = extractPlainReadableText(bytes)
            if (fallbackText.isNotBlank()) {
                pages.add(ExtractedPage(pageNumber = 1, text = fallbackText))
                return pages
            }
            return emptyList()
        }

        // Distribute text blocks across estimated page boundaries
        val blocksPerPage = (textBlocks.size + pageCount - 1) / pageCount
        for (pageIdx in 0 until pageCount) {
            val start = pageIdx * blocksPerPage
            val end = minOf(start + blocksPerPage, textBlocks.size)
            if (start < textBlocks.size) {
                val pageText = textBlocks.subList(start, end).joinToString("\n")
                if (pageText.isNotBlank()) {
                    pages.add(ExtractedPage(pageNumber = pageIdx + 1, text = pageText.trim()))
                }
            }
        }

        if (pages.isEmpty() && textBlocks.isNotEmpty()) {
            pages.add(ExtractedPage(pageNumber = 1, text = textBlocks.joinToString("\n")))
        }

        return pages
    }

    fun decodePdfTextBlock(block: String): String {
        val sb = StringBuilder()
        var i = 0
        val len = block.length
        var lastWasNewline = false

        while (i < len) {
            val c = block[i]

            // 1. Literal string: ( ... )
            if (c == '(') {
                i++
                val strBuilder = StringBuilder()
                var depth = 1
                while (i < len && depth > 0) {
                    val sc = block[i]
                    if (sc == '\\' && i + 1 < len) {
                        when (val next = block[i + 1]) {
                            'n' -> strBuilder.append('\n')
                            'r' -> strBuilder.append('\r')
                            't' -> strBuilder.append('\t')
                            '(', ')', '\\' -> strBuilder.append(next)
                            else -> strBuilder.append(next)
                        }
                        i += 2
                    } else if (sc == '(') {
                        depth++
                        strBuilder.append('(')
                        i++
                    } else if (sc == ')') {
                        depth--
                        if (depth > 0) strBuilder.append(')')
                        i++
                    } else {
                        strBuilder.append(sc)
                        i++
                    }
                }
                sb.append(strBuilder.toString())
                lastWasNewline = false
                continue
            }

            // 2. Hex string: < ... > (ignoring << dictionary >>)
            if (c == '<' && (i + 1 < len && block[i + 1] != '<') && (i == 0 || block[i - 1] != '<')) {
                i++
                val hexBuilder = StringBuilder()
                while (i < len && block[i] != '>') {
                    val hc = block[i]
                    if (hc.isLetterOrDigit()) hexBuilder.append(hc)
                    i++
                }
                if (i < len && block[i] == '>') i++
                val hexStr = hexBuilder.toString()
                if (hexStr.length >= 2 && hexStr.length % 2 == 0) {
                    try {
                        val hexBytes = hexStr.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                        sb.append(String(hexBytes, Charsets.ISO_8859_1))
                        lastWasNewline = false
                    } catch (_: Exception) {}
                }
                continue
            }

            // 3. Newline operators: T*, ', " or Td/TD
            if (c == 'T' && i + 1 < len && block[i + 1] == '*') {
                if (!lastWasNewline && sb.isNotEmpty() && sb.last() != '\n') {
                    sb.append('\n')
                    lastWasNewline = true
                }
                i += 2
                continue
            }

            if (c == '\'' || c == '"') {
                if (!lastWasNewline && sb.isNotEmpty() && sb.last() != '\n') {
                    sb.append('\n')
                    lastWasNewline = true
                }
                i++
                continue
            }

            if (c == 'T' && i + 1 < len && (block[i + 1] == 'd' || block[i + 1] == 'D')) {
                if (!lastWasNewline && sb.isNotEmpty() && sb.last() != '\n') {
                    sb.append('\n')
                    lastWasNewline = true
                }
                i += 2
                continue
            }

            // Whitespace inside TJ array or spacing
            if (c == ' ' || c == '\t') {
                if (sb.isNotEmpty() && sb.last() != ' ' && sb.last() != '\n') {
                    sb.append(' ')
                }
            }

            i++
        }

        return sb.toString().trim()
    }

    private fun extractPlainReadableText(bytes: ByteArray): String {
        val sb = StringBuilder()
        var currentWord = StringBuilder()
        for (b in bytes) {
            val c = b.toInt().toChar()
            if (c in ' '..'~' || c == '\n' || c == '\t') {
                currentWord.append(c)
            } else {
                if (currentWord.length >= 4) {
                    sb.append(currentWord).append(" ")
                }
                currentWord.clear()
            }
        }
        if (currentWord.length >= 4) sb.append(currentWord)
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun countOccurrences(str: String, sub: String): Int {
        var count = 0
        var idx = 0
        while (str.indexOf(sub, idx).also { idx = it } != -1) {
            count++
            idx += sub.length
        }
        return count
    }

    private fun detectReportDate(text: String): Long? {
        val datePatterns = listOf(
            Regex("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})"),
            Regex("(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})"),
            Regex("(\\d{1,2})\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+(\\d{4})", RegexOption.IGNORE_CASE)
        )

        for (pattern in datePatterns) {
            val match = pattern.find(text)
            if (match != null) {
                try {
                    val raw = match.value
                    val formats = listOf(
                        "dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "d MMM yyyy", "dd MMM yyyy"
                    )
                    for (fmt in formats) {
                        try {
                            val sdf = SimpleDateFormat(fmt, Locale.US)
                            val parsed = sdf.parse(raw)
                            if (parsed != null) return parsed.time
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }
        }
        return null
    }
}
