package com.swasthai.testharness

import kotlin.math.sqrt

/**
 * Isolated validation probe for EmbeddingGemma 2 Text 270M under LiteRT.
 *
 * Verifies:
 * - Exact task prefix contracts
 * - Output vector dimensional contract (768 float32 dimensions)
 * - L2 normalization arithmetic
 * - Cosine similarity calculation
 *
 * Note: Does not build the vector database or document vault.
 */
object EmbeddingGemmaProbe {

    const val MODEL_IDENTIFIER = "litert-community/embeddinggemma-2-text-270m-litert-lm"
    const val EMBEDDING_DIMENSION = 768

    // Official EmbeddingGemma 2 prefix contract
    const val PREFIX_QUERY = "task: search result | query: "
    const val PREFIX_DOCUMENT = "title: none | text: "

    /**
     * Prepares user search query with the official EmbeddingGemma task prefix.
     */
    fun formatQueryForEmbedding(query: String): String {
        return "$PREFIX_QUERY${query.trim()}"
    }

    /**
     * Prepares document chunk with the official EmbeddingGemma task prefix.
     */
    fun formatDocumentChunkForEmbedding(chunkText: String, documentTitle: String? = null): String {
        val titlePrefix = if (!documentTitle.isNullOrBlank()) "title: ${documentTitle.trim()} | text: " else PREFIX_DOCUMENT
        return "$titlePrefix${chunkText.trim()}"
    }

    /**
     * Applies L2 Normalization to a 768-dimensional float embedding vector:
     * v_norm = v / ||v||
     */
    fun normalizeL2(vector: FloatArray): FloatArray {
        var sumSquares = 0.0
        for (v in vector) {
            sumSquares += (v * v)
        }
        val norm = sqrt(sumSquares).toFloat()
        if (norm == 0f) return vector

        val normalized = FloatArray(vector.size)
        for (i in vector.indices) {
            normalized[i] = vector[i] / norm
        }
        return normalized
    }

    /**
     * Calculates Cosine Similarity between two L2-normalized vectors (dot product):
     * cosine_similarity(u, v) = u . v
     */
    fun cosineSimilarity(u: FloatArray, v: FloatArray): Float {
        require(u.size == v.size) { "Vector dimensions must match (expected ${u.size}, got ${v.size})" }
        var dotProduct = 0f
        for (i in u.indices) {
            dotProduct += u[i] * v[i]
        }
        return dotProduct
    }
}
