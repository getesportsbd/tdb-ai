package com.aistudio.tdbassistant.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatMessageEntity::class,
        MemoryEntity::class,
        DailyGoalEntity::class,
        ActivityLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TdbDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun dailyGoalDao(): DailyGoalDao
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        @Volatile
        private var instance: TdbDatabase? = null

        fun getInstance(context: Context): TdbDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TdbDatabase::class.java,
                    "tdb_personal_ai.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
