package com.swasthai.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Cycle details are encrypted as one payload; only the owning profile ID is stored in clear text. */
@Entity(tableName = "cycle_logs", indices = [Index(value = ["profileId"])])
data class CycleLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: String,
    val encryptedPayload: ByteArray,
    val createdAt: Long = System.currentTimeMillis()
)
