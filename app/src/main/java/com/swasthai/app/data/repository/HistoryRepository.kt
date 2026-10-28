package com.swasthai.app.data.repository

import com.swasthai.app.data.local.dao.DocumentDao
import com.swasthai.app.data.local.dao.MeasurementDao
import com.swasthai.app.domain.model.HealthTimelineItem
import com.swasthai.app.domain.model.StructuredMeasurement
import com.swasthai.app.data.local.entity.DEFAULT_PERSON_PROFILE_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext

class HistoryRepository(
    private val documentDao: DocumentDao,
    private val measurementDao: MeasurementDao,
    private val personProfileRepository: PersonProfileRepository
) {

    /**
     * Builds a chronological health timeline strictly from real user data:
     * - Imported medical reports
     * - Real extracted measurements
     * Strictly avoids fabricated conditions or synthetic health scores.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getHealthTimeline(): Flow<List<HealthTimelineItem>> {
        return personProfileRepository.activeProfileId.flatMapLatest { profileId ->
            combine(
                documentDao.getAllDocuments(profileId),
                measurementDao.getAllMeasurements()
            ) { docs, measurements ->
            val items = mutableListOf<HealthTimelineItem>()
            val docMap = docs.associateBy { it.id }
            val scopedMeasurements = measurements.filter { it.documentId in docMap }

            for (doc in docs) {
                items.add(
                    HealthTimelineItem.ReportImported(
                        documentId = doc.id,
                        documentTitle = doc.fileName,
                        pageCount = doc.pageCount,
                        timestamp = doc.importDate
                    )
                )
            }

            for (m in scopedMeasurements) {
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
                            rawText = m.rawText,
                            canonicalCode = m.canonicalCode,
                            sourcePageNumber = m.sourcePageNumber,
                            reviewStatus = m.reviewStatus,
                            canonicalSystem = m.canonicalSystem,
                            unitSystem = m.unitSystem,
                            unitCode = m.unitCode,
                            valueComparator = m.valueComparator,
                            rawValue = m.rawValue,
                            testDateSource = m.testDateSource,
                            referenceRangeSource = m.referenceRangeSource,
                            extractionMethod = m.extractionMethod,
                            schemaVersion = m.schemaVersion
                        ),
                        documentTitle = doc?.fileName ?: "Saved Report",
                        timestamp = m.testDate
                    )
                )
            }

            items.sortedByDescending { it.timestamp }
            }
        }
    }

    suspend fun getBiomarkerHistory(
        biomarkerName: String,
        profileId: String = personProfileRepository.activeProfileId.value
    ): List<StructuredMeasurement> = withContext(Dispatchers.IO) {
        val allowedDocumentIds = documentDao.getAllDocumentsList(profileId).map { it.id }.toSet()
        measurementDao.getMeasurementsByBiomarker(biomarkerName).filter { it.documentId in allowedDocumentIds }.map { m ->
            StructuredMeasurement(
                id = m.id,
                documentId = m.documentId,
                biomarkerName = m.biomarkerName,
                numericValue = m.numericValue,
                unit = m.unit,
                referenceLow = m.referenceLow,
                referenceHigh = m.referenceHigh,
                testDate = m.testDate,
                rawText = m.rawText,
                canonicalCode = m.canonicalCode,
                sourcePageNumber = m.sourcePageNumber,
                reviewStatus = m.reviewStatus,
                canonicalSystem = m.canonicalSystem,
                unitSystem = m.unitSystem,
                unitCode = m.unitCode,
                valueComparator = m.valueComparator,
                rawValue = m.rawValue,
                testDateSource = m.testDateSource,
                referenceRangeSource = m.referenceRangeSource,
                extractionMethod = m.extractionMethod,
                schemaVersion = m.schemaVersion
            )
        }
    }
}
