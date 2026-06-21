package com.webscraper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.webscraper.data.db.AppDatabase
import com.webscraper.data.entity.RuleEntity
import com.webscraper.data.entity.ScrapedDataEntity
import com.webscraper.data.entity.TaskEntity
import com.webscraper.data.model.FieldConfig
import com.webscraper.data.repository.TaskRepository
import com.webscraper.engine.ScraperEngine
import com.webscraper.engine.WebViewRenderer
import com.webscraper.export.ExcelExporter
import com.webscraper.export.JsonExporter
import com.webscraper.scheduler.ScrapeScheduler
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class TaskDetailUiState(
    val task: TaskEntity? = null,
    val rules: List<RuleEntity> = emptyList(),
    val dataItems: List<ScrapedDataEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isRunning: Boolean = false,
    val error: String? = null,
    val exportedFile: File? = null
)

class TaskDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val repo = TaskRepository(db.taskDao(), db.ruleDao(), db.scrapedDataDao())
    private val gson = Gson()

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState

    fun loadTask(taskId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val task = repo.getTask(taskId)
            launch { repo.getRulesByTaskId(taskId).collect { _uiState.value = _uiState.value.copy(rules = it) } }
            launch { repo.getDataByTaskId(taskId).collect { _uiState.value = _uiState.value.copy(dataItems = it) } }
            _uiState.value = _uiState.value.copy(task = task, isLoading = false)
        }
    }

    fun runScrape(taskId: String) {
        viewModelScope.launch {
            val task = repo.getTask(taskId) ?: return@launch
            val rules = repo.getRulesByTaskIdOnce(taskId)
            if (rules.isEmpty()) { _uiState.value = _uiState.value.copy(error = "请先配置采集规则"); return@launch }

            _uiState.value = _uiState.value.copy(isRunning = true, error = null)
            repo.updateTaskStatus(taskId, "running")

            try {
                val engine = ScraperEngine(task.proxyHost, task.proxyPort)
                val fieldType = object : TypeToken<List<FieldConfig>>() {}.type
                val allItems = mutableListOf<Map<String, String?>>()

                for (rule in rules) {
                    val fields: List<FieldConfig> = gson.fromJson(rule.fieldsJson, fieldType) ?: emptyList()
                    val result = if (task.useJsRender) {
                        WebViewRenderer(getApplication()).renderPage(task.url, task.waitSeconds, task.scrollToBottom).map { html ->
                            engine.extractData(html, rule.listSelector, fields, rule.selectorType)
                        }
                    } else { engine.scrape(task.url, rule.listSelector, fields, rule.selectorType) }

                    result.fold(
                        onSuccess = { items -> allItems.addAll(items) },
                        onFailure = { e -> repo.updateTaskError(taskId, e.message ?: "Unknown error"); _uiState.value = _uiState.value.copy(isRunning = false, error = e.message); loadTask(taskId); return@launch }
                    )
                }

                repo.clearDataForTask(taskId)
                repo.saveScrapedData(taskId, allItems)
                repo.updateTaskResult(taskId, allItems.size, "completed")
                _uiState.value = _uiState.value.copy(isRunning = false)
                loadTask(taskId)
            } catch (e: Exception) {
                repo.updateTaskError(taskId, e.message ?: "Unknown error")
                _uiState.value = _uiState.value.copy(isRunning = false, error = e.message)
                loadTask(taskId)
            }
        }
    }

    fun addRule(taskId: String, name: String, listSelector: String, selectorType: String, fields: List<FieldConfig>) {
        viewModelScope.launch { repo.createRule(taskId, name, listSelector, selectorType, fields) }
    }

    fun deleteRule(ruleId: String) { viewModelScope.launch { repo.deleteRule(ruleId) } }

    fun exportJson(taskName: String, dataItems: List<ScrapedDataEntity>) {
        viewModelScope.launch {
            JsonExporter.export(getApplication(), taskName, dataItems.map { it.dataJson }).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(exportedFile = it) },
                onFailure = { _uiState.value = _uiState.value.copy(error = "导出失败: ${it.message}") }
            )
        }
    }

    fun exportExcel(taskName: String, dataItems: List<ScrapedDataEntity>) {
        viewModelScope.launch {
            ExcelExporter.export(getApplication(), taskName, dataItems.map { it.dataJson }).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(exportedFile = it) },
                onFailure = { _uiState.value = _uiState.value.copy(error = "导出失败: ${it.message}") }
            )
        }
    }

    fun clearExportedFile() { _uiState.value = _uiState.value.copy(exportedFile = null) }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    fun updateTaskSchedule(taskId: String, scheduleType: String, intervalMinutes: Long, scheduleTime: String?, wifiOnly: Boolean) {
        viewModelScope.launch {
            val task = repo.getTask(taskId) ?: return@launch
            val updated = task.copy(scheduleType = scheduleType, scheduleIntervalMinutes = intervalMinutes, scheduleTime = scheduleTime, wifiOnly = wifiOnly)
            repo.updateTask(updated)
            ScrapeScheduler.schedule(getApplication(), updated)
            loadTask(taskId)
        }
    }
}
