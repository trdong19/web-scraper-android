package com.webscraper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.webscraper.viewmodel.CreateTaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    onBack: () -> Unit,
    onOpenSelector: (url: String, name: String) -> Unit,
    viewModel: CreateTaskViewModel
) {
    var name by remember { mutableStateOf(viewModel.taskName) }
    var url by remember { mutableStateOf(viewModel.taskUrl) }
    var useJsRender by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建采集任务") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("任务名称") },
                placeholder = { Text("例如：电商商品采集") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Edit, null) }
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("目标网址") },
                placeholder = { Text("https://example.com/products") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Link, null) }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useJsRender, onCheckedChange = { useJsRender = it })
                Spacer(Modifier.width(4.dp))
                Column {
                    Text("JS 渲染模式")
                    Text(
                        "适用于 SPA / 动态加载页面",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onOpenSelector(url, name) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = name.isNotBlank() && url.isNotBlank()
            ) {
                Icon(Icons.Default.TouchApp, null)
                Spacer(Modifier.width(8.dp))
                Text("打开页面，框选采集内容", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "💡 下一步：在网页中点击要采集的内容，App 会自动识别同类元素并生成采集规则",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
