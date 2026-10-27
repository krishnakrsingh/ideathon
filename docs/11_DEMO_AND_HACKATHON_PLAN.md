# SwasthAI: Hackathon Demonstration Script, Video Storyboard & Impact Plan

**Document Version:** 1.1.0  
**Classification:** Product Pitch & Hackathon Deliverables Specification  
**Status:** Approved for Implementation Planning (Revised Single Generative Model Architecture)  

---

## 1. Live Demonstration Script (3-Minute Hackathon Presentation)

**Device Setup:** Samsung Galaxy S25 connected to external display / screen mirror; **Airplane Mode strictly ON** (Wi-Fi OFF, Cellular OFF).

```mermaid
journey
    title 3-Minute Live Hackathon Demonstration
    section Opening (0:00 - 0:30)
      Show Airplane Mode Active: 5: Presenter
      Ask general health question to MedGemma: 5: Judge
      Observe instant streaming token output: 5: Judge
    section The Medical Vault (0:30 - 1:15)
      Import synthetic CBC & Lipid PDF: 5: Presenter
      Watch on-device OCR, chunking & EmbeddingGemma index: 5: Judge
      Review structured biomarker extraction: 5: Judge
    section Grounded RAG & Verification (1:15 - 2:00)
      Ask: "What were my cholesterol numbers and what do they mean?": 5: Judge
      Read grounded explanation with [1] citation: 5: Judge
      Tap [1] to jump directly to original PDF page: 5: Judge
    section Longitudinal Comparison (2:00 - 2:30)
      Select Jan 2025 vs Aug 2025 reports: 5: Presenter
      View deterministic delta grid (-43 mg/dL cholesterol): 5: Judge
      Toggle Hindi language explanation: 5: Judge
    section Safety & Grand Finale (2:30 - 3:00)
      Type red-line emergency query (Chest pain): 5: Presenter
      Observe instant (<5ms) Emergency Alert & 112 Action Card: 5: Judge
      Summarize air-gap privacy guarantee: 5: Judge
```

### Detailed Step-by-Step Script
1. **The Air-Gap Proof (0:00 - 0:25):**
   * *Action:* Presenter pulls down the Android Quick Settings panel showing Airplane Mode active.
   * *Spoken Hook:* *"Judges, healthcare privacy shouldn't depend on trusting cloud servers. SwasthAI runs completely on this Samsung Galaxy S25. No internet, no API keys, zero data leakage."*
   * *Action:* Types *"Explain what HDL cholesterol does in the human body."* Streaming tokens appear powered by our on-device MedGemma 1.5 4B IT engine.
2. **Medical Vault & Document Ingestion (0:25 - 0:55):**
   * *Action:* Opens Medical Vault. Taps **Import Report**. Selects `synthetic_report_jan2025.pdf`.
   * *Screen:* Progressive indicator completes: parsing text, chunking, and embedding with `EmbeddingGemma 300M`.
   * *Spoken:* *"We don't just dump text into an LLM. SwasthAI deterministically structures biomarkers into a local database and indexes semantic chunks separately."*
3. **Grounded RAG with Document Jump (0:55 - 1:35):**
   * *Action:* In chat, asks: *"What were my cholesterol numbers in this report, and are they normal?"*
   * *Screen:* MedGemma streams answer: *"Your Total Cholesterol was 248 mg/dL [1], which is above the desirable limit of 200 mg/dL..."*
   * *Action:* Presenter taps footnote `[1]`.
   * *Screen:* Bottom drawer opens; tapping "View Source" opens the built-in PDF viewer with Page 1 displayed and the Lipid Profile highlighted.
   * *Spoken:* *"Every claim is grounded. The user can verify the original laboratory report with a single tap."*
4. **Longitudinal Report Comparison (1:35 - 2:10):**
   * *Action:* Navigates to **Report Simplifier & Trends**. Selects Jan 2025 and Aug 2025 reports.
   * *Screen:* Renders the comparison grid: Fasting Blood Sugar down from 138 to 112 mg/dL; Total Cholesterol down from 248 to 205 mg/dL.
   * *Action:* Switches language toggle to **Hindi**.
   * *Screen:* MedGemma renders plain-language Hindi summary: *"आपके कोलेस्ट्रॉल में 43 mg/dL का सुधार हुआ है..."*
5. **Clinical Emergency Escalation (2:10 - 2:45):**
   * *Action:* Presenter types: *"My father has severe crushing chest pain and difficulty breathing."*
   * *Screen:* Instantly (<5ms), without LLM invocation, the red Emergency Alert Modal appears with the **"Call 112"** button.
   * *Spoken:* *"Generative AI can hallucinate. For acute emergencies, SwasthAI enforces deterministic triage rules, instantly offering India's unified 112 emergency response."*
6. **Closing (2:45 - 3:00):**
   * *Spoken:* *"One coherent app. Specialized MedGemma local AI. True RAG. Grounded truth. Zero cloud. This is SwasthAI."*

---

## 2. Technical Differentiators for Judges

| Feature Dimension | Typical Hackathon Project | SwasthAI Flagship Architecture |
| :--- | :--- | :--- |
| **Inference Location** | Calls OpenAI / Gemini API over Wi-Fi | 100% on-device via LiteRT on Snapdragon 8 Elite |
| **Model Efficiency** | Monolithic multi-model bloat or cloud API | Single specialized generative engine (MedGemma) + lightweight embedding engine |
| **RAG Implementation** | Naive prompt stuffing (pasting whole file) | Semantic chunking + EmbeddingGemma + Vector Store + Relational SQL |
| **Source Provenance** | Unreferenced AI text | Inline bracketed footnotes `[1]` jumping directly to PDF page & coordinates |
| **Numerical Accuracy**| LLM guesses whether 138 mg/dL is high | Deterministic mathematical comparison against printed reference ranges |
| **Emergency Safety** | LLM gives a paragraph of disclaimers | Deterministic pre-LLM regex interceptor launching native 112 emergency dialer |
| **Data Security** | Plaintext files in Downloads folder | AES-256-GCM encryption at rest backed by Android Keystore StrongBox |

---

## 3. One-Slide Impact Plan

```
+---------------------------------------------------------------------------------------------------+
| SWASTHAI: ONE-SLIDE IMPACT & ADOPTION ROADMAP                                                     |
+---------------------------------------------------------------------------------------------------+
| 1. TARGET EARLY ADOPTERS                                                                           |
|    - Primary: 75M+ Indians managing Type 2 Diabetes & Hypertension who receive quarterly lab tests|
|    - Secondary: Family caregivers managing elderly parents' multi-specialist medical records      |
|    - Tertiary: Semi-urban and rural users seeking plain-language vernacular (Hindi) explanations  |
+---------------------------------------------------------------------------------------------------+
| 2. USER ACQUISITION STRATEGY                                                                      |
|    - Phase 1 (First 100 Users): Closed pilot with chronic diabetes clinic in New Delhi NCR         |
|    - Phase 2 (1,000 Users): Open-source release on F-Droid / GitHub + patient caregiver advocacy   |
|    - Phase 3 (Scaling): Diagnostic chain integration (QR code on printed reports to open locally)  |
+---------------------------------------------------------------------------------------------------+
| 3. MEASURABLE VALUE METRICS (HYPOTHESES TO VALIDATE)                                              |
|    - Comprehension Uplift: Target >= 80% increase in patient understanding of abnormal lab flags |
|    - Consultation Efficiency: Save 3-5 minutes per clinic visit via 1-page structured doctor summary|
|    - Privacy Compliance: 100% on-device data sovereignty; 0 bytes transmitted off-device          |
|    - Zero Missed Emergencies: 100% deterministic interception of acute cardiac/stroke symptoms   |
+---------------------------------------------------------------------------------------------------+
```

---

## 4. 45–90 Second Demonstration Video Storyboard

* **Total Runtime:** 60 Seconds  
* **Music:** Subtle, modern, rhythmic tech-ambient track  
* **Style:** High-resolution screen recording of physical Galaxy S25 + live hand interaction  

| Timecode | Visual Shot Description | On-Screen Caption / Graphic | Voiceover Script |
| :--- | :--- | :--- | :--- |
| **0:00 - 0:08** | Close-up of physical Galaxy S25. Phone pulled down to show **Airplane Mode** enabled. Quick cut to a stack of confusing medical lab reports. | *No Internet. Complete Privacy.* | *"Every year, millions receive complex medical reports they can't understand. And uploading them to cloud AI compromises your most sensitive data."* |
| **0:08 - 0:18** | User imports a synthetic blood test PDF in SwasthAI. UI shows on-device OCR, chunking, and vector indexing completing in seconds. | *On-Device Medical Vault & EmbeddingGemma* | *"Meet SwasthAI. An offline-first healthcare companion running specialized local AI directly on your smartphone."* |
| **0:18 - 0:32** | User asks: *"Explain my cholesterol numbers."* Instant streaming response from MedGemma with interactive `[1]` citation. Tapping `[1]` jumps directly to the original PDF page. | *MedGemma 1.5 4B IT & Grounded Citations* | *"Ask questions in plain language. SwasthAI performs genuine on-device RAG, grounding every explanation in your actual report with verifiable citations."* |
| **0:32 - 0:44** | Screen shows two reports side-by-side. Blood sugar deltas displayed with green "Improved" badges. Switched to Hindi with one tap. | *Longitudinal Tracking & Multilingual Support* | *"Track your health over time. Compare tests across months, view deterministic trends, and understand your health in your native language."* |
| **0:44 - 0:54** | User types *"Severe chest pain radiating to left arm."* Instant Red Emergency Modal appears with prominent `Call 112` button. | *Deterministic Emergency Safety (112 India)*| *"And for emergencies, SwasthAI never hallucinates. Built-in deterministic guardrails instantly connect you to emergency care."* |
| **0:54 - 1:00** | SwasthAI logo animation, open-weight model badges (MedGemma 1.5 4B, LiteRT, EmbeddingGemma). | *SwasthAI: Private. Local. Grounded.* | *"Your health. Your device. Your privacy. SwasthAI."* |
