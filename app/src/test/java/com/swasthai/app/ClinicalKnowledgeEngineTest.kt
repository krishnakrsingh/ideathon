package com.swasthai.app

import com.swasthai.app.ai.ExecutedMedicalTool
import com.swasthai.app.ai.RetrievalMode
import com.swasthai.app.ai.RetrievalResult
import com.swasthai.app.domain.model.SourceCitation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalKnowledgeEngineTest {

    @Test
    fun testRetrievalResultAggregationAndFormatting() {
        val citations = listOf(
            SourceCitation(
                documentId = 101L,
                documentName = "Sample_Comprehensive_Health_Panel.pdf",
                pageNumber = 1,
                chunkText = "Hemoglobin: 14.8 g/dL (13.5 - 17.5). White Blood Cells (WBC): 6.8 x10^3/uL.",
                similarityScore = 1.0f
            )
        )
        val structured = listOf("Hemoglobin: 14.8 g/dL (Ref: 13.5 - 17.5)")
        val tools = listOf(
            ExecutedMedicalTool("search_medical_records", 2, "HYBRID")
        )

        val result = RetrievalResult(
            citations = citations,
            searchMode = RetrievalMode.HYBRID,
            structuredBiomarkersFound = structured,
            executedTools = tools
        )

        assertEquals(RetrievalMode.HYBRID, result.searchMode)
        assertEquals(1, result.citations.size)
        assertEquals("Sample_Comprehensive_Health_Panel.pdf", result.citations[0].documentName)
        assertEquals(1, result.structuredBiomarkersFound.size)
        assertEquals(1, result.executedTools.size)
        assertEquals("search_medical_records", result.executedTools[0].name)
    }

    @Test
    fun testExecutedMedicalToolTracking() {
        val trace = ExecutedMedicalTool(
            name = "get_biomarker_history",
            resultCount = 3,
            retrievalMode = RetrievalMode.HYBRID.name
        )

        assertEquals("get_biomarker_history", trace.name)
        assertEquals(3, trace.resultCount)
        assertEquals("HYBRID", trace.retrievalMode)
    }
}
