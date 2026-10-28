package com.swasthai.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.ai.EmbeddingEngine
import com.swasthai.app.ai.Gemma4ToolCallingModel
import com.swasthai.app.ai.GemmaModelArtifactManager
import com.swasthai.app.ai.ModelArtifactDownload
import com.swasthai.app.ai.ModelArtifactDownloadStatus
import com.swasthai.app.data.repository.DocumentRepository
import com.swasthai.app.data.repository.AccountFamilyRepository
import com.swasthai.app.data.repository.AccessibleCloudProfile
import com.swasthai.app.data.repository.FamilyAccount
import com.swasthai.app.data.repository.FamilyCredentials
import com.swasthai.app.security.DeviceKeyManager
import com.swasthai.app.domain.model.ModelInferenceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class SettingsUiState(
    val modelName: String = "Gemma 4 E2B · no-think · RAG and tool calling",
    val modelStatus: ModelInferenceStatus = ModelInferenceStatus.NOT_INSTALLED,
    val toolModelPath: String = "",
    val toolModelError: String? = null,
    val isModelArtifactAvailable: Boolean = false,
    val modelDownload: ModelArtifactDownload? = null,
    val embeddingModelName: String = "EmbeddingGemma 2 Text 270M",
    val embeddingStatus: ModelInferenceStatus = ModelInferenceStatus.NOT_INSTALLED,
    val embeddingPath: String = "",
    val isInitializing: Boolean = false,
    val isClearingRecords: Boolean = false,
    val localReportCount: Int = 0,
    val recordsOperationMessage: String? = null,
    val recordsOperationError: String? = null,
    val statusMessage: String? = null,
    val authUrl: String = "",
    val recordsApiUrl: String = "",
    val accountName: String = "",
    val accountEmail: String = "",
    val accountPassword: String = "",
    val signedInEmail: String? = null,
    val signedInUserId: String? = null,
    val accountBusy: Boolean = false,
    val accountMessage: String? = null,
    val accountError: String? = null,
    val familyIdInput: String = "",
    val familyPasswordInput: String = "",
    val families: List<FamilyAccount> = emptyList(),
    val selectedFamily: FamilyAccount? = null,
    val accessibleCloudProfiles: List<AccessibleCloudProfile> = emptyList(),
    val oneTimeFamilyCredentials: FamilyCredentials? = null
)

class SettingsViewModel(
    private val embeddingEngine: EmbeddingEngine,
    private val documentRepository: DocumentRepository,
    private val gemma4ToolCallingModel: Gemma4ToolCallingModel,
    private val gemmaModelArtifactManager: GemmaModelArtifactManager,
    private val accountFamilyRepository: AccountFamilyRepository,
    private val deviceKeyManager: DeviceKeyManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private var modelDownloadJob: Job? = null

    init {
        refreshStatus()
        val session = accountFamilyRepository.currentSession
        _uiState.value = _uiState.value.copy(
            authUrl = accountFamilyRepository.savedAuthUrl,
            recordsApiUrl = accountFamilyRepository.savedApiUrl,
            signedInEmail = session?.email,
            signedInUserId = session?.userId
        )
        if (session != null) refreshFamilies()
        gemmaModelArtifactManager.activeDownloadId()?.let(::observeModelDownload)
        viewModelScope.launch {
            documentRepository.getAllDocuments().collect { documents ->
                _uiState.value = _uiState.value.copy(localReportCount = documents.size)
            }
        }
        viewModelScope.launch {
            gemma4ToolCallingModel.statusFlow.collect { status ->
                _uiState.value = _uiState.value.copy(modelStatus = status, toolModelError = gemma4ToolCallingModel.lastError)
            }
        }
    }

    fun refreshStatus() {
        val detectedEmb = embeddingEngine.findAvailableModelFile()
        val detectedTool = gemma4ToolCallingModel.findAvailableModelFile()

        _uiState.value = _uiState.value.copy(
            modelStatus = gemma4ToolCallingModel.status,
            toolModelPath = detectedTool?.absolutePath ?: gemma4ToolCallingModel.suggestedModelPath(),
            toolModelError = gemma4ToolCallingModel.lastError,
            isModelArtifactAvailable = detectedTool != null,
            embeddingStatus = embeddingEngine.status,
            embeddingPath = embeddingEngine.modelFilePath ?: detectedEmb?.absolutePath
                ?: "/sdcard/Download/${EmbeddingEngine.MODEL_FILE_NAME}"
        )
    }

    fun clearLocalReports() {
        _uiState.value = _uiState.value.copy(
            isClearingRecords = true,
            recordsOperationMessage = null,
            recordsOperationError = null
        )
        viewModelScope.launch {
            try {
                val deleted = documentRepository.deleteAllDocuments()
                _uiState.value = _uiState.value.copy(recordsOperationMessage = "$deleted local report${if (deleted == 1) "" else "s"} deleted.")
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(recordsOperationError = error.localizedMessage ?: "Could not delete all local reports")
            } finally {
                _uiState.value = _uiState.value.copy(isClearingRecords = false)
            }
        }
    }

    fun setEmbeddingPath(path: String) {
        _uiState.value = _uiState.value.copy(embeddingPath = path)
    }

    fun setToolModelPath(path: String) {
        _uiState.value = _uiState.value.copy(toolModelPath = path)
    }

    fun initializeToolModel() {
        _uiState.value = _uiState.value.copy(isInitializing = true, statusMessage = null)
        viewModelScope.launch {
            val ready = gemma4ToolCallingModel.initialize(_uiState.value.toolModelPath)
            _uiState.value = _uiState.value.copy(
                isInitializing = false,
                modelStatus = gemma4ToolCallingModel.status,
                toolModelError = gemma4ToolCallingModel.lastError,
                statusMessage = if (ready) "Gemma 4 E2B tool model is ready." else gemma4ToolCallingModel.lastError
            )
        }
    }

    fun downloadGemmaModel() {
        try {
            val id = gemmaModelArtifactManager.enqueue()
            _uiState.value = _uiState.value.copy(modelDownload = ModelArtifactDownload(ModelArtifactDownloadStatus.PENDING))
            observeModelDownload(id)
        } catch (error: Exception) {
            _uiState.value = _uiState.value.copy(
                modelDownload = ModelArtifactDownload(
                    ModelArtifactDownloadStatus.FAILED,
                    message = error.localizedMessage ?: "Could not start model download."
                )
            )
        }
    }

    fun cancelGemmaModelDownload() {
        val id = gemmaModelArtifactManager.activeDownloadId() ?: return
        modelDownloadJob?.cancel()
        gemmaModelArtifactManager.cancel(id)
        _uiState.value = _uiState.value.copy(
            modelDownload = null,
            isModelArtifactAvailable = false,
            statusMessage = "Model download cancelled."
        )
    }

    private fun observeModelDownload(downloadId: Long) {
        modelDownloadJob?.cancel()
        modelDownloadJob = viewModelScope.launch {
            while (true) {
                val snapshot = gemmaModelArtifactManager.query(downloadId)
                _uiState.value = _uiState.value.copy(modelDownload = snapshot)
                when (snapshot.status) {
                    ModelArtifactDownloadStatus.READY -> {
                        refreshStatus()
                        _uiState.value = _uiState.value.copy(
                            isModelArtifactAvailable = true,
                            statusMessage = "Gemma 4 E2B downloaded and SHA-256 verified. Initialize it to enable chat."
                        )
                        return@launch
                    }
                    ModelArtifactDownloadStatus.FAILED,
                    ModelArtifactDownloadStatus.MISSING -> return@launch
                    else -> delay(MODEL_DOWNLOAD_POLL_INTERVAL_MS)
                }
            }
        }
    }

    fun initializeEmbedding() {
        _uiState.value = _uiState.value.copy(isInitializing = true, statusMessage = null)
        viewModelScope.launch {
            val initialized = embeddingEngine.initialize(_uiState.value.embeddingPath)
            if (initialized) documentRepository.indexMissingEmbeddings()
            _uiState.value = _uiState.value.copy(
                isInitializing = false,
                embeddingStatus = embeddingEngine.status,
                statusMessage = if (initialized) "EmbeddingGemma is ready for local semantic search."
                else embeddingEngine.lastError ?: "Embedding initialization failed."
            )
        }
    }

    fun setAuthUrl(value: String) { _uiState.value = _uiState.value.copy(authUrl = value) }
    fun setRecordsApiUrl(value: String) { _uiState.value = _uiState.value.copy(recordsApiUrl = value) }
    fun setAccountName(value: String) { _uiState.value = _uiState.value.copy(accountName = value) }
    fun setAccountEmail(value: String) { _uiState.value = _uiState.value.copy(accountEmail = value) }
    fun setAccountPassword(value: String) { _uiState.value = _uiState.value.copy(accountPassword = value) }
    fun setFamilyIdInput(value: String) { _uiState.value = _uiState.value.copy(familyIdInput = value) }
    fun setFamilyPasswordInput(value: String) { _uiState.value = _uiState.value.copy(familyPasswordInput = value) }

    fun signIn() = accountAction {
        accountFamilyRepository.saveServiceUrls(_uiState.value.authUrl, _uiState.value.recordsApiUrl)
        val session = accountFamilyRepository.signIn(_uiState.value.authUrl, _uiState.value.accountEmail, _uiState.value.accountPassword)
        _uiState.value = _uiState.value.copy(
            signedInEmail = session.email,
            signedInUserId = session.userId,
            accountPassword = "",
            accountMessage = "Signed in. Local records remain available on this device."
        )
        loadFamilies()
    }

    fun signUp() = accountAction {
        accountFamilyRepository.saveServiceUrls(_uiState.value.authUrl, _uiState.value.recordsApiUrl)
        val session = accountFamilyRepository.signUp(
            _uiState.value.authUrl,
            _uiState.value.accountName,
            _uiState.value.accountEmail,
            _uiState.value.accountPassword
        )
        _uiState.value = _uiState.value.copy(
            signedInEmail = session.email,
            signedInUserId = session.userId,
            accountPassword = "",
            accountMessage = "Account created and signed in."
        )
        loadFamilies()
    }

    fun signOut() = accountAction {
        accountFamilyRepository.signOut()
        _uiState.value = _uiState.value.copy(
            signedInEmail = null,
            signedInUserId = null,
            families = emptyList(),
            selectedFamily = null,
            accessibleCloudProfiles = emptyList(),
            accountMessage = "Signed out. Your local profiles and records remain on this device."
        )
    }

    fun refreshFamilies() = accountAction { loadFamilies() }

    fun createFamily() = accountAction {
        val credentials = accountFamilyRepository.createFamily()
        _uiState.value = _uiState.value.copy(
            oneTimeFamilyCredentials = credentials,
            familyIdInput = credentials.familyId,
            accountMessage = "Save these join credentials now. The password is shown only once."
        )
        loadFamilies()
    }

    fun joinFamily() = accountAction {
        accountFamilyRepository.joinFamily(_uiState.value.familyIdInput, _uiState.value.familyPasswordInput)
        _uiState.value = _uiState.value.copy(familyPasswordInput = "", accountMessage = "Joined family. Loading members…")
        loadFamilies()
    }

    fun selectFamily(familyId: String) = accountAction {
        _uiState.value = _uiState.value.copy(selectedFamily = accountFamilyRepository.getFamily(familyId))
    }

    fun rotateJoinPassword(familyId: String) = accountAction {
        val credentials = accountFamilyRepository.rotateJoinPassword(familyId)
        _uiState.value = _uiState.value.copy(
            oneTimeFamilyCredentials = credentials,
            accountMessage = "Join password rotated. Save the new password now."
        )
        loadFamilies(familyId)
    }

    fun removeFamilyMember(familyId: String, memberUserId: String) = accountAction {
        accountFamilyRepository.removeMember(familyId, memberUserId)
        _uiState.value = _uiState.value.copy(accountMessage = "Member removed and future family access revoked.")
        loadFamilies(familyId)
    }

    fun unblockFamilyMember(familyId: String, memberUserId: String) = accountAction {
        accountFamilyRepository.unblockMember(familyId, memberUserId)
        _uiState.value = _uiState.value.copy(accountMessage = "Account may join again using the current family credentials.")
        loadFamilies(familyId)
    }

    fun leaveFamily(familyId: String) = accountAction {
        accountFamilyRepository.leaveFamily(familyId)
        _uiState.value = _uiState.value.copy(accountMessage = "You left the family.", selectedFamily = null)
        loadFamilies()
    }

    private suspend fun loadFamilies(selectedId: String? = null) {
        val session = accountFamilyRepository.currentSession
        val deviceKeyRegistered = session?.let { activeSession ->
            runCatching {
                accountFamilyRepository.registerDevicePublicKey(
                    deviceKeyManager.getOrCreatePublicKey(activeSession.userId)
                )
            }.isSuccess
        } ?: false
        val listed = accountFamilyRepository.listFamilies()
        val cloudProfiles = accountFamilyRepository.listAccessibleProfiles()
        val selected = selectedId ?: _uiState.value.selectedFamily?.familyId ?: listed.firstOrNull()?.familyId
        _uiState.value = _uiState.value.copy(
            families = listed,
            selectedFamily = selected?.let { id -> runCatching { accountFamilyRepository.getFamily(id) }.getOrNull() },
            accessibleCloudProfiles = cloudProfiles,
            accountMessage = if (!deviceKeyRegistered) {
                "Account loaded, but this device's key could not be registered for future sharing. Retry refresh after reconnecting."
            } else _uiState.value.accountMessage
        )
    }

    private fun accountAction(action: suspend () -> Unit) {
        _uiState.value = _uiState.value.copy(accountBusy = true, accountError = null, accountMessage = null)
        viewModelScope.launch {
            try {
                action()
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(accountError = error.localizedMessage ?: "Account request failed")
            } finally {
                _uiState.value = _uiState.value.copy(accountBusy = false)
            }
        }
    }

    private companion object {
        const val MODEL_DOWNLOAD_POLL_INTERVAL_MS = 1_000L
    }
}
