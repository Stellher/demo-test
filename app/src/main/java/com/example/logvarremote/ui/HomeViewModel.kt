package com.example.logvarremote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
                    _state.update { it.copy(busy = false, running = true, stage = "LogVar 已运行：127.0.0.1:19321") }
                }
                .onFailure { error ->
                    _state.update { it.copy(busy = false, error = error.stackTraceToString(), stage = "启动失败") }
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
                    _state.update {
                        it.copy(
                            busy = false,
                            stage = "请求完成",
                            result = body.take(20_000)
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(busy = false, error = error.stackTraceToString(), stage = "请求失败") }
                }
        }
    }
}
