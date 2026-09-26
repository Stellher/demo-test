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
        val nodeArtifact = RemoteCatalog.nodeRuntime(abi)
        val logvarArtifact = RemoteCatalog.logvarRuntime(abi)

        paths.ensureBaseDirs()

        val nodeZip = File(paths.downloadDir, nodeArtifact.fileName)
        val logvarZip = File(paths.downloadDir, logvarArtifact.fileName)

        onProgress(Progress("下载 Node.js Mobile FULL ($abi)", 0f))
        downloader.download(nodeArtifact.url, nodeZip) { done, total ->
            onProgress(Progress("下载 Node.js Mobile FULL ($abi)", fraction(done, total)))
        }

        onProgress(Progress("校验 Node Runtime SHA-256"))
        FileIntegrity.requireSha256(nodeZip, nodeArtifact.sha256)

        val logvarTarget = if (RemoteCatalog.LOGVAR_DEPENDENCY_MODE == "universal") {
            "universal"
        } else {
            abi
        }
        onProgress(Progress("下载原版 LogVar Runtime ($logvarTarget)", 0f))
        downloader.download(logvarArtifact.url, logvarZip) { done, total ->
            onProgress(Progress("下载原版 LogVar Runtime ($logvarTarget)", fraction(done, total)))
        }

        onProgress(Progress("校验 LogVar Runtime SHA-256"))
        FileIntegrity.requireSha256(logvarZip, logvarArtifact.sha256)

        val stagingRoot = File(paths.downloadDir.parentFile, "install-v6.staging")
        stagingRoot.deleteRecursively()
        stagingRoot.mkdirs()

        onProgress(Progress("解压 Node + LogVar"))
        ZipTools.extractAll(nodeZip, stagingRoot)
        ZipTools.extractAll(logvarZip, stagingRoot)

        val runtimeStaging = File(stagingRoot, "runtime")
        val logvarStaging = File(stagingRoot, "logvar")

        val stagingEnvFile = File(logvarStaging, "config/.env")
        stagingEnvFile.parentFile?.mkdirs()
        paths.existingEnvFile()?.copyTo(stagingEnvFile, overwrite = true)
        paths.ensureRuntimeConfigDefaults(stagingEnvFile)

        File(logvarStaging, "tmp").mkdirs()

        validateStaging(runtimeStaging, logvarStaging)
        makeNativeFilesLoadable(runtimeStaging)

        onProgress(Progress("原子替换 Node + LogVar Runtime"))
        replaceRuntimePair(runtimeStaging, logvarStaging)
        stagingRoot.deleteRecursively()

        paths.writeHostLauncher()

        paths.installedMarker.parentFile?.mkdirs()
        paths.installedMarker.writeText(
            buildString {
                appendLine("schema=6")
                appendLine("abi=$abi")
                appendLine("nodeVersion=${RemoteCatalog.NODE_VERSION}")
                appendLine("nodeFlavor=${RemoteCatalog.NODE_FLAVOR}")
                appendLine("nodeSha256=${nodeArtifact.sha256}")
                appendLine("logvarRepository=${RemoteCatalog.LOGVAR_REPOSITORY}")
                appendLine("logvarCommit=${RemoteCatalog.LOGVAR_COMMIT}")
                appendLine("logvarDependencyMode=${RemoteCatalog.LOGVAR_DEPENDENCY_MODE}")
                appendLine("logvarSha256=${logvarArtifact.sha256}")
                appendLine("logvarSourceModified=false")
            }
        )

        nodeZip.delete()
        logvarZip.delete()
        onProgress(Progress("安装完成：Node + 原版 LogVar / $abi", 1f))
    }

    private fun chooseAbi(): String {
        val supported = setOf("arm64-v8a", "armeabi-v7a", "x86_64")
        return Build.SUPPORTED_ABIS.firstOrNull { it in supported }
            ?: error("Unsupported ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
    }

    private fun validateStaging(runtime: File, logvar: File) {
        check(File(runtime, "libnode.so").isFile) { "Missing FULL libnode.so" }
        check(File(logvar, "package.json").isFile) { "Missing upstream package.json" }
        check(File(logvar, "danmu_api/server.js").isFile) { "Missing upstream danmu_api/server.js" }
        check(File(logvar, "danmu_api/worker.js").isFile) { "Missing upstream danmu_api/worker.js" }
        check(File(logvar, "node_modules").isDirectory) { "Missing resolved node_modules" }
        check(File(logvar, "config/.env").isFile) { "Missing writable LogVar config/.env" }
    }

    private fun makeNativeFilesLoadable(runtime: File) {
        runtime.listFiles { file -> file.isFile && file.extension == "so" }
            ?.forEach { file ->
                file.setReadable(true, false)
                file.setExecutable(true, false)
                check(file.setWritable(false, false)) {
                    "Cannot mark ${file.name} read-only"
                }
            }
    }

    private fun replaceRuntimePair(runtimeStaging: File, logvarStaging: File) {
        val entries = listOf(
            Triple(runtimeStaging, paths.runtimeDir, File(paths.runtimeDir.parentFile, "runtime.backup")),
            Triple(logvarStaging, paths.logvarDir, File(paths.logvarDir.parentFile, "logvar.backup"))
        )

        entries.forEach { (_, target, backup) ->
            backup.deleteRecursively()
            if (target.exists() && !target.renameTo(backup)) {
                error("Cannot create backup for ${target.name}")
            }
        }

        val installed = mutableListOf<File>()
        try {
            entries.forEach { (staging, target, _) ->
                if (!staging.renameTo(target)) {
                    error("Cannot install ${target.name}")
                }
                installed += target
            }
        } catch (error: Throwable) {
            installed.asReversed().forEach { it.deleteRecursively() }
            entries.forEach { (_, target, backup) ->
                if (backup.exists()) {
                    backup.renameTo(target)
                }
            }
            throw error
        }

        entries.forEach { (_, _, backup) -> backup.deleteRecursively() }
    }

    private fun fraction(done: Long, total: Long): Float? =
        if (total > 0L) {
            (done.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
        } else {
            null
        }
}
