# Version history

## v0.6.0
- Replaced the old combined Bootstrap/Core/dependencies bundle with a two-component runtime model: Node Runtime + LogVar Runtime.
- Node.js Mobile FULL remains independently packaged for arm64-v8a, armeabi-v7a and x86_64.
- LogVar Runtime now contains the complete unmodified upstream LogVar source snapshot plus node_modules resolved from the upstream package.json.
- Removed build-time patches to LogVar envs.js and systemsettings.js; Android-specific behavior now lives outside the upstream source tree.
- Added three-ABI dependency resolution and automatic universal/per-ABI LogVar packaging based on dependency tree and native/ELF inspection.
- Added committed per-ABI dependency locks outside the upstream tree for reproducible npm resolution.
- Added scheduled upstream monitoring that publishes new LogVar commits as prerelease candidates without automatically promoting them to stable.
- Added an external Android host shim that restricts the upstream Node server to localhost and maps the proxy service to port 19322 without modifying LogVar.
- Preserved environment config migration and defaults through config/.env rather than source patches.
- Preserved Android WebView JSON export through an injected host-side download shim while leaving the upstream Web UI unchanged.
- Removed legacy Bootstrap-only debugger routes and switched health checks to the upstream LogVar root page.
- Server-side runtime selection can now be modeled with only nodeVersion and logvarVersion.

## v0.5.1
- Fixed environment configuration export inside the Android WebView by bridging JSON export to Android's Storage Access Framework.
- SOURCE_ORDER default is now: 360,vod,tmdb,douban,tencent,youku,iqiyi,imgo,bilibili,renren,hanjutv,dandan,migu.
- RATE_LIMIT_MAX_REQUESTS now defaults to 0 (unlimited).
- Existing .env values are preserved; only missing SOURCE_ORDER / RATE_LIMIT_MAX_REQUESTS keys receive the new defaults.
- Patched LogVar Core runtime fallback values and Web-panel descriptions to match the Android defaults.
- Rebuilt arm64-v8a, armeabi-v7a and x86_64 FULL runtime bundles from the same patched dependency set.
- Runtime marker was advanced so upgrading from v0.5.0 performs one controlled runtime refresh while preserving the user's .env.

## v0.5.0
- Unified arm64-v8a, armeabi-v7a and x86_64 as equal first-class runtime targets; no ABI is designated primary.
- Standardized every ABI on Node.js Mobile FULL 24.21.0-0 plus the same Bootstrap, node_modules and LogVar Core dependency set.
- Added deterministic per-ABI Node ZIPs and complete LogVar Runtime Bundle ZIPs for all three ABIs.
- Added separate common Bootstrap, node_modules and LogVar Core assets to every current Release.
- Moved runtime download URLs to the matching source GitHub Release instead of a separate runtime-only release version.
- Added automatic Tag Release publishing with APK, dependencies, manifest and SHA256SUMS.
- Added historical Release backfill for v0.1.0 through v0.4.0.
- Synced APK versionName/versionCode to 0.5.0 / 5.

## v0.4.0
- Switched Android runtime downloads to Node.js Mobile FULL flavor.
- Made arm64-v8a the primary runtime and removed cross-ABI native payloads from its package.
- Added separate FULL bundles for arm64-v8a, armeabi-v7a and x86_64.
- Each complete bundle contains exactly one native ABI plus Bootstrap, node_modules, LogVar Core, config/.env and tmp.
- Android now downloads one ABI-specific bundle instead of a universal Node archive plus multiple dependency downloads.
- Added deterministic ZIP generation and stable GitHub Release assets with fixed SHA-256 values.

## v0.3.2
- Switched runtime packaging to Node.js Mobile FULL flavor only.
- Made arm64-v8a the primary Android runtime package.
- Added complete per-ABI LogVar bundles: each ZIP contains only one native libnode ABI plus Bootstrap, node_modules, LogVar Core, config/.env and tmp.
- Kept armeabi-v7a and x86_64 as separate compatibility bundles; no cross-ABI native files are mixed into the arm64-v8a package.
- Added stable-release publishing workflow for per-ABI runtime assets.

## v0.3.1
- Added Node.js Mobile full runtime packaging alongside the existing lite runtime.
- Renamed generated Node packages to explicitly include `lite` or `full`.
- Added universal and per-ABI full runtime ZIPs.
- Added full-runtime upstream SHA-256 to the generated manifest.

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
