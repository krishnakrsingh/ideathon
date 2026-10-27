package com.swasthai.app.ai

import com.swasthai.app.data.local.dao.DocumentDao
import com.swasthai.app.data.local.dao.MeasurementDao
import com.swasthai.app.data.local.dao.RAGDao
import com.swasthai.app.data.local.entity.DocumentChunkEntity
import com.swasthai.app.domain.model.ChatContextScope
import com.swasthai.app.domain.model.ModelInferenceStatus
import com.swasthai.app.domain.model.ScopeMode
import com.swasthai.app.domain.model.SourceCitation
import com.swasthai.app.domain.model.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class RetrievalResult(
    val citations: List<SourceCitation>,
    val searchMode: RetrievalMode,
    val structuredBiomarkersFound: List<String> = emptyList()
)

enum class RetrievalMode {
    SEMANTIC_VECTOR,
    LOCAL_KEYWORD_FALLBACK,
    STRUCTURED_SQL_ONLY,
    NO_RECORDS_FOUND
}

class RAGPipeline(
    private val documentDao: DocumentDao,
    private val ragDao: RAGDao,
    private val measurementDao: MeasurementDao,
    private val embeddingEngine: EmbeddingEngine
) {

    companion object {
        val STOP_WORDS = setOf(
            "what", "is", "my", "the", "for", "and", "or", "in", "how", "tell", "me", "about",
            "can", "you", "explain", "does", "are", "of", "to", "a", "an", "this", "that",
            "with", "from", "at", "by", "on", "was", "were", "been", "have", "has", "had",
            "do", "did", "please", "results", "result", "level", "levels", "report", "test",
            "tests", "normal", "range", "ranges", "value", "values", "show", "give", "check",
            "good", "bad", "high", "low", "should", "would", "could", "there", "their"
        )

        fun extractSearchTerms(query: String): List<String> {
            val clean = query.lowercase(Locale.US)
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .trim()

            val tokens = clean.split(Regex("\\s+")).filter { it.length >= 2 }
            val terms = mutableListOf<String>()

            for (t in tokens) {
                if (t in STOP_WORDS) continue

                // Expand common medical abbreviations and synonyms
                when (t) {
                    "hb" -> { terms.add("hemoglobin"); terms.add("hb") }
                    "bp" -> { terms.add("pressure"); terms.add("systolic") }
                    "sugar" -> { terms.add("glucose"); terms.add("sugar") }
                    "cholesterol" -> { terms.add("cholesterol"); terms.add("lipid"); terms.add("ldl") }
                    "vit" -> { terms.add("vitamin") }
                    "wbc" -> { terms.add("white"); terms.add("leukocyte"); terms.add("wbc") }
                    "rbc" -> { terms.add("red"); terms.add("erythrocyte"); terms.add("rbc") }
                    "plt" -> { terms.add("platelet"); terms.add("platelets") }
                    else -> {
                        if (t.length >= 3) terms.add(t)
                    }
                }
            }

            return terms.distinct()
        }
    }

    /**
     * Executes hybrid retrieval across saved documents constrained by the active ChatContextScope.
     * Evaluates ScopeMode (Conversation only vs All History vs Specific Documents)
     * and TimeRangeFilter (30 days, 3 months, 12 months, custom).
     */
    suspend fun retrieveRelevantContext(
        query: String,
        targetDocumentId: Long? = null,
        topK: Int = 4,
        scope: ChatContextScope = ChatContextScope()
    ): RetrievalResult = withContext(Dispatchers.IO) {
        if (query.isBlank() || scope.mode == ScopeMode.CONVERSATION_ONLY) {
            return@withContext RetrievalResult(
                citations = emptyList(),
                searchMode = RetrievalMode.NO_RECORDS_FOUND
            )
        }

        // 1. Resolve eligible document IDs by scope mode and date range
        val allDocs = documentDao.getAllDocumentsList()
        if (allDocs.isEmpty()) {
            return@withContext RetrievalResult(
                citations = emptyList(),
                searchMode = RetrievalMode.NO_RECORDS_FOUND
            )
        }

        val baseDocs = when {
            targetDocumentId != null -> allDocs.filter { it.id == targetDocumentId }
            scope.mode == ScopeMode.SPECIFIC_DOCS -> {
                if (scope.selectedDocumentIds.isEmpty()) return@withContext RetrievalResult(
                    citations = emptyList(),
                    searchMode = RetrievalMode.NO_RECORDS_FOUND
                )
                allDocs.filter { it.id in scope.selectedDocumentIds }
            }
            else -> allDocs
        }

        val now = System.currentTimeMillis()
        val eligibleDocs = baseDocs.filter { doc ->
            val docTime = doc.reportDate ?: doc.importDate
            when (scope.timeRange) {
                TimeRangeFilter.ALL_TIME -> true
                TimeRangeFilter.LAST_30_DAYS -> docTime >= (now - 30L * 24 * 3600 * 1000)
                TimeRangeFilter.LAST_3_MONTHS -> docTime >= (now - 90L * 24 * 3600 * 1000)
                TimeRangeFilter.LAST_12_MONTHS -> docTime >= (now - 365L * 24 * 3600 * 1000)
                TimeRangeFilter.CUSTOM -> {
                    val start = scope.customStartDate ?: 0L
                    val end = scope.customEndDate ?: Long.MAX_VALUE
                    docTime in start..end
                }
            }
        }

        if (eligibleDocs.isEmpty()) {
            return@withContext RetrievalResult(
                citations = emptyList(),
                searchMode = RetrievalMode.NO_RECORDS_FOUND
            )
        }

        val eligibleDocIds = eligibleDocs.map { it.id }.toSet()
        val searchTerms = extractSearchTerms(query)

        // 2. Structured SQL lookup for lab tests (if enabled by scope)
        val structuredHits = if (scope.includeStructuredVitals) {
            findStructuredBiomarkerHits(searchTerms, eligibleDocIds)
        } else {
            emptyList()
        }

        // 3. Semantic vector retrieval using local embeddings
        if (embeddingEngine.status == ModelInferenceStatus.READY) {
            val queryVector = embeddingEngine.generateEmbedding(query, isQuery = true)
            if (queryVector != null) {
                val candidateEmbeddings = ragDao.getEmbeddingsForDocuments(eligibleDocIds.toList())

                if (candidateEmbeddings.isNotEmpty()) {
                    val ranked = embeddingEngine.rankChunks(
                        queryVector = queryVector,
                        candidates = candidateEmbeddings.map { it.chunkId to it.embeddingBlob },
                        topK = topK
                    )

                    // Filter out non-relevant matches (cosine similarity < 0.35)
                    val relevantRanked = ranked.filter { it.score >= 0.35f }

                    if (relevantRanked.isNotEmpty()) {
                        val chunkIds = relevantRanked.map { it.chunkId }
                        val chunksWithDocs = ragDao.getChunksWithDocuments(chunkIds)
                        val scoreMap = relevantRanked.associate { it.chunkId to it.score }

                        val citations = chunksWithDocs
                            .filter { it.document.id in eligibleDocIds }
                            .map { item ->
                                SourceCitation(
                                    documentId = item.document.id,
                                    documentName = item.document.fileName,
                                    pageNumber = item.chunk.pageNumber,
                                    chunkText = item.chunk.text,
                                    similarityScore = scoreMap[item.chunk.id] ?: 0.0f
                                )
                            }

                        if (citations.isNotEmpty()) {
                            return@withContext RetrievalResult(
                                citations = citations,
                                searchMode = RetrievalMode.SEMANTIC_VECTOR,
                                structuredBiomarkersFound = structuredHits.map { it.displayText }
                            )
                        }
                    }
                }
            }
        }

        // 4. Local keyword search across eligible documents
        val candidateChunks = mutableListOf<DocumentChunkEntity>()
        if (searchTerms.isNotEmpty()) {
            for (docId in eligibleDocIds) {
                for (term in searchTerms) {
                    val chunks = documentDao.searchChunksInDocumentByKeyword(docId, term, limit = topK)
                    candidateChunks.addAll(chunks)
                }
            }
        } else if (eligibleDocIds.size == 1) {
            candidateChunks.addAll(documentDao.getChunksForDocument(eligibleDocIds.first()).take(topK))
        }

        if (candidateChunks.isNotEmpty()) {
            val scoredChunks = candidateChunks
                .groupBy { it.id }
                .map { (_, list) ->
                    val chunk = list.first()
                    var termMatchCount = 0
                    val lowerText = chunk.text.lowercase(Locale.US)
                    for (t in searchTerms) {
                        if (lowerText.contains(t)) termMatchCount++
                    }
                    val score = termMatchCount.toFloat() + (1.0f / (chunk.pageNumber + 1))
                    chunk to score
                }
                .sortedByDescending { it.second }
                .take(topK)
                .map { it.first }

            val docMap = eligibleDocs.associateBy { it.id }
            val citations = scoredChunks.map { chunk ->
                val doc = docMap[chunk.documentId]
                SourceCitation(
                    documentId = chunk.documentId,
                    documentName = doc?.fileName ?: "Document #${chunk.documentId}",
                    pageNumber = chunk.pageNumber,
                    chunkText = chunk.text,
                    similarityScore = 1.0f
                )
            }

            return@withContext RetrievalResult(
                citations = citations,
                searchMode = RetrievalMode.LOCAL_KEYWORD_FALLBACK,
                structuredBiomarkersFound = structuredHits.map { it.displayText }
            )
        }

        // 5. Structured SQL hits only (if any exist)
        if (structuredHits.isNotEmpty()) {
            val docMap = eligibleDocs.associateBy { it.id }
            val structuredCitations = structuredHits.map { hit ->
                val doc = docMap[hit.documentId]
                SourceCitation(
                    documentId = hit.documentId,
                    documentName = doc?.fileName ?: "Lab Record #${hit.documentId}",
                    pageNumber = 1,
                    chunkText = "Verified Lab Result: ${hit.displayText}",
                    similarityScore = 0.95f
                )
            }.take(topK)

            return@withContext RetrievalResult(
                citations = structuredCitations,
                searchMode = RetrievalMode.STRUCTURED_SQL_ONLY,
                structuredBiomarkersFound = structuredHits.map { it.displayText }
            )
        }

        // 6. No relevant records found within scope
        RetrievalResult(
            citations = emptyList(),
            searchMode = RetrievalMode.NO_RECORDS_FOUND,
            structuredBiomarkersFound = emptyList()
        )
    }

    private data class StructuredHit(
        val documentId: Long,
        val displayText: String
    )

    private suspend fun findStructuredBiomarkerHits(
        searchTerms: List<String>,
        allowedDocIds: Set<Long>
    ): List<StructuredHit> {
        val hits = mutableListOf<StructuredHit>()

        for (term in searchTerms) {
            val measurements = measurementDao.getMeasurementsByBiomarker(term)
            val filtered = measurements.filter { it.documentId in allowedDocIds }
            for (m in filtered) {
                val refStr = if (m.referenceLow != null && m.referenceHigh != null) {
                    " (Ref: ${m.referenceLow} - ${m.referenceHigh})"
                } else ""
                hits.add(
                    StructuredHit(
                        documentId = m.documentId,
                        displayText = "${m.biomarkerName}: ${m.numericValue} ${m.unit}$refStr"
                    )
                )
            }
        }
        return hits.distinctBy { it.displayText }
    }
}
