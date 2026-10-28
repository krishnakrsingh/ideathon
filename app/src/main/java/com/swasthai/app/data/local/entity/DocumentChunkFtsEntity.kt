package com.swasthai.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/** External-content index for report text; Room keeps it synchronized with document_chunks. */
@Fts4(contentEntity = DocumentChunkEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "document_chunks_fts")
data class DocumentChunkFtsEntity(
    @ColumnInfo(name = "text") val text: String
)
