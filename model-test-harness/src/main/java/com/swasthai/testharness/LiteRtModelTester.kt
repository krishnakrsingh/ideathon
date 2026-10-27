package com.swasthai.testharness

import android.content.Context
import android.os.Debug
import android.os.SystemClock
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Benchmark result data model holding real, measured performance values.
 */
data class BenchmarkMetrics(
    val coldStartLoadMs: Long = 0L,
    val warmStartInitMs: Long = 0L,
    val timeToFirstTokenMs: Long = 0L,
    val totalGenerationMs: Long = 0L,
    val tokenCount: Int = 0,
    val tokensPerSecond: Double = 0.0,
    val peakProcessMemoryMb: Long = 0L,
    val modelFilePath: String = "",
    val backendUsed: String = "GPU"
)

sealed interface ModelTesterState {
    data object Uninitialized : ModelTesterState
    data class Loading(val message: String) : ModelTesterState
    data class Ready(val modelPath: String, val loadTimeMs: Long, val backend: String) : ModelTesterState
    data class Generating(val currentOutput: String, val tokensEmitted: Int) : ModelTesterState
    data class Completed(val fullOutput: String, val metrics: BenchmarkMetrics) : ModelTesterState
    data class Error(val errorType: String, val details: String, val technicalHint: String) : ModelTesterState
}

/**
 * Isolated LiteRT-LM model test harness wrapper.
 * Strictly decoupled from application UI, RAG, or Vault components.
 */
class LiteRtModelTester(private val context: Context) {

    private var activeEngine: Engine? = null
    private var activeConversation: Conversation? = null
    private var loadedModelPath: String? = null
    private var lastColdStartMs: Long = 0L
    private var lastBackend: String = "GPU"

    /**
     * Attempts to initialize the LiteRT-LM engine with the specified model file path.
     * Validates file existence, permissions, and catches native initialization faults.
     */
    suspend fun initializeEngine(
        modelFilePath: String,
        useGpu: Boolean = true
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val file = File(modelFilePath)
            if (!file.exists()) {
                return@withContext Result.failure(
                    IllegalArgumentException(
                        "Model file does not exist at path: $modelFilePath. " +
                        "Ensure the file has been pushed to the device via ADB or copied to app-private storage."
                    )
                )
            }

            if (!file.canRead()) {
                return@withContext Result.failure(
                    SecurityException("Model file exists but cannot be read due to file permissions: $modelFilePath")
                )
            }

            if (file.length() < 100 * 1024 * 1024) { // < 100 MB is suspicious for a 4B model
                return@withContext Result.failure(
                    IllegalStateException(
                        "Model file size (${file.length() / (1024 * 1024)} MB) is smaller than expected. " +
                        "Ensure the full ~2.41 GiB MedGemma artifact was transferred completely."
                    )
                )
            }

            // Close existing engine if previously initialized
            closeCurrentSession()

            val backend = if (useGpu) Backend.GPU() else Backend.CPU()
            lastBackend = if (useGpu) "Adreno GPU" else "CPU XNNPACK"

            val startTime = SystemClock.elapsedRealtime()

            val config = EngineConfig(
                modelPath = file.absolutePath,
                backend = backend
            )

            val engine = Engine(config)
            engine.initialize()

            val loadDuration = SystemClock.elapsedRealtime() - startTime
            lastColdStartMs = loadDuration
            activeEngine = engine
            loadedModelPath = file.absolutePath

            Result.success(loadDuration)
        } catch (t: Throwable) {
            closeCurrentSession()
            Result.failure(t)
        }
    }

    /**
     * Executes real streaming inference using LiteRT-LM and measures performance metrics.
     * Yields progressive text chunks and emits the final BenchmarkMetrics.
     */
    fun runBenchmarkPrompt(
        prompt: String
    ): Flow<Pair<String, BenchmarkMetrics?>> = flow {
        val engine = activeEngine ?: throw IllegalStateException("Engine must be initialized before running inference.")
        
        val warmStartBegin = SystemClock.elapsedRealtime()
        val conversationConfig = ConversationConfig()
        val conversation = engine.createConversation(conversationConfig)
        activeConversation = conversation
        val warmStartDuration = SystemClock.elapsedRealtime() - warmStartBegin

        var firstTokenTime: Long? = null
        val promptDispatchTime = SystemClock.elapsedRealtime()
        val fullResponseBuilder = StringBuilder()
        var tokenCount = 0

        try {
            conversation.sendMessageAsync(prompt).collect { tokenChunk ->
                if (firstTokenTime == null) {
                    firstTokenTime = SystemClock.elapsedRealtime()
                }
                tokenCount++
                fullResponseBuilder.append(tokenChunk)
                emit(Pair(fullResponseBuilder.toString(), null))
            }

            val finishTime = SystemClock.elapsedRealtime()
            val ttftMs = (firstTokenTime ?: finishTime) - promptDispatchTime
            val totalGenMs = finishTime - promptDispatchTime
            val generationTimeOnlyMs = if (firstTokenTime != null) finishTime - firstTokenTime!! else totalGenMs

            val tokensPerSec = if (generationTimeOnlyMs > 0) {
                (tokenCount.toDouble() / generationTimeOnlyMs.toDouble()) * 1000.0
            } else {
                0.0
            }

            val peakMemoryMb = getProcessMemoryMb()

            val metrics = BenchmarkMetrics(
                coldStartLoadMs = lastColdStartMs,
                warmStartInitMs = warmStartDuration,
                timeToFirstTokenMs = ttftMs,
                totalGenerationMs = totalGenMs,
                tokenCount = tokenCount,
                tokensPerSecond = tokensPerSec,
                peakProcessMemoryMb = peakMemoryMb,
                modelFilePath = loadedModelPath.orEmpty(),
                backendUsed = lastBackend
            )

            emit(Pair(fullResponseBuilder.toString(), metrics))

        } catch (e: CancellationException) {
            throw e
        } finally {
            try {
                conversation.close()
            } catch (_: Exception) {}
            activeConversation = null
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Measures total Resident Set Size (RSS) / PSS memory of the current app process in MB.
     */
    fun getProcessMemoryMb(): Long {
        return try {
            val pssKb = Debug.getPss()
            if (pssKb > 0) {
                pssKb / 1024
            } else {
                val runtime = Runtime.getRuntime()
                (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            }
        } catch (_: Exception) {
            0L
        }
    }

    fun isEngineReady(): Boolean = activeEngine != null

    fun getLoadedPath(): String? = loadedModelPath

    fun closeCurrentSession() {
        try {
            activeConversation?.close()
        } catch (_: Exception) {}
        activeConversation = null

        try {
            activeEngine?.close()
        } catch (_: Exception) {}
        activeEngine = null
        loadedModelPath = null
    }
}
