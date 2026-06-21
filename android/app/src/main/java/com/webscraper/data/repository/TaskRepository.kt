package com.webscraper.data.repository

import com.webscraper.data.db.TaskDao
import com.webscraper.data.db.RuleDao
import com.webscraper.data.db.ScrapedDataDao
import com.webscraper.data.entity.TaskEntity
import com.webscraper.data.entity.RuleEntity
import com.webscraper.data.entity.ScrapedDataEntity
import com.webscraper.data.model.FieldConfig
import com.webscraper.data.model.TaskStats
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TaskRepository(
    private val taskDao: TaskDao,
    private val ruleDao: RuleDao,
    private val dataDao: ScrapedDataDao
) {
    private val gson = Gson()

    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    suspend fun getTask(id: String): TaskEntity? = taskDao.getTaskById(id)

    suspend fun getScheduledTasks(): List<TaskEntity> = taskDao.getScheduledTasks()

    suspend fun createTask(
        name: String, url: String, useJsRender: Boolean = false,
        waitSeconds: Int = 3, scrollToBottom: Boolean = false
    ): TaskEntity {
        val task = TaskEntity(
            id = UUID.randomUUID().toString(), name = name, url = url,
            useJsRender = useJsRender, waitSeconds = waitSeconds, scrollToBottom = scrollToBottom
        )
        taskDao.insertTask(task)
        return task
    }

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)
    suspend fun deleteTask(id: String) {
        dataDao.deleteDataByTaskId(id); ruleDao.deleteRulesByTaskId(id); taskDao.deleteTaskById(id)
    }

    suspend fun updateTaskStatus(id: String, status: String) = taskDao.updateStatus(id, status)
    suspend fun updateTaskResult(id: String, count: Int, status: String) = taskDao.updateResult(id, count, status)
    suspend fun updateTaskError(id: String, msg: String) = taskDao.updateError(id, msg)

    fun getRulesByTaskId(taskId: String): Flow<List<RuleEntity>> = ruleDao.getRulesByTaskId(taskId)
    suspend fun getRulesByTaskIdOnce(taskId: String): List<RuleEntity> = ruleDao.getRulesByTaskIdOnce(taskId)

    suspend fun createRule(taskId: String, name: String, listSelector: String, selectorType: String = "css", fields: List<FieldConfig>): RuleEntity {
        val rule = RuleEntity(id = UUID.randomUUID().toString(), taskId = taskId, name = name, listSelector = listSelector, selectorType = selectorType, fieldsJson = gson.toJson(fields))
        ruleDao.insertRule(rule)
        return rule
    }

    suspend fun deleteRule(id: String) { ruleDao.getRuleById(id)?.let { ruleDao.deleteRule(it) } }

    fun parseFields(fieldsJson: String): List<FieldConfig> {
        return try {
            val type = object : TypeToken<List<FieldConfig>>() {}.type
            gson.fromJson(fieldsJson, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    fun getDataByTaskId(taskId: String): Flow<List<ScrapedDataEntity>> = dataDao.getDataByTaskId(taskId)
    suspend fun getDataByTaskIdOnce(taskId: String): List<ScrapedDataEntity> = dataDao.getDataByTaskIdOnce(taskId)

    suspend fun saveScrapedData(taskId: String, dataList: List<Map<String, String?>>) {
        val entities = dataList.map { ScrapedDataEntity(id = UUID.randomUUID().toString(), taskId = taskId, dataJson = gson.toJson(it)) }
        dataDao.insertDataList(entities)
    }

    suspend fun clearDataForTask(taskId: String) = dataDao.deleteDataByTaskId(taskId)

    suspend fun getTaskStats(): TaskStats {
        return TaskStats(totalDataItems = dataDao.getTotalDataCount(), tasksWithData = dataDao.getTaskCountWithData())
    }
}
