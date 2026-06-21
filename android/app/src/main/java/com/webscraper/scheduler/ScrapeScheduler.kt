package com.webscraper.scheduler

import android.content.Context
import androidx.work.*
import com.webscraper.data.entity.TaskEntity
import java.util.concurrent.TimeUnit

/**
 * Schedules scraping tasks using WorkManager.
 * Supports: one-time, fixed interval, daily at specific time.
 */
object ScrapeScheduler {

    private const val TAG_PREFIX = "scrape_"

    /**
     * Run a task immediately (one-time).
     */
    fun runNow(context: Context, taskId: String) {
        val data = workDataOf("task_id" to taskId)
        val request = OneTimeWorkRequestBuilder<ScrapeWorker>()
            .setInputData(data)
            .addTag(TAG_PREFIX + taskId)
            .setConstraints(buildConstraints(null))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "scrape_now_$taskId",
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    /**
     * Schedule a task with flexible options.
     */
    fun schedule(context: Context, task: TaskEntity) {
        cancel(context, task.id)

        when (task.scheduleType) {
            "interval" -> scheduleInterval(context, task)
            "daily" -> scheduleDaily(context, task)
            // "none" — no scheduling
        }
    }

    /**
     * Schedule with fixed interval (e.g., every 30 minutes).
     */
    private fun scheduleInterval(context: Context, task: TaskEntity) {
        if (task.scheduleIntervalMinutes <= 0) return

        val data = workDataOf("task_id" to task.id)
        val constraints = buildConstraints(task)

        val request = PeriodicWorkRequestBuilder<ScrapeWorker>(
            task.scheduleIntervalMinutes, TimeUnit.MINUTES
        )
            .setInputData(data)
            .addTag(TAG_PREFIX + task.id)
            .setConstraints(constraints)
            .setInitialDelay(task.scheduleIntervalMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "scrape_interval_${task.id}",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
    }

    /**
     * Schedule daily at a specific time (e.g., "08:00").
     * Uses a chain of one-time workers that re-schedule themselves.
     */
    private fun scheduleDaily(context: Context, task: TaskEntity) {
        val time = task.scheduleTime ?: return
        val parts = time.split(":")
        if (parts.size != 2) return

        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
        }

        if (target.before(now)) {
            target.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        val delayMs = target.timeInMillis - now.timeInMillis

        val data = workDataOf("task_id" to task.id)
        val constraints = buildConstraints(task)

        val request = OneTimeWorkRequestBuilder<ScrapeWorker>()
            .setInputData(data)
            .addTag(TAG_PREFIX + task.id)
            .setConstraints(constraints)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "scrape_daily_${task.id}",
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    /**
     * Cancel scheduled work for a task.
     */
    fun cancel(context: Context, taskId: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("scrape_interval_$taskId")
        WorkManager.getInstance(context)
            .cancelUniqueWork("scrape_daily_$taskId")
        WorkManager.getInstance(context)
            .cancelUniqueWork("scrape_now_$taskId")
    }

    /**
     * Build constraints based on task settings.
     */
    private fun buildConstraints(task: TaskEntity?): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(
                if (task?.wifiOnly == true) NetworkType.UNMETERED
                else NetworkType.CONNECTED
            )
            .build()
    }
}
