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
    val rawText: String
) {
    val isOutOfRange: Boolean
        get() {
            val low = referenceLow
            val high = referenceHigh
            return (low != null && numericValue < low) || (high != null && numericValue > high)
        }
}

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
