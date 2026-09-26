# LogVar Remote Runtime Demo

Android Studio / Jetpack Compose / MVVM Demo。APK 只保留 Android 宿主、JNI Bridge 与 C++ Runtime；Node.js Mobile 和 LogVar 作为远程 Runtime 下载。

## v0.6 Runtime 模型

从 **v0.6.0** 起，远程 Runtime 收敛为两个核心组件：

```text
Android APK
├── libremote_node_bridge.so
├── libc++_shared.so
├── Runtime Installer
├── WebView / Compose UI
└── Android Host Shim
        │
        ├── Node Runtime
        │   └── libnode.so
        │
        └── LogVar Runtime
            ├── 完整上游 LogVar 源码
            └── node_modules
```

服务端以后只需要控制：

```text
nodeVersion
logvarVersion
```

不再把 Bootstrap、LogVar Core 和 node_modules 作为三个独立的客户端版本。

## Node Runtime

Node.js Mobile 使用 **FULL 24.21.0-0**，三个 Android ABI 完全平级并独立发布：

- `arm64-v8a`
- `armeabi-v7a`
- `x86_64`

Release 资产：

```text
node-runtime-full-arm64-v8a-24.21.0-0.zip
node-runtime-full-armeabi-v7a-24.21.0-0.zip
node-runtime-full-x86_64-24.21.0-0.zip
```

每个 ZIP 只包含当前 ABI 的：

```text
runtime/
└── libnode.so
```

APK 自己提供 `libc++_shared.so`，远程 Node 包不重复携带它。

## LogVar Runtime

LogVar Runtime 直接使用上游仓库：

```text
lilixu3/danmu_api
```

稳定版本由 `.runtime/logvar-upstream.json` 锁定到明确 commit。

核心原则：

- 上游源码 **不修改**。
- 不 patch `envs.js`。
- 不 patch `systemsettings.js`。
- 不裁剪 LogVar 源码。
- ZIP 中保存完整上游源码 snapshot（仅排除 Git 元数据）。
- `node_modules` 根据上游自己的 `package.json` 解析并加入 Runtime ZIP。
- dependency lock 存在本仓库的 `.runtime/locks/` 中，不写入上游源码目录。

LogVar Runtime 结构：

```text
logvar-runtime-<target>-<commit>.zip
└── logvar/
    ├── package.json
    ├── danmu_api/
    ├── config/
    ├── config_example/
    ├── ...上游其他文件
    └── node_modules/
```

## node_modules ABI 策略

GitHub Actions 会针对三个 Android ABI 分别解析依赖：

```text
arm64-v8a
armeabi-v7a
x86_64
```

然后扫描 `node_modules` 中的 `*.node`、ELF 和平台相关内容。

如果三个依赖树完全相同且没有 Native/平台二进制：

```text
dependencyMode = universal
```

只发布：

```text
logvar-runtime-universal-<commit>.zip
```

如果依赖树存在 ABI/平台差异：

```text
dependencyMode = per-abi
```

发布：

```text
logvar-runtime-arm64-v8a-<commit>.zip
logvar-runtime-armeabi-v7a-<commit>.zip
logvar-runtime-x86_64-<commit>.zip
```

当前锁定的 LogVar 依赖包含 `esbuild` 的 Android 平台实现，因此当前结果为 **per-ABI**。

## 可复现依赖

上游当前没有 package lock，因此 CI 第一次解析某个 LogVar commit 时生成各 ABI 的 lock：

```text
.runtime/locks/<logvar-commit>/
├── arm64-v8a-package-lock.json
├── armeabi-v7a-package-lock.json
└── x86_64-package-lock.json
```

后续稳定构建使用：

```text
npm ci --omit=dev --ignore-scripts --os=android --cpu=<abi>
```

这样同一个 LogVar commit 不会因为未来 npm 解析到不同依赖版本而产生不可复现的 Runtime。

## Android 安装目录

v0.6 使用：

```text
files/remote_logvar/
├── runtime/
│   └── libnode.so
│
├── logvar/
│   ├── package.json
│   ├── danmu_api/
│   ├── node_modules/
│   └── config/.env
│
├── host/
│   └── main.mjs
│
└── downloads/
```

启动链：

```text
Android
  ↓
libremote_node_bridge.so
  ↓
libnode.so
  ↓
host/main.mjs
  ↓
原版 logvar/danmu_api/server.js
```

`host/main.mjs` 属于 Android 宿主，不属于 LogVar 源码。它只负责把原版 Node Server 的监听地址限制在 `127.0.0.1`，并把上游代理端口映射到 Demo 的 `19322`。

## 环境变量

用户配置保存在：

```text
logvar/config/.env
```

v0.5.x 升级时会迁移旧的 `project/config/.env`。

Android 只在键不存在时补充：

```env
SOURCE_ORDER=360,vod,tmdb,douban,tencent,youku,iqiyi,imgo,bilibili,renren,hanjutv,dandan,migu
RATE_LIMIT_MAX_REQUESTS=0
```

不会为了改变默认值去修改上游 LogVar 源码，也不会覆盖用户已经保存的值。

## Web 面板导入 / 导出

LogVar 上游 Web UI 保持原样。

- 导入继续使用 WebView 文件选择器。
- 上游导出使用 `blob:` URL。
- Android 在页面加载后注入一个宿主 shim，只拦截 `blob:` 文件下载并转交 Android Storage Access Framework。
- 导出 JSON 最终由系统文件保存器写入。
- `systemsettings.js` 不做任何修改。

## 上游自动监控

`.github/workflows/watch-logvar-upstream.yml` 每 6 小时检查一次上游 `main`。

发现未打包的新 commit 后：

```text
获取新 commit
↓
原样 checkout 上游源码
↓
三个 ABI 分别解析 node_modules
↓
生成 package-lock
↓
Native / ELF / ABI 检查
↓
生成 LogVar Runtime
↓
发布 logvar-upstream-<commit> prerelease candidate
```

candidate 不会自动替换 stable。稳定渠道仍由明确的 LogVar commit/版本选择控制。

## GitHub Release

v0.6 起正式 Release 主要包含：

```text
LogVarRemoteDemo-vX.Y.Z.apk

node-runtime-full-arm64-v8a-24.21.0-0.zip
node-runtime-full-armeabi-v7a-24.21.0-0.zip
node-runtime-full-x86_64-24.21.0-0.zip

logvar-runtime-universal-<commit>.zip
# 或按检测结果：
logvar-runtime-arm64-v8a-<commit>.zip
logvar-runtime-armeabi-v7a-<commit>.zip
logvar-runtime-x86_64-<commit>.zip

node-runtime-manifest.json
logvar-runtime-manifest.json
runtime-manifest.json
dependency-lock-*.json
SHA256SUMS.txt
```

`runtime-manifest.json` 是机器读取的统一索引。

## 本地服务

```text
Main API:  http://127.0.0.1:19321
Proxy:     http://127.0.0.1:19322
Token:     87654321
```

健康检查使用原版 LogVar 根页面，不依赖旧 Bootstrap 的 `/__health`。

## 构建工具链

- JDK 17
- Android SDK 36
- Build Tools 36.0.0
- NDK 28.2.13676358
- CMake 3.22.1
- Gradle 9.1.0
- AGP 9.0.1
- Kotlin / Compose 2.4.0

Demo Release 当前仍使用 debug signing config，正式项目需替换为自己的 release keystore。
