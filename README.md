# SwasthAI: Offline-First, Privacy-Focused AI Healthcare Companion

[![Platform](https://img.shields.io/badge/Platform-Android%2015%20(API%2035)-green.svg)](https://developer.android.com)
[![Target Device](https://img.shields.io/badge/Target-Samsung%20Galaxy%20S25-blue.svg)](https://www.samsung.com)
[![Processor](https://img.shields.io/badge/SoC-Snapdragon%208%20Elite-red.svg)](https://www.qualcomm.com)
[![Generative Model](https://img.shields.io/badge/Generative%20Model-MedGemma%201.5%204B%20IT-blueviolet.svg)](https://developers.google.com/health-ai-developer-foundations/medgemma/model-card)
[![Embedding Model](https://img.shields.io/badge/Embedding%20Model-EmbeddingGemma%20300M-orange.svg)](https://ai.google.dev/gemma/docs/embeddinggemma)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20On--Device%20(Zero%20Network)-purple.svg)](#)

---

## 1. Executive Summary

**SwasthAI** is an offline-first, on-device AI healthcare companion designed to empower patients and caregivers to understand complex medical reports, track longitudinal health trends, manage persistent health records, and receive responsible clinical guidance—**with complete privacy and zero reliance on cloud infrastructure**.

Running entirely on modern mobile hardware (optimized for the **Samsung Galaxy S25** with Qualcomm Snapdragon 8 Elite), SwasthAI pairs a **single specialized on-device generative model (MedGemma 1.5 4B IT)** for conversation and medical explanations, a **separate dedicated local embedding engine (EmbeddingGemma 300M)** for semantic retrieval, and a deterministic structured health record layer with **genuine local retrieval-augmented generation (RAG)**.

```
+-----------------------------------------------------------------------------------+
|                               SwasthAI Core Philosophy                            |
|                                                                                   |
|   "One coherent mobile application. One specialized generative model (MedGemma).  |
|    One dedicated local embedding engine. One persistent local health record.      |
|    Genuine hybrid retrieval and grounded answers. Explicit safety controls.      |
|    Zero mandatory reliance on cloud infrastructure."                              |
+-----------------------------------------------------------------------------------+
```

---

## 2. Core Pillars & Architecture

1. **Single Generative Model Architecture:** A single on-device model (**MedGemma 1.5 4B IT**) handles general health dialogue, medical terminology explanation, and report summarization, eliminating multi-model bloat.
2. **Dedicated Separate Embedding Engine:** A lightweight local model (**EmbeddingGemma 300M**) handles chunk and query vector embedding, operating completely separately from generative LLM inference.
3. **Encrypted Medical Vault:** Secure local repository for digital PDFs and scanned paper reports, protected by AES-256-GCM encryption with keys generated in the Qualcomm StrongBox Keymaster.
4. **Genuine Local Hybrid RAG:** Layout-aware semantic document chunking, on-device vector embeddings, and hybrid retrieval combining vector similarity with deterministic Room SQL queries for exact numerical and temporal records.
5. **Verifiable Source Grounding:** Every statement referencing personal health data includes interactive bracketed footnotes (`[1]`, `[2]`) that allow users to jump directly to the original document page and highlighted test coordinate.
6. **Report Simplifier & Longitudinal Tracking:** Parses laboratory measurements (values, units, reference intervals), evaluates out-of-range flags mathematically, and tracks biomarker deltas across historical reports (e.g., comparing HbA1c over 6 months). MedGemma explains verified data rather than discovering raw numbers from unparsed text.
7. **Deterministic Healthcare Safety:** Pre-LLM deterministic emergency triage filter intercepting life-threatening conditions (chest pain, stroke, severe bleeding) in <5ms, presenting an emergency action card with one-tap dialing to **India's unified emergency standard (112)**.
8. **Multilingual Health Literacy:** Plain-language explanations in English and Hindi, preserving canonical drug names and units in Latin script to prevent ambiguity.

---

## 3. Engineering Documentation Package

This repository contains a comprehensive, research-backed product blueprint, technical specifications, and implementation checklists located in the [`docs/`](file:///c:/Users/krish/Downloads/ideathon/docs/) directory:

| Document | Title & Focus | Description |
| :--- | :--- | :--- |
| **[`00_PROJECT_BRIEF.md`](file:///c:/Users/krish/Downloads/ideathon/docs/00_PROJECT_BRIEF.md)** | Project Brief & Foundations | Vision, problem statement, personas, hardware constraints, single-model strategy. |
| **[`01_PRODUCT_REQUIREMENTS.md`](file:///c:/Users/krish/Downloads/ideathon/docs/01_PRODUCT_REQUIREMENTS.md)** | Product Requirements Document | Detailed functional (FR), non-functional (NFR), and target acceptance metrics. |
| **[`02_FEATURE_CATALOG.md`](file:///c:/Users/krish/Downloads/ideathon/docs/02_FEATURE_CATALOG.md)** | Comprehensive Feature Catalog | Feature inventory across Groups A through N with IDs, priorities, and criteria. |
| **[`03_USER_FLOWS.md`](file:///c:/Users/krish/Downloads/ideathon/docs/03_USER_FLOWS.md)** | User Journeys & Mermaid Flows | End-to-end user flows with Mermaid sequence diagrams for all core journeys. |
| **[`04_SYSTEM_ARCHITECTURE.md`](file:///c:/Users/krish/Downloads/ideathon/docs/04_SYSTEM_ARCHITECTURE.md)** | System Architecture Blueprint | Multi-module Android architecture, LiteRT orchestration, and data flows. |
| **[`05_MODEL_AND_RUNTIME_PLAN.md`](file:///c:/Users/krish/Downloads/ideathon/docs/05_MODEL_AND_RUNTIME_PLAN.md)** | Model & Runtime Strategy | MedGemma 1.5 Feasibility Gate, claims audit, facts vs estimates, fallback plan. |
| **[`06_LOCAL_RAG_AND_DATA_SCHEMA.md`](file:///c:/Users/krish/Downloads/ideathon/docs/06_LOCAL_RAG_AND_DATA_SCHEMA.md)** | RAG Architecture & Data Schema | Chunking, hybrid retrieval, citations, and complete Room/SQLite database schema. |
| **[`07_PRIVACY_SECURITY_THREAT_MODEL.md`](file:///c:/Users/krish/Downloads/ideathon/docs/07_PRIVACY_SECURITY_THREAT_MODEL.md)** | Privacy, Security & Threat Model | Sensitive data classification, StrongBox Keystore, STRIDE matrix, and air-gap proof. |
| **[`08_HEALTHCARE_SAFETY.md`](file:///c:/Users/krish/Downloads/ideathon/docs/08_HEALTHCARE_SAFETY.md)** | Clinical Safety & Guardrails | Emergency protocols, 112 India workflow, hallucination prevention, synthetic data. |
| **[`09_IMPLEMENTATION_ROADMAP.md`](file:///c:/Users/krish/Downloads/ideathon/docs/09_IMPLEMENTATION_ROADMAP.md)** | Phased Implementation Roadmap | Phased milestones (Phases 0–7), feasibility gates, and critical path analysis. |
| **[`10_TEST_PLAN_AND_ACCEPTANCE.md`](file:///c:/Users/krish/Downloads/ideathon/docs/10_TEST_PLAN_AND_ACCEPTANCE.md)** | Test Plan & Benchmark Suite | Reproducible Galaxy S25 benchmark suite and 10-point single-model validation. |
| **[`11_DEMO_AND_HACKATHON_PLAN.md`](file:///c:/Users/krish/Downloads/ideathon/docs/11_DEMO_AND_HACKATHON_PLAN.md)** | Hackathon Demo & Impact Plan | 3-minute live presentation script, 60-second video storyboard, and 1-slide impact. |
| **[`12_DECISIONS_AND_OPEN_QUESTIONS.md`](file:///c:/Users/krish/Downloads/ideathon/docs/12_DECISIONS_AND_OPEN_QUESTIONS.md)** | ADRs & Technical Risk Ledger | Confirmed architectural decision records, claims audit, and open risk ledger. |
| **[`MASTER_CHECKLIST.md`](file:///c:/Users/krish/Downloads/ideathon/docs/MASTER_CHECKLIST.md)** | Engineering Implementation Tasks | Exhaustive, actionable task checklist with priorities, dependencies, and gates. |

---

## 4. Hardware Constraints & Deployment Profile

* **Primary Test Target:** Samsung Galaxy S25 (SM-S931B)
* **Chipset:** Qualcomm Snapdragon 8 Elite for Galaxy (3nm TSMC N3E)
* **RAM Budget:** 12 GB LPDDR5X (App RSS targeted at **<= 3.5 GB peak** to prevent LMK eviction; to be validated on device)
* **Storage Allocation:** Capped at **<= 5.0 GB** total for all model weights and runtimes
* **OS:** Android 15 (API Level 35) / One UI 7
* **Zero Network Dependency:** Operates with 100% fidelity in Airplane Mode (no cellular, no Wi-Fi)

---

## 5. Scope Boundary: Future Multimodal Research

Per architectural decision [ADR-02](file:///c:/Users/krish/Downloads/ideathon/docs/12_DECISIONS_AND_OPEN_QUESTIONS.md), continuous live camera streaming, microphone voice streaming, and the Box application are **strictly deferred to future research gates**. They are not included in the MVP implementation scope, ensuring laser focus on local chat, document vaulting, genuine RAG, and grounded report simplification.
