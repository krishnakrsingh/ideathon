package com.swasthai.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.swasthai.app.data.local.entity.StructuredMeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {

    @Query("SELECT * FROM structured_measurements ORDER BY testDate DESC")
    fun getAllMeasurements(): Flow<List<StructuredMeasurementEntity>>

    @Query("SELECT * FROM structured_measurements ORDER BY testDate DESC")
    suspend fun getAllMeasurementsList(): List<StructuredMeasurementEntity>

    @Query("SELECT * FROM structured_measurements WHERE documentId = :documentId ORDER BY biomarkerName ASC")
    suspend fun getMeasurementsForDocument(documentId: Long): List<StructuredMeasurementEntity>

    @Query("SELECT * FROM structured_measurements WHERE biomarkerName LIKE '%' || :name || '%' ORDER BY testDate ASC")
    suspend fun getMeasurementsByBiomarker(name: String): List<StructuredMeasurementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurements(measurements: List<StructuredMeasurementEntity>): List<Long>

    @Query("DELETE FROM structured_measurements WHERE documentId = :documentId")
    suspend fun deleteMeasurementsForDocument(documentId: Long)
}
