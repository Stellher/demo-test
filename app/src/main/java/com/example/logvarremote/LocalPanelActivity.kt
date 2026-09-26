package com.example.logvarremote

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.example.logvarremote.data.runtime.RemoteCatalog

class LocalPanelActivity : ComponentActivity() {
    companion object {
        private const val LOCAL_HOST = "127.0.0.1"

        private val CONFIG_EXPORT_SHIM = """
            (() => {
              if (window.__logvarAndroidExportShimInstalled) return;
              window.__logvarAndroidExportShimInstalled = true;

              const originalClick = HTMLAnchorElement.prototype.click;

              HTMLAnchorElement.prototype.click = function () {
                const anchor = this;
                const href = String(anchor.href || '');
                const fileName = String(anchor.download || '');

                if (
                  fileName &&
                  href.startsWith('blob:') &&
                  window.AndroidConfigBridge &&
                  typeof window.AndroidConfigBridge.saveConfig === 'function'
                ) {
                  fetch(href)
                    .then(response => response.text())
                    .then(text => window.AndroidConfigBridge.saveConfig(text, fileName))
                    .catch(() => originalClick.call(anchor));
                  return;
                }

                return originalClick.call(anchor);
              };
            })();
        """.trimIndent()
    }

    private var webView: WebView? = null
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingExportJson: String? = null

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = fileCallback ?: return@registerForActivityResult
            fileCallback = null
            callback.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            )
        }

    private val configExportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            val json = pendingExportJson
            pendingExportJson = null
            if (uri == null || json == null) return@registerForActivityResult

            runCatching {
                contentResolver.openOutputStream(uri, "wt")
                    ?.bufferedWriter(Charsets.UTF_8)
                    .use { writer ->
                        checkNotNull(writer) { "Unable to open export destination" }
                        writer.write(json)
                    }
            }.onSuccess {
                Toast.makeText(this, "配置导出成功", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "配置导出失败：${it.message}", Toast.LENGTH_LONG).show()
            }
        }

    private inner class ConfigExportBridge {
        @JavascriptInterface
        fun saveConfig(json: String, fileName: String) {
            runOnUiThread {
                pendingExportJson = json
                val safeName = fileName
                    .ifBlank { "danmu-api-config.json" }
                    .replace(Regex("""[\\/:*?"<>|]"""), "_")
                configExportLauncher.launch(safeName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).also { view ->
            view.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            view.addJavascriptInterface(ConfigExportBridge(), "AndroidConfigBridge")
            view.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val uri = runCatching { Uri.parse(url) }.getOrNull()
                    if (uri?.host == LOCAL_HOST) {
                        view?.evaluateJavascript(CONFIG_EXPORT_SHIM, null)
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val uri = request?.url ?: return false
                    if (uri.host == LOCAL_HOST) return false

                    return runCatching {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                        true
                    }.getOrDefault(true)
                }
            }

            view.webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    this@LocalPanelActivity.fileCallback?.onReceiveValue(null)
                    this@LocalPanelActivity.fileCallback = filePathCallback
                    return try {
                        val intent = fileChooserParams?.createIntent() ?: return false
                        fileChooserLauncher.launch(intent)
                        true
                    } catch (_: ActivityNotFoundException) {
                        this@LocalPanelActivity.fileCallback = null
                        filePathCallback?.onReceiveValue(null)
                        false
                    }
                }
            }

            view.loadUrl(
                "http://$LOCAL_HOST:" + RemoteCatalog.PORT + "/" + RemoteCatalog.TOKEN + "/"
            )
        }

        setContentView(webView)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val view = webView
                if (view != null && view.canGoBack()) view.goBack() else finish()
            }
        })
    }

    override fun onDestroy() {
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        pendingExportJson = null
        webView?.apply {
            removeJavascriptInterface("AndroidConfigBridge")
            stopLoading()
            webChromeClient = null
            webViewClient = WebViewClient()
            destroy()
        }
        webView = null
        super.onDestroy()
    }
}
