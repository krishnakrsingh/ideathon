package com.swasthai.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.ai.Gemma4ToolCallingModel
import com.swasthai.app.data.repository.ChatRepository
import com.swasthai.app.data.repository.DocumentRepository
import com.swasthai.app.data.repository.HistoryRepository
import com.swasthai.app.data.repository.PersonProfileRepository
import com.swasthai.app.data.repository.MedicationReminderRepository
import com.swasthai.app.data.repository.CycleTrackingRepository
import com.swasthai.app.data.repository.CycleTrackerSummary
import com.swasthai.app.data.repository.MenstrualFlow
import com.swasthai.app.data.local.entity.PersonProfileEntity
import com.swasthai.app.data.local.entity.MedicationReminderEntity
import com.swasthai.app.domain.model.Conversation
import com.swasthai.app.domain.model.HealthTimelineItem
import com.swasthai.app.domain.model.MedicalDocument
import com.swasthai.app.domain.model.ModelInferenceStatus
import com.swasthai.app.domain.model.StructuredMeasurement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val recentConversations: List<Conversation> = emptyList(),
    val recentDocuments: List<MedicalDocument> = emptyList(),
    val latestMeasurements: List<StructuredMeasurement> = emptyList(),
    val modelStatus: ModelInferenceStatus = ModelInferenceStatus.NOT_INSTALLED,
    val profiles: List<PersonProfileEntity> = emptyList(),
    val activeProfileId: String = "local-default-profile",
    val profileError: String? = null,
    val medicationReminders: List<MedicationReminderEntity> = emptyList(),
    val reminderError: String? = null,
    val cycleSummary: CycleTrackerSummary = CycleTrackerSummary(),
    val cycleError: String? = null,
    val isLoading: Boolean = false
) {
    val aiModelStatus: ModelInferenceStatus get() = modelStatus
}

class HomeViewModel(
    private val chatRepository: ChatRepository,
    private val documentRepository: DocumentRepository,
    private val historyRepository: HistoryRepository,
    private val gemma4ToolCallingModel: Gemma4ToolCallingModel,
    private val personProfileRepository: PersonProfileRepository,
    private val medicationReminderRepository: MedicationReminderRepository,
    private val cycleTrackingRepository: CycleTrackingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            personProfileRepository.profiles.collect { profiles ->
                _uiState.value = _uiState.value.copy(profiles = profiles)
            }
        }
        viewModelScope.launch {
            personProfileRepository.activeProfileId.collect { profileId ->
                _uiState.value = _uiState.value.copy(activeProfileId = profileId)
            }
        }
        viewModelScope.launch {
            medicationReminderRepository.observeActiveProfileReminders().collect { reminders ->
                _uiState.value = _uiState.value.copy(medicationReminders = reminders)
            }
        }
        viewModelScope.launch {
            cycleTrackingRepository.observeActiveSummary().collect { summary ->
                _uiState.value = _uiState.value.copy(cycleSummary = summary)
            }
        }
        viewModelScope.launch {
            gemma4ToolCallingModel.statusFlow.collect { status ->
                _uiState.value = _uiState.value.copy(modelStatus = status)
            }
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

    fun selectProfile(profileId: String) {
        viewModelScope.launch {
            runCatching { personProfileRepository.selectProfile(profileId) }
                .onFailure { error -> _uiState.value = _uiState.value.copy(profileError = error.message) }
        }
    }

    fun createProfile(
        displayName: String,
        relationship: String,
        dateOfBirthIso: String,
        sexAtBirth: String?,
        avatarSeed: String,
        onCreated: () -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                val dateOfBirth = dateOfBirthIso.trim().takeIf(String::isNotEmpty)?.let { value ->
                    LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                }
                personProfileRepository.createProfile(displayName, relationship, dateOfBirth, sexAtBirth, avatarSeed)
            }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(profileError = null)
                    onCreated()
                }
                .onFailure { error -> _uiState.value = _uiState.value.copy(profileError = error.message) }
        }
    }

    fun clearProfileError() {
        _uiState.value = _uiState.value.copy(profileError = null)
    }

    fun createMedicationReminder(name: String, instructions: String, time: String, onCreated: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                val localTime = LocalTime.parse(time.trim(), DateTimeFormatter.ofPattern("H:mm"))
                medicationReminderRepository.createReminder(name, instructions, localTime.hour, localTime.minute)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(reminderError = null)
                onCreated()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(reminderError = error.message ?: "Could not save reminder")
            }
        }
    }

    fun deleteMedicationReminder(id: Long) {
        viewModelScope.launch { medicationReminderRepository.deleteReminder(id) }
    }

    fun logCycleDay(dateIso: String, flow: MenstrualFlow, isPeriodStart: Boolean, symptoms: Set<String>, onSaved: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                cycleTrackingRepository.logDay(LocalDate.parse(dateIso.trim()), flow, isPeriodStart, symptoms)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(cycleError = null)
                onSaved()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(cycleError = error.message ?: "Could not save cycle log")
            }
        }
    }

    fun clearCycleError() { _uiState.value = _uiState.value.copy(cycleError = null) }

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
