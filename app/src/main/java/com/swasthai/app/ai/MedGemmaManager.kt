package com.swasthai.app.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.swasthai.app.domain.model.ChatMessage
import com.swasthai.app.domain.model.MessageRole
import com.swasthai.app.domain.model.ModelInferenceStatus
import com.swasthai.app.domain.model.SourceCitation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class ModelNotInstalledException(message: String) : Exception(message)

class MedGemmaManager(private val context: Context? = null) {

    var status: ModelInferenceStatus = ModelInferenceStatus.NOT_INSTALLED
        private set

    var modelPath: String? = null
        private set

    var lastErrorMessage: String? = null
        private set

    private var activeEngine: Engine? = null

    companion object {
        const val PREFERRED_MODEL_NAME = "medgemma-1.5-4b-it_q4_block32_ekv2048.litertlm"

        val DEFAULT_LOOKUP_PATHS = listOf(
            "/sdcard/Android/data/com.swasthai.app/files/$PREFERRED_MODEL_NAME",
            "/storage/emulated/0/Android/data/com.swasthai.app/files/$PREFERRED_MODEL_NAME",
            "/sdcard/Download/$PREFERRED_MODEL_NAME",
            "/sdcard/Download/SwasthAI/$PREFERRED_MODEL_NAME",
            "/storage/emulated/0/Download/$PREFERRED_MODEL_NAME"
        )
    }

    /**
     * Checks known model paths on storage and app internal files.
     */
    fun findAvailableModelFile(): File? {
        val candidates = listOfNotNull(
            context?.getExternalFilesDir(null)?.let { File(it, PREFERRED_MODEL_NAME) },
            context?.filesDir?.let { File(it, PREFERRED_MODEL_NAME) }
        )
        for (f in candidates) {
            if (f.exists() && f.length() > 0L) {
                return f
            }
        }

        for (path in DEFAULT_LOOKUP_PATHS) {
            val file = File(path)
            if (file.exists() && file.length() > 0L) {
                return file
            }
        }
        return null
    }

    /**
     * Initializes the LiteRT-LM runtime with the model file.
     */
    suspend fun initialize(customPath: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val targetFile = if (customPath != null) {
            File(customPath)
        } else {
            findAvailableModelFile()
        }

        if (targetFile == null || !targetFile.exists() || targetFile.length() == 0L) {
            status = ModelInferenceStatus.NOT_INSTALLED
            lastErrorMessage = "MedGemma 1.5 4B IT artifact not found. Please place '$PREFERRED_MODEL_NAME' into your device Downloads folder."
            return@withContext Result.failure(ModelNotInstalledException(lastErrorMessage!!))
        }

        status = ModelInferenceStatus.INITIALIZING
        modelPath = targetFile.absolutePath

        try {
            release()

            // Try GPU first; if initialization fails (e.g. unsupported ops on GPU delegate), fallback to CPU
            val engine = try {
                android.util.Log.i("MedGemmaManager", "Attempting initialization with Backend.GPU()...")
                val gpuConfig = EngineConfig(
                    modelPath = targetFile.absolutePath,
                    backend = Backend.GPU()
                )
                val eng = Engine(gpuConfig)
                eng.initialize()
                android.util.Log.i("MedGemmaManager", "MedGemma initialized successfully with GPU backend!")
                eng
            } catch (gpuError: Throwable) {
                android.util.Log.w("MedGemmaManager", "GPU initialization failed (${gpuError.message}), attempting fallback to Backend.CPU()...")
                val cpuConfig = EngineConfig(
                    modelPath = targetFile.absolutePath,
                    backend = Backend.CPU()
                )
                val eng = Engine(cpuConfig)
                eng.initialize()
                android.util.Log.i("MedGemmaManager", "MedGemma initialized successfully with CPU backend!")
                eng
            }

            activeEngine = engine
            status = ModelInferenceStatus.READY
            lastErrorMessage = null
            Result.success(Unit)
        } catch (e: Throwable) {
            android.util.Log.e("MedGemmaManager", "LiteRT-LM initialization failed on both backends", e)
            status = ModelInferenceStatus.ERROR
            lastErrorMessage = "LiteRT-LM initialization failed: ${e.localizedMessage ?: e.message}"
            Result.failure(e)
        }
    }

    /**
     * Constructs a self-contained, RAG-grounded prompt.
     * Integrates conversation history, retrieved evidence chunks, and clear instructions.
     */
    fun buildSelfContainedPrompt(
        currentQuery: String,
        history: List<ChatMessage>,
        retrievedCitations: List<SourceCitation>
    ): String {
        val sb = StringBuilder()

        sb.append("You are SwasthAI, an on-device personal healthcare companion. ")
        sb.append("Provide clear, empathetic, source-grounded educational explanations. ")
        sb.append("Do not diagnose medical conditions or fabricate medical facts. ")
        sb.append("Always preserve numeric values and units exactly as stated in the evidence.\n\n")

        if (retrievedCitations.isNotEmpty()) {
            sb.append("--- RETRIEVED MEDICAL RECORDS & EVIDENCE ---\n")
            for ((idx, citation) in retrievedCitations.withIndex()) {
                sb.append("[Evidence ${idx + 1}] Source: ${citation.documentName}, Page: ${citation.pageNumber}\n")
                sb.append("${citation.chunkText.trim()}\n\n")
            }
            sb.append("Use the evidence above to answer the user's question. Cite the exact source name and page number when referring to evidence.\n\n")
        }

        val recentHistory = history.takeLast(6)
        if (recentHistory.isNotEmpty()) {
            sb.append("--- RECENT CONVERSATION CONTEXT ---\n")
            for (msg in recentHistory) {
                val roleName = when (msg.role) {
                    MessageRole.USER -> "User"
                    MessageRole.ASSISTANT -> "SwasthAI"
                    MessageRole.SYSTEM -> "System"
                }
                sb.append("$roleName: ${msg.content}\n")
            }
            sb.append("\n")
        }

        sb.append("User Question: $currentQuery\n")
        sb.append("SwasthAI Response: ")

        return sb.toString()
    }

    /**
     * Streams generative response tokens from MedGemma.
     * Never fabricates fake AI responses. If model is not loaded, yields an error.
     */
    fun generateStream(
        prompt: String
    ): Flow<String> = flow {
        val engine = activeEngine ?: throw ModelNotInstalledException(
            lastErrorMessage ?: "MedGemma 1.5 4B IT is not loaded. Cannot run inference."
        )

        status = ModelInferenceStatus.GENERATING
        val conversationConfig = ConversationConfig()
        val conversation = engine.createConversation(conversationConfig)

        try {
            conversation.sendMessageAsync(prompt).collect { tokenChunk ->
                emit(tokenChunk.toString())
            }
        } finally {
            try {
                conversation.close()
            } catch (_: Throwable) {}
            if (status == ModelInferenceStatus.GENERATING) {
                status = ModelInferenceStatus.READY
            }
        }
    }.flowOn(Dispatchers.IO)

    fun release() {
        try {
            activeEngine?.close()
        } catch (_: Throwable) {}
        activeEngine = null
        status = ModelInferenceStatus.NOT_INSTALLED
    }
}
