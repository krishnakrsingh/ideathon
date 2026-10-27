package com.swasthai.app.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import com.swasthai.app.data.local.entity.ChunkEmbeddingEntity
import com.swasthai.app.data.local.entity.DocumentChunkEntity
import com.swasthai.app.data.local.entity.DocumentEntity

data class ChunkWithDocument(
    @Embedded val chunk: DocumentChunkEntity,
    @Relation(
        parentColumn = "documentId",
        entityColumn = "id"
    )
    val document: DocumentEntity
)

@Dao
interface RAGDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbeddings(embeddings: List<ChunkEmbeddingEntity>)

    @Query("SELECT * FROM chunk_embeddings")
    suspend fun getAllEmbeddings(): List<ChunkEmbeddingEntity>

    @Query("SELECT * FROM chunk_embeddings WHERE documentId = :documentId")
    suspend fun getEmbeddingsForDocument(documentId: Long): List<ChunkEmbeddingEntity>

    @Query("SELECT * FROM chunk_embeddings WHERE documentId IN (:documentIds)")
    suspend fun getEmbeddingsForDocuments(documentIds: List<Long>): List<ChunkEmbeddingEntity>

    @androidx.room.Transaction
    @Query("SELECT * FROM document_chunks WHERE id = :chunkId LIMIT 1")
    suspend fun getChunkWithDocument(chunkId: Long): ChunkWithDocument?

    @androidx.room.Transaction
    @Query("SELECT * FROM document_chunks WHERE id IN (:chunkIds)")
    suspend fun getChunksWithDocuments(chunkIds: List<Long>): List<ChunkWithDocument>

    @Query("DELETE FROM chunk_embeddings WHERE documentId = :documentId")
    suspend fun deleteEmbeddingsForDocument(documentId: Long)
}
