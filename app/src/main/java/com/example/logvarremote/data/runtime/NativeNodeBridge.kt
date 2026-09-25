package com.example.logvarremote.data.runtime

object NativeNodeBridge {
    init {
        System.loadLibrary("remote_node_bridge")
    }

    external fun startNode(
        libNodePath: String,
        libcxxPath: String?,
        projectDir: String,
        entryScript: String,
        port: Int,
        token: String
    ): Int

    external fun lastError(): String
}
