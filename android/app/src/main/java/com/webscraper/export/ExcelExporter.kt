package com.webscraper.export

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {

    private val gson = Gson()

    fun export(
        context: Context,
        taskName: String,
        dataList: List<String>  // List of JSON strings
    ): Result<File> {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val items = dataList.map { json ->
                gson.fromJson<Map<String, Any?>>(json, type) ?: emptyMap()
            }

            if (items.isEmpty()) {
                return Result.failure(Exception("No data to export"))
            }

            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet(taskName.take(31))  // Sheet name max 31 chars

            // Collect all unique keys
            val allKeys = mutableListOf<String>()
            items.forEach { item ->
                item.keys.forEach { key ->
                    if (key !in allKeys) allKeys.add(key)
                }
            }

            // Header style
            val headerStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.LIGHT_BLUE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                val font = workbook.createFont()
                font.bold = true
                setFont(font)
            }

            // Header row
            val headerRow = sheet.createRow(0)
            allKeys.forEachIndexed { index, key ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(key)
                cell.cellStyle = headerStyle
            }

            // Data rows
            items.forEachIndexed { rowIndex, item ->
                val row = sheet.createRow(rowIndex + 1)
                allKeys.forEachIndexed { colIndex, key ->
                    val cell = row.createCell(colIndex)
                    val value = item[key]
                    when (value) {
                        is Number -> cell.setCellValue(value.toDouble())
                        is Boolean -> cell.setCellValue(value.toString())
                        else -> cell.setCellValue(value?.toString() ?: "")
                    }
                }
            }

            // Auto-size columns
            allKeys.indices.forEach { sheet.autoSizeColumn(it) }

            // Save file
            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "WebScraper"
            )
            if (!dir.exists()) dir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeName = taskName.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "_")
            val file = File(dir, "${safeName}_$timestamp.xlsx")

            FileOutputStream(file).use { fos ->
                workbook.write(fos)
            }
            workbook.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
