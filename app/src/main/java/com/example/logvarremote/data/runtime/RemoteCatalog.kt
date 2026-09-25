package com.example.logvarremote.data.runtime

object RemoteCatalog {
    const val NODE_VERSION = "24.21.0-0"
    const val NODE_FLAVOR = "full"

    const val ANDROID_SHELL_COMMIT = "4d75ca0420d2946956b34ed8c835b4912870a987"
    const val RUNTIME_PACK_SHA256 =
        "4be12e7971f079cd357496974113342f6839919a247e24b4fee69a594301c8a7"
    const val CORE_COMMIT = "280b2327ccf06fb5e32a1db7051025359121a2c1"

    private const val RELEASE_TAG = "v0.5.1"
    private const val RELEASE_BASE =
        "https://github.com/Stellher/demo-test/releases/download/$RELEASE_TAG"

    data class RuntimeBundle(
        val abi: String,
        val url: String,
        val sha256: String
    )

    private val bundles = mapOf(
        "arm64-v8a" to RuntimeBundle(
            abi = "arm64-v8a",
            url = "$RELEASE_BASE/logvar-runtime-full-arm64-v8a-$NODE_VERSION.zip",
            sha256 = "db65811035a0af4af9d5f09678756393ee8e468592813caea7ffdd8577575a90"
        ),
        "armeabi-v7a" to RuntimeBundle(
            abi = "armeabi-v7a",
            url = "$RELEASE_BASE/logvar-runtime-full-armeabi-v7a-$NODE_VERSION.zip",
            sha256 = "2d581b3525acd267e47836d64bd83a4095febff909050f75648f00a87374cda2"
        ),
        "x86_64" to RuntimeBundle(
            abi = "x86_64",
            url = "$RELEASE_BASE/logvar-runtime-full-x86_64-$NODE_VERSION.zip",
            sha256 = "1027877089e6dec7fdef968e4b7d9115c6f5c260d702cbd0993ea84b0356763f"
        )
    )

    fun runtimeBundle(abi: String): RuntimeBundle =
        bundles[abi] ?: error("Unsupported ABI: $abi")

    const val PORT = 19321
    const val TOKEN = "87654321"
}
