package com.swasthai.testharness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

/**
 * Isolated, minimal Android test harness for Phase 0 MedGemma 1.5 4B IT evaluation.
 *
 * Capabilities:
 * - Load model from user-selected path or app-private storage.
 * - Displays loading, memory, and inference state.
 * - Streams real generated tokens without mocking.
 * - Handles missing files, OOM, and native execution failures.
 * - Operates 100% offline (zero network sockets).
 */
class ModelTestHarnessActivity : ComponentActivity() {

    private lateinit var tester: LiteRtModelTester

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tester = LiteRtModelTester(applicationContext)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF81D4FA),
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E),
                    error = Color(0xFFCF6679)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HarnessScreen(tester)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tester.closeCurrentSession()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HarnessScreen(tester: LiteRtModelTester) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var modelPathInput by remember {
        mutableStateOf("/sdcard/Download/medgemma-1.5-4b-it_q4_block32_ekv2048.litertlm")
    }
    var useGpu by remember { mutableStateOf(true) }

    var statusMessage by remember { mutableStateOf("Ready to load model.") }
    var isModelLoaded by remember { mutableStateOf(false) }
    var isLoadingModel by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }

    var selectedPromptIndex by remember { mutableIntStateOf(0) }
    var customPromptText by remember { mutableStateOf("") }
    var generatedOutput by remember { mutableStateOf("") }
    var lastMetrics by remember { mutableStateOf<BenchmarkMetrics?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var activeJob by remember { mutableStateOf<Job?>(null) }

    val promptOptions = listOf(
        "1. General Health (HDL)",
        "2. Medical Term (Anemia)",
        "3. Grounded Lab Panel",
        "4. Multi-Turn Self-Contained",
        "5. Custom Input"
    )

    fun getSelectedPrompt(): String {
        return when (selectedPromptIndex) {
            0 -> BenchmarkPrompts.PROMPT_1_GENERAL_HEALTH
            1 -> BenchmarkPrompts.PROMPT_2_MEDICAL_TERMINOLOGY
            2 -> BenchmarkPrompts.PROMPT_3_REPORT_GROUNDED
            3 -> BenchmarkPrompts.buildSelfContainedMultiTurnPrompt(
                conversationSummary = BenchmarkPrompts.SAMPLE_SCENARIO_SUMMARY,
                relevantEvidence = "Synthetic Lab Report (Jan 15, 2025): Triglycerides: 195 mg/dL [High]",
                currentUserQuestion = BenchmarkPrompts.SAMPLE_SCENARIO_QUESTION_TURN_2
            )
            else -> customPromptText.ifBlank { BenchmarkPrompts.PROMPT_1_GENERAL_HEALTH }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Text(
            text = "SwasthAI Phase 0: Model Feasibility Harness",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Target: Samsung Galaxy S25 | Model: MedGemma 1.5 4B IT (INT4 LiteRT-LM)",
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray
        )

        HorizontalDivider(color = Color.DarkGray)

        // Section 1: Model File Configuration
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "1. Model Artifact Location",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = modelPathInput,
                    onValueChange = { modelPathInput = it },
                    label = { Text("Model File Absolute Path (.litertlm)") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    enabled = !isLoadingModel && !isGenerating
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = useGpu,
                            onCheckedChange = { useGpu = it },
                            enabled = !isLoadingModel && !isGenerating
                        )
                        Text(text = if (useGpu) "Adreno GPU Delegate" else "CPU XNNPACK", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            errorMessage = null
                            isLoadingModel = true
                            statusMessage = "Loading model from disk..."
                            coroutineScope.launch {
                                val result = tester.initializeEngine(modelPathInput, useGpu)
                                isLoadingModel = false
                                result.onSuccess { loadDurationMs ->
                                    isModelLoaded = true
                                    statusMessage = "Engine initialized in $loadDurationMs ms on ${if (useGpu) "Adreno GPU" else "CPU"}."
                                }.onFailure { error ->
                                    isModelLoaded = false
                                    errorMessage = error.localizedMessage ?: error.javaClass.simpleName
                                    statusMessage = "Model initialization failed."
                                }
                            }
                        },
                        enabled = !isLoadingModel && !isGenerating
                    ) {
                        Text(text = if (isLoadingModel) "Loading..." else if (isModelLoaded) "Reload Model" else "Initialize Model")
                    }
                }

                // File Existence Helper Notice
                val checkFile = File(modelPathInput)
                Text(
                    text = if (checkFile.exists()) {
                        "File Found: ${(checkFile.length() / (1024 * 1024))} MB"
                    } else {
                        "File not found at this path. Use ADB to push the model file."
                    },
                    fontSize = 11.sp,
                    color = if (checkFile.exists()) Color(0xFF81C784) else Color(0xFFFFB74D)
                )
            }
        }

        // Error Banner
        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Execution Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(text = errorMessage.orEmpty(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        // Section 2: Prompt Selection & Benchmark Execution
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "2. Benchmark Prompt Selection",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Prompt selection chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    promptOptions.forEachIndexed { index, title ->
                        FilterChip(
                            selected = selectedPromptIndex == index,
                            onClick = { selectedPromptIndex = index },
                            label = { Text(title, fontSize = 12.sp) }
                        )
                    }
                }

                if (selectedPromptIndex == 4) {
                    OutlinedTextField(
                        value = customPromptText,
                        onValueChange = { customPromptText = it },
                        label = { Text("Enter custom text-only prompt") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (!isModelLoaded) {
                                errorMessage = "Please initialize the model before running inference."
                                return@Button
                            }
                            errorMessage = null
                            isGenerating = true
                            generatedOutput = ""
                            lastMetrics = null
                            statusMessage = "Generating response..."

                            activeJob = coroutineScope.launch {
                                try {
                                    val prompt = getSelectedPrompt()
                                    tester.runBenchmarkPrompt(prompt).collect { (partialOutput, metrics) ->
                                        generatedOutput = partialOutput
                                        if (metrics != null) {
                                            lastMetrics = metrics
                                            statusMessage = "Generation complete (${metrics.tokenCount} tokens)."
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.localizedMessage ?: "Inference error."
                                    statusMessage = "Inference halted."
                                } finally {
                                    isGenerating = false
                                }
                            }
                        },
                        enabled = isModelLoaded && !isGenerating
                    ) {
                        Text(if (isGenerating) "Generating..." else "Run Prompt")
                    }

                    if (isGenerating) {
                        OutlinedButton(
                            onClick = {
                                activeJob?.cancel()
                                isGenerating = false
                                statusMessage = "Generation cancelled by user."
                            }
                        ) {
                            Text("Stop")
                        }
                    }
                }
            }
        }

        // Section 3: Generated Output Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3. Real Generated Output",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = statusMessage,
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 300.dp)
                        .background(Color(0xFF0A0A0A), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = generatedOutput.ifEmpty { "(Generated tokens will stream here in real time...)" },
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = if (generatedOutput.isEmpty()) Color.DarkGray else Color.White
                    )
                }
            }
        }

        // Section 4: Benchmark Metrics Display
        if (lastMetrics != null) {
            val m = lastMetrics!!
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2B34)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "4. Objective Benchmark Measurements",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF81D4FA)
                    )

                    MetricRow("Cold-Start Load Time:", "${m.coldStartLoadMs} ms")
                    MetricRow("Warm-Start Init Time:", "${m.warmStartInitMs} ms")
                    MetricRow("Time-to-First-Token (TTFT):", "${m.timeToFirstTokenMs} ms")
                    MetricRow("Total Generation Time:", "${m.totalGenerationMs} ms")
                    MetricRow("Generated Token Count:", "${m.tokenCount} tokens")
                    MetricRow("Throughput Speed:", "%.2f tokens / second".format(m.tokensPerSecond))
                    MetricRow("Peak Process Memory (PSS):", "${m.peakProcessMemoryMb} MB")
                    MetricRow("Hardware Delegate Active:", m.backendUsed)
                }
            }
        }

        // Section 5: Embedding Model Verification Probe
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "5. EmbeddingGemma 2 Text 270M Contract",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Search Query Prefix: \"${EmbeddingGemmaProbe.PREFIX_QUERY}\"",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
                Text(
                    text = "Document Prefix: \"${EmbeddingGemmaProbe.PREFIX_DOCUMENT}\"",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
                Text(
                    text = "Dimensions: 768 float32 (Matryoshka support: 512, 256, 128)",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.LightGray)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
