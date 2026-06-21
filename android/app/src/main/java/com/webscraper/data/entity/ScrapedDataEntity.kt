package com.webscraper.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scraped_data")
data class ScrapedDataEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val dataJson: String = "{}",
    val scrapedAt: Long = System.currentTimeMillis()
)
