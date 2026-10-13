package com.example.bangumioauthdemo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val vm: OAuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Screen(vm) } }
        vm.callback(intent?.data)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        vm.callback(intent.data)
    }
    private fun login() {
        if (BuildConfig.BGM_CLIENT_ID.isBlank() || vm.clientSecret.value.isBlank()) return
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(vm.beginLogin())))
    }

    @Composable
    private fun Screen(vm: OAuthViewModel) {
        val output by vm.output.collectAsStateWithLifecycle()
        val busy by vm.busy.collectAsStateWithLifecycle()
        val secret by vm.clientSecret.collectAsStateWithLifecycle()
        val clipboard = LocalClipboardManager.current
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Bangumi OAuth Demo", style = MaterialTheme.typography.headlineMedium)
            Text("本地 OAuth 2.0 授权演示 · Compose / MVVM")
            Text("回调地址：" + OAuthRepository.REDIRECT)
            OutlinedTextField(
                value = secret,
                onValueChange = vm::updateClientSecret,
                label = { Text("App Secret（首次使用填写，仅存本机）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Button(
                onClick = { login() },
                enabled = !busy && secret.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("使用 Bangumi 登录并授权") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::refresh, enabled = !busy) { Text("刷新 Token") }
                OutlinedButton(onClick = vm::status, enabled = !busy) { Text("Token 状态") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::me, enabled = !busy) { Text("用户信息") }
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(output)) }) { Text("复制结果") }
                TextButton(onClick = vm::clear) { Text("清除") }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            HorizontalDivider()
            Text("授权返回数据", style = MaterialTheme.typography.titleMedium)
            SelectionContainer { Text(output, style = MaterialTheme.typography.bodySmall) }
            Text(
                "演示版本：App ID 已内置；开发者密钥不上传到公共仓库，输入后只存储在本机应用私有存储。Token 不会永久保存。",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
