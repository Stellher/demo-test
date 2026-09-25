package com.example.logvarremote.data.runtime

import android.content.Context
import android.os.Build
import java.io.File

class RuntimeInstaller(
    context: Context,
    private val downloader: DownloadClient = DownloadClient()
) {
    private val paths = RuntimePaths(context.applicationContext)

    data class Progress(val stage: String, val fraction: Float? = null)

    fun isInstalled(): Boolean = paths.isInstalled()

    fun install(onProgress: (Progress) -> Unit) {
        val abi = chooseAbi()
        val bundle = RemoteCatalog.runtimeBundle(abi)
        paths.ensureBaseDirs()

        val bundleZip = File(
            paths.downloadDir,
            "logvar-runtime-${RemoteCatalog.NODE_FLAVOR}-$abi-${RemoteCatalog.NODE_VERSION}.zip"
        )

        onProgress(Progress("下载 FULL Node + LogVar Bundle ($abi)", 0f))
        downloader.download(bundle.url, bundleZip) { done, total ->
            onProgress(Progress("下载 FULL Node + LogVar Bundle ($abi)", fraction(done, total)))
        }

        onProgress(Progress("校验 $abi Bundle SHA-256"))
        FileIntegrity.requireSha256(bundleZip, bundle.sha256)

        val bundleStaging = File(paths.projectDir.parentFile, "bundle.staging")
        bundleStaging.deleteRecursively()
        bundleStaging.mkdirs()

        onProgress(Progress("解压 $abi 专用 Runtime"))
        ZipTools.extractAll(bundleZip, bundleStaging)

        val runtimeStaging = File(bundleStaging, "runtime")
        val projectStaging = File(bundleStaging, "project")

        val stagingEnvFile = File(projectStaging, "config/.env")
        stagingEnvFile.parentFile?.mkdirs()
        if (paths.envFile.isFile) {
            paths.envFile.copyTo(stagingEnvFile, overwrite = true)
        } else if (!stagingEnvFile.exists()) {
            stagingEnvFile.writeText("")
        }
        paths.ensureRuntimeConfigDefaults(stagingEnvFile)

        File(projectStaging, "tmp").mkdirs()

        validateStaging(runtimeStaging, projectStaging)
        makeNativeFilesLoadable(runtimeStaging)

        onProgress(Progress("原子替换 FULL $abi Runtime"))
        replaceDir(runtimeStaging, paths.runtimeDir)
        replaceDir(projectStaging, paths.projectDir)
        bundleStaging.deleteRecursively()

        paths.installedMarker.parentFile?.mkdirs()
        paths.installedMarker.writeText(
            buildString {
                appendLine("node=${RemoteCatalog.NODE_VERSION}")
                appendLine("flavor=${RemoteCatalog.NODE_FLAVOR}")
                appendLine("abi=$abi")
                appendLine("bundleSha256=${bundle.sha256}")
                appendLine("shell=${RemoteCatalog.ANDROID_SHELL_COMMIT}")
                appendLine("deps=${RemoteCatalog.RUNTIME_PACK_SHA256}")
                appendLine("core=${RemoteCatalog.CORE_COMMIT}")
            }
        )

        bundleZip.delete()
        onProgress(Progress("安装完成：FULL Node / $abi", 1f))
    }

    private fun chooseAbi(): String {
        val supported = setOf("arm64-v8a", "armeabi-v7a", "x86_64")
        return Build.SUPPORTED_ABIS.firstOrNull { it in supported }
            ?: error("Unsupported ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
    }

    private fun validateStaging(runtime: File, project: File) {
        check(File(runtime, "libnode.so").isFile) { "Missing FULL libnode.so" }
        check(File(project, "main.js").isFile) { "Missing Android main.js" }
        check(File(project, "android-server.js").isFile) { "Missing android-server.js" }
        check(File(project, "node_modules").isDirectory) { "Missing node_modules" }
        check(File(project, "danmu_api_stable/worker.js").isFile) { "Missing LogVar worker.js" }
        check(File(project, "config/.env").isFile) { "Missing writable LogVar config/.env" }
    }

    private fun makeNativeFilesLoadable(runtime: File) {
        runtime.listFiles { file -> file.isFile && file.extension == "so" }
            ?.forEach { file ->
                file.setReadable(true, false)
                file.setExecutable(true, false)
                check(file.setWritable(false, false)) { "Cannot mark ${file.name} read-only" }
            }
    }

    private fun replaceDir(staging: File, target: File) {
        val backup = File(target.parentFile, target.name + ".backup")
        backup.deleteRecursively()
        if (target.exists() && !target.renameTo(backup)) {
            error("Cannot create backup for ${target.name}")
        }
        if (!staging.renameTo(target)) {
            if (backup.exists()) backup.renameTo(target)
            error("Cannot install ${target.name}")
        }
        backup.deleteRecursively()
    }

    private fun fraction(done: Long, total: Long): Float? =
        if (total > 0L) (done.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f) else null
}
