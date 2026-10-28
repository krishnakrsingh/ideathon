package com.swasthai.app.domain.model

enum class IndexingStatus {
    PENDING,
    EXTRACTED,
    INDEXED,
    PARTIAL,
    FAILED
}

data class MedicalDocument(
    val id: Long = 0,
    val fileName: String,
    val localFilePath: String,
    val fileSize: Long,
    val importDate: Long = System.currentTimeMillis(),
    val reportDate: Long? = null,
    val indexingStatus: IndexingStatus = IndexingStatus.PENDING,
    val pageCount: Int = 1,
    val failureReason: String? = null
)

data class DocumentChunk(
    val id: Long = 0,
    val documentId: Long,
    val chunkIndex: Int,
    val pageNumber: Int,
    val text: String,
    val tokenCount: Int = 0
)
