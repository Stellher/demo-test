package com.example.logvarremote.data.runtime

object RemoteCatalog {
    const val NODE_VERSION = "24.21.0-0"
    const val NODE_URL =
        "https://github.com/fogtape/nodejs-mobile/releases/download/v24.21.0-0/" +
            "nodejs-mobile-android-lite-24.21.0-0.zip"
    const val NODE_SHA256 =
        "1424d88050fcf1e114afc2408aa504dc9ce6043e86605bc3ce0b2cd3f9d04d9c"

    const val ANDROID_SHELL_COMMIT = "4d75ca0420d2946956b34ed8c835b4912870a987"
    const val ANDROID_SHELL_RAW_BASE =
        "https://raw.githubusercontent.com/lilixu3/danmu-api-android/" +
            "$ANDROID_SHELL_COMMIT/app/src/main/assets/nodejs-project"
    val ANDROID_BOOTSTRAP_FILES = listOf(
        "main.js",
        "android-server.js",
        "favorite-scheduler-host.js",
        "runtime-polyfills.js",
        "startup-failure.js",
        "worker-proxy.js"
    )

    const val RUNTIME_PACK_URL =
        "https://github.com/lilixu3/danmu-api-runtime-packs/releases/download/" +
            "runtime-dependencies-4be12e7971f0/node_modules.zip"
    const val RUNTIME_PACK_SHA256 =
        "4be12e7971f079cd357496974113342f6839919a247e24b4fee69a594301c8a7"

    const val CORE_COMMIT = "280b2327ccf06fb5e32a1db7051025359121a2c1"
    const val CORE_URL = "https://codeload.github.com/lilixu3/danmu_api/zip/$CORE_COMMIT"

    const val PORT = 19321
    const val TOKEN = "87654321"
}
