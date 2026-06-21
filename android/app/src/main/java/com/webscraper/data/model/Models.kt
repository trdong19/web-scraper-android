package com.webscraper.data.model

data class FieldConfig(
    val name: String,
    val selector: String,
    val type: String = "text",
    val attribute: String? = null
)

data class SelectedElement(
    val tagName: String,
    val className: String,
    val id: String,
    val text: String,
    val cssSelector: String,
    val xpath: String
)

data class TaskStats(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val failedTasks: Int = 0,
    val totalDataItems: Int = 0,
    val tasksWithData: Int = 0
)
