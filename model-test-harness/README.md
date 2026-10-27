# SwasthAI: Phase 0 Model Feasibility Test Harness

**Module:** `:model-test-harness`  
**Target Hardware:** Samsung Galaxy S25 (SM-S931B, Snapdragon 8 Elite, 12 GB RAM)  
**Primary Objective:** Empirically validate on-device execution of `MedGemma 1.5 4B IT` and separate `EmbeddingGemma 2 Text 270M` before building application features.

---

## 1. Verified Model Artifacts

### 1.1 Preferred Generative Artifact
* **Repository:** [https://huggingface.co/litert-community/MedGemma-1.5-4B-IT](https://huggingface.co/litert-community/MedGemma-1.5-4B-IT)
* **File Name:** `medgemma-1.5-4b-it_q4_block32_ekv2048.litertlm`
* **File Size:** ~2.41 GiB (2,586,812,416 bytes)
* **Quantization:** 4-bit (`q4_block32`)
* **KV-Cache Limit:** 2,048 tokens (`ekv2048`)
* **License & Gating:** Google Health AI Developer Foundations (HAI-DEF) Terms of Use. Access must be granted on Hugging Face before downloading. **Do not bypass gating terms or commit weights to Git.**

### 1.2 Dedicated Embedding Artifact
* **Repository:** [https://huggingface.co/litert-community/embeddinggemma-2-text-270m-litert-lm](https://huggingface.co/litert-community/embeddinggemma-2-text-270m-litert-lm)
* **Model Type:** Text-only core (270M parameters)
* **Dimensions:** 768 float32 (Matryoshka truncation: 512, 256, 128)
* **License:** Apache 2.0 (Permissive)

---

## 2. Hardware Deployment & Device Setup (Galaxy S25)

The Samsung Galaxy S25 (Device ID: `RZGL325K28P`) is connected via USB debugging with Android 15 / One UI 7.

### Step 1: Push Model File to Device
Once the user downloads `medgemma-1.5-4b-it_q4_block32_ekv2048.litertlm` from Hugging Face to their local machine:
```bash
# Push model file to device Download directory
adb -s RZGL325K28P push "C:\path\to\medgemma-1.5-4b-it_q4_block32_ekv2048.litertlm" /sdcard/Download/

# Verify file presence and size on device
adb -s RZGL325K28P shell ls -lh /sdcard/Download/medgemma-1.5-4b-it_q4_block32_ekv2048.litertlm
```

### Step 2: Build and Install the Test Harness App
```bash
cd c:\Users\krish\Downloads\ideathon
.\gradlew :model-test-harness:installDebug
```

### Step 3: Launch the Test Harness Activity
```bash
adb -s RZGL325K28P shell am start -n com.swasthai.testharness/.ModelTestHarnessActivity
```

---

## 3. Benchmark Execution Procedure

1. **Verify Air-Gap:** Toggle Airplane Mode ON on the Galaxy S25 (Wi-Fi OFF, Cellular OFF).
2. **Model Initialization:**
   - In the test harness UI, verify the path `/sdcard/Download/medgemma-1.5-4b-it_q4_block32_ekv2048.litertlm`.
   - Select **Adreno GPU Delegate**.
   - Tap **Initialize Model**.
   - Record the measured Cold-Start Load Time (ms).
3. **Run Benchmark Prompts:**
   - **Prompt 1 (General Health):** *"What is the primary function of high-density lipoprotein (HDL) cholesterol in cardiovascular health?"*
   - **Prompt 2 (Medical Term):** *"Explain what microcytic hypochromic anemia indicates in a complete blood count panel and its common physiological causes."*
   - **Prompt 3 (Report-Grounded):** Synthetic CBC/Lipid panel excerpt (5 measurements).
   - **Prompt 4 (Multi-Turn Test):** Application-prepared self-contained prompt containing conversation summary + evidence + follow-up question.
4. **Record Observations in Matrix:**
   - Time to First Token (TTFT) (ms)
   - Tokens per Second (tps)
   - Peak Process Memory (PSS / RSS) (MB)
   - Response Coherence and Accuracy
   - Battery/Thermal status via `adb shell dumpsys battery`

---

## 4. Troubleshooting & Diagnostic Error Codes

| Observed Symptom | Probable Cause | Corrective Action |
| :--- | :--- | :--- |
| `File does not exist` | Model file path misspelled or not pushed to device. | Run `adb push` to `/sdcard/Download/` and check exact file name. |
| `File size < 100 MB` | Incomplete download or LFS pointer file downloaded instead of binary weights. | Check file size: must be ~2.41 GiB (Git LFS clone required). |
| Native JNI Crash during Init | GPU driver incompatibility with Adreno 830 OpenCL kernels. | Uncheck "Adreno GPU Delegate" in the UI to fall back to CPU XNNPACK. |
| Process killed silently | Low Memory Killer (LMK) eviction (RSS > device threshold). | Close all background apps; verify device has at least 5 GB free physical RAM. |
