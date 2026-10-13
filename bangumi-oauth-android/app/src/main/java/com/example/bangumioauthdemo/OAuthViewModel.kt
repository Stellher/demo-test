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

data class OAuthSession(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val scope: String,
    val userId: String,
    val expiresIn: Long,
    val expiresAtMillis: Long,
    val raw: String
)

class OAuthViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("oauth_demo", 0)
    private val _output = MutableStateFlow("授权后将在这里显示 OAuth 返回的所有字段。")
    val output = _output.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _clientSecret = MutableStateFlow(prefs.getString("client_secret", "").orEmpty())
    val clientSecret = _clientSecret.asStateFlow()
    private val _session = MutableStateFlow<OAuthSession?>(null)
    val session = _session.asStateFlow()
    private var pendingState: String? = prefs.getString("pending_state", null)

    fun updateClientSecret(secret: String) {
        _clientSecret.value = secret
        prefs.edit().putString("client_secret", secret).apply()
    }

    fun beginLogin(): String {
        val bytes = ByteArray(24).also { SecureRandom().nextBytes(it) }
        val state = bytes.joinToString("") { "%02x".format(it) }
        pendingState = state
        prefs.edit().putString("pending_state", state).apply()
        _output.value = "正在等待 Bangumi 浏览器授权回调…"
        return OAuthRepository.authorizeUrl(state)
    }

    fun callback(uri: Uri?) {
        if (uri?.scheme != "bgmoauthdemo" || uri.host != "callback") return
        val error = uri.getQueryParameter("error")
        if (error != null) {
            pendingState = null
            prefs.edit().remove("pending_state").apply()
            _output.value = "授权失败：" + error
            return
        }
        val state = uri.getQueryParameter("state")
        if (pendingState == null || state != pendingState) {
            _output.value = "拒绝授权回调：state 不匹配，请重新登录。"
            return
        }
        val code = uri.getQueryParameter("code")
        if (code.isNullOrBlank()) {
            _output.value = "回调中没有 authorization code。"
            return
        }
        pendingState = null
        prefs.edit().remove("pending_state").apply()
        _output.value = "Authorization Code:\n" + code + "\n\n正在交换 Access Token…"
        exchange(code)
    }

    private fun launchOperation(action: suspend () -> JSONObject, updatesTokens: Boolean = false) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val result = action()
                if (updatesTokens) {
                    val token = result.optString("access_token")
                    if (token.isBlank()) error("Token 接口未返回 access_token: " + result)
                    val old = _session.value
                    val expires = result.optLong("expires_in", 0)
                    _session.value = OAuthSession(
                        accessToken = token,
                        refreshToken = result.optString("refresh_token").ifBlank { old?.refreshToken.orEmpty() },
                        tokenType = result.optString("token_type", "Bearer"),
                        scope = result.optString("scope", "null"),
                        userId = result.optString("user_id").ifBlank { old?.userId.orEmpty() },
                        expiresIn = expires,
                        expiresAtMillis = System.currentTimeMillis() + expires * 1000L,
                        raw = result.toString(2)
                    )
                }
                _output.value = result.toString(2)
            } catch (e: Exception) {
                _output.value = "请求失败：" + e.message
            } finally {
                _busy.value = false
            }
        }
    }

    private fun exchange(code: String) {
        launchOperation({ OAuthRepository.exchange(code, _clientSecret.value) }, updatesTokens = true)
    }

    fun refresh() {
        val token = _session.value?.refreshToken.orEmpty()
        if (token.isBlank()) { _output.value = "没有 Refresh Token，请先登录"; return }
        launchOperation({ OAuthRepository.refresh(token, _clientSecret.value) }, updatesTokens = true)
    }

    fun status() {
        val token = _session.value?.accessToken.orEmpty()
        if (token.isBlank()) { _output.value = "没有 Access Token，请先登录"; return }
        launchOperation({ OAuthRepository.status(token) })
    }

    fun me() {
        val token = _session.value?.accessToken.orEmpty()
        if (token.isBlank()) { _output.value = "没有 Access Token，请先登录"; return }
        launchOperation({ OAuthRepository.me(token) })
    }

    fun clear() {
        _session.value = null
        pendingState = null
        prefs.edit().remove("pending_state").remove("client_secret").apply()
        _clientSecret.value = ""
        _output.value = "已清除 Access Token、Refresh Token 和保存在本机的 App Secret。"
    }
}
