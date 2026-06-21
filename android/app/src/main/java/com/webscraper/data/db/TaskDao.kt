package com.webscraper.data.db

import androidx.room.*
import com.webscraper.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE scheduleType != 'none'")
    suspend fun getScheduledTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("UPDATE tasks SET status = :status, updatedAt = :time WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET totalItems = :count, status = :status, updatedAt = :time WHERE id = :id")
    suspend fun updateResult(id: String, count: Int, status: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET errorMessage = :msg, status = :status, updatedAt = :time WHERE id = :id")
    suspend fun updateError(id: String, msg: String, status: String = "failed", time: Long = System.currentTimeMillis())
}
