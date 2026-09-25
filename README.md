# LogVar Remote Runtime Demo

Android Studio / Jetpack Compose / MVVM Demo，用于验证：**APK 不内置 Node.js `libnode.so`、LogVar Core 或 `node_modules`，运行时首次使用时从远程固定版本下载到 App 私有目录并启动本地 LogVar API。**

## 架构

```text
Compose UI
   ↓
HomeViewModel
   ↓
LogVarRepository
   ├─ RuntimeInstaller
   │    ├─ DownloadClient
   │    ├─ SHA-256 verify
   │    └─ ZipTools
   ├─ NodeRuntimeManager
   │    └─ NativeNodeBridge
   └─ LogVarLocalClient
        ↓
127.0.0.1:19321
```

项目采用单 Activity + Jetpack Compose + MVVM。UI 不直接处理下载、文件系统或 Node 启动；ViewModel 管理 UI state；Repository 组合 RuntimeInstaller、NodeRuntimeManager 和本地 API Client。

## APK 内容

APK 包含：

- Compose / MVVM Kotlin 代码
- 很小的 `libremote_node_bridge.so`
- NDK 提供的 `libc++_shared.so`

APK **不包含**：

- `libnode.so`
- LogVar JS Core
- Node `node_modules`

首次安装远程 Runtime 时会下载：

1. `fogtape/nodejs-mobile` Node 24.21.0-0 Android lite
2. `lilixu3/danmu-api-android` 固定 commit 的 Android bootstrap JS
3. `lilixu3/danmu-api-runtime-packs` 移动端裁剪 `node_modules.zip`
4. `lilixu3/danmu_api` 固定 commit 的 LogVar Core

版本、URL、commit 和 SHA-256 均集中在 `RemoteCatalog.kt`。

## 本地服务

Node 启动时配置：

```text
DANMU_API_HOST=127.0.0.1
DANMU_API_PORT=19321
DANMU_API_VARIANT=stable
DANMU_API_WORKER=0
DANMU_API_HOT_RELOAD=0
TOKEN=87654321
```

Demo 默认只监听 localhost。

## 构建

GitHub Actions 已验证可以成功执行：

```bash
gradle :app:assembleRelease
```

工具链：

- JDK 17
- Android SDK 36
- Build Tools 36.0.0
- NDK 28.2.13676358
- CMake 3.22.1
- Gradle 9.1.0
- AGP 9.0.1
- Kotlin / Compose compiler 2.4.0

Release Demo 暂时使用 debug signing config，方便安装测试。正式发布请改成自己的 release keystore。

## Demo 操作

1. 安装 APK。
2. 点击“下载并安装远程运行时”。
3. 等待 Node + bootstrap + node_modules + Core 安装完成。
4. 点击“启动本地 LogVar”。
5. 输入影视/番剧名和集数，调用 `/api/v2/search/episodes`。

## 注意

远程下载并执行 native / JS code 存在安全和应用商店政策风险。正式产品建议使用自有可信 CDN、签名 manifest、完整文件 hash 校验和回滚机制。
