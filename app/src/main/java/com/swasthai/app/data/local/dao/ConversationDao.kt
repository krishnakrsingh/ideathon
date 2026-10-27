package com.swasthai.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.swasthai.app.data.local.entity.ConversationEntity
import com.swasthai.app.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: Long): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET updatedAt = :timestamp WHERE id = :id")
    suspend fun updateConversationTimestamp(id: Long, timestamp: Long)

    @Query("UPDATE conversations SET scopeMode = :scopeMode, selectedDocIdsJson = :selectedDocIdsJson, timeRangeFilter = :timeRangeFilter, customStartDate = :customStartDate, customEndDate = :customEndDate, includeStructuredRecords = :includeStructuredRecords, responseLanguage = :responseLanguage WHERE id = :id")
    suspend fun updateConversationScope(
        id: Long,
        scopeMode: String,
        selectedDocIdsJson: String,
        timeRangeFilter: String,
        customStartDate: Long?,
        customEndDate: Long?,
        includeStructuredRecords: Boolean,
        responseLanguage: String
    )

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesList(conversationId: Long): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: Long)
}
