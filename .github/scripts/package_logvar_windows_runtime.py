#!/usr/bin/env python3
import hashlib
import json
import os
import stat
import zipfile
from pathlib import Path

SOURCE = Path(os.environ.get("LOGVAR_SOURCE", "work/logvar-source"))
WORK = Path(os.environ.get("WORK_DIR", "work"))
OUT = Path(os.environ.get("OUT_DIR", "out"))
OUT.mkdir(parents=True, exist_ok=True)

UPSTREAM_REPOSITORY = os.environ.get("LOGVAR_REPOSITORY", "huangxd-/danmu_api")
UPSTREAM_COMMIT = os.environ["LOGVAR_COMMIT"]
SHORT = UPSTREAM_COMMIT[:12]
TARGET = "win-x86_64"


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def iter_tree(root: Path, exclude=()):
    excluded = set(exclude)
    for path in sorted(root.rglob("*"), key=lambda p: p.as_posix()):
        rel = path.relative_to(root).as_posix()
        if any(rel == x or rel.startswith(x + "/") for x in excluded):
            continue
        if path.is_file() or path.is_symlink():
            yield path, rel


def file_bytes(path: Path) -> bytes:
    if path.is_symlink():
        return path.resolve(strict=True).read_bytes()
    return path.read_bytes()


def tree_digest(root: Path, exclude=()) -> str:
    h = hashlib.sha256()
    for path, rel in iter_tree(root, exclude):
        data = file_bytes(path)
        h.update(rel.encode("utf-8"))
        h.update(b"\0")
        h.update(hashlib.sha256(data).digest())
        h.update(b"\0")
    return h.hexdigest()


def pe_machine(data: bytes):
    if len(data) < 0x40 or data[:2] != b"MZ":
        return None
    offset = int.from_bytes(data[0x3C:0x40], "little")
    if offset + 6 > len(data) or data[offset:offset + 4] != b"PE\0\0":
        return None
    return int.from_bytes(data[offset + 4:offset + 6], "little")


def write_entry(dst: zipfile.ZipFile, name: str, data: bytes, executable=False):
    info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    mode = 0o755 if executable else 0o644
    info.external_attr = mode << 16
    dst.writestr(info, data)


def write_tree(dst: zipfile.ZipFile, root: Path, prefix: str, exclude=()):
    for path, rel in iter_tree(root, exclude):
        data = file_bytes(path)
        mode = path.stat().st_mode if not path.is_symlink() else path.resolve().stat().st_mode
        executable = bool(mode & stat.S_IXUSR) or path.suffix.lower() in {".exe", ".cmd", ".bat"}
        write_entry(dst, f"{prefix}/{rel}", data, executable)


source_digest = tree_digest(SOURCE, exclude=(".git", "node_modules"))
package_json = SOURCE / "package.json"
if not package_json.is_file():
    raise RuntimeError("Upstream package.json is missing")

modules_root = WORK / "logvar-win-x86_64" / "node_modules"
lock_path = WORK / "logvar-win-x86_64" / "package-lock.json"
if not modules_root.is_dir():
    raise RuntimeError("Missing Windows x86_64 node_modules")
if not lock_path.is_file():
    raise RuntimeError("Missing Windows x86_64 package-lock.json")

native_files = []
mismatched = []
for path, rel in iter_tree(modules_root):
    data = file_bytes(path)
    machine = pe_machine(data)
    suffix = path.suffix.lower()
    if suffix in {".node", ".exe", ".dll"} or machine is not None:
        native_files.append(rel)
    if machine is not None and machine != 0x8664:
        mismatched.append({"file": rel, "machine": hex(machine), "expected": "0x8664"})

if mismatched:
    raise RuntimeError(
        "Windows dependency tree contains non-x86_64 PE binaries: " +
        json.dumps(mismatched[:20], ensure_ascii=False)
    )

file_name = f"logvar-runtime-win-x86_64-{SHORT}.zip"
output = OUT / file_name
meta = {
    "repository": UPSTREAM_REPOSITORY,
    "commit": UPSTREAM_COMMIT,
    "platform": "windows",
    "arch": "x86_64",
    "sourceTreeSha256": source_digest,
    "packageJsonSha256": sha256_file(package_json),
    "dependencyTreeSha256": tree_digest(modules_root),
    "dependencyLockSha256": sha256_file(lock_path),
    "nativeFiles": native_files,
    "nativeFileCount": len(native_files),
    "sourceModified": False,
}

with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as dst:
    write_tree(dst, SOURCE, "logvar", exclude=(".git", "node_modules"))
    write_tree(dst, modules_root, "logvar/node_modules")
    write_entry(
        dst,
        "runtime-meta/logvar-runtime.json",
        (json.dumps(meta, ensure_ascii=False, indent=2) + "\n").encode("utf-8"),
    )

manifest = {
    "schemaVersion": 1,
    "component": "logvar-runtime",
    "platform": "windows",
    "arch": "x86_64",
    "repository": UPSTREAM_REPOSITORY,
    "commit": UPSTREAM_COMMIT,
    "shortCommit": SHORT,
    "sourceModified": False,
    "artifact": {
        "file": file_name,
        "sha256": sha256_file(output),
        "bytes": output.stat().st_size,
        "dependencyTreeSha256": meta["dependencyTreeSha256"],
        "dependencyLockSha256": meta["dependencyLockSha256"],
        "nativeFileCount": len(native_files),
    },
}
manifest_path = OUT / "logvar-runtime-windows-manifest.json"
manifest_path.write_text(
    json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)

print(f"win-x86_64: {manifest['artifact']['sha256']}")
print(f"nativeFileCount={len(native_files)}")
print(f"manifest={sha256_file(manifest_path)}")
