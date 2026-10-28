package com.swasthai.app.security

import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.io.File

/**
 * Version 1 payload envelope shared with the local API:
 * `SWAI` magic, one version byte, a 12-byte AES-GCM nonce, then ciphertext + 128-bit tag.
 * Key creation, storage, recovery, and distribution belong to the key manager, not this codec.
 */
object EncryptedPayloadEnvelope {
    const val VERSION: Int = 1
    const val HEADER_AND_NONCE_AND_TAG_BYTES: Int = 5 + 12 + 16

    private val MAGIC = byteArrayOf('S'.code.toByte(), 'W'.code.toByte(), 'A'.code.toByte(), 'I'.code.toByte())
    private const val HEADER_SIZE = 5
    private const val NONCE_SIZE = 12
    private const val TAG_SIZE_BITS = 128
    private const val TAG_SIZE_BYTES = TAG_SIZE_BITS / 8
    @Throws(GeneralSecurityException::class)
    fun encrypt(plaintext: ByteArray, key: SecretKey, associatedData: ByteArray = byteArrayOf()): ByteArray {
        val header = byteArrayOf(MAGIC[0], MAGIC[1], MAGIC[2], MAGIC[3], VERSION.toByte())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val nonce = requireGeneratedNonce(cipher.iv)
        cipher.updateAAD(header)
        if (associatedData.isNotEmpty()) cipher.updateAAD(associatedData)
        val ciphertextAndTag = cipher.doFinal(plaintext)
        return ByteArray(header.size + nonce.size + ciphertextAndTag.size).also { envelope ->
            header.copyInto(envelope, 0)
            nonce.copyInto(envelope, header.size)
            ciphertextAndTag.copyInto(envelope, header.size + nonce.size)
        }
    }

    @Throws(GeneralSecurityException::class)
    fun decrypt(envelope: ByteArray, key: SecretKey, associatedData: ByteArray = byteArrayOf()): ByteArray {
        val header = validateAndReadHeader(envelope)
        val nonceStart = header.size
        val nonceEnd = nonceStart + NONCE_SIZE
        val nonce = envelope.copyOfRange(nonceStart, nonceEnd)
        val ciphertextAndTag = envelope.copyOfRange(nonceEnd, envelope.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, nonce))
        cipher.updateAAD(header)
        if (associatedData.isNotEmpty()) cipher.updateAAD(associatedData)
        return cipher.doFinal(ciphertextAndTag)
    }

    /** Stream a file through AES-GCM without buffering the report in memory. */
    @Throws(GeneralSecurityException::class)
    fun encryptFile(input: File, output: File, key: SecretKey, associatedData: ByteArray = byteArrayOf()) {
        val header = byteArrayOf(MAGIC[0], MAGIC[1], MAGIC[2], MAGIC[3], VERSION.toByte())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val nonce = requireGeneratedNonce(cipher.iv)
        cipher.updateAAD(header)
        if (associatedData.isNotEmpty()) cipher.updateAAD(associatedData)

        output.parentFile?.mkdirs()
        try {
            input.inputStream().buffered().use { source ->
                output.outputStream().buffered().use { sink ->
                    sink.write(header)
                    sink.write(nonce)
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        cipher.update(buffer, 0, count)?.let(sink::write)
                    }
                    cipher.doFinal().let(sink::write)
                    sink.flush()
                }
            }
        } catch (error: Throwable) {
            output.delete()
            throw error
        }
    }

    /** Decrypt into a caller-owned temporary file; do not expose it until GCM verifies the tag. */
    @Throws(GeneralSecurityException::class)
    fun decryptFile(input: File, output: File, key: SecretKey, associatedData: ByteArray = byteArrayOf()) {
        output.parentFile?.mkdirs()
        try {
            input.inputStream().buffered().use { source ->
                val header = ByteArray(HEADER_SIZE)
                readFully(source, header)
                validateHeader(header)
                val nonce = ByteArray(NONCE_SIZE)
                readFully(source, nonce)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, nonce))
                cipher.updateAAD(header)
                if (associatedData.isNotEmpty()) cipher.updateAAD(associatedData)

                output.outputStream().buffered().use { sink ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        cipher.update(buffer, 0, count)?.let(sink::write)
                    }
                    cipher.doFinal().let(sink::write)
                    sink.flush()
                }
            }
        } catch (error: Throwable) {
            output.delete()
            throw error
        }
    }

    fun hasSupportedEnvelopeHeader(envelope: ByteArray): Boolean =
        runCatching { validateAndReadHeader(envelope) }.isSuccess

    private fun validateAndReadHeader(envelope: ByteArray): ByteArray {
        val minimumSize = HEADER_SIZE + NONCE_SIZE + TAG_SIZE_BYTES
        if (envelope.size < minimumSize) {
            throw GeneralSecurityException("Encrypted payload is shorter than a version 1 AES-GCM envelope")
        }
        val header = envelope.copyOfRange(0, HEADER_SIZE)
        validateHeader(header)
        return header
    }

    private fun validateHeader(header: ByteArray) {
        if (header.size != HEADER_SIZE || !header.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw GeneralSecurityException("Encrypted payload magic is invalid")
        }
        if (header[MAGIC.size].toInt() and 0xff != VERSION) {
            throw GeneralSecurityException("Encrypted payload version is unsupported")
        }
    }

    private fun requireGeneratedNonce(nonce: ByteArray?): ByteArray {
        if (nonce == null || nonce.size != NONCE_SIZE) {
            throw GeneralSecurityException("AES-GCM provider did not generate a 12-byte nonce")
        }
        return nonce
    }

    private fun readFully(input: java.io.InputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read < 0) throw GeneralSecurityException("Encrypted payload ended unexpectedly")
            offset += read
        }
    }
}
