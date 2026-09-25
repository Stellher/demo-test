# LogVar Remote Runtime Demo

一个最小的 Android Studio / Jetpack Compose / MVVM Demo，用来验证：**APK 不内置 Node.js `libnode.so`、LogVar Core 或 `node_modules`，第一次使用时从远程固定版本下载到 App 私有目录并在本机启动 LogVar API。**

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
   │    └─ NativeNodeBridge (tiny JNI bridge packaged in APK)
   └─ LogVarLocalClient
        ↓
127.0.0.1:19321
```

这是标准单 Activity Compose 项目，业务至少遵循 MVVM：UI 不直接做下载、文件操作和 Node 启动；ViewModel 只维护 UI state 和发起 use-case；Repository 组合安装器、Runtime 与本地 API client。

## APK 内有什么 / 没有什么

APK 内包含：Compose/MVVM Kotlin 代码 + `libremote_node_bridge.so`（很小的 JNI/dlopen 桥）+ 由 NDK/Gradle 自动打包的标准 `libc++_shared.so`。Node.js Mobile 的 Android `libnode.so` 使用 shared libc++，所以这部分保留在 APK 中以保证动态加载可靠。

APK **不包含**：`libnode.so`、LogVar JS Core、Node `node_modules`。仓库里也没有这些 Node/LogVar 运行时二进制或依赖包。

首次点击“下载并安装远程运行时”后，Demo 下载：

1. `fogtape/nodejs-mobile` Node 24.21.0-0 Android lite ZIP，并校验固定 SHA-256；只提取当前 ABI 的 `libnode.so` / 可选 `libc++_shared.so`。
2. `lilixu3/danmu-api-android` 固定 commit 的 6 个 Android Node bootstrap JS 文件。
3. `lilixu3/danmu-api-runtime-packs` 的移动端裁剪 `node_modules.zip`，并校验固定 SHA-256。
4. `lilixu3/danmu_api` 固定 commit 的 `danmu_api/` Core，安装成 `danmu_api_stable/`。

所有 URL / commit / hash 集中在 `RemoteCatalog.kt`。

## 关键实现

`RuntimeInstaller` 下载到临时文件，做 SHA-256（有发布 hash 的产物），Zip Slip 防护后解压到 staging；最后原子替换正式目录。远程 Native `.so` 在加载前设为只读，以兼容 Android 17+ 对动态 native code loading 的新限制。JNI bridge 采用 `c++_shared`，让 Gradle 从 NDK 正常提供 `libc++_shared.so`。

`NativeNodeBridge` 自身不链接 `libnode.so`，所以 Gradle 构建阶段完全不需要 Node binary/header。运行时使用 `dlopen()` 打开远程下载的 `libnode.so`，再解析 `node::Start(int,char**)` 入口。这个桥为了 Demo 简化，固定针对 Node 24 mobile ABI；正式产品更推荐由你自己构建一个稳定 C ABI shim，并与 Node runtime 一起发布和签名。

Node 启动时强制：

```text
DANMU_API_HOST=127.0.0.1
DANMU_API_PORT=19321
DANMU_API_VARIANT=stable
DANMU_API_WORKER=0
DANMU_API_HOT_RELOAD=0
TOKEN=87654321
```

因此 Demo 默认只监听 localhost，不给局域网开放。

## Android Studio 构建

要求：JDK 17、Android SDK 36、Build Tools 36.0.0、NDK 28.2.13676358、CMake 3.22.1、Gradle 9.1.0。

项目使用 AGP 9.0.1 + built-in Kotlin；为了与 Compose compiler 2.4.0 对齐，根 build script 显式把 KGP runtime 提升到 2.4.0。

命令行：

```bash
gradle :app:assembleRelease
```

Release 为了 Demo 可直接安装，暂时复用 debug keystore。**正式发布必须换成你自己的 release keystore。**

也提供 `.github/workflows/build-release.yml`，推到 GitHub 后可以手动触发或在 `main` push 时编译并上传 release APK artifact。

## Demo 操作

1. 安装 App。
2. 点击“下载并安装远程运行时”。
3. 等待 Node + bootstrap + node_modules + Core 下载/安装完成。
4. 点击“启动本地 LogVar”。
5. 输入番剧名、集数，调用本机 `/api/v2/search/episodes` 查看原始返回。

## 注意

- 这是验证远程 Runtime 架构的 Demo，不是完整 LogVar Android 客户端。
- 从远程下载并执行 native/JS code 有安全和 Google Play 政策风险；正式环境建议改成你自己的可信 CDN + manifest 数字签名 + 每个文件 hash + 回滚机制。
- 目前 Node runtime 的 SHA-256 和 runtime dependency pack SHA-256 固定校验；raw bootstrap/core 用不可变 Git commit 固定版本。生产环境建议把它们也打成你自己的签名发布包。
