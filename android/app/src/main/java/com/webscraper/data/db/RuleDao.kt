package com.webscraper.data.db

import androidx.room.*
import com.webscraper.data.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules WHERE taskId = :taskId")
    fun getRulesByTaskId(taskId: String): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE taskId = :taskId")
    suspend fun getRulesByTaskIdOnce(taskId: String): List<RuleEntity>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getRuleById(id: String): RuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity)

    @Update
    suspend fun updateRule(rule: RuleEntity)

    @Delete
    suspend fun deleteRule(rule: RuleEntity)

    @Query("DELETE FROM rules WHERE taskId = :taskId")
    suspend fun deleteRulesByTaskId(taskId: String)
}
