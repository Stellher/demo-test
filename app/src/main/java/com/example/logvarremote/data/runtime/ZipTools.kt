package com.example.logvarremote.data.runtime

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object ZipTools {
    fun extractSelectedNativeLibraries(zip: File, abi: String, outputDir: File) {
        outputDir.mkdirs()
        var foundNode = false

        ZipInputStream(zip.inputStream().buffered()).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                val normalized = entry.name.replace('\\', '/')
                if (!entry.isDirectory && normalized.contains("/$abi/")) {
                    val fileName = normalized.substringAfterLast('/')
                    if (fileName == "libnode.so" || fileName == "libc++_shared.so") {
                        val target = safeTarget(outputDir, fileName)
                        FileOutputStream(target).use { output -> input.copyTo(output, 128 * 1024) }
                        if (fileName == "libnode.so") foundNode = true
                    }
                }
                input.closeEntry()
            }
        }

        check(foundNode) { "libnode.so for ABI $abi was not found in Node archive" }
    }

    fun extractAfterMarker(zip: File, marker: String, outputDir: File) {
        outputDir.mkdirs()
        var extracted = 0
        val normalizedMarker = marker.replace('\\', '/')

        ZipInputStream(zip.inputStream().buffered()).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                val normalized = entry.name.replace('\\', '/')
                val index = normalized.indexOf(normalizedMarker)
                if (index >= 0) {
                    val relative = normalized.substring(index + normalizedMarker.length)
                    if (relative.isNotBlank()) {
                        val target = safeTarget(outputDir, relative)
                        if (entry.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            FileOutputStream(target).use { output -> input.copyTo(output, 128 * 1024) }
                            extracted++
                        }
                    }
                }
                input.closeEntry()
            }
        }
        check(extracted > 0) { "No files found after marker: $marker" }
    }

    fun extractAll(zip: File, outputDir: File) {
        outputDir.mkdirs()
        var extracted = 0
        ZipInputStream(zip.inputStream().buffered()).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                val normalized = entry.name.replace('\\', '/')
                if (normalized.isNotBlank()) {
                    val target = safeTarget(outputDir, normalized)
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { output -> input.copyTo(output, 128 * 1024) }
                        extracted++
                    }
                }
                input.closeEntry()
            }
        }
        check(extracted > 0) { "Archive is empty: ${zip.name}" }
    }

    private fun safeTarget(root: File, relative: String): File {
        val target = File(root, relative)
        val rootPath = root.canonicalFile.toPath()
        val targetPath = target.canonicalFile.toPath()
        require(targetPath.startsWith(rootPath)) { "Blocked Zip Slip entry: $relative" }
        return target
    }
}
