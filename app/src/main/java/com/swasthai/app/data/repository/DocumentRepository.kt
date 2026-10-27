package com.swasthai.app.data.repository

import android.content.Context
import android.net.Uri
import com.swasthai.app.ai.EmbeddingEngine
import com.swasthai.app.data.local.dao.DocumentDao
import com.swasthai.app.data.local.dao.MeasurementDao
import com.swasthai.app.data.local.dao.RAGDao
import com.swasthai.app.data.local.entity.ChunkEmbeddingEntity
import com.swasthai.app.data.local.entity.DocumentChunkEntity
import com.swasthai.app.data.local.entity.DocumentEntity
import com.swasthai.app.domain.model.IndexingStatus
import com.swasthai.app.domain.model.MedicalDocument
import com.swasthai.app.domain.model.ModelInferenceStatus
import com.swasthai.app.pdf.DocumentChunker
import com.swasthai.app.pdf.PdfExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DocumentRepository(
    private val context: Context,
    private val documentDao: DocumentDao,
    private val ragDao: RAGDao,
    private val measurementDao: MeasurementDao,
    private val embeddingEngine: EmbeddingEngine
) {

    private val vaultDir = File(context.filesDir, "medical_vault").apply {
        if (!exists()) mkdirs()
    }

    fun getAllDocuments(): Flow<List<MedicalDocument>> {
        return documentDao.getAllDocuments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getDocumentById(id: Long): MedicalDocument? = withContext(Dispatchers.IO) {
        documentDao.getDocumentById(id)?.toDomain()
    }

    suspend fun getChunksForDocument(id: Long): List<DocumentChunkEntity> = withContext(Dispatchers.IO) {
        documentDao.getChunksForDocument(id)
    }

    /**
     * Imports a PDF from content URI.
     * Preserves original file locally in vault, extracts text, chunks, indexes embeddings, and extracts biomarkers.
     */
    suspend fun importDocument(uri: Uri, fileName: String): Result<MedicalDocument> = withContext(Dispatchers.IO) {
        try {
            val safeFileName = "${System.currentTimeMillis()}_$fileName"
            val destinationFile = File(vaultDir, safeFileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Cannot read document from URI"))

            processAndIndexFile(destinationFile, fileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imports a verified clinical sample laboratory panel (CBC & Metabolic Panel)
     * into the app vault, extracting 12+ real clinical biomarkers and indexing chunks.
     */
    suspend fun importSampleDocument(): Result<MedicalDocument> = withContext(Dispatchers.IO) {
        try {
            val fileName = "Sample_Comprehensive_Health_Panel.pdf"
            val safeFileName = "${System.currentTimeMillis()}_$fileName"
            val destinationFile = File(vaultDir, safeFileName)

            val samplePdfBytes = generateSamplePdfBytes()
            FileOutputStream(destinationFile).use { output ->
                output.write(samplePdfBytes)
            }

            processAndIndexFile(destinationFile, "Comprehensive Health Panel (Sample).pdf")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun processAndIndexFile(destinationFile: File, displayFileName: String): Result<MedicalDocument> {
        // 1. Insert initial document record (PENDING)
        val docEntity = DocumentEntity(
            fileName = displayFileName,
            localFilePath = destinationFile.absolutePath,
            fileSize = destinationFile.length(),
            importDate = System.currentTimeMillis(),
            indexingStatus = IndexingStatus.PENDING.name
        )
        val docId = documentDao.insertDocument(docEntity)

        // 2. Extract text locally
        val extraction = PdfExtractor.extractTextFromPdf(destinationFile)

        if (extraction.isScannedOnly || extraction.pages.isEmpty()) {
            val failedEntity = docEntity.copy(
                id = docId,
                indexingStatus = IndexingStatus.FAILED.name,
                failureReason = extraction.failureReason ?: "No extractable text"
            )
            documentDao.updateDocument(failedEntity)
            return Result.success(failedEntity.toDomain())
        }

        // 3. Split into semantic chunks
        val chunks = DocumentChunker.chunkPages(docId, extraction.pages)
        val chunkIds = documentDao.insertChunks(chunks)

        // 4. Extract structured biomarkers
        val measurements = DocumentChunker.extractStructuredMeasurements(
            documentId = docId,
            pages = extraction.pages,
            reportDate = extraction.reportDate ?: System.currentTimeMillis()
        )
        if (measurements.isNotEmpty()) {
            measurementDao.insertMeasurements(measurements)
        }

        // 5. Generate embeddings if embedding engine is ready
        var finalStatus = IndexingStatus.EXTRACTED
        if (embeddingEngine.status == ModelInferenceStatus.READY) {
            val embeddings = mutableListOf<ChunkEmbeddingEntity>()
            for ((idx, chunk) in chunks.withIndex()) {
                val assignedId = chunkIds.getOrElse(idx) { chunk.id }
                val vec = embeddingEngine.generateEmbedding(chunk.text, isQuery = false)
                if (vec != null) {
                    embeddings.add(
                        ChunkEmbeddingEntity(
                            chunkId = assignedId,
                            documentId = docId,
                            dimension = vec.size,
                            embeddingBlob = EmbeddingEngine.floatsToByteArray(vec)
                        )
                    )
                }
            }
            if (embeddings.isNotEmpty()) {
                ragDao.insertEmbeddings(embeddings)
                finalStatus = IndexingStatus.INDEXED
            }
        }

        val updatedEntity = docEntity.copy(
            id = docId,
            pageCount = extraction.pages.size,
            reportDate = extraction.reportDate,
            indexingStatus = finalStatus.name
        )
        documentDao.updateDocument(updatedEntity)

        return Result.success(updatedEntity.toDomain())
    }

    private fun generateSamplePdfBytes(): ByteArray {
        val lines = listOf(
            "METROPOLITAN CLINICAL HEALTH LABORATORIES",
            "Patient Diagnostic Health Panel & Laboratory Report",
            "Report Date: 15/10/2026",
            "Specimen Type: Whole Blood / Fasting Serum",
            "",
            "COMPLETE BLOOD COUNT (CBC) WITH DIFFERENTIAL",
            "Hemoglobin: 14.8 g/dL (13.5 - 17.5)",
            "RBC Count: 5.10 x10^6/uL (4.50 - 5.90)",
            "White Blood Cells (WBC): 6.8 x10^3/uL (4.5 - 11.0)",
            "Platelets: 245 x10^3/uL (150 - 450)",
            "Hematocrit: 44.2 % (41.0 - 50.0)",
            "",
            "COMPREHENSIVE METABOLIC & GLYCEMIC PANEL",
            "Fasting Blood Sugar: 92 mg/dL (70 - 99)",
            "Serum Creatinine: 0.95 mg/dL (0.70 - 1.30)",
            "Blood Urea Nitrogen (BUN): 14 mg/dL (7 - 20)",
            "ALT (SGPT): 24 U/L (7 - 56)",
            "AST (SGOT): 22 U/L (8 - 48)",
            "",
            "LIPID CARDIOVASCULAR PANEL",
            "Total Cholesterol: 185 mg/dL (100 - 199)",
            "HDL Cholesterol: 55 mg/dL (40 - 60)",
            "LDL Cholesterol: 108 mg/dL (50 - 99)",
            "Triglycerides: 130 mg/dL (50 - 149)",
            "",
            "MICRONUTRIENT & ENDOCRINE STATUS",
            "Vitamin D (25-OH): 38 ng/mL (30 - 100)",
            "Vitamin B12: 450 pg/mL (200 - 900)",
            "Thyroid Stimulating Hormone (TSH): 2.1 uIU/mL (0.4 - 4.0)",
            "",
            "CLINICAL SUMMARY & PHYSICIAN NOTES:",
            "Patient demonstrates balanced metabolic and hematologic parameters.",
            "All vital organ markers fall within expected normal physiological limits.",
            "Continue standard healthy diet, daily hydration, and routine physical activity."
        )

        val streamContent = buildString {
            append("BT\n/F1 12 Tf\n50 720 Td\n")
            for (line in lines) {
                val escaped = line.replace("(", "\\(").replace(")", "\\)")
                append("($escaped) Tj\nT*\n")
            }
            append("ET\n")
        }

        val pdf = buildString {
            append("%PDF-1.4\n")
            append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
            append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")
            append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n")
            append("4 0 obj\n<< /Length ${streamContent.length} >>\nstream\n")
            append(streamContent)
            append("endstream\nendobj\n")
            append("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n")
            append("xref\n0 6\n0000000000 65535 f \n0000000010 00000 n \n0000000060 00000 n \n0000000117 00000 n \n0000000234 00000 n \n0000000450 00000 n \n")
            append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n540\n%%EOF\n")
        }

        return pdf.toByteArray(Charsets.ISO_8859_1)
    }

    /**
     * Deletes a document and cascades deletion of its local file, chunks, embeddings, and measurements.
     */
    suspend fun deleteDocument(id: Long): Boolean = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(id) ?: return@withContext false

        // Delete physical file
        try {
            val file = File(doc.localFilePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}

        // Cascade delete in Room
        documentDao.deleteDocumentById(id)
        measurementDao.deleteMeasurementsForDocument(id)
        true
    }

    private fun DocumentEntity.toDomain(): MedicalDocument {
        return MedicalDocument(
            id = id,
            fileName = fileName,
            localFilePath = localFilePath,
            fileSize = fileSize,
            importDate = importDate,
            reportDate = reportDate,
            indexingStatus = IndexingStatus.valueOf(indexingStatus),
            pageCount = pageCount,
            failureReason = failureReason
        )
    }
}
