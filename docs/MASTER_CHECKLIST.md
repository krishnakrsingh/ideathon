# SwasthAI: Master Engineering Implementation Checklist

**Document Version:** 1.2.0  
**Classification:** Actionable Implementation Task Inventory  
**Status:** Phase 0 Feasibility Findings Updated (Test Harness Created)  

> **Notation & Status Legend:**  
> - `[x]` Completed & Verified  
> - `[ ]` Not Started  
> - `[R]` Ready to Implement (Zero blockers, architecture approved)  
> - `[B]` Blocked on Preceding Task / Feasibility Gate  
> - Priorities: `P0` (Core MVP), `P1` (High-Value), `P2` (Extended), `RG` (Research Gate)  

---

## 1. Product Definition & Architecture Governance
- [x] **TSK-ARC-01** [P0] Approve Product Brief & Single-Model Strategy ([00_PROJECT_BRIEF.md](file:///c:/Users/krish/Downloads/ideathon/docs/00_PROJECT_BRIEF.md)). *Verified vision, single MedGemma 1.5 4B IT generative candidate, separate EmbeddingGemma 2 Text 270M, and hardware constraints.*
- [x] **TSK-ARC-02** [P0] Approve Product Requirements Document ([01_PRODUCT_REQUIREMENTS.md](file:///c:/Users/krish/Downloads/ideathon/docs/01_PRODUCT_REQUIREMENTS.md)). *Verified all functional/non-functional requirements and target acceptance metrics.*
- [x] **TSK-ARC-03** [P0] Approve System Architecture & Single-Model Orchestration ([04_SYSTEM_ARCHITECTURE.md](file:///c:/Users/krish/Downloads/ideathon/docs/04_SYSTEM_ARCHITECTURE.md)). *Verified module boundaries and sequential execution pipeline.*
- [x] **TSK-ARC-04** [P0] Approve Healthcare AI Safety Charter ([08_HEALTHCARE_SAFETY.md](file:///c:/Users/krish/Downloads/ideathon/docs/08_HEALTHCARE_SAFETY.md)). *Verified deterministic emergency rules and 112 India standard.*

---

## 2. Research & Phase 0 Feasibility Spike
- [x] **TSK-RES-01** [P0] Execute LiteRT / LiteRT-LM Android Integration Spike. *Verified `com.google.ai.edge.litertlm:litertlm-android:0.18.0` and `com.google.ai.edge.litert:litert:2.3.0` dependencies; verified physical Galaxy S25 connected via ADB (`RZGL325K28P`, `SM-S931B`).*
- [x] **TSK-RES-02A** [P0] Verify MedGemma 1.5 4B IT Community Artifact. *Identified and verified `litert-community/MedGemma-1.5-4B-IT` (`medgemma-1.5-4b-it_q4_block32_ekv2048.litertlm`, 2.41 GiB) under HAI-DEF gating on Hugging Face.*
- [x] **TSK-RES-02B** [P0] Build Minimal Model Test Harness (`:model-test-harness`). *Created isolated test application module with Compose UI, `LiteRtModelTester`, real token streaming, and benchmark metrics.*
- [ ] **TSK-RES-02C** [P0] [R] **Execute Gate 1 On-Device Benchmark on Galaxy S25**. *Push 2.41 GiB model to `/sdcard/Download/`; run test harness; record cold-start, TTFT, tps, and peak RSS on device.*
- [x] **TSK-RES-03** [P0] Implement Multi-Turn Self-Contained Prompting. *Constructed and validated `BenchmarkPrompts.buildSelfContainedMultiTurnPrompt` (summary + evidence + question) in test harness.*
- [x] **TSK-RES-04** [P0] Validate EmbeddingGemma 2 Text 270M Artifact & Prefixes. *Identified `litert-community/embeddinggemma-2-text-270m-litert-lm`; implemented `EmbeddingGemmaProbe` with official prefixes and 768-dim normalization.*
- [ ] **TSK-RES-05** [P0] [R] Evaluate ML Kit Offline OCR on Synthetic Blood Reports. *Verify character accuracy >= 90% across Latin and Devanagari test scans.*

---

## 3. UI/UX & Design System
- [ ] **TSK-UI-01** [P0] [R] Implement Core Material 3 Design System (`:core:designsystem`). *Define color palettes (accessible dark/light modes), custom typography (Inter/Roboto), and elevation tokens.*
- [ ] **TSK-UI-02** [P0] [R] Build Reusable Health UI Components. *Create Biomarker Status Badge (Green/Amber/Red), Grounding Footnote Chip `[1]`, and Document Preview Card.*
- [ ] **TSK-UI-03** [P0] [R] Implement App Navigation Scaffold & Top-Level Bottom Bar. *Configure Jetpack Compose NavHost with 4 tabs: Dashboard, Chat, Medical Vault, Care Planner.*
- [ ] **TSK-UI-04** [P0] [R] Implement Full-Screen Emergency Alert Dialog. *Create high-contrast red alert modal with persistent vibration and one-tap `Call 112` button.*
- [ ] **TSK-UI-05** [P0] [R] Enforce `FLAG_SECURE` on Sensitive Health Screens. *Attach window security flag to Medical Vault and Chat screens to block unauthorized screencasting/screenshots.*

---

## 4. Android Foundation & Security Layer
- [ ] **TSK-SEC-01** [P0] [R] Configure Air-Gap Manifest Security. *Verify total absence of `android.permission.INTERNET` in `AndroidManifest.xml`.*
- [ ] **TSK-SEC-02** [P0] [R] Implement Qualcomm StrongBox Keymaster Wrapper (`:core:security`). *Generate hardware-backed 256-bit AES master key in Android Keystore.*
- [ ] **TSK-SEC-03** [P0] [R] Initialize Encrypted Room Database (`:core:database`). *Configure SQLCipher with dynamic key derivation from StrongBox master key.*
- [ ] **TSK-SEC-04** [P0] [R] Implement `EncryptedFile` Vault Storage Manager. *Provide atomic encrypted file read, write, and cryptographic shredding utilities in `context.filesDir/vault/`.*
- [ ] **TSK-SEC-05** [P0] [R] Configure Coroutine Dispatchers & Thread Isolation. *Establish dedicated single-threaded inference executor and IO dispatchers.*

---

## 5. Local AI Inference Engine (`:ai:runtime`, `:ai:generative`)
- [ ] **TSK-INF-01** [P0] [B: TSK-RES-02C] Implement Production `MedGemmaLlmEngine` Abstraction. *Port tested `LiteRtModelTester` logic into application domain layer.*
- [ ] **TSK-INF-02** [P0] [B: TSK-INF-01] Implement Real-Time Token Streaming with Flow. *Expose asynchronous `Flow<String>` token stream with dynamic UI updates.*
- [ ] **TSK-INF-03** [P0] [B: TSK-INF-01] Implement Cooperative Cancellation Mechanism. *Add thread-safe `abortGeneration()` method to halt active compute within 100ms.*
- [ ] **TSK-INF-04** [P0] [B: TSK-INF-01] Configure Device Delegate Priorities. *Configure Adreno GPU -> CPU XNNPACK fallback based on Gate 1 benchmark findings.*
- [ ] **TSK-INF-05** [P0] [B: TSK-INF-01] Implement Turn Re-Formatting Engine. *Serialize conversation history turns into `<start_of_turn>user ... <end_of_turn><start_of_turn>model ...` prompt.*

---

## 6. Medical Vault & Document Processing (`:feature:vault`)
- [ ] **TSK-VLT-01** [P0] [B: TSK-SEC-04] Implement SAF Document Picker Integration. *Allow user to select PDF, JPEG, PNG files; validate MIME types and file size (< 50MB).*
- [ ] **TSK-VLT-02** [P0] [B: TSK-VLT-01] Implement Document Ingestion & Hashing. *Compute SHA-256 hash, copy file to encrypted vault, and insert `DocumentEntity`.*
- [ ] **TSK-VLT-03** [P0] [B: TSK-VLT-02] Implement Digital PDF Text Extractor. *Extract text streams and page coordinate rectangles using native Android PDF parsing.*
- [ ] **TSK-VLT-04** [P0] [B: TSK-VLT-02] Implement Fallback On-Device OCR. *Render PDF page bitmaps via `PdfRenderer` and extract text blocks using ML Kit OCR.*
- [ ] **TSK-VLT-05** [P0] [B: TSK-VLT-03] Implement Document Extraction Review Sheet. *Allow user to inspect and edit extracted document date, lab provider, and title.*
- [ ] **TSK-VLT-06** [P0] [B: TSK-SEC-03] Implement Atomic Document Cascade Deletion. *Execute atomic transaction deleting document, chunks, embeddings, measurements, and physical file.*
- [ ] **TSK-VLT-07** [P0] [B: TSK-VLT-02] Build In-App PDF Document Viewer. *Render interactive PDF view with pinch-to-zoom and coordinate highlighting.*

---

## 7. Embedding Generation & Vector Store (`:ai:embedding`, `:ai:vectorstore`)
- [ ] **TSK-EMB-01** [P0] [B: TSK-RES-04] Implement `EmbeddingGemmaEngine`. *Load quantized 270M model; generate 768-dim float array vectors with $L_2$ normalization.*
- [ ] **TSK-EMB-02** [P0] [B: TSK-EMB-01] Implement Layout-Aware Semantic Chunker (`:ai:chunker`). *Segment text into 256–512 token chunks while keeping clinical table rows intact.*
- [ ] **TSK-EMB-03** [P0] [B: TSK-EMB-01] Build In-Process Cosine Similarity Vector Store. *Implement fast vector index with metadata mapping to `DocumentChunkEntity`.*
- [ ] **TSK-EMB-04** [P0] [B: TSK-EMB-03] Implement Vector Search Top-K Retrieval. *Execute nearest neighbor query returning top-K chunks exceeding cosine similarity threshold (0.55).*

---

## 8. Genuine Local RAG & Hybrid Retrieval (`:ai:rag`)
- [ ] **TSK-RAG-01** [P0] [B: TSK-EMB-04] Implement Hybrid Query Router. *Analyze query intent; route numerical/temporal inquiries to SQL and conceptual inquiries to vectors.*
- [ ] **TSK-RAG-02** [P0] [B: TSK-RAG-01] Build Grounded Context Assembler. *Assemble retrieved chunks and structured measurements into closed-world prompt with citations `[1]`, `[2]`.*
- [ ] **TSK-RAG-03** [P0] [B: TSK-RAG-02] Implement Negative Retrieval Handling. *Output standardized "No records found" when chunk similarity falls below 0.55.*
- [ ] **TSK-RAG-04** [P0] [B: TSK-RAG-02] Implement Interactive Citation Drawer in Chat. *Render Grounding Badge; clicking opens bottom sheet with source document, page, and snippet.*
- [ ] **TSK-RAG-05** [P0] [B: TSK-RAG-04] Implement Document Coordinate Jump. *Tapping "View Source" in citation drawer opens PDF viewer focused on exact page and coordinate.*

---

## 9. Structured Medical History & Simplifier (`:feature:simplifier`, `:feature:history`)
- [ ] **TSK-SIM-01** [P0] [B: TSK-VLT-03] Implement Deterministic Biomarker Regex Parser. *Extract canonical test name, observed value, unit, and reference range from lab text.*
- [ ] **TSK-SIM-02** [P0] [B: TSK-SIM-01] Implement Deterministic Range Comparator. *Compute LOW, NORMAL, HIGH, CRITICAL flags mathematically from printed reference range.*
- [ ] **TSK-SIM-03** [P0] [B: TSK-SIM-02] Implement MedGemma Biomarker Demystifier. *Prompt MedGemma with structured test details to explain biological function.*
- [ ] **TSK-SIM-04** [P0] [B: TSK-SIM-02] Implement Multi-Report Trend Comparison Engine. *Perform SQL join on matching biomarkers across two dates; calculate delta and percentage change.*
- [ ] **TSK-SIM-05** [P0] [B: TSK-SIM-04] Build Report Comparison UI Grid. *Render visual comparison cards with delta indicators and MedGemma narrative summary.*
- [ ] **TSK-SIM-06** [P0] [B: TSK-SEC-03] Implement Four-Tier Provenance Audit Architecture. *Tag all records with `USER_DECLARED`, `EXTRACTED_UNVERIFIED`, `EXTRACTED_CONFIRMED`, or `AI_INFERRED`.*

---

## 10. Healthcare AI Safety & Emergency Controls (`:ai:safety`)
- [ ] **TSK-SAF-01** [P0] [R] Implement Pre-LLM Deterministic Emergency Filter. *Intercept acute chest pain, stroke, anaphylaxis, severe bleeding before LLM invocation.*
- [ ] **TSK-SAF-02** [P0] [B: TSK-SAF-01] Implement Emergency Dialer Intent (India 112). *Connect Emergency Alert Modal to `Intent.ACTION_DIAL` pre-filled with `tel:112`.*
- [ ] **TSK-SAF-03** [P0] [B: TSK-INF-02] Implement Post-Generation Numerical Fact Checker. *Extract numbers + units from generated text; verify presence in retrieved context; flag ungrounded values.*
- [ ] **TSK-SAF-04** [P0] [R] Enforce Mandatory Non-Diagnostic Disclaimers. *Attach persistent, accessible disclaimers to conversational and report views.*
- [ ] **TSK-SAF-05** [P0] [R] Implement Medication Alteration Refusal Guardrail. *Refuse dosage modification questions and direct user to prescribing physician.*

---

## 11. Multilingual Support (`:feature:localization`)
- [ ] **TSK-LNG-01** [P1] [B: TSK-INF-02] Implement Hindi Medical Explanation Prompts for MedGemma. *Craft and evaluate culturally natural Hindi system prompts for report simplification.*
- [ ] **TSK-LNG-02** [P1] [B: TSK-LNG-01] Enforce Medical Entity Preservation in Hindi. *Guarantee that drug brand names, lab test canonical names, and units remain in Latin script.*
- [ ] **TSK-LNG-03** [P1] [R] Implement In-App Language Switcher. *Provide seamless toggle between English and Hindi in app toolbar.*

---

## 12. Care Planning & Bounded Agentic Workflows (`:feature:careplanner`)
- [ ] **TSK-AGT-01** [P1] [B: TSK-INF-02] Implement Bounded Tool Schemas & Parser. *Define strict JSON schemas for MedGemma: `search_vault`, `get_biomarkers`, `create_reminder`.*
- [ ] **TSK-AGT-02** [P1] [B: TSK-AGT-01] Implement Human-in-the-Loop Confirmation Gate. *Intercept state-mutating tool calls; render interactive confirmation card in chat.*
- [ ] **TSK-AGT-03** [P1] [B: TSK-AGT-02] Implement Local Notification Engine. *Schedule medication reminders using Android `WorkManager` / `AlarmManager` without cloud push.*
- [ ] **TSK-AGT-04** [P1] [B: TSK-SIM-06] Implement Doctor Visit Summary PDF Generator. *Synthesize confirmed health facts and recent abnormal tests into a clean 1-page vector PDF.*

---

## 13. Testing, Performance & Verification
- [ ] **TSK-TST-01** [P0] [B: TSK-SIM-02] Execute Unit Test Suite for Parsers & Range Evaluators. *Run JUnit tests across 50 simulated clinical test rows.*
- [ ] **TSK-TST-02** [P0] [B: TSK-SAF-01] Execute 20 Emergency Clinical Safety Tests. *Verify 100% deterministic interception of red-line prompts.*
- [ ] **TSK-TST-03** [P0] [B: TSK-SEC-01] Conduct Network Profiler Air-Gap Audit. *Verify 0 bytes transmitted during full 30-minute user session on Galaxy S25.*
- [ ] **TSK-TST-04** [P0] [B: TSK-RES-02C] **Execute Reproducible Galaxy S25 Performance Benchmark Suite**. *Record cold-start, warm-start, TTFT, tokens/sec, peak RSS, and thermals.* (See [10_TEST_PLAN_AND_ACCEPTANCE.md](file:///c:/Users/krish/Downloads/ideathon/docs/10_TEST_PLAN_AND_ACCEPTANCE.md))
- [ ] **TSK-TST-05** [P0] [B: TSK-RES-02C] **Execute Single-Model Quality Validation Suite (10 Dimensions)**. *Evaluate ordinary chat, multi-turn follow-ups, grounding, citations, numerical fidelity, Hindi quality.*

---

## 14. Hackathon Polish & Submission Assets
- [ ] **TSK-HCK-01** [P0] [R] Load Synthetic Patient Datasets into Project Assets. *Include `synthetic_report_jan2025.pdf`, `synthetic_report_aug2025.pdf`, and prescription.*
- [ ] **TSK-HCK-02** [P0] [B: TSK-TST-05] Rehearse 3-Minute Live Hackathon Demonstration. *Execute flawless walkthrough in Airplane Mode according to [11_DEMO_AND_HACKATHON_PLAN.md](file:///c:/Users/krish/Downloads/ideathon/docs/11_DEMO_AND_HACKATHON_PLAN.md).*
- [ ] **TSK-HCK-03** [P0] [R] Finalize One-Slide Impact Plan Graphic. *Render visual impact summary detailing early adoption, clinical safety, and privacy metrics.*
- [ ] **TSK-HCK-04** [P0] [B: TSK-HCK-02] Record & Edit 60-Second Video Demonstration. *Capture physical device interaction and overlay voiceover based on the storyboard.*
- [ ] **TSK-HCK-05** [P0] [R] Complete Hackathon Submission Form & Repository README. *Verify all project documentation links are functional and clear.*
