package com.swasthai.app

import com.swasthai.app.pdf.DocumentChunker
import com.swasthai.app.pdf.ExtractedPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentChunkerTest {

    @Test
    fun testChunkPagesPreservesPageNumbersAndMonotonicIndex() {
        val pages = listOf(
            ExtractedPage(
                pageNumber = 1,
                text = "Patient Name: Rajesh Sharma\nAge: 45\n\nComplete Blood Count (CBC):\nHemoglobin: 14.5 g/dL (13.0 - 17.0)\nWBC: 6500 /mcL (4000 - 11000)"
            ),
            ExtractedPage(
                pageNumber = 2,
                text = "Platelet Count: 245000 /mcL (150000 - 450000)\nESR: 12 mm/hr (0 - 15)\n\nConclusion: Normal hemogram."
            )
        )

        val chunks = DocumentChunker.chunkPages(documentId = 101L, pages = pages)

        assertTrue("Should produce at least 2 chunks", chunks.size >= 2)
        assertEquals(0, chunks[0].chunkIndex)
        assertEquals(1, chunks[0].pageNumber)
        assertEquals(101L, chunks[0].documentId)

        val lastChunk = chunks.last()
        assertEquals(2, lastChunk.pageNumber)
        assertEquals(chunks.size - 1, lastChunk.chunkIndex)
    }

    @Test
    fun testBiomarkerExtractionFromLabReport() {
        val pages = listOf(
            ExtractedPage(
                pageNumber = 1,
                text = "Lab Report\nHemoglobin: 14.2 g/dL (13.0 - 17.0)\nPlatelets: 250000 /mcL (150000 - 450000)\nFasting Blood Sugar: 95 mg/dL (70 - 99)"
            )
        )

        val measurements = DocumentChunker.extractStructuredMeasurements(
            documentId = 202L,
            pages = pages,
            reportDate = 1700000000000L
        )

        assertEquals(3, measurements.size)

        val hb = measurements.find { it.biomarkerName.contains("Hemoglobin", ignoreCase = true) }
        assertNotNull(hb)
        assertEquals(14.2, hb!!.numericValue, 0.01)
        assertEquals("g/dL", hb.unit)
        assertEquals(13.0, hb.referenceLow!!, 0.01)
        assertEquals(17.0, hb.referenceHigh!!, 0.01)
        assertTrue("Hemoglobin should be within range", !hb.isOutOfRange)

        val fbs = measurements.find { it.biomarkerName.contains("Fasting", ignoreCase = true) }
        assertNotNull(fbs)
        assertEquals(95.0, fbs!!.numericValue, 0.01)
    }
}
