package com.swasthai.app

import android.content.Context
import com.swasthai.app.ai.MedGemmaManager
import com.swasthai.app.domain.model.ChatMessage
import com.swasthai.app.domain.model.MessageRole
import com.swasthai.app.domain.model.SourceCitation
import org.junit.Assert.assertTrue
import org.junit.Test

class MedGemmaPromptBuilderTest {

    @Test
    fun testSelfContainedPromptAssemblyWithCitationsAndHistory() {
        val manager = MedGemmaManager()

        val history = listOf(
            ChatMessage(
                conversationId = 1L,
                role = MessageRole.USER,
                content = "Hi, can you review my lab tests?"
            ),
            ChatMessage(
                conversationId = 1L,
                role = MessageRole.ASSISTANT,
                content = "Certainly! Please ask about any specific measurement or document."
            )
        )

        val citations = listOf(
            SourceCitation(
                documentId = 55L,
                documentName = "Blood_Report_2026.pdf",
                pageNumber = 2,
                chunkText = "Platelet Count: 245000 /mcL (150000 - 450000). Hemoglobin: 14.5 g/dL.",
                similarityScore = 0.92f
            )
        )

        val prompt = manager.buildSelfContainedPrompt(
            currentQuery = "What was my platelet count in the 2026 report?",
            history = history,
            retrievedCitations = citations
        )

        // Verify key structural elements
        assertTrue(prompt.contains("RETRIEVED MEDICAL RECORDS & EVIDENCE"))
        assertTrue(prompt.contains("Source: Blood_Report_2026.pdf, Page: 2"))
        assertTrue(prompt.contains("Platelet Count: 245000 /mcL"))
        assertTrue(prompt.contains("RECENT CONVERSATION CONTEXT"))
        assertTrue(prompt.contains("User: Hi, can you review my lab tests?"))
        assertTrue(prompt.contains("User Question: What was my platelet count in the 2026 report?"))
        assertTrue(prompt.contains("SwasthAI Response:"))
    }
}
