# Version history

## v0.3.0
- Added reproducible remote dependency ZIP packaging workflow.
- Split Node.js Mobile runtime into arm64-v8a, armeabi-v7a and x86_64 ZIPs.
- Kept split Node ZIP layout compatible with the Android runtime extractor.
- Added manifest generation with SHA-256 and sizes.

## v0.2.1
- Fixed LogVar Web panel environment-variable persistence.
- Ensures project/config/.env exists before Node startup.
- Preserves existing .env when runtime is repaired or reinstalled.

## v0.2.0
- Added full LogVar API debugger catalog and generic request runner.
- Added in-app WebView for LogVar's built-in local Web panel.
- Added WebView file chooser support for local danmaku upload.
- Added local ADMIN_TOKEN wiring.

## v0.1.0
- Initial Compose + MVVM remote runtime demo.
- APK excludes libnode.so, LogVar Core and node_modules.
- Downloads Node.js Mobile, bootstrap, node_modules and LogVar Core at runtime.
- Includes JNI dlopen bridge and release APK build workflow.
