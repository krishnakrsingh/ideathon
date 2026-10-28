package com.swasthai.app.data.repository

import android.content.Context
import android.net.Uri
import com.swasthai.app.ai.EmbeddingEngine
import com.swasthai.app.ai.ImageTextExtractor
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
import com.swasthai.app.security.EncryptedPayloadEnvelope
import com.swasthai.app.security.ProfileKeyManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.security.MessageDigest
import org.json.JSONObject

class DocumentRepository(
    private val context: Context,
    private val documentDao: DocumentDao,
    private val ragDao: RAGDao,
    private val measurementDao: MeasurementDao,
    private val embeddingEngine: EmbeddingEngine,
    private val profileKeyManager: ProfileKeyManager,
    private val personProfileRepository: PersonProfileRepository
) {

    companion object {
        const val DEFAULT_LOCAL_PROFILE_ID = "local-default-profile"
        private const val CURRENT_ENCRYPTION_CONTEXT_VERSION = 2
        private const val MAX_PDF_BYTES = 100L * 1024 * 1024
    }

    private val vaultDir = File(context.filesDir, "medical_vault").apply {
        if (!exists()) mkdirs()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllDocuments(profileId: String? = null): Flow<List<MedicalDocument>> {
        val source = if (profileId == null) {
            personProfileRepository.activeProfileId.flatMapLatest(documentDao::getAllDocuments)
        } else documentDao.getAllDocuments(profileId)
        return source.map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getDocumentById(id: Long, profileId: String? = null): MedicalDocument? = withContext(Dispatchers.IO) {
        documentDao.getDocumentById(id, profileId ?: personProfileRepository.activeProfileId.value)?.toDomain()
    }

    suspend fun getChunksForDocument(id: Long): List<DocumentChunkEntity> = withContext(Dispatchers.IO) {
        documentDao.getChunksForDocument(id)
    }

    /**
     * Imports a PDF from content URI.
     * Preserves original file locally in vault, extracts text, chunks, indexes embeddings, and extracts biomarkers.
     */
    suspend fun importDocument(uri: Uri, fileName: String, profileId: String? = null): Result<MedicalDocument> = withContext(Dispatchers.IO) {
        val stagingFile = File.createTempFile("incoming-", ".pdf", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(stagingFile).use { output ->
                    copyPdfBounded(input, output)
                    output.fd.sync()
                }
            } ?: return@withContext Result.failure(Exception("Cannot read document from URI"))

            processAndIndexFile(stagingFile, fileName, profileId ?: personProfileRepository.activeProfileId.value)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            stagingFile.delete()
        }
    }

    private suspend fun processAndIndexFile(
        plaintextInput: File,
        displayFileName: String,
        profileId: String,
        customStableId: String? = null,
        existingEncryptedFile: File? = null
    ): Result<MedicalDocument> {
        val assignedStableId = customStableId ?: UUID.randomUUID().toString()
        val docEntity = DocumentEntity(
            fileName = displayFileName,
            localFilePath = "",
            fileSize = 0,
            importDate = System.currentTimeMillis(),
            indexingStatus = IndexingStatus.PENDING.name,
            stableId = assignedStableId,
            encryptionContextVersion = CURRENT_ENCRYPTION_CONTEXT_VERSION,
            profileId = profileId
        )
        val docId = documentDao.insertDocument(docEntity)
        val timelineFolder = SimpleDateFormat("yyyy/MM", Locale.ROOT).format(Date(docEntity.importDate))
        val profileFolder = UUID.nameUUIDFromBytes(profileId.toByteArray(Charsets.UTF_8)).toString()
        val recordDir = File(vaultDir, "profiles/$profileFolder/$timelineFolder")
        if (!recordDir.isDirectory && !recordDir.mkdirs()) {
            documentDao.deleteDocumentById(docId, profileId)
            return Result.failure(IllegalStateException("Could not create the profile record folder"))
        }
        val encryptedFile = existingEncryptedFile ?: File(recordDir, "record_${docEntity.stableId}.swai")
        val encryptedTemp = File(recordDir, ".record_$docId.${UUID.randomUUID()}.part")
        val extractionFile = File(context.cacheDir, "extract_$docId.${UUID.randomUUID()}.pdf")
        val key = try {
            profileKeyManager.getOrCreateProfileKey(profileId)
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                ragDao.deleteEmbeddingsForDocument(docId)
                measurementDao.deleteMeasurementsForDocument(docId)
                documentDao.deleteDocumentById(docId, profileId)
                if (existingEncryptedFile == null) encryptedFile.delete()
                encryptedTemp.delete()
            }
            throw cancelled
        } catch (error: Exception) {
            documentDao.deleteDocumentById(docId, profileId)
            return Result.failure(error)
        }

        try {
            if (existingEncryptedFile == null) {
                EncryptedPayloadEnvelope.encryptFile(plaintextInput, encryptedTemp, key, recordAad(profileId, docEntity.stableId))
                moveReplacing(encryptedTemp, encryptedFile)
                plaintextInput.delete()
            }
            EncryptedPayloadEnvelope.decryptFile(encryptedFile, extractionFile, key, recordAad(profileId, docEntity.stableId))

            val extraction = PdfExtractor.extractTextFromPdf(extractionFile)
            var ocrFailureReason: String? = null
            val ocrResult = try {
                ImageTextExtractor.extractMissingPdfPages(
                    extractionFile,
                    extraction.pages.mapTo(mutableSetOf()) { it.pageNumber }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.w("DocumentRepository", "Local PDF OCR failed for document $docId", error)
                ocrFailureReason = error.localizedMessage ?: error.javaClass.simpleName
                null
            }
            val renderedPageCount = ocrResult?.pageCount
            val extractedPages = (extraction.pages + ocrResult?.pages.orEmpty())
                .distinctBy { it.pageNumber }
                .filter { renderedPageCount == null || it.pageNumber <= renderedPageCount }
                .sortedBy { it.pageNumber }
            if (extractedPages.isEmpty()) {
                val failure = "This PDF contains no readable text after local OCR. Try a clearer scan or another copy."
                documentDao.updateDocument(
                    docEntity.copy(
                        id = docId,
                        localFilePath = encryptedFile.absolutePath,
                        fileSize = encryptedFile.length(),
                        indexingStatus = IndexingStatus.FAILED.name,
                        failureReason = failure
                    )
                )
                return Result.failure(IllegalArgumentException(failure))
            }

            val chunks = DocumentChunker.chunkPages(docId, extractedPages)
            val chunkIds = documentDao.insertChunks(chunks)
            val detectedReportDate = extraction.reportDate ?: PdfExtractor.detectReportDate(
                extractedPages.joinToString("\n") { it.text }
            )
            val resolvedReportDate = detectedReportDate ?: docEntity.importDate
            val sourcePageCount = renderedPageCount ?: extraction.pageCount
                ?: extractedPages.maxOfOrNull { it.pageNumber } ?: extractedPages.size
            val extractedPageNumbers = extractedPages.mapTo(mutableSetOf()) { it.pageNumber }
            val pagesWithoutText = (1..sourcePageCount).filterNot { it in extractedPageNumbers }
            val failedOcrPages = ocrResult?.failedPageNumbers.orEmpty()
            val incomplete = pagesWithoutText.isNotEmpty() || failedOcrPages.isNotEmpty()
            val extractionNote = buildList {
                if (pagesWithoutText.isNotEmpty()) {
                    add("No searchable text was extracted from page${if (pagesWithoutText.size == 1) "" else "s"} ${pagesWithoutText.take(12).joinToString(", ")}${if (pagesWithoutText.size > 12) " and ${pagesWithoutText.size - 12} more" else ""}.")
                }
                if (failedOcrPages.isNotEmpty()) add("OCR failed on page${if (failedOcrPages.size == 1) "" else "s"} ${failedOcrPages.take(12).joinToString(", ")}.")
                if (pagesWithoutText.isNotEmpty()) ocrFailureReason?.let { add("OCR could not run: $it") }
            }.joinToString(" ").takeIf { it.isNotBlank() }
            val measurements = DocumentChunker.extractStructuredMeasurements(
                documentId = docId,
                pages = extractedPages,
                reportDate = resolvedReportDate,
                testDateSource = if (detectedReportDate != null) "REPORT_DATE" else "IMPORT_DATE_FALLBACK"
            )
            if (measurements.isNotEmpty()) measurementDao.insertMeasurements(measurements)

            var finalStatus = IndexingStatus.EXTRACTED
            if (embeddingEngine.status == ModelInferenceStatus.READY) {
                val embeddings = mutableListOf<ChunkEmbeddingEntity>()
                for ((idx, chunk) in chunks.withIndex()) {
                    val assignedId = chunkIds.getOrElse(idx) { chunk.id }
                    val vector = embeddingEngine.generateEmbedding(chunk.text, isQuery = false)
                    if (vector != null) {
                        embeddings += ChunkEmbeddingEntity(
                            chunkId = assignedId,
                            documentId = docId,
                            dimension = vector.size,
                            embeddingBlob = EmbeddingEngine.floatsToByteArray(vector)
                        )
                    }
                }
                if (embeddings.isNotEmpty()) {
                    ragDao.insertEmbeddings(embeddings)
                    finalStatus = IndexingStatus.INDEXED
                }
            }

            if (incomplete) finalStatus = IndexingStatus.PARTIAL

            val completed = docEntity.copy(
                id = docId,
                localFilePath = encryptedFile.absolutePath,
                fileSize = encryptedFile.length(),
                pageCount = sourcePageCount,
                reportDate = resolvedReportDate,
                indexingStatus = finalStatus.name,
                failureReason = extractionNote
            )
            documentDao.updateDocument(completed)
            return Result.success(completed.toDomain())
        } catch (error: Exception) {
            ragDao.deleteEmbeddingsForDocument(docId)
            measurementDao.deleteMeasurementsForDocument(docId)
            documentDao.deleteDocumentById(docId, profileId)
            encryptedFile.delete()
            return Result.failure(error)
        } finally {
            encryptedTemp.delete()
            extractionFile.delete()
            plaintextInput.delete()
        }
    }

    /** One-time, crash-tolerant migration of previously stored plaintext PDFs. */
    suspend fun encryptExistingPlaintextReports() = withContext(Dispatchers.IO) {
        cleanupAbandonedPlaintextStaging()
        for (document in documentDao.getAllDocumentsList()) {
            val key = profileKeyManager.getOrCreateProfileKey(document.profileId)
            val file = File(document.localFilePath)
            if (!file.isFile) continue
            if (hasEncryptedEnvelope(file)) {
                if (document.encryptionContextVersion != CURRENT_ENCRYPTION_CONTEXT_VERSION) {
                    val plaintextTemp = File(context.cacheDir, "migrate_${UUID.randomUUID()}.pdf")
                    val encryptedTemp = File(file.parentFile, ".${file.name}.${UUID.randomUUID()}.part")
                    try {
                        val legacyContexts = buildList {
                            add(recordAad(document.profileId, document.stableId))
                            if (document.encryptionContextVersion == 0) add(byteArrayOf())
                            add(legacyRecordAad(document.profileId, document.id))
                        }.distinctBy(ByteArray::contentHashCode)
                        val decrypted = legacyContexts.any { oldContext ->
                            runCatching {
                                EncryptedPayloadEnvelope.decryptFile(file, plaintextTemp, key, oldContext)
                            }.isSuccess
                        }
                        check(decrypted) { "Existing encrypted report could not be authenticated" }
                        EncryptedPayloadEnvelope.encryptFile(
                            plaintextTemp,
                            encryptedTemp,
                            key,
                            recordAad(document.profileId, document.stableId)
                        )
                        moveReplacing(encryptedTemp, file)
                    } finally {
                        plaintextTemp.delete()
                        encryptedTemp.delete()
                    }
                }
                if (document.fileSize != file.length()) {
                    documentDao.updateDocument(
                        document.copy(
                            fileSize = file.length(),
                            encryptionContextVersion = CURRENT_ENCRYPTION_CONTEXT_VERSION
                        )
                    )
                } else if (document.encryptionContextVersion != CURRENT_ENCRYPTION_CONTEXT_VERSION) {
                    documentDao.updateDocument(
                        document.copy(encryptionContextVersion = CURRENT_ENCRYPTION_CONTEXT_VERSION)
                    )
                }
                continue
            }
            val encryptedTemp = File(file.parentFile, ".${file.name}.${UUID.randomUUID()}.part")
            try {
                EncryptedPayloadEnvelope.encryptFile(file, encryptedTemp, key, recordAad(document.profileId, document.stableId))
                moveReplacing(encryptedTemp, file)
                documentDao.updateDocument(
                    document.copy(
                        fileSize = file.length(),
                        encryptionContextVersion = CURRENT_ENCRYPTION_CONTEXT_VERSION
                    )
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                encryptedTemp.delete()
                android.util.Log.e("DocumentRepository", "Could not encrypt stored report ${document.id}", error)
                throw error
            }
        }
    }

    suspend fun cleanupAbandonedPlaintextStaging() = withContext(Dispatchers.IO) {
        context.cacheDir.listFiles()
            ?.filter { it.name.startsWith("incoming-") || it.name.startsWith("extract_") || it.name.startsWith("migrate_") }
            ?.forEach(File::delete)
        vaultDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".part") }
            .forEach(File::delete)
    }

    private fun copyPdfBounded(input: java.io.InputStream, output: FileOutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= MAX_PDF_BYTES) { "Report exceeds the 100 MB import limit" }
            output.write(buffer, 0, count)
        }
    }

    private fun hasEncryptedEnvelope(file: File): Boolean = runCatching {
        file.inputStream().use { input ->
            val headerAndNonceAndTag = ByteArray(EncryptedPayloadEnvelope.HEADER_AND_NONCE_AND_TAG_BYTES)
            var offset = 0
            while (offset < headerAndNonceAndTag.size) {
                val read = input.read(headerAndNonceAndTag, offset, headerAndNonceAndTag.size - offset)
                if (read < 0) return false
                offset += read
            }
            EncryptedPayloadEnvelope.hasSupportedEnvelopeHeader(headerAndNonceAndTag)
        }
    }.getOrDefault(false)

    private fun moveReplacing(source: File, destination: File) {
        try {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun recordAad(profileId: String, stableDocumentId: String) =
        "swasthai-record-v2:$profileId:$stableDocumentId".toByteArray(Charsets.UTF_8)

    private fun legacyRecordAad(profileId: String, documentId: Long) =
        "swasthai-record-v1:$profileId:$documentId".toByteArray(Charsets.UTF_8)

    /** Backfills vectors for locally stored chunks after the embedding model becomes available. */
    suspend fun indexMissingEmbeddings() = withContext(Dispatchers.IO) {
        if (embeddingEngine.status != ModelInferenceStatus.READY) return@withContext

        for (document in documentDao.getAllDocumentsList()) {
            val chunks = documentDao.getChunksForDocument(document.id)
            if (chunks.isEmpty()) continue

            val existing = ragDao.getEmbeddingsForDocument(document.id)
            val staleIds = existing
                .filter { it.dimension != EmbeddingEngine.EMBEDDING_DIMENSION || it.embeddingBlob.size != EmbeddingEngine.EMBEDDING_DIMENSION * Float.SIZE_BYTES }
                .map { it.chunkId }
                .toSet()
            if (staleIds.isNotEmpty()) ragDao.deleteEmbeddingsForDocument(document.id)

            val indexedIds = if (staleIds.isEmpty()) existing.map { it.chunkId }.toSet() else emptySet()
            val missing = chunks.filterNot { it.id in indexedIds }
            val embeddings = missing.mapNotNull { chunk ->
                embeddingEngine.generateEmbedding(chunk.text, isQuery = false)?.let { vector ->
                    ChunkEmbeddingEntity(
                        chunkId = chunk.id,
                        documentId = document.id,
                        dimension = vector.size,
                        embeddingBlob = EmbeddingEngine.floatsToByteArray(vector)
                    )
                }
            }
            if (embeddings.isNotEmpty()) ragDao.insertEmbeddings(embeddings)
            if ((embeddings.isNotEmpty() || indexedIds.isNotEmpty()) && document.indexingStatus != IndexingStatus.PARTIAL.name) {
                documentDao.updateDocument(document.copy(indexingStatus = IndexingStatus.INDEXED.name))
            }
        }
    }

    /**
     * Deletes a document and cascades deletion of its local file, chunks, embeddings, and measurements.
     */
    suspend fun deleteDocument(id: Long, profileId: String? = null): Boolean = withContext(Dispatchers.IO) {
        val activeProfileId = profileId ?: personProfileRepository.activeProfileId.value
        val doc = documentDao.getDocumentById(id, activeProfileId) ?: return@withContext false

        // Delete physical file
        val file = File(doc.localFilePath)
        if (file.exists() && !file.delete()) return@withContext false

        // Cascade delete in Room
        documentDao.deleteDocumentById(id, activeProfileId)
        measurementDao.deleteMeasurementsForDocument(id)
        true
    }

    suspend fun deleteAllDocuments(profileId: String? = null): Int = withContext(Dispatchers.IO) {
        val activeProfileId = profileId ?: personProfileRepository.activeProfileId.value
        val documents = documentDao.getAllDocumentsList(activeProfileId)
        for (document in documents) {
            check(deleteDocument(document.id, activeProfileId)) {
                "Could not delete report ${document.fileName}; it remains in the vault."
            }
        }
        documents.size
    }

    data class SyncSummary(
        val uploadedCount: Int,
        val downloadedCount: Int,
        val errors: List<String> = emptyList()
    )

    suspend fun syncEncryptedCloudRecords(
        accountFamilyRepo: AccountFamilyRepository,
        profileId: String? = null
    ): SyncSummary = withContext(Dispatchers.IO) {
        val targetProfileId = profileId ?: personProfileRepository.activeProfileId.value
        val key = try {
            profileKeyManager.getOrCreateProfileKey(targetProfileId)
        } catch (e: Exception) {
            return@withContext SyncSummary(0, 0, listOf("Profile key unavailable: ${e.message}"))
        }

        var uploaded = 0
        var downloaded = 0
        val errors = mutableListOf<String>()

        // 1. Upload local documents
        val localDocs = documentDao.getAllDocumentsList(targetProfileId)
        for (doc in localDocs) {
            val file = File(doc.localFilePath)
            if (!file.isFile || file.length() == 0L) continue
            try {
                val fileBytes = file.readBytes()
                val sha256Bytes = MessageDigest.getInstance("SHA-256").digest(fileBytes)
                val sha256Hex = sha256Bytes.joinToString("") { "%02x".format(it) }

                val metaJson = JSONObject()
                    .put("file_name", doc.fileName)
                    .put("file_size", doc.fileSize)
                    .put("page_count", doc.pageCount)
                    .put("report_date", doc.reportDate ?: doc.importDate)
                    .put("stable_id", doc.stableId)

                val encMeta = EncryptedPayloadEnvelope.encrypt(
                    metaJson.toString().toByteArray(Charsets.UTF_8),
                    key,
                    recordMetadataAad(targetProfileId)
                )
                val encMetaB64 = android.util.Base64.encodeToString(encMeta, android.util.Base64.NO_WRAP)

                val cloudRecordId = accountFamilyRepo.createCloudRecord(
                    profileId = targetProfileId,
                    encryptedMetadataBase64 = encMetaB64,
                    ciphertextSha256 = sha256Hex,
                    ciphertextSize = file.length(),
                    cryptoVersion = 1
                )
                val ok = accountFamilyRepo.uploadRecordCiphertext(cloudRecordId, file, sha256Hex)
                if (ok) uploaded++
            } catch (err: Exception) {
                android.util.Log.d("DocumentRepository", "Cloud upload skipped for ${doc.fileName}: ${err.message}")
            }
        }

        // 2. Download cloud records
        try {
            val cloudRecords = accountFamilyRepo.listCloudRecords(targetProfileId)
            for (cloudRec in cloudRecords) {
                val recordId = cloudRec.getString("record_id")
                val encMetaB64 = cloudRec.getString("encrypted_metadata")
                val encMetaBytes = android.util.Base64.decode(encMetaB64, android.util.Base64.NO_WRAP)

                val metaJson = try {
                    val decrypted = EncryptedPayloadEnvelope.decrypt(encMetaBytes, key, recordMetadataAad(targetProfileId))
                    JSONObject(decrypted.toString(Charsets.UTF_8))
                } catch (e: Exception) {
                    null
                }

                val stableId = metaJson?.optString("stable_id")?.takeIf(String::isNotBlank) ?: "cloud-$recordId"
                if (documentDao.getDocumentByStableId(stableId) != null) continue

                val fileName = metaJson?.optString("file_name")?.takeIf(String::isNotBlank) ?: "cloud-report-$recordId.pdf"
                val timelineFolder = SimpleDateFormat("yyyy/MM", Locale.ROOT).format(Date())
                val profileFolder = UUID.nameUUIDFromBytes(targetProfileId.toByteArray(Charsets.UTF_8)).toString()
                val recordDir = File(vaultDir, "profiles/$profileFolder/$timelineFolder").apply { mkdirs() }
                val encryptedFile = File(recordDir, "record_$stableId.swai")

                val downloadedOk = accountFamilyRepo.downloadRecordCiphertext(recordId, encryptedFile)
                if (downloadedOk && encryptedFile.isFile && encryptedFile.length() > 0L) {
                    val tempPlain = File(context.cacheDir, "temp_cloud_$stableId.pdf")
                    try {
                        EncryptedPayloadEnvelope.decryptFile(encryptedFile, tempPlain, key, recordAad(targetProfileId, stableId))
                        val result = processAndIndexFile(
                            plaintextInput = tempPlain,
                            displayFileName = fileName,
                            profileId = targetProfileId,
                            customStableId = stableId,
                            existingEncryptedFile = encryptedFile
                        )
                        if (result.isSuccess) downloaded++
                    } catch (e: Exception) {
                        errors.add("Failed to decrypt and index cloud record $recordId: ${e.message}")
                    } finally {
                        tempPlain.delete()
                    }
                }
            }
        } catch (e: Exception) {
            errors.add("Failed to list cloud records: ${e.message}")
        }

        SyncSummary(uploaded, downloaded, errors)
    }

    private fun recordMetadataAad(profileId: String) =
        "swasthai.record_metadata.v1:$profileId".toByteArray(Charsets.UTF_8)

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
