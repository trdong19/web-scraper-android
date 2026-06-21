package com.webscraper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.webscraper.data.db.AppDatabase
import com.webscraper.data.entity.TaskEntity
import com.webscraper.data.repository.TaskRepository
import com.webscraper.scheduler.ScrapeScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskListViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val repo = TaskRepository(db.taskDao(), db.ruleDao(), db.scrapedDataDao())

    val tasks: StateFlow<List<TaskEntity>> = repo.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun createTask(name: String, url: String, useJsRender: Boolean) {
        viewModelScope.launch { repo.createTask(name = name, url = url, useJsRender = useJsRender) }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch { ScrapeScheduler.cancel(getApplication(), taskId); repo.deleteTask(taskId) }
    }

    fun runTask(taskId: String) {
        viewModelScope.launch { repo.updateTaskStatus(taskId, "running"); ScrapeScheduler.runNow(getApplication(), taskId) }
    }
}
