package com.example.logvarremote.data.runtime

import android.content.Context
import java.io.File

class RuntimePaths(context: Context) {
    companion object {
        const val DEFAULT_SOURCE_ORDER =
            "360,vod,tmdb,douban,tencent,youku,iqiyi,imgo,bilibili,renren,hanjutv,dandan,migu"
        const val DEFAULT_RATE_LIMIT_MAX_REQUESTS = "0"

        private val HOST_LAUNCHER = """
            import net from 'node:net';

            const originalListen = net.Server.prototype.listen;
            const configuredProxyPort = Number.parseInt(
              process.env.DANMU_API_PROXY_PORT || '19322',
              10
            );

            function rewriteListenArgs(args) {
              if (!args.length) return args;

              const first = args[0];

              if (first && typeof first === 'object' && !Array.isArray(first)) {
                const options = { ...first };
                options.host = '127.0.0.1';
                delete options.ipv6Only;

                if (options.port === 5321 && Number.isFinite(configuredProxyPort)) {
                  options.port = configuredProxyPort;
                }

                args[0] = options;
                return args;
              }

              if (typeof first === 'number') {
                if (first === 5321 && Number.isFinite(configuredProxyPort)) {
                  args[0] = configuredProxyPort;
                }

                if (typeof args[1] === 'string') {
                  args[1] = '127.0.0.1';
                } else {
                  args.splice(1, 0, '127.0.0.1');
                }
              }

              return args;
            }

            net.Server.prototype.listen = function (...args) {
              return originalListen.apply(this, rewriteListenArgs(args));
            };

            await import('../logvar/danmu_api/server.js');
        """.trimIndent() + "\n"
    }

    private val root = File(context.filesDir, "remote_logvar")

    val runtimeDir = File(root, "runtime")
    val logvarDir = File(root, "logvar")
    val hostDir = File(root, "host")
    val downloadDir = File(root, "downloads")

    val configDir = File(logvarDir, "config")
    val envFile = File(configDir, ".env")
    val legacyEnvFile = File(root, "project/config/.env")

    val nodeLib = File(runtimeDir, "libnode.so")
    val libcxxLib = File(runtimeDir, "libc++_shared.so")
    val hostEntry = File(hostDir, "main.mjs")
    val upstreamServer = File(logvarDir, "danmu_api/server.js")
    val packageJson = File(logvarDir, "package.json")
    val nodeModulesDir = File(logvarDir, "node_modules")

    val installedMarker = File(root, ".installed-v6-node-logvar")

    fun isInstalled(): Boolean =
        nodeLib.isFile &&
            hostEntry.isFile &&
            upstreamServer.isFile &&
            packageJson.isFile &&
            nodeModulesDir.isDirectory &&
            installedMarker.isFile

    fun ensureBaseDirs() {
        root.mkdirs()
        runtimeDir.mkdirs()
        logvarDir.mkdirs()
        hostDir.mkdirs()
        downloadDir.mkdirs()
        configDir.mkdirs()
    }

    fun existingEnvFile(): File? = when {
        envFile.isFile -> envFile
        legacyEnvFile.isFile -> legacyEnvFile
        else -> null
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

    fun writeHostLauncher() {
        hostDir.mkdirs()
        hostEntry.writeText(HOST_LAUNCHER)
        check(hostEntry.isFile && hostEntry.canRead()) {
            "Host launcher is not readable: ${hostEntry.absolutePath}"
        }
    }
}
