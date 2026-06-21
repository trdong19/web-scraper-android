package com.webscraper.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.webscraper.data.entity.ScrapedDataEntity
import com.webscraper.ui.components.StatusBadge
import com.webscraper.viewmodel.TaskDetailViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(taskId: String, onBack: () -> Unit, viewModel: TaskDetailViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(taskId) { viewModel.loadTask(taskId) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(uiState.task?.name ?: "任务详情") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = {
                    var showExportMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showExportMenu = true }) { Icon(Icons.Default.MoreVert, "更多") }
                    DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                        DropdownMenuItem(text = { Text("导出 JSON") }, onClick = { viewModel.exportJson(uiState.task?.name ?: "data", uiState.dataItems); showExportMenu = false }, leadingIcon = { Icon(Icons.Default.Code, null) })
                        DropdownMenuItem(text = { Text("导出 Excel") }, onClick = { viewModel.exportExcel(uiState.task?.name ?: "data", uiState.dataItems); showExportMenu = false }, leadingIcon = { Icon(Icons.Default.TableChart, null) })
                    }
                })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            uiState.task?.let { task ->
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(task.status); Text("共 ${task.totalItems} 条数据", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(task.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        task.errorMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.runScrape(taskId) }, enabled = !uiState.isRunning, modifier = Modifier.fillMaxWidth()) {
                            if (uiState.isRunning) { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(8.dp)); Text("采集中...") }
                            else { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("开始采集") }
                        }
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("数据 (${uiState.dataItems.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("规则 (${uiState.rules.size})") })
            }

            when (selectedTab) {
                0 -> if (uiState.dataItems.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无数据", color = MaterialTheme.colorScheme.outline) } }
                     else { LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(uiState.dataItems, key = { it.id }) { DataItemCard(it) } } }
                1 -> if (uiState.rules.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无规则", color = MaterialTheme.colorScheme.outline) } }
                     else { LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(uiState.rules, key = { it.id }) { rule -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(rule.name, fontWeight = FontWeight.Bold); Text(rule.listSelector, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) } } } } }
            }
        }
    }

    uiState.exportedFile?.let { file ->
        LaunchedEffect(file) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "*/*"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "分享文件"))
            viewModel.clearExportedFile()
        }
    }

    uiState.error?.let { LaunchedEffect(it) { viewModel.clearError() } }
}

@Composable
fun DataItemCard(item: ScrapedDataEntity) {
    var expanded by remember { mutableStateOf(false) }
    val gson = remember { Gson() }
    val type = remember { object : TypeToken<Map<String, Any?>>() {}.type }
    val data: Map<String, Any?> = remember(item.dataJson) { try { gson.fromJson(item.dataJson, type) ?: emptyMap() } catch (e: Exception) { emptyMap() } }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            data.entries.take(2).forEach { (key, value) -> Row { Text("$key: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold); Text(value?.toString() ?: "null", style = MaterialTheme.typography.bodySmall) } }
            if (data.size > 2) {
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "收起" else "展开全部") }
                if (expanded) { data.entries.drop(2).forEach { (key, value) -> Row { Text("$key: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold); Text(value?.toString() ?: "null", style = MaterialTheme.typography.bodySmall) } } }
            }
        }
    }
}
