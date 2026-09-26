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

UPSTREAM_REPOSITORY = os.environ.get("LOGVAR_REPOSITORY", "lilixu3/danmu_api")
UPSTREAM_COMMIT = os.environ["LOGVAR_COMMIT"]
SHORT = UPSTREAM_COMMIT[:12]
ABIS = ("arm64-v8a", "armeabi-v7a", "x86_64")
ELF_MACHINE = {
    "arm64-v8a": 183,
    "armeabi-v7a": 40,
    "x86_64": 62,
}


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


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
        target = path.resolve(strict=True)
        return target.read_bytes()
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


def elf_machine(data: bytes):
    if len(data) < 20 or data[:4] != b"\x7fELF":
        return None
    endian = "little" if data[5] == 1 else "big"
    return int.from_bytes(data[18:20], endian)


def scan_dependencies(root: Path, abi: str):
    native = []
    mismatched = []
    for path, rel in iter_tree(root):
        data = file_bytes(path)
        machine = elf_machine(data)
        if path.suffix == ".node" or machine is not None:
            native.append(rel)
        if machine is not None and machine != ELF_MACHINE[abi]:
            mismatched.append({
                "file": rel,
                "machine": machine,
                "expected": ELF_MACHINE[abi],
            })
    return native, mismatched


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
        executable = bool(mode & stat.S_IXUSR)
        write_entry(dst, f"{prefix}/{rel}", data, executable)


source_digest = tree_digest(SOURCE, exclude=(".git", "node_modules"))
package_json = SOURCE / "package.json"
if not package_json.is_file():
    raise RuntimeError("Upstream package.json is missing")
package_json_sha = sha256_file(package_json)

dependency_info = {}
for abi in ABIS:
    root = WORK / f"logvar-{abi}" / "node_modules"
    if not root.is_dir():
        raise RuntimeError(f"Missing node_modules for {abi}: {root}")
    native, mismatched = scan_dependencies(root, abi)
    if mismatched:
        raise RuntimeError(
            f"{abi} contains ELF files for another architecture: " +
            json.dumps(mismatched[:20], ensure_ascii=False)
        )
    lock_path = WORK / f"logvar-{abi}" / "package-lock.json"
    if not lock_path.is_file():
        raise RuntimeError(f"Missing generated dependency lock for {abi}")
    dependency_info[abi] = {
        "treeSha256": tree_digest(root),
        "lockSha256": sha256_file(lock_path),
        "nativeFiles": native,
        "nativeFileCount": len(native),
    }

digests = {v["treeSha256"] for v in dependency_info.values()}
has_native = any(v["nativeFileCount"] > 0 for v in dependency_info.values())
mode = "universal" if len(digests) == 1 and not has_native else "per-abi"

artifacts = {}

def package_for(label: str, modules_root: Path):
    file_name = f"logvar-runtime-{label}-{SHORT}.zip"
    output = OUT / file_name
    meta = {
        "repository": UPSTREAM_REPOSITORY,
        "commit": UPSTREAM_COMMIT,
        "sourceTreeSha256": source_digest,
        "packageJsonSha256": package_json_sha,
        "dependencyMode": mode,
        "dependencyTreeSha256": tree_digest(modules_root),
        "dependencyLockSha256": dependency_info[label]["lockSha256"] if label in dependency_info else next(iter(dependency_info.values()))["lockSha256"],
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
    return {
        "file": file_name,
        "sha256": sha256_file(output),
        "bytes": output.stat().st_size,
        "dependencyTreeSha256": meta["dependencyTreeSha256"],
    }

if mode == "universal":
    artifacts["universal"] = package_for(
        "universal",
        WORK / "logvar-arm64-v8a" / "node_modules",
    )
else:
    for abi in ABIS:
        artifacts[abi] = package_for(
            abi,
            WORK / f"logvar-{abi}" / "node_modules",
        )

manifest = {
    "schemaVersion": 1,
    "component": "logvar-runtime",
    "repository": UPSTREAM_REPOSITORY,
    "commit": UPSTREAM_COMMIT,
    "shortCommit": SHORT,
    "sourceModified": False,
    "sourceTreeSha256": source_digest,
    "packageJsonSha256": package_json_sha,
    "dependencyMode": mode,
    "dependencyBuilds": dependency_info,
    "artifacts": artifacts,
}
manifest_path = OUT / "logvar-runtime-manifest.json"
manifest_path.write_text(
    json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)

print("dependencyMode=" + mode)
for key, value in artifacts.items():
    print(f"{key}: {value['sha256']}")
print("manifest=" + sha256_file(manifest_path))
