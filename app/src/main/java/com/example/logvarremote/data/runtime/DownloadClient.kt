package com.example.logvarremote.data.runtime

import java.io.File
import java.io.FileOutputStream
import okhttp3.OkHttpClient
import okhttp3.Request

class DownloadClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    fun download(
        url: String,
        target: File,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ) {
        target.parentFile?.mkdirs()
        val part = File(target.absolutePath + ".part")
        if (part.exists()) part.delete()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "LogVarRemoteDemo/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code}: $url")
            }
            val body = response.body ?: error("Empty response body: $url")
            val total = body.contentLength()
            var written = 0L
            body.byteStream().use { input ->
                FileOutputStream(part).use { output ->
                    val buffer = ByteArray(128 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        output.write(buffer, 0, count)
                        written += count
                        onProgress(written, total)
                    }
                    output.fd.sync()
                }
            }
        }

        if (target.exists() && !target.delete()) error("Cannot replace ${target.name}")
        if (!part.renameTo(target)) error("Cannot commit download ${target.name}")
    }
}
