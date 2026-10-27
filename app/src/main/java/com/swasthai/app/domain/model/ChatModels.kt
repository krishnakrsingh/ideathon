package com.swasthai.app.domain.model

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

enum class ModelInferenceStatus {
    NOT_INSTALLED,
    INITIALIZING,
    READY,
    GENERATING,
    ERROR
}

enum class ScopeMode {
    CONVERSATION_ONLY,
    ALL_HISTORY,
    SPECIFIC_DOCS
}

enum class TimeRangeFilter {
    ALL_TIME,
    LAST_30_DAYS,
    LAST_3_MONTHS,
    LAST_12_MONTHS,
    CUSTOM
}

enum class ResponseLanguage {
    AUTO,
    ENGLISH,
    HINDI
}

data class ChatContextScope(
    val mode: ScopeMode = ScopeMode.ALL_HISTORY,
    val selectedDocumentIds: Set<Long> = emptySet(),
    val timeRange: TimeRangeFilter = TimeRangeFilter.ALL_TIME,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val includeStructuredVitals: Boolean = true,
    val responseLanguage: ResponseLanguage = ResponseLanguage.AUTO
) {
    fun formatScopePill(availableDocuments: List<MedicalDocument> = emptyList()): String {
        val langSuffix = when (responseLanguage) {
            ResponseLanguage.HINDI -> " · Hindi"
            ResponseLanguage.ENGLISH -> " · EN"
            ResponseLanguage.AUTO -> ""
        }
        return when (mode) {
            ScopeMode.CONVERSATION_ONLY -> "Conversation Only$langSuffix"
            ScopeMode.ALL_HISTORY -> {
                val timeStr = when (timeRange) {
                    TimeRangeFilter.ALL_TIME -> "All Records"
                    TimeRangeFilter.LAST_30_DAYS -> "Last 30 Days"
                    TimeRangeFilter.LAST_3_MONTHS -> "Last 3 Months"
                    TimeRangeFilter.LAST_12_MONTHS -> "Last 12 Months"
                    TimeRangeFilter.CUSTOM -> "Custom Range"
                }
                "$timeStr${if (includeStructuredVitals) " + Vitals" else ""}$langSuffix"
            }
            ScopeMode.SPECIFIC_DOCS -> {
                val count = selectedDocumentIds.size
                val docLabel = if (count == 1) {
                    val doc = availableDocuments.find { it.id in selectedDocumentIds }
                    doc?.fileName?.take(18) ?: "1 report"
                } else {
                    "$count reports"
                }
                "Context: $docLabel$langSuffix"
            }
        }
    }
}

data class SourceCitation(
    val documentId: Long,
    val documentName: String,
    val pageNumber: Int,
    val chunkText: String,
    val similarityScore: Float = 0.0f
) {
    fun formatDisplay(): String = "[Source: $documentName, page $pageNumber]"
}

data class ChatMessage(
    val id: Long = 0,
    val conversationId: Long,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val citations: List<SourceCitation> = emptyList(),
    val isStreaming: Boolean = false,
    val imageUri: String? = null
)

data class Conversation(
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val activeDocumentId: Long? = null,
    val scope: ChatContextScope = ChatContextScope()
)
