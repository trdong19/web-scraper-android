package com.webscraper.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.webscraper.data.db.AppDatabase
import com.webscraper.data.model.FieldConfig
import com.webscraper.data.repository.TaskRepository
import com.webscraper.engine.InjectedJS
import com.webscraper.engine.JsBridge
import com.webscraper.engine.SelectorGenerator
import com.google.gson.Gson
import com.webscraper.viewmodel.CreateTaskViewModel
import kotlinx.coroutines.launch

enum class SelectorMode { BROWSE, PICK_LIST, PICK_FIELD }

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VisualSelectorScreen(
    viewModel: CreateTaskViewModel,
    onBack: () -> Unit,
    onRulesCreated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val url = viewModel.taskUrl
    val taskName = viewModel.taskName

    var currentMode by remember { mutableStateOf(SelectorMode.BROWSE) }
    var listSelector by remember { mutableStateOf("") }
    var matchedCount by remember { mutableIntStateOf(0) }
    var selectedFields by remember { mutableStateOf(mutableListOf<FieldConfig>()) }
    var currentElementInfo by remember { mutableStateOf<String?>(null) }
    var pageLoaded by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf(url) }

    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { TaskRepository(db.taskDao(), db.ruleDao(), db.scrapedDataDao()) }

    // JS Bridge callbacks
    val jsBridge = remember {
        JsBridge(
            onListSelected = { selector, count, info ->
                listSelector = selector
                matchedCount = count
                currentMode = SelectorMode.PICK_FIELD
            },
            onFieldSelected = { fieldInfo, total ->
                val parsed = gson.fromJson(fieldInfo, Map::class.java) as? Map<*, *>
                val fieldName = parsed?.get("name") as? String ?: "field_${selectedFields.size + 1}"
                val fieldSelector = parsed?.get("selector") as? String ?: ""
                val sampleText = parsed?.get("sampleText") as? String ?: ""

                // Generate relative selector
                val relativeSelector = SelectorGenerator.generateFieldSelector(fieldSelector, listSelector)

                selectedFields = (selectedFields + FieldConfig(
                    name = fieldName,
                    selector = relativeSelector,
                    type = "text"
                )).toMutableList()
            },
            onElementClicked = { info ->
                currentElementInfo = info
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("框选采集内容") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (listSelector.isNotBlank() && selectedFields.isNotEmpty()) {
                        IconButton(onClick = { showSaveDialog = true }) {
                            Icon(Icons.Default.Save, "保存规则")
                        }
                    }
                }
            )
        },
        bottomBar = {
            // Mode toolbar
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // URL bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall,
                            placeholder = { Text("输入网址") }
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = {
                            webView?.loadUrl(urlInput)
                            isLoading = true
                        }) {
                            Icon(Icons.Default.OpenInBrowser, "Go")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Mode buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ModeButton(
                            label = "浏览",
                            icon = Icons.Default.Public,
                            isActive = currentMode == SelectorMode.BROWSE,
                            onClick = {
                                currentMode = SelectorMode.BROWSE
                                webView?.evaluateJavascript(InjectedJS.setBrowseMode(), null)
                            }
                        )
                        ModeButton(
                            label = "选列表",
                            icon = Icons.Default.GridView,
                            isActive = currentMode == SelectorMode.PICK_LIST,
                            onClick = {
                                currentMode = SelectorMode.PICK_LIST
                                webView?.evaluateJavascript(InjectedJS.setPickListMode(), null)
                            }
                        )
                        ModeButton(
                            label = "选字段",
                            icon = Icons.Default.TouchApp,
                            isActive = currentMode == SelectorMode.PICK_FIELD,
                            enabled = listSelector.isNotBlank(),
                            onClick = {
                                currentMode = SelectorMode.PICK_FIELD
                                webView?.evaluateJavascript(InjectedJS.setPickFieldMode(), null)
                            }
                        )
                        ModeButton(
                            label = "重置",
                            icon = Icons.Default.Refresh,
                            isActive = false,
                            onClick = {
                                currentMode = SelectorMode.BROWSE
                                listSelector = ""
                                matchedCount = 0
                                selectedFields = mutableListOf()
                                webView?.evaluateJavascript(InjectedJS.resetSelection(), null)
                                webView?.evaluateJavascript(InjectedJS.setBrowseMode(), null)
                            }
                        )
                    }

                    // Status
                    if (listSelector.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "已识别 $matchedCount 个元素",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "已选 ${selectedFields.size} 个字段",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // WebView
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = userAgentString.replace("wv", "")
                        }

                        addJavascriptInterface(jsBridge, "AndroidBridge")

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                pageLoaded = true
                                // Inject selector JS
                                evaluateJavascript(InjectedJS.initSelector(), null)
                            }
                        }

                        loadUrl(url)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Loading indicator
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Selected fields panel (overlay at bottom)
            if (selectedFields.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                        .fillMaxWidth(0.9f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("已选字段:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        selectedFields.forEachIndexed { index, field ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${field.name}: ${field.selector}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        selectedFields = selectedFields.toMutableList().apply { removeAt(index) }
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, "移除", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Mode indicator
            if (currentMode != SelectorMode.BROWSE) {
                val (label, color) = when (currentMode) {
                    SelectorMode.PICK_LIST -> "请点击要采集的列表元素" to Color(0xFFFF9800)
                    SelectorMode.PICK_FIELD -> "请点击列表内的字段（标题/价格/图片）" to Color(0xFF4CAF50)
                    else -> "" to Color.Transparent
                }
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .background(color, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    // Save dialog
    if (showSaveDialog) {
        SaveRuleDialog(
            fieldCount = selectedFields.size,
            matchedCount = matchedCount,
            onDismiss = { showSaveDialog = false },
            onSave = { ruleName ->
                scope.launch {
                    val task = repo.createTask(name = taskName, url = url)
                    repo.createRule(
                        taskId = task.id,
                        name = ruleName,
                        listSelector = listSelector,
                        selectorType = "css",
                        fields = selectedFields
                    )
                    showSaveDialog = false
                    onRulesCreated()
                }
            }
        )
    }
}

@Composable
fun ModeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = if (isActive) ButtonDefaults.textButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else ButtonDefaults.textButtonColors()
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label)
    }
}

@Composable
fun SaveRuleDialog(
    fieldCount: Int,
    matchedCount: Int,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var ruleName by remember { mutableStateOf("采集规则") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存采集规则") },
        text = {
            Column {
                Text("识别到 $matchedCount 个元素，已选 $fieldCount 个字段")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text("规则名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(ruleName) },
                enabled = ruleName.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
