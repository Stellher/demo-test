# LogVar Remote Runtime Demo

一个最小的 Android Studio / Jetpack Compose / MVVM Demo，用来验证：**APK 不内置 Node.js `libnode.so`、LogVar Core 或 `node_modules`，第一次使用时从远程固定版本下载到 App 私有目录并在本机启动 LogVar API。**

> CI: GitHub Actions builds the release APK from `main`.
