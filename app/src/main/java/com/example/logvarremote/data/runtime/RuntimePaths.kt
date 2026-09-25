package com.example.logvarremote.data.runtime

import android.content.Context
import java.io.File

class RuntimePaths(context: Context) {
    companion object {
        const val DEFAULT_SOURCE_ORDER =
            "360,vod,tmdb,douban,tencent,youku,iqiyi,imgo,bilibili,renren,hanjutv,dandan,migu"
        const val DEFAULT_RATE_LIMIT_MAX_REQUESTS = "0"
    }

    private val root = File(context.filesDir, "remote_logvar")

    val runtimeDir = File(root, "runtime")
    val projectDir = File(root, "project")
    val downloadDir = File(root, "downloads")
    val configDir = File(projectDir, "config")
    val envFile = File(configDir, ".env")

    val nodeLib = File(runtimeDir, "libnode.so")
    val libcxxLib = File(runtimeDir, "libc++_shared.so")
    val mainJs = File(projectDir, "main.js")
    val coreDir = File(projectDir, "danmu_api_stable")
    val installedMarker = File(root, ".installed-v5-env-export")

    fun isInstalled(): Boolean =
        nodeLib.isFile && mainJs.isFile && File(coreDir, "worker.js").isFile && installedMarker.isFile

    fun ensureBaseDirs() {
        root.mkdirs()
        runtimeDir.mkdirs()
        projectDir.mkdirs()
        downloadDir.mkdirs()
        File(projectDir, "tmp").mkdirs()
        configDir.mkdirs()
    }

    fun ensureRuntimeConfigFile() {
        ensureRuntimeConfigDefaults(envFile)
    }

    fun ensureRuntimeConfigDefaults(target: File) {
        target.parentFile?.mkdirs()
        if (!target.exists()) target.writeText("")

        val existingText = target.readText()
        val existingKeys = existingText
            .lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") && '=' in it }
            .map { it.substringBefore('=').trim() }
            .toSet()

        val missing = buildList {
            if ("SOURCE_ORDER" !in existingKeys) {
                add("SOURCE_ORDER=$DEFAULT_SOURCE_ORDER")
            }
            if ("RATE_LIMIT_MAX_REQUESTS" !in existingKeys) {
                add("RATE_LIMIT_MAX_REQUESTS=$DEFAULT_RATE_LIMIT_MAX_REQUESTS")
            }
        }

        if (missing.isNotEmpty()) {
            val prefix = if (existingText.isBlank() || existingText.endsWith("\n")) "" else "\n"
            target.appendText(prefix + missing.joinToString(separator = "\n", postfix = "\n"))
        }

        check(target.isFile && target.canWrite()) {
            "Runtime config is not writable: ${target.absolutePath}"
        }
    }
}
