package com.webscraper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.webscraper.viewmodel.DataStatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataStatsScreen(
    viewModel: DataStatsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadStats() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("数据统计") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DataArray,
                    label = "总数据量",
                    value = "${uiState.stats.totalDataItems}",
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.FolderOpen,
                    label = "有数据的任务",
                    value = "${uiState.stats.tasksWithData}",
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(8.dp))

            // Info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("使用说明", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    InfoItem("1. 点击「任务」标签创建新任务")
                    InfoItem("2. 输入网址后，在网页中点击要采集的内容")
                    InfoItem("3. App 自动识别同类元素并生成规则")
                    InfoItem("4. 保存规则后，点击「运行」开始采集")
                    InfoItem("5. 采集完成后可导出 JSON 或 Excel")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("功能特点", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    FeatureItem("🎯", "视觉框选", "点击网页元素即可选取，无需手写代码")
                    FeatureItem("🔄", "JS 渲染", "支持 SPA 等动态页面")
                    FeatureItem("⏰", "定时采集", "支持间隔/每日定时自动采集")
                    FeatureItem("📊", "数据导出", "JSON + Excel 格式")
                    FeatureItem("🛡️", "反爬策略", "UA 轮换 + 请求延迟 + 代理支持")
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun InfoItem(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
}

@Composable
fun FeatureItem(emoji: String, title: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(emoji, modifier = Modifier.width(32.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}
