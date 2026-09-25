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
        paths.ensureBaseDirs()

        val nodeZip = File(paths.downloadDir, "node-${RemoteCatalog.NODE_VERSION}.zip")
        onProgress(Progress("下载 Node.js Mobile ($abi)", 0f))
        downloader.download(RemoteCatalog.NODE_URL, nodeZip) { done, total ->
            onProgress(Progress("下载 Node.js Mobile ($abi)", fraction(done, total)))
        }
        onProgress(Progress("校验 Node.js SHA-256"))
        FileIntegrity.requireSha256(nodeZip, RemoteCatalog.NODE_SHA256)

        val runtimeStaging = File(paths.runtimeDir.parentFile, "runtime.staging")
        runtimeStaging.deleteRecursively()
        runtimeStaging.mkdirs()
        onProgress(Progress("提取 $abi/libnode.so"))
        ZipTools.extractSelectedNativeLibraries(nodeZip, abi, runtimeStaging)

        val projectStaging = File(paths.projectDir.parentFile, "project.staging")
        projectStaging.deleteRecursively()
        projectStaging.mkdirs()

        RemoteCatalog.ANDROID_BOOTSTRAP_FILES.forEachIndexed { index, name ->
            onProgress(
                Progress(
                    "下载 Android 启动层：$name",
                    index.toFloat() / RemoteCatalog.ANDROID_BOOTSTRAP_FILES.size.toFloat()
                )
            )
            downloader.download(
                "${RemoteCatalog.ANDROID_SHELL_RAW_BASE}/$name",
                File(projectStaging, name)
            )
        }

        val dependenciesZip = File(paths.downloadDir, "node_modules-4be12e7971f0.zip")
        onProgress(Progress("下载移动端 node_modules", 0f))
        downloader.download(RemoteCatalog.RUNTIME_PACK_URL, dependenciesZip) { done, total ->
            onProgress(Progress("下载移动端 node_modules", fraction(done, total)))
        }
        onProgress(Progress("校验 node_modules SHA-256"))
        FileIntegrity.requireSha256(dependenciesZip, RemoteCatalog.RUNTIME_PACK_SHA256)
        onProgress(Progress("提取 node_modules"))
        ZipTools.extractAll(dependenciesZip, projectStaging)

        val coreZip = File(paths.downloadDir, "logvar-core-${RemoteCatalog.CORE_COMMIT.take(8)}.zip")
        onProgress(Progress("下载 LogVar Core", 0f))
        downloader.download(RemoteCatalog.CORE_URL, coreZip) { done, total ->
            onProgress(Progress("下载 LogVar Core", fraction(done, total)))
        }
        onProgress(Progress("提取 LogVar Core"))
        ZipTools.extractAfterMarker(
            coreZip,
            "/danmu_api/",
            File(projectStaging, "danmu_api_stable")
        )
        File(projectStaging, "tmp").mkdirs()
        File(projectStaging, "config").mkdirs()

        validateStaging(runtimeStaging, projectStaging)
        makeNativeFilesLoadable(runtimeStaging)

        onProgress(Progress("原子替换已安装运行时"))
        replaceDir(runtimeStaging, paths.runtimeDir)
        replaceDir(projectStaging, paths.projectDir)

        paths.installedMarker.parentFile?.mkdirs()
        paths.installedMarker.writeText(
            buildString {
                appendLine("node=${RemoteCatalog.NODE_VERSION}")
                appendLine("abi=$abi")
                appendLine("shell=${RemoteCatalog.ANDROID_SHELL_COMMIT}")
                appendLine("deps=${RemoteCatalog.RUNTIME_PACK_SHA256}")
                appendLine("core=${RemoteCatalog.CORE_COMMIT}")
            }
        )

        nodeZip.delete()
        dependenciesZip.delete()
        coreZip.delete()
        onProgress(Progress("安装完成", 1f))
    }

    private fun chooseAbi(): String {
        val supported = setOf("arm64-v8a", "armeabi-v7a", "x86_64")
        return Build.SUPPORTED_ABIS.firstOrNull { it in supported }
            ?: error("Unsupported ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
    }

    private fun validateStaging(runtime: File, project: File) {
        check(File(runtime, "libnode.so").isFile) { "Missing libnode.so" }
        check(File(project, "main.js").isFile) { "Missing Android main.js" }
        check(File(project, "android-server.js").isFile) { "Missing android-server.js" }
        check(File(project, "node_modules").isDirectory) { "Missing node_modules" }
        check(File(project, "danmu_api_stable/worker.js").isFile) { "Missing LogVar worker.js" }
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
