package com.swasthai.app

import com.swasthai.app.ai.EmbeddingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddingEngineTest {

    @Test
    fun testVectorSerializationRoundTrip() {
        val original = floatArrayOf(0.123f, -0.456f, 0.789f, 1.0f, -1.0f)
        val bytes = EmbeddingEngine.floatsToByteArray(original)
        assertEquals(original.size * 4, bytes.size)

        val restored = EmbeddingEngine.byteArrayToFloats(bytes)
        assertEquals(original.size, restored.size)
        for (i in original.indices) {
            assertEquals(original[i], restored[i], 1e-6f)
        }
    }

    @Test
    fun testCosineSimilarityIdenticalAndOrthogonal() {
        val v1 = floatArrayOf(1.0f, 0.0f, 0.0f)
        val v2 = floatArrayOf(1.0f, 0.0f, 0.0f)
        assertEquals(1.0f, EmbeddingEngine.cosineSimilarity(v1, v2), 1e-4f)

        val vOrthogonal = floatArrayOf(0.0f, 1.0f, 0.0f)
        assertEquals(0.0f, EmbeddingEngine.cosineSimilarity(v1, vOrthogonal), 1e-4f)

        val vOpposite = floatArrayOf(-1.0f, 0.0f, 0.0f)
        assertEquals(-1.0f, EmbeddingEngine.cosineSimilarity(v1, vOpposite), 1e-4f)
    }

    @Test
    fun testRankingOrdering() {
        val engine = EmbeddingEngine()
        val queryVector = floatArrayOf(1.0f, 0.0f, 0.0f)

        val c1 = 1L to EmbeddingEngine.floatsToByteArray(floatArrayOf(0.2f, 0.8f, 0.0f))
        val c2 = 2L to EmbeddingEngine.floatsToByteArray(floatArrayOf(0.9f, 0.1f, 0.0f))
        val c3 = 3L to EmbeddingEngine.floatsToByteArray(floatArrayOf(0.5f, 0.5f, 0.0f))

        val ranked = engine.rankChunks(queryVector, listOf(c1, c2, c3), topK = 3)

        assertEquals(3, ranked.size)
        assertEquals(2L, ranked[0].chunkId) // c2 is closest to [1, 0, 0]
        assertEquals(3L, ranked[1].chunkId) // c3 is second
        assertEquals(1L, ranked[2].chunkId) // c1 is third
        assertTrue(ranked[0].score > ranked[1].score)
        assertTrue(ranked[1].score > ranked[2].score)
    }
}
