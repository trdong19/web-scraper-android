package com.webscraper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.webscraper.data.db.AppDatabase
import com.webscraper.data.entity.TaskEntity
import com.webscraper.data.model.TaskStats
import com.webscraper.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class StatsUiState(
    val stats: TaskStats = TaskStats(),
    val recentTasks: List<TaskEntity> = emptyList(),
    val isLoading: Boolean = false
)

class DataStatsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val repo = TaskRepository(db.taskDao(), db.ruleDao(), db.scrapedDataDao())

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val stats = repo.getTaskStats()
            _uiState.value = _uiState.value.copy(
                stats = stats,
                isLoading = false
            )
        }
    }
}
