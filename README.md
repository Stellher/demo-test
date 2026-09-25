# LogVar Remote Runtime Demo

Android Studio / Jetpack Compose / MVVM Demo。Node.js Mobile、LogVar Core 与 Node 依赖均采用远程 Runtime 架构。

## Runtime 发行策略

从 **v0.5.0** 起，三个 Android ABI 完全平级：

- `arm64-v8a`
- `armeabi-v7a`
- `x86_64`

没有“主 ABI”。每个 ABI 都独立发布：

- `node-runtime-full-<abi>-24.21.0-0.zip`
- `logvar-runtime-full-<abi>-24.21.0-0.zip`

三个 ABI 统一使用完全相同的依赖集合：

```text
Node.js Mobile  FULL 24.21.0-0
Bootstrap       4d75ca04
node_modules    4be12e7971f0
LogVar Core     280b2327
```

只有 Native Node Runtime 的 ABI 不同；Bootstrap、node_modules、LogVar Core 均为 ABI-independent，并且由同一套确定性打包流程生成。

## GitHub Release

每一个源码 Tag 都对应一个 GitHub Release。Release 包含：

```text
LogVarRemoteDemo-vX.Y.Z.apk

node-runtime-full-arm64-v8a-24.21.0-0.zip
node-runtime-full-armeabi-v7a-24.21.0-0.zip
node-runtime-full-x86_64-24.21.0-0.zip

logvar-runtime-full-arm64-v8a-24.21.0-0.zip
logvar-runtime-full-armeabi-v7a-24.21.0-0.zip
logvar-runtime-full-x86_64-24.21.0-0.zip

logvar-android-bootstrap-4d75ca04.zip
logvar-node-modules-4be12e7971f0.zip
logvar-core-280b2327.zip

remote-deps-manifest.json
SHA256SUMS.txt
```

GitHub 自身还会为每个 Tag 自动提供 Source code ZIP / tar.gz。

历史版本 `v0.1.0 ~ v0.4.0` 使用补档 workflow 建立对应 Release，并按各版本当时的 Runtime 体系归档，不用当前版本依赖冒充历史依赖。

## App 下载逻辑

App 根据 `Build.SUPPORTED_ABIS` 选择当前设备支持的 ABI：

```text
Build.SUPPORTED_ABIS
       ↓
选择 arm64-v8a / armeabi-v7a / x86_64
       ↓
下载该 ABI 对应的 FULL LogVar Runtime Bundle
       ↓
SHA-256 校验
       ↓
解压 runtime/ + project/
       ↓
dlopen(libnode.so)
       ↓
启动本地 LogVar
```

每台设备只下载自己 ABI 的 Native Runtime，不下载另外两个 ABI。

## Bundle 结构

```text
logvar-runtime-full-<abi>-24.21.0-0.zip
├── runtime/
│   └── libnode.so
└── project/
    ├── main.js
    ├── android-server.js
    ├── favorite-scheduler-host.js
    ├── runtime-polyfills.js
    ├── startup-failure.js
    ├── worker-proxy.js
    ├── node_modules/
    ├── danmu_api_stable/
    ├── config/.env
    └── tmp/
```

## 功能

- Compose + MVVM。
- 远程 Node / LogVar Runtime。
- FULL Node.js Mobile。
- 全 LogVar API 调试器。
- App 内 LogVar Web 面板。
- WebView 文件选择 / 本地弹幕上传。
- 环境变量持久化。
- ABI 独立 Runtime 下载。
- SHA-256 完整性验证。
- 确定性 ZIP 构建。
- Git Tag / Release 自动发布与历史版本归档。

## 本地服务

```text
Main API:  http://127.0.0.1:19321
Proxy:     http://127.0.0.1:19322
Token:     87654321
```

Runtime 只监听 localhost。

## 构建工具链

- JDK 17
- Android SDK 36
- Build Tools 36.0.0
- NDK 28.2.13676358
- CMake 3.22.1
- Gradle 9.1.0
- AGP 9.0.1
- Kotlin / Compose 2.4.0

Release Demo 当前仍使用 debug signing config，正式发布时请替换为自己的 release keystore。

## v0.5.1 Android Web 面板配置

- 环境变量配置支持 JSON 导入与导出。Android WebView 导出通过系统文件保存器（Storage Access Framework）写入 JSON，不依赖 `blob:` 下载。
- 默认 `SOURCE_ORDER`：`360,vod,tmdb,douban,tencent,youku,iqiyi,imgo,bilibili,renren,hanjutv,dandan,migu`。
- 默认 `RATE_LIMIT_MAX_REQUESTS=0`，即关闭每 IP 分钟限流。
- 升级时已有 `.env` 配置会保留，仅给缺失的上述两个变量补默认值。
