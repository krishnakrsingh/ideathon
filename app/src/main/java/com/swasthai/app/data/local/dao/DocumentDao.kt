package com.swasthai.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.swasthai.app.data.local.entity.DocumentChunkEntity
import com.swasthai.app.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY importDate DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY importDate DESC")
    suspend fun getAllDocumentsList(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: DocumentEntity): Long

    @Update
    suspend fun updateDocument(doc: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<DocumentChunkEntity>): List<Long>

    @Query("SELECT * FROM document_chunks WHERE documentId = :docId ORDER BY chunkIndex ASC")
    suspend fun getChunksForDocument(docId: Long): List<DocumentChunkEntity>

    @Query("SELECT * FROM document_chunks WHERE id = :chunkId LIMIT 1")
    suspend fun getChunkById(chunkId: Long): DocumentChunkEntity?

    @Query("SELECT * FROM document_chunks WHERE text LIKE '%' || :query || '%' ORDER BY id ASC LIMIT :limit")
    suspend fun searchChunksByKeyword(query: String, limit: Int = 20): List<DocumentChunkEntity>

    @Query("SELECT * FROM document_chunks WHERE documentId = :docId AND text LIKE '%' || :query || '%' ORDER BY id ASC LIMIT :limit")
    suspend fun searchChunksInDocumentByKeyword(docId: Long, query: String, limit: Int = 20): List<DocumentChunkEntity>

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun getDocumentCount(): Int
}
