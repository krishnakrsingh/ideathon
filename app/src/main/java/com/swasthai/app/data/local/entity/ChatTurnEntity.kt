package com.swasthai.app.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.swasthai.app.data.local.entity.DEFAULT_PERSON_PROFILE_ID

/** Durable snapshot and lifecycle for one user submission and its assistant response. */
@Entity(
    tableName = "chat_turns",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId"), Index("createdAt"), Index("status")]
)
data class ChatTurnEntity(
    @PrimaryKey val id: String,
    val conversationId: Long,
    val userMessageId: Long,
    val assistantMessageId: Long? = null,
    val activeDocumentId: Long? = null,
    val scopeMode: String,
    val selectedDocIdsJson: String,
    val timeRangeFilter: String,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val includeStructuredRecords: Boolean,
    val responseLanguage: String,
    val status: String = "PREPARING",
    val retrievalMode: String? = null,
    val modelName: String? = null,
    @ColumnInfo(defaultValue = "'[]'") val toolCallsJson: String = "[]",
    val profileId: String = DEFAULT_PERSON_PROFILE_ID,
    val citationsJson: String = "[]",
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
