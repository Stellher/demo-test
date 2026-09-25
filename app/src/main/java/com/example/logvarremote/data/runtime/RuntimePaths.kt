package com.example.logvarremote.data.runtime

import android.content.Context
import java.io.File

class RuntimePaths(context: Context) {
    private val root = File(context.filesDir, "remote_logvar")

    val runtimeDir = File(root, "runtime")
    val projectDir = File(root, "project")
    val downloadDir = File(root, "downloads")

    val nodeLib = File(runtimeDir, "libnode.so")
    val libcxxLib = File(runtimeDir, "libc++_shared.so")
    val mainJs = File(projectDir, "main.js")
    val coreDir = File(projectDir, "danmu_api_stable")
    val installedMarker = File(root, ".installed-v3")

    fun isInstalled(): Boolean =
        nodeLib.isFile && mainJs.isFile && File(coreDir, "worker.js").isFile && installedMarker.isFile

    fun ensureBaseDirs() {
        root.mkdirs()
        runtimeDir.mkdirs()
        projectDir.mkdirs()
        downloadDir.mkdirs()
        File(projectDir, "tmp").mkdirs()
    }
}
