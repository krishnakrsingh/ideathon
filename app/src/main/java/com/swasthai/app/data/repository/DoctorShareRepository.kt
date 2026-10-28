package com.swasthai.app.data.repository

import android.util.Base64
import com.swasthai.app.data.local.dao.DocumentDao
import com.swasthai.app.data.local.dao.MeasurementDao
import com.swasthai.app.data.local.dao.MedicationReminderDao
import com.swasthai.app.security.EncryptedPayloadEnvelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

data class BiomarkerSummaryItem(
    val name: String,
    val value: Double,
    val unit: String,
    val referenceLow: Double?,
    val referenceHigh: Double?,
    val isOutOfRange: Boolean,
    val testDate: Long
)

data class MedicationSummaryItem(
    val medicationName: String,
    val instructions: String,
    val hour: Int,
    val minute: Int
)

data class DocumentSummaryItem(
    val fileName: String,
    val reportDate: Long?,
    val pageCount: Int
)

data class DoctorClinicalSummary(
    val patientName: String,
    val relationship: String,
    val generatedAt: Long,
    val validUntil: Long,
    val biomarkers: List<BiomarkerSummaryItem>,
    val medications: List<MedicationSummaryItem>,
    val documents: List<DocumentSummaryItem>,
    val clinicalNotes: String = ""
)

data class CreatedDoctorShare(
    val shareId: String,
    val token: String,
    val qrPayloadUrl: String,
    val expiresAt: Long
)

class DoctorShareRepository(
    private val accountFamilyRepository: AccountFamilyRepository,
    private val personProfileRepository: PersonProfileRepository,
    private val measurementDao: MeasurementDao,
    private val medicationReminderDao: MedicationReminderDao,
    private val documentDao: DocumentDao
) {

    suspend fun generateClinicalSummary(
        profileId: String? = null,
        expiresInMinutes: Int = 15,
        clinicalNotes: String = ""
    ): CreatedDoctorShare = withContext(Dispatchers.IO) {
        val targetProfileId = profileId ?: personProfileRepository.activeProfileId.value
        val activeProfile = personProfileRepository.getProfile(targetProfileId)
            ?: throw IllegalStateException("Active profile not found")

        val measurements = measurementDao.getMeasurementsForProfileSync(targetProfileId)
            .sortedByDescending { it.testDate }
            .take(20)
            .map {
                BiomarkerSummaryItem(
                    name = it.biomarkerName,
                    value = it.numericValue,
                    unit = it.unit,
                    referenceLow = it.referenceLow,
                    referenceHigh = it.referenceHigh,
                    isOutOfRange = it.isOutOfRange,
                    testDate = it.testDate
                )
            }

        val medications = medicationReminderDao.getRemindersForProfileSync(targetProfileId)
            .filter { it.enabled }
            .map {
                MedicationSummaryItem(
                    medicationName = it.medicationName,
                    instructions = it.instructions,
                    hour = it.hour,
                    minute = it.minute
                )
            }

        val documents = documentDao.getAllDocumentsList(targetProfileId)
            .take(10)
            .map {
                DocumentSummaryItem(
                    fileName = it.fileName,
                    reportDate = it.reportDate ?: it.importDate,
                    pageCount = it.pageCount
                )
            }

        val now = System.currentTimeMillis()
        val validUntil = now + (expiresInMinutes * 60 * 1000L)

        val summary = DoctorClinicalSummary(
            patientName = activeProfile.displayName,
            relationship = activeProfile.relationship,
            generatedAt = now,
            validUntil = validUntil,
            biomarkers = measurements,
            medications = medications,
            documents = documents,
            clinicalNotes = clinicalNotes
        )

        val summaryJson = serializeSummary(summary)
        val rawKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val secretKey = SecretKeySpec(rawKey, "AES")

        val envelope = EncryptedPayloadEnvelope.encrypt(
            summaryJson.toString().toByteArray(Charsets.UTF_8),
            secretKey,
            AAD_DOCTOR_SHARE
        )
        val base64Envelope = Base64.encodeToString(envelope, Base64.NO_WRAP)

        val response = accountFamilyRepository.createDoctorShare(targetProfileId, base64Envelope, expiresInMinutes)
        val shareId = response.getString("share_id")
        val token = response.getString("token")
        val expiresAt = response.getLong("expires_at")

        val keyUrlSafe = Base64.encodeToString(rawKey, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val origin = accountFamilyRepository.savedApiUrl.ifBlank { "http://10.0.2.2:8765" }
        val qrUrl = "$origin/doctor#share=$shareId&token=$token&key=$keyUrlSafe"

        CreatedDoctorShare(
            shareId = shareId,
            token = token,
            qrPayloadUrl = qrUrl,
            expiresAt = expiresAt
        )
    }

    suspend fun redeemAndDecryptShare(token: String, keyUrlSafe: String): DoctorClinicalSummary = withContext(Dispatchers.IO) {
        val ciphertextBytes = accountFamilyRepository.redeemDoctorShare(token.trim())
        val rawKey = Base64.decode(keyUrlSafe.trim(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        require(rawKey.size == 32) { "Invalid one-time doctor decryption key" }

        val plaintextBytes = EncryptedPayloadEnvelope.decrypt(
            ciphertextBytes,
            SecretKeySpec(rawKey, "AES"),
            AAD_DOCTOR_SHARE
        )
        val json = JSONObject(plaintextBytes.toString(Charsets.UTF_8))
        deserializeSummary(json)
    }

    suspend fun revokeShare(shareId: String) = withContext(Dispatchers.IO) {
        accountFamilyRepository.revokeDoctorShare(shareId)
    }

    private fun serializeSummary(summary: DoctorClinicalSummary): JSONObject {
        val root = JSONObject()
        root.put("patient_name", summary.patientName)
        root.put("relationship", summary.relationship)
        root.put("generated_at", summary.generatedAt)
        root.put("valid_until", summary.validUntil)
        root.put("clinical_notes", summary.clinicalNotes)

        val bioArray = JSONArray()
        summary.biomarkers.forEach { b ->
            val obj = JSONObject()
                .put("name", b.name)
                .put("value", b.value)
                .put("unit", b.unit)
                .put("is_out_of_range", b.isOutOfRange)
                .put("test_date", b.testDate)
            b.referenceLow?.let { obj.put("ref_low", it) }
            b.referenceHigh?.let { obj.put("ref_high", it) }
            bioArray.put(obj)
        }
        root.put("biomarkers", bioArray)

        val medArray = JSONArray()
        summary.medications.forEach { m ->
            val obj = JSONObject()
                .put("name", m.medicationName)
                .put("instructions", m.instructions)
                .put("hour", m.hour)
                .put("minute", m.minute)
            medArray.put(obj)
        }
        root.put("medications", medArray)

        val docArray = JSONArray()
        summary.documents.forEach { d ->
            val obj = JSONObject()
                .put("file_name", d.fileName)
                .put("report_date", d.reportDate ?: 0L)
                .put("page_count", d.pageCount)
            docArray.put(obj)
        }
        root.put("documents", docArray)

        return root
    }

    private fun deserializeSummary(root: JSONObject): DoctorClinicalSummary {
        val bioList = mutableListOf<BiomarkerSummaryItem>()
        val bioArray = root.optJSONArray("biomarkers") ?: JSONArray()
        for (i in 0 until bioArray.length()) {
            val obj = bioArray.getJSONObject(i)
            bioList.add(
                BiomarkerSummaryItem(
                    name = obj.getString("name"),
                    value = obj.getDouble("value"),
                    unit = obj.getString("unit"),
                    referenceLow = if (obj.has("ref_low")) obj.getDouble("ref_low") else null,
                    referenceHigh = if (obj.has("ref_high")) obj.getDouble("ref_high") else null,
                    isOutOfRange = obj.optBoolean("is_out_of_range", false),
                    testDate = obj.getLong("test_date")
                )
            )
        }

        val medList = mutableListOf<MedicationSummaryItem>()
        val medArray = root.optJSONArray("medications") ?: JSONArray()
        for (i in 0 until medArray.length()) {
            val obj = medArray.getJSONObject(i)
            medList.add(
                MedicationSummaryItem(
                    medicationName = obj.getString("name"),
                    instructions = obj.getString("instructions"),
                    hour = obj.getInt("hour"),
                    minute = obj.getInt("minute")
                )
            )
        }

        val docList = mutableListOf<DocumentSummaryItem>()
        val docArray = root.optJSONArray("documents") ?: JSONArray()
        for (i in 0 until docArray.length()) {
            val obj = docArray.getJSONObject(i)
            docList.add(
                DocumentSummaryItem(
                    fileName = obj.getString("file_name"),
                    reportDate = obj.optLong("report_date").takeIf { it > 0 },
                    pageCount = obj.optInt("page_count", 1)
                )
            )
        }

        return DoctorClinicalSummary(
            patientName = root.getString("patient_name"),
            relationship = root.getString("relationship"),
            generatedAt = root.getLong("generated_at"),
            validUntil = root.getLong("valid_until"),
            biomarkers = bioList,
            medications = medList,
            documents = docList,
            clinicalNotes = root.optString("clinical_notes", "")
        )
    }

    companion object {
        private val AAD_DOCTOR_SHARE = "doctor_share".toByteArray(Charsets.UTF_8)
    }
}
