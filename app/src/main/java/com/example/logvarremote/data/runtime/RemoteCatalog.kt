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
            sha256 = "2425a8f767ad9ea2216f7ea98e77602deb87dadb59003f767c87373f2fe4656a"
        ),
        "armeabi-v7a" to RuntimeArtifact(
            fileName = "logvar-runtime-armeabi-v7a-$LOGVAR_SHORT_COMMIT.zip",
            url = "$RELEASE_BASE/logvar-runtime-armeabi-v7a-$LOGVAR_SHORT_COMMIT.zip",
            sha256 = "4a23b49231123fb3af627023cc43b2c4121b181cb3f17087a5b658a9162d2241"
        ),
        "x86_64" to RuntimeArtifact(
            fileName = "logvar-runtime-x86_64-$LOGVAR_SHORT_COMMIT.zip",
            url = "$RELEASE_BASE/logvar-runtime-x86_64-$LOGVAR_SHORT_COMMIT.zip",
            sha256 = "196aea9fd0b73296e6f788145fa3ad6a317c6c6258ec6fa9286dbbb003089796"
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
