package com.swasthai.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID

data class DevicePublicKeyMaterial(
    val deviceId: String,
    val algorithm: String,
    val publicKeyBase64: String
)

/** Per-account RSA keys used to wrap profile keys for authorized family devices. */
class DeviceKeyManager(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getOrCreatePublicKey(accountUserId: String): DevicePublicKeyMaterial {
        require(accountUserId.isNotBlank())
        val alias = keyAlias(accountUserId)
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.getCertificate(alias) == null) {
            val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
            generator.initialize(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setKeySize(RSA_KEY_SIZE_BITS)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
                    .setUserAuthenticationRequired(false)
                    .build()
            )
            generator.generateKeyPair()
        }
        val publicKey = requireNotNull(keyStore.getCertificate(alias)?.publicKey) {
            "Could not read this account's Android Keystore public key"
        }
        return DevicePublicKeyMaterial(
            deviceId = deviceId(accountUserId),
            algorithm = ALGORITHM,
            publicKeyBase64 = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        )
    }

    @Synchronized
    private fun deviceId(accountUserId: String): String {
        val key = "device-id-${accountDigest(accountUserId)}"
        preferences.getString(key, null)?.let { return it }
        val id = UUID.randomUUID().toString()
        check(preferences.edit().putString(key, id).commit()) { "Could not save the device key ID" }
        return id
    }

    private fun keyAlias(accountUserId: String): String = "$KEY_ALIAS_PREFIX${accountDigest(accountUserId)}"

    private fun accountDigest(accountUserId: String): String = MessageDigest.getInstance("SHA-256")
        .digest(accountUserId.toByteArray(Charsets.UTF_8))
        .take(12)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    companion object {
        const val ALGORITHM = "RSA-OAEP-SHA256-MGF1-SHA1"
        private const val RSA_KEY_SIZE_BITS = 3072
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS_PREFIX = "swasthai.family.device."
        private const val PREFERENCES_NAME = "swasthai_family_device_keys"
    }
}
