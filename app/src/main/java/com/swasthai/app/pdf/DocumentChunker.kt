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
        reportDate: Long,
        testDateSource: String = "UNKNOWN"
    ): List<StructuredMeasurementEntity> {
        val measurements = mutableListOf<StructuredMeasurementEntity>()
        val number = "[+-]?(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\.\\d+)?"
        val resultPattern = Regex(
            "^\\s*(<=|>=|<|>|≤|≥)?\\s*($number)\\s*([^\\s()\\[\\]|,;]+)?\\s*(?:[(\\[]\\s*($number)\\s*(?:-|–|—|to)\\s*($number)\\s*[)\\]])?.*$",
            RegexOption.IGNORE_CASE
        )

        for (page in pages) {
            val lines = page.text.split("\n")
            for (line in lines) {
                val trimmed = line.trim()
                val labeled = LAB_LABELS.firstNotNullOfOrNull { (alias, definition) ->
                    val prefix = Regex(
                        "^\\s*${Regex.escape(alias)}(?:\\s*[:=|\\t]\\s*|\\s+)(.+)$",
                        RegexOption.IGNORE_CASE
                    )
                    prefix.find(trimmed)?.groupValues?.get(1)?.let { definition to it.trim() }
                } ?: continue
                val (definition, resultText) = labeled
                val match = resultPattern.matchEntire(resultText) ?: continue
                val rawNumeric = match.groupValues[2]
                val numericValue = rawNumeric.replace(",", "").toDoubleOrNull() ?: continue
                val rawUnit = match.groupValues[3].trim()
                val rangeLow = match.groupValues[4].takeIf(String::isNotBlank)?.replace(",", "")?.toDoubleOrNull()
                val rangeHigh = match.groupValues[5].takeIf(String::isNotBlank)?.replace(",", "")?.toDoubleOrNull()
                val validRange = rangeLow != null && rangeHigh != null && rangeLow <= rangeHigh
                val comparator = when (match.groupValues[1]) {
                    "≤", "<=" -> "<="
                    "≥", ">=" -> ">="
                    "<" -> "<"
                    ">" -> ">"
                    else -> "="
                }
                val unitCode = UCUM_UNITS[rawUnit.lowercase()]
                measurements.add(
                    StructuredMeasurementEntity(
                        documentId = documentId,
                        biomarkerName = definition.displayName,
                        numericValue = numericValue,
                        unit = rawUnit.ifBlank { "unit not stated" },
                        referenceLow = rangeLow.takeIf { validRange },
                        referenceHigh = rangeHigh.takeIf { validRange },
                        testDate = reportDate,
                        rawText = trimmed,
                        canonicalCode = definition.loincCode,
                        sourcePageNumber = page.pageNumber,
                        reviewStatus = "UNREVIEWED",
                        canonicalSystem = "http://loinc.org".takeIf { definition.loincCode != null },
                        unitSystem = "http://unitsofmeasure.org".takeIf { unitCode != null },
                        unitCode = unitCode,
                        valueComparator = comparator,
                        rawValue = match.value.trim(),
                        testDateSource = testDateSource,
                        referenceRangeSource = "SOURCE_REPORT".takeIf { validRange },
                        extractionMethod = EXTRACTION_METHOD,
                        schemaVersion = 1
                    )
                )
            }
        }

        return measurements
    }

    private data class LabLabel(val displayName: String, val loincCode: String?)

    private val LAB_LABELS = listOf(
        "hemoglobin a1c" to LabLabel("Hemoglobin A1c", "4548-4"),
        "glycated hemoglobin" to LabLabel("Hemoglobin A1c", "4548-4"),
        "hba1c" to LabLabel("Hemoglobin A1c", "4548-4"),
        "hb a1c" to LabLabel("Hemoglobin A1c", "4548-4"),
        "fasting blood sugar" to LabLabel("Glucose (fasting)", null),
        "fasting glucose" to LabLabel("Glucose (fasting)", null),
        "total cholesterol" to LabLabel("Total cholesterol", "2093-3"),
        "white blood cell count" to LabLabel("Leukocytes", "6690-2"),
        "white blood cells" to LabLabel("Leukocytes", "6690-2"),
        "red blood cell count" to LabLabel("Erythrocytes", "789-8"),
        "red blood cells" to LabLabel("Erythrocytes", "789-8"),
        "platelet count" to LabLabel("Platelets", "777-3"),
        "hematocrit" to LabLabel("Hematocrit", "4544-3"),
        "packed cell volume" to LabLabel("Hematocrit", "4544-3"),
        "serum creatinine" to LabLabel("Creatinine", "2160-0"),
        "creatinine" to LabLabel("Creatinine", "2160-0"),
        "serum glucose" to LabLabel("Glucose", "2345-7"),
        "blood glucose" to LabLabel("Glucose", "2345-7"),
        "glucose" to LabLabel("Glucose", "2345-7"),
        "triglycerides" to LabLabel("Triglyceride", "2571-8"),
        "triglyceride" to LabLabel("Triglyceride", "2571-8"),
        "hemoglobin" to LabLabel("Hemoglobin", "718-7"),
        "haemoglobin" to LabLabel("Hemoglobin", "718-7"),
        "platelets" to LabLabel("Platelets", "777-3"),
        "wbc" to LabLabel("Leukocytes", "6690-2"),
        "rbc" to LabLabel("Erythrocytes", "789-8"),
        "cholesterol" to LabLabel("Total cholesterol", "2093-3"),
        "hb" to LabLabel("Hemoglobin", "718-7"),
        "हीमोग्लोबिन" to LabLabel("Hemoglobin", "718-7"),
        "ग्लूकोज" to LabLabel("Glucose", "2345-7"),
        "प्लेटलेट्स" to LabLabel("Platelets", "777-3")
    ).sortedByDescending { it.first.length }

    private val UCUM_UNITS = mapOf(
        "mg/dl" to "mg/dL",
        "g/dl" to "g/dL",
        "g/l" to "g/L",
        "mmol/l" to "mmol/L",
        "%" to "%",
        "10^3/ul" to "10*3/uL",
        "10*3/ul" to "10*3/uL",
        "10^6/ul" to "10*6/uL",
        "10*6/ul" to "10*6/uL"
    )

    private const val EXTRACTION_METHOD = "label-allowlist-v1"
}
