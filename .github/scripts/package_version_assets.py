#!/usr/bin/env python3
import hashlib
import json
import os
import shutil
import zipfile
from pathlib import Path

WORK = Path(os.environ.get("WORK_DIR", "work"))
OUT = Path(os.environ.get("OUT_DIR", "release-assets"))
OUT.mkdir(parents=True, exist_ok=True)

MODE = os.environ["RELEASE_MODE"]
NODE_VERSION = "24.21.0-0"
CORE_COMMIT = "280b2327ccf06fb5e32a1db7051025359121a2c1"
SHELL_COMMIT = "4d75ca0420d2946956b34ed8c835b4912870a987"
RUNTIME_PACK_SHA256 = "4be12e7971f079cd357496974113342f6839919a247e24b4fee69a594301c8a7"
ABIS = ("arm64-v8a", "armeabi-v7a", "x86_64")

def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()

def write_bytes(dst, name, data):
    info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = 0o644 << 16
    dst.writestr(info, data)

def native_entries(src, abi):
    result = []
    for name in src.namelist():
        normalized = name.replace("\\", "/")
        parts = normalized.split("/")
        base = parts[-1]
        if abi in parts and base in {"libnode.so", "libc++_shared.so"}:
            result.append((name, base))
    if not any(base == "libnode.so" for _, base in result):
        raise RuntimeError(f"Missing libnode.so for {abi}")
    return result

bootstrap = {
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
        idx = normalized.find(marker)
        if idx >= 0 and not info.is_dir():
            rel = normalized[idx + len(marker):]
            if rel:
                core_entries.append((rel, src.read(info)))

# Common ABI-independent assets.
bootstrap_zip = OUT / f"logvar-android-bootstrap-{SHELL_COMMIT[:8]}.zip"
with zipfile.ZipFile(bootstrap_zip, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as dst:
    for name, data in bootstrap.items():
        write_bytes(dst, name, data)

modules_zip = OUT / f"logvar-node-modules-{RUNTIME_PACK_SHA256[:12]}.zip"
shutil.copy2(WORK / "node-modules.zip", modules_zip)

core_zip = OUT / f"logvar-core-{CORE_COMMIT[:8]}.zip"
with zipfile.ZipFile(core_zip, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as dst:
    for rel, data in core_entries:
        write_bytes(dst, f"danmu_api/{rel}", data)

packages = []

def emit_node_set(source_path: Path, flavor: str, emit_universal: bool, emit_bundle: bool):
    if emit_universal:
        universal = OUT / f"node-runtime-{flavor}-android-universal-{NODE_VERSION}.zip"
        shutil.copy2(source_path, universal)
        packages.append({
            "kind": "node-universal",
            "flavor": flavor,
            "file": universal.name,
            "bytes": universal.stat().st_size,
            "sha256": sha256(universal),
        })

    with zipfile.ZipFile(source_path) as src:
        for abi in ABIS:
            entries = native_entries(src, abi)
            node_zip = OUT / f"node-runtime-{flavor}-{abi}-{NODE_VERSION}.zip"
            with zipfile.ZipFile(node_zip, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as dst:
                for source_name, base in entries:
                    write_bytes(dst, f"bin/{abi}/{base}", src.read(source_name))
            item = {
                "kind": "node",
                "flavor": flavor,
                "abi": abi,
                "file": node_zip.name,
                "bytes": node_zip.stat().st_size,
                "sha256": sha256(node_zip),
            }
            packages.append(item)

            if emit_bundle:
                bundle = OUT / f"logvar-runtime-{flavor}-{abi}-{NODE_VERSION}.zip"
                with zipfile.ZipFile(bundle, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as dst:
                    for source_name, base in entries:
                        write_bytes(dst, f"runtime/{base}", src.read(source_name))
                    for name, data in bootstrap.items():
                        write_bytes(dst, f"project/{name}", data)
                    for name, data in node_modules_entries:
                        write_bytes(dst, f"project/{name}", data)
                    for rel, data in core_entries:
                        write_bytes(dst, f"project/danmu_api_stable/{rel}", data)
                    write_bytes(dst, "project/config/.env", b"")
                    write_bytes(dst, "project/tmp/.keep", b"")
                packages.append({
                    "kind": "bundle",
                    "flavor": flavor,
                    "abi": abi,
                    "file": bundle.name,
                    "bytes": bundle.stat().st_size,
                    "sha256": sha256(bundle),
                })

if MODE == "legacy-lite":
    emit_node_set(WORK / "node-lite.zip", "lite", True, False)
elif MODE == "dual":
    emit_node_set(WORK / "node-lite.zip", "lite", True, False)
    emit_node_set(WORK / "node-full.zip", "full", True, False)
elif MODE == "full-bundle":
    emit_node_set(WORK / "node-full.zip", "full", False, True)
else:
    raise RuntimeError(f"Unknown RELEASE_MODE={MODE}")

common = {}
for key, path in {
    "bootstrap": bootstrap_zip,
    "nodeModules": modules_zip,
    "core": core_zip,
}.items():
    common[key] = {
        "file": path.name,
        "bytes": path.stat().st_size,
        "sha256": sha256(path),
    }

manifest = {
    "schemaVersion": 1,
    "releaseMode": MODE,
    "nodeVersion": NODE_VERSION,
    "supportedAbis": list(ABIS),
    "dependencySet": {
        "androidBootstrapCommit": SHELL_COMMIT,
        "runtimePackSha256": RUNTIME_PACK_SHA256,
        "coreCommit": CORE_COMMIT,
    },
    "common": common,
    "packages": packages,
}
manifest_path = OUT / "remote-deps-manifest.json"
manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps(manifest, ensure_ascii=False, indent=2))
