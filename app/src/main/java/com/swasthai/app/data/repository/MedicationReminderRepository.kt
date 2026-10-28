package com.swasthai.app.data.repository

import android.content.Context
import com.swasthai.app.data.local.dao.MedicationReminderDao
import com.swasthai.app.data.local.entity.MedicationReminderEntity
import com.swasthai.app.notifications.MedicationReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

class MedicationReminderRepository(
    private val context: Context,
    private val reminderDao: MedicationReminderDao,
    private val personProfileRepository: PersonProfileRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeActiveProfileReminders(): Flow<List<MedicationReminderEntity>> =
        personProfileRepository.activeProfileId.flatMapLatest(reminderDao::observeForProfile)

    suspend fun createReminder(name: String, instructions: String, hour: Int, minute: Int, targetProfileId: String? = null) {
        val cleanedName = name.trim()
        require(cleanedName.isNotEmpty()) { "Enter a medicine name" }
        require(cleanedName.length <= 100) { "Medicine name is too long" }
        require(instructions.length <= 160) { "Reminder instructions must be under 160 characters" }
        require(hour in 0..23 && minute in 0..59) { "Choose a valid reminder time" }
        val profileId = targetProfileId ?: personProfileRepository.activeProfileId.value
        val reminder = MedicationReminderEntity(
            profileId = profileId,
            medicationName = cleanedName,
            instructions = instructions.trim(),
            hour = hour,
            minute = minute
        )
        val id = reminderDao.insert(reminder)
        MedicationReminderScheduler.scheduleNext(context, reminder.copy(id = id))
    }

    suspend fun deleteReminder(id: Long) {
        val profileId = personProfileRepository.activeProfileId.value
        val reminder = reminderDao.getForProfile(id, profileId) ?: return
        MedicationReminderScheduler.cancel(context, id)
        reminderDao.delete(id, profileId)
    }

    suspend fun setReminderEnabled(id: Long, enabled: Boolean) {
        val profileId = personProfileRepository.activeProfileId.value
        val reminder = reminderDao.getForProfile(id, profileId) ?: return
        val updated = reminder.copy(enabled = enabled)
        reminderDao.update(updated)
        if (enabled) MedicationReminderScheduler.scheduleNext(context, updated)
        else MedicationReminderScheduler.cancel(context, id)
    }
}
