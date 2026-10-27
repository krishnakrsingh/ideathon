package com.swasthai.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.ai.EmbeddingEngine
import com.swasthai.app.ai.LfmBenchmarkMetrics
import com.swasthai.app.ai.LfmManager
import com.swasthai.app.domain.model.ModelInferenceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val generativeModelName: String = "LiquidAI/LFM2.5-1.2B-Instruct",
    val generativeStatus: ModelInferenceStatus = ModelInferenceStatus.NOT_INSTALLED,
    val generativePath: String = "",
    val generativeError: String? = null,
    val embeddingModelName: String = "LiquidAI/LFM2.5-Embedding-350M",
    val embeddingStatus: ModelInferenceStatus = ModelInferenceStatus.NOT_INSTALLED,
    val embeddingPath: String = "",
    val isInitializing: Boolean = false,
    val statusMessage: String? = null,
    val benchmarkMetrics: LfmBenchmarkMetrics? = null
) {
    val medGemmaStatus: ModelInferenceStatus get() = generativeStatus
    val medGemmaPath: String get() = generativePath
    val medGemmaError: String? get() = generativeError
}

class SettingsViewModel(
    private val lfmManager: LfmManager,
    private val embeddingEngine: EmbeddingEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        val detectedGen = lfmManager.findAvailableGenerativeModel()
        val detectedEmb = lfmManager.findAvailableEmbeddingModel()

        _uiState.value = _uiState.value.copy(
            generativeStatus = lfmManager.status,
            generativePath = lfmManager.modelPath ?: detectedGen?.absolutePath ?: LfmManager.DEFAULT_GENERATIVE_PATHS.first(),
            generativeError = lfmManager.lastErrorMessage,
            embeddingStatus = lfmManager.embeddingStatus,
            embeddingPath = lfmManager.embeddingModelPath ?: detectedEmb?.absolutePath ?: LfmManager.DEFAULT_EMBEDDING_PATHS.first(),
            benchmarkMetrics = lfmManager.lastBenchmarkMetrics
        )
    }

    fun setGenerativePath(path: String) {
        _uiState.value = _uiState.value.copy(generativePath = path)
    }

    fun setMedGemmaPath(path: String) {
        setGenerativePath(path)
    }

    fun setEmbeddingPath(path: String) {
        _uiState.value = _uiState.value.copy(embeddingPath = path)
    }

    fun initializeGenerative() {
        _uiState.value = _uiState.value.copy(isInitializing = true, statusMessage = null)
        viewModelScope.launch {
            val result = lfmManager.initialize(_uiState.value.generativePath)
            _uiState.value = _uiState.value.copy(
                isInitializing = false,
                generativeStatus = lfmManager.status,
                generativeError = lfmManager.lastErrorMessage,
                statusMessage = if (result.isSuccess) "Liquid AI LFM2.5 initialized successfully!" else "Initialization failed: ${lfmManager.lastErrorMessage}"
            )
        }
    }

    fun initializeMedGemma() {
        initializeGenerative()
    }

    fun initializeEmbedding() {
        _uiState.value = _uiState.value.copy(isInitializing = true, statusMessage = null)
        viewModelScope.launch {
            val result = lfmManager.initializeEmbedding(_uiState.value.embeddingPath)
            _uiState.value = _uiState.value.copy(
                isInitializing = false,
                embeddingStatus = lfmManager.embeddingStatus,
                statusMessage = if (result.isSuccess) "LFM2.5 Embedding ready!" else "Embedding initialization failed."
            )
        }
    }
}
