package com.example.bangumioauthdemo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExplorerResult(
    val endpointId: String,
    val response: ApiResult? = null,
    val error: String? = null
)

class ApiExplorerViewModel(app: Application) : AndroidViewModel(app) {
    val endpoints: List<ApiEndpoint> = ApiCatalog.load(app)
    private val executor = ApiExecutor()

    private val _selected = MutableStateFlow<ApiEndpoint?>(null)
    val selected = _selected.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    private val _result = MutableStateFlow<ExplorerResult?>(null)
    val result = _result.asStateFlow()

    private val _history = MutableStateFlow<List<ApiHistoryEntry>>(emptyList())
    val history = _history.asStateFlow()

    fun select(endpoint: ApiEndpoint) {
        _selected.value = endpoint
        _result.value = null
    }

    fun backToList() {
        _selected.value = null
        _result.value = null
    }

    fun execute(
        endpoint: ApiEndpoint,
        params: Map<String, String>,
        body: String,
        bearerToken: String?
    ) {
        if (_busy.value) return
        _busy.value = true
        _result.value = null
        viewModelScope.launch {
            try {
                val value = executor.execute(endpoint, params, body, bearerToken)
                _result.value = ExplorerResult(endpoint.id, response = value)
                _history.value = (
                    listOf(ApiHistoryEntry(
                        label = endpoint.id,
                        code = value.httpCode,
                        duration = value.durationMillis,
                        detail = value.url
                    )) + _history.value
                ).take(30)
            } catch (e: Exception) {
                val message = e.message ?: e.javaClass.simpleName
                _result.value = ExplorerResult(endpoint.id, error = message)
                _history.value = (
                    listOf(ApiHistoryEntry(endpoint.id, null, null, message)) + _history.value
                ).take(30)
            } finally {
                _busy.value = false
            }
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
    }
}
