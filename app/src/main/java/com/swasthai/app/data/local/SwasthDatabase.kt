package com.swasthai.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.swasthai.app.data.local.dao.ConversationDao
import com.swasthai.app.data.local.dao.DocumentDao
import com.swasthai.app.data.local.dao.MeasurementDao
import com.swasthai.app.data.local.dao.RAGDao
import com.swasthai.app.data.local.dao.PersonProfileDao
import com.swasthai.app.data.local.dao.MedicationReminderDao
import com.swasthai.app.data.local.dao.CycleLogDao
import com.swasthai.app.data.local.entity.ChunkEmbeddingEntity
import com.swasthai.app.data.local.entity.ConversationEntity
import com.swasthai.app.data.local.entity.ChatTurnEntity
import com.swasthai.app.data.local.entity.DocumentChunkEntity
import com.swasthai.app.data.local.entity.DocumentChunkFtsEntity
import com.swasthai.app.data.local.entity.DocumentEntity
import com.swasthai.app.data.local.entity.MessageEntity
import com.swasthai.app.data.local.entity.StructuredMeasurementEntity
import com.swasthai.app.data.local.entity.PersonProfileEntity
import com.swasthai.app.data.local.entity.MedicationReminderEntity
import com.swasthai.app.data.local.entity.CycleLogEntity

@Database(
    entities = [
        ConversationEntity::class,
        ChatTurnEntity::class,
        MessageEntity::class,
        DocumentEntity::class,
        DocumentChunkEntity::class,
        DocumentChunkFtsEntity::class,
        ChunkEmbeddingEntity::class,
        StructuredMeasurementEntity::class,
        PersonProfileEntity::class,
        MedicationReminderEntity::class,
        CycleLogEntity::class
    ],
    version = 15,
    exportSchema = false
)
abstract class SwasthDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun documentDao(): DocumentDao
    abstract fun ragDao(): RAGDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun personProfileDao(): PersonProfileDao
    abstract fun medicationReminderDao(): MedicationReminderDao
    abstract fun cycleLogDao(): CycleLogDao

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN turnId TEXT")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS chat_turns (
                        id TEXT NOT NULL PRIMARY KEY,
                        conversationId INTEGER NOT NULL,
                        userMessageId INTEGER NOT NULL,
                        assistantMessageId INTEGER,
                        activeDocumentId INTEGER,
                        scopeMode TEXT NOT NULL,
                        selectedDocIdsJson TEXT NOT NULL,
                        timeRangeFilter TEXT NOT NULL,
                        customStartDate INTEGER,
                        customEndDate INTEGER,
                        includeStructuredRecords INTEGER NOT NULL,
                        responseLanguage TEXT NOT NULL,
                        status TEXT NOT NULL,
                        retrievalMode TEXT,
                        modelName TEXT,
                        citationsJson TEXT NOT NULL,
                        errorMessage TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(conversationId) REFERENCES conversations(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_turns_conversationId ON chat_turns(conversationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_turns_createdAt ON chat_turns(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_turns_status ON chat_turns(status)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_turns ADD COLUMN toolCallsJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE documents ADD COLUMN stableId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE documents ADD COLUMN encryptionContextVersion INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE documents SET stableId = 'legacy-' || lower(hex(randomblob(16))) WHERE stableId = ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_documents_stableId ON documents(stableId)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS person_profiles (id TEXT NOT NULL PRIMARY KEY, displayName TEXT NOT NULL, relationship TEXT NOT NULL, dateOfBirth INTEGER, sexAtBirth TEXT, createdAt INTEGER NOT NULL, isOwner INTEGER NOT NULL)")
                db.execSQL("INSERT OR IGNORE INTO person_profiles (id, displayName, relationship, dateOfBirth, sexAtBirth, createdAt, isOwner) VALUES ('local-default-profile', 'Me', 'self', NULL, NULL, 0, 1)")
                db.execSQL("ALTER TABLE documents ADD COLUMN profileId TEXT NOT NULL DEFAULT 'local-default-profile'")
                db.execSQL("ALTER TABLE conversations ADD COLUMN profileId TEXT NOT NULL DEFAULT 'local-default-profile'")
                db.execSQL("ALTER TABLE chat_turns ADD COLUMN profileId TEXT NOT NULL DEFAULT 'local-default-profile'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_documents_profileId ON documents(profileId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_profileId ON conversations(profileId)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS medication_reminders (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, profileId TEXT NOT NULL, medicationName TEXT NOT NULL, instructions TEXT NOT NULL, hour INTEGER NOT NULL, minute INTEGER NOT NULL, enabled INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_medication_reminders_profileId ON medication_reminders(profileId)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS cycle_logs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, profileId TEXT NOT NULL, encryptedPayload BLOB NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cycle_logs_profileId ON cycle_logs(profileId)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE person_profiles ADD COLUMN avatarSeed TEXT NOT NULL DEFAULT 'avatar-01'")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_documents_profileId")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_documents_profileId_importDate ON documents(profileId, importDate)")
                db.execSQL("DROP INDEX IF EXISTS index_document_chunks_documentId")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_document_chunks_documentId_pageNumber_chunkIndex ON document_chunks(documentId, pageNumber, chunkIndex)")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE structured_measurements ADD COLUMN canonicalCode TEXT")
                db.execSQL("ALTER TABLE structured_measurements ADD COLUMN sourcePageNumber INTEGER")
                db.execSQL("ALTER TABLE structured_measurements ADD COLUMN reviewStatus TEXT NOT NULL DEFAULT 'UNREVIEWED'")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE structured_measurements ADD COLUMN canonicalSystem TEXT")
                db.execSQL("ALTER TABLE structured_measurements ADD COLUMN unitSystem TEXT")
                db.execSQL("ALTER TABLE structured_measurements ADD COLUMN unitCode TEXT")
                db.execSQL("ALTER TABLE structured_measurements ADD COLUMN valueComparator TEXT NOT NULL DEFAULT 'EQ'")
                db.execSQL("ALTER TABLE structured_measurements ADD COLUMN rawValue TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE structured_measurements ADD COLUMN testDateSource TEXT NOT NULL DEFAULT 'UNKNOWN'")
                db.execSQL("ALTER TABLE structured_measurements ADD COLUMN referenceRangeSource TEXT")
                db.execSQL("ALTER TABLE structured_measurements ADD COLUMN extractionMethod TEXT NOT NULL DEFAULT 'legacy-v1'")
                db.execSQL("ALTER TABLE structured_measurements ADD COLUMN schemaVersion INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `document_chunks_fts` USING FTS4(`text` TEXT NOT NULL, tokenize=unicode61, content=`document_chunks`)")
                db.execSQL("INSERT INTO document_chunks_fts(document_chunks_fts) VALUES('rebuild')")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `document_chunks_fts`")
                db.execSQL("CREATE VIRTUAL TABLE `document_chunks_fts` USING FTS4(`text` TEXT NOT NULL, tokenize=unicode61, content=`document_chunks`)")
                db.execSQL("INSERT INTO document_chunks_fts(document_chunks_fts) VALUES('rebuild')")
            }
        }

        @Volatile
        private var INSTANCE: SwasthDatabase? = null

        fun getInstance(context: Context): SwasthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SwasthDatabase::class.java,
                    "swasth_ai_local.db"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
