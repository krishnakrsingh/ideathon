package com.swasthai.app.domain.model

data class StructuredMeasurement(
    val id: Long = 0,
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
    val reviewStatus: String = "UNREVIEWED",
    val canonicalSystem: String? = null,
    val unitSystem: String? = null,
    val unitCode: String? = null,
    val valueComparator: String = "EQ",
    val rawValue: String = "",
    val testDateSource: String = "UNKNOWN",
    val referenceRangeSource: String? = null,
    val extractionMethod: String = "legacy-v1",
    val schemaVersion: Int = 1
)

sealed class HealthTimelineItem {
    abstract val timestamp: Long

    data class ReportImported(
        val documentId: Long,
        val documentTitle: String,
        val pageCount: Int,
        override val timestamp: Long
    ) : HealthTimelineItem()

    data class MeasurementRecorded(
        val measurement: StructuredMeasurement,
        val documentTitle: String,
        override val timestamp: Long
    ) : HealthTimelineItem()
}
