package com.webscraper.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val status: String = "pending",  // pending|running|completed|failed
    val useJsRender: Boolean = false,
    val waitSeconds: Int = 3,
    val scrollToBottom: Boolean = false,
    val totalItems: Int = 0,
    val errorMessage: String? = null,
    // Schedule
    val scheduleType: String = "none",  // none|interval|daily
    val scheduleIntervalMinutes: Long = 0,
    val scheduleTime: String? = null,   // "08:00"
    val wifiOnly: Boolean = false,
    // Proxy
    val proxyHost: String? = null,
    val proxyPort: Int = 0,
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
