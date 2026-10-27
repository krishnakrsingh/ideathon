package com.swasthai.app

import com.swasthai.app.ai.ClinicalKnowledgeEngine
import com.swasthai.app.ai.EmbeddingEngine
import com.swasthai.app.ai.RAGPipeline
import com.swasthai.app.domain.model.SourceCitation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalKnowledgeEngineTest {

    @Test
    fun testRecordGroundedSynthesisWithCitations() {
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

        val response = ClinicalKnowledgeEngine.generateClinicalResponse(
            query = "What is my hemoglobin level?",
            citations = citations,
            structuredBiomarkers = structured
        )

        assertTrue("Should contain evidence headline", response.contains("saved health records") || response.contains("Analysis"))
        assertTrue("Should include structured lab measurement", response.contains("Hemoglobin: 14.8 g/dL"))
        assertTrue("Should reference the source document", response.contains("Sample_Comprehensive_Health_Panel"))
        assertTrue("Should explain hemoglobin role", response.contains("Hemoglobin"))
    }

    @Test
    fun testGeneralBloodPressureGuide() {
        val response = ClinicalKnowledgeEngine.generateClinicalResponse(
            query = "What is normal blood pressure?",
            citations = emptyList(),
            structuredBiomarkers = emptyList()
        )

        assertTrue(response.contains("Systolic"))
        assertTrue(response.contains("Diastolic"))
        assertTrue(response.contains("120"))
        assertTrue(response.contains("80"))
    }

    @Test
    fun testSearchTermsExtractionInRAGPipeline() {
        val terms = RAGPipeline.extractSearchTerms("What is the normal reference range for my hemoglobin level?")
        assertTrue(terms.contains("hemoglobin"))
        assertFalse(terms.contains("what"))
        assertFalse(terms.contains("the"))
        assertFalse(terms.contains("for"))
        assertFalse(terms.contains("normal"))
    }
}
