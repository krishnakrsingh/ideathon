package com.swasthai.app.data.repository

import android.content.Context
import com.swasthai.app.security.SecureAuthSessionStore
import com.swasthai.app.security.StoredAuthSession
import com.swasthai.app.security.DevicePublicKeyMaterial
import com.swasthai.app.security.DeviceKeyManager
import com.swasthai.app.security.ProfileKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class FamilyMemberAccount(val userId: String, val role: String, val joinedAt: Long)
data class FamilyDevicePublicKey(
    val userId: String,
    val role: String,
    val deviceId: String,
    val algorithm: String,
    val publicKeyBase64: String
)
data class FamilyBlockedAccount(val userId: String, val blockedBy: String, val blockedAt: Long)
data class AccessibleCloudProfile(
    val profileId: String,
    val accessRole: String,
    val hasDecryptionGrant: Boolean,
    val createdAt: Long
)
data class FamilyAccount(
    val familyId: String,
    val headUserId: String,
    val yourRole: String,
    val members: List<FamilyMemberAccount>,
    val blockedMembers: List<FamilyBlockedAccount> = emptyList()
)
data class FamilyCredentials(val familyId: String, val joinPassword: String)
data class PendingFamilyInvite(
    val inviteId: String,
    val profileId: String,
    val inviterUserId: String,
    val role: String,
    val expiresAt: Long,
    val createdAt: Long
)
data class AcceptedKeyGrant(
    val inviteId: String,
    val profileId: String,
    val role: String,
    val encryptedKeyGrantBase64: String,
    val acceptedAt: Long
)

/** Better Auth account sessions plus the authenticated family-group API. */
class AccountFamilyRepository(context: Context) {
    private val sessionStore = SecureAuthSessionStore(context)
    private val preferences = context.getSharedPreferences("swasthai_service_endpoints", Context.MODE_PRIVATE)

    val currentSession: StoredAuthSession? get() = sessionStore.read()
    val savedAuthUrl: String get() = preferences.getString(AUTH_URL, "") ?: ""
    val savedApiUrl: String get() = preferences.getString(API_URL, "") ?: ""

    fun saveServiceUrls(authUrl: String, apiUrl: String) {
        val normalizedAuth = validateBaseUrl(authUrl)
        val normalizedApi = validateBaseUrl(apiUrl)
        check(preferences.edit().putString(AUTH_URL, normalizedAuth).putString(API_URL, normalizedApi).commit()) {
            "Could not save the server addresses"
        }
    }

    suspend fun signIn(authUrl: String, email: String, password: String): StoredAuthSession = withContext(Dispatchers.IO) {
        saveServiceUrls(authUrl, savedApiUrl.ifBlank { authUrl })
        val response = request(
            baseUrl = normalizedAuthUrl(),
            path = "/api/auth/sign-in/email",
            method = "POST",
            body = JSONObject().put("email", email.trim()).put("password", password).put("rememberMe", true)
        )
        var cookie = mergeCookies(null, response.setCookies)
        val userResponse = request(normalizedAuthUrl(), "/api/auth/get-session", "GET", cookieHeader = cookie)
        cookie = mergeCookies(cookie, userResponse.setCookies)
        val user = userResponse.json.optJSONObject("user") ?: throw IllegalStateException("The account server returned no signed-in user")
        require(cookie.isNotBlank()) { "Sign-in returned no session cookie" }
        StoredAuthSession(cookie, user.getString("id"), user.getString("email")).also(sessionStore::write)
    }

    suspend fun signUp(authUrl: String, name: String, email: String, password: String): StoredAuthSession = withContext(Dispatchers.IO) {
        saveServiceUrls(authUrl, savedApiUrl.ifBlank { authUrl })
        val response = request(
            baseUrl = normalizedAuthUrl(),
            path = "/api/auth/sign-up/email",
            method = "POST",
            body = JSONObject().put("name", name.trim()).put("email", email.trim()).put("password", password)
        )
        var cookie = mergeCookies(null, response.setCookies)
        val userResponse = request(normalizedAuthUrl(), "/api/auth/get-session", "GET", cookieHeader = cookie)
        cookie = mergeCookies(cookie, userResponse.setCookies)
        val user = userResponse.json.optJSONObject("user") ?: response.json.optJSONObject("user")
            ?: throw IllegalStateException("Account created. Verify the email address, then sign in to continue.")
        require(cookie.isNotBlank()) { "Account created. Verify the email address, then sign in to continue." }
        StoredAuthSession(cookie, user.getString("id"), user.getString("email")).also(sessionStore::write)
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        val session = sessionStore.read() ?: return@withContext
        runCatching { request(normalizedAuthUrl(), "/api/auth/sign-out", "POST", JSONObject(), session.cookieHeader) }
        sessionStore.clear()
    }

    suspend fun listFamilies(): List<FamilyAccount> = withContext(Dispatchers.IO) {
        val response = apiRequest("GET", "/api/v1/families")
        response.jsonArray().map { row ->
            FamilyAccount(
                familyId = row.getString("family_id"),
                headUserId = row.getString("head_user_id"),
                yourRole = row.getString("role"),
                members = emptyList()
            )
        }
    }

    suspend fun getFamily(familyId: String): FamilyAccount = withContext(Dispatchers.IO) {
        val row = apiRequest("GET", "/api/v1/families/${pathSegment(familyId)}").json
        val members = row.optJSONArray("members") ?: JSONArray()
        val blocked = row.optJSONArray("blocked_members") ?: JSONArray()
        FamilyAccount(
            familyId = row.getString("family_id"),
            headUserId = row.getString("head_user_id"),
            yourRole = row.getString("your_role"),
            members = (0 until members.length()).map { index ->
                val member = members.getJSONObject(index)
                FamilyMemberAccount(member.getString("user_id"), member.getString("role"), member.getLong("joined_at"))
            },
            blockedMembers = (0 until blocked.length()).map { index ->
                val member = blocked.getJSONObject(index)
                FamilyBlockedAccount(member.getString("user_id"), member.getString("blocked_by"), member.getLong("blocked_at"))
            }
        )
    }

    suspend fun listAccessibleProfiles(): List<AccessibleCloudProfile> = withContext(Dispatchers.IO) {
        apiRequest("GET", "/api/v1/profiles").jsonArray().map { row ->
            AccessibleCloudProfile(
                profileId = row.getString("profile_id"),
                accessRole = row.getString("access_role"),
                hasDecryptionGrant = row.optBoolean("has_decryption_grant", false),
                createdAt = row.getLong("created_at")
            )
        }
    }

    suspend fun registerDevicePublicKey(material: DevicePublicKeyMaterial): Unit = withContext(Dispatchers.IO) {
        apiRequest(
            "PUT",
            "/api/v1/devices/${pathSegment(material.deviceId)}/public-key",
            JSONObject().put("key_algorithm", material.algorithm).put("public_key", material.publicKeyBase64)
        )
        Unit
    }

    suspend fun revokeDevicePublicKey(deviceId: String): Unit = withContext(Dispatchers.IO) {
        apiRequest("DELETE", "/api/v1/devices/${pathSegment(deviceId)}/public-key")
        Unit
    }

    suspend fun listFamilyDevicePublicKeys(familyId: String): List<FamilyDevicePublicKey> = withContext(Dispatchers.IO) {
        apiRequest("GET", "/api/v1/families/${pathSegment(familyId)}/device-keys").jsonArray().map { row ->
            FamilyDevicePublicKey(
                userId = row.getString("user_id"),
                role = row.getString("role"),
                deviceId = row.getString("device_id"),
                algorithm = row.getString("key_algorithm"),
                publicKeyBase64 = row.getString("public_key")
            )
        }
    }

    suspend fun createFamily(): FamilyCredentials = withContext(Dispatchers.IO) {
        val row = apiRequest("POST", "/api/v1/families").json
        FamilyCredentials(row.getString("family_id"), row.getString("join_password"))
    }

    suspend fun joinFamily(familyId: String, password: String): Unit = withContext(Dispatchers.IO) {
        apiRequest(
            "POST", "/api/v1/families/join",
            JSONObject().put("family_id", familyId.trim()).put("join_password", password.trim())
        )
        Unit
    }

    suspend fun rotateJoinPassword(familyId: String): FamilyCredentials = withContext(Dispatchers.IO) {
        val row = apiRequest("POST", "/api/v1/families/${pathSegment(familyId)}/join-password/rotate").json
        FamilyCredentials(row.getString("family_id"), row.getString("join_password"))
    }

    suspend fun removeMember(familyId: String, memberUserId: String): Unit = withContext(Dispatchers.IO) {
        apiRequest("DELETE", "/api/v1/families/${pathSegment(familyId)}/members/${pathSegment(memberUserId)}")
        Unit
    }

    suspend fun unblockMember(familyId: String, memberUserId: String): Unit = withContext(Dispatchers.IO) {
        apiRequest("POST", "/api/v1/families/${pathSegment(familyId)}/members/${pathSegment(memberUserId)}/unblock")
        Unit
    }

    suspend fun leaveFamily(familyId: String): Unit = withContext(Dispatchers.IO) {
        apiRequest("DELETE", "/api/v1/families/${pathSegment(familyId)}/membership")
        Unit
    }

    suspend fun createFamilyInvite(
        profileId: String,
        inviteeUserId: String,
        encryptedKeyGrantBase64: String,
        role: String = "caregiver",
        expiresInHours: Int = 72
    ): String = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("invitee_user_id", inviteeUserId)
            .put("role", role)
            .put("encrypted_key_grant", encryptedKeyGrantBase64)
            .put("expires_in_hours", expiresInHours)
        val response = apiRequest("POST", "/api/v1/profiles/${pathSegment(profileId)}/family-invites", body)
        response.json.getString("invite_id")
    }

    suspend fun listPendingInvites(): List<PendingFamilyInvite> = withContext(Dispatchers.IO) {
        val rows = apiRequest("GET", "/api/v1/family-invites").jsonArray()
        rows.map { row ->
            PendingFamilyInvite(
                inviteId = row.getString("invite_id"),
                profileId = row.getString("profile_id"),
                inviterUserId = row.getString("inviter_user_id"),
                role = row.getString("role"),
                expiresAt = row.getLong("expires_at"),
                createdAt = row.getLong("created_at")
            )
        }
    }

    suspend fun acceptFamilyInvite(inviteId: String): AcceptedKeyGrant = withContext(Dispatchers.IO) {
        val row = apiRequest("POST", "/api/v1/family-invites/${pathSegment(inviteId)}/accept").json
        AcceptedKeyGrant(
            inviteId = row.getString("invite_id"),
            profileId = row.getString("profile_id"),
            role = row.getString("role"),
            encryptedKeyGrantBase64 = row.getString("encrypted_key_grant"),
            acceptedAt = row.getLong("accepted_at")
        )
    }

    suspend fun rejectFamilyInvite(inviteId: String): Unit = withContext(Dispatchers.IO) {
        apiRequest("POST", "/api/v1/family-invites/${pathSegment(inviteId)}/reject")
        Unit
    }

    suspend fun grantProfileAccessToFamilyHead(
        familyId: String,
        profileId: String,
        profileKeyBytes: ByteArray,
        deviceKeyManager: DeviceKeyManager
    ): Boolean = withContext(Dispatchers.IO) {
        val family = getFamily(familyId)
        val headUserId = family.headUserId
        val currentUserId = currentSession?.userId ?: return@withContext false
        if (headUserId == currentUserId) return@withContext false

        val keys = listFamilyDevicePublicKeys(familyId)
        val headKey = keys.firstOrNull { it.userId == headUserId } ?: return@withContext false

        val wrappedGrant = deviceKeyManager.wrapProfileKey(headKey.publicKeyBase64, profileKeyBytes)
        createFamilyInvite(profileId, headUserId, wrappedGrant, role = "caregiver")
        true
    }

    suspend fun syncPendingKeyGrants(
        deviceKeyManager: DeviceKeyManager,
        profileKeyManager: ProfileKeyManager
    ): Int = withContext(Dispatchers.IO) {
        val session = currentSession ?: return@withContext 0
        val pending = listPendingInvites()
        var importedCount = 0
        for (invite in pending) {
            try {
                val accepted = acceptFamilyInvite(invite.inviteId)
                val rawKey = deviceKeyManager.unwrapProfileKey(session.userId, accepted.encryptedKeyGrantBase64)
                profileKeyManager.importProfileKey(accepted.profileId, rawKey)
                importedCount++
            } catch (err: Exception) {
                android.util.Log.e("AccountFamilyRepo", "Failed to sync key grant for invite ${invite.inviteId}", err)
            }
        }
        importedCount
    }

    suspend fun createDoctorShare(profileId: String, encryptedSummaryBase64: String, expiresInMinutes: Int = 15): JSONObject = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("encrypted_summary", encryptedSummaryBase64)
            .put("expires_in_minutes", expiresInMinutes)
        apiRequest("POST", "/api/v1/profiles/${pathSegment(profileId)}/doctor-shares", body).json
    }

    suspend fun revokeDoctorShare(shareId: String): Unit = withContext(Dispatchers.IO) {
        apiRequest("DELETE", "/api/v1/doctor-shares/${pathSegment(shareId)}")
        Unit
    }

    suspend fun redeemDoctorShare(token: String): ByteArray = withContext(Dispatchers.IO) {
        val connection = URL(normalizedApiUrl() + "/public/v1/doctor-shares/redeem").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/octet-stream")
        val body = JSONObject().put("token", token).toString()
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                val err = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.readText().orEmpty()
                throw IllegalStateException("Redemption failed ($code): $err")
            }
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun createCloudRecord(
        profileId: String,
        encryptedMetadataBase64: String,
        ciphertextSha256: String,
        ciphertextSize: Long,
        cryptoVersion: Int = 1
    ): String = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("encrypted_metadata", encryptedMetadataBase64)
            .put("ciphertext_sha256", ciphertextSha256)
            .put("ciphertext_size", ciphertextSize)
            .put("crypto_version", cryptoVersion)
        apiRequest("POST", "/api/v1/profiles/${pathSegment(profileId)}/records", body).json.getString("record_id")
    }

    suspend fun uploadRecordCiphertext(
        recordId: String,
        file: java.io.File,
        sha256Hex: String
    ): Boolean = withContext(Dispatchers.IO) {
        val session = sessionStore.read() ?: throw IllegalStateException("Sign in to sync records")
        val tokenResponse = request(normalizedAuthUrl(), "/api/auth/token", "GET", cookieHeader = session.cookieHeader)
        val token = tokenResponse.json.optString("token")
        val connection = URL(normalizedApiUrl() + "/api/v1/records/${pathSegment(recordId)}/ciphertext").openConnection() as HttpURLConnection
        connection.requestMethod = "PUT"
        connection.connectTimeout = 30_000
        connection.readTimeout = 60_000
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/octet-stream")
        connection.setRequestProperty("X-Ciphertext-SHA256", sha256Hex)
        connection.setFixedLengthStreamingMode(file.length())
        file.inputStream().use { input ->
            connection.outputStream.use { output ->
                input.copyTo(output)
            }
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                val err = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.readText().orEmpty()
                throw IllegalStateException("Upload failed ($code): $err")
            }
            true
        } finally {
            connection.disconnect()
        }
    }

    suspend fun listCloudRecords(profileId: String): List<JSONObject> = withContext(Dispatchers.IO) {
        apiRequest("GET", "/api/v1/profiles/${pathSegment(profileId)}/records").jsonArray()
    }

    suspend fun downloadRecordCiphertext(recordId: String, destinationFile: java.io.File): Boolean = withContext(Dispatchers.IO) {
        val session = sessionStore.read() ?: throw IllegalStateException("Sign in to sync records")
        val tokenResponse = request(normalizedAuthUrl(), "/api/auth/token", "GET", cookieHeader = session.cookieHeader)
        val token = tokenResponse.json.optString("token")
        val connection = URL(normalizedApiUrl() + "/api/v1/records/${pathSegment(recordId)}/ciphertext").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 30_000
        connection.readTimeout = 60_000
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Accept", "application/octet-stream")
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                val err = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.readText().orEmpty()
                throw IllegalStateException("Download failed ($code): $err")
            }
            destinationFile.parentFile?.mkdirs()
            connection.inputStream.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun apiRequest(method: String, path: String, body: JSONObject? = null): HttpResponse {
        val session = sessionStore.read() ?: throw IllegalStateException("Sign in to manage your family")
        val tokenResponse = request(normalizedAuthUrl(), "/api/auth/token", "GET", cookieHeader = session.cookieHeader)
        val refreshedCookie = mergeCookies(session.cookieHeader, tokenResponse.setCookies)
        if (refreshedCookie != session.cookieHeader) sessionStore.write(session.copy(cookieHeader = refreshedCookie))
        val token = tokenResponse.json.optString("token").takeIf(String::isNotBlank)
            ?: throw IllegalStateException("The account server returned no access token")
        return request(normalizedApiUrl(), path, method, body, bearerToken = token)
    }

    private fun normalizedAuthUrl() = validateBaseUrl(savedAuthUrl)
    private fun normalizedApiUrl() = validateBaseUrl(savedApiUrl)

    private fun request(
        baseUrl: String,
        path: String,
        method: String,
        body: JSONObject? = null,
        cookieHeader: String? = null,
        bearerToken: String? = null
    ): HttpResponse {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.setRequestProperty("Accept", "application/json")
        cookieHeader?.takeIf(String::isNotBlank)?.let { connection.setRequestProperty("Cookie", it) }
        bearerToken?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = text.takeIf(String::isNotBlank)?.let { runCatching { JSONObject(it) }.getOrNull() } ?: JSONObject()
            if (code !in 200..299) {
                val detail = json.optString("message").ifBlank { json.optString("detail") }
                throw IllegalStateException(detail.ifBlank { "Server request failed ($code)" })
            }
            return HttpResponse(
                code,
                json,
                connection.headerFields.entries.firstOrNull { it.key.equals("Set-Cookie", ignoreCase = true) }?.value.orEmpty(),
                text
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun mergeCookies(previous: String?, setCookies: List<String>): String {
        val cookies = linkedMapOf<String, String>()
        previous.orEmpty().split(';').map(String::trim).filter(String::isNotBlank).forEach { item ->
            val name = item.substringBefore('=').trim()
            if (name.isNotBlank()) cookies[name] = item.substringAfter('=', "")
        }
        setCookies.forEach { header ->
            val pair = header.substringBefore(';').trim()
            val name = pair.substringBefore('=').trim()
            if (name.isNotBlank()) cookies[name] = pair.substringAfter('=', "")
        }
        return cookies.entries.joinToString("; ") { (name, value) -> "$name=$value" }
    }

    private fun validateBaseUrl(value: String): String {
        val normalized = value.trim().trimEnd('/')
        val url = URL(normalized)
        require(url.protocol == "https" || (url.protocol == "http" && url.host in LOCAL_HTTP_HOSTS)) {
            "Use HTTPS for account and records servers; HTTP is allowed only for loopback development"
        }
        require(url.userInfo == null && url.query == null && url.ref == null && url.path.isNullOrBlank()) {
            "Enter a server origin only, without a path, query, or credentials"
        }
        return normalized
    }

    private fun pathSegment(value: String) = java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun HttpResponse.jsonArray(): List<JSONObject> {
        val array = JSONArray(rawBody)
        return (0 until array.length()).map(array::getJSONObject)
    }

    private data class HttpResponse(val status: Int, val json: JSONObject, val setCookies: List<String>, val rawBody: String = json.toString())

    private companion object {
        val LOCAL_HTTP_HOSTS = setOf("localhost", "127.0.0.1", "::1", "10.0.2.2")
        const val AUTH_URL = "auth_url"
        const val API_URL = "api_url"
    }
}
