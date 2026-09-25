package com.example.logvarremote.data.runtime

object RemoteCatalog {
    const val NODE_VERSION = "24.21.0-0"
    const val NODE_FLAVOR = "full"

    const val ANDROID_SHELL_COMMIT = "4d75ca0420d2946956b34ed8c835b4912870a987"
    const val RUNTIME_PACK_SHA256 =
        "4be12e7971f079cd357496974113342f6839919a247e24b4fee69a594301c8a7"
    const val CORE_COMMIT = "280b2327ccf06fb5e32a1db7051025359121a2c1"

    private const val RELEASE_TAG = "v0.5.0"
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
            sha256 = "f8bbde8c2672ede62c558749751f267f63fa984605771ee6079564b61daf65df"
        ),
        "armeabi-v7a" to RuntimeBundle(
            abi = "armeabi-v7a",
            url = "$RELEASE_BASE/logvar-runtime-full-armeabi-v7a-$NODE_VERSION.zip",
            sha256 = "f2c7c35e5c7b9a152350f0b714753a3f0caca26eea50d502b53fe0b264f4f15b"
        ),
        "x86_64" to RuntimeBundle(
            abi = "x86_64",
            url = "$RELEASE_BASE/logvar-runtime-full-x86_64-$NODE_VERSION.zip",
            sha256 = "6c43d4a6c0218c8fbb59e299cdb84dc116819fd04a203fc75b6d1ec0e9fd4cd1"
        )
    )

    fun runtimeBundle(abi: String): RuntimeBundle =
        bundles[abi] ?: error("Unsupported ABI: $abi")

    const val PORT = 19321
    const val TOKEN = "87654321"
}
