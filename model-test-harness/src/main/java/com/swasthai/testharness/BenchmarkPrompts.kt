package com.swasthai.testharness

/**
 * Standardized, repeatable benchmark prompts and self-contained multi-turn templates
 * for Phase 0 MedGemma 1.5 4B IT evaluation on Samsung Galaxy S25.
 *
 * NOTE: These prompts are designed for objective technical benchmarking (latency,
 * throughput, memory, coherence), NOT for clinical diagnosis or validation.
 */
object BenchmarkPrompts {

    /**
     * Test 1: Short General Health-Education Prompt
     * Context: Short prompt (~20 tokens), testing general biological concept explanation.
     */
    const val PROMPT_1_GENERAL_HEALTH =
        "What is the primary function of high-density lipoprotein (HDL) cholesterol in cardiovascular health?"

    /**
     * Test 2: Medical Terminology & Pathophysiology Prompt
     * Context: Medium prompt (~25 tokens), testing clinical vocabulary explanation in accessible language.
     */
    const val PROMPT_2_MEDICAL_TERMINOLOGY =
        "Explain what microcytic hypochromic anemia indicates in a complete blood count panel and its common physiological causes."

    /**
     * Test 3: Synthetic Report-Grounded Prompt
     * Context: Longer context prompt (~120 tokens), testing closed-world numerical interpretation
     * without statistical hallucination.
     */
    const val PROMPT_3_REPORT_GROUNDED = """
You are an educational healthcare companion. Given this excerpt from a verified synthetic patient laboratory report:
[REPORT EXCERPT]
Patient: Ramesh Verma | UHID: SYNTH-88219 | Date: 2025-01-15
- Total Cholesterol: 248 mg/dL [Reference Range: Desirable < 200 mg/dL]
- Triglycerides: 195 mg/dL [Reference Range: Normal < 150 mg/dL]
- Fasting Blood Sugar: 138 mg/dL [Reference Range: Normal 70 - 100 mg/dL]
- HbA1c: 7.2 % [Reference Range: Normal < 5.7 %, Diabetic >= 6.5 %]
- Serum Creatinine: 0.95 mg/dL [Reference Range: Normal 0.70 - 1.30 mg/dL]

Identify which measurements are outside standard reference ranges and explain in plain language what these specific elevations indicate. Do not provide a diagnosis.
""".trimIndent()

    /**
     * Test 4: Application-Prepared Self-Contained Prompt (Addressing Single-Model Multi-Turn Limitation)
     *
     * Official MedGemma limitation: Model card explicitly notes MedGemma has NOT been evaluated
     * or optimized for native multi-turn conversation.
     *
     * Architectural Mitigation:
     * Instead of relying on raw multi-turn chat templates with conversational drift, the application
     * layer constructs a self-contained single-turn prompt containing:
     * 1. Compact Conversation Summary (Turn history)
     * 2. Relevant Retrieved Evidence
     * 3. Current User Question
     *
     * LiteRT-LM applies the bundled model chat template to this single user prompt, preventing
     * duplicate conversation-control tokens while staying within the ekv2048 token boundary.
     */
    fun buildSelfContainedMultiTurnPrompt(
        conversationSummary: String,
        relevantEvidence: String,
        currentUserQuestion: String
    ): String {
        return buildString {
            appendLine("You are SwasthAI, an on-device personal healthcare companion.")
            appendLine("Review the following prior conversation context and verified medical evidence to answer the current question.")
            appendLine()
            appendLine("[CONVERSATION HISTORY SUMMARY]")
            appendLine(conversationSummary.ifBlank { "No prior conversation history." })
            appendLine()
            if (relevantEvidence.isNotBlank()) {
                appendLine("[VERIFIED MEDICAL EVIDENCE]")
                appendLine(relevantEvidence)
                appendLine()
            }
            appendLine("[CURRENT USER QUESTION]")
            appendLine(currentUserQuestion)
            appendLine()
            appendLine("Provide a clear, grounded explanation in simple language. If the question cannot be answered from the evidence, state so clearly.")
        }
    }

    /**
     * Sample 3-Turn Multi-Turn Validation Scenario for Task 4
     */
    val SAMPLE_SCENARIO_SUMMARY = """
User previously discussed high triglycerides (195 mg/dL on Jan 15, 2025 report). Assistant explained that triglycerides are blood fats influenced by dietary sugars and simple carbohydrates.
""".trimIndent()

    val SAMPLE_SCENARIO_QUESTION_TURN_2 =
        "What specific dietary modifications can help lower these elevated blood fats over the next 3 to 6 months?"
}
