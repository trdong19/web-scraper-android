package com.webscraper.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Shared ViewModel to pass task creation data between CreateTaskScreen and VisualSelectorScreen.
 * Avoids URL encoding issues in navigation routes.
 */
class CreateTaskViewModel : ViewModel() {
    var taskName by mutableStateOf("")
    var taskUrl by mutableStateOf("")
}
