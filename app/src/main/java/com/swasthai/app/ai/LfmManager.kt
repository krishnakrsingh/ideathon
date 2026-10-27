package com.swasthai.app.ai

import android.content.Context
import android.util.Log
import com.swasthai.app.domain.model.ChatMessage
import com.swasthai.app.domain.model.MessageRole
import com.swasthai.app.domain.model.ModelInferenceStatus
import com.swasthai.app.domain.model.ResponseLanguage
import com.swasthai.app.domain.model.SourceCitation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class LfmBenchmarkMetrics(
    val promptTokens: Int = 0,
    val promptTimeMs: Double = 0.0,
    val promptTokensPerSec: Double = 0.0,
    val completionTokens: Int = 0,
    val generationTimeMs: Double = 0.0,
    val generationTokensPerSec: Double = 0.0,
    val cachedTokens: Int = 0,
    val totalTimeMs: Double = 0.0
)

class LfmManager(private val context: Context? = null) {
    val appContext: Context? get() = context

    var status: ModelInferenceStatus = ModelInferenceStatus.NOT_INSTALLED
        private set

    var embeddingStatus: ModelInferenceStatus = ModelInferenceStatus.NOT_INSTALLED
        private set

    var modelPath: String? = null
        private set

    var embeddingModelPath: String? = null
        private set

    var lastErrorMessage: String? = null
        private set

    var lastBenchmarkMetrics: LfmBenchmarkMetrics? = null
        private set

    private var chatServerProcess: Process? = null
    private var embeddingServerProcess: Process? = null

    companion object {
        private const val TAG = "LfmManager"
        const val PREFERRED_GENERATIVE_MODEL = "gemma-3-1b-it-Q4_K_M.gguf"
        const val PREFERRED_EMBEDDING_MODEL = "LFM2.5-Embedding-350M-Q4_0.gguf"
        const val CHAT_SERVER_PORT = 8080
        const val EMBEDDING_SERVER_PORT = 8081

        val CANDIDATE_GENERATIVE_MODELS = listOf(
            "gemma-3-1b-it-Q4_K_M.gguf",
            "gemma-3-1b-it-Q4_0.gguf",
            "gemma-4-E2B-it-Q4_0.gguf",
            "gemma-4-E2B-it-Q4_K_M.gguf",
            "LFM2.5-1.2B-Instruct-Q4_0.gguf"
        )

        val DEFAULT_GENERATIVE_PATHS = CANDIDATE_GENERATIVE_MODELS.flatMap { modelName ->
            listOf(
                "/sdcard/Download/$modelName",
                "/storage/emulated/0/Download/$modelName",
                "/sdcard/Download/SwasthAI/$modelName"
            )
        }

        val DEFAULT_EMBEDDING_PATHS = listOf(
            "/sdcard/Download/$PREFERRED_EMBEDDING_MODEL",
            "/storage/emulated/0/Download/$PREFERRED_EMBEDDING_MODEL",
            "/sdcard/Download/SwasthAI/$PREFERRED_EMBEDDING_MODEL",
            "/storage/emulated/0/Android/data/com.swasthai.app/files/$PREFERRED_EMBEDDING_MODEL"
        )
    }

    /**
     * Checks for available generative model files on internal and shared storage.
     * Prioritizes Gemma 3 1B / Gemma 4, then falls back to LFM2.5.
     */
    fun findAvailableGenerativeModel(): File? {
        val searchDirs = listOfNotNull(
            context?.getExternalFilesDir(null),
            context?.filesDir,
            File("/sdcard/Download"),
            File("/storage/emulated/0/Download"),
            File("/sdcard/Download/SwasthAI")
        )

        for (modelName in CANDIDATE_GENERATIVE_MODELS) {
            for (dir in searchDirs) {
                val f = File(dir, modelName)
                if (f.exists() && f.length() > 50L * 1024 * 1024) { // at least 50MB
                    return f
                }
            }
        }
        return null
    }

    fun findAvailableEmbeddingModel(): File? {
        val candidates = listOfNotNull(
            context?.getExternalFilesDir(null)?.let { File(it, PREFERRED_EMBEDDING_MODEL) },
            context?.filesDir?.let { File(it, PREFERRED_EMBEDDING_MODEL) }
        )
        for (f in candidates) {
            if (f.exists() && f.length() > 0L) return f
        }
        for (path in DEFAULT_EMBEDDING_PATHS) {
            val f = File(path)
            if (f.exists() && f.length() > 0L) return f
        }
        return null
    }

    /**
     * Locates the native llama-server binary.
     */
    private fun findNativeServerBinary(): File? {
        val libDir = context?.applicationInfo?.nativeLibraryDir
        if (libDir != null) {
            val libServer = File(libDir, "libllama_server.so")
            if (libServer.exists() && libServer.canExecute()) {
                return libServer
            }
        }
        // Fallback to local dev path
        val fallback = File("/data/local/tmp/llama/llama-b11933/llama-server")
        if (fallback.exists() && fallback.canExecute()) {
            return fallback
        }
        return null
    }

    /**
     * Initializes the native inference runtime with GPU acceleration.
     */
    suspend fun initialize(customPath: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val targetGenFile = if (customPath != null) File(customPath) else findAvailableGenerativeModel()

        if (targetGenFile == null || !targetGenFile.exists() || targetGenFile.length() == 0L) {
            status = ModelInferenceStatus.NOT_INSTALLED
            lastErrorMessage = "Gemma model artifact not found. Please place '$PREFERRED_GENERATIVE_MODEL' into your Downloads folder."
            Log.w(TAG, lastErrorMessage!!)
            return@withContext Result.failure(Exception(lastErrorMessage))
        }

        status = ModelInferenceStatus.INITIALIZING
        modelPath = targetGenFile.absolutePath

        val serverBinary = findNativeServerBinary()
        if (serverBinary == null) {
            status = ModelInferenceStatus.ERROR
            lastErrorMessage = "Native llama.cpp engine binary not found or not executable."
            Log.e(TAG, lastErrorMessage!!)
            return@withContext Result.failure(Exception(lastErrorMessage))
        }

        try {
            // Check if chat server is already running and healthy
            if (isServerHealthy(CHAT_SERVER_PORT)) {
                Log.i(TAG, "Chat server is already running and healthy on port $CHAT_SERVER_PORT")
                status = ModelInferenceStatus.READY
                lastErrorMessage = null
            } else {
                startChatServer(serverBinary, targetGenFile)
            }

            // Check embedding model
            val targetEmbFile = findAvailableEmbeddingModel()
            if (targetEmbFile != null && targetEmbFile.exists() && targetEmbFile.length() > 0L) {
                embeddingModelPath = targetEmbFile.absolutePath
                if (!isServerHealthy(EMBEDDING_SERVER_PORT)) {
                    startEmbeddingServer(serverBinary, targetEmbFile)
                } else {
                    embeddingStatus = ModelInferenceStatus.READY
                }
            }

            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize runtime", e)
            status = ModelInferenceStatus.ERROR
            lastErrorMessage = "Initialization failed: ${e.message}"
            Result.failure(e)
        }
    }

    private suspend fun startChatServer(serverBinary: File, modelFile: File) {
        val libDir = context?.applicationInfo?.nativeLibraryDir ?: "/system/lib64"
        Log.i(TAG, "Launching chat server with GPU acceleration (-ngl 99): ${serverBinary.absolutePath} in libDir: $libDir")

        // GPU acceleration: -ngl 99 offloads all layers to Adreno GPU / Vulkan if available
        // CPU threads: -t 6 for Snapdragon 8 Elite high performance
        // Context: -c 4096 -cb (cache reuse) -np 1 --slots (slot management)
        val command = "export LD_LIBRARY_PATH=\"$libDir\"; exec \"${serverBinary.absolutePath}\" -m \"${modelFile.absolutePath}\" --port $CHAT_SERVER_PORT -t 6 -ngl 99 --ctx-size 4096 -cb -np 1 --slots"
        val pb = ProcessBuilder("sh", "-c", command)
        pb.redirectErrorStream(true)

        val process = pb.start()
        chatServerProcess = process

        // Log output asynchronously
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        Log.d("LfmServer-Chat", line)
                    }
                }
            } catch (_: Throwable) {}
        }

        // Wait up to 15 seconds for health check to pass
        var healthy = false
        for (i in 0 until 30) {
            delay(500)
            if (isServerHealthy(CHAT_SERVER_PORT)) {
                healthy = true
                break
            }
        }

        if (healthy) {
            status = ModelInferenceStatus.READY
            lastErrorMessage = null
            Log.i(TAG, "Chat server successfully initialized on port $CHAT_SERVER_PORT!")
        } else {
            status = ModelInferenceStatus.ERROR
            lastErrorMessage = "Chat server started but health check timed out."
            Log.e(TAG, lastErrorMessage!!)
        }
    }

    private suspend fun startEmbeddingServer(serverBinary: File, modelFile: File) {
        val libDir = context?.applicationInfo?.nativeLibraryDir ?: "/system/lib64"
        Log.i(TAG, "Launching embedding server with: ${serverBinary.absolutePath}")

        val command = "export LD_LIBRARY_PATH=\"$libDir\"; exec \"${serverBinary.absolutePath}\" -m \"${modelFile.absolutePath}\" --port $EMBEDDING_SERVER_PORT --embedding -t 4"
        val pb = ProcessBuilder("sh", "-c", command)
        pb.redirectErrorStream(true)

        val process = pb.start()
        embeddingServerProcess = process

        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        Log.d("LfmServer-Emb", line)
                    }
                }
            } catch (_: Throwable) {}
        }

        var healthy = false
        for (i in 0 until 20) {
            delay(500)
            if (isServerHealthy(EMBEDDING_SERVER_PORT)) {
                healthy = true
                break
            }
        }

        if (healthy) {
            embeddingStatus = ModelInferenceStatus.READY
            Log.i(TAG, "Embedding server successfully initialized on port $EMBEDDING_SERVER_PORT!")
        } else {
            embeddingStatus = ModelInferenceStatus.ERROR
        }
    }

    private fun isServerHealthy(port: Int): Boolean {
        return try {
            val url = URL("http://127.0.0.1:$port/health")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 1000
            conn.readTimeout = 1000
            val code = conn.responseCode
            conn.disconnect()
            code == 200
        } catch (_: Exception) {
            false
        }
    }

    suspend fun initializeEmbedding(customPath: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val targetEmbFile = if (customPath != null) File(customPath) else findAvailableEmbeddingModel()
        if (targetEmbFile == null || !targetEmbFile.exists()) {
            embeddingStatus = ModelInferenceStatus.ERROR
            return@withContext Result.failure(Exception("Embedding model not found at $customPath"))
        }
        val serverBinary = findNativeServerBinary()
        if (serverBinary == null) {
            embeddingStatus = ModelInferenceStatus.ERROR
            return@withContext Result.failure(Exception("llama-server binary not found"))
        }
        startEmbeddingServer(serverBinary, targetEmbFile)
        if (embeddingStatus == ModelInferenceStatus.READY) Result.success(Unit) else Result.failure(Exception("Embedding initialization failed"))
    }

    suspend fun generateEmbedding(text: String): FloatArray? = withContext(Dispatchers.IO) {
        if (!isServerHealthy(EMBEDDING_SERVER_PORT)) return@withContext null
        try {
            val url = URL("http://127.0.0.1:$EMBEDDING_SERVER_PORT/v1/embeddings")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 3000
            conn.readTimeout = 10000

            val body = JSONObject()
            body.put("input", text)
            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(response)
            val data = json.optJSONArray("data")
            if (data != null && data.length() > 0) {
                val emb = data.getJSONObject(0).optJSONArray("embedding")
                if (emb != null) {
                    val floatArr = FloatArray(emb.length())
                    for (i in 0 until emb.length()) {
                        floatArr[i] = emb.getDouble(i).toFloat()
                    }
                    return@withContext floatArr
                }
            }
        } catch (_: Exception) {}
        null
    }

    /**
     * Builds the system prompt with strict non-thinking, factual grounding, and language selection.
     */
    fun buildSystemPrompt(
        language: ResponseLanguage = ResponseLanguage.AUTO,
        currentQuery: String = "",
        citations: List<SourceCitation> = emptyList(),
        allHistoryMeasurements: List<String> = emptyList()
    ): String = buildString {
        append("You are Dr. Swasth, an AI clinical health companion running 100% locally on-device. ")
        append("Provide concise, clear, and empathetic medical explanations. ")
        append("Do NOT use thinking tags, internal reasoning, or filler pleasantries. ")
        append("Never diagnose or fabricate facts. Always preserve exact numbers and units.\n\n")

        val effectiveLang = if (language == ResponseLanguage.AUTO) {
            val isDevanagari = currentQuery.any { it in '\u0900'..'\u097F' }
            if (isDevanagari) ResponseLanguage.HINDI else ResponseLanguage.ENGLISH
        } else {
            language
        }

        when (effectiveLang) {
            ResponseLanguage.HINDI -> {
                append("LANGUAGE DIRECTIVE: Respond in natural, fluent Hindi using Devanagari script (हिन्दी). ")
                append("Provide clinical context clearly and include English medical names in parentheses where helpful (e.g. हीमोग्लोबिन (Haemoglobin), रक्तचाप (Blood Pressure)).\n\n")
            }
            ResponseLanguage.ENGLISH -> {
                append("LANGUAGE DIRECTIVE: Respond in clear, concise English.\n\n")
            }
            else -> {}
        }

        if (citations.isNotEmpty()) {
            append("--- RETRIEVED MEDICAL RECORD EVIDENCE ---\n")
            citations.forEachIndexed { idx, c ->
                append("[Evidence ${idx + 1}] Source: ${c.documentName}, Page ${c.pageNumber}:\n")
                append("${c.chunkText.trim()}\n\n")
            }
            append("Base your answer strictly on the patient's records above. Always cite the document name when mentioning values.\n\n")
        }

        if (allHistoryMeasurements.isNotEmpty()) {
            append("--- PATIENT BIOMARKERS PROFILE ---\n")
            allHistoryMeasurements.take(12).forEach { append("- $it\n") }
            append("\n")
        }
    }

    /**
     * Streams generative response tokens using Google Gemma (or LFM2.5).
     * Fully preserves KV-cache across chat turns and never outputs <think> tags.
     */
    fun generateStream(
        currentQuery: String,
        history: List<ChatMessage>,
        citations: List<SourceCitation>,
        allHistoryMeasurements: List<String> = emptyList(),
        responseLanguage: ResponseLanguage = ResponseLanguage.AUTO
    ): Flow<String> = flow {
        if (!isServerHealthy(CHAT_SERVER_PORT)) {
            val serverBinary = findNativeServerBinary()
            val modelFile = findAvailableGenerativeModel()
            if (serverBinary != null && modelFile != null) {
                startChatServer(serverBinary, modelFile)
            }
            if (!isServerHealthy(CHAT_SERVER_PORT)) {
                throw IllegalStateException(lastErrorMessage ?: "Inference native server is not available.")
            }
        }

        status = ModelInferenceStatus.GENERATING

        val messagesArray = JSONArray()

        // 1. System Prompt (No thinking, language instructed)
        val systemPrompt = buildSystemPrompt(responseLanguage, currentQuery, citations, allHistoryMeasurements)
        messagesArray.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })

        // 2. Preceding multi-turn conversation history (excluding the current query)
        val priorTurns = history.filter {
            it.content.isNotBlank() && !(it.role == MessageRole.USER && it.content == currentQuery)
        }.takeLast(12)

        for (msg in priorTurns) {
            val role = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.SYSTEM -> "system"
            }
            messagesArray.put(JSONObject().apply {
                put("role", role)
                put("content", msg.content)
            })
        }

        // 3. Current user query
        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", currentQuery)
        })

        val requestBody = JSONObject().apply {
            put("messages", messagesArray)
            put("temperature", 0.2)
            put("max_tokens", 512)
            put("stream", true)
        }

        val url = URL("http://127.0.0.1:$CHAT_SERVER_PORT/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 5000
        conn.readTimeout = 30000

        try {
            OutputStreamWriter(conn.outputStream).use { it.write(requestBody.toString()) }

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: break
                if (currentLine.startsWith("data: ")) {
                    val dataContent = currentLine.removePrefix("data: ").trim()
                    if (dataContent == "[DONE]") break

                    try {
                        val chunkObj = JSONObject(dataContent)
                        val choices = chunkObj.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val delta = choices.getJSONObject(0).optJSONObject("delta")
                            if (delta != null && !delta.isNull("content") && delta.has("content")) {
                                var content = delta.optString("content", "")
                                if (!content.isNullOrEmpty() && content != "null" && content != "None") {
                                    // Strip accidental "null" prefix if leaked
                                    if (content.startsWith("null")) {
                                        content = content.removePrefix("null")
                                    }
                                    // Strip thinking tags and control tokens
                                    val cleanContent = content
                                        .replace("<think>", "")
                                        .replace("</think>", "")
                                        .replace("<|think|>", "")
                                        .replace("</|think|>", "")
                                        .replace("<|channel>", "")
                                        .replace("<|im_start|>", "")
                                        .replace("<|im_end|>", "")
                                    if (cleanContent.isNotEmpty()) {
                                        emit(cleanContent)
                                    }
                                }
                            }
                        }

                        // Check timings telemetry
                        if (chunkObj.has("timings")) {
                            val timings = chunkObj.getJSONObject("timings")
                            lastBenchmarkMetrics = LfmBenchmarkMetrics(
                                promptTokens = timings.optInt("prompt_n"),
                                promptTimeMs = timings.optDouble("prompt_ms"),
                                promptTokensPerSec = timings.optDouble("prompt_per_second"),
                                completionTokens = timings.optInt("predicted_n"),
                                generationTimeMs = timings.optDouble("predicted_ms"),
                                generationTokensPerSec = timings.optDouble("predicted_per_second"),
                                cachedTokens = timings.optInt("cache_n")
                            )
                        }
                    } catch (_: Exception) {}
                }
            }
            reader.close()
        } finally {
            conn.disconnect()
            status = ModelInferenceStatus.READY
        }
    }.flowOn(Dispatchers.IO)

    fun release() {
        try {
            chatServerProcess?.destroy()
            embeddingServerProcess?.destroy()
        } catch (_: Throwable) {}
        chatServerProcess = null
        embeddingServerProcess = null
        status = ModelInferenceStatus.NOT_INSTALLED
        embeddingStatus = ModelInferenceStatus.NOT_INSTALLED
    }
}
