package com.webscraper.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.webscraper.data.db.AppDatabase
import com.webscraper.data.model.FieldConfig
import com.webscraper.data.repository.TaskRepository
import com.webscraper.engine.ScraperEngine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Background worker that executes a scraping task.
 */
class ScrapeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString("task_id") ?: return Result.failure()

        val db = AppDatabase.getInstance(applicationContext)
        val repo = TaskRepository(db.taskDao(), db.ruleDao(), db.scrapedDataDao())

        val task = repo.getTask(taskId) ?: return Result.failure()
        val rules = repo.getRulesByTaskIdOnce(taskId)

        if (rules.isEmpty()) {
            repo.updateTaskError(taskId, "No scraping rules configured")
            return Result.failure()
        }

        return try {
            repo.updateTaskStatus(taskId, "running")

            val engine = ScraperEngine(task.proxyHost, task.proxyPort)
            val gson = Gson()
            val fieldType = object : TypeToken<List<FieldConfig>>() {}.type
            val allItems = mutableListOf<Map<String, String?>>()

            for (rule in rules) {
                val fields: List<FieldConfig> = gson.fromJson(rule.fieldsJson, fieldType) ?: emptyList()

                val result = if (task.useJsRender) {
                    // WebView rendering mode
                    val renderer = com.webscraper.engine.WebViewRenderer(applicationContext)
                    renderer.renderPage(task.url, task.waitSeconds, task.scrollToBottom).map { html ->
                        engine.extractData(html, rule.listSelector, fields, rule.selectorType)
                    }
                } else {
                    // Static mode
                    engine.scrape(task.url, rule.listSelector, fields, rule.selectorType)
                }

                result.fold(
                    onSuccess = { items -> allItems.addAll(items) },
                    onFailure = { e ->
                        repo.updateTaskError(taskId, e.message ?: "Unknown error")
                        return Result.failure()
                    }
                )
            }

            // Clear old data and save new
            repo.clearDataForTask(taskId)
            repo.saveScrapedData(taskId, allItems)
            repo.updateTaskResult(taskId, allItems.size, "completed")

            Result.success()
        } catch (e: Exception) {
            repo.updateTaskError(taskId, e.message ?: "Unknown error")
            Result.failure()
        }
    }
}
