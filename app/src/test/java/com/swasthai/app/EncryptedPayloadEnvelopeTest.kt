package com.swasthai.app

import com.swasthai.app.security.EncryptedPayloadEnvelope
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.File
import javax.crypto.spec.SecretKeySpec

class EncryptedPayloadEnvelopeTest {
    private val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")

    @Test
    fun byteEnvelopeRoundTripsWithAuthenticatedContext() {
        val plaintext = "private health record".toByteArray()
        val aad = "profile:member-1".toByteArray()

        val encrypted = EncryptedPayloadEnvelope.encrypt(plaintext, key, aad)

        assertArrayEquals(plaintext, EncryptedPayloadEnvelope.decrypt(encrypted, key, aad))
    }

    @Test
    fun fileEnvelopeRoundTripsWithoutBufferingWholeFile() {
        val input = File.createTempFile("swasthai-plain-", ".tmp")
        val encrypted = File.createTempFile("swasthai-encrypted-", ".tmp")
        val output = File.createTempFile("swasthai-restored-", ".tmp")
        val plaintext = ByteArray(128 * 1024) { (it % 239).toByte() }
        val aad = "profile:member-1:record:report-1".toByteArray()
        try {
            input.writeBytes(plaintext)
            EncryptedPayloadEnvelope.encryptFile(input, encrypted, key, aad)
            EncryptedPayloadEnvelope.decryptFile(encrypted, output, key, aad)

            assertArrayEquals(plaintext, output.readBytes())
        } finally {
            input.delete()
            encrypted.delete()
            output.delete()
        }
    }
}
