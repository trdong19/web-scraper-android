package com.webscraper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("设置") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("默认采集设置", style = MaterialTheme.typography.titleMedium)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    var waitSeconds by remember { mutableFloatStateOf(3f) }
                    Text("页面等待时间: ${waitSeconds.toInt()}秒")
                    Slider(value = waitSeconds, onValueChange = { waitSeconds = it }, valueRange = 1f..10f, steps = 8)
                }
            }

            Text("反爬策略", style = MaterialTheme.typography.titleMedium)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    var minDelay by remember { mutableFloatStateOf(1f) }
                    var maxDelay by remember { mutableFloatStateOf(3f) }
                    Text("请求延迟: ${minDelay.toInt()}~${maxDelay.toInt()}秒")
                    RangeSlider(value = minDelay..maxDelay, onValueChange = { minDelay = it.start; maxDelay = it.endInclusive }, valueRange = 0f..10f)
                }
            }

            Text("代理设置", style = MaterialTheme.typography.titleMedium)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    var host by remember { mutableStateOf("") }
                    var port by remember { mutableStateOf("") }
                    OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("代理地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("端口") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Text("关于", style = MaterialTheme.typography.titleMedium)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("网页采集系统 v1.0.0", fontWeight = FontWeight.Bold)
                    Text("纯 Android 本地运行，无需服务器", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text("Kotlin + Compose + Jsoup + OkHttp + Room", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
