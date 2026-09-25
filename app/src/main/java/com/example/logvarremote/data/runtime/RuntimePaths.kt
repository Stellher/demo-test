package com.example.logvarremote.data.runtime

import android.content.Context
import java.io.File

class RuntimePaths(context: Context) {
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
    val installedMarker = File(root, ".installed-v4-full-bundle")

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
        configDir.mkdirs()
        if (!envFile.exists()) {
            envFile.writeText("")
        }
        check(envFile.isFile && envFile.canWrite()) {
            "Runtime config is not writable: ${envFile.absolutePath}"
        }
    }
}
