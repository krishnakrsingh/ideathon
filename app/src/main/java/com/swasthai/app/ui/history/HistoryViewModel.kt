package com.swasthai.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.data.repository.HistoryRepository
import com.swasthai.app.domain.model.HealthTimelineItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val timelineItems: List<HealthTimelineItem> = emptyList(),
    val filteredItems: List<HealthTimelineItem> = emptyList(),
    val filterQuery: String = ""
)

class HistoryViewModel(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadTimeline()
    }

    private fun loadTimeline() {
        viewModelScope.launch {
            historyRepository.getHealthTimeline().collect { items ->
                _uiState.value = _uiState.value.copy(
                    timelineItems = items,
                    filteredItems = filterTimeline(items, _uiState.value.filterQuery)
                )
            }
        }
    }

    fun setFilterQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            filterQuery = query,
            filteredItems = filterTimeline(_uiState.value.timelineItems, query)
        )
    }

    private fun filterTimeline(items: List<HealthTimelineItem>, query: String): List<HealthTimelineItem> {
        if (query.isBlank()) return items
        return items.filter { item ->
            when (item) {
                is HealthTimelineItem.ReportImported -> item.documentTitle.contains(query, ignoreCase = true)
                is HealthTimelineItem.MeasurementRecorded -> item.measurement.biomarkerName.contains(query, ignoreCase = true)
            }
        }
    }
}
