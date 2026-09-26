package com.example.logvarremote.data.runtime

object RemoteCatalog {
    const val NODE_VERSION = "24.21.0-0"
    const val NODE_FLAVOR = "full"

    const val LOGVAR_REPOSITORY = "lilixu3/danmu_api"
    const val LOGVAR_COMMIT = "280b2327ccf06fb5e32a1db7051025359121a2c1"
    const val LOGVAR_SHORT_COMMIT = "280b2327ccf0"
    const val LOGVAR_DEPENDENCY_MODE = "per-abi"

    private const val RELEASE_TAG = "v0.6.0"
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
            sha256 = "061dfba08ce395863a13563c49b7105fc7133e175344d86779d990c7f23261db"
        ),
        "armeabi-v7a" to RuntimeArtifact(
            fileName = "logvar-runtime-armeabi-v7a-$LOGVAR_SHORT_COMMIT.zip",
            url = "$RELEASE_BASE/logvar-runtime-armeabi-v7a-$LOGVAR_SHORT_COMMIT.zip",
            sha256 = "ee39a1104def6308a05db8efb4061bb6ca6eb7a6cffc6e0f5173c58a7eee087f"
        ),
        "x86_64" to RuntimeArtifact(
            fileName = "logvar-runtime-x86_64-$LOGVAR_SHORT_COMMIT.zip",
            url = "$RELEASE_BASE/logvar-runtime-x86_64-$LOGVAR_SHORT_COMMIT.zip",
            sha256 = "79da6f780f3c3c0948fb4e4231e81582995ad5abde35ddbf6ada48a1b62a92c4"
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
