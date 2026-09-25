# LogVar Remote Runtime Demo

Android Studio / Jetpack Compose / MVVM Demo：APK 不内置 Node.js `libnode.so`、LogVar Core 或 `node_modules`，首次使用时从固定远程版本下载到 App 私有目录并启动本地 LogVar。

## 当前功能

- 远程下载并校验 Node.js Mobile、LogVar Core、Android bootstrap 与移动端 node_modules。
- App 内启动 `127.0.0.1:19321` 的 LogVar 主服务与 `19322` 代理端口。
- Compose **全 API 调试器**：当前目录覆盖 47 个路由操作，包括 Runtime、业务搜索/匹配/弹幕、FongMi/短地址兼容、收藏、本地弹幕 CRUD、日志/请求记录、缓存、环境变量、Cookie、AI、Forward Trace 和 Proxy。
- App 内 **LogVar 官方本地 Web 面板**：WebView 打开 Core 自带 `danmu_api/ui`，支持 JavaScript、DOM Storage 和文件选择，因此本地弹幕 multipart 上传也可直接操作。
- 本地 Demo 将 `ADMIN_TOKEN` 与 `TOKEN` 对齐，仅监听 `127.0.0.1`，便于完整调试系统管理接口。

## 架构

```text
Compose UI
   ├─ Runtime
   ├─ Full API Debugger
   └─ LocalPanelActivity (WebView)
          ↓
HomeViewModel
          ↓
LogVarRepository
   ├─ RuntimeInstaller
   ├─ NodeRuntimeManager
   ├─ ApiDebugClient
   └─ LogVarLocalClient
          ↓
127.0.0.1:19321 / 19322
```

## API 调试器

路由来源按当前固定版本核对：

- `lilixu3/danmu_api` commit `280b2327ccf06fb5e32a1db7051025359121a2c1`
- `lilixu3/danmu-api-android` commit `4d75ca0420d2946956b34ed8c835b4912870a987`

每个 API 可编辑 Path、Query 和 JSON Body，并显示实际请求 URL、HTTP 状态码、耗时、响应头和响应体。

本地弹幕上传是 `multipart/form-data`，Compose 调试器会列出该路由；实际文件上传直接进入本地 Web 面板完成。

## 本地面板

Node 启动后点击 **打开 LogVar 本地面板**：

```text
http://127.0.0.1:19321/87654321/
```

这是 LogVar Core 自带的 UI，不是另外伪造的静态页面。

## 构建

GitHub Actions 已验证：

```bash
gradle :app:assembleRelease
```

工具链：JDK 17、SDK 36、Build Tools 36.0.0、NDK 28.2.13676358、CMake 3.22.1、Gradle 9.1.0、AGP 9.0.1、Kotlin/Compose 2.4.0。

Release Demo 暂时使用 debug signing config，正式发布请替换为自己的 release keystore。
