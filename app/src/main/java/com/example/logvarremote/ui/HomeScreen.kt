package com.example.logvarremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.logvarremote.data.runtime.RemoteCatalog

@Composable
fun HomeRoute(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    HomeScreen(
        state = state,
        onAnimeChange = viewModel::setAnime,
        onEpisodeChange = viewModel::setEpisode,
        onInstall = viewModel::installRuntime,
        onStart = viewModel::startRuntime,
        onSearch = viewModel::search
    )
}

@Composable
private fun HomeScreen(
    state: HomeUiState,
    onAnimeChange: (String) -> Unit,
    onEpisodeChange: (String) -> Unit,
    onInstall: () -> Unit,
    onStart: () -> Unit,
    onSearch: () -> Unit
) {
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
                "APK 不包含 libnode.so、LogVar Core 或 Node node_modules；首次安装时从固定远程版本下载。",
                style = MaterialTheme.typography.bodyMedium
            )

            StatusCard(state)

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

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("本地 API 测试", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = state.anime,
                            onValueChange = onAnimeChange,
                            label = { Text("影视/番剧名") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.episode,
                            onValueChange = onEpisodeChange,
                            label = { Text("集") },
                            modifier = Modifier.weight(0.35f),
                            singleLine = true
                        )
                    }
                    Button(
                        onClick = onSearch,
                        enabled = state.running && !state.busy && state.anime.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("调用 /api/v2/search/episodes")
                    }
                    Text(
                        "http://127.0.0.1:${RemoteCatalog.PORT}/${RemoteCatalog.TOKEN}/…",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (state.result.isNotBlank()) {
                Text("返回结果", style = MaterialTheme.typography.titleMedium)
                Text(state.result, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }

            state.error?.let {
                Text("错误", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                Text(it, color = MaterialTheme.colorScheme.error, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(24.dp))
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
