package com.swasthai.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.swasthai.app.data.local.entity.ConversationEntity
import com.swasthai.app.data.local.entity.ChatTurnEntity
import com.swasthai.app.data.local.entity.MessageEntity
import com.swasthai.app.data.local.entity.DEFAULT_PERSON_PROFILE_ID
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations WHERE profileId = :profileId ORDER BY updatedAt DESC")
    fun getAllConversations(profileId: String = DEFAULT_PERSON_PROFILE_ID): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id AND profileId = :profileId LIMIT 1")
    suspend fun getConversationById(id: Long, profileId: String = DEFAULT_PERSON_PROFILE_ID): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET updatedAt = :timestamp WHERE id = :id AND profileId = :profileId")
    suspend fun updateConversationTimestamp(id: Long, timestamp: Long, profileId: String = DEFAULT_PERSON_PROFILE_ID)

    @Query("UPDATE conversations SET profileId = :profileId, scopeMode = :scopeMode, selectedDocIdsJson = :selectedDocIdsJson, timeRangeFilter = :timeRangeFilter, customStartDate = :customStartDate, customEndDate = :customEndDate, includeStructuredRecords = :includeStructuredRecords, responseLanguage = :responseLanguage WHERE id = :id")
    suspend fun updateConversationScope(
        id: Long,
        profileId: String,
        scopeMode: String,
        selectedDocIdsJson: String,
        timeRangeFilter: String,
        customStartDate: Long?,
        customEndDate: Long?,
        includeStructuredRecords: Boolean,
        responseLanguage: String
    )

    @Query("DELETE FROM conversations WHERE id = :id AND profileId = :profileId")
    suspend fun deleteConversationById(id: Long, profileId: String = DEFAULT_PERSON_PROFILE_ID)

    @Query("SELECT m.* FROM messages m JOIN conversations c ON c.id = m.conversationId WHERE m.conversationId = :conversationId AND c.profileId = :profileId ORDER BY m.timestamp ASC")
    fun getMessagesForConversation(conversationId: Long, profileId: String = DEFAULT_PERSON_PROFILE_ID): Flow<List<MessageEntity>>

    @Query("SELECT m.* FROM messages m JOIN conversations c ON c.id = m.conversationId WHERE m.conversationId = :conversationId AND c.profileId = :profileId ORDER BY m.timestamp ASC")
    suspend fun getMessagesList(conversationId: Long, profileId: String = DEFAULT_PERSON_PROFILE_ID): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTurn(turn: ChatTurnEntity)

    @Update
    suspend fun updateTurn(turn: ChatTurnEntity)

    @Query("SELECT * FROM chat_turns WHERE id = :turnId LIMIT 1")
    suspend fun getTurn(turnId: String): ChatTurnEntity?

    @Transaction
    suspend fun beginTurn(userMessage: MessageEntity, turn: ChatTurnEntity): Long {
        val userMessageId = insertMessage(userMessage)
        insertTurn(turn.copy(userMessageId = userMessageId))
        return userMessageId
    }

    @Transaction
    suspend fun completeTurn(
        assistantMessage: MessageEntity,
        status: String,
        errorMessage: String?,
        toolCallsJson: String = "[]"
    ): Long {
        val assistantMessageId = insertMessage(assistantMessage)
        val turnId = assistantMessage.turnId ?: return assistantMessageId
        getTurn(turnId)?.let { turn ->
            updateTurn(
                turn.copy(
                    assistantMessageId = assistantMessageId,
                    status = status,
                    citationsJson = assistantMessage.citationsJson,
                    errorMessage = errorMessage,
                    toolCallsJson = toolCallsJson,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return assistantMessageId
    }

    @Query("SELECT * FROM chat_turns WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getTurnsForConversation(conversationId: Long): Flow<List<ChatTurnEntity>>

    @Query("UPDATE chat_turns SET status = 'INTERRUPTED', errorMessage = 'App closed before the response completed', updatedAt = :timestamp WHERE status IN ('PREPARING', 'RETRIEVING', 'GENERATING') AND createdAt < :processStartedAt")
    suspend fun interruptUnfinishedTurns(timestamp: Long, processStartedAt: Long)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: Long)
}
