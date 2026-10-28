package com.swasthai.app

import com.swasthai.app.data.repository.BiomarkerSummaryItem
import com.swasthai.app.data.repository.DoctorClinicalSummary
import com.swasthai.app.data.repository.DocumentSummaryItem
import com.swasthai.app.data.repository.MedicationSummaryItem
import com.swasthai.app.security.EncryptedPayloadEnvelope
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec
import java.security.spec.MGF1ParameterSpec

class DoctorShareTest {

    @Test
    fun testClinicalSummaryEncryptionAndDecryptionRoundtrip() {
        val summary = DoctorClinicalSummary(
            patientName = "Rajesh Sharma",
            relationship = "Father",
            generatedAt = 1700000000000L,
            validUntil = 1700000900000L,
            biomarkers = listOf(
                BiomarkerSummaryItem(
                    name = "Hemoglobin",
                    value = 14.5,
                    unit = "g/dL",
                    referenceLow = 13.0,
                    referenceHigh = 17.0,
                    isOutOfRange = false,
                    testDate = 1700000000000L
                ),
                BiomarkerSummaryItem(
                    name = "Fasting Blood Sugar",
                    value = 142.0,
                    unit = "mg/dL",
                    referenceLow = 70.0,
                    referenceHigh = 99.0,
                    isOutOfRange = true,
                    testDate = 1700000000000L
                )
            ),
            medications = listOf(
                MedicationSummaryItem(
                    medicationName = "Metformin 500mg",
                    instructions = "Take after breakfast",
                    hour = 9,
                    minute = 0
                )
            ),
            documents = listOf(
                DocumentSummaryItem(
                    fileName = "CBC_Report_Oct2026.pdf",
                    reportDate = 1700000000000L,
                    pageCount = 2
                )
            ),
            clinicalNotes = "Follow up on elevated glycemic levels"
        )

        // Serialize to JSON
        val root = JSONObject()
        root.put("patient_name", summary.patientName)
        root.put("relationship", summary.relationship)
        root.put("generated_at", summary.generatedAt)
        root.put("valid_until", summary.validUntil)
        root.put("clinical_notes", summary.clinicalNotes)

        val bioArray = JSONArray()
        summary.biomarkers.forEach { b ->
            val obj = JSONObject()
            obj.put("name", b.name)
            obj.put("value", b.value)
            obj.put("unit", b.unit)
            obj.put("is_out_of_range", b.isOutOfRange)
            obj.put("test_date", b.testDate)
            b.referenceLow?.let { obj.put("ref_low", it) }
            b.referenceHigh?.let { obj.put("ref_high", it) }
            bioArray.put(obj)
        }
        root.put("biomarkers", bioArray)

        // Encrypt with 256-bit AES one-time key
        val rawKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val secretKey = SecretKeySpec(rawKey, "AES")
        val aad = "doctor_share".toByteArray(Charsets.UTF_8)

        val ciphertext = EncryptedPayloadEnvelope.encrypt(
            root.toString().toByteArray(Charsets.UTF_8),
            secretKey,
            aad
        )

        // Decrypt with key
        val decryptedBytes = EncryptedPayloadEnvelope.decrypt(ciphertext, secretKey, aad)
        val decryptedJson = JSONObject(decryptedBytes.toString(Charsets.UTF_8))

        assertEquals("Rajesh Sharma", decryptedJson.getString("patient_name"))
        assertEquals("Father", decryptedJson.getString("relationship"))
        assertEquals("Follow up on elevated glycemic levels", decryptedJson.getString("clinical_notes"))

        val readBios = decryptedJson.getJSONArray("biomarkers")
        assertEquals(2, readBios.length())
        val fbs = readBios.getJSONObject(1)
        assertEquals("Fasting Blood Sugar", fbs.getString("name"))
        assertEquals(142.0, fbs.getDouble("value"), 0.01)
        assertTrue(fbs.getBoolean("is_out_of_range"))
    }

    @Test
    fun testRsaOaepKeyWrappingRoundtrip() {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val keyPair = keyGen.generateKeyPair()

        val rawAesKey = ByteArray(32).also { SecureRandom().nextBytes(it) }

        // Wrap with RSA-OAEP SHA-256 / MGF1-SHA-1
        val cipherEncrypt = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        val oaepParams = OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT
        )
        cipherEncrypt.init(Cipher.ENCRYPT_MODE, keyPair.public, oaepParams)
        val wrapped = cipherEncrypt.doFinal(rawAesKey)

        assertNotNull(wrapped)
        assertTrue(wrapped.size >= 256)

        // Unwrap with RSA Private Key
        val cipherDecrypt = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipherDecrypt.init(Cipher.DECRYPT_MODE, keyPair.private, oaepParams)
        val unwrapped = cipherDecrypt.doFinal(wrapped)

        assertEquals(32, unwrapped.size)
        org.junit.Assert.assertArrayEquals(rawAesKey, unwrapped)
    }
}
