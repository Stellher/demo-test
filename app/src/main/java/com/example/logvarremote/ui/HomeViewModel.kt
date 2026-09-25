package com.example.logvarremote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.logvarremote.data.api.LogVarApiCatalog
import com.example.logvarremote.data.repository.LogVarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LogVarRepository(application)

    private val _state = MutableStateFlow(
        HomeUiState(
            installed = repository.isInstalled(),
            stage = if (repository.isInstalled()) "远程运行时已安装" else "尚未安装远程运行时"
        )
    )
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun setAnime(value: String) = _state.update { it.copy(anime = value) }
    fun setEpisode(value: String) = _state.update { it.copy(episode = value.filter(Char::isDigit)) }

    fun installRuntime() {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, result = "") }
            val result = repository.install { progress ->
                _state.update { it.copy(stage = progress.stage, progress = progress.fraction) }
            }
            result.onSuccess {
                _state.update {
                    it.copy(
                        installed = true,
                        busy = false,
                        progress = 1f,
                        stage = "安装完成：Node + Android 启动层 + LogVar Core 均来自远程"
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(busy = false, progress = null, error = error.stackTraceToString(), stage = "安装失败")
                }
            }
        }
    }

    fun startRuntime() {
        if (_state.value.busy || !_state.value.installed) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, stage = "启动本地 Node/LogVar…") }
            repository.startAndWait()
                .onSuccess {
                    _state.update {
                        it.copy(
                            busy = false,
                            running = true,
                            stage = "LogVar 已运行：127.0.0.1:19321 · 面板与全 API 调试已可用"
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(busy = false, error = error.stackTraceToString(), stage = "启动失败") }
                }
        }
    }

    fun selectApi(id: String) {
        val endpoint = LogVarApiCatalog.byId(id)
        _state.update {
            it.copy(
                selectedApiId = endpoint.id,
                debugPath = endpoint.path,
                debugQuery = endpoint.defaultQuery,
                debugBody = endpoint.defaultBody,
                debugRequestUrl = "",
                debugResponse = "",
                debugStatusCode = null,
                debugElapsedMs = null,
                error = null
            )
        }
    }

    fun setDebugPath(value: String) = _state.update { it.copy(debugPath = value) }
    fun setDebugQuery(value: String) = _state.update { it.copy(debugQuery = value) }
    fun setDebugBody(value: String) = _state.update { it.copy(debugBody = value) }

    fun executeDebugApi() {
        val snapshot = _state.value
        if (snapshot.busy || !snapshot.running) return
        val endpoint = LogVarApiCatalog.byId(snapshot.selectedApiId)
        if (endpoint.multipartOnly) {
            _state.update {
                it.copy(
                    error = "本地弹幕上传需要 multipart 文件选择，请打开“本地 Web 面板”执行上传。",
                    debugResponse = ""
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    busy = true,
                    error = null,
                    debugResponse = "",
                    debugStatusCode = null,
                    debugElapsedMs = null,
                    stage = "调试 ${endpoint.method} ${snapshot.debugPath}…"
                )
            }
            repository.executeDebug(
                endpoint = endpoint,
                path = snapshot.debugPath,
                query = snapshot.debugQuery,
                body = snapshot.debugBody
            ).onSuccess { result ->
                _state.update {
                    it.copy(
                        busy = false,
                        stage = "接口调试完成",
                        debugRequestUrl = result.requestUrl,
                        debugStatusCode = result.code,
                        debugElapsedMs = result.elapsedMs,
                        debugResponse = buildString {
                            appendLine("HTTP ${result.code} · ${result.elapsedMs} ms")
                            result.contentType?.let { type -> appendLine("Content-Type: $type") }
                            if (result.headers.isNotBlank()) {
                                appendLine()
                                appendLine(result.headers)
                            }
                            if (result.body.isNotBlank()) {
                                appendLine()
                                append(result.body.take(60_000))
                            }
                        }
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        busy = false,
                        stage = "接口调试失败",
                        error = error.stackTraceToString()
                    )
                }
            }
        }
    }

    fun search() {
        val snapshot = _state.value
        if (snapshot.busy || !snapshot.running) return
        val episode = snapshot.episode.toIntOrNull() ?: 1
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, result = "", stage = "调用本地 LogVar API…") }
            repository.searchEpisode(snapshot.anime.trim(), episode)
                .onSuccess { body ->
                    _state.update { it.copy(busy = false, stage = "请求完成", result = body.take(20_000)) }
                }
                .onFailure { error ->
                    _state.update { it.copy(busy = false, error = error.stackTraceToString(), stage = "请求失败") }
                }
        }
    }
}
