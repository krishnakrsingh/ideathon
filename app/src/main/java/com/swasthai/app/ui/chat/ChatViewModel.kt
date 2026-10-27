package com.swasthai.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.ai.LfmManager
import com.swasthai.app.data.repository.ChatRepository
import com.swasthai.app.data.repository.DocumentRepository
import com.swasthai.app.domain.model.ChatContextScope
import com.swasthai.app.domain.model.ChatMessage
import com.swasthai.app.domain.model.Conversation
import com.swasthai.app.domain.model.MedicalDocument
import com.swasthai.app.domain.model.MessageRole
import com.swasthai.app.domain.model.ModelInferenceStatus
import com.swasthai.app.domain.model.ResponseLanguage
import com.swasthai.app.domain.model.ScopeMode
import com.swasthai.app.domain.model.TimeRangeFilter
import com.swasthai.app.domain.safety.EmergencyTriage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val currentConversationId: Long? = null,
    val currentMessages: List<ChatMessage> = emptyList(),
    val availableDocuments: List<MedicalDocument> = emptyList(),
    val activeDocumentId: Long? = null,
    val currentScope: ChatContextScope = ChatContextScope(),
    val isContextSheetOpen: Boolean = false,
    val inputText: String = "",
    val isStreaming: Boolean = false,
    val modelStatus: ModelInferenceStatus = ModelInferenceStatus.NOT_INSTALLED,
    val emergencyWarning: String? = null,
    val selectedImageUri: String? = null
) {
    val medGemmaStatus: ModelInferenceStatus get() = modelStatus
}

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val documentRepository: DocumentRepository,
    private val lfmManager: LfmManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
        loadDocuments()
        updateModelStatus()
    }

    fun updateModelStatus() {
        _uiState.value = _uiState.value.copy(
            modelStatus = lfmManager.status
        )
    }

    private fun loadConversations() {
        viewModelScope.launch {
            chatRepository.getConversations().collect { convos ->
                _uiState.value = _uiState.value.copy(conversations = convos)
                if (_uiState.value.currentConversationId == null && convos.isNotEmpty()) {
                    selectConversation(convos.first().id)
                }
            }
        }
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            documentRepository.getAllDocuments().collect { docs ->
                _uiState.value = _uiState.value.copy(availableDocuments = docs)
            }
        }
    }

    fun selectConversation(id: Long) {
        _uiState.value = _uiState.value.copy(currentConversationId = id)
        viewModelScope.launch {
            val convo = chatRepository.getConversationById(id)
            if (convo != null) {
                _uiState.value = _uiState.value.copy(currentScope = convo.scope)
            }

            chatRepository.getMessages(id).collect { messages ->
                if (!_uiState.value.isStreaming) {
                    _uiState.value = _uiState.value.copy(currentMessages = messages)
                } else {
                    val streamingAssistant = _uiState.value.currentMessages.find { it.isStreaming }
                    if (streamingAssistant != null && !messages.any { it.id == streamingAssistant.id }) {
                        _uiState.value = _uiState.value.copy(currentMessages = messages + streamingAssistant)
                    } else {
                        _uiState.value = _uiState.value.copy(currentMessages = messages)
                    }
                }
            }
        }
    }

    fun openContextSheet(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isContextSheetOpen = isOpen)
    }

    fun updateScope(newScope: ChatContextScope) {
        _uiState.value = _uiState.value.copy(currentScope = newScope)
        val currentConvoId = _uiState.value.currentConversationId ?: return
        viewModelScope.launch {
            chatRepository.updateConversationScope(currentConvoId, newScope)
        }
    }

    fun setScopeMode(mode: ScopeMode) {
        updateScope(_uiState.value.currentScope.copy(mode = mode))
    }

    fun toggleDocumentSelection(docId: Long) {
        val current = _uiState.value.currentScope.selectedDocumentIds.toMutableSet()
        if (current.contains(docId)) {
            current.remove(docId)
        } else {
            current.add(docId)
        }
        updateScope(
            _uiState.value.currentScope.copy(
                mode = ScopeMode.SPECIFIC_DOCS,
                selectedDocumentIds = current
            )
        )
    }

    fun setTimeRange(range: TimeRangeFilter) {
        updateScope(_uiState.value.currentScope.copy(timeRange = range))
    }

    fun toggleStructuredVitals() {
        val current = _uiState.value.currentScope.includeStructuredVitals
        updateScope(_uiState.value.currentScope.copy(includeStructuredVitals = !current))
    }

    fun setResponseLanguage(language: ResponseLanguage) {
        updateScope(_uiState.value.currentScope.copy(responseLanguage = language))
    }

    fun createNewConversation(title: String = "New Consultation") {
        viewModelScope.launch {
            val newId = chatRepository.createConversation(
                title = title,
                activeDocumentId = _uiState.value.activeDocumentId,
                scope = _uiState.value.currentScope
            )
            selectConversation(newId)
        }
    }

    fun deleteCurrentConversation() {
        val currentId = _uiState.value.currentConversationId ?: return
        viewModelScope.launch {
            chatRepository.deleteConversation(currentId)
            val remaining = _uiState.value.conversations.filter { it.id != currentId }
            if (remaining.isNotEmpty()) {
                selectConversation(remaining.first().id)
            } else {
                _uiState.value = _uiState.value.copy(
                    currentConversationId = null,
                    currentMessages = emptyList()
                )
            }
        }
    }

    fun setActiveDocument(documentId: Long?) {
        _uiState.value = _uiState.value.copy(activeDocumentId = documentId)
        if (documentId != null) {
            updateScope(
                _uiState.value.currentScope.copy(
                    mode = ScopeMode.SPECIFIC_DOCS,
                    selectedDocumentIds = setOf(documentId)
                )
            )
        }
    }

    fun setInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
        val triage = EmergencyTriage.evaluate(text)
        _uiState.value = _uiState.value.copy(
            emergencyWarning = if (triage.isEmergency) triage.emergencyNotice else null
        )
    }

    fun selectImage(uri: String?) {
        _uiState.value = _uiState.value.copy(selectedImageUri = uri)
    }

    fun clearSelectedImage() {
        _uiState.value = _uiState.value.copy(selectedImageUri = null)
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        val imageUri = _uiState.value.selectedImageUri
        if (text.isBlank() && imageUri == null) return

        val titleText = if (text.isNotBlank()) text.take(30) else "Medical Image Consultation"
        val convoId = _uiState.value.currentConversationId
        if (convoId == null) {
            viewModelScope.launch {
                val newId = chatRepository.createConversation(
                    title = titleText,
                    activeDocumentId = _uiState.value.activeDocumentId,
                    scope = _uiState.value.currentScope
                )
                selectConversation(newId)
                executeSend(newId, text, imageUri)
            }
        } else {
            executeSend(convoId, text, imageUri)
        }
    }

    private fun executeSend(conversationId: Long, text: String, imageUri: String?) {
        _uiState.value = _uiState.value.copy(
            inputText = "",
            selectedImageUri = null,
            isStreaming = true
        )

        viewModelScope.launch {
            chatRepository.sendMessage(
                conversationId = conversationId,
                userQuery = text,
                activeDocId = _uiState.value.activeDocumentId,
                overrideScope = _uiState.value.currentScope,
                imageUri = imageUri
            ).collect { msg ->
                val currentList = _uiState.value.currentMessages.toMutableList()
                val existingIndex = currentList.indexOfFirst {
                    (msg.id > 0 && it.id == msg.id) || (it.role == MessageRole.ASSISTANT && it.isStreaming)
                }
                if (existingIndex != -1) {
                    currentList[existingIndex] = msg
                } else {
                    currentList.add(msg)
                }

                _uiState.value = _uiState.value.copy(
                    currentMessages = currentList,
                    isStreaming = msg.isStreaming,
                    modelStatus = lfmManager.status
                )
            }
        }
    }
}
