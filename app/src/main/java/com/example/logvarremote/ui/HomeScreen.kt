package com.example.logvarremote.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logvarremote.LocalPanelActivity
import com.example.logvarremote.data.api.ApiHttpMethod
import com.example.logvarremote.data.api.LogVarApiCatalog
import com.example.logvarremote.data.runtime.RemoteCatalog

@Composable
fun HomeRoute(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    HomeScreen(
        state = state,
        onInstall = viewModel::installRuntime,
        onStart = viewModel::startRuntime,
        onSelectApi = viewModel::selectApi,
        onDebugPathChange = viewModel::setDebugPath,
        onDebugQueryChange = viewModel::setDebugQuery,
        onDebugBodyChange = viewModel::setDebugBody,
        onExecuteDebug = viewModel::executeDebugApi
    )
}

@Composable
private fun HomeScreen(
    state: HomeUiState,
    onInstall: () -> Unit,
    onStart: () -> Unit,
    onSelectApi: (String) -> Unit,
    onDebugPathChange: (String) -> Unit,
    onDebugQueryChange: (String) -> Unit,
    onDebugBodyChange: (String) -> Unit,
    onExecuteDebug: () -> Unit
) {
    val context = LocalContext.current
    var section by remember { mutableStateOf("runtime") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("LogVar Remote Runtime Demo", style = MaterialTheme.typography.headlineSmall)
            Text(
                "远程 Node + LogVar Core · 全 API 调试 · 官方本地 Web 面板",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (section == "runtime") {
                    Button(onClick = { section = "runtime" }) { Text("运行时") }
                } else {
                    OutlinedButton(onClick = { section = "runtime" }) { Text("运行时") }
                }

                if (section == "api") {
                    Button(onClick = { section = "api" }) { Text("全 API 调试") }
                } else {
                    OutlinedButton(onClick = { section = "api" }) { Text("全 API 调试") }
                }
            }

            StatusCard(state)

            if (section == "runtime") {
                RuntimeSection(
                    state = state,
                    onInstall = onInstall,
                    onStart = onStart,
                    onOpenPanel = {
                        context.startActivity(Intent(context, LocalPanelActivity::class.java))
                    }
                )
            } else {
                ApiDebuggerSection(
                    state = state,
                    onSelectApi = onSelectApi,
                    onDebugPathChange = onDebugPathChange,
                    onDebugQueryChange = onDebugQueryChange,
                    onDebugBodyChange = onDebugBodyChange,
                    onExecuteDebug = onExecuteDebug,
                    onOpenPanel = {
                        context.startActivity(Intent(context, LocalPanelActivity::class.java))
                    }
                )
            }

            state.error?.let {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("错误", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                        SelectionContainer {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RuntimeSection(
    state: HomeUiState,
    onInstall: () -> Unit,
    onStart: () -> Unit,
    onOpenPanel: () -> Unit
) {
    Button(
        onClick = onInstall,
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (state.installed) "重新安装 / 修复远程运行时" else "下载并安装远程运行时")
    }

    Button(
        onClick = onStart,
        enabled = state.installed && !state.running && !state.busy,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (state.running) "LogVar 已启动" else "启动本地 LogVar")
    }

    Button(
        onClick = onOpenPanel,
        enabled = state.running && !state.busy,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("打开 LogVar 本地面板")
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("本地服务", style = MaterialTheme.typography.titleMedium)
            Text(
                "面板：http://127.0.0.1:${RemoteCatalog.PORT}/${RemoteCatalog.TOKEN}/",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "API：http://127.0.0.1:${RemoteCatalog.PORT}/${RemoteCatalog.TOKEN}/api/…",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Proxy：http://127.0.0.1:${RemoteCatalog.PORT + 1}/",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "本地面板使用 LogVar Core 自带 UI，并支持 WebView 文件选择，可直接测试本地弹幕上传。",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ApiDebuggerSection(
    state: HomeUiState,
    onSelectApi: (String) -> Unit,
    onDebugPathChange: (String) -> Unit,
    onDebugQueryChange: (String) -> Unit,
    onDebugBodyChange: (String) -> Unit,
    onExecuteDebug: () -> Unit,
    onOpenPanel: () -> Unit
) {
    val endpoint = LogVarApiCatalog.byId(state.selectedApiId)
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "全 API 调试器 · ${LogVarApiCatalog.endpoints.size} 个路由操作",
                style = MaterialTheme.typography.titleMedium
            )

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${endpoint.category} · ${endpoint.method} · ${endpoint.name}")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    var lastCategory = ""
                    LogVarApiCatalog.endpoints.forEach { item ->
                        if (item.category != lastCategory) {
                            if (lastCategory.isNotEmpty()) HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(item.category, style = MaterialTheme.typography.labelMedium) },
                                onClick = {},
                                enabled = false
                            )
                            lastCategory = item.category
                        }
                        DropdownMenuItem(
                            text = { Text("${item.method}  ${item.name}") },
                            onClick = {
                                expanded = false
                                onSelectApi(item.id)
                            }
                        )
                    }
                }
            }

            if (endpoint.description.isNotBlank()) {
                Text(endpoint.description, style = MaterialTheme.typography.bodySmall)
            }

            Text(
                "目标端口 ${endpoint.port} · ${if (endpoint.tokenRequired) "自动添加本地 Token" else "无需 Token"}",
                style = MaterialTheme.typography.labelMedium
            )

            OutlinedTextField(
                value = state.debugPath,
                onValueChange = onDebugPathChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Path") },
                singleLine = true
            )

            OutlinedTextField(
                value = state.debugQuery,
                onValueChange = onDebugQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Query（不含 ?）") },
                minLines = 2
            )

            if (endpoint.method !in setOf(ApiHttpMethod.GET, ApiHttpMethod.HEAD)) {
                OutlinedTextField(
                    value = state.debugBody,
                    onValueChange = onDebugBodyChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (endpoint.multipartOnly) "Body（该接口使用 multipart）" else "JSON Body") },
                    minLines = 5
                )
            }

            if (endpoint.multipartOnly) {
                Text(
                    "该接口是 multipart 文件上传。Compose 调试器列出并识别该路由；实际文件选择请使用下面的本地 Web 面板。",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    onClick = onOpenPanel,
                    enabled = state.running,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("打开面板上传本地弹幕")
                }
            }

            Button(
                onClick = onExecuteDebug,
                enabled = state.running && !state.busy && !endpoint.multipartOnly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("发送 ${endpoint.method} 请求")
            }

            if (state.debugRequestUrl.isNotBlank()) {
                Text("Request", style = MaterialTheme.typography.labelLarge)
                SelectionContainer {
                    Text(
                        state.debugRequestUrl,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (state.debugStatusCode != null) {
                Text(
                    "HTTP ${state.debugStatusCode} · ${state.debugElapsedMs ?: 0} ms",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            if (state.debugResponse.isNotBlank()) {
                HorizontalDivider()
                Text("Response", style = MaterialTheme.typography.labelLarge)
                SelectionContainer {
                    Text(
                        state.debugResponse,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(state: HomeUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("运行状态", style = MaterialTheme.typography.titleMedium)
            Text("Runtime：${if (state.installed) "已安装" else "未安装"}")
            Text("LogVar：${if (state.running) "运行中" else "未运行"}")
            Text(state.stage)
            if (state.busy) {
                if (state.progress != null) {
                    LinearProgressIndicator(
                        progress = { state.progress ?: 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
