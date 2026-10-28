package com.swasthai.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

data class StoredAuthSession(val cookieHeader: String, val userId: String, val email: String)

/** Stores the Better Auth session cookie encrypted by a non-exportable Android Keystore key. */
class SecureAuthSessionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("swasthai_auth_session", Context.MODE_PRIVATE)

    @Synchronized
    fun read(): StoredAuthSession? {
        val encoded = preferences.getString(SESSION_KEY, null) ?: return null
        return runCatching {
            val plaintext = EncryptedPayloadEnvelope.decrypt(
                Base64.decode(encoded, Base64.DEFAULT),
                getOrCreateKey(),
                SESSION_AAD
            )
            val json = JSONObject(String(plaintext, Charsets.UTF_8))
            StoredAuthSession(
                cookieHeader = json.getString("cookie"),
                userId = json.getString("userId"),
                email = json.getString("email")
            )
        }.getOrNull()
    }

    @Synchronized
    fun write(session: StoredAuthSession) {
        val plaintext = JSONObject()
            .put("cookie", session.cookieHeader)
            .put("userId", session.userId)
            .put("email", session.email)
            .toString()
            .toByteArray(Charsets.UTF_8)
        val encrypted = EncryptedPayloadEnvelope.encrypt(plaintext, getOrCreateKey(), SESSION_AAD)
        check(preferences.edit().putString(SESSION_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit()) {
            "Could not securely store the account session"
        }
    }

    @Synchronized
    fun clear() {
        check(preferences.edit().remove(SESSION_KEY).commit()) { "Could not clear the account session" }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "swasthai.auth.session.v1"
        const val SESSION_KEY = "encrypted_session"
        val SESSION_AAD = "swasthai-better-auth-session-v1".toByteArray(Charsets.UTF_8)
    }
}
