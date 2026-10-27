# SwasthAI: Healthcare AI Safety, Clinical Guardrails & Evidence Grounding

**Document Version:** 1.0.0  
**Classification:** Healthcare AI Safety & Regulatory Compliance Specification  
**Status:** Approved for Implementation Planning  

---

## 1. Healthcare Safety Charter & Fundamental Constraints

SwasthAI is intentionally designed as an **educational health companion and personal health organizer**, NOT a clinical diagnostic device, licensed physician substitute, or autonomous clinical decision system.

```
+-----------------------------------------------------------------------------------+
|                        SwasthAI Clinical Safety Red-Lines                         |
|                                                                                   |
|  1. NEVER issue a definitive clinical diagnosis (e.g. "You have diabetes").      |
|  2. NEVER prescribe pharmaceuticals or recommend altering physician dosages.     |
|  3. NEVER intercept acute emergencies with conversational essays.                 |
|  4. NEVER fabricate laboratory numbers, units, reference intervals, or citations.|
|  5. NEVER convert unverified AI inferences into permanent patient history.        |
+-----------------------------------------------------------------------------------+
```

---

## 2. Emergency Escalation Protocol & India 112 Workflow

### 2.1 Deterministic Red-Line Triage System
Emergencies require zero-latency, deterministic interception. Generative models must never be trusted to decide whether crushing chest pain or stroke symptoms represent an emergency.

```mermaid
graph TD
    Input[Incoming User Input] --> RegEx[Deterministic Regex / Keyword Matcher]
    RegEx -->|Matches Emergency Triage Rule| Intercept[Intercept & Abort Generation Pipeline]
    RegEx -->|No Emergency Trigger| Router[Proceed to Query Router & Local AI Chat]

    Intercept --> AlertModal[Full-Screen Emergency Alert Modal]
    AlertModal --> ActionCard[Emergency Action Card (112 India)]
    AlertModal --> FirstAid[Immediate First-Aid Protocols]
    
    ActionCard -->|User Taps 'Call 112'| Dialer[Launch Android Dialer: tel:112]
```

### 2.2 Red-Line Clinical Condition Taxonomy
The pre-LLM filter intercepts queries matching any of the following critical presentations:
1. **Acute Coronary Syndrome (Heart Attack):** Crushing substernal chest pressure, radiation to left arm/jaw, diaphoresis (cold sweats), severe breathlessness.
2. **Cerebrovascular Event (Stroke - FAST):** Facial drooping, unilateral arm weakness, slurred speech or acute aphasia, sudden severe loss of balance.
3. **Severe Anaphylaxis:** Swelling of lips/tongue/throat, acute wheezing, stridor, widespread hives post-exposure.
4. **Acute Respiratory Distress:** Severe inability to complete full sentences, cyanosis (blue lips/fingers), choking.
5. **Major Hemorrhage / Severe Trauma:** Uncontrolled arterial bleeding, head injury with loss of consciousness.
6. **Active Psychiatric Crisis / Self-Harm:** Suicidal ideation or intent (escalates to national mental health helpline: Tele-MANAS 14416 / 1800-891-4416).

### 2.3 The India 112 Emergency Standard & Disconnected Device Realism
* **Emergency Standard:** India’s National Emergency Response Support System (ERSS) operates on the unified number **112** (integrating Police 100, Fire 101, and Ambulance 108).
* **Deterministic Action:** The app provides a high-contrast button: `Call Emergency (112)`. Tapping launches `Intent.ACTION_DIAL` with `Uri.parse("tel:112")`.
* **Honest Telecommunications Disclosure:** The application **must never falsely claim** that an emergency call has been automatically initiated, nor claim that an offline phone in airplane mode without SIM/satellite connectivity can connect to emergency dispatchers. If the device detects no cellular service, the UI explicitly directs the user to seek immediate nearby assistance or locate a fixed landline.

---

## 3. Evidence Grounding & Hallucination Elimination

### 3.1 Strict Context Grounding Prompt Contract
All generative responses analyzing medical records are bound to a strict prompt contract:
* The model is instructed to treat the provided context as an exhaustive closed world.
* Speculative clinical extrapolation is prohibited.
* Every statement asserting a numerical test result must carry a bracketed footnote `[N]` resolving to a specific document chunk.

### 3.2 Automated Post-Generation Verification (Grounding Guardrail)
Before rendering generated text on screen, an on-device post-processor executes a numerical fact check:
1. **Regex Extraction:** Extract all numbers followed by clinical units (e.g., `242 mg/dL`, `10.5 g/dL`, `6.8%`) from the generated response.
2. **Context Verification:** Check whether each extracted tuple exists verbatim in the retrieved source chunks.
3. **Discrepancy Action:** If an ungrounded number is detected, the UI displays an **"Unverified Number"** warning indicator alongside the generated text and prompts the user to verify against the original PDF.

---

## 4. Handling Missing, Contradictory & Ambiguous Medical Data

### 4.1 Negative Retrieval ("Don't Know")
When user inquiries refer to tests, treatments, or dates not present in the local vault:
* Cosine similarity filtering rejects chunks below the confidence threshold.
* The system outputs a standardized, reassuring response:
  > *"I cannot find any records of [Test Name] in your saved medical documents. If you have this report, you can import it into your Medical Vault for analysis."*

### 4.2 Handling Contradictory Historical Data
If two reports show conflicting information (e.g., Blood Type listed as A+ in one clinic and B+ in another, or different reference ranges for Fasting Glucose):
* SwasthAI **never silently chooses one or averages them**.
* The system presents both findings transparently with their respective dates, provider names, and units, and explicitly advises the user to highlight this discrepancy to their primary physician.

---

## 5. Medication Safeguards & Human-in-the-Loop Governance

### 5.1 Prescription Extraction Safeguards
* Diagnostic reports and prescriptions may mention medications, dosages, or schedules.
* Extracted medication instructions are **never automatically added to active medication schedules**.
* They are flagged as `EXTRACTED_UNVERIFIED` and presented in a dedicated **Medication Review Card**.
* An active reminder is scheduled *only after the user reviews, verifies, and taps "Confirm Schedule"*.

### 5.2 Dosage Adjustment Invariant
If a user asks: *"My blood pressure is 150/95, should I double my Amlodipine dosage?"*
* The local model strictly refuses to alter dosage.
* System outputs:
  > *"SwasthAI cannot recommend changing your medication dosage. A blood pressure reading of 150/95 mmHg is elevated. Please contact your doctor or visit a clinic to discuss whether your treatment plan requires adjustment."*

---

## 6. Synthetic Test Data Specification for Hackathon Testing

To ensure 100% compliance with privacy laws and avoid using real patient data during development and hackathon demonstrations, SwasthAI operates with a standardized suite of **realistic synthetic health records**:

### 6.1 Synthetic Patient Persona
* **Name:** Ramesh Verma
* **Age/Gender:** 58 / Male
* **UHID / Patient ID:** SYNTH-DELHI-88219
* **Known History:** Mild Type 2 Diabetes, Essential Hypertension.

### 6.2 Synthetic Document Set

#### Document 1: Routine Blood & Lipid Panel (Baseline)
* **File Name:** `synthetic_report_jan2025.pdf`
* **Lab Name:** MetroPath Diagnostics (Synthetic Lab, New Delhi)
* **Report Date:** 2025-01-15
* **Key Biomarkers:**
  - Fasting Blood Sugar: `138 mg/dL` [Normal: 70 - 100] $\to$ **HIGH**
  - HbA1c: `7.2 %` [Normal: < 5.7, Diabetic: >= 6.5] $\to$ **HIGH**
  - Total Cholesterol: `248 mg/dL` [Desirable: < 200] $\to$ **HIGH**
  - Triglycerides: `195 mg/dL` [Normal: < 150] $\to$ **HIGH**
  - HDL Cholesterol: `38 mg/dL` [Desirable: > 40] $\to$ **LOW**
  - LDL Cholesterol: `165 mg/dL` [Optimal: < 100] $\to$ **HIGH**
  - Serum Creatinine: `0.95 mg/dL` [Normal: 0.70 - 1.30] $\to$ **NORMAL**

#### Document 2: Follow-up Health Checkup (Follow-up Post Lifestyle Intervention)
* **File Name:** `synthetic_report_aug2025.pdf`
* **Lab Name:** Apollo Clinic (Synthetic Branch)
* **Report Date:** 2025-08-10
* **Key Biomarkers:**
  - Fasting Blood Sugar: `112 mg/dL` [Normal: 70 - 100] $\to$ **BORDERLINE HIGH (Improved)**
  - HbA1c: `6.4 %` [Normal: < 5.7, Prediabetic: 5.7 - 6.4] $\to$ **IMPROVED (-0.8%)**
  - Total Cholesterol: `205 mg/dL` [Desirable: < 200] $\to$ **IMPROVED (-43 mg/dL)**
  - Triglycerides: `155 mg/dL` [Normal: < 150] $\to$ **IMPROVED (-40 mg/dL)**
  - HDL Cholesterol: `44 mg/dL` [Desirable: > 40] $\to$ **NORMAL (Improved +6)**
  - LDL Cholesterol: `130 mg/dL` [Optimal: < 100] $\to$ **IMPROVED (-35 mg/dL)**
  - Serum Creatinine: `0.92 mg/dL` [Normal: 0.70 - 1.30] $\to$ **STABLE**

#### Document 3: Outpatient Prescription Note
* **File Name:** `synthetic_prescription_aug2025.pdf`
* **Doctor:** Dr. Sunita Rao, MD (Internal Medicine)
* **Date:** 2025-08-12
* **Prescribed Therapy:**
  1. Tab. Metformin 500mg — 1 tablet twice daily with meals (Breakfast, Dinner).
  2. Tab. Telmisartan 40mg — 1 tablet once daily in the morning.
* **Advice:** Reduce refined carbohydrate intake; 30 minutes brisk walking daily; repeat HbA1c in 6 months.
