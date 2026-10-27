# SwasthAI: Model and Inference Runtime Plan

**Document Version:** 1.2.0  
**Classification:** Machine Learning & Systems Engineering Specification  
**Status:** Phase 0 Feasibility Findings Updated (Verified Community Artifacts)  

---

## 1. Single Generative Model Strategy & Engineering Rationale

SwasthAI adopts a **Single Generative Model Strategy**:
1. **One Generative Model:** Rather than maintaining two separate generative models, the application selects **MedGemma 1.5 4B IT** as the preferred candidate for ALL conversational and medical synthesis tasks.
2. **Dedicated Separate Embedding Model:** A distinct, lightweight on-device embedding model (**EmbeddingGemma 2 Text 270M**) handles chunk and query vector representations. Embedding inference is fundamentally distinct from generative LLM inference and runs separately.
3. **Sequential Execution Principle:** Embedding generation (for document ingestion) and generative chat (for conversational synthesis) are scheduled sequentially rather than concurrently to prevent out-of-memory (OOM) faults.

---

## 2. In-Depth Candidate Model Evaluation

### 2.1 Candidate A: MedGemma 1.5 4B IT (Preferred Single Generative Candidate)
* **Official Model Card:** [https://developers.google.com/health-ai-developer-foundations/medgemma/model-card](https://developers.google.com/health-ai-developer-foundations/medgemma/model-card)
* **Verified Community Artifact:** [https://huggingface.co/litert-community/MedGemma-1.5-4B-IT](https://huggingface.co/litert-community/MedGemma-1.5-4B-IT)
* **Target File:** `medgemma-1.5-4b-it_q4_block32_ekv2048.litertlm` (~2.41 GiB)
* **Architecture:** Developed by Google under the Health AI Developer Foundations (HAI-DEF) initiative. 4B parameter multimodal foundation model fine-tuned for clinical reasoning, EHR comprehension, and laboratory report extraction.
* **Intended Role in SwasthAI:**
  - General health-related user conversation.
  - Medical terminology and physiological function explanations.
  - Explaining synthetic laboratory reports.
  - Answering questions grounded in retrieved medical documents.
  - Summarizing saved medical history.
  - Explaining historical biomarker changes when supplied with verified structured data.
* **Supported Modalities in SwasthAI:** Text-only instruction input $\to$ Text output (executed within the `ekv2048` KV-cache ceiling; vision encoder is bypassed; document images are processed via on-device OCR before passing text to MedGemma).
* **Licensing & Access Conditions:** Google Health AI Developer Foundations (HAI-DEF) License. Gated access on Hugging Face; users must accept terms. **Strictly not approved as a standalone diagnostic medical device**.
* **Key Findings from Phase 0 Spike:**
  1. *Turnkey LiteRT-LM Artifact Confirmed:* The model is available in native `.litertlm` format (`medgemma-1.5-4b-it_q4_block32_ekv2048.litertlm`), eliminating the immediate need for a custom desktop conversion script unless this published package exhibits runtime faults.
  2. *Official Multi-Turn Limitation:* The official HAI-DEF model card explicitly states: **"The model has not been evaluated or optimized for multi-turn applications."** To overcome this limitation, the application layer manages conversation history via structured self-contained prompts (injecting recent user/assistant turns) rather than assuming native multi-turn conversation tuning.
  3. *Resource Requirements:* The 4-bit quantized artifact is 2.41 GiB on disk. Estimated in-memory resident footprint is ~2.7–3.1 GB RSS including the 2,048-token KV cache.

### 2.2 Candidate B: EmbeddingGemma 2 Text 270M (Dedicated Embedding Model)
* **Official Source:** [https://ai.google.dev/gemma/docs/embeddinggemma](https://ai.google.dev/gemma/docs/embeddinggemma) & [https://huggingface.co/litert-community/embeddinggemma-2-text-270m-litert-lm](https://huggingface.co/litert-community/embeddinggemma-2-text-270m-litert-lm)
* **Architecture:** Modular text-only core of EmbeddingGemma 2 (270M parameters) built on Gemma 4 architecture.
* **Intended Role in SwasthAI:** On-device semantic embedding generation for document chunks and user search queries.
* **Task Prefix Requirements:**
  - Search Query: `task: search result | query: {query}`
  - Document Chunk: `title: {title} | text: {chunk}` (or `title: none | text: {chunk}`)
* **Output Dimensions:** Native 768-dimensional float32 embeddings. Supports Matryoshka Representation Learning (MRL), allowing dimension truncation to 512, 256, or 128 dimensions.
* **Context Window:** 8,192 tokens.
* **Licensing:** Apache 2.0 License (Commercially permissive).
* **Target Footprint:** Quantized RAM footprint < 200 MB; latency target < 35ms per 256-token chunk.
* **Separation Guarantee:** Generates vector representations only; performs no text generation.

### 2.3 Documented Fallback Candidate: Gemma 4 E2B-it (Fallback Alternative Only)
* **Role:** Documented fallback alternative ONLY if MedGemma 1.5 4B fails on-device testing.
* **Status in MVP:** **Removed from default architecture.** Not loaded or bundled unless MedGemma runtime stability or multi-turn quality proves unviable.
* **Artifact:** `litert-community/gemma-4-E2B-it-litert-lm` (~2.58 GB).

---

## 3. Comprehensive Model Selection Matrix

| Field | Primary Generative Candidate | Dedicated Embedding Model | Fallback Generative Alternative |
| :--- | :--- | :--- | :--- |
| **Exact Model Name** | `MedGemma-1.5-4B-IT` | `embeddinggemma-2-text-270m-litert-lm` | `gemma-4-E2B-it-litert-lm` |
| **Artifact File** | `medgemma-1.5-4b-it_q4_block32_ekv2048.litertlm` | `embeddinggemma-2-text-270m.tflite` / `.litertlm` | `gemma-4-E2B-it.litertlm` |
| **Role in SwasthAI** | Single unified generative engine: health chat, report simplifier, grounded RAG, longitudinal narrative | Semantic chunk & query vector embedding | Emergency fallback if MedGemma fails on device |
| **Supported Modalities**| Text $\to$ Text (Text-only execution) | Text $\to$ 768-dim Vector | Text $\to$ Text |
| **Artifact Size** | **2.41 GiB** (Published fact) | **~270 MB** (Published fact) | **2.58 GiB** (Published fact) |
| **Resident RAM (Target)**| ~2.7 GB – 3.1 GB (Engineering estimate) | < 200 MB (Engineering estimate) | ~607 MB – 1.2 GB (Published benchmark) |
| **Inference Runtime** | LiteRT-LM (`com.google.ai.edge.litertlm:litertlm-android`) | LiteRT Core (`com.google.ai.edge.litert:litert`) | LiteRT-LM (`litertlm-android`) |
| **Android Support Status** | **Published `.litertlm` artifact exists; on-device load pending** | **Verified in documentation** | **Verified in documentation** |
| **Quantization Format** | 4-bit weight-only (`q4_block32`) | 8-bit integer / dynamic range | 4-bit weight-only |
| **Licensing** | HAI-DEF License (Research & Commercial, Non-Diagnostic) | Apache 2.0 (Permissive) | Gemma Open Terms |
| **Target Hardware Acceleration** | Adreno 830 GPU / CPU XNNPACK | Adreno GPU / CPU XNNPACK | Adreno GPU / Hexagon NPU |
| **Suitability Status** | **Preferred Candidate (Subject to Device Test)** | **Recommended Primary Embedding** | **Documented Fallback Only** |
| **Current Readiness** | Artifact published on Hugging Face; test harness built | Supported in LiteRT ecosystem; probe built | Turnkey in LiteRT ecosystem |

---

## 4. Technical Claims Audit: Fact vs. Estimate vs. Assumption

To maintain complete engineering integrity, all technical claims across the SwasthAI documentation are explicitly classified below:

| Claim Description | Category | Current Empirical Basis | Engineering Status |
| :--- | :--- | :--- | :--- |
| **`litert-community/MedGemma-1.5-4B-IT` artifact exists** | **Verified Fact** | Verified on Hugging Face (`medgemma-1.5-4b-it_q4_block32_ekv2048.litertlm`, 2.41 GiB) | Confirmed available for download under HAI-DEF gating. |
| **MedGemma 1.5 is NOT clinically validated as a diagnostic device** | **Verified Fact** | Explicitly stated in Google's official HAI-DEF model card | Enforce non-diagnostic disclaimers and deterministic guardrails. |
| **MedGemma 1.5 is NOT evaluated or optimized for multi-turn chat** | **Verified Fact** | Documented directly in official MedGemma model card | Mitigated via application-level self-contained prompt construction. |
| **EmbeddingGemma 2 Text 270M supports 768-dim vectors & 8K context** | **Verified Fact** | Official Google AI Edge documentation; Apache 2.0 license | Validated probe implemented in `:model-test-harness`. |
| **Gemma 4 E2B-it memory footprint can be as low as ~607MB** | **Published Benchmark** | Google AI Edge published benchmarks for LiteRT-LM | Retained as verified fallback alternative. |
| **MedGemma 1.5 artifact will load without JNI crash on Galaxy S25** | **Untested Assumption** | Theoretical compatibility of LiteRT-LM runtime with community artifact | **Test pending user push of 2.41 GiB file to device.** |
| **Adreno 830 GPU OpenCL delegate will execute MedGemma without fallback** | **Untested Assumption** | Snapdragon 8 Elite Adreno driver compatibility | Test harness provides toggle for GPU vs CPU XNNPACK fallback. |
| **Total app resident memory will remain <= 3.5 GB during MedGemma generation** | **Engineering Estimate** | Based on 2.41 GB weights + 500 MB KV + 300 MB app UI | Measured dynamically via `Debug.getPss()` in test harness. |
| **Snapdragon 8 Elite will sustain >= 12–15 tokens/sec for MedGemma 1.5** | **Proposed Target** | Target based on 8 Elite TOPS and 4B arithmetic | Target to be recorded in test harness benchmark card. |
| **Time-to-First-Token will be < 1,200 ms with 512 context tokens** | **Proposed Target** | Target threshold for responsive mobile UX | Target to be recorded in test harness benchmark card. |

---

## 5. MedGemma 1.5 Feasibility Protocol & Device Benchmark Harness

The minimal test harness module (`:model-test-harness`) has been constructed to execute the **Gate 1 Feasibility Protocol**:

```mermaid
graph TD
    A[Start Gate 1: Model Feasibility Verification] --> B[User Downloads 2.41 GiB medgemma-...litertlm from Hugging Face]
    B --> C[Push to Device: adb push artifact.litertlm /sdcard/Download/]
    C --> D[Launch Test Harness: com.swasthai.testharness/.ModelTestHarnessActivity]
    
    D --> E{Step 1: Engine Initialization}
    E -->|Crash / Incompatible Kernel| F[Toggle CPU XNNPACK Fallback in Harness]
    F -->|Still Fails| G[TRIGGER FALLBACK: Deploy Gemma-4 E2B-it]
    E -->|Success: Model Initialized| H[Step 2: Run Benchmark Prompts 1, 2, 3]
    
    H --> I{Step 3: Evaluate Metrics}
    I -->|Peak RSS > 3.5 GB or OOM| G
    I -->|Throughput < 8 tps or Severe Heat| G
    I -->|Throughput >= 10 tps & Memory Stable| J[Step 4: Run Multi-Turn Self-Contained Test (Prompt 4)]
    
    J --> K{Step 5: Coherence Check}
    K -->|Coherent Response| L[APPROVE MEDGEMMA 1.5 AS SINGLE GENERATIVE ENGINE (GO)]
    K -->|Incoherent Context Drift| M[Refine Turn Summary Template; If Unresolvable -> Fallback]
```

### The 6 Measured Device Gates in the Test Harness:
1. **Model Initialization Gate:** Model loads into memory via `Engine(config).initialize()`; cold-start loading time recorded.
2. **Memory Ceiling Gate:** Peak resident process memory (measured via `Debug.getPss()`) must not exceed **3.5 GB RSS** on Galaxy S25.
3. **Execution Throughput Gate:** Generates >= 10 tokens/second on Adreno GPU or CPU delegate without immediate thermal throttling.
4. **Text-Only Instruction Gate:** Correctly processes text-only medical questions formatted with standard Gemma tokens.
5. **Multi-Turn Coherence Gate:** Successfully answers Prompt 4 (self-contained conversation history + retrieved evidence + current question).
6. **Grounding & Citation Gate:** Ingests synthetic CBC report context and answers with correct numerical values.
