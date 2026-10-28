package com.swasthai.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.swasthai.app.data.local.entity.CycleLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleLogDao {
    @Query("SELECT * FROM cycle_logs WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun observeForProfile(profileId: String): Flow<List<CycleLogEntity>>

    @Query("SELECT * FROM cycle_logs WHERE profileId = :profileId")
    suspend fun getForProfile(profileId: String): List<CycleLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CycleLogEntity): Long

    @Query("DELETE FROM cycle_logs WHERE id = :id AND profileId = :profileId")
    suspend fun delete(id: Long, profileId: String)
}
