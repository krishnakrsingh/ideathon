package com.swasthai.app.ai

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.swasthai.app.domain.model.ChatContextScope
import com.swasthai.app.domain.model.ScopeMode
import com.swasthai.app.domain.model.SourceCitation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

data class ExecutedMedicalTool(
    val name: String,
    val resultCount: Int,
    val retrievalMode: String
)

/** Per-turn, read-only view of one profile's mini record filesystem. Scope is frozen before model inference. */
class MedicalRecordToolDefinitions(
    private val ragPipeline: RAGPipeline,
    private val scope: ChatContextScope,
    private val targetDocumentId: Long?,
    initialRetrieval: RetrievalResult,
    private val onActivity: (String) -> Unit = {}
) : ToolSet {
    private val calls = AtomicInteger(0)
    private val traces = mutableListOf(
        ExecutedMedicalTool("rag_preflight", initialRetrieval.citations.size + initialRetrieval.structuredBiomarkersFound.size, initialRetrieval.searchMode.name)
    )
    private val citations = initialRetrieval.citations.toMutableList()
    private val structured = initialRetrieval.structuredBiomarkersFound.toMutableList()

    @Tool(description = "List medical report files available to this account/profile in import-date order. Use an empty query to inspect the in-scope catalog in pages; increase offset by limit to inspect older files. Use a short name/type query to find likely files. This tool cannot access another profile or any file outside the frozen date and document scope.")
    fun list_medical_files(
        @ToolParam(description = "Report name/type keywords, or empty text to list in-scope files") query: String,
        @ToolParam(description = "Zero-based result offset for timeline pagination") offset: Int,
        @ToolParam(description = "Number of reports to return, from 1 to 8") limit: Int
    ): String = execute("list_medical_files", recordEvidence = false) {
        if (query.isBlank()) ragPipeline.listMedicalDocuments(targetDocumentId, scope, offset = offset, limit = limit)
        else ragPipeline.findMedicalDocuments(query.take(240), targetDocumentId, scope, offset = offset, limit = limit)
    }

    @Tool(description = "Read extracted report text in bounded pages/chunk groups. Use page_number 0 to scan the whole report from chunk_offset 0; continue with the returned next_offset while has_more is true. Use a positive page_number to read only that page. The document ID is checked against the active profile and frozen scope.")
    fun read_medical_document(
        @ToolParam(description = "Report ID returned by list_medical_files, as text") document_id: String,
        @ToolParam(description = "Page number to read, or 0 to scan the report in chunk groups") page_number: Int,
        @ToolParam(description = "Zero-based chunk offset; use next_offset returned by a prior call") chunk_offset: Int,
        @ToolParam(description = "Number of text chunks to read, from 1 to 8") limit: Int
    ): String {
        val parsedDocumentId = document_id.toLongOrNull() ?: return error("invalid_document_id")
        return execute("read_medical_document") {
            ragPipeline.readMedicalDocument(parsedDocumentId, page_number, targetDocumentId, scope, chunk_offset, limit)
        }
    }

    @Tool(description = "Search report text for evidence relevant to a focused clinical question. Returns source-linked excerpts from the current profile and frozen scope only.")
    fun search_medical_records(
        @ToolParam(description = "A focused question about report contents, maximum 240 characters") query: String
    ): String = execute("search_medical_records") {
        ragPipeline.retrieveRelevantContext(query.take(240), targetDocumentId, topK = 6, scope = scope)
    }

    @Tool(description = "Retrieve dated, structured lab history for one biomarker from reports in the active profile and frozen scope. This is read-only.")
    fun get_biomarker_history(
        @ToolParam(description = "The biomarker name, such as hemoglobin, glucose, or cholesterol") biomarker: String
    ): String = execute("get_biomarker_history") {
        ragPipeline.retrieveRelevantContext(
            biomarker.take(120), targetDocumentId, topK = 8, scope = scope,
            intent = RetrievalIntent.BIOMARKER_HISTORY
        )
    }

    private fun execute(name: String, recordEvidence: Boolean = true, operation: suspend () -> RetrievalResult): String {
        if (scope.mode == ScopeMode.CONVERSATION_ONLY) return error("record_access_not_enabled")
        if (calls.incrementAndGet() > MAX_TOOL_CALLS) return error("per_turn_tool_limit_reached")
        onActivity(activityLabel(name))
        return try {
            val result = runBlocking(Dispatchers.IO) { operation() }
            synchronized(this) {
                if (recordEvidence) citations += result.citations
                structured += result.structuredBiomarkersFound
                traces += ExecutedMedicalTool(
                    name,
                    result.files.size + result.citations.size + result.structuredBiomarkersFound.size,
                    result.searchMode.name
                )
            }
            val count = result.files.size + result.citations.size + result.structuredBiomarkersFound.size
            onActivity(if (count == 0) "No matching records found; continuing" else "Reviewed $count record result${if (count == 1) "" else "s"}; continuing")
            resultJson(result)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.w("MedicalRecordTools", "Read-only record tool failed: $name", error)
            synchronized(this) {
                traces += ExecutedMedicalTool(name, 0, "TOOL_ERROR")
            }
            onActivity("A record search failed; continuing")
            JSONObject().put("error", "record_tool_failed").toString()
        }
    }

    private fun activityLabel(name: String): String = when (name) {
        "list_medical_files" -> "Browsing your report timeline"
        "read_medical_document" -> "Reading a report"
        "search_medical_records" -> "Searching your reports"
        "get_biomarker_history" -> "Checking lab history"
        else -> "Checking your records"
    }

    fun retrievalResult(): RetrievalResult = synchronized(this) {
        val unique = citations.distinctBy { Triple(it.documentId, it.pageNumber, it.chunkText) }.take(MAX_EVIDENCE)
        val modes = traces.mapNotNull { trace ->
            runCatching { RetrievalMode.valueOf(trace.retrievalMode) }.getOrNull()
        }
        val mode = when {
            modes.any { it == RetrievalMode.HYBRID } -> RetrievalMode.HYBRID
            modes.any { it == RetrievalMode.SEMANTIC_VECTOR } -> RetrievalMode.SEMANTIC_VECTOR
            modes.any { it == RetrievalMode.STRUCTURED_SQL_ONLY } -> RetrievalMode.STRUCTURED_SQL_ONLY
            modes.any { it == RetrievalMode.LOCAL_KEYWORD_FALLBACK } -> RetrievalMode.LOCAL_KEYWORD_FALLBACK
            else -> RetrievalMode.NO_RECORDS_FOUND
        }
        RetrievalResult(unique, mode, structured.distinct().take(16), traces.toList())
    }

    private fun resultJson(result: RetrievalResult): String = JSONObject()
        .put("mode", result.searchMode.name)
        .put("files", JSONArray().apply {
            result.files.take(8).forEach { file ->
                val imported = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT)
                    .format(java.util.Date(file.importedAt))
                val reportDate = file.reportDate?.let {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date(it))
                }
                put(
                    JSONObject()
                        .put("document_id", file.documentId)
                        .put("file_name", file.fileName.take(180))
                        .put("imported_date", imported)
                        .put("report_date", reportDate ?: JSONObject.NULL)
                        .put("page_count", file.pageCount)
                        .put("indexing_status", file.indexingStatus)
                        .put("indexing_note", file.failureReason ?: JSONObject.NULL)
                )
            }
        })
        .put("evidence", JSONArray().apply {
            result.citations.take(8).forEach { citation ->
                put(
                    JSONObject()
                        .put("document_id", citation.documentId)
                        .put("document_name", citation.documentName.take(180))
                        .put("page", citation.pageNumber)
                        .put("text", citation.chunkText.take(MAX_CHARS_PER_EVIDENCE))
                )
            }
        })
        .put("structured_measurements", JSONArray(result.structuredBiomarkersFound.take(8).map { it.take(220) }))
        .put("has_more", result.hasMore)
        .put("next_offset", result.nextOffset)
        .toString()

    private fun error(code: String) = JSONObject().put("error", code).toString()

    companion object {
        private const val MAX_TOOL_CALLS = 12
        private const val MAX_EVIDENCE = 16
        private const val MAX_CHARS_PER_EVIDENCE = 1_000
    }
}
