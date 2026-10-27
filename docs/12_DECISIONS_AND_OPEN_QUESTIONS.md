# SwasthAI: Architecture Decision Records (ADRs) & Open Questions

**Document Version:** 1.2.0  
**Classification:** Technical Governance & Risk Ledger  
**Status:** Phase 0 Feasibility Findings Updated (Verified Community Artifacts)  

---

## 1. Confirmed Architecture Decision Records (ADRs)

### ADR-01: Native Android (Kotlin + Jetpack Compose) Over Cross-Platform Frameworks
* **Decision:** Develop SwasthAI as a native Android application in Kotlin using Jetpack Compose.
* **Rationale:** Direct access to native C++ NDK inference runtimes (LiteRT-LM, Adreno OpenCL, Hexagon QNN), hardware StrongBox Keystore, and smooth 120Hz UI rendering.

---

### ADR-02: Deferral of Gemini Live-Style Camera & Voice Streaming
* **Decision:** Strictly postpone continuous camera streaming, live voice interaction, and Box integration to future research.
* **Rationale:** High thermal saturation, rapid battery drain, and poor conversational quality on current mobile hardware. Immediate user value lies in solving report opacity, longitudinal tracking, and grounded RAG.

---

### ADR-03: Primary On-Device Runtime: Google LiteRT Stack
* **Decision:** Standardize on Google LiteRT-LM (`com.google.ai.edge.litertlm:litertlm-android`) for LLM generation and LiteRT Core (`com.google.ai.edge.litert:litert`) for embedding inference.
* **Rationale:** Production-ready on-device framework developed specifically for Android edge deployment, supporting quantized flatbuffers (`.litertlm`), dynamic KV cache management, and hardware acceleration on Qualcomm Snapdragon processors.

---

### ADR-04: Single Generative Model Strategy: MedGemma 1.5 4B IT
* **Decision:** Adopt **MedGemma 1.5 4B IT** via the existing community-converted artifact (`litert-community/MedGemma-1.5-4B-IT`, `medgemma-1.5-4b-it_q4_block32_ekv2048.litertlm`, 2.41 GiB) as the preferred single generative model for ALL conversational and medical explanation tasks in the MVP.
* **Rationale:**
  1. Prevents multi-model weight bloat and memory contention.
  2. The community-converted `.litertlm` artifact removes the immediate need for a desktop conversion pipeline.
  3. Pre-trained on medical terminology, clinical reasoning, and laboratory report comprehension.
* **Explicit Limitation & Mitigation:** MedGemma’s official documentation notes it has not been evaluated or optimized for multi-turn applications. SwasthAI resolves this at the application layer via self-contained prompts (injecting recent conversation summary + retrieved evidence + current question within the 2,048 prefill limit).
* **Documented Fallback:** Gemma-4 E2B-it is retained as an alternative fallback *only* if MedGemma on-device initialization or multi-turn quality fails testing on the Galaxy S25.

---

### ADR-05: Dedicated Separate Embedding Model: EmbeddingGemma 2 Text 270M
* **Decision:** Deploy a separate, dedicated on-device embedding model (**EmbeddingGemma 2 Text 270M**, `litert-community/embeddinggemma-2-text-270m-litert-lm`) for chunk and query vector embedding.
* **Rationale:**
  1. Embedding generation is mathematically and architecturally distinct from generative token emission.
  2. At 270M parameters, the text-only core requires < 200MB of RAM and executes in < 35ms per chunk.
  3. Uses required official task prefixes: `task: search result | query: ` for queries, and `title: {title} | text: {chunk}` for documents.
  4. Native support for 768 dimensions and Matryoshka Representation Learning (truncation to 512, 256, 128).

---

### ADR-06: Hybrid Retrieval Architecture (Vector + Relational SQL)
* **Decision:** Combine semantic vector search with deterministic SQL queries over Room database tables.
* **Rationale:** Pure vector similarity fails at distinguishing precise numbers (e.g., 242 vs 142) and evaluating chronological order. Hybrid retrieval routes numerical questions to SQL and conceptual questions to vectors.

---

### ADR-07: Hardware-Backed Encryption at Rest
* **Decision:** Encrypt all databases (SQLCipher) and vault files (`EncryptedFile`) with AES-256-GCM using keys generated in the Qualcomm StrongBox Keymaster.
* **Rationale:** Guarantees that sensitive health records remain protected even if physical storage is dumped via ADB.

---

### ADR-08: Deterministic Emergency Escalation (India 112 Standard)
* **Decision:** Intercept acute emergency symptoms using a deterministic regex/keyword filter *prior* to LLM invocation, immediately displaying an emergency card with a one-tap `tel:112` dialer button.
* **Rationale:** Generative AI cannot be trusted with life-or-death emergency triage. Zero latency (<5ms) and 100% deterministic reliability are clinically mandatory.

---

## 2. Technical Claims Audit & Classification (Phase 0 Spike Findings)

| Claim Description | Category | Current Empirical Basis | Status |
| :--- | :--- | :--- | :--- |
| **`litert-community/MedGemma-1.5-4B-IT` artifact exists** | **Verified Fact** | Verified on Hugging Face (`medgemma-1.5-4b-it_q4_block32_ekv2048.litertlm`, 2.41 GiB) | **Confirmed** |
| **MedGemma 1.5 is NOT a clinically validated diagnostic device** | **Verified Fact** | Explicitly stated in Google's official HAI-DEF model card | **Enforced in Design** |
| **MedGemma 1.5 is NOT evaluated/optimized for multi-turn chat** | **Verified Fact** | Explicitly stated in Google's official HAI-DEF model card | **Mitigated in Harness** |
| **EmbeddingGemma 2 Text 270M supports 768-dim embeddings & prefixes** | **Verified Fact** | Official Google AI Edge documentation; Apache 2.0 license | **Verified in Probe** |
| **Gemma 4 E2B-it memory footprint can be as low as ~607MB** | **Published Benchmark** | Google AI Edge published benchmarks for LiteRT-LM | **Retained as Fallback** |
| **MedGemma 1.5 artifact will load without JNI crash on Galaxy S25** | **Untested Assumption** | Theoretical compatibility of LiteRT-LM runtime with community artifact | **Test Harness Built; Device Push Pending** |
| **Adreno 830 GPU OpenCL delegate will execute MedGemma without fallback** | **Untested Assumption** | Snapdragon 8 Elite Adreno driver compatibility | **Harness provides GPU/CPU toggle** |
| **Total app resident memory will remain <= 3.5 GB RSS** | **Engineering Estimate** | Based on 2.41 GB weights + 500 MB KV + 300 MB UI | **To be measured on device** |
| **Snapdragon 8 Elite will sustain >= 12–15 tokens/sec for MedGemma 1.5** | **Proposed Target** | Target based on 8 Elite TOPS and 4B arithmetic | **To be measured on device** |
| **Time-to-First-Token will be < 1,200 ms with 512 context** | **Proposed Target** | Target threshold for mobile UX | **To be measured on device** |

---

## 3. Unresolved Technical Risks & Risk Resolution Status

### Risk 1: On-Device Load & GPU Delegate Compatibility
* **Previous Assessment:** Stalled on whether a desktop model conversion script would succeed.
* **Phase 0 Finding:** **Resolved / De-risked.** The pre-converted community artifact `medgemma-1.5-4b-it_q4_block32_ekv2048.litertlm` (2.41 GiB) is published on Hugging Face. Custom conversion is no longer an immediate blocker.
* **Remaining Risk:** Verifying that the Adreno 830 GPU OpenCL driver on the Galaxy S25 initializes this artifact without native kernel faults.
* **Resolution Action:** User executes `adb push` of the artifact to the device and taps "Initialize Model" in `:model-test-harness`.

### Risk 2: Multi-Turn Context Coherence Under 2,048 KV Boundary
* **Status:** Mitigated in code; pending device verification.
* **Resolution Action:** Test Prompt 4 in `:model-test-harness` evaluates the application-prepared self-contained prompt to verify coherent follow-up questions within the `ekv2048` prefill budget.

### Risk 3: Qualcomm Hexagon NPU Support
* **Status:** Classified as an unverified assumption.
* **Resolution Action:** Deferred to post-hackathon optimization; Phase 0/MVP focuses strictly on Adreno GPU and CPU XNNPACK.

---

## 4. Phase 0 Recommendation: Provisional GO for Implementation

* **Current Status:** **PROVISIONAL GO (with Fallback Safety Net)**.
* **Rationale:**
  1. Both required model artifacts (`MedGemma 1.5 4B IT` and `EmbeddingGemma 2 Text 270M`) have been identified in official/community LiteRT-LM repositories.
  2. The test harness (`:model-test-harness`) has been built, providing clean runtime isolation, benchmark timers, and error handling.
  3. The physical target device (Galaxy S25, `SM-S931B`, Snapdragon 8 Elite) is confirmed connected and recognized via ADB.
  4. If the device test of MedGemma encounters unexpected kernel issues, the architecture can swap to `gemma-4-E2B-it-litert-lm` with zero impact on database, vault, or RAG code.
* **Next Immediate Step:** User pushes the 2.41 GiB model file to the device, executes the 4 benchmark prompts in `:model-test-harness`, and confirms the final **FULL GO** for Phase 1.
