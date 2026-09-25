package com.example.logvarremote.data.runtime

import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NodeRuntimeManager(context: Context) {
    private val paths = RuntimePaths(context.applicationContext)
    private val starting = AtomicBoolean(false)

    fun isRunningOrStarting(): Boolean = starting.get()

    suspend fun start(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            check(paths.isInstalled()) { "Runtime is not installed" }

            // LogVar NodeHandler saves Web-panel environment variables here.
            // Older demo installs only created config/, so ensure .env exists before Node starts.
            paths.ensureRuntimeConfigFile()

            check(starting.compareAndSet(false, true)) { "Node is already running/starting in this app process" }

            val libcxx = paths.libcxxLib.takeIf { it.isFile }?.absolutePath
            Thread({
                val rc = NativeNodeBridge.startNode(
                    libNodePath = paths.nodeLib.absolutePath,
                    libcxxPath = libcxx,
                    projectDir = paths.projectDir.absolutePath,
                    entryScript = paths.mainJs.absolutePath,
                    port = RemoteCatalog.PORT,
                    token = RemoteCatalog.TOKEN
                )
                starting.set(false)
                android.util.Log.i("NodeRuntimeManager", "node::Start returned $rc: ${NativeNodeBridge.lastError()}")
            }, "logvar-node").apply {
                isDaemon = true
                start()
            }
            Unit
        }
    }
}
