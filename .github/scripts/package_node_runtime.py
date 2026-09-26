#!/usr/bin/env python3
import hashlib
import json
import os
import zipfile
from pathlib import Path

WORK = Path(os.environ.get("WORK_DIR", "work"))
OUT = Path(os.environ.get("OUT_DIR", "out"))
OUT.mkdir(parents=True, exist_ok=True)

NODE_VERSION = os.environ.get("NODE_VERSION", "24.21.0-0")
NODE_FLAVOR = os.environ.get("NODE_FLAVOR", "full")
NODE_UPSTREAM_SHA256 = os.environ["NODE_UPSTREAM_SHA256"]
ABIS = ("arm64-v8a", "armeabi-v7a", "x86_64")


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


def find_libnode(src: zipfile.ZipFile, abi: str) -> bytes:
    matches = []
    for name in src.namelist():
        normalized = name.replace("\\", "/")
        parts = normalized.split("/")
        if abi in parts and parts[-1] == "libnode.so":
            matches.append(name)
    if len(matches) != 1:
        raise RuntimeError(f"Expected exactly one libnode.so for {abi}, found {matches}")
    return src.read(matches[0])


node_archive = WORK / "node-full.zip"
if sha256(node_archive) != NODE_UPSTREAM_SHA256:
    raise RuntimeError("Node upstream SHA-256 mismatch")

artifacts = {}
with zipfile.ZipFile(node_archive) as src:
    for abi in ABIS:
        output = OUT / f"node-runtime-{NODE_FLAVOR}-{abi}-{NODE_VERSION}.zip"
        with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as dst:
            write_bytes(dst, "runtime/libnode.so", find_libnode(src, abi))
        artifacts[abi] = {
            "file": output.name,
            "sha256": sha256(output),
            "bytes": output.stat().st_size,
        }
        print(f"{abi}: {artifacts[abi]['sha256']}")

manifest = {
    "schemaVersion": 1,
    "component": "node-runtime",
    "version": NODE_VERSION,
    "flavor": NODE_FLAVOR,
    "upstreamSha256": NODE_UPSTREAM_SHA256,
    "artifacts": artifacts,
}
path = OUT / "node-runtime-manifest.json"
path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("manifest=" + sha256(path))
