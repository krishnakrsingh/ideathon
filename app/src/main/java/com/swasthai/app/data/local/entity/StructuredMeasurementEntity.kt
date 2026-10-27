package com.swasthai.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "structured_measurements",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["biomarkerName"]),
        Index(value = ["testDate"])
    ]
)
data class StructuredMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val biomarkerName: String,
    val numericValue: Double,
    val unit: String,
    val referenceLow: Double? = null,
    val referenceHigh: Double? = null,
    val testDate: Long,
    val rawText: String
) {
    val isOutOfRange: Boolean
        get() {
            val low = referenceLow
            val high = referenceHigh
            return (low != null && numericValue < low) || (high != null && numericValue > high)
        }
}
