package com.webscraper.export

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object JsonExporter {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun export(
        context: Context,
        taskName: String,
        dataList: List<String>  // List of JSON strings
    ): Result<File> {
        return try {
            // Parse each JSON string into a map, then build a list
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val items = dataList.map { json ->
                gson.fromJson<Map<String, Any?>>(json, type) ?: emptyMap()
            }

            val jsonContent = gson.toJson(items)

            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "WebScraper"
            )
            if (!dir.exists()) dir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeName = taskName.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "_")
            val file = File(dir, "${safeName}_$timestamp.json")
            file.writeText(jsonContent)

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
