# SwasthAI: Comprehensive Feature Catalog

**Document Version:** 1.1.0  
**Classification:** Product Engineering Inventory  
**Status:** Approved for Implementation Planning (Revised Single Generative Model Architecture)  

---

## 1. Feature Taxonomy & Priority Structure

Each feature in this catalog is systematically documented with:
* **Feature ID:** Standardized domain identifier.
* **Name & Purpose:** Clear user-facing utility and clinical/operational rationale.
* **Priority Tier:**
  * **P0:** Essential Core MVP (Mandatory for physical device hackathon demonstration).
  * **P1:** High-Value Expansion (Enhances clinical completeness; immediate post-MVP).
  * **P2:** Advanced Feature (Long-term product differentiation).
  * **Research Gate (RG):** Contingent on empirical feasibility and performance validation on Galaxy S25.
* **Dependencies:** Preceding features, libraries, models, and runtime prerequisites.
* **Technical Requirements:** Models, database entities, APIs, and memory allocations.
* **Safety Implications:** Clinical risk mitigation, uncertainty handling, and guardrail rules.
* **Demonstration Value:** Direct impact during hackathon live presentation and judging.
* **Acceptance Criteria:** Precise, testable operational pass conditions.

---

## 2. Feature Group A: Local AI Chat

### FT-A01: Single On-Device Generative Engine (MedGemma 1.5 4B IT)
* **Purpose:** Serves as the single generative intelligence for general health conversation, medical terminology explanation, and report summarization without relying on cloud servers.
* **Priority:** **P0**
* **Dependencies:** LiteRT-LM runtime, MedGemma 1.5 4B IT quantized model artifact (`.litertlm`).
* **Technical Requirements:** LiteRT-LM Android binding, dynamic KV cache, hardware acceleration delegate (GPU/NPU/CPU), text-only instruction format.
* **Safety Implications:** System prompt strictly prohibits definitive diagnostic declarations; disclaimers enforced.
* **Demonstration Value:** **High:** Shows immediate, offline responsive chat in airplane mode.
* **Acceptance Criteria:** Generates contextually coherent health responses on Galaxy S25; zero network packets transmitted.

### FT-A01A: Multi-Turn History Management & Turn Formatting
* **Purpose:** Overcomes MedGemma's documented limitation (not natively tuned for multi-turn chat) by structuring conversational turns in the application layer.
* **Priority:** **P0**
* **Dependencies:** FT-A01, Room Message History.
* **Technical Requirements:** Conversation manager serializing the last 3–5 turns into a standardized `<start_of_turn>user ... <end_of_turn><start_of_turn>model ...` prompt sequence.
* **Safety Implications:** Prevents conversational context drift and confusion across turns.
* **Demonstration Value:** **High:** Enables coherent follow-up dialogue without needing a second model.
* **Acceptance Criteria:** Successfully resolves contextual references (e.g., "What about my second test?") across 5 consecutive chat turns.

### FT-A02: Real-Time Token Streaming & Cancellation
* **Purpose:** Delivers progressive token rendering to minimize perceived user latency and allows immediate cancellation if the user reframes their prompt.
* **Priority:** **P0**
* **Dependencies:** FT-A01, Kotlin Coroutines `Flow<String>`, Jetpack Compose.
* **Technical Requirements:** Asynchronous token listener attached to LiteRT-LM generator loop; thread-safe interrupt call.
* **Safety Implications:** Aborting generation clears in-flight speculative memory and prevents UI lockups.
* **Demonstration Value:** **Medium:** Creates a polished, responsive consumer-grade feel.
* **Acceptance Criteria:** Tapping "Stop" halts token emission within 100ms; releases active compute resources.

### FT-A03: Persistent Conversation Lifecycle & Branching
* **Purpose:** Stores chat threads with auto-generated titles, timestamps, and message history in SQLite.
* **Priority:** **P0**
* **Dependencies:** Room Database, Android Keystore encryption.
* **Technical Requirements:** `ConversationEntity`, `MessageEntity`, foreign key cascades, room pagination.
* **Safety Implications:** Messages marked as containing sensitive lab citations persist under encrypted storage.
* **Demonstration Value:** **Medium:** Demonstrates continuity across app restarts.
* **Acceptance Criteria:** App process termination and restart restores exact chat state with 100% fidelity.

### FT-A04: Grounding Badge & Source Evidence Drawer
* **Purpose:** Clearly separates general AI knowledge from facts retrieved from personal medical records.
* **Priority:** **P0**
* **Dependencies:** FT-A01, FT-E05 (RAG Citations).
* **Technical Requirements:** Custom Compose UI tag rendering inline chip: *"Answer grounded in [Report Date]"*; click event opens bottom sheet citation viewer.
* **Safety Implications:** Prevents user confusion between general statistical medical facts and personal test numbers.
* **Demonstration Value:** **Very High:** Visual differentiator proving real RAG vs generic LLM responses.
* **Acceptance Criteria:** Every response containing retrieved chunks renders the interactive Grounding Badge.

---

## 3. Feature Group B: Medical Vault

### FT-B01: Multi-Format Document Ingestion (PDF & Scanned Images)
* **Purpose:** Centralized, secure local repository for all diagnostic reports, discharge summaries, and prescriptions.
* **Priority:** **P0**
* **Dependencies:** Android Storage Access Framework (SAF), Android App-Private Storage.
* **Technical Requirements:** System Document Picker intent; file size validation (< 50MB); MIME-type filtering (PDF, JPEG, PNG, HEIC).
* **Safety Implications:** Files copied to internal sandbox (`context.filesDir/vault/`); original user files left untouched.
* **Demonstration Value:** **High:** Allows judges to import real or synthetic lab reports live.
* **Acceptance Criteria:** Successful ingestion of a 10-page PDF report within 1.5 seconds.

### FT-B02: Hybrid Text Extraction Pipeline (Native PDF + Local OCR)
* **Purpose:** Extracts raw text and structural coordinates from digital PDFs and scanned paper reports entirely on-device.
* **Priority:** **P0**
* **Dependencies:** FT-B01, ML Kit Text Recognition (offline bundle) or Tesseract, Android PdfRenderer.
* **Technical Requirements:** Dual-path extractor: checks for embedded text stream first; falls back to bitmap rendering + OCR if text stream is empty.
* **Safety Implications:** Poor OCR quality flags document as "Low Confidence", preventing silent hallucination of lab numbers.
* **Demonstration Value:** **High:** Ingests both crisp digital PDFs and camera-captured physical lab printouts.
* **Acceptance Criteria:** OCR character recognition accuracy >= 90% on standard 300 DPI synthetic blood report scans.

### FT-B03: Document Provenance & Original Artifact Preservation
* **Purpose:** Guarantees that the original, unmodified PDF/image is permanently preserved for visual verification.
* **Priority:** **P0**
* **Dependencies:** FT-B01, Room Database (`DocumentEntity`).
* **Technical Requirements:** SHA-256 hash generated on ingestion; immutable file path stored in Room; built-in PDF viewer with pan/zoom.
* **Safety Implications:** User can always audit AI statements against the true signed clinical laboratory report.
* **Demonstration Value:** **High:** Tapping a citation immediately highlights the real document on screen.
* **Acceptance Criteria:** Document hash remains identical before and after extraction and embedding workflows.

### FT-B04: Atomic Document Deletion & Cascade Purge
* **Purpose:** Complies with privacy-first principles by permanently wiping all traces of a document on user command.
* **Priority:** **P0**
* **Dependencies:** Room Database, Vector Store, Local File System.
* **Technical Requirements:** Room `@Delete` with cascading foreign keys: purges `DocumentChunkEntity`, `EmbeddingEntity`, `StructuredMeasurementEntity`, and physical binary file.
* **Safety Implications:** Complete removal ensures deleted records are never surfaced in subsequent RAG queries.
* **Demonstration Value:** **Medium:** Demonstrates privacy and GDPR/DPDP user data sovereignty.
* **Acceptance Criteria:** Zero orphaned chunks or embeddings remain in SQLite/vector storage post-deletion.

---

## 4. Feature Group C: Medical Report Simplifier

### FT-C01: Deterministic Biomarker & Measurement Parser
* **Purpose:** Parses complex tabular laboratory reports into structured numerical data (Biomarker, Value, Unit, Range).
* **Priority:** **P0**
* **Dependencies:** FT-B02 (Extracted Text), Regex Pattern Matcher.
* **Technical Requirements:** Rule-based parser for standard Indian/International diagnostic templates (Thyroid, CBC, Lipid, Metabolic).
* **Safety Implications:** MedGemma explains verified data rather than discovering numbers from raw text.
* **Demonstration Value:** **High:** Transforms messy PDF tables into clean, structured UI cards.
* **Acceptance Criteria:** Correctly parses >= 95% of synthetic CBC/Lipid panel fields in benchmark test set.

### FT-C02: Deterministic Reference Range Evaluation & Visual Flagging
* **Purpose:** Evaluates whether a lab value is Low, Normal, High, or Critical using deterministic mathematical comparison.
* **Priority:** **P0**
* **Dependencies:** FT-C01, Room Database (`StructuredMeasurementEntity`).
* **Technical Requirements:** Mathematical parser comparing `observedValue` against `referenceRangeLow` and `referenceRangeHigh`.
* **Safety Implications:** Prevents LLM statistical hallucination of what is "normal" or "dangerous".
* **Demonstration Value:** **High:** Clear color-coded badges (Green = Normal, Amber = Borderline, Red = High/Low).
* **Acceptance Criteria:** 100% deterministic accuracy for numerical range checks across test suites.

### FT-C03: Plain-Language Biomarker Demystifier (Powered by MedGemma)
* **Purpose:** Translates medical jargon (e.g., "Mean Corpuscular Volume", "Serum Bilirubin") into clear physiological functions.
* **Priority:** **P0**
* **Dependencies:** FT-C01, MedGemma 1.5 4B IT / Curated Medical Glossary.
* **Technical Requirements:** MedGemma grounded prompt injecting structured test details; fallback verified offline dictionary for top 200 lab tests.
* **Safety Implications:** Explanations explicitly state that out-of-range values do not equate to a definitive disease diagnosis.
* **Demonstration Value:** **High:** Non-technical users immediately understand their reports.
* **Acceptance Criteria:** Explanations pass readability target (comprehensible to 8th grader).

---

## 5. Feature Group D: Longitudinal Medical History

### FT-D01: Four-Tier Provenance Tracking & Audit Trail
* **Purpose:** Explicitly tags and tracks every health fact, medication, condition, and measurement by its source and verification status.
* **Priority:** **P0**
* **Dependencies:** Room Database Architecture.
* **Technical Requirements:** `ProvenanceType` enum (`USER_DECLARED`, `EXTRACTED_UNVERIFIED`, `EXTRACTED_CONFIRMED`, `AI_INFERRED`) attached to all relational entities.
* **Safety Implications:** AI inferences never silently upgrade into verified patient medical history.
* **Demonstration Value:** **High:** Showcases medical-grade safety and transparency.
* **Acceptance Criteria:** UI clearly distinguishes unconfirmed extractions from confirmed user history.

### FT-D02: Multi-Report Biomarker Trend Comparison
* **Purpose:** Graphically and tabularly compares identical biomarkers across two or more historical dates.
* **Priority:** **P0**
* **Dependencies:** FT-C01, FT-C02, Jetpack Compose Canvas / Charting.
* **Technical Requirements:** SQL query joining measurements on `canonicalTestName` ordered by `reportDate ASC`; dynamic percentage change calculation.
* **Safety Implications:** Normalized units check: prevents comparing mg/dL with mmol/L without explicit unit conversion.
* **Demonstration Value:** **Very High:** Core hackathon demo scenario (comparing Jan 2025 vs Aug 2025 blood tests).
* **Acceptance Criteria:** Accurately calculates delta and directional trend between two historical reports.

---

## 6. Feature Group E: Genuine Local RAG

### FT-E01: Layout-Aware Semantic Document Chunking
* **Purpose:** Splits medical documents into coherent, context-preserving segments without severing test names from results.
* **Priority:** **P0**
* **Dependencies:** FT-B02 (Extracted Text).
* **Technical Requirements:** Custom parser recognizing tabular rows, section headers, and paragraph boundaries; chunk size 256–512 tokens with 50-token overlap.
* **Safety Implications:** Preserving test name + value + range in a single chunk eliminates cross-line attribution hallucinations.
* **Demonstration Value:** **Medium:** Technical foundation for retrieval accuracy.
* **Acceptance Criteria:** Zero chunks split a biomarker name from its corresponding numerical value.

### FT-E02: Dedicated Local Vector Embedding Generation (EmbeddingGemma 300M)
* **Purpose:** Generates semantic vectors for document chunks and user queries locally.
* **Priority:** **P0**
* **Dependencies:** LiteRT runtime, EmbeddingGemma 300M model artifact.
* **Technical Requirements:** 768-dimensional float32/int8 embeddings; $L_2$ normalization; model resident RAM < 200 MB.
* **Safety Implications:** Fully local; embeddings run entirely in-process on CPU/GPU. Distinct from generative LLM inference.
* **Demonstration Value:** **High:** Validates genuine on-device semantic retrieval.
* **Acceptance Criteria:** Embedding generation latency target < 35ms per 256-token chunk on Snapdragon 8 Elite.

### FT-E03: High-Performance On-Device Vector Store
* **Purpose:** Indexes and retrieves chunk embeddings with sub-30ms latency.
* **Priority:** **P0**
* **Dependencies:** FT-E02, SQLite / In-Memory Cosine Similarity Engine.
* **Technical Requirements:** Memory-mapped vector cache with cosine similarity search; metadata foreign key mapping to `DocumentChunkEntity`.
* **Safety Implications:** Strictly filtered by user-selected document scope when requested.
* **Demonstration Value:** **High:** Instant retrieval across 500+ document chunks.
* **Acceptance Criteria:** Top-5 nearest neighbors retrieved in < 30ms for 1,000 vectors.

### FT-E04: Hybrid Retrieval Engine (Vector Semantic + Structured SQL)
* **Purpose:** Routes queries intelligently: conceptual queries to vector search; temporal/numerical queries to deterministic SQL.
* **Priority:** **P0**
* **Dependencies:** FT-E03, Room Relational Database.
* **Technical Requirements:** Query intent router: detects entities and dates; combines top-k semantic chunks with structured measurement rows into MedGemma prompt.
* **Safety Implications:** Eliminates vector "fuzziness" when user asks for exact historical numbers.
* **Demonstration Value:** **Very High:** Superior accuracy over purely vector-based RAG implementations.
* **Acceptance Criteria:** Successfully answers "What was my highest cholesterol?" using SQL aggregate + grounded explanation.

### FT-E05: Verifiable Source Footnotes & Document Coordinate Jump
* **Purpose:** Links generated sentences to source chunks and opens the original document at the exact coordinate.
* **Priority:** **P0**
* **Dependencies:** FT-A04, FT-B03, FT-E04.
* **Technical Requirements:** Prompt instruction demanding bracketed citations `[1]`; parser resolves `[1]` to `ChunkId` -> `DocumentId` + `PageNumber` + `BoundingBox`.
* **Safety Implications:** Enables user to verify any AI claim against the clinical ground truth.
* **Demonstration Value:** **Very High:** Direct visual proof of unhallucinated answers.
* **Acceptance Criteria:** Tapping `[1]` scrolls the built-in document viewer directly to the cited lab result on page N.

---

## 7. Feature Group F: Medical Chat With Documents

### FT-F01: Explicit Document Focus & Multi-Report Scoping
* **Purpose:** Allows user to select specific reports (e.g., "Compare Report A and Report B") or query the entire vault.
* **Priority:** **P0**
* **Dependencies:** FT-A01, FT-B01, FT-E04.
* **Technical Requirements:** UI document selector chips; passes `filterDocumentIds` to retrieval pipeline.
* **Safety Implications:** Prevents irrelevant historical records from polluting the context window.
* **Demonstration Value:** **High:** Clean user control over conversational context.
* **Acceptance Criteria:** Retrieval strictly constrained to selected document IDs when filter is active.

### FT-F02: Negative Evidence Handling & "Out of Scope" Signaling
* **Purpose:** Explicitly informs the user when a question cannot be answered from saved records, preventing fabrication.
* **Priority:** **P0**
* **Dependencies:** FT-E04.
* **Technical Requirements:** Cosine distance cutoff (> 0.45 threshold); prompt instruction for missing evidence.
* **Safety Implications:** Eliminates confabulation of tests that were never performed or uploaded.
* **Demonstration Value:** **High:** Establishes trust and clinical rigor.
* **Acceptance Criteria:** MedGemma states "No records found" when queried about an unperformed test.

---

## 8. Feature Group G: Multilingual Health Assistant

### FT-G01: Hindi Plain-Language Medical Explanations
* **Purpose:** Generates empathetic, easy-to-understand explanations of lab reports in Hindi for vernacular users.
* **Priority:** **P1**
* **Dependencies:** FT-C03, MedGemma 1.5 4B IT Tokenizer.
* **Technical Requirements:** Hindi system prompt templates; Devanagari Unicode rendering in Compose UI.
* **Safety Implications:** Clinical drug names and exact numbers retained in Latin/transliterated script.
* **Demonstration Value:** **High:** Broadens accessibility and inclusivity.
* **Acceptance Criteria:** Hindi explanations evaluated as clear, culturally natural, and clinically accurate.

### FT-G02: Regional Indian Language Decoupling (Tamil/Telugu/Bengali)
* **Purpose:** Extensible localization framework allowing plug-and-play regional Indian languages.
* **Priority:** **P2**
* **Dependencies:** FT-G01, Android String Resources.
* **Technical Requirements:** Dynamic locale switcher; externalized prompt catalog.
* **Safety Implications:** Strict verification required before advertising any regional dialect as supported.
* **Demonstration Value:** **Medium:** Architectural readiness for 1B+ non-English speakers.
* **Acceptance Criteria:** Clean separation of localization keys from business logic.

---

## 9. Feature Group H: Smart Symptom Guidance

### FT-H01: Deterministic Pre-LLM Emergency Escalation (India 112 Standard)
* **Purpose:** Instantly intercepts life-threatening emergencies (chest pain, stroke, severe bleeding, anaphylaxis) before calling MedGemma.
* **Priority:** **P0**
* **Dependencies:** Deterministic Regex & Keyword Classifier, Android Dialer Intent.
* **Technical Requirements:** Pre-execution regex filter; bypasses LLM; displays full-screen Emergency Dialog with direct call to `112`.
* **Safety Implications:** **Critical:** Zero latency, zero hallucination risk during acute medical crises.
* **Demonstration Value:** **Very High:** Highlight of the safety and healthcare engineering architecture.
* **Acceptance Criteria:** 100% interception of 20 clinical emergency scenarios in < 10ms.

### FT-H02: Structured Symptom Intake & Clarification Flow
* **Purpose:** Guides users through structured questions (duration, onset, severity, accompanying symptoms) for non-emergency complaints.
* **Priority:** **P1**
* **Dependencies:** FT-A01, FT-H01.
* **Technical Requirements:** State machine prompting user for structured qualifiers before generating educational guidance.
* **Safety Implications:** Refuses to suggest specific prescription treatments; consistently recommends seeing a physician.
* **Demonstration Value:** **Medium:** Demonstrates responsible conversational flow.
* **Acceptance Criteria:** Conversational flow collects duration and severity before providing lifestyle or doctor visit advice.

---

## 10. Feature Group I: Agentic Health Workflows

### FT-I01: Bounded Tool-Calling Architecture with Strict JSON Schema
* **Purpose:** Allows MedGemma to perform specific, sandboxed app tasks via structured function calls.
* **Priority:** **P1**
* **Dependencies:** FT-A01, Kotlin Serialization.
* **Technical Requirements:** Tool parser handling: `search_vault`, `get_trends`, `create_reminder`, `draft_summary`.
* **Safety Implications:** Zero unrestricted OS access; all arguments strictly type-checked against Kotlin dataclasses.
* **Demonstration Value:** **High:** Shows modern agentic capabilities running 100% locally.
* **Acceptance Criteria:** Model correctly outputs validated JSON tool calls matching schema.

### FT-I02: Mandatory Human-in-the-Loop Confirmation Gate
* **Purpose:** Ensures no state-altering action occurs without explicit user review and button press.
* **Priority:** **P1**
* **Dependencies:** FT-I01, Jetpack Compose Dialogs.
* **Technical Requirements:** Intercepts state-modifying tool calls; renders an actionable "Approve / Cancel" card in the chat.
* **Safety Implications:** Prevents unauthorized schedule creation or data modifications by model completions.
* **Demonstration Value:** **High:** Best-practice AI safety demonstration.
* **Acceptance Criteria:** Reminder or record creation executes *only* after user taps "Confirm".

---

## 11. Feature Group J: Preventive Healthcare

### FT-J01: Local Medication & Appointment Reminders
* **Purpose:** Schedules private, offline notifications for daily medications and upcoming doctor visits.
* **Priority:** **P1**
* **Dependencies:** Android `WorkManager`, `AlarmManager`, Notification Manager.
* **Technical Requirements:** Local alarm scheduling with battery-efficient wake locks; snooze and mark-as-taken buttons.
* **Safety Implications:** Prescription-extracted reminders require user confirmation before activation.
* **Demonstration Value:** **Medium:** Demonstrates day-to-day patient utility.
* **Acceptance Criteria:** Notification fires accurately at scheduled local time in airplane mode.

### FT-J02: Preventive Care Dashboard & Timeline
* **Purpose:** Displays an integrated timeline of past lab tests, current medications, and upcoming screening checkups.
* **Priority:** **P1**
* **Dependencies:** Room Database, Jetpack Compose.
* **Technical Requirements:** Unified `TimelineItem` query aggregating documents, measurements, and reminders chronologically.
* **Safety Implications:** Read-only view; clear provenance badges.
* **Demonstration Value:** **High:** Visual dashboard showcasing complete personal health record.
* **Acceptance Criteria:** Renders complete chronological timeline smoothly.

---

## 12. Feature Group K: Medical Summary Export

### FT-K01: Structured Doctor Visit Summary Generator
* **Purpose:** Synthesizes confirmed conditions, current medications, allergies, and recent abnormal lab tests into a 1-page summary.
* **Priority:** **P1**
* **Dependencies:** FT-D01, FT-C01, Android `PdfDocument` / Print Framework.
* **Technical Requirements:** Local PDF canvas renderer; generates clean, print-ready A4 medical summary with provenance footnotes.
* **Safety Implications:** User must preview and approve the summary before sharing via Android Share Sheet.
* **Demonstration Value:** **High:** Tangible output for clinical visits.
* **Acceptance Criteria:** Generates crisp vector PDF summary within 2 seconds.

---

## 13. Feature Group L: Privacy & Security

### FT-L01: Zero-Network Air-Gap Enforcement
* **Purpose:** Guarantees absolute data sovereignty by design.
* **Priority:** **P0**
* **Dependencies:** Android Manifest Configuration.
* **Technical Requirements:** Absence of `android.permission.INTERNET` in release manifest; all assets bundled or loaded via internal storage.
* **Safety Implications:** Eliminates data breach, tracking, and telemetry exfiltration vectors.
* **Demonstration Value:** **Critical:** Core privacy pillar of SwasthAI.
* **Acceptance Criteria:** Zero outbound network connections verified via network profiler during full test suite.

### FT-L02: Hardware-Backed Encryption at Rest (Android Keystore + AES-GCM)
* **Purpose:** Protects stored health databases, original reports, and vectors against physical device theft or root compromise.
* **Priority:** **P0**
* **Dependencies:** Android Keystore System (StrongBox / TEE), SQLCipher / EncryptedFile.
* **Technical Requirements:** 256-bit AES master key generated in StrongBox; wraps database and document storage encryption.
* **Safety Implications:** Ensures complete compliance with healthcare data protection standards.
* **Demonstration Value:** **High:** Highlights enterprise-grade mobile security architecture.
* **Acceptance Criteria:** Raw database files unreadable if extracted via ADB without hardware key unwrapping.

---

## 14. Feature Group M: UX & Accessibility

### FT-M01: Modern, Intuitive Health Companion Dashboard
* **Purpose:** Delivers a warm, accessible, highly responsive mobile experience for users of all technical proficiencies.
* **Priority:** **P0**
* **Dependencies:** Jetpack Compose, Material Design 3, Accessible Color Palette.
* **Technical Requirements:** High-contrast text; minimum 48dp touch targets; dynamic font scaling support.
* **Safety Implications:** Clear visual hierarchy prevents misinterpreting normal vs abnormal test values.
* **Demonstration Value:** **High:** Premium, polished app presentation for judges.
* **Acceptance Criteria:** Passes Android Accessibility Scanner with zero high-severity contrast or touch-target flags.

---

## 15. Feature Group N: Future Multimodal Interaction (Research Gate)

### FT-N01: Live Camera & Voice Real-Time Consultation (Long-Term R&D)
* **Purpose:** Live continuous video and bidirectional audio exploration.
* **Priority:** **Research Gate (Strictly Deferred / Non-MVP)**
* **Dependencies:** Future Gemini Live on-device models, TEN VAD, Smart Turn audio framing, Box integration.
* **Status:** **Postponed.** Must NOT be implemented, downloaded, or permitted to block core MVP delivery.
* **Future Gate Requirement:** Requires empirical proof that real-time audio/video inference can run on Galaxy S25 without thermal throttling after 3 minutes.
