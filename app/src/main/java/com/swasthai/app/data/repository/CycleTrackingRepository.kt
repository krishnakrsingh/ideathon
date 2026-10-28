package com.swasthai.app.data.repository

import com.swasthai.app.data.local.dao.CycleLogDao
import com.swasthai.app.data.local.entity.CycleLogEntity
import com.swasthai.app.security.EncryptedPayloadEnvelope
import com.swasthai.app.security.ProfileKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.roundToInt

enum class MenstrualFlow { NONE, LIGHT, MEDIUM, HEAVY }

data class CycleLog(
    val id: Long,
    val date: LocalDate,
    val flow: MenstrualFlow,
    val isPeriodStart: Boolean,
    val symptoms: Set<String>
)

data class CycleTrackerSummary(
    val recentLogs: List<CycleLog> = emptyList(),
    val nextPeriodEstimate: LocalDate? = null,
    val intervalsUsed: Int = 0
)

class CycleTrackingRepository(
    private val cycleLogDao: CycleLogDao,
    private val profileKeyManager: ProfileKeyManager,
    private val personProfileRepository: PersonProfileRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeActiveSummary(): Flow<CycleTrackerSummary> =
        personProfileRepository.activeProfileId.flatMapLatest { profileId ->
            cycleLogDao.observeForProfile(profileId).map { rows -> summarize(rows.map { decode(it) }) }
        }

    suspend fun logDay(date: LocalDate, flow: MenstrualFlow, isPeriodStart: Boolean, symptoms: Set<String>) = withContext(Dispatchers.IO) {
        require(!date.isAfter(LocalDate.now())) { "Cycle logs cannot be in the future" }
        require(!isPeriodStart || flow != MenstrualFlow.NONE) { "A period start needs a flow level" }
        require(symptoms.all { it in SYMPTOM_TAGS }) { "Choose symptoms from the list" }
        val profileId = personProfileRepository.activeProfileId.value
        val existingRow = cycleLogDao.getForProfile(profileId).firstOrNull { decode(it).date == date }
        val payload = JSONObject()
            .put("date", date.toEpochDay())
            .put("flow", flow.name)
            .put("periodStart", isPeriodStart)
            .put("symptoms", JSONArray(symptoms.sorted()))
            .toString()
            .toByteArray(Charsets.UTF_8)
        val encrypted = EncryptedPayloadEnvelope.encrypt(
            payload,
            profileKeyManager.getOrCreateProfileKey(profileId),
            aad(profileId)
        )
        cycleLogDao.insert(
            CycleLogEntity(
                id = existingRow?.id ?: 0,
                profileId = profileId,
                encryptedPayload = encrypted,
                createdAt = existingRow?.createdAt ?: System.currentTimeMillis()
            )
        )
    }

    private fun decode(row: CycleLogEntity): CycleLog {
        val plaintext = EncryptedPayloadEnvelope.decrypt(
            row.encryptedPayload,
            profileKeyManager.getOrCreateProfileKey(row.profileId),
            aad(row.profileId)
        )
        val json = JSONObject(plaintext.toString(Charsets.UTF_8))
        val symptoms = json.getJSONArray("symptoms")
        return CycleLog(
            id = row.id,
            date = LocalDate.ofEpochDay(json.getLong("date")),
            flow = MenstrualFlow.valueOf(json.getString("flow")),
            isPeriodStart = json.getBoolean("periodStart"),
            symptoms = buildSet { for (index in 0 until symptoms.length()) add(symptoms.getString(index)) }
        )
    }

    private fun summarize(logs: List<CycleLog>): CycleTrackerSummary {
        val starts = logs.filter { it.isPeriodStart }.map { it.date }.distinct().sorted()
        val intervals = starts.zipWithNext { first, second -> java.time.temporal.ChronoUnit.DAYS.between(first, second).toInt() }
            .filter { it > 0 }
            .takeLast(4)
        val averageInterval = intervals.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
        val estimate = if (averageInterval != null) starts.last().plusDays(averageInterval.toLong()) else null
        return CycleTrackerSummary(
            recentLogs = logs.sortedByDescending { it.date }.take(14),
            nextPeriodEstimate = estimate,
            intervalsUsed = intervals.size
        )
    }

    private fun aad(profileId: String) = "swasthai-cycle-log-v1:$profileId".toByteArray(Charsets.UTF_8)

    companion object {
        val SYMPTOM_TAGS = setOf("cramps", "headache", "bloating", "fatigue", "mood changes")
    }
}
