#!/usr/bin/env python3
import hashlib
import json
import os
import shutil
import zipfile
from pathlib import Path

WORK = Path(os.environ.get("WORK_DIR", "work"))
OUT = Path(os.environ.get("OUT_DIR", "out"))
OUT.mkdir(parents=True, exist_ok=True)

NODE_VERSION = os.environ.get("NODE_VERSION", "24.21.0-0")
NODE_FLAVOR = os.environ.get("NODE_FLAVOR", "full")
NODE_UPSTREAM_SHA256 = os.environ["NODE_UPSTREAM_SHA256"]
CORE_COMMIT = os.environ.get("CORE_COMMIT", "280b2327ccf06fb5e32a1db7051025359121a2c1")
SHELL_COMMIT = os.environ.get("SHELL_COMMIT", "4d75ca0420d2946956b34ed8c835b4912870a987")
RUNTIME_PACK_SHA256 = os.environ.get(
    "RUNTIME_PACK_SHA256",
    "4be12e7971f079cd357496974113342f6839919a247e24b4fee69a594301c8a7",
)
ABIS = ("arm64-v8a", "armeabi-v7a", "x86_64")
DEFAULT_SOURCE_ORDER = "360,vod,tmdb,douban,tencent,youku,iqiyi,imgo,bilibili,renren,hanjutv,dandan,migu"
DEFAULT_RATE_LIMIT_MAX_REQUESTS = "0"
DEFAULT_ENV_BYTES = (
    f"SOURCE_ORDER={DEFAULT_SOURCE_ORDER}\n"
    f"RATE_LIMIT_MAX_REQUESTS={DEFAULT_RATE_LIMIT_MAX_REQUESTS}\n"
).encode("utf-8")


def patch_core_file(rel: str, data: bytes) -> bytes:
    if rel not in {"configs/envs.js", "ui/js/systemsettings.js"}:
        return data

    text = data.decode("utf-8")

    if rel == "configs/envs.js":
        replacements = {
            "this.get('SOURCE_ORDER', 'douban,360,renren,hanjutv', 'string')":
                f"this.get('SOURCE_ORDER', '{DEFAULT_SOURCE_ORDER}', 'string')",
            "return orderArr.length > 0 ? orderArr : ['douban', '360', 'renren', 'hanjutv'];":
                "return orderArr.length > 0 ? orderArr : " +
                str(DEFAULT_SOURCE_ORDER.split(",")).replace('"', "'") + ";",
            "'RATE_LIMIT_MAX_REQUESTS': { category: 'api', type: 'number', description: '限流配置：1分钟内最大请求次数，0表示不限流，默认3', min: 0, max: 50 }":
                "'RATE_LIMIT_MAX_REQUESTS': { category: 'api', type: 'number', description: '限流配置：1分钟内最大请求次数，0表示不限流，默认0', min: 0, max: 50 }",
            "'SOURCE_ORDER': { category: 'source', type: 'multi-select', options: this.ALLOWED_SOURCES, description: '源排序配置，默认douban,360,renren,hanjutv；添加 local 可搜索已上传的本地弹幕，按配置顺序排列搜索结果' }":
                f"'SOURCE_ORDER': {{ category: 'source', type: 'multi-select', options: this.ALLOWED_SOURCES, description: '源排序配置，默认{DEFAULT_SOURCE_ORDER}；添加 local 可搜索已上传的本地弹幕，按配置顺序排列搜索结果' }}",
            "this.get('RATE_LIMIT_MAX_REQUESTS', 3, 'number')":
                "this.get('RATE_LIMIT_MAX_REQUESTS', 0, 'number')",
            "限流配置：时间窗口内最大请求次数（默认 3，0表示不限流）":
                "限流配置：时间窗口内最大请求次数（默认 0，0表示不限流）",
        }
        for old, new in replacements.items():
            if old not in text:
                raise RuntimeError(f"Core patch target not found in envs.js: {old[:80]}")
            text = text.replace(old, new)

    if rel == "ui/js/systemsettings.js":
        old = """        const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        const date = new Date().toISOString().slice(0, 10);
        link.href = url;
        link.download = 'danmu-api-config-' + date + '.json';
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        addLog('配置文件导出成功，共 ' + Object.keys(values).length + ' 项', 'success');"""
        new = """        const date = new Date().toISOString().slice(0, 10);
        const fileName = 'danmu-api-config-' + date + '.json';
        const jsonText = JSON.stringify(exportData, null, 2);

        // Android WebView cannot reliably download blob: URLs. When the native
        // bridge is available, hand the JSON to Android's Storage Access Framework.
        if (window.AndroidConfigBridge && typeof window.AndroidConfigBridge.saveConfig === 'function') {
            window.AndroidConfigBridge.saveConfig(jsonText, fileName);
            addLog('配置文件已交给 Android 保存，共 ' + Object.keys(values).length + ' 项', 'success');
            return;
        }

        const blob = new Blob([jsonText], { type: 'application/json;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        addLog('配置文件导出成功，共 ' + Object.keys(values).length + ' 项', 'success');"""
        if old not in text:
            raise RuntimeError("Core patch target not found in systemsettings.js exportSystemConfig")
        text = text.replace(old, new)

    return text.encode("utf-8")


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def write_bytes(dst: zipfile.ZipFile, name: str, data: bytes):
    info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = 0o644 << 16
    dst.writestr(info, data)


def find_native_entries(src: zipfile.ZipFile, abi: str):
    chosen = []
    for name in src.namelist():
        normalized = name.replace("\\", "/")
        parts = normalized.split("/")
        base = parts[-1] if parts else ""
        if abi in parts and base in {"libnode.so", "libc++_shared.so"}:
            chosen.append((name, base))
    if not any(base == "libnode.so" for _, base in chosen):
        raise RuntimeError(f"libnode.so not found for {NODE_FLAVOR}/{abi}")
    return chosen


bootstrap_files = {
    p.name: p.read_bytes()
    for p in sorted((WORK / "bootstrap").iterdir())
    if p.is_file()
}

node_modules_entries = []
with zipfile.ZipFile(WORK / "node-modules.zip") as src:
    for info in src.infolist():
        if not info.is_dir():
            node_modules_entries.append((info.filename.replace("\\", "/").lstrip("/"), src.read(info)))

core_entries = []
with zipfile.ZipFile(WORK / "core-source.zip") as src:
    for info in src.infolist():
        normalized = info.filename.replace("\\", "/")
        marker = "/danmu_api/"
        index = normalized.find(marker)
        if index < 0 or info.is_dir():
            continue
        rel = normalized[index + len(marker):]
        if rel:
            core_entries.append((rel, patch_core_file(rel, src.read(info))))
if not core_entries:
    raise RuntimeError("No danmu_api core files found")

# ABI-independent assets. All ABI bundles below consume these exact bytes.
bootstrap_zip = OUT / f"logvar-android-bootstrap-{SHELL_COMMIT[:8]}.zip"
with zipfile.ZipFile(bootstrap_zip, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as dst:
    for name, data in bootstrap_files.items():
        write_bytes(dst, name, data)

node_modules_zip = OUT / f"logvar-node-modules-{RUNTIME_PACK_SHA256[:12]}.zip"
shutil.copy2(WORK / "node-modules.zip", node_modules_zip)

core_zip = OUT / f"logvar-core-{CORE_COMMIT[:8]}.zip"
with zipfile.ZipFile(core_zip, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as dst:
    for rel, data in core_entries:
        write_bytes(dst, f"danmu_api/{rel}", data)

common = {
    "bootstrap": {
        "file": bootstrap_zip.name,
        "bytes": bootstrap_zip.stat().st_size,
        "sha256": sha256(bootstrap_zip),
    },
    "nodeModules": {
        "file": node_modules_zip.name,
        "bytes": node_modules_zip.stat().st_size,
        "sha256": sha256(node_modules_zip),
    },
    "core": {
        "file": core_zip.name,
        "bytes": core_zip.stat().st_size,
        "sha256": sha256(core_zip),
    },
}

packages = []
with zipfile.ZipFile(WORK / "node.zip") as node_src:
    for abi in ABIS:
        native_entries = find_native_entries(node_src, abi)

        node_zip = OUT / f"node-runtime-{NODE_FLAVOR}-{abi}-{NODE_VERSION}.zip"
        with zipfile.ZipFile(node_zip, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as dst:
            for source_name, base in native_entries:
                write_bytes(dst, f"bin/{abi}/{base}", node_src.read(source_name))

        bundle = OUT / f"logvar-runtime-{NODE_FLAVOR}-{abi}-{NODE_VERSION}.zip"
        with zipfile.ZipFile(bundle, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as dst:
            for source_name, base in native_entries:
                write_bytes(dst, f"runtime/{base}", node_src.read(source_name))
            for name, data in bootstrap_files.items():
                write_bytes(dst, f"project/{name}", data)
            for name, data in node_modules_entries:
                write_bytes(dst, f"project/{name}", data)
            for rel, data in core_entries:
                write_bytes(dst, f"project/danmu_api_stable/{rel}", data)
            write_bytes(dst, "project/config/.env", DEFAULT_ENV_BYTES)
            write_bytes(dst, "project/tmp/.keep", b"")

        packages.append({
            "abi": abi,
            "node": {
                "file": node_zip.name,
                "bytes": node_zip.stat().st_size,
                "sha256": sha256(node_zip),
            },
            "bundle": {
                "file": bundle.name,
                "bytes": bundle.stat().st_size,
                "sha256": sha256(bundle),
            },
        })

manifest = {
    "schemaVersion": 3,
    "nodeVersion": NODE_VERSION,
    "nodeFlavor": NODE_FLAVOR,
    "supportedAbis": list(ABIS),
    "dependencySet": {
        "androidBootstrapCommit": SHELL_COMMIT,
        "runtimePackSha256": RUNTIME_PACK_SHA256,
        "coreCommit": CORE_COMMIT,
        "nodeUpstreamSha256": NODE_UPSTREAM_SHA256,
        "androidDefaults": {
            "SOURCE_ORDER": DEFAULT_SOURCE_ORDER,
            "RATE_LIMIT_MAX_REQUESTS": DEFAULT_RATE_LIMIT_MAX_REQUESTS,
        },
        "corePatches": [
            "Android WebView config export bridge",
            "SOURCE_ORDER default override",
            "RATE_LIMIT_MAX_REQUESTS default override",
        ],
    },
    "common": common,
    "packages": packages,
    "invariants": [
        "No ABI is designated primary.",
        "All ABIs use the same Node version and flavor.",
        "All ABI bundles embed byte-identical Bootstrap, node_modules and LogVar Core inputs.",
        "Only native Node runtime files differ by ABI.",
    ],
}

manifest_path = OUT / "remote-deps-manifest.json"
manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

for item in packages:
    print(
        f'{item["abi"]}: node={item["node"]["sha256"]} '
        f'bundle={item["bundle"]["sha256"]}'
    )
print(f"manifest={sha256(manifest_path)}")
