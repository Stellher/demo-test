package com.example.logvarremote.data.api

import com.example.logvarremote.data.runtime.RemoteCatalog
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import okhttp3.OkHttpClient
import okhttp3.Request

class LogVarLocalClient(
    private val client: OkHttpClient = OkHttpClient.Builder().build()
) {
    fun health(): Boolean {
        val request = Request.Builder()
            .url("http://127.0.0.1:${RemoteCatalog.PORT}/__health")
            .get()
            .build()
        return runCatching {
            client.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    fun searchEpisode(anime: String, episode: Int): String {
        val encoded = URLEncoder.encode(anime, StandardCharsets.UTF_8.name())
        val url = buildString {
            append("http://127.0.0.1:${RemoteCatalog.PORT}/")
            append(RemoteCatalog.TOKEN)
            append("/api/v2/search/episodes?anime=")
            append(encoded)
            append("&episode=")
            append(episode)
        }
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("HTTP ${response.code}: $body")
            return body
        }
    }
}
