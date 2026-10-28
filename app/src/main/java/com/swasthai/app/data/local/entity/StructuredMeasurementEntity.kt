package com.swasthai.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

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
    val rawText: String,
    val canonicalCode: String? = null,
    val sourcePageNumber: Int? = null,
    @ColumnInfo(defaultValue = "'UNREVIEWED'") val reviewStatus: String = "UNREVIEWED",
    val canonicalSystem: String? = null,
    val unitSystem: String? = null,
    val unitCode: String? = null,
    @ColumnInfo(defaultValue = "'EQ'") val valueComparator: String = "EQ",
    @ColumnInfo(defaultValue = "''") val rawValue: String = "",
    @ColumnInfo(defaultValue = "'UNKNOWN'") val testDateSource: String = "UNKNOWN",
    val referenceRangeSource: String? = null,
    @ColumnInfo(defaultValue = "'legacy-v1'") val extractionMethod: String = "legacy-v1",
    @ColumnInfo(defaultValue = "1") val schemaVersion: Int = 1
)
