#!/usr/bin/env python3
import hashlib
import json
import os
import zipfile
from pathlib import Path

WORK = Path(os.environ.get("WORK_DIR", "work"))
OUT = Path(os.environ.get("OUT_DIR", "out"))
OUT.mkdir(parents=True, exist_ok=True)

NODE_VERSION = os.environ.get("WINDOWS_NODE_VERSION", "24.21.0")
UPSTREAM_SHA256 = os.environ["WINDOWS_NODE_UPSTREAM_SHA256"]
ARCHIVE = WORK / "node-win-x64.zip"


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def write_bytes(dst: zipfile.ZipFile, name: str, data: bytes):
    info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = 0o755 << 16
    dst.writestr(info, data)


if sha256(ARCHIVE) != UPSTREAM_SHA256:
    raise RuntimeError("Official Windows Node archive SHA-256 mismatch")

with zipfile.ZipFile(ARCHIVE) as src:
    node_matches = [
        name for name in src.namelist()
        if name.replace("\\", "/").lower().endswith("/node.exe")
    ]
    if len(node_matches) != 1:
        raise RuntimeError(f"Expected exactly one node.exe, found {node_matches}")
    node_exe = src.read(node_matches[0])

output = OUT / f"node-runtime-win-x86_64-{NODE_VERSION}.zip"
with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as dst:
    write_bytes(dst, "runtime/node.exe", node_exe)

manifest = {
    "schemaVersion": 1,
    "component": "node-runtime",
    "platform": "windows",
    "arch": "x86_64",
    "version": NODE_VERSION,
    "upstream": "nodejs.org",
    "upstreamFile": f"node-v{NODE_VERSION}-win-x64.zip",
    "upstreamSha256": UPSTREAM_SHA256,
    "artifact": {
        "file": output.name,
        "sha256": sha256(output),
        "bytes": output.stat().st_size,
    },
}
manifest_path = OUT / "node-runtime-windows-manifest.json"
manifest_path.write_text(
    json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)

print(f"win-x86_64: {manifest['artifact']['sha256']}")
print(f"manifest={sha256(manifest_path)}")
