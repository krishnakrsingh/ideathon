package com.swasthai.app

import android.app.Application
import com.swasthai.app.ai.EmbeddingEngine
import com.swasthai.app.ai.LfmManager
import com.swasthai.app.ai.RAGPipeline
import com.swasthai.app.data.local.SwasthDatabase
import com.swasthai.app.data.repository.ChatRepository
import com.swasthai.app.data.repository.DocumentRepository
import com.swasthai.app.data.repository.HistoryRepository
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

    lateinit var lfmManager: LfmManager
        private set

    lateinit var ragPipeline: RAGPipeline
        private set

    lateinit var documentRepository: DocumentRepository
        private set

    lateinit var chatRepository: ChatRepository
        private set

    lateinit var historyRepository: HistoryRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Database
        database = SwasthDatabase.getInstance(this)

        // 2. AI Runtimes
        lfmManager = LfmManager(this)
        embeddingEngine = EmbeddingEngine(lfmManager = lfmManager)

        // 3. RAG Pipeline
        ragPipeline = RAGPipeline(
            documentDao = database.documentDao(),
            ragDao = database.ragDao(),
            measurementDao = database.measurementDao(),
            embeddingEngine = embeddingEngine
        )

        // 4. Repositories
        documentRepository = DocumentRepository(
            context = this,
            documentDao = database.documentDao(),
            ragDao = database.ragDao(),
            measurementDao = database.measurementDao(),
            embeddingEngine = embeddingEngine
        )

        chatRepository = ChatRepository(
            conversationDao = database.conversationDao(),
            ragPipeline = ragPipeline,
            lfmManager = lfmManager,
            measurementDao = database.measurementDao(),
            documentDao = database.documentDao()
        )

        historyRepository = HistoryRepository(
            documentDao = database.documentDao(),
            measurementDao = database.measurementDao()
        )

        // 5. Background non-blocking check for local Liquid AI model files
        applicationScope.launch {
            val detectedGen = lfmManager.findAvailableGenerativeModel()
            if (detectedGen != null) {
                lfmManager.initialize(detectedGen.absolutePath)
            }
        }
    }
}
