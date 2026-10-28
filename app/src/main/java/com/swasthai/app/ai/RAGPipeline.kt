package com.swasthai.app.ai

import com.swasthai.app.data.local.dao.DocumentDao
import com.swasthai.app.data.local.dao.MeasurementDao
import com.swasthai.app.data.local.dao.RAGDao
import com.swasthai.app.data.local.dao.RankedChunkCandidate
import com.swasthai.app.data.local.entity.DocumentChunkEntity
import com.swasthai.app.domain.model.ChatContextScope
import com.swasthai.app.domain.model.ModelInferenceStatus
import com.swasthai.app.domain.model.ScopeMode
import com.swasthai.app.domain.model.SourceCitation
import com.swasthai.app.domain.model.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ln

data class RetrievalResult(
    val citations: List<SourceCitation>,
    val searchMode: RetrievalMode,
    val structuredBiomarkersFound: List<String> = emptyList(),
    val executedTools: List<ExecutedMedicalTool> = emptyList(),
    val hasMore: Boolean = false,
    val nextOffset: Int? = null,
    val files: List<MedicalFileEntry> = emptyList()
)

data class MedicalFileEntry(
    val documentId: Long,
    val fileName: String,
    val importedAt: Long,
    val reportDate: Long?,
    val pageCount: Int,
    val indexingStatus: String,
    val failureReason: String?
)

enum class RetrievalMode {
    HYBRID,
    SEMANTIC_VECTOR,
    LOCAL_KEYWORD_FALLBACK,
    STRUCTURED_SQL_ONLY,
    NO_RECORDS_FOUND
}

enum class RetrievalIntent {
    RECORD_SEARCH,
    BIOMARKER_HISTORY
}

class RAGPipeline(
    private val documentDao: DocumentDao,
    private val ragDao: RAGDao,
    private val measurementDao: MeasurementDao,
    private val embeddingEngine: EmbeddingEngine
) {

    companion object {
        private const val MIN_SEMANTIC_SIMILARITY = 0.35f
        private const val SQLITE_IN_BATCH_SIZE = 400
        private const val MAX_TOOL_PAGE_SIZE = 8
        private const val BM25_K1 = 1.2f
        private const val BM25_B = 0.75f
        val STOP_WORDS = setOf(
            "what", "is", "my", "the", "for", "and", "or", "in", "how", "tell", "me", "about",
            "can", "you", "explain", "does", "are", "of", "to", "a", "an", "this", "that",
            "with", "from", "at", "by", "on", "was", "were", "been", "have", "has", "had",
            "do", "did", "please", "results", "result", "level", "levels", "report", "test",
            "tests", "normal", "range", "ranges", "value", "values", "show", "give", "check",
            "good", "bad", "high", "low", "should", "would", "could", "there", "their",
            "मेरा", "मेरी", "मेरे", "क्या", "कैसे", "कितना", "कितनी", "और", "में", "का", "की", "के",
            "है", "हैं", "था", "थे", "बताओ", "दिखाओ", "समझाओ", "रिपोर्ट", "रिपोर्ट्स", "जांच", "स्तर"
        )

        fun extractSearchTerms(query: String): List<String> {
            val clean = query.lowercase(Locale.ROOT)
                .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
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
                    "हीमोग्लोबिन", "हिमोग्लोबिन" -> { terms.add("hemoglobin"); terms.add("haemoglobin"); terms.add("hb") }
                    "शुगर", "मधुमेह" -> { terms.add("glucose"); terms.add("sugar"); terms.add("hba1c") }
                    "कोलेस्ट्रॉल" -> { terms.add("cholesterol"); terms.add("lipid"); terms.add("ldl") }
                    "रक्तचाप", "बीपी", "प्रेशर" -> { terms.add("pressure"); terms.add("systolic"); terms.add("diastolic") }
                    "प्लेटलेट", "प्लेटलेट्स" -> { terms.add("platelet"); terms.add("platelets"); terms.add("plt") }
                    "सफेद", "श्वेत" -> { terms.add("white"); terms.add("leukocyte"); terms.add("wbc") }
                    "लाल" -> { terms.add("red"); terms.add("erythrocyte"); terms.add("rbc") }
                    "विटामिन" -> { terms.add("vitamin"); terms.add("vit") }
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
        scope: ChatContextScope = ChatContextScope(),
        intent: RetrievalIntent = RetrievalIntent.RECORD_SEARCH
    ): RetrievalResult = withContext(Dispatchers.IO) {
        if (query.isBlank() || scope.mode == ScopeMode.CONVERSATION_ONLY) {
            return@withContext RetrievalResult(
                citations = emptyList(),
                searchMode = RetrievalMode.NO_RECORDS_FOUND
            )
        }

        // 1. Resolve eligible document IDs by scope mode and date range
        val allDocs = documentDao.getAllDocumentsList(scope.profileId)
        if (allDocs.isEmpty()) {
            return@withContext RetrievalResult(
                citations = emptyList(),
                searchMode = RetrievalMode.NO_RECORDS_FOUND
            )
        }

        val scopedDocs = when (scope.mode) {
            ScopeMode.SPECIFIC_DOCS -> {
                if (scope.selectedDocumentIds.isEmpty()) return@withContext RetrievalResult(
                    citations = emptyList(),
                    searchMode = RetrievalMode.NO_RECORDS_FOUND
                )
                allDocs.filter { it.id in scope.selectedDocumentIds }
            }
            ScopeMode.ALL_HISTORY -> allDocs
            ScopeMode.CONVERSATION_ONLY -> emptyList()
        }
        // A report opened from the vault narrows the conversation scope; it must never widen it.
        val baseDocs = if (targetDocumentId != null) {
            scopedDocs.filter { it.id == targetDocumentId }
        } else {
            scopedDocs
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
            findStructuredBiomarkerHits(searchTerms, eligibleDocIds).filter { hit ->
                isWithinTimeRange(hit.testDate, scope)
            }
        } else {
            emptyList()
        }
        // Bound prompt evidence while favoring recent measurements for trend questions.
        val boundedStructuredHits = structuredHits.sortedByDescending { it.testDate }.take(12)

        if (intent == RetrievalIntent.BIOMARKER_HISTORY) {
            return@withContext RetrievalResult(
                citations = boundedStructuredHits.toCitations(eligibleDocs).take(topK.coerceAtMost(8)),
                searchMode = if (boundedStructuredHits.isEmpty()) RetrievalMode.NO_RECORDS_FOUND else RetrievalMode.STRUCTURED_SQL_ONLY,
                structuredBiomarkersFound = boundedStructuredHits.map { it.displayText }
            )
        }

        // Build lexical and semantic candidates together so exact values/names survive even
        // when the embedding model finds a plausible but incomplete semantic match.
        val requestedTopK = topK.coerceIn(1, 12)
        val candidateLimit = (requestedTopK * 3).coerceAtMost(36)
        val lexicalCandidates = linkedMapOf<Long, DocumentChunkEntity>()
        val lexicalScores = mutableMapOf<Long, Float>()
        val documentIdBatches = eligibleDocIds.toList().chunked(SQLITE_IN_BATCH_SIZE)
        val chunkCounts = documentIdBatches.map { documentDao.getChunkCountForDocuments(it) }
        val scopedChunkCount = chunkCounts.sum().coerceAtLeast(1)
        val averageChunkLength = documentIdBatches.mapIndexed { index, documentIds ->
            (documentDao.getAverageChunkLengthForDocuments(documentIds) ?: 0.0) * chunkCounts[index]
        }.sum().div(scopedChunkCount).toFloat().coerceAtLeast(1f)
        for (term in searchTerms.asSequence().filter { it.length >= 2 }.distinct().take(12)) {
            val matchQuery = "\"${term.replace("\"", "\"\"")}\""
            val hits = documentIdBatches.flatMap { documentIds ->
                documentDao.searchChunksAcrossDocumentsByFullText(documentIds, matchQuery)
            }
            // FTS4 matchinfo's global document frequency spans every local profile.
            // Count only returned rows from this frozen scope to keep BM25 statistics
            // profile-isolated.
            val documentFrequency = hits.size.toLong().coerceAtMost(scopedChunkCount.toLong())
            val inverseDocumentFrequency = ln(
                1.0 + (scopedChunkCount.toDouble() - documentFrequency + 0.5) / (documentFrequency + 0.5)
            ).toFloat()
            hits.forEach { candidate ->
                val chunk = candidate.chunk
                lexicalCandidates.putIfAbsent(chunk.id, chunk)
                val termFrequency = candidate.matchInfoValue(2).coerceAtLeast(1L).toFloat()
                val documentLength = chunk.tokenCount.coerceAtLeast(1).toFloat()
                val lengthNormalization = 1f - BM25_B + BM25_B * documentLength / averageChunkLength
                val termScore = inverseDocumentFrequency *
                    (termFrequency * (BM25_K1 + 1f)) / (termFrequency + BM25_K1 * lengthNormalization)
                lexicalScores[chunk.id] = (lexicalScores[chunk.id] ?: 0f) + termScore
            }
        }
        val rankedLexicalEntries = lexicalScores.entries
            .sortedByDescending { it.value }
            .take(candidateLimit)
        val rankedLexicalCandidates = rankedLexicalEntries.mapNotNull { (chunkId, _) -> lexicalCandidates[chunkId] }
        val lexicalRanks = rankedLexicalEntries.mapIndexed { rank, entry -> entry.key to rank }.toMap()

        val semanticRanks = linkedMapOf<Long, Pair<Int, Float>>()
        if (embeddingEngine.status == ModelInferenceStatus.READY) {
            val queryVector = embeddingEngine.generateEmbedding(query, isQuery = true)
            if (queryVector != null) {
                val semanticHits = documentIdBatches.flatMap { documentIds ->
                    val embeddings = ragDao.getEmbeddingsForDocuments(documentIds)
                    if (embeddings.isEmpty()) emptyList() else embeddingEngine.rankChunks(
                        queryVector = queryVector,
                        candidates = embeddings.map { it.chunkId to it.embeddingBlob },
                        topK = candidateLimit
                    ).filter { it.score >= MIN_SEMANTIC_SIMILARITY }
                }.sortedByDescending { it.score }.take(candidateLimit)
                semanticHits.forEachIndexed { rank, hit ->
                    semanticRanks[hit.chunkId] = rank to hit.score
                }
            }
        }

        if (rankedLexicalCandidates.isEmpty() && semanticRanks.isEmpty() && eligibleDocIds.size == 1 && searchTerms.isEmpty()) {
            documentDao.getChunksForDocument(eligibleDocIds.first()).take(candidateLimit)
                .forEachIndexed { rank, chunk ->
                    lexicalCandidates[chunk.id] = chunk
                    lexicalScores[chunk.id] = 1f / (60 + rank)
                }
        }

        if (rankedLexicalCandidates.isNotEmpty() || semanticRanks.isNotEmpty()) {
            val candidates = rankedLexicalCandidates.associateBy { it.id }.toMutableMap()
            if (semanticRanks.isNotEmpty()) {
                ragDao.getChunksWithDocuments(semanticRanks.keys.toList())
                    .filter { it.document.id in eligibleDocIds }
                    .forEach { candidates.putIfAbsent(it.chunk.id, it.chunk) }
            }
            val ranked = candidates.values.mapNotNull { chunk ->
                val lexical = lexicalRanks[chunk.id]?.let { 1f / (60 + it) } ?: 0f
                val semantic = semanticRanks[chunk.id]
                val fusedScore = lexical + (semantic?.let { 1f / (60 + it.first) } ?: 0f)
                if (fusedScore == 0f) null else Triple(chunk, fusedScore, semantic?.second ?: 0f)
            }.sortedByDescending { it.second }.take(requestedTopK)

            val docMap = eligibleDocs.associateBy { it.id }
            val chunkCitations = ranked.map { (chunk, fusedScore, semanticScore) ->
                SourceCitation(
                    documentId = chunk.documentId,
                    documentName = docMap[chunk.documentId]?.fileName ?: "Document #${chunk.documentId}",
                    pageNumber = chunk.pageNumber,
                    chunkText = chunk.text,
                    similarityScore = semanticScore.takeIf { it > 0f } ?: fusedScore
                )
            }
            val structuredCitations = boundedStructuredHits.toCitations(eligibleDocs).take(requestedTopK)
            val citations = (structuredCitations + chunkCitations.take((requestedTopK - structuredCitations.size).coerceAtLeast(0)))
                .distinctBy { it.documentId to it.chunkText }
                .take(requestedTopK)
            val hasSemantic = ranked.any { it.third > 0f }
            val hasLexical = ranked.any { lexicalScores.containsKey(it.first.id) }
            return@withContext RetrievalResult(
                citations = citations,
                searchMode = when {
                    hasSemantic && hasLexical -> RetrievalMode.HYBRID
                    hasSemantic -> RetrievalMode.SEMANTIC_VECTOR
                    else -> RetrievalMode.LOCAL_KEYWORD_FALLBACK
                },
                structuredBiomarkersFound = boundedStructuredHits.map { it.displayText }
            )
        }

        // 5. Structured SQL hits only (if any exist)
        if (structuredHits.isNotEmpty()) {
            val structuredCitations = boundedStructuredHits.toCitations(eligibleDocs).take(requestedTopK)

            return@withContext RetrievalResult(
                citations = structuredCitations,
                searchMode = RetrievalMode.STRUCTURED_SQL_ONLY,
                structuredBiomarkersFound = boundedStructuredHits.map { it.displayText }
            )
        }

        // 6. No relevant records found within scope
        RetrievalResult(
            citations = emptyList(),
            searchMode = RetrievalMode.NO_RECORDS_FOUND,
            structuredBiomarkersFound = emptyList()
        )
    }

    /** Returns file-level matches so the planner can locate a report before searching its contents. */
    suspend fun findMedicalDocuments(
        query: String,
        targetDocumentId: Long? = null,
        scope: ChatContextScope = ChatContextScope(),
        offset: Int = 0,
        limit: Int = 3
    ): RetrievalResult = withContext(Dispatchers.IO) {
        if (scope.mode == ScopeMode.CONVERSATION_ONLY) {
            return@withContext RetrievalResult(emptyList(), RetrievalMode.NO_RECORDS_FOUND)
        }

        val terms = extractSearchTerms(query).toSet()
        val now = System.currentTimeMillis()
        val documents = documentDao.getAllDocumentsList(scope.profileId).filter { document ->
            val inScope = when (scope.mode) {
                ScopeMode.CONVERSATION_ONLY -> false
                ScopeMode.ALL_HISTORY -> true
                ScopeMode.SPECIFIC_DOCS -> document.id in scope.selectedDocumentIds
            }
            val matchesTarget = targetDocumentId == null || document.id == targetDocumentId
            val date = document.reportDate ?: document.importDate
            val inDateRange = when (scope.timeRange) {
                TimeRangeFilter.ALL_TIME -> true
                TimeRangeFilter.LAST_30_DAYS -> date >= now - 30L * 24 * 3600 * 1000
                TimeRangeFilter.LAST_3_MONTHS -> date >= now - 90L * 24 * 3600 * 1000
                TimeRangeFilter.LAST_12_MONTHS -> date >= now - 365L * 24 * 3600 * 1000
                TimeRangeFilter.CUSTOM -> date in (scope.customStartDate ?: 0L)..(scope.customEndDate ?: Long.MAX_VALUE)
            }
            inScope && matchesTarget && inDateRange
        }

        val rankedDocuments = documents.mapNotNull { document ->
            val name = document.fileName.lowercase(Locale.ROOT)
            val score = terms.count { term -> name.contains(term.lowercase(Locale.ROOT)) }
            if (terms.isNotEmpty() && score == 0) null else document to score
        }.sortedWith(
            compareByDescending<Pair<com.swasthai.app.data.local.entity.DocumentEntity, Int>> { it.second }
                .thenByDescending { it.first.importDate }
        )
        val safeOffset = offset.coerceAtLeast(0)
        val safeLimit = limit.coerceIn(1, MAX_TOOL_PAGE_SIZE)
        val page = rankedDocuments.drop(safeOffset).take(safeLimit)

        val files = page.map { (document, _) -> document.toMedicalFileEntry() }
        RetrievalResult(
            citations = emptyList(),
            searchMode = if (files.isEmpty()) RetrievalMode.NO_RECORDS_FOUND else RetrievalMode.LOCAL_KEYWORD_FALLBACK,
            hasMore = safeOffset + page.size < rankedDocuments.size,
            nextOffset = (safeOffset + page.size).takeIf { safeOffset + page.size < rankedDocuments.size },
            files = files
        )
    }

    /** Lists the in-scope local report catalog in import chronology for agent navigation. */
    suspend fun listMedicalDocuments(
        targetDocumentId: Long? = null,
        scope: ChatContextScope = ChatContextScope(),
        offset: Int = 0,
        limit: Int = 10
    ): RetrievalResult = withContext(Dispatchers.IO) {
        val safeOffset = offset.coerceAtLeast(0)
        val safeLimit = limit.coerceIn(1, MAX_TOOL_PAGE_SIZE)
        val now = System.currentTimeMillis()
        val (startDate, endDate) = when (scope.timeRange) {
            TimeRangeFilter.ALL_TIME -> 0L to Long.MAX_VALUE
            TimeRangeFilter.LAST_30_DAYS -> (now - 30L * 24 * 3600 * 1000) to Long.MAX_VALUE
            TimeRangeFilter.LAST_3_MONTHS -> (now - 90L * 24 * 3600 * 1000) to Long.MAX_VALUE
            TimeRangeFilter.LAST_12_MONTHS -> (now - 365L * 24 * 3600 * 1000) to Long.MAX_VALUE
            TimeRangeFilter.CUSTOM -> (scope.customStartDate ?: 0L) to (scope.customEndDate ?: Long.MAX_VALUE)
        }
        val rows = if (scope.mode == ScopeMode.CONVERSATION_ONLY) emptyList() else documentDao.getTimelinePage(
            profileId = scope.profileId,
            allHistory = scope.mode == ScopeMode.ALL_HISTORY,
            selectedDocumentIds = scope.selectedDocumentIds.toList(),
            targetDocumentId = targetDocumentId,
            startDate = startDate,
            endDate = endDate,
            limit = safeLimit + 1,
            offset = safeOffset
        )
        val hasMore = rows.size > safeLimit
        val page = rows.take(safeLimit)
        val files = page.map { it.toMedicalFileEntry() }
        RetrievalResult(
            citations = emptyList(),
            searchMode = if (files.isEmpty()) RetrievalMode.NO_RECORDS_FOUND else RetrievalMode.LOCAL_KEYWORD_FALLBACK,
            hasMore = hasMore,
            nextOffset = (safeOffset + page.size).takeIf { hasMore },
            files = files
        )
    }

    /** Reads extracted page chunks only after rechecking profile, document, target, and date scope. */
    suspend fun readMedicalDocument(
        documentId: Long,
        pageNumber: Int,
        targetDocumentId: Long? = null,
        scope: ChatContextScope = ChatContextScope(),
        chunkOffset: Int = 0,
        limit: Int = 10
    ): RetrievalResult = withContext(Dispatchers.IO) {
        if (scope.mode == ScopeMode.CONVERSATION_ONLY ||
            (scope.mode == ScopeMode.SPECIFIC_DOCS && documentId !in scope.selectedDocumentIds) ||
            (targetDocumentId != null && documentId != targetDocumentId)
        ) return@withContext RetrievalResult(emptyList(), RetrievalMode.NO_RECORDS_FOUND)

        val document = documentDao.getDocumentById(documentId, scope.profileId)
            ?: return@withContext RetrievalResult(emptyList(), RetrievalMode.NO_RECORDS_FOUND)
        val date = document.reportDate ?: document.importDate
        val now = System.currentTimeMillis()
        val inDateScope = when (scope.timeRange) {
            TimeRangeFilter.ALL_TIME -> true
            TimeRangeFilter.LAST_30_DAYS -> date >= now - 30L * 24 * 3600 * 1000
            TimeRangeFilter.LAST_3_MONTHS -> date >= now - 90L * 24 * 3600 * 1000
            TimeRangeFilter.LAST_12_MONTHS -> date >= now - 365L * 24 * 3600 * 1000
            TimeRangeFilter.CUSTOM -> date in (scope.customStartDate ?: 0L)..(scope.customEndDate ?: Long.MAX_VALUE)
        }
        if (!inDateScope) return@withContext RetrievalResult(emptyList(), RetrievalMode.NO_RECORDS_FOUND)

        val safeOffset = chunkOffset.coerceAtLeast(0)
        val safeLimit = limit.coerceIn(1, MAX_TOOL_PAGE_SIZE)
        val rows = documentDao.getChunksForDocumentPage(documentId, pageNumber, safeLimit + 1, safeOffset)
        val hasMore = rows.size > safeLimit
        val chunks = rows.take(safeLimit)
        val citations = chunks.map { chunk ->
            SourceCitation(
                documentId = document.id,
                documentName = document.fileName,
                pageNumber = chunk.pageNumber,
                chunkText = chunk.text,
                similarityScore = 1.0f
            )
        }
        RetrievalResult(
            citations,
            if (citations.isEmpty()) RetrievalMode.NO_RECORDS_FOUND else RetrievalMode.LOCAL_KEYWORD_FALLBACK,
            hasMore = hasMore,
            nextOffset = (safeOffset + chunks.size).takeIf { hasMore }
        )
    }

    private data class StructuredHit(
        val measurementId: Long,
        val documentId: Long,
        val displayText: String,
        val sourceText: String,
        val pageNumber: Int?,
        val testDate: Long
    )

    private fun isWithinTimeRange(timestamp: Long, scope: ChatContextScope): Boolean {
        val now = System.currentTimeMillis()
        return when (scope.timeRange) {
            TimeRangeFilter.ALL_TIME -> true
            TimeRangeFilter.LAST_30_DAYS -> timestamp >= now - 30L * 24 * 3600 * 1000
            TimeRangeFilter.LAST_3_MONTHS -> timestamp >= now - 90L * 24 * 3600 * 1000
            TimeRangeFilter.LAST_12_MONTHS -> timestamp >= now - 365L * 24 * 3600 * 1000
            TimeRangeFilter.CUSTOM -> timestamp in (scope.customStartDate ?: 0L)..(scope.customEndDate ?: Long.MAX_VALUE)
        }
    }

    private fun List<StructuredHit>.toCitations(eligibleDocs: List<com.swasthai.app.data.local.entity.DocumentEntity>): List<SourceCitation> {
        val docMap = eligibleDocs.associateBy { it.id }
        return mapNotNull { hit ->
            val page = hit.pageNumber ?: return@mapNotNull null
            SourceCitation(
                documentId = hit.documentId,
                documentName = docMap[hit.documentId]?.fileName ?: "Lab Record #${hit.documentId}",
                pageNumber = page,
                chunkText = hit.sourceText,
                similarityScore = 0.95f
            )
        }
    }

    private fun RankedChunkCandidate.matchInfoValue(index: Int): Long {
        val byteOffset = index * Int.SIZE_BYTES
        if (byteOffset + Int.SIZE_BYTES > matchInfo.size) return 0L
        return ByteBuffer.wrap(matchInfo)
            .order(ByteOrder.nativeOrder())
            .getInt(byteOffset)
            .toLong() and 0xffff_ffffL
    }

    private fun com.swasthai.app.data.local.entity.DocumentEntity.toMedicalFileEntry() = MedicalFileEntry(
        documentId = id,
        fileName = fileName,
        importedAt = importDate,
        reportDate = reportDate,
        pageCount = pageCount,
        indexingStatus = indexingStatus,
        failureReason = failureReason
    )

    private suspend fun findStructuredBiomarkerHits(
        searchTerms: List<String>,
        allowedDocIds: Set<Long>
    ): List<StructuredHit> {
        val hits = mutableListOf<StructuredHit>()
        val chunksByDocument = mutableMapOf<Long, List<DocumentChunkEntity>>()

        for (term in searchTerms) {
            val measurements = measurementDao.getMeasurementsByBiomarker(term)
            val filtered = measurements.filter { it.documentId in allowedDocIds }
            for (m in filtered) {
                val refStr = if (m.referenceLow != null && m.referenceHigh != null) {
                    " (reported range ${m.referenceLow} - ${m.referenceHigh}; not clinically normalized)"
                } else ""
                val dateSource = if (m.testDateSource == "IMPORT_DATE_FALLBACK") " (report date not found; import date used)" else ""
                val comparator = when (m.valueComparator) {
                    "<" -> "< "
                    "<=" -> "≤ "
                    ">" -> "> "
                    ">=" -> "≥ "
                    else -> ""
                }
                val codeCandidate = m.canonicalCode?.let { " (unreviewed LOINC code candidate $it)" } ?: ""
                val chunks = chunksByDocument.getOrPut(m.documentId) {
                    documentDao.getChunksForDocument(m.documentId)
                }
                val sourceChunk = chunks.firstOrNull { chunk ->
                    chunk.text.contains(m.rawText, ignoreCase = true)
                }
                hits.add(
                    StructuredHit(
                        measurementId = m.id,
                        documentId = m.documentId,
                        displayText = "UNREVIEWED EXTRACTION — ${SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(m.testDate))}$dateSource — ${m.biomarkerName}: $comparator${m.numericValue} ${m.unit}$refStr$codeCandidate; verify against the source report.",
                        sourceText = m.rawText,
                        pageNumber = m.sourcePageNumber ?: sourceChunk?.pageNumber,
                        testDate = m.testDate
                    )
                )
            }
        }
        return hits.distinctBy { it.measurementId }
    }
}
