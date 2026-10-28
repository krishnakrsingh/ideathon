package com.swasthai.app.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "documents", indices = [Index(value = ["stableId"], unique = true), Index(value = ["profileId", "importDate"])])
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val localFilePath: String,
    val fileSize: Long,
    val importDate: Long = System.currentTimeMillis(),
    val reportDate: Long? = null,
    val indexingStatus: String,
    val pageCount: Int = 1,
    val failureReason: String? = null,
    @ColumnInfo(defaultValue = "''") val stableId: String = "",
    @ColumnInfo(defaultValue = "0") val encryptionContextVersion: Int = 0,
    @ColumnInfo(defaultValue = "'local-default-profile'") val profileId: String = DEFAULT_PERSON_PROFILE_ID
)

@Entity(
    tableName = "document_chunks",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId", "pageNumber", "chunkIndex"])]
)
data class DocumentChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val chunkIndex: Int,
    val pageNumber: Int,
    val text: String,
    val tokenCount: Int = 0
)

@Entity(
    tableName = "chunk_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = DocumentChunkEntity::class,
            parentColumns = ["id"],
            childColumns = ["chunkId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"])]
)
data class ChunkEmbeddingEntity(
    @PrimaryKey val chunkId: Long,
    val documentId: Long,
    val dimension: Int,
    val embeddingBlob: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ChunkEmbeddingEntity
        return chunkId == other.chunkId
    }

    override fun hashCode(): Int = chunkId.hashCode()
}
