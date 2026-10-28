package com.swasthai.app

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.swasthai.app.ai.EmbeddingEngine
import com.swasthai.app.ai.Gemma4ToolCallingModel
import com.swasthai.app.ai.GemmaModelArtifactManager
import com.swasthai.app.ai.MedicalRagOrchestrator
import com.swasthai.app.ai.RAGPipeline
import com.swasthai.app.security.ProfileKeyManager
import com.swasthai.app.security.DeviceKeyManager
import com.swasthai.app.data.local.SwasthDatabase
import com.swasthai.app.data.local.entity.DEFAULT_PERSON_PROFILE_ID
import com.swasthai.app.data.local.entity.PersonProfileEntity
import com.swasthai.app.data.repository.ChatRepository
import com.swasthai.app.data.repository.DocumentRepository
import com.swasthai.app.data.repository.HistoryRepository
import com.swasthai.app.data.repository.PersonProfileRepository
import com.swasthai.app.data.repository.MedicationReminderRepository
import com.swasthai.app.data.repository.CycleTrackingRepository
import com.swasthai.app.data.repository.AccountFamilyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SwasthApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: SwasthDatabase
        private set

    lateinit var embeddingEngine: EmbeddingEngine
        private set

    lateinit var ragPipeline: RAGPipeline
        private set

    lateinit var medicalRagOrchestrator: MedicalRagOrchestrator
        private set

    lateinit var gemma4ToolCallingModel: Gemma4ToolCallingModel
    lateinit var gemmaModelArtifactManager: GemmaModelArtifactManager
        private set

    lateinit var documentRepository: DocumentRepository
        private set

    lateinit var profileKeyManager: ProfileKeyManager
        private set

    lateinit var personProfileRepository: PersonProfileRepository
        private set

    lateinit var chatRepository: ChatRepository
        private set

    lateinit var historyRepository: HistoryRepository
        private set

    lateinit var medicationReminderRepository: MedicationReminderRepository
        private set

    lateinit var cycleTrackingRepository: CycleTrackingRepository
        private set

    lateinit var accountFamilyRepository: AccountFamilyRepository
        private set

    lateinit var deviceKeyManager: DeviceKeyManager
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)

        // 1. Database
        database = SwasthDatabase.getInstance(this)

        // 2. AI Runtimes
        embeddingEngine = EmbeddingEngine(this)
        profileKeyManager = ProfileKeyManager(this)
        personProfileRepository = PersonProfileRepository(this, database.personProfileDao())
        medicationReminderRepository = MedicationReminderRepository(
            this,
            database.medicationReminderDao(),
            personProfileRepository
        )
        cycleTrackingRepository = CycleTrackingRepository(
            database.cycleLogDao(), profileKeyManager, personProfileRepository
        )
        accountFamilyRepository = AccountFamilyRepository(this)
        deviceKeyManager = DeviceKeyManager(this)

        // 3. RAG Pipeline
        ragPipeline = RAGPipeline(
            documentDao = database.documentDao(),
            ragDao = database.ragDao(),
            measurementDao = database.measurementDao(),
            embeddingEngine = embeddingEngine
        )

        gemma4ToolCallingModel = Gemma4ToolCallingModel(this)
        gemmaModelArtifactManager = GemmaModelArtifactManager(this)
        medicalRagOrchestrator = MedicalRagOrchestrator(
            ragPipeline = ragPipeline,
            toolCallingModel = gemma4ToolCallingModel
        )

        // 4. Repositories
        documentRepository = DocumentRepository(
            context = this,
            documentDao = database.documentDao(),
            ragDao = database.ragDao(),
            measurementDao = database.measurementDao(),
            embeddingEngine = embeddingEngine,
            profileKeyManager = profileKeyManager,
            personProfileRepository = personProfileRepository
        )

        chatRepository = ChatRepository(
            conversationDao = database.conversationDao(),
            appContext = this,
            medicalRagOrchestrator = medicalRagOrchestrator,
            personProfileRepository = personProfileRepository
        )

        historyRepository = HistoryRepository(
            documentDao = database.documentDao(),
            measurementDao = database.measurementDao(),
            personProfileRepository = personProfileRepository
        )

        val processStartedAt = System.currentTimeMillis()

        // 5. Background non-blocking initialization of the single chat/tool model
        applicationScope.launch {
            database.personProfileDao().insertIfMissing(
                PersonProfileEntity(
                    id = DEFAULT_PERSON_PROFILE_ID,
                    displayName = "Me",
                    relationship = "self",
                    isOwner = true
                )
            )
            database.conversationDao().interruptUnfinishedTurns(System.currentTimeMillis(), processStartedAt)
            try {
                documentRepository.encryptExistingPlaintextReports()
            } catch (error: Exception) {
                android.util.Log.e("SwasthApplication", "Local report encryption migration failed", error)
            }
            gemma4ToolCallingModel.findAvailableModelFile()?.let { model ->
                gemma4ToolCallingModel.initialize(model.absolutePath)
            }
            embeddingEngine.findAvailableModelFile()?.let { model ->
                if (embeddingEngine.initialize(model.absolutePath)) {
                    documentRepository.indexMissingEmbeddings()
                }
            }
        }
    }
}
