#!/usr/bin/env python3
"""Cross-platform staged-snapshot secret scanner for SuiLearn."""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
ALLOWED_BINARY_EXT = {
    ".jar", ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".pdf",
    ".ttf", ".otf", ".woff", ".woff2", ".zip", ".gz", ".mp3", ".mp4",
}
RULES = [
    ("github-token", re.compile(r"\bgh[pousr]_[A-Za-z0-9]{36,255}\b")),
    ("github-fine-grained-token", re.compile(r"\bgithub_pat_[A-Za-z0-9_]{22,255}\b")),
    ("openai-api-key", re.compile(r"\bsk-[A-Za-z0-9_-]{20,}\b")),
    ("jwt-token", re.compile(r"\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\b")),
    ("aws-access-key-id", re.compile(r"\bAKIA[0-9A-Z]{16}\b")),
    ("slack-token", re.compile(r"\bxox[baprs]-[A-Za-z0-9-]{10,}\b")),
    ("private-key-header", re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----")),
    ("credential-assignment", re.compile(
        r"(?im)^\s*(?:[A-Z][A-Z0-9_]*(?:KEY|SECRET|TOKEN|PASSWORD)|"
        r"(?:api[_-]?key|access[_-]?token|client[_-]?secret))\s*[:=]\s*"
        r"[\"']?[A-Za-z0-9_./+=-]{16,}"
    )),
]


def git(*args: str) -> bytes:
    proc = subprocess.run(
        ["git", "-c", f"safe.directory={ROOT}", *args],
        cwd=ROOT,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if proc.returncode != 0:
        raise RuntimeError("git could not inspect the staged snapshot")
    return proc.stdout


def is_binary(data: bytes) -> bool:
    if b"\x00" in data:
        return True
    try:
        data.decode("utf-8", errors="strict")
    except UnicodeDecodeError:
        return True
    return False


def main() -> int:
    try:
        path_bytes = git("diff", "--cached", "--name-only", "-z", "--diff-filter=ACMR")
    except RuntimeError as exc:
        print("提交前预检无法安全完成。")
        return 1
    paths = [p for p in path_bytes.decode("utf-8", errors="replace").split("\0") if p]
    if not paths:
        print("预检通过：没有需要检查的暂存文件。")
        return 0

    failures = 0
    for path in paths:
        ext = Path(path).suffix.lower()
        if ext in ALLOWED_BINARY_EXT:
            continue
        try:
            blob = git("cat-file", "blob", ":" + path)
        except RuntimeError:
            print(f"提交前预检失败：无法读取暂存文件 {path}")
            failures += 1
            continue
        if is_binary(blob):
            print(f"提交前预检失败：规则 'binary-or-invalid-utf8' 命中暂存文件 {path}")
            failures += 1
            continue
        content = blob.decode("utf-8", errors="strict")
        for name, pattern in RULES:
            if pattern.search(content):
                print(f"提交前预检失败：规则 '{name}' 命中暂存文件 {path}")
                failures += 1

    if failures:
        return 1
    print("提交前预检通过。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
