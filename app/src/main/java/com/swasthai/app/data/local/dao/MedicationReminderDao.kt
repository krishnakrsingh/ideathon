package com.swasthai.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.swasthai.app.data.local.entity.MedicationReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationReminderDao {
    @Query("SELECT * FROM medication_reminders WHERE profileId = :profileId ORDER BY hour, minute")
    fun observeForProfile(profileId: String): Flow<List<MedicationReminderEntity>>

    @Query("SELECT * FROM medication_reminders WHERE profileId = :profileId ORDER BY hour, minute")
    suspend fun getRemindersForProfileSync(profileId: String): List<MedicationReminderEntity>

    @Query("SELECT * FROM medication_reminders WHERE enabled = 1")
    suspend fun getEnabledReminders(): List<MedicationReminderEntity>

    @Query("SELECT * FROM medication_reminders WHERE id = :id AND enabled = 1 LIMIT 1")
    suspend fun getEnabledReminder(id: Long): MedicationReminderEntity?

    @Query("SELECT * FROM medication_reminders WHERE id = :id AND profileId = :profileId LIMIT 1")
    suspend fun getForProfile(id: Long, profileId: String): MedicationReminderEntity?

    @Insert
    suspend fun insert(reminder: MedicationReminderEntity): Long

    @Update
    suspend fun update(reminder: MedicationReminderEntity)

    @Query("DELETE FROM medication_reminders WHERE id = :id AND profileId = :profileId")
    suspend fun delete(id: Long, profileId: String)
}
