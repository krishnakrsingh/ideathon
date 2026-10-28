package com.swasthai.app.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.EmbeddingEngineConfig
import com.google.ai.edge.litertlm.EmbeddingOptions
import com.google.ai.edge.litertlm.InputData
import com.swasthai.app.domain.model.ModelInferenceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

data class ScoredChunk(
    val chunkId: Long,
    val documentId: Long,
    val score: Float
)

/** Local EmbeddingGemma 2 runtime. A missing or invalid model never produces synthetic vectors. */
class EmbeddingEngine @JvmOverloads constructor(private val context: Context? = null) : AutoCloseable {

    @Volatile
    var status: ModelInferenceStatus = ModelInferenceStatus.NOT_INSTALLED
        private set

    @Volatile
    var modelFilePath: String? = null
        private set

    @Volatile
    var lastError: String? = null
        private set

    private val engineMutex = Mutex()
    private var runtime: com.google.ai.edge.litertlm.EmbeddingEngine? = null

    companion object {
        const val EMBEDDING_DIMENSION = 768
        const val MODEL_FILE_NAME = "embeddinggemma-2-text-270m.litertlm"
        const val QUERY_PREFIX = "task: search result | query: "
        const val DOCUMENT_PREFIX = "title: none | text: "

        fun floatsToByteArray(floats: FloatArray): ByteArray {
            val buffer = ByteBuffer.allocate(floats.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
            floats.forEach(buffer::putFloat)
            return buffer.array()
        }

        fun byteArrayToFloats(bytes: ByteArray): FloatArray {
            if (bytes.size % Float.SIZE_BYTES != 0) return FloatArray(0)
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            return FloatArray(bytes.size / Float.SIZE_BYTES) { buffer.float }
        }

        fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
            if (v1.size != v2.size || v1.isEmpty()) return 0.0f
            var dot = 0.0
            var norm1 = 0.0
            var norm2 = 0.0
            for (i in v1.indices) {
                val a = v1[i].toDouble()
                val b = v2[i].toDouble()
                dot += a * b
                norm1 += a * a
                norm2 += b * b
            }
            val denominator = sqrt(norm1) * sqrt(norm2)
            return if (denominator <= 1e-9) 0.0f else (dot / denominator).toFloat()
        }
    }

    fun findAvailableModelFile(): File? {
        val candidates = listOfNotNull(
            context?.getExternalFilesDir(null)?.let { File(it, MODEL_FILE_NAME) },
            context?.filesDir?.let { File(it, MODEL_FILE_NAME) },
            File("/sdcard/Download/$MODEL_FILE_NAME"),
            File("/storage/emulated/0/Download/$MODEL_FILE_NAME"),
            File("/sdcard/Download/SwasthAI/$MODEL_FILE_NAME"),
            File("/storage/emulated/0/Download/SwasthAI/$MODEL_FILE_NAME")
        )
        return candidates.firstOrNull { it.isFile && it.length() > 0L }
    }

    suspend fun initialize(modelPath: String): Boolean = engineMutex.withLock {
        val file = File(modelPath)
        if (!file.isFile || file.length() == 0L) {
            status = ModelInferenceStatus.NOT_INSTALLED
            lastError = "EmbeddingGemma model file not found at: $modelPath"
            return@withLock false
        }

        status = ModelInferenceStatus.INITIALIZING
        lastError = null
        withContext(Dispatchers.IO) {
            try {
                runtime?.close()
                val candidate = com.google.ai.edge.litertlm.EmbeddingEngine(
                    EmbeddingEngineConfig(
                        modelPath = file.absolutePath,
                        backend = Backend.CPU()
                    )
                )
                candidate.initialize()
                runtime = candidate
                modelFilePath = file.absolutePath
                status = ModelInferenceStatus.READY
                true
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                runtime = null
                status = ModelInferenceStatus.ERROR
                lastError = "Failed to initialize EmbeddingGemma: ${error.localizedMessage ?: error.javaClass.simpleName}"
                false
            }
        }
    }

    suspend fun generateEmbedding(text: String, isQuery: Boolean): FloatArray? = engineMutex.withLock {
        if (status != ModelInferenceStatus.READY || text.isBlank()) return@withLock null
        val model = runtime ?: return@withLock null
        withContext(Dispatchers.IO) {
            try {
                val prefix = if (isQuery) QUERY_PREFIX else DOCUMENT_PREFIX
                model.computeEmbedding(
                    listOf(InputData.Text(prefix + text)),
                    EmbeddingOptions(normalize = true, outputSize = EMBEDDING_DIMENSION)
                ).embedding.takeIf { it.size == EMBEDDING_DIMENSION && it.all(Float::isFinite) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastError = "Embedding failed: ${error.localizedMessage ?: error.javaClass.simpleName}"
                null
            }
        }
    }

    fun rankChunks(
        queryVector: FloatArray,
        candidates: List<Pair<Long, ByteArray>>,
        topK: Int = 5
    ): List<ScoredChunk> = candidates.mapNotNull { (chunkId, blob) ->
        val vector = byteArrayToFloats(blob)
        if (vector.size != queryVector.size) null
        else ScoredChunk(chunkId, 0, cosineSimilarity(queryVector, vector))
    }.sortedByDescending { it.score }.take(topK)

    override fun close() {
        runtime?.close()
        runtime = null
        status = ModelInferenceStatus.NOT_INSTALLED
    }
}
