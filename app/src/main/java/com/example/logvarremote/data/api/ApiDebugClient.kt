package com.example.logvarremote.data.api

import com.example.logvarremote.data.runtime.RemoteCatalog
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ApiDebugClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    fun execute(endpoint: ApiEndpoint, path: String, query: String, body: String): ApiDebugResult {
        require(!endpoint.multipartOnly) {
            "该接口需要 multipart 文件上传，请使用“本地面板”中的本地弹幕上传功能"
        }

        val normalizedPath = if (path.startsWith('/')) path else "/$path"
        val routedPath = if (endpoint.tokenRequired) {
            "/" + RemoteCatalog.TOKEN + normalizedPath
        } else {
            normalizedPath
        }
        val queryText = query.trim().removePrefix("?")
        val url = buildString {
            append("http://127.0.0.1:")
            append(endpoint.port)
            append(routedPath)
            if (queryText.isNotBlank()) {
                append('?')
                append(queryText)
            }
        }

        val json = "application/json; charset=utf-8".toMediaType()
        val requestBody = when (endpoint.method) {
            ApiHttpMethod.GET, ApiHttpMethod.HEAD -> null
            else -> body.ifBlank { "{}" }.toRequestBody(json)
        }

        val request = Request.Builder()
            .url(url)
            .header("Accept", "*/*")
            .method(endpoint.method.name, requestBody)
            .build()

        val started = System.nanoTime()
        client.newCall(request).execute().use { response ->
            val elapsed = (System.nanoTime() - started) / 1_000_000L
            return ApiDebugResult(
                requestUrl = url,
                code = response.code,
                elapsedMs = elapsed,
                contentType = response.header("Content-Type"),
                headers = response.headers.toString().trim(),
                body = response.body?.string().orEmpty()
            )
        }
    }
}
