package com.example.logvarremote.data.repository

import android.content.Context
import com.example.logvarremote.data.api.LogVarLocalClient
import com.example.logvarremote.data.runtime.NodeRuntimeManager
import com.example.logvarremote.data.runtime.RemoteCatalog
import com.example.logvarremote.data.runtime.RuntimeInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class LogVarRepository(context: Context) {
    private val appContext = context.applicationContext
    private val installer = RuntimeInstaller(appContext)
    private val runtimeManager = NodeRuntimeManager(appContext)
    private val api = LogVarLocalClient()

    fun isInstalled(): Boolean = installer.isInstalled()

    suspend fun install(onProgress: (RuntimeInstaller.Progress) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { installer.install(onProgress) }
        }

    suspend fun startAndWait(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (api.health()) return@runCatching
            runtimeManager.start().getOrThrow()
            repeat(40) {
                if (api.health()) return@runCatching
                delay(250)
            }
            error("Node started but LogVar did not become healthy on 127.0.0.1:${RemoteCatalog.PORT}")
        }
    }

    suspend fun searchEpisode(anime: String, episode: Int): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching { api.searchEpisode(anime, episode) }
        }
}
