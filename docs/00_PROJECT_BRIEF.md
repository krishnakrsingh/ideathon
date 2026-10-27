# SwasthAI: Project Brief & Foundational Blueprint

**Document Version:** 1.1.0  
**Classification:** Product Planning & Architectural Blueprint  
**Status:** Approved for Pre-Implementation Planning (Revised Single Generative Model Architecture)  
**Target Hardware:** Samsung Galaxy S25 (Qualcomm Snapdragon 8 Elite for Galaxy, 12 GB LPDDR5X RAM, 256 GB UFS 4.0 Storage, Android 15 / One UI 7)  
**Primary Execution Mandate:** Strict Documentation & Engineering Plan (Zero Application Source Code in this Phase)

---

## 1. Executive Summary & Vision

**SwasthAI** (derived from the Sanskrit *Swasthya*, meaning health, wholeness, and well-being) is an offline-first, privacy-absolute, on-device AI healthcare companion. Designed for modern mobile hardware, SwasthAI transforms fragmented, opaque medical reports and unstructured patient narratives into an understandable, persistent, and actionable personal medical history.

Unlike cloud-dependent chatbots or generic wrapper applications, SwasthAI executes entirely on the user's smartphone. It pairs **a single specialized on-device generative model (MedGemma 1.5 4B IT)** for all conversational and medical explanation tasks, **a dedicated local embedding model (EmbeddingGemma 300M)** for genuine semantic retrieval, and a **deterministic structured health record layer** with genuine retrieval-augmented generation (RAG). Sensitive personal health information (PHI)—including lab reports, diagnostic summaries, prescriptions, and symptom histories—never leaves the physical boundary of the device without explicit, user-initiated consent.

```
+-----------------------------------------------------------------------------------+
|                               SwasthAI Core Thesis                                |
|                                                                                   |
|   "One coherent mobile application. One specialized generative model (MedGemma).  |
|    One dedicated local embedding engine. One persistent local health record.      |
|    Genuine hybrid retrieval and grounded answers. Explicit safety controls.      |
|    Zero mandatory reliance on cloud infrastructure."                              |
+-----------------------------------------------------------------------------------+
```

---

## 2. Problem Statement

### 2.1 The Healthcare Information Gap
1. **Opaque Medical Reports:** Over 75% of patients in developing and emerging healthcare markets cannot interpret standard diagnostic laboratory panels (e.g., Complete Blood Count, Lipid Profiles, HbA1c, Liver Function Tests) or identify reference range flags without clinical consultation.
2. **Fragmented, Ephemeral Health Records:** Patients carry physical paper printouts or disparate PDFs across multiple providers, clinics, and diagnostic chains. There is no unified, chronological health record accessible during consultations or emergencies.
3. **Pervasive Health Inequity & Jargon:** Non-technical individuals, elderly citizens, and semi-literate demographics face acute linguistic barriers when diagnostic reports are exclusively issued in complex English clinical terminology.
4. **Pervasive Privacy Violations:** Uploading personal lab reports, pathology findings, and symptom narratives to commercial cloud AI APIs creates unacceptable privacy hazards, regulatory exposure (HIPAA, GDPR, India DPDP Act 2023), and risks of unconsented data harvesting.
5. **AI Hallucinations & Diagnostic Overconfidence:** Generic cloud conversational models frequently fabricate clinical reference ranges, offer definitive diagnostic declarations, or fail to cite specific document sources when answering questions about a patient's historical tests.

---

## 3. Product Vision & Value Proposition

SwasthAI bridges this divide through an intelligent, empathetic, and rigorously bounded personal healthcare assistant:

* **Demystify Diagnostics:** Translates dense laboratory measurements, reference ranges, and diagnostic notes into plain, compassionate, and linguistically accessible explanations.
* **Preserve Document Truth:** Operates on original laboratory artifacts. Every AI explanation cites the exact document title, date, page number, and observed laboratory value.
* **Longitudinal Understanding:** Connects past and present. When a user asks, *"How has my cholesterol changed since last year?"*, SwasthAI performs hybrid retrieval across the local vault and executes deterministic numerical comparisons between reports.
* **Responsible Symptom Triage:** Provides bounded, preliminary symptom guidance driven by conservative clinical safety rules, clear uncertainty signaling, and deterministic emergency escalation protocols (including India's 112 emergency standard).
* **Uncompromising Data Sovereignty:** Operates in 100% airplane mode. All vector embeddings, relational schemas, document scans, and model inferences execute directly on the phone's local compute hardware.

---

## 4. Target Users & Personas

### Persona 1: Priya Sharma (Chronic Disease Caregiver)
* **Demographics:** 34 years old, working professional, caregiver for her 68-year-old diabetic father.
* **Pain Points:** Father visits three different specialists; lab tests from Dr. Lal PathLabs and Apollo Clinic are scattered across WhatsApp and physical folders. Struggle to track whether HbA1c and creatinine are trending upwards over 18 months.
* **Needs:** A local, private vault that tracks longitudinal trends, flags out-of-range biomarkers across multiple years, and explains what dietary questions she should bring to the next nephrologist consultation.

### Persona 2: Rajesh Patel (Everyday Consumer / Health Literacy)
* **Demographics:** 46 years old, small business owner, native Hindi speaker with basic English comprehension.
* **Pain Points:** Receives an annual health checkup report filled with terms like "Microcytic Hypochromic Anemia" and "SGPT/ALT Elevation". Panics, searches online forums, and encounters frightening, inaccurate diagnoses.
* **Needs:** Plain-language, culturally grounded explanation in Hindi; clarity on what the reference ranges mean; immediate reassurance and advice on which specialist to see.

### Persona 3: Ananya Sen (Privacy-Conscious Tech Enthusiast)
* **Demographics:** 29 years old, software engineer.
* **Pain Points:** Refuses to upload therapy notes, genetic test results, and hormone panels to cloud-based AI services.
* **Needs:** Complete local execution, verifiable air-gapped security, open-weight models, and full export/deletion control over all vectors and relational records.

---

## 5. Primary Goals & Explicit Non-Goals

### 5.1 Primary Goals (In-Scope for Product Blueprint & Hackathon MVP)
1. **Single Generative Model Architecture:** Evaluate and deploy MedGemma 1.5 4B IT as the sole generative engine for both general health conversation and grounded medical report comprehension, avoiding redundant dual-model complexity.
2. **Dedicated Local Embedding Engine:** Separate EmbeddingGemma 300M model for semantic chunk and query vector embedding.
3. **Medical Vault & Original Preservation:** Encrypted local repository for diagnostic PDFs and images, preserving original files and page mappings.
4. **Genuine On-Device Hybrid RAG:** Local document chunking, semantic vector embedding, and vector search coupled with deterministic Room/SQLite relational queries for numerical and temporal records.
5. **Grounded Report Explanations:** Deterministic parsing of lab tests (biomarker, value, unit, reference range, abnormal flag) combined with cited, contextual AI summaries.
6. **Longitudinal Health History:** Tracking confirmed conditions, allergies, medications, and biomarker trends across multiple historical reports.
7. **Deterministic Safety Architecture:** Red-line emergency triage rules (e.g., acute chest pain, anaphylaxis, stroke symptoms) bypassing LLM generation to display immediate emergency action cards (112 India).
8. **Multilingual Foundations:** Health explanations and UI support in English and Hindi, with architectural readiness for Indian regional languages (Tamil/Bengali/Telugu).

### 5.2 Explicit Non-Goals (Out of Scope for MVP)
1. **NO Dual Generative Models in MVP:** We do NOT load or bundle two generative LLMs (e.g., Gemma 4 + MedGemma). MedGemma 1.5 4B IT is evaluated as the sole generative model. (Gemma 4 remains a documented fallback alternative only).
2. **NO Live Camera / Continuous Voice Streaming:** Gemini Live-style bidirectional streaming, continuous microphone listening, or the Box application are **strictly deferred** to future research. They will not dominate or block the MVP delivery.
3. **NO Definitive Clinical Diagnosis:** SwasthAI will never state: *"You have Type 2 Diabetes"* or *"You are suffering from Chronic Kidney Disease."* It acts solely as an educational, comprehension, and preparation companion.
4. **NO Autonomous Prescription or Medication Alteration:** SwasthAI will never calculate drug dosages, suggest prescription pharmaceuticals, or advise discontinuing physician-prescribed treatments.
5. **NO Cloud Server Dependency:** The core MVP must function in complete airplane mode. Cloud endpoints must not be required for inference, embedding, retrieval, or storage.
6. **NO Unbounded Autonomous Agents:** No arbitrary shell commands, automated phone calls without user confirmation, or unrestricted filesystem access.

---

## 6. Target Hardware Specifications & Deployment Constraints

The primary validation target is the **Samsung Galaxy S25** flagship platform:

| Hardware Component | Specification | Deployment Constraint & Implication |
| :--- | :--- | :--- |
| **SoC** | Qualcomm Snapdragon 8 Elite for Galaxy | Dual Oryon CPU (up to 4.32 GHz), Adreno 830 GPU, Hexagon NPU. Peak compute is high, but sustained power loads require thermal monitoring. Acceleration delegates (GPU/NPU) must be empirically validated on device. |
| **System Memory** | 12 GB LPDDR5X RAM | OS and baseline background services consume ~4.5–5.5 GB. Maximum safe resident memory (RSS) target for SwasthAI is **3.5 GB peak** (LLM + Embeddings + App UI + OS headroom) to prevent Android Low Memory Killer (LMK) eviction. Target must be measured on device. |
| **Storage** | 256 GB UFS 4.0 | High-speed read (>3 GB/s) allows rapid model weight memory-mapping (`mmap`). On-device model cache budget capped at **<= 5.0 GB total** (single generative model + embedding model). |
| **OS / Runtime** | Android 15 / One UI 7 (API Level 35) | Android 15 16KB page-size compatibility required; aggressive background battery optimization necessitates foreground services or WorkManager for long indexing tasks. |
| **Connectivity** | Complete Offline Capability | Must function with zero cellular/Wi-Fi signal. All models, dictionaries, tokenizers, and vector indices must reside on internal flash storage. |

---

## 7. Core Differentiators

| Dimension | Generic Cloud Chatbot (e.g., ChatGPT / Gemini Web) | Naive Local Wrappers | SwasthAI Native Solution |
| :--- | :--- | :--- | :--- |
| **Data Privacy** | PHI transmitted to remote corporate servers; subject to terms of service and breach risk. | May leak telemetry or send queries to cloud APIs for embeddings. | **100% On-Device:** All data, embeddings, and model weights reside in encrypted app-private storage. |
| **Model Architecture** | Massive cloud generalist model. | Bundles multiple heavy models indiscriminately. | **Lean Single-Generative + Embedding Pipeline:** Single MedGemma 1.5 4B IT for generation + EmbeddingGemma for retrieval. |
| **Grounding & Provenance** | Opaque knowledge retrieval; prone to hallucinating lab values and page numbers. | Basic prompt stuffing (pasting raw text into context window). | **Genuine Hybrid RAG:** Semantic chunking + vector search + deterministic SQL biomarker comparison + verifiable document citations. |
| **Safety Governance** | Generic safety filter (refuses to answer or provides generic web disclaimers). | Unchecked generation; prone to confident medical misinformation. | **Dual-Layer Guardrails:** Pre-LLM deterministic emergency classification (112 India) + Post-LLM clinical hallucination verification. |
| **Offline Reliability** | Inoperable without high-speed internet connectivity. | High latency, unoptimized CPU execution causing severe thermal lag. | **Hardware-Targeted:** On-device LiteRT execution targeted to Snapdragon 8 Elite hardware backends. |
| **Medical Continuity** | Ephemeral chat sessions; forgets previous reports across sessions. | Flat local storage without longitudinal tracking. | **Relational Longitudinal Health Record:** Structured biomarker tracking across time with date-normalized trend analytics. |

---

## 8. Hackathon Objectives & Success Criteria

The SwasthAI hackathon project must deliver three flawless deliverables:

1. **A Working Prototype:** A fully functioning native Android application running on the physical Galaxy S25 device, showcasing:
   - Fluid local AI chat using single MedGemma 1.5 4B IT model.
   - Importing and locally parsing a complex synthetic blood panel PDF in airplane mode.
   - Semantic retrieval via EmbeddingGemma and grounded explanations referencing exact lab values and page coordinates.
   - Cross-report comparison illustrating longitudinal trends between two distinct dates.
   - Deterministic emergency escalation demonstration.
2. **One-Slide Impact Plan:** A clear, data-driven narrative detailing early adopter acquisition (first 100 users → 1,000 users), clinical safety verification methodologies, health literacy impact metrics, and sustainable open-source/partnership scaling.
3. **45–90 Second Demonstration Video:** A tightly scripted, high-impact video demonstration highlighting the patient dilemma, the offline breakthrough, grounded report interaction, and verifiable privacy protection.
