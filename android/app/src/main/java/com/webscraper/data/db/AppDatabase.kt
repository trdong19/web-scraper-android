package com.webscraper.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.webscraper.data.entity.TaskEntity
import com.webscraper.data.entity.RuleEntity
import com.webscraper.data.entity.ScrapedDataEntity

@Database(
    entities = [TaskEntity::class, RuleEntity::class, ScrapedDataEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun ruleDao(): RuleDao
    abstract fun scrapedDataDao(): ScrapedDataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "web_scraper.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
