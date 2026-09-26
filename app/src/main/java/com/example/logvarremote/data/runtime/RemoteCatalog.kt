package com.example.logvarremote.data.runtime

object RemoteCatalog {
    const val NODE_VERSION = "24.21.0-0"
    const val NODE_FLAVOR = "full"

    const val LOGVAR_REPOSITORY = "huangxd-/danmu_api"
    const val LOGVAR_COMMIT = "fc1b7ff6add61d8af24c9bf978253273833f5afc"
    const val LOGVAR_SHORT_COMMIT = "fc1b7ff6add6"
    const val LOGVAR_DEPENDENCY_MODE = "per-abi"

    private const val RELEASE_TAG = "v0.6.1"
    private const val RELEASE_BASE =
        "https://github.com/Stellher/demo-test/releases/download/$RELEASE_TAG"

    data class RuntimeArtifact(
        val fileName: String,
        val url: String,
        val sha256: String
    )

    private val nodeArtifacts = mapOf(
        "arm64-v8a" to RuntimeArtifact(
            fileName = "node-runtime-full-arm64-v8a-$NODE_VERSION.zip",
            url = "$RELEASE_BASE/node-runtime-full-arm64-v8a-$NODE_VERSION.zip",
            sha256 = "1b135c530255f3c519b2100254002264701ef08fde3d6309a3f4b54e6f6e6c01"
        ),
        "armeabi-v7a" to RuntimeArtifact(
            fileName = "node-runtime-full-armeabi-v7a-$NODE_VERSION.zip",
            url = "$RELEASE_BASE/node-runtime-full-armeabi-v7a-$NODE_VERSION.zip",
            sha256 = "b3681a48033e45e9ffcc6a223a02e773f59ad91aa6bcecabed613a1256bb2ff3"
        ),
        "x86_64" to RuntimeArtifact(
            fileName = "node-runtime-full-x86_64-$NODE_VERSION.zip",
            url = "$RELEASE_BASE/node-runtime-full-x86_64-$NODE_VERSION.zip",
            sha256 = "10dccfc5ea44b6e624a9a579bee7be6d11f3f7c377b75e143ec6a8a516df0e53"
        )
    )

    private val logvarArtifacts = mapOf(
        "arm64-v8a" to RuntimeArtifact(
            fileName = "logvar-runtime-arm64-v8a-$LOGVAR_SHORT_COMMIT.zip",
            url = "$RELEASE_BASE/logvar-runtime-arm64-v8a-$LOGVAR_SHORT_COMMIT.zip",
            sha256 = "9257d3d4849a6be50f54bff02dbb08dbfd68de47601eac2b07ee0788ecf69be5"
        ),
        "armeabi-v7a" to RuntimeArtifact(
            fileName = "logvar-runtime-armeabi-v7a-$LOGVAR_SHORT_COMMIT.zip",
            url = "$RELEASE_BASE/logvar-runtime-armeabi-v7a-$LOGVAR_SHORT_COMMIT.zip",
            sha256 = "6bde512786b1832aa40486ce63b6e9128cf3ab3f91a4c09f49f87cf9df1d7a5c"
        ),
        "x86_64" to RuntimeArtifact(
            fileName = "logvar-runtime-x86_64-$LOGVAR_SHORT_COMMIT.zip",
            url = "$RELEASE_BASE/logvar-runtime-x86_64-$LOGVAR_SHORT_COMMIT.zip",
            sha256 = "0318d4195c9b67d007ca48628fae97044813b0dd2870e4f2ff2a07603ac001d1"
        )
    )

    fun nodeRuntime(abi: String): RuntimeArtifact =
        nodeArtifacts[abi] ?: error("Unsupported Node ABI: $abi")

    fun logvarRuntime(abi: String): RuntimeArtifact =
        when (LOGVAR_DEPENDENCY_MODE) {
            "universal" -> logvarArtifacts["universal"]
                ?: error("Missing universal LogVar runtime")
            "per-abi" -> logvarArtifacts[abi]
                ?: error("Unsupported LogVar ABI: $abi")
            else -> error("Unknown LogVar dependency mode: $LOGVAR_DEPENDENCY_MODE")
        }

    const val PORT = 19321
    const val PROXY_PORT = 19322
    const val TOKEN = "87654321"
}
