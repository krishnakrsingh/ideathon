package com.swasthai.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.swasthai.app.data.local.entity.PersonProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonProfileDao {
    @Query("SELECT * FROM person_profiles ORDER BY isOwner DESC, createdAt ASC")
    fun observeProfiles(): Flow<List<PersonProfileEntity>>

    @Query("SELECT * FROM person_profiles WHERE id = :profileId LIMIT 1")
    suspend fun getProfile(profileId: String): PersonProfileEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(profile: PersonProfileEntity)

    @Insert
    suspend fun insert(profile: PersonProfileEntity)

    @Query("DELETE FROM person_profiles WHERE id = :profileId AND isOwner = 0")
    suspend fun deleteFamilyProfile(profileId: String)
}
