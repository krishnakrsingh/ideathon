package com.swasthai.app

import com.swasthai.app.ai.LfmManager
import com.swasthai.app.domain.model.ChatMessage
import com.swasthai.app.domain.model.MessageRole
import com.swasthai.app.domain.model.ResponseLanguage
import com.swasthai.app.domain.model.SourceCitation
import org.junit.Assert.assertTrue
import org.junit.Test

class LfmPromptBuilderTest {

    @Test
    fun testLfmSystemPromptAssemblyWithCitationsAndHistory() {
        val manager = LfmManager()

        val citations = listOf(
            SourceCitation(
                documentId = 55L,
                documentName = "Blood_Report_2026.pdf",
                pageNumber = 2,
                chunkText = "Platelet Count: 245000 /mcL (150000 - 450000). Fasting Glucose: 92 mg/dL.",
                similarityScore = 0.94f
            )
        )

        val measurements = listOf(
            "Fasting Blood Glucose: 92.0 mg/dL (Normal: 70 - 99 mg/dL)",
            "Total Cholesterol: 185.0 mg/dL (Normal: 125 - 200 mg/dL)"
        )

        val systemPrompt = manager.buildSystemPrompt(
            language = ResponseLanguage.ENGLISH,
            currentQuery = "What does my glucose show?",
            citations = citations,
            allHistoryMeasurements = measurements
        )

        // Verify key structural elements
        assertTrue(systemPrompt.contains("Dr. Swasth"))
        assertTrue(systemPrompt.contains("Fasting Blood Glucose: 92.0 mg/dL"))
        assertTrue(systemPrompt.contains("RETRIEVED MEDICAL RECORD EVIDENCE"))
        assertTrue(systemPrompt.contains("Blood_Report_2026.pdf, Page 2"))
        assertTrue(systemPrompt.contains("Platelet Count: 245000 /mcL"))
    }

    @Test
    fun testHindiSystemPromptDirective() {
        val manager = LfmManager()
        val prompt = manager.buildSystemPrompt(
            language = ResponseLanguage.HINDI,
            currentQuery = "कृपया मेरी रिपोर्ट समझाइए"
        )

        assertTrue(prompt.contains("हिन्दी"))
        assertTrue(prompt.contains("Devanagari"))
    }
}
