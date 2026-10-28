package com.swasthai.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.swasthai.app.data.local.entity.DocumentChunkEntity
import com.swasthai.app.data.local.entity.DocumentEntity
import com.swasthai.app.data.local.entity.DEFAULT_PERSON_PROFILE_ID
import kotlinx.coroutines.flow.Flow

data class RankedChunkCandidate(
    @androidx.room.Embedded val chunk: DocumentChunkEntity,
    val matchInfo: ByteArray
)

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents WHERE profileId = :profileId ORDER BY importDate DESC")
    fun getAllDocuments(profileId: String = DEFAULT_PERSON_PROFILE_ID): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE profileId = :profileId ORDER BY importDate DESC")
    suspend fun getAllDocumentsList(profileId: String = DEFAULT_PERSON_PROFILE_ID): List<DocumentEntity>

    @Query("""SELECT * FROM documents
        WHERE profileId = :profileId
          AND (:targetDocumentId IS NULL OR id = :targetDocumentId)
          AND (:allHistory = 1 OR id IN (:selectedDocumentIds))
          AND COALESCE(reportDate, importDate) BETWEEN :startDate AND :endDate
        ORDER BY importDate DESC
        LIMIT :limit OFFSET :offset""")
    suspend fun getTimelinePage(
        profileId: String,
        allHistory: Boolean,
        selectedDocumentIds: List<Long>,
        targetDocumentId: Long?,
        startDate: Long,
        endDate: Long,
        limit: Int,
        offset: Int
    ): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE id = :id AND profileId = :profileId LIMIT 1")
    suspend fun getDocumentById(id: Long, profileId: String = DEFAULT_PERSON_PROFILE_ID): DocumentEntity?

    @Query("SELECT * FROM documents WHERE stableId = :stableId LIMIT 1")
    suspend fun getDocumentByStableId(stableId: String): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: DocumentEntity): Long

    @Update
    suspend fun updateDocument(doc: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id AND profileId = :profileId")
    suspend fun deleteDocumentById(id: Long, profileId: String = DEFAULT_PERSON_PROFILE_ID)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<DocumentChunkEntity>): List<Long>

    @Query("SELECT * FROM document_chunks WHERE documentId = :docId ORDER BY chunkIndex ASC")
    suspend fun getChunksForDocument(docId: Long): List<DocumentChunkEntity>

    @Query("SELECT * FROM document_chunks WHERE documentId = :docId AND (:pageNumber <= 0 OR pageNumber = :pageNumber) ORDER BY chunkIndex ASC LIMIT :limit OFFSET :offset")
    suspend fun getChunksForDocumentPage(docId: Long, pageNumber: Int, limit: Int, offset: Int): List<DocumentChunkEntity>

    @Query("SELECT * FROM document_chunks WHERE id = :chunkId LIMIT 1")
    suspend fun getChunkById(chunkId: Long): DocumentChunkEntity?

    @Query("SELECT * FROM document_chunks WHERE text LIKE '%' || :query || '%' ORDER BY id ASC LIMIT :limit")
    suspend fun searchChunksByKeyword(query: String, limit: Int = 20): List<DocumentChunkEntity>

    @Query("SELECT * FROM document_chunks WHERE documentId = :docId AND text LIKE '%' || :query || '%' ORDER BY id ASC LIMIT :limit")
    suspend fun searchChunksInDocumentByKeyword(docId: Long, query: String, limit: Int = 20): List<DocumentChunkEntity>

    @Query("SELECT document_chunks.*, matchinfo(document_chunks_fts, 'pcx') AS matchInfo FROM document_chunks JOIN document_chunks_fts ON document_chunks.id = document_chunks_fts.rowid WHERE document_chunks_fts MATCH :query AND document_chunks.documentId IN (:documentIds)")
    suspend fun searchChunksAcrossDocumentsByFullText(documentIds: List<Long>, query: String): List<RankedChunkCandidate>

    @Query("SELECT COUNT(*) FROM document_chunks WHERE documentId IN (:documentIds)")
    suspend fun getChunkCountForDocuments(documentIds: List<Long>): Int

    @Query("SELECT AVG(tokenCount) FROM document_chunks WHERE documentId IN (:documentIds)")
    suspend fun getAverageChunkLengthForDocuments(documentIds: List<Long>): Double?

    @Query("SELECT COUNT(*) FROM documents WHERE profileId = :profileId")
    suspend fun getDocumentCount(profileId: String = DEFAULT_PERSON_PROFILE_ID): Int
}
