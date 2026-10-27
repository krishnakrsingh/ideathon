package com.swasthai.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.ai.LfmManager
import com.swasthai.app.data.repository.ChatRepository
import com.swasthai.app.data.repository.DocumentRepository
import com.swasthai.app.data.repository.HistoryRepository
import com.swasthai.app.domain.model.Conversation
import com.swasthai.app.domain.model.HealthTimelineItem
import com.swasthai.app.domain.model.MedicalDocument
import com.swasthai.app.domain.model.ModelInferenceStatus
import com.swasthai.app.domain.model.StructuredMeasurement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HealthWidgetData(
    val id: String,
    val title: String,
    val value: String,
    val unit: String,
    val statusText: String,
    val isNormal: Boolean,
    val referenceRange: String,
    val subtitle: String
)

data class HomeUiState(
    val recentConversations: List<Conversation> = emptyList(),
    val recentDocuments: List<MedicalDocument> = emptyList(),
    val latestMeasurements: List<StructuredMeasurement> = emptyList(),
    val modelStatus: ModelInferenceStatus = ModelInferenceStatus.NOT_INSTALLED,
    val isLoading: Boolean = false
) {
    val medGemmaStatus: ModelInferenceStatus get() = modelStatus
}

class HomeViewModel(
    private val chatRepository: ChatRepository,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val lfmManager: LfmManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                modelStatus = lfmManager.status
            )
        }

        viewModelScope.launch {
            chatRepository.getConversations().collect { convos ->
                _uiState.value = _uiState.value.copy(
                    recentConversations = convos.take(5)
                )
            }
        }

        viewModelScope.launch {
            documentRepository.getAllDocuments().collect { docs ->
                _uiState.value = _uiState.value.copy(
                    recentDocuments = docs.take(5)
                )
            }
        }

        viewModelScope.launch {
            historyRepository.getHealthTimeline().collect { timeline ->
                val measurements = timeline.filterIsInstance<HealthTimelineItem.MeasurementRecorded>()
                    .map { it.measurement }
                _uiState.value = _uiState.value.copy(
                    latestMeasurements = measurements
                )
            }
        }
    }

    fun createNewChat(initialQuery: String? = null, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val title = if (!initialQuery.isNullOrBlank()) {
                if (initialQuery.length > 28) initialQuery.take(28) + "..." else initialQuery
            } else {
                "Health Consultation #${System.currentTimeMillis() % 1000}"
            }
            val newId = chatRepository.createConversation(title = title)
            if (!initialQuery.isNullOrBlank()) {
                viewModelScope.launch {
                    chatRepository.sendMessage(newId, initialQuery).collect { }
                }
            }
            onCreated(newId)
        }
    }
}
