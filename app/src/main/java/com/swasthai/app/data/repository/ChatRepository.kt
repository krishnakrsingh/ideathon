package com.swasthai.app.data.repository

import com.swasthai.app.ai.LfmManager
import com.swasthai.app.ai.RAGPipeline
import com.swasthai.app.data.local.dao.ConversationDao
import com.swasthai.app.data.local.entity.ConversationEntity
import com.swasthai.app.data.local.entity.MessageEntity
import com.swasthai.app.domain.model.ChatContextScope
import com.swasthai.app.domain.model.ChatMessage
import com.swasthai.app.domain.model.Conversation
import com.swasthai.app.domain.model.MessageRole
import com.swasthai.app.domain.model.ModelInferenceStatus
import com.swasthai.app.domain.model.ResponseLanguage
import com.swasthai.app.domain.model.ScopeMode
import com.swasthai.app.domain.model.SourceCitation
import com.swasthai.app.domain.model.TimeRangeFilter
import com.swasthai.app.domain.safety.EmergencyTriage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val ragPipeline: RAGPipeline,
    private val lfmManager: LfmManager,
    private val measurementDao: com.swasthai.app.data.local.dao.MeasurementDao? = null,
    private val documentDao: com.swasthai.app.data.local.dao.DocumentDao? = null
) {

    fun getConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getMessages(conversationId: Long): Flow<List<ChatMessage>> {
        return conversationDao.getMessagesForConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getConversationById(id: Long): Conversation? = withContext(Dispatchers.IO) {
        conversationDao.getConversationById(id)?.toDomain()
    }

    suspend fun createConversation(
        title: String,
        activeDocumentId: Long? = null,
        scope: ChatContextScope = ChatContextScope()
    ): Long = withContext(Dispatchers.IO) {
        val selectedIdsJson = JSONArray(scope.selectedDocumentIds.toList()).toString()
        conversationDao.insertConversation(
            ConversationEntity(
                title = title,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                activeDocumentId = activeDocumentId,
                scopeMode = scope.mode.name,
                selectedDocIdsJson = selectedIdsJson,
                timeRangeFilter = scope.timeRange.name,
                customStartDate = scope.customStartDate,
                customEndDate = scope.customEndDate,
                includeStructuredRecords = scope.includeStructuredVitals,
                responseLanguage = scope.responseLanguage.name
            )
        )
    }

    suspend fun updateConversationScope(
        conversationId: Long,
        scope: ChatContextScope
    ) = withContext(Dispatchers.IO) {
        val selectedIdsJson = JSONArray(scope.selectedDocumentIds.toList()).toString()
        conversationDao.updateConversationScope(
            id = conversationId,
            scopeMode = scope.mode.name,
            selectedDocIdsJson = selectedIdsJson,
            timeRangeFilter = scope.timeRange.name,
            customStartDate = scope.customStartDate,
            customEndDate = scope.customEndDate,
            includeStructuredRecords = scope.includeStructuredVitals,
            responseLanguage = scope.responseLanguage.name
        )
    }

    suspend fun deleteConversation(id: Long) = withContext(Dispatchers.IO) {
        conversationDao.deleteConversationById(id)
    }

    /**
     * Determines if a query is a conversational follow-up referencing prior context.
     */
    private fun isFollowUpQuery(query: String): Boolean {
        val lower = query.lowercase(Locale.US).trim()
        val followUpTriggers = listOf(
            "what about", "how about", "what of", "why", "and", "is that", "does that",
            "previous", "yesterday", "that report", "the report", "this report",
            "haemoglobin", "hemoglobin", "blood report", "my result", "those results",
            "aur", "kya", "iska", "uska", "ye", "yeh", "report", "lab", "image", "photo",
            "और", "क्या", "इसका", "उसका", "ये", "रिपोर्ट", "नतीजा"
        )
        return followUpTriggers.any { lower.contains(it) } || lower.split(" ").size <= 5
    }

    /**
     * Sends a user message, performs scoped RAG retrieval, evaluates emergency triage,
     * resolves conversational references, and streams an evidence-grounded response.
     * Guarantees zero "null" prefixes.
     */
    fun sendMessage(
        conversationId: Long,
        userQuery: String,
        activeDocId: Long? = null,
        overrideScope: ChatContextScope? = null,
        imageUri: String? = null
    ): Flow<ChatMessage> = flow {
        // Multi-turn prior history (captured before inserting current turn)
        val priorHistory = conversationDao.getMessagesList(conversationId).map { it.toDomain() }

        // 1. On-device OCR extraction if an image is attached
        var imageOcrText = ""
        if (imageUri != null && lfmManager.appContext != null) {
            try {
                imageOcrText = com.swasthai.app.ai.ImageTextExtractor.extractText(
                    context = lfmManager.appContext!!,
                    imageUri = android.net.Uri.parse(imageUri)
                )
            } catch (e: Throwable) {
                android.util.Log.e("ChatRepository", "OCR extraction error: ${e.message}")
            }
        }

        val effectiveQuery = buildString {
            if (userQuery.isNotBlank()) {
                append(userQuery)
            }
            if (imageOcrText.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append("--- ATTACHED PATIENT CLINICAL IMAGE / REPORT (ON-DEVICE OCR) ---\n")
                append(imageOcrText)
                append("\n--------------------------------------------------------------")
            } else if (imageUri != null) {
                if (isNotEmpty()) append("\n\n")
                append("[Patient attached a clinical image/photo for evaluation. Please review observed details, differential clinical insights, red flags, and recommended next steps.]")
            }
            if (isEmpty()) {
                append("Please analyze and explain this attached medical record/clinical image in detail.")
            }
        }

        // 2. Persist User Message
        val userMsgEntity = MessageEntity(
            conversationId = conversationId,
            role = MessageRole.USER.name,
            content = if (userQuery.isNotBlank()) userQuery else if (imageOcrText.isNotBlank()) "Attached Medical Report" else "Attached Medical Image",
            timestamp = System.currentTimeMillis(),
            imageUri = imageUri
        )
        conversationDao.insertMessage(userMsgEntity)

        // 3. Load Conversation Scope
        val convoEntity = conversationDao.getConversationById(conversationId)
        val activeScope = overrideScope ?: convoEntity?.toDomain()?.scope ?: ChatContextScope()

        // 4. Multi-turn context and reference resolution
        val priorUserTurns = priorHistory.filter { it.role == MessageRole.USER }
        val retrievalQuery = if (isFollowUpQuery(userQuery) && priorUserTurns.isNotEmpty()) {
            "${priorUserTurns.last().content} $userQuery"
        } else {
            userQuery.ifBlank { imageOcrText.take(100) }
        }

        // 4. Deterministic Emergency Triage Check
        val triage = EmergencyTriage.evaluate(userQuery)
        val emergencyNotice = if (triage.isEmergency) triage.emergencyNotice else null

        // 5. Scoped RAG Retrieval (respects scopeMode, selectedDocs, timeRange, vitals)
        val allHistoryBiomarkers = if (activeScope.includeStructuredVitals) {
            try {
                measurementDao?.getAllMeasurementsList()?.map { m ->
                    val refStr = if (m.referenceLow != null && m.referenceHigh != null) {
                        " (Normal: ${m.referenceLow} - ${m.referenceHigh})"
                    } else ""
                    "${m.biomarkerName}: ${m.numericValue} ${m.unit}$refStr"
                } ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val retrievalResult = ragPipeline.retrieveRelevantContext(
            query = retrievalQuery,
            targetDocumentId = activeDocId,
            topK = 3,
            scope = activeScope
        )
        val citations = retrievalResult.citations

        // 6. Initialize clean assistant response buffer (Regression Defense: never "null")
        val responseBuffer = StringBuilder()
        if (emergencyNotice != null) {
            responseBuffer.append("$emergencyNotice\n\n")
        }

        val initialAssistantMsg = ChatMessage(
            conversationId = conversationId,
            role = MessageRole.ASSISTANT,
            content = responseBuffer.toString(),
            timestamp = System.currentTimeMillis(),
            citations = citations,
            isStreaming = true
        )
        emit(initialAssistantMsg)

        // 7. Check if generative model is installed and ready
        if (lfmManager.status == ModelInferenceStatus.READY) {
            try {
                lfmManager.generateStream(
                    currentQuery = effectiveQuery,
                    history = priorHistory,
                    citations = citations,
                    allHistoryMeasurements = allHistoryBiomarkers,
                    responseLanguage = activeScope.responseLanguage
                ).collect { token ->
                    // Regression defense against any null tokens
                    var cleanToken = token
                    if (responseBuffer.isEmpty() && cleanToken.startsWith("null")) {
                        cleanToken = cleanToken.removePrefix("null")
                    }
                    if (cleanToken.isNotEmpty() && cleanToken != "null") {
                        responseBuffer.append(cleanToken)
                        emit(
                            initialAssistantMsg.copy(
                                content = responseBuffer.toString(),
                                isStreaming = true
                            )
                        )
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.e("ChatRepository", "Inference error: ${e.message}", e)
                responseBuffer.append("\n\n[Inference Error: ${e.message}]")
                emit(
                    initialAssistantMsg.copy(
                        content = responseBuffer.toString(),
                        isStreaming = false
                    )
                )
            }
        } else {
            val notInstalledMsg = buildString {
                append("⚠️ **Model Engine Initializing or Artifact Not Found**\n\n")
                append("SwasthAI runs 100% on-device using **Gemma 3 1B / Gemma 4** (~75 tok/sec with GPU acceleration).\n\n")
                append("Please ensure `${LfmManager.PREFERRED_GENERATIVE_MODEL}` is in your Downloads folder (`/sdcard/Download/`).")
            }
            responseBuffer.append(notInstalledMsg)
            emit(
                initialAssistantMsg.copy(
                    content = responseBuffer.toString(),
                    isStreaming = false
                )
            )
        }

        // 8. Persist completed response (Never storing "null" prefixes)
        val finalContent = if (responseBuffer.startsWith("null")) {
            responseBuffer.substring(4)
        } else {
            responseBuffer.toString()
        }

        val finalEntity = MessageEntity(
            conversationId = conversationId,
            role = MessageRole.ASSISTANT.name,
            content = finalContent,
            timestamp = System.currentTimeMillis(),
            citationsJson = citationsToJson(citations)
        )
        val savedId = conversationDao.insertMessage(finalEntity)

        conversationDao.updateConversationTimestamp(conversationId, System.currentTimeMillis())

        emit(finalEntity.copy(id = savedId).toDomain().copy(isStreaming = false))
    }

    private fun citationsToJson(citations: List<SourceCitation>): String {
        val array = JSONArray()
        for (c in citations) {
            val obj = JSONObject()
            obj.put("documentId", c.documentId)
            obj.put("documentName", c.documentName)
            obj.put("pageNumber", c.pageNumber)
            obj.put("chunkText", c.chunkText)
            obj.put("similarityScore", c.similarityScore.toDouble())
            array.put(obj)
        }
        return array.toString()
    }

    private fun jsonToCitations(json: String): List<SourceCitation> {
        val list = mutableListOf<SourceCitation>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SourceCitation(
                        documentId = obj.getLong("documentId"),
                        documentName = obj.getString("documentName"),
                        pageNumber = obj.getInt("pageNumber"),
                        chunkText = obj.getString("chunkText"),
                        similarityScore = obj.optDouble("similarityScore", 0.0).toFloat()
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun MessageEntity.toDomain(): ChatMessage {
        val cleanContent = if (content.startsWith("null")) content.removePrefix("null") else content
        return ChatMessage(
            id = id,
            conversationId = conversationId,
            role = MessageRole.valueOf(role),
            content = cleanContent,
            timestamp = timestamp,
            citations = jsonToCitations(citationsJson),
            isStreaming = false,
            imageUri = imageUri
        )
    }

    private fun ConversationEntity.toDomain(): Conversation {
        val docIds = mutableSetOf<Long>()
        try {
            val arr = JSONArray(selectedDocIdsJson)
            for (i in 0 until arr.length()) {
                docIds.add(arr.getLong(i))
            }
        } catch (_: Exception) {}

        val scope = ChatContextScope(
            mode = try { ScopeMode.valueOf(scopeMode) } catch (_: Exception) { ScopeMode.ALL_HISTORY },
            selectedDocumentIds = docIds,
            timeRange = try { TimeRangeFilter.valueOf(timeRangeFilter) } catch (_: Exception) { TimeRangeFilter.ALL_TIME },
            customStartDate = customStartDate,
            customEndDate = customEndDate,
            includeStructuredVitals = includeStructuredRecords,
            responseLanguage = try { ResponseLanguage.valueOf(responseLanguage) } catch (_: Exception) { ResponseLanguage.AUTO }
        )

        return Conversation(
            id = id,
            title = title,
            createdAt = createdAt,
            updatedAt = updatedAt,
            activeDocumentId = activeDocumentId,
            scope = scope
        )
    }
}
