package com.swasthai.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.swasthai.app.data.local.dao.ConversationDao
import com.swasthai.app.data.local.dao.DocumentDao
import com.swasthai.app.data.local.dao.MeasurementDao
import com.swasthai.app.data.local.dao.RAGDao
import com.swasthai.app.data.local.entity.ChunkEmbeddingEntity
import com.swasthai.app.data.local.entity.ConversationEntity
import com.swasthai.app.data.local.entity.DocumentChunkEntity
import com.swasthai.app.data.local.entity.DocumentEntity
import com.swasthai.app.data.local.entity.MessageEntity
import com.swasthai.app.data.local.entity.StructuredMeasurementEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        DocumentEntity::class,
        DocumentChunkEntity::class,
        ChunkEmbeddingEntity::class,
        StructuredMeasurementEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SwasthDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun documentDao(): DocumentDao
    abstract fun ragDao(): RAGDao
    abstract fun measurementDao(): MeasurementDao

    companion object {
        @Volatile
        private var INSTANCE: SwasthDatabase? = null

        fun getInstance(context: Context): SwasthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SwasthDatabase::class.java,
                    "swasth_ai_local.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
