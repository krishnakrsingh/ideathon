package com.swasthai.app.pdf

import com.swasthai.app.data.local.entity.DocumentChunkEntity
import com.swasthai.app.data.local.entity.StructuredMeasurementEntity

object DocumentChunker {

    private const val TARGET_CHUNK_SIZE = 450
    private const val CHUNK_OVERLAP = 60

    /**
     * Splits extracted pages into stable, overlapping chunks with page metadata.
     */
    fun chunkPages(
        documentId: Long,
        pages: List<ExtractedPage>
    ): List<DocumentChunkEntity> {
        val result = mutableListOf<DocumentChunkEntity>()
        var globalChunkIndex = 0

        for (page in pages) {
            val text = page.text.trim()
            if (text.isBlank()) continue

            // Split into paragraphs / lines
            val paragraphs = text.split(Regex("\n{2,}")).map { it.trim() }.filter { it.isNotBlank() }

            if (paragraphs.isEmpty()) {
                val chunks = chunkTextDirectly(text, TARGET_CHUNK_SIZE, CHUNK_OVERLAP)
                for (chunk in chunks) {
                    result.add(
                        DocumentChunkEntity(
                            documentId = documentId,
                            chunkIndex = globalChunkIndex++,
                            pageNumber = page.pageNumber,
                            text = chunk,
                            tokenCount = estimateTokens(chunk)
                        )
                    )
                }
                continue
            }

            var currentBuffer = StringBuilder()

            for (paragraph in paragraphs) {
                if (currentBuffer.length + paragraph.length <= TARGET_CHUNK_SIZE) {
                    if (currentBuffer.isNotEmpty()) currentBuffer.append("\n\n")
                    currentBuffer.append(paragraph)
                } else {
                    if (currentBuffer.isNotEmpty()) {
                        val chunkStr = currentBuffer.toString().trim()
                        result.add(
                            DocumentChunkEntity(
                                documentId = documentId,
                                chunkIndex = globalChunkIndex++,
                                pageNumber = page.pageNumber,
                                text = chunkStr,
                                tokenCount = estimateTokens(chunkStr)
                            )
                        )
                        // Preserve overlap
                        val overlapText = if (chunkStr.length > CHUNK_OVERLAP) {
                            chunkStr.takeLast(CHUNK_OVERLAP)
                        } else ""
                        currentBuffer = StringBuilder(overlapText)
                        if (currentBuffer.isNotEmpty()) currentBuffer.append("\n\n")
                    }

                    if (paragraph.length > TARGET_CHUNK_SIZE) {
                        val subChunks = chunkTextDirectly(paragraph, TARGET_CHUNK_SIZE, CHUNK_OVERLAP)
                        for (sub in subChunks) {
                            result.add(
                                DocumentChunkEntity(
                                    documentId = documentId,
                                    chunkIndex = globalChunkIndex++,
                                    pageNumber = page.pageNumber,
                                    text = sub,
                                    tokenCount = estimateTokens(sub)
                                )
                            )
                        }
                        currentBuffer.clear()
                    } else {
                        currentBuffer.append(paragraph)
                    }
                }
            }

            if (currentBuffer.isNotBlank()) {
                val remaining = currentBuffer.toString().trim()
                result.add(
                    DocumentChunkEntity(
                        documentId = documentId,
                        chunkIndex = globalChunkIndex++,
                        pageNumber = page.pageNumber,
                        text = remaining,
                        tokenCount = estimateTokens(remaining)
                    )
                )
            }
        }

        return result
    }

    private fun chunkTextDirectly(text: String, size: Int, overlap: Int): List<String> {
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            val end = minOf(start + size, text.length)
            chunks.add(text.substring(start, end).trim())
            if (end == text.length) break
            start += (size - overlap)
        }
        return chunks.filter { it.isNotBlank() }
    }

    private fun estimateTokens(text: String): Int {
        return (text.length / 4).coerceAtLeast(1)
    }

    /**
     * Extracts structured lab biomarkers from raw text lines.
     * Looks for patterns like:
     * "Hemoglobin: 14.2 g/dL (13.0 - 17.0)"
     * "Platelets: 250000 /mcL (150000 - 450000)"
     * "Fasting Blood Sugar: 95 mg/dL (70 - 99)"
     */
    fun extractStructuredMeasurements(
        documentId: Long,
        pages: List<ExtractedPage>,
        reportDate: Long
    ): List<StructuredMeasurementEntity> {
        val measurements = mutableListOf<StructuredMeasurementEntity>()

        val linePattern = Regex(
            "([A-Za-z0-9\\s\\-()]+?)\\s*[:\\t|]\\s*([0-9]+(?:\\.[0-9]+)?)\\s*([A-Za-z/%µmcgmgdLIU]+)?(?:\\s*[(\\[]([0-9]+(?:\\.[0-9]+)?)\\s*-\\s*([0-9]+(?:\\.[0-9]+)?)[)\\]])?",
            RegexOption.IGNORE_CASE
        )

        for (page in pages) {
            val lines = page.text.split("\n")
            for (line in lines) {
                val match = linePattern.find(line.trim()) ?: continue
                val name = match.groupValues[1].trim()
                val valStr = match.groupValues[2].trim()
                val unit = match.groupValues[3].trim().ifBlank { "units" }
                val lowStr = match.groupValues.getOrNull(4)?.trim()
                val highStr = match.groupValues.getOrNull(5)?.trim()

                // Filter out non-biomarker metadata lines
                val lowerName = name.lowercase()
                if (name.length < 3 ||
                    lowerName == "page" ||
                    lowerName.contains("date") ||
                    lowerName.contains("patient") ||
                    lowerName.contains("specimen") ||
                    lowerName.contains("doctor") ||
                    lowerName.contains("hospital") ||
                    lowerName.contains("clinic") ||
                    lowerName.contains("laboratory")
                ) {
                    continue
                }

                val numVal = valStr.toDoubleOrNull() ?: continue
                val low = lowStr?.toDoubleOrNull()
                val high = highStr?.toDoubleOrNull()

                measurements.add(
                    StructuredMeasurementEntity(
                        documentId = documentId,
                        biomarkerName = name,
                        numericValue = numVal,
                        unit = unit,
                        referenceLow = low,
                        referenceHigh = high,
                        testDate = reportDate,
                        rawText = line.trim()
                    )
                )
            }
        }

        return measurements
    }
}
