package com.swasthai.app

import com.swasthai.app.pdf.DocumentChunker
import com.swasthai.app.pdf.PdfExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PdfExtractorTest {

    @Test
    fun testSamplePdfGenerationAndExtraction() {
        val lines = listOf(
            "METROPOLITAN CLINICAL HEALTH LABORATORIES",
            "Patient Diagnostic Health Panel & Laboratory Report",
            "Report Date: 15/10/2026",
            "Specimen Type: Whole Blood / Fasting Serum",
            "",
            "COMPLETE BLOOD COUNT (CBC) WITH DIFFERENTIAL",
            "Hemoglobin: 14.8 g/dL (13.5 - 17.5)",
            "RBC Count: 5.10 x10^6/uL (4.50 - 5.90)",
            "White Blood Cells (WBC): 6.8 x10^3/uL (4.5 - 11.0)",
            "Platelets: 245 x10^3/uL (150 - 450)",
            "Hematocrit: 44.2 % (41.0 - 50.0)",
            "",
            "COMPREHENSIVE METABOLIC & GLYCEMIC PANEL",
            "Fasting Blood Sugar: 92 mg/dL (70 - 99)",
            "Serum Creatinine: 0.95 mg/dL (0.70 - 1.30)",
            "Blood Urea Nitrogen (BUN): 14 mg/dL (7 - 20)",
            "ALT (SGPT): 24 U/L (7 - 56)",
            "AST (SGOT): 22 U/L (8 - 48)",
            "",
            "LIPID CARDIOVASCULAR PANEL",
            "Total Cholesterol: 185 mg/dL (100 - 199)",
            "HDL Cholesterol: 55 mg/dL (40 - 60)",
            "LDL Cholesterol: 108 mg/dL (50 - 99)",
            "Triglycerides: 130 mg/dL (50 - 149)",
            "",
            "MICRONUTRIENT & ENDOCRINE STATUS",
            "Vitamin D (25-OH): 38 ng/mL (30 - 100)",
            "Vitamin B12: 450 pg/mL (200 - 900)",
            "Thyroid Stimulating Hormone (TSH): 2.1 uIU/mL (0.4 - 4.0)"
        )

        val streamContent = buildString {
            append("BT\n/F1 12 Tf\n50 720 Td\n")
            for (line in lines) {
                val escaped = line.replace("(", "\\(").replace(")", "\\)")
                append("($escaped) Tj\nT*\n")
            }
            append("ET\n")
        }

        val pdf = buildString {
            append("%PDF-1.4\n")
            append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
            append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")
            append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n")
            append("4 0 obj\n<< /Length ${streamContent.length} >>\nstream\n")
            append(streamContent)
            append("endstream\nendobj\n")
            append("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n")
            append("xref\n0 6\n0000000000 65535 f \n0000000010 00000 n \n0000000060 00000 n \n0000000117 00000 n \n0000000234 00000 n \n0000000450 00000 n \n")
            append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n540\n%%EOF\n")
        }

        val tempFile = File.createTempFile("test_sample", ".pdf")
        tempFile.writeBytes(pdf.toByteArray(Charsets.ISO_8859_1))

        val result = PdfExtractor.extractTextFromPdf(tempFile)
        println("Result pages: ${result.pages.size}, isScanned: ${result.isScannedOnly}, failureReason: ${result.failureReason}")
        if (result.pages.isNotEmpty()) {
            println("Text snippet:\n${result.pages[0].text.take(200)}")
            val measurements = DocumentChunker.extractStructuredMeasurements(1L, result.pages, System.currentTimeMillis())
            println("Extracted measurements count: ${measurements.size}")
            for (m in measurements) {
                println(" - ${m.biomarkerName}: ${m.numericValue} ${m.unit}")
            }
        }
        tempFile.delete()

        assertFalse("Should not be scanned only", result.isScannedOnly)
        assertTrue("Should extract pages", result.pages.isNotEmpty())
    }
}
