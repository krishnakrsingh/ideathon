# Graph Report - ideathon  (2026-10-28)

## Corpus Check
- 106 files · ~80,589 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1177 nodes · 2292 edges · 75 communities (71 shown, 4 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 45 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `927761a7`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- DocumentRepository
- ChatRepository.kt
- MainActivity.kt
- VaultViewModel
- design.md
- DocumentChunker
- EmbeddingEngine
- SettingsViewModel
- ChatViewModel
- SwasthAI: Healthcare AI Safety, Clinical Guardrails & Evidence Grounding
- 3. Functional Requirements (FR)
- SwasthAI: Detailed User Flows & Interaction Journeys
- 3. Single-Model Quality Validation Suite (MedGemma 1.5 4B IT)
- 5. Complete Relational Database Schema (Room / SQLite)
- StructuredMeasurement
- 1. Confirmed Architecture Decision Records (ADRs)
- SwasthAI: Project Brief & Foundational Blueprint
- SwasthAI: Master Engineering Implementation Checklist
- SwasthAI: Privacy Architecture, Threat Model & Security Specification
- SwasthAI: Comprehensive System Architecture & Engineering Blueprint
- 2. Phase Breakdown
- SwasthAI: Model and Inference Runtime Plan
- download_models.sh script
- SwasthAI: Comprehensive Feature Catalog
- SwasthAI: Hackathon Demonstration Script, Video Storyboard & Impact Plan
- SwasthAI: Offline-First, Privacy-Focused AI Healthcare Companion
- 2. Feature Group A: Local AI Chat
- 6. Feature Group E: Genuine Local RAG
- 3. Feature Group B: Medical Vault
- 4. Feature Group C: Medical Report Simplifier
- gradlew
- 10. Feature Group I: Agentic Health Workflows
- 11. Feature Group J: Preventive Healthcare
- 13. Feature Group L: Privacy & Security
- 5. Feature Group D: Longitudinal Medical History
- 7. Feature Group F: Medical Chat With Documents
- 8. Feature Group G: Multilingual Health Assistant
- 9. Feature Group H: Smart Symptom Guidance
- main.py
- AccountFamilyRepository
- ChatViewModel.kt
- EmergencyTriage
- SwasthApplication
- HomeViewModel.kt
- MedicationReminderEntity
- SourceCitation
- ConversationDao
- ProfileKeyManager
- EncryptedPayloadEnvelope
- Gemma4ToolCallingModel
- MedicalRecordToolDefinitions
- MedicalDocument
- ModelInferenceStatus
- Row
- package.json
- MainAppContent
- ChatScreen.kt
- ChatContextScope
- Screen
- ChatTurnStatus
- Dp
- Text
- RetrievalMode
- auth.mjs
- run-local.sh
- SwasthAI local API
- run-local-auth.sh
- swasthai-api

## God Nodes (most connected - your core abstractions)
1. `AccountFamilyRepository` - 46 edges
2. `SettingsViewModel` - 46 edges
3. `_connection()` - 42 edges
4. `Text` - 31 edges
5. `ChatContextScope` - 28 edges
6. `ChatViewModel` - 28 edges
7. `DocumentRepository` - 27 edges
8. `ModelInferenceStatus` - 26 edges
9. `DocumentDao` - 25 edges
10. `ConversationDao` - 22 edges

## Surprising Connections (you probably didn't know these)
- `AppleChatMessageBubble()` --calls--> `Text`  [INFERRED]
  app/src/main/java/com/swasthai/app/ui/chat/ChatScreen.kt → app/src/main/java/com/swasthai/app/ai/MedicalRagOrchestrator.kt
- `ChatScreen()` --calls--> `Text`  [INFERRED]
  app/src/main/java/com/swasthai/app/ui/chat/ChatScreen.kt → app/src/main/java/com/swasthai/app/ai/MedicalRagOrchestrator.kt
- `AppBottomNav()` --calls--> `Text`  [INFERRED]
  app/src/main/java/com/swasthai/app/ui/components/AppBottomNav.kt → app/src/main/java/com/swasthai/app/ai/MedicalRagOrchestrator.kt
- `AppTopBar()` --calls--> `Text`  [INFERRED]
  app/src/main/java/com/swasthai/app/ui/components/AppTopBar.kt → app/src/main/java/com/swasthai/app/ai/MedicalRagOrchestrator.kt
- `CitationInspectionDialog()` --calls--> `Text`  [INFERRED]
  app/src/main/java/com/swasthai/app/ui/components/EmergencyBanner.kt → app/src/main/java/com/swasthai/app/ai/MedicalRagOrchestrator.kt

## Import Cycles
- None detected.

## Communities (75 total, 4 thin omitted)

### Community 0 - "DocumentRepository"
Cohesion: 0.06
Nodes (15): DocumentDao, Flow, RankedChunkCandidate, ChunkWithDocument, RAGDao, ChunkEmbeddingEntity, DocumentChunkEntity, DocumentEntity (+7 more)

### Community 1 - "ChatRepository.kt"
Cohesion: 0.26
Nodes (4): ChatRepository, Flow, ChatMessage, Conversation

### Community 2 - "MainActivity.kt"
Cohesion: 0.27
Nodes (6): MainActivity, AppBottomNav(), SwasthAITheme(), Bundle, ComponentActivity, NavController

### Community 3 - "VaultViewModel"
Cohesion: 0.23
Nodes (5): StateFlow, Uri, ViewModel, VaultUiState, VaultViewModel

### Community 4 - "design.md"
Cohesion: 0.05
Nodes (37): Border Radius Scale, Brand & Accent, Brand Gradient, Breakpoints, Buttons, Cards & Containers, Collapsing Strategy, Colors (+29 more)

### Community 5 - "DocumentChunker"
Cohesion: 0.09
Nodes (13): PdfExtractorTest, ImageTextExtractor, Context, Uri, PdfOcrResult, DocumentChunker, LabLabel, ExtractedPage (+5 more)

### Community 6 - "EmbeddingEngine"
Cohesion: 0.16
Nodes (7): EmbeddingEngine, AutoCloseable, ByteArray, com, ScoredChunk, EmbeddingEngineTest, FloatArray

### Community 7 - "SettingsViewModel"
Cohesion: 0.06
Nodes (13): BiomarkerSummaryItem, CreatedDoctorShare, DoctorClinicalSummary, DoctorShareRepository, DocumentSummaryItem, JSONObject, MedicationSummaryItem, Job (+5 more)

### Community 8 - "ChatViewModel"
Cohesion: 0.13
Nodes (4): ChatViewModel, Job, StateFlow, ViewModel

### Community 9 - "SwasthAI: Healthcare AI Safety, Clinical Guardrails & Evidence Grounding"
Cohesion: 0.09
Nodes (21): 1. Healthcare Safety Charter & Fundamental Constraints, 2.1 Deterministic Red-Line Triage System, 2.2 Red-Line Clinical Condition Taxonomy, 2.3 The India 112 Emergency Standard & Disconnected Device Realism, 2. Emergency Escalation Protocol & India 112 Workflow, 3.1 Strict Context Grounding Prompt Contract, 3.2 Automated Post-Generation Verification (Grounding Guardrail), 3. Evidence Grounding & Hallucination Elimination (+13 more)

### Community 10 - "3. Functional Requirements (FR)"
Cohesion: 0.10
Nodes (20): 1. Overview & Scope Definition, 2. User Personas & User Stories, 3.1 Local AI Chat & Conversational Experience (FR-CHAT), 3.2 Medical Vault & Document Processing (FR-VAULT), 3.3 Medical Report Simplifier & Biomarker Extraction (FR-SIMP), 3.4 Longitudinal Medical History (FR-HIST), 3.5 Genuine Local RAG (FR-RAG), 3.6 Healthcare Safety & Emergency Escalation (FR-SAFE) (+12 more)

### Community 11 - "SwasthAI: Detailed User Flows & Interaction Journeys"
Cohesion: 0.10
Nodes (20): 1. Primary User Journey Architecture, 2.1 Narrative Journey, 2.2 Sequence & Flow Diagram, 2. User Flow 1: Document Ingestion, OCR & Extraction, 3.1 Narrative Journey, 3.2 Sequence & Flow Diagram, 3. User Flow 2: Grounded Local RAG & Document Citation Verification, 4.1 Narrative Journey (+12 more)

### Community 12 - "3. Single-Model Quality Validation Suite (MedGemma 1.5 4B IT)"
Cohesion: 0.10
Nodes (20): 1. Testing Strategy & Multi-Tier Test Pyramid, 2.1 Benchmark Environment & Conditions, 2.2 Measurable Performance Benchmark Specifications, 2. Reproducible Galaxy S25 Performance Benchmark Suite, 3. Single-Model Quality Validation Suite (MedGemma 1.5 4B IT), 4.1 Unit Testing (JUnit 5 + MockK), 4.2 Integration Testing (AndroidX Test + Robolectric), 4. Subsystem Integration & Unit Testing (+12 more)

### Community 13 - "5. Complete Relational Database Schema (Room / SQLite)"
Cohesion: 0.11
Nodes (17): 1. Principles of On-Device Healthcare RAG, 2. Document Processing & Ingestion Lifecycle, 3.1 Chunking Rules, 3.2 Vector Indexing & Similarity Metric, 3. Layout-Aware Semantic Chunking & Vector Search, 4.1 Grounded Prompt Template Structure, 4. Hybrid Retrieval & Grounded Context Assembly, 5.1 `documents` (+9 more)

### Community 14 - "StructuredMeasurement"
Cohesion: 0.18
Nodes (12): Flow, HealthTimelineItem, MeasurementRecorded, ReportImported, StructuredMeasurement, AppleMeasurementTrendCard(), HistoryScreen(), SimpleDateFormat (+4 more)

### Community 15 - "1. Confirmed Architecture Decision Records (ADRs)"
Cohesion: 0.12
Nodes (16): 1. Confirmed Architecture Decision Records (ADRs), 2. Technical Claims Audit & Classification (Phase 0 Spike Findings), 3. Unresolved Technical Risks & Risk Resolution Status, 4. Phase 0 Recommendation: Provisional GO for Implementation, ADR-01: Native Android (Kotlin + Jetpack Compose) Over Cross-Platform Frameworks, ADR-02: Deferral of Gemini Live-Style Camera & Voice Streaming, ADR-03: Primary On-Device Runtime: Google LiteRT Stack, ADR-04: Single Generative Model Strategy: MedGemma 1.5 4B IT (+8 more)

### Community 16 - "SwasthAI: Project Brief & Foundational Blueprint"
Cohesion: 0.12
Nodes (15): 1. Executive Summary & Vision, 2.1 The Healthcare Information Gap, 2. Problem Statement, 3. Product Vision & Value Proposition, 4. Target Users & Personas, 5.1 Primary Goals (In-Scope for Product Blueprint & Hackathon MVP), 5.2 Explicit Non-Goals (Out of Scope for MVP), 5. Primary Goals & Explicit Non-Goals (+7 more)

### Community 17 - "SwasthAI: Master Engineering Implementation Checklist"
Cohesion: 0.12
Nodes (15): 10. Healthcare AI Safety & Emergency Controls (`:ai:safety`), 11. Multilingual Support (`:feature:localization`), 12. Care Planning & Bounded Agentic Workflows (`:feature:careplanner`), 13. Testing, Performance & Verification, 14. Hackathon Polish & Submission Assets, 1. Product Definition & Architecture Governance, 2. Research & Phase 0 Feasibility Spike, 3. UI/UX & Design System (+7 more)

### Community 18 - "SwasthAI: Privacy Architecture, Threat Model & Security Specification"
Cohesion: 0.14
Nodes (13): 1. Sensitive Data Inventory & Classification, 2. Security Trust Boundaries & System Architecture, 3.1 Master Key Generation & Hardware Binding, 3.2 Database Encryption (SQLCipher), 3.3 File Vault Encryption (`EncryptedFile`), 3. Cryptographic Key Management & Storage Protection, 4. Threat Modeling (STRIDE Matrix), 5. Android Permissions Strategy & Attack Surface Minimization (+5 more)

### Community 19 - "SwasthAI: Comprehensive System Architecture & Engineering Blueprint"
Cohesion: 0.15
Nodes (12): 1. Architectural Philosophy & Principles, 2. Multi-Module Project Structure, 3. High-Level System Architecture Diagram, 4.1 Strict Sequential Model Execution, 4.2 Hardware Acceleration Delegates (Targets to be Validated), 4. Local AI Orchestration & Memory Management, 5.1 Layout-Aware Semantic Chunking, 5. Document Ingestion & Processing Architecture (+4 more)

### Community 20 - "2. Phase Breakdown"
Cohesion: 0.15
Nodes (12): 1. Roadmap Strategy & Dependency Sequence, 2. Phase Breakdown, 3. Technical Feasibility Gates & Risk Milestones, Phase 0: Model Feasibility Spike (Status: In Progress / Harness Built), Phase 1: Mobile Foundation & Local AI Chat, Phase 2: Medical Vault & Document Processing, Phase 3: Genuine Local RAG Pipeline, Phase 4: Structured Medical History & Report Comparison (+4 more)

### Community 21 - "SwasthAI: Model and Inference Runtime Plan"
Cohesion: 0.18
Nodes (10): 1. Single Generative Model Strategy & Engineering Rationale, 2.1 Candidate A: MedGemma 1.5 4B IT (Preferred Single Generative Candidate), 2.2 Candidate B: EmbeddingGemma 2 Text 270M (Dedicated Embedding Model), 2.3 Documented Fallback Candidate: Gemma 4 E2B-it (Fallback Alternative Only), 2. In-Depth Candidate Model Evaluation, 3. Comprehensive Model Selection Matrix, 4. Technical Claims Audit: Fact vs. Estimate vs. Assumption, 5. MedGemma 1.5 Feasibility Protocol & Device Benchmark Harness (+2 more)

### Community 23 - "SwasthAI: Comprehensive Feature Catalog"
Cohesion: 0.22
Nodes (8): 12. Feature Group K: Medical Summary Export, 14. Feature Group M: UX & Accessibility, 15. Feature Group N: Future Multimodal Interaction (Research Gate), 1. Feature Taxonomy & Priority Structure, FT-K01: Structured Doctor Visit Summary Generator, FT-M01: Modern, Intuitive Health Companion Dashboard, FT-N01: Live Camera & Voice Real-Time Consultation (Long-Term R&D), SwasthAI: Comprehensive Feature Catalog

### Community 24 - "SwasthAI: Hackathon Demonstration Script, Video Storyboard & Impact Plan"
Cohesion: 0.29
Nodes (6): 1. Live Demonstration Script (3-Minute Hackathon Presentation), 2. Technical Differentiators for Judges, 3. One-Slide Impact Plan, 4. 45–90 Second Demonstration Video Storyboard, Detailed Step-by-Step Script, SwasthAI: Hackathon Demonstration Script, Video Storyboard & Impact Plan

### Community 26 - "SwasthAI: Offline-First, Privacy-Focused AI Healthcare Companion"
Cohesion: 0.29
Nodes (6): 1. Executive Summary, 2. Core Pillars & Architecture, 3. Engineering Documentation Package, 4. Hardware Constraints & Deployment Profile, 5. Scope Boundary: Future Multimodal Research, SwasthAI: Offline-First, Privacy-Focused AI Healthcare Companion

### Community 27 - "2. Feature Group A: Local AI Chat"
Cohesion: 0.33
Nodes (6): 2. Feature Group A: Local AI Chat, FT-A01: Single On-Device Generative Engine (MedGemma 1.5 4B IT), FT-A01A: Multi-Turn History Management & Turn Formatting, FT-A02: Real-Time Token Streaming & Cancellation, FT-A03: Persistent Conversation Lifecycle & Branching, FT-A04: Grounding Badge & Source Evidence Drawer

### Community 28 - "6. Feature Group E: Genuine Local RAG"
Cohesion: 0.33
Nodes (6): 6. Feature Group E: Genuine Local RAG, FT-E01: Layout-Aware Semantic Document Chunking, FT-E02: Dedicated Local Vector Embedding Generation (EmbeddingGemma 300M), FT-E03: High-Performance On-Device Vector Store, FT-E04: Hybrid Retrieval Engine (Vector Semantic + Structured SQL), FT-E05: Verifiable Source Footnotes & Document Coordinate Jump

### Community 29 - "3. Feature Group B: Medical Vault"
Cohesion: 0.40
Nodes (5): 3. Feature Group B: Medical Vault, FT-B01: Multi-Format Document Ingestion (PDF & Scanned Images), FT-B02: Hybrid Text Extraction Pipeline (Native PDF + Local OCR), FT-B03: Document Provenance & Original Artifact Preservation, FT-B04: Atomic Document Deletion & Cascade Purge

### Community 30 - "4. Feature Group C: Medical Report Simplifier"
Cohesion: 0.50
Nodes (4): 4. Feature Group C: Medical Report Simplifier, FT-C01: Deterministic Biomarker & Measurement Parser, FT-C02: Deterministic Reference Range Evaluation & Visual Flagging, FT-C03: Plain-Language Biomarker Demystifier (Powered by MedGemma)

### Community 31 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 32 - "10. Feature Group I: Agentic Health Workflows"
Cohesion: 0.67
Nodes (3): 10. Feature Group I: Agentic Health Workflows, FT-I01: Bounded Tool-Calling Architecture with Strict JSON Schema, FT-I02: Mandatory Human-in-the-Loop Confirmation Gate

### Community 33 - "11. Feature Group J: Preventive Healthcare"
Cohesion: 0.67
Nodes (3): 11. Feature Group J: Preventive Healthcare, FT-J01: Local Medication & Appointment Reminders, FT-J02: Preventive Care Dashboard & Timeline

### Community 34 - "13. Feature Group L: Privacy & Security"
Cohesion: 0.67
Nodes (3): 13. Feature Group L: Privacy & Security, FT-L01: Zero-Network Air-Gap Enforcement, FT-L02: Hardware-Backed Encryption at Rest (Android Keystore + AES-GCM)

### Community 35 - "5. Feature Group D: Longitudinal Medical History"
Cohesion: 0.67
Nodes (3): 5. Feature Group D: Longitudinal Medical History, FT-D01: Four-Tier Provenance Tracking & Audit Trail, FT-D02: Multi-Report Biomarker Trend Comparison

### Community 36 - "7. Feature Group F: Medical Chat With Documents"
Cohesion: 0.67
Nodes (3): 7. Feature Group F: Medical Chat With Documents, FT-F01: Explicit Document Focus & Multi-Report Scoping, FT-F02: Negative Evidence Handling & "Out of Scope" Signaling

### Community 37 - "8. Feature Group G: Multilingual Health Assistant"
Cohesion: 0.67
Nodes (3): 8. Feature Group G: Multilingual Health Assistant, FT-G01: Hindi Plain-Language Medical Explanations, FT-G02: Regional Indian Language Decoupling (Tamil/Telugu/Bengali)

### Community 38 - "9. Feature Group H: Smart Symptom Guidance"
Cohesion: 0.67
Nodes (3): 9. Feature Group H: Smart Symptom Guidance, FT-H01: Deterministic Pre-LLM Emergency Escalation (India 112 Standard), FT-H02: Structured Symptom Intake & Clarification Flow

### Community 39 - "main.py"
Cohesion: 0.10
Nodes (70): BaseModel, Connection, CurrentUser, delete, FastAPI, get, Header, post (+62 more)

### Community 43 - "AccountFamilyRepository"
Cohesion: 0.10
Nodes (17): AcceptedKeyGrant, AccessibleCloudProfile, AccountFamilyRepository, FamilyAccount, FamilyBlockedAccount, FamilyCredentials, FamilyDevicePublicKey, FamilyMemberAccount (+9 more)

### Community 46 - "ChatViewModel.kt"
Cohesion: 0.12
Nodes (18): MessageRole, ASSISTANT, SYSTEM, USER, ResponseLanguage, AUTO, ENGLISH, HINDI (+10 more)

### Community 47 - "EmergencyTriage"
Cohesion: 0.25
Nodes (3): EmergencyTriage, TriageResult, EmergencyTriageTest

### Community 48 - "SwasthApplication"
Cohesion: 0.07
Nodes (24): GemmaModelArtifactManager, ModelArtifactDownload, ModelArtifactDownloadStatus, FAILED, MISSING, PAUSED, PENDING, READY (+16 more)

### Community 49 - "HomeViewModel.kt"
Cohesion: 0.08
Nodes (16): CycleLogDao, Flow, CycleLogEntity, CycleLog, CycleTrackerSummary, CycleTrackingRepository, Flow, MenstrualFlow (+8 more)

### Community 50 - "MedicationReminderEntity"
Cohesion: 0.11
Nodes (12): Flow, MedicationReminderDao, MedicationReminderEntity, Flow, MedicationReminderRepository, Context, MedicationReminderReceiver, Context (+4 more)

### Community 51 - "SourceCitation"
Cohesion: 0.16
Nodes (10): com, MedicalFileEntry, RAGPipeline, RetrievalIntent, BIOMARKER_HISTORY, RECORD_SEARCH, RetrievalResult, StructuredHit (+2 more)

### Community 52 - "ConversationDao"
Cohesion: 0.07
Nodes (12): ConversationDao, Flow, Flow, MeasurementDao, ChatTurnEntity, ConversationEntity, MessageEntity, DocumentChunkFtsEntity (+4 more)

### Community 53 - "ProfileKeyManager"
Cohesion: 0.16
Nodes (9): KeyRecoveryRequiredException, ByteArray, SecretKey, ProfileKeyManager, RecoverySetup, ByteArray, SecretKey, RecoveryPhraseCodec (+1 more)

### Community 54 - "EncryptedPayloadEnvelope"
Cohesion: 0.24
Nodes (6): EncryptedPayloadEnvelope, ByteArray, java, SecretKey, EncryptedPayloadEnvelopeTest, GeneralSecurityException

### Community 55 - "Gemma4ToolCallingModel"
Cohesion: 0.21
Nodes (9): Activity, Complete, Gemma4ToolCallingModel, AutoCloseable, Flow, StateFlow, ToolCallingModelUnavailableException, ToolCallingResponseEvent (+1 more)

### Community 56 - "MedicalRecordToolDefinitions"
Cohesion: 0.21
Nodes (4): ExecutedMedicalTool, MedicalRecordToolDefinitions, ClinicalKnowledgeEngineTest, ToolSet

### Community 57 - "MedicalDocument"
Cohesion: 0.18
Nodes (13): DocumentChunk, IndexingStatus, EXTRACTED, FAILED, INDEXED, PARTIAL, PENDING, MedicalDocument (+5 more)

### Community 58 - "ModelInferenceStatus"
Cohesion: 0.19
Nodes (13): androidx, ModelInferenceStatus, ERROR, GENERATING, INITIALIZING, NOT_INSTALLED, READY, ChatUiState (+5 more)

### Community 59 - "Row"
Cohesion: 0.36
Nodes (14): ConversationRow(), DocumentRow(), EmptyState(), homeGreeting(), HomeScreen(), indexingLabel(), Modifier, SimpleDateFormat (+6 more)

### Community 60 - "package.json"
Cohesion: 0.13
Nodes (14): better-auth, better-sqlite3, nodemailer, allowScripts, better-sqlite3@13.0.3, dependencies, better-auth, better-sqlite3 (+6 more)

### Community 61 - "MainAppContent"
Cohesion: 0.26
Nodes (8): MainAppContent(), ViewModelProvider, ViewModelProvider, ViewModelProvider, ViewModelProvider, ViewModelProvider, T, ViewModelProvider

### Community 62 - "ChatScreen.kt"
Cohesion: 0.36
Nodes (10): AppleChatMessageBubble(), ChatScreen(), Modifier, MessageImageThumbnail(), CitationInspectionDialog(), EmergencyBanner(), EmergencyCallButton(), Context (+2 more)

### Community 64 - "Screen"
Cohesion: 0.22
Nodes (7): Chat, History, Home, Integrations, Screen, Settings, Vault

### Community 65 - "ChatTurnStatus"
Cohesion: 0.25
Nodes (8): ChatTurnStatus, CANCELLED, COMPLETED, FAILED, GENERATING, INTERRUPTED, PREPARING, RETRIEVING

### Community 66 - "Dp"
Cohesion: 0.39
Nodes (6): DiceBearAvatar(), Modifier, SettingsDivider(), SettingsScreen(), SettingsSectionHeader(), Dp

### Community 67 - "Text"
Cohesion: 0.67
Nodes (6): Text, ContextConfigSheet(), Modifier, LanguageFilterChip(), ScopeOptionRow(), TimeFilterChip()

### Community 68 - "RetrievalMode"
Cohesion: 0.33
Nodes (6): RetrievalMode, HYBRID, LOCAL_KEYWORD_FALLBACK, NO_RECORDS_FOUND, SEMANTIC_VECTOR, STRUCTURED_SQL_ONLY

### Community 69 - "auth.mjs"
Cohesion: 0.40
Nodes (4): auth, authDirectory, database, port

### Community 70 - "run-local.sh"
Cohesion: 0.33
Nodes (5): run-local.sh script, SWASTHAI_AUTH_AUDIENCE, SWASTHAI_AUTH_ISSUER, SWASTHAI_AUTH_JWKS_URL, SWASTHAI_AUTH_MODE

### Community 71 - "SwasthAI local API"
Cohesion: 0.40
Nodes (4): Contract outline, Local setup (WSL Ubuntu), Security and production boundary, SwasthAI local API

## Knowledge Gaps
- **297 isolated node(s):** `PENDING`, `RUNNING`, `PAUSED`, `READY`, `FAILED` (+292 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **4 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ModelInferenceStatus` connect `ModelInferenceStatus` to `DocumentRepository`, `Dp`, `EmbeddingEngine`, `SettingsViewModel`, `ChatViewModel.kt`, `HomeViewModel.kt`, `SourceCitation`, `Gemma4ToolCallingModel`, `Row`, `ChatScreen.kt`?**
  _High betweenness centrality (0.062) - this node is a cross-community bridge._
- **Why does `AccountFamilyRepository` connect `AccountFamilyRepository` to `DocumentRepository`, `SwasthApplication`, `SettingsViewModel`?**
  _High betweenness centrality (0.051) - this node is a cross-community bridge._
- **Why does `SettingsViewModel` connect `SettingsViewModel` to `MainActivity.kt`, `Dp`, `MainAppContent`?**
  _High betweenness centrality (0.044) - this node is a cross-community bridge._
- **Are the 29 inferred relationships involving `Text` (e.g. with `AppleChatMessageBubble()` and `ChatScreen()`) actually correct?**
  _`Text` has 29 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PENDING`, `RUNNING`, `PAUSED` to the rest of the system?**
  _297 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `DocumentRepository` be split into smaller, more focused modules?**
  _Cohesion score 0.057692307692307696 - nodes in this community are weakly interconnected._
- **Should `design.md` be split into smaller, more focused modules?**
  _Cohesion score 0.05263157894736842 - nodes in this community are weakly interconnected._