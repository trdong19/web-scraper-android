package com.webscraper.data.db

import androidx.room.*
import com.webscraper.data.entity.ScrapedDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScrapedDataDao {
    @Query("SELECT * FROM scraped_data WHERE taskId = :taskId ORDER BY scrapedAt DESC")
    fun getDataByTaskId(taskId: String): Flow<List<ScrapedDataEntity>>

    @Query("SELECT * FROM scraped_data WHERE taskId = :taskId ORDER BY scrapedAt DESC")
    suspend fun getDataByTaskIdOnce(taskId: String): List<ScrapedDataEntity>

    @Query("SELECT COUNT(*) FROM scraped_data WHERE taskId = :taskId")
    suspend fun getDataCount(taskId: String): Int

    @Query("SELECT COUNT(*) FROM scraped_data")
    suspend fun getTotalDataCount(): Int

    @Query("SELECT COUNT(DISTINCT taskId) FROM scraped_data")
    suspend fun getTaskCountWithData(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertData(data: ScrapedDataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataList(dataList: List<ScrapedDataEntity>)

    @Query("DELETE FROM scraped_data WHERE taskId = :taskId")
    suspend fun deleteDataByTaskId(taskId: String)

    @Query("DELETE FROM scraped_data WHERE id = :id")
    suspend fun deleteDataById(id: String)
}
