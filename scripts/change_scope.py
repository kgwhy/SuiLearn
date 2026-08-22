#!/usr/bin/env python3
"""Report committed and worktree scope for a SuiLearn change."""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def git(*args: str) -> str:
    result = subprocess.run(
        ["git", "-c", f"safe.directory={ROOT}", *args],
        cwd=ROOT,
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    if result.returncode != 0:
        detail = result.stderr.strip() or f"git exited with status {result.returncode}"
        raise RuntimeError(f"git {' '.join(args)}: {detail}")
    return result.stdout


def norm(path: str) -> str:
    return path.replace("\\", "/").strip()


def default_base_ref() -> str:
    for remote in ("origin/dev", "origin/main"):
        base = subprocess.run(
            ["git", "-c", f"safe.directory={ROOT}", "merge-base", "HEAD", remote],
            cwd=ROOT,
            check=False,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        ).stdout.strip()
        if base:
            return base
    return git("rev-parse", "HEAD").strip()


def changed_paths(base_ref: str) -> dict[str, list[str]]:
    paths: dict[str, list[str]] = {
        "committed": [],
        "staged": [],
        "unstaged": [],
        "untracked": [],
    }

    base_sha = git("rev-parse", "--verify", "--end-of-options", f"{base_ref}^{{commit}}").strip()
    head_sha = git("rev-parse", "HEAD").strip()
    merge_base = git("merge-base", base_sha, head_sha).strip()

    def diff(*args: str) -> list[str]:
        output = git("diff", "--name-only", "-z", "--no-renames", *args, "--")
        return sorted({norm(p) for p in output.split("\0") if p})

    paths["committed"] = diff(merge_base, head_sha)
    paths["staged"] = diff("--cached")
    paths["unstaged"] = diff()
    untracked = git("ls-files", "--others", "--exclude-standard", "-z", "--")
    paths["untracked"] = sorted({norm(p) for p in untracked.split("\0") if p})
    return paths


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", "--base-ref", dest="base", default="")
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    try:
        base = args.base or default_base_ref()
        base_sha = git("rev-parse", "--verify", "--end-of-options", f"{base}^{{commit}}").strip()
        head_sha = git("rev-parse", "HEAD").strip()
        paths = changed_paths(base)
    except RuntimeError as error:
        print(f"change-scope failed: {error}", file=sys.stderr)
        return 1

    if args.json:
        payload = {
            "base": base,
            "baseSha": base_sha,
            "headSha": head_sha,
            "paths": paths,
        }
        print(json.dumps(payload, ensure_ascii=False, indent=2))
        return 0

    print(f"Base: {base}")
    print(f"Base SHA: {base_sha}")
    print(f"Head SHA: {head_sha}")
    print("committed:")
    for path in paths["committed"]:
        print(f"  {path}")
    print("staged:")
    for path in paths["staged"]:
        print(f"  {path}")
    print("unstaged:")
    for path in paths["unstaged"]:
        print(f"  {path}")
    print("untracked:")
    for path in paths["untracked"]:
        print(f"  {path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
