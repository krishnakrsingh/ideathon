package com.swasthai.app.ui.vault

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swasthai.app.data.local.entity.DocumentChunkEntity
import com.swasthai.app.data.repository.DocumentRepository
import com.swasthai.app.domain.model.MedicalDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VaultUiState(
    val documents: List<MedicalDocument> = emptyList(),
    val filteredDocuments: List<MedicalDocument> = emptyList(),
    val searchQuery: String = "",
    val isImporting: Boolean = false,
    val selectedDocChunks: List<DocumentChunkEntity> = emptyList(),
    val inspectingDocument: MedicalDocument? = null,
    val errorMessage: String? = null
)

class VaultViewModel(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        loadDocuments()
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            documentRepository.getAllDocuments().collect { docs ->
                _uiState.value = _uiState.value.copy(
                    documents = docs,
                    filteredDocuments = filterList(docs, _uiState.value.searchQuery)
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredDocuments = filterList(_uiState.value.documents, query)
        )
    }

    private fun filterList(list: List<MedicalDocument>, query: String): List<MedicalDocument> {
        if (query.isBlank()) return list
        return list.filter { it.fileName.contains(query, ignoreCase = true) }
    }

    fun importPdf(uri: Uri, fileName: String) {
        _uiState.value = _uiState.value.copy(isImporting = true, errorMessage = null)
        viewModelScope.launch {
            val result = documentRepository.importDocument(uri, fileName)
            _uiState.value = _uiState.value.copy(
                isImporting = false,
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun importMultiplePdfs(urisWithNames: List<Pair<Uri, String>>) {
        if (urisWithNames.isEmpty()) return
        _uiState.value = _uiState.value.copy(isImporting = true, errorMessage = null)
        viewModelScope.launch {
            var lastError: String? = null
            for ((uri, fileName) in urisWithNames) {
                val result = documentRepository.importDocument(uri, fileName)
                if (result.isFailure) {
                    lastError = result.exceptionOrNull()?.localizedMessage
                }
            }
            _uiState.value = _uiState.value.copy(
                isImporting = false,
                errorMessage = lastError
            )
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            documentRepository.deleteDocument(id)
            if (_uiState.value.inspectingDocument?.id == id) {
                closeInspector()
            }
        }
    }

    fun inspectDocument(document: MedicalDocument) {
        _uiState.value = _uiState.value.copy(inspectingDocument = document)
        viewModelScope.launch {
            val chunks = documentRepository.getChunksForDocument(document.id)
            _uiState.value = _uiState.value.copy(selectedDocChunks = chunks)
        }
    }

    fun closeInspector() {
        _uiState.value = _uiState.value.copy(
            inspectingDocument = null,
            selectedDocChunks = emptyList()
        )
    }
}
