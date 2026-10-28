package com.swasthai.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations", indices = [Index(value = ["profileId"])])
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val activeDocumentId: Long? = null,
    val scopeMode: String = "ALL_HISTORY",
    val selectedDocIdsJson: String = "[]",
    val timeRangeFilter: String = "ALL_TIME",
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val includeStructuredRecords: Boolean = true,
    val responseLanguage: String = "AUTO",
    val profileId: String = DEFAULT_PERSON_PROFILE_ID
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val citationsJson: String = "[]",
    val imageUri: String? = null,
    val turnId: String? = null
)
