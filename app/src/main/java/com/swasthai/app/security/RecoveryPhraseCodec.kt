package com.swasthai.app.security

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/** BIP-39 256-bit English mnemonic encoding, used only as a user-held key-encryption key. */
class RecoveryPhraseCodec(context: Context) {
    private val words by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        context.assets.open(WORDLIST_ASSET).bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readLines().also { require(it.size == WORD_COUNT) { "Recovery wordlist is invalid" } }
        }
    }
    private val wordIndices by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { words.withIndex().associate { it.value to it.index } }

    fun generate(): String {
        val entropy = ByteArray(ENTROPY_BYTES).also(random::nextBytes)
        return encodeEntropy(entropy)
    }

    /** Validates the phrase checksum and returns the 256-bit entropy as an AES-256 key. */
    fun keyFromPhrase(phrase: String): SecretKey {
        val tokens = phrase.trim().lowercase(Locale.ROOT).split(Regex("\\s+")).filter(String::isNotBlank)
        require(tokens.size == WORD_COUNT) { "Enter all 24 recovery words" }
        val bits = BooleanArray(TOTAL_BITS)
        tokens.forEachIndexed { wordPosition, word ->
            val value = wordIndices[word] ?: throw IllegalArgumentException("Recovery phrase contains an unknown word")
            for (bit in 0 until BITS_PER_WORD) {
                bits[wordPosition * BITS_PER_WORD + bit] = value and (1 shl (BITS_PER_WORD - bit - 1)) != 0
            }
        }
        val entropy = bitsToBytes(bits, ENTROPY_BITS)
        val expectedChecksum = MessageDigest.getInstance("SHA-256").digest(entropy)
        for (bit in 0 until CHECKSUM_BITS) {
            val expected = expectedChecksum[0].toInt() and (1 shl (7 - bit)) != 0
            require(bits[ENTROPY_BITS + bit] == expected) { "Recovery phrase checksum is invalid" }
        }
        return SecretKeySpec(entropy, "AES")
    }

    private fun encodeEntropy(entropy: ByteArray): String {
        require(entropy.size == ENTROPY_BYTES)
        val checksum = MessageDigest.getInstance("SHA-256").digest(entropy)
        val wordsOut = ArrayList<String>(WORD_COUNT)
        for (wordPosition in 0 until WORD_COUNT) {
            var value = 0
            for (bitPosition in 0 until BITS_PER_WORD) {
                val globalBit = wordPosition * BITS_PER_WORD + bitPosition
                val bit = if (globalBit < ENTROPY_BITS) {
                    entropy[globalBit / 8].toInt() and (1 shl (7 - globalBit % 8)) != 0
                } else {
                    val checksumBit = globalBit - ENTROPY_BITS
                    checksum[0].toInt() and (1 shl (7 - checksumBit)) != 0
                }
                value = (value shl 1) or if (bit) 1 else 0
            }
            wordsOut += words[value]
        }
        return wordsOut.joinToString(" ")
    }

    private fun bitsToBytes(bits: BooleanArray, count: Int): ByteArray = ByteArray(count / 8).also { bytes ->
        for (bit in 0 until count) {
            if (bits[bit]) bytes[bit / 8] = (bytes[bit / 8].toInt() or (1 shl (7 - bit % 8))).toByte()
        }
    }

    companion object {
        const val WORD_COUNT = 24
        private const val WORDLIST_ASSET = "bip39-english.txt"
        private const val ENTROPY_BYTES = 32
        private const val ENTROPY_BITS = ENTROPY_BYTES * 8
        private const val CHECKSUM_BITS = ENTROPY_BITS / 32
        private const val BITS_PER_WORD = 11
        private const val TOTAL_BITS = ENTROPY_BITS + CHECKSUM_BITS
        private val random = SecureRandom()
    }
}
