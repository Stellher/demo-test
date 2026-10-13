package com.example.bangumioauthdemo

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.security.SecureRandom

class OAuthViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("oauth_demo", 0)
    private val _output = MutableStateFlow("点击登录，授权结果将显示在这里。")
    val output = _output.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _clientSecret = MutableStateFlow(prefs.getString("client_secret", "").orEmpty())
    val clientSecret = _clientSecret.asStateFlow()

    fun updateClientSecret(secret: String) {
        _clientSecret.value = secret
        prefs.edit().putString("client_secret", secret).apply()
    }

    private var pendingState: String? = prefs.getString("pending_state", null)
    private var accessToken: String? = null
    private var refreshToken: String? = null

    fun beginLogin(): String {
        val bytes = ByteArray(24).also { SecureRandom().nextBytes(it) }
        val state = bytes.joinToString("") { "%02x".format(it) }
        pendingState = state
        prefs.edit().putString("pending_state", state).apply()
        _output.value = "等待 Bangumi 授权回调…"
        return OAuthRepository.authorizeUrl(state)
    }

    fun callback(uri: Uri?) {
        if (uri?.scheme != "bgmoauthdemo" || uri.host != "callback") return
        val error = uri.getQueryParameter("error")
        if (error != null) {
            _output.value = "授权失败：" + error
            return
        }
        val state = uri.getQueryParameter("state")
        if (pendingState == null || state != pendingState) {
            _output.value = "拒绝回调：state 不匹配"
            return
        }
        val code = uri.getQueryParameter("code")
        if (code.isNullOrBlank()) {
            _output.value = "回调缺少 code"
            return
        }
        pendingState = null
        prefs.edit().remove("pending_state").apply()
        _output.value = "Authorization Code:\n" + code + "\n\n正在交换 Token…"
        execute { OAuthRepository.exchange(code, _clientSecret.value) }
    }

    private fun execute(block: suspend () -> JSONObject) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val result = block()
                result.optString("access_token").takeIf { it.isNotBlank() }?.let { accessToken = it }
                result.optString("refresh_token").takeIf { it.isNotBlank() }?.let { refreshToken = it }
                _output.value = result.toString(2)
            } catch (e: Exception) {
                _output.value = "请求失败：" + e.message
            } finally {
                _busy.value = false
            }
        }
    }

    fun refresh() {
        val token = refreshToken ?: return
        execute { OAuthRepository.refresh(token, _clientSecret.value) }
    }
    fun status() {
        val token = accessToken ?: return
        execute { OAuthRepository.status(token) }
    }
    fun me() {
        val token = accessToken ?: return
        execute { OAuthRepository.me(token) }
    }
    fun clear() {
        accessToken = null
        refreshToken = null
        pendingState = null
        prefs.edit().remove("pending_state").apply()
        _output.value = "Token 与临时授权已清除"
    }
}
