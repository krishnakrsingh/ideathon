package com.swasthai.app.data.repository

import com.swasthai.app.data.local.dao.DocumentDao
import com.swasthai.app.data.local.dao.MeasurementDao
import com.swasthai.app.domain.model.HealthTimelineItem
import com.swasthai.app.domain.model.StructuredMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class HistoryRepository(
    private val documentDao: DocumentDao,
    private val measurementDao: MeasurementDao
) {

    /**
     * Builds a chronological health timeline strictly from real user data:
     * - Imported medical reports
     * - Real extracted measurements
     * Strictly avoids fabricated conditions or synthetic health scores.
     */
    fun getHealthTimeline(): Flow<List<HealthTimelineItem>> {
        return combine(
            documentDao.getAllDocuments(),
            measurementDao.getAllMeasurements()
        ) { docs, measurements ->
            val items = mutableListOf<HealthTimelineItem>()
            val docMap = docs.associateBy { it.id }

            for (doc in docs) {
                items.add(
                    HealthTimelineItem.ReportImported(
                        documentId = doc.id,
                        documentTitle = doc.fileName,
                        pageCount = doc.pageCount,
                        timestamp = doc.reportDate ?: doc.importDate
                    )
                )
            }

            for (m in measurements) {
                val doc = docMap[m.documentId]
                items.add(
                    HealthTimelineItem.MeasurementRecorded(
                        measurement = StructuredMeasurement(
                            id = m.id,
                            documentId = m.documentId,
                            biomarkerName = m.biomarkerName,
                            numericValue = m.numericValue,
                            unit = m.unit,
                            referenceLow = m.referenceLow,
                            referenceHigh = m.referenceHigh,
                            testDate = m.testDate,
                            rawText = m.rawText
                        ),
                        documentTitle = doc?.fileName ?: "Saved Report",
                        timestamp = m.testDate
                    )
                )
            }

            items.sortedByDescending { it.timestamp }
        }
    }

    suspend fun getBiomarkerHistory(biomarkerName: String): List<StructuredMeasurement> = withContext(Dispatchers.IO) {
        measurementDao.getMeasurementsByBiomarker(biomarkerName).map { m ->
            StructuredMeasurement(
                id = m.id,
                documentId = m.documentId,
                biomarkerName = m.biomarkerName,
                numericValue = m.numericValue,
                unit = m.unit,
                referenceLow = m.referenceLow,
                referenceHigh = m.referenceHigh,
                testDate = m.testDate,
                rawText = m.rawText
            )
        }
    }
}
