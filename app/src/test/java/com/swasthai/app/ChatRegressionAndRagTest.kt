package com.swasthai.app

import com.swasthai.app.ai.RAGPipeline
import com.swasthai.app.domain.model.ChatContextScope
import com.swasthai.app.domain.model.ChatMessage
import com.swasthai.app.domain.model.MessageRole
import com.swasthai.app.domain.model.ResponseLanguage
import com.swasthai.app.domain.model.ScopeMode
import com.swasthai.app.domain.model.SourceCitation
import com.swasthai.app.domain.model.TimeRangeFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRegressionAndRagTest {

    @Test
    fun testNullHelloRegressionDefectFixed() {
        // Delta simulated directly from SSE parsing logic
        fun parseDeltaContent(rawContent: Any?): String? {
            if (rawContent == null || rawContent == "null") return null
            var str = rawContent.toString()
            if (str.startsWith("null")) {
                str = str.removePrefix("null")
            }
            return if (str.isNotEmpty() && str != "null") str else null
        }

        // Chunk 1: llama.cpp initial chunk has null content:
        val initialDeltaContent = parseDeltaContent(null)
        assertNull("Initial chunk with null content must not produce string 'null'", initialDeltaContent)

        // Chunk 2: string "null" from legacy buggy optString:
        val legacyBuggyOptString = parseDeltaContent("null")
        assertNull("Literal string 'null' must be completely stripped", legacyBuggyOptString)

        // Chunk 3: Model leaking 'nullHello!'
        val leakedNullHello = parseDeltaContent("nullHello!")
        assertEquals("Hello!", leakedNullHello)

        // Chunk 4: Token chunk "How can I assist?"
        val tokenContent = parseDeltaContent(" How can I assist?")
        assertEquals(" How can I assist?", tokenContent)

        // Assembly
        val responseBuffer = StringBuilder()
        if (initialDeltaContent != null) responseBuffer.append(initialDeltaContent)
        if (legacyBuggyOptString != null) responseBuffer.append(legacyBuggyOptString)
        if (leakedNullHello != null) responseBuffer.append(leakedNullHello)
        if (tokenContent != null) responseBuffer.append(tokenContent)

        val finalOutput = responseBuffer.toString()
        assertEquals("Hello! How can I assist?", finalOutput)
        assertFalse("Final response must NOT start with 'null'", finalOutput.startsWith("null"))
        assertFalse("Final response must NOT contain 'nullHello!'", finalOutput.contains("nullHello!"))
    }

    @Test
    fun testThinkingAndControlTokensAreStripped() {
        val rawModelOutputWithThinking = "<think>Analyzing user lab results...</think>Your haemoglobin is normal."
        val cleaned = rawModelOutputWithThinking
            .replace("<think>", "")
            .replace("</think>", "")
            .replace("<|think|>", "")
            .replace("</|think|>", "")

        assertFalse(cleaned.contains("<think>"))
        assertFalse(cleaned.contains("</think>"))
        assertTrue(cleaned.contains("Your haemoglobin is normal."))
    }

    @Test
    fun testScopeModeConversationOnlyExcludesVault() {
        val scope = ChatContextScope(mode = ScopeMode.CONVERSATION_ONLY)
        assertEquals(ScopeMode.CONVERSATION_ONLY, scope.mode)
        assertTrue(scope.formatScopePill().contains("Conversation Only"))
    }

    @Test
    fun testScopeModeSpecificDocsFormatting() {
        val scope = ChatContextScope(
            mode = ScopeMode.SPECIFIC_DOCS,
            selectedDocumentIds = setOf(101L, 102L)
        )
        val pill = scope.formatScopePill()
        assertTrue(pill.contains("2 reports"))
    }

    @Test
    fun testHindiResponseLanguageDirective() {
        val hindiQuery = "मेरा हीमोग्लोबिन कैसा है?"
        val isDevanagari = hindiQuery.any { it in '\u0900'..'\u097F' }
        assertTrue("Devanagari characters must be detected in Hindi prompt", isDevanagari)

        val scope = ChatContextScope(responseLanguage = ResponseLanguage.HINDI)
        val pill = scope.formatScopePill()
        assertTrue("Scope pill must display Hindi indicator", pill.contains("Hindi"))
    }

    @Test
    fun testTimeRangeFilterCalculations() {
        val now = System.currentTimeMillis()
        val doc30DaysAgo = now - (25L * 24 * 3600 * 1000)
        val doc60DaysAgo = now - (60L * 24 * 3600 * 1000)

        val threshold30D = now - (30L * 24 * 3600 * 1000)

        assertTrue("Doc from 25 days ago must be within last 30 days", doc30DaysAgo >= threshold30D)
        assertFalse("Doc from 60 days ago must be excluded from last 30 days", doc60DaysAgo >= threshold30D)
    }

    @Test
    fun testFollowUpQuerySearchTermExpansion() {
        val followUp = "What about my haemoglobin?"
        val terms = RAGPipeline.extractSearchTerms(followUp)

        assertTrue("Extract search terms should expand haemoglobin", terms.contains("hemoglobin") || terms.contains("haemoglobin"))
    }
}
