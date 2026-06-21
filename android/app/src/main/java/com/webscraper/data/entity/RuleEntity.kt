package com.webscraper.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val name: String,
    val listSelector: String,
    val selectorType: String = "css",  // css | xpath
    val fieldsJson: String = "[]"      // JSON array of FieldConfig
)
