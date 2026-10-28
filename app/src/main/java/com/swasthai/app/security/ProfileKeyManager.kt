package com.swasthai.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

data class RecoverySetup(val phrase: String, val encryptedKeyEnvelope: ByteArray)

class KeyRecoveryRequiredException(message: String, cause: Throwable? = null) : GeneralSecurityException(message, cause)

/** Profile DEKs are random AES-256 keys wrapped by a non-exportable Android Keystore key. */
class ProfileKeyManager(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val recoveryPhraseCodec = RecoveryPhraseCodec(appContext)

    @Synchronized
    fun getOrCreateProfileKey(profileId: String): SecretKey {
        require(profileId.isNotBlank())
        val preferenceKey = preferenceKey(profileId)
        val stored = preferences.getString(preferenceKey, null)
        if (stored != null) {
            return try {
                val wrapped = Base64.decode(stored, Base64.NO_WRAP)
                val raw = EncryptedPayloadEnvelope.decrypt(wrapped, getLocalWrappingKey(), localAad(profileId))
                require(raw.size == PROFILE_KEY_BYTES) { "Stored profile key has invalid length" }
                SecretKeySpec(raw, "AES")
            } catch (error: Exception) {
                throw KeyRecoveryRequiredException(
                    "This device's local key is unavailable. Restore the profile using its recovery phrase.",
                    error
                )
            }
        }
        val raw = ByteArray(PROFILE_KEY_BYTES).also(random::nextBytes)
        storeProfileKey(profileId, raw)
        return SecretKeySpec(raw, "AES")
    }

    /** Generates a phrase once and returns a server-storable envelope; the phrase is never persisted. */
    @Synchronized
    fun createRecoverySetup(profileId: String): RecoverySetup {
        val profileKey = getOrCreateProfileKey(profileId)
        val phrase = recoveryPhraseCodec.generate()
        val recoveryKey = recoveryPhraseCodec.keyFromPhrase(phrase)
        val envelope = EncryptedPayloadEnvelope.encrypt(
            profileKey.encoded,
            recoveryKey,
            recoveryAad(profileId)
        )
        return RecoverySetup(phrase = phrase, encryptedKeyEnvelope = envelope)
    }

    /** Regenerates the opaque backup wrap after re-entering the existing phrase. */
    fun createRecoveryEnvelope(profileId: String, phrase: String): ByteArray {
        val profileKey = getOrCreateProfileKey(profileId)
        val recoveryKey = recoveryPhraseCodec.keyFromPhrase(phrase)
        return EncryptedPayloadEnvelope.encrypt(profileKey.encoded, recoveryKey, recoveryAad(profileId))
    }

    /** Restores a profile key on a new device and immediately rewraps it under that device's Keystore. */
    @Synchronized
    fun restoreProfileKey(profileId: String, phrase: String, encryptedKeyEnvelope: ByteArray) {
        val recoveryKey = recoveryPhraseCodec.keyFromPhrase(phrase)
        val raw = EncryptedPayloadEnvelope.decrypt(encryptedKeyEnvelope, recoveryKey, recoveryAad(profileId))
        require(raw.size == PROFILE_KEY_BYTES) { "Recovery bundle contains an invalid profile key" }
        storeProfileKey(profileId, raw)
    }

    private fun storeProfileKey(profileId: String, rawKey: ByteArray) {
        val wrapped = EncryptedPayloadEnvelope.encrypt(rawKey, getLocalWrappingKey(), localAad(profileId))
        val saved = preferences.edit().putString(preferenceKey(profileId), Base64.encodeToString(wrapped, Base64.NO_WRAP)).commit()
        check(saved) { "Could not securely store the local profile key" }
    }

    private fun getLocalWrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(LOCAL_WRAPPING_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                LOCAL_WRAPPING_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun preferenceKey(profileId: String): String = "profile-key-${
        MessageDigest.getInstance("SHA-256").digest(profileId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }"
    private fun localAad(profileId: String) = "swasthai-local-profile-key-v1:$profileId".toByteArray(Charsets.UTF_8)
    private fun recoveryAad(profileId: String) = "swasthai-recovery-profile-key-v1:$profileId".toByteArray(Charsets.UTF_8)

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val LOCAL_WRAPPING_KEY_ALIAS = "swasthai.local.profile.wrap.v1"
        private const val PREFERENCES_NAME = "swasthai_local_profile_keys"
        private const val PROFILE_KEY_BYTES = 32
        private val random = SecureRandom()
    }
}
