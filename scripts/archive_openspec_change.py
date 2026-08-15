#!/usr/bin/env python3
"""Archive a completed SuiLearn OpenSpec change to a flat date directory."""
from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CHANGES = ROOT / "openspec" / "changes"
ARCHIVE = CHANGES / "archive"


def run_closeout_check(change_name: str) -> None:
    proc = subprocess.run(
        [
            sys.executable,
            str(ROOT / "scripts" / "check_suilearn_workflow.py"),
            "--closing-change",
            change_name,
        ],
        cwd=ROOT,
    )
    if proc.returncode != 0:
        raise RuntimeError(f"workflow closeout check failed for {change_name}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--change-name", required=True)
    parser.add_argument("--archive-date", default=date.today().isoformat())
    # Accepted for compatibility with the old PowerShell wrapper. No longer used.
    parser.add_argument("--primary-domain", default=None)
    parser.add_argument("--related-domains", nargs="*", default=[])
    args = parser.parse_args()

    if args.change_name == "archive":
        print("The archive directory is reserved and cannot be archived.")
        return 1

    source = CHANGES / args.change_name
    if not source.is_dir():
        print(f"active change directory does not exist: {source}")
        return 1
    if source.parent != CHANGES:
        print("Only a direct child of openspec/changes can be archived.")
        return 1

    leaf = f"{args.archive_date}-{args.change_name}"
    target = ARCHIVE / leaf
    if target.exists():
        print(f"archive target already exists: {target}")
        return 1

    run_closeout_check(args.change_name)

    ARCHIVE.mkdir(parents=True, exist_ok=True)
    shutil.move(str(source), str(target))
    print(f"Archived {args.change_name} to {target}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
