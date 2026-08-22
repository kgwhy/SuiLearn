#!/usr/bin/env python3
"""Validate the SuiLearn single-language Agent Notes corpus."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
NOTES = ROOT / ".agents" / "notes"

LIFECYCLES = {"proposed", "implemented", "rejected"}
CLASSES = {
    "feature",
    "bug-fix",
    "simplification",
    "architecture",
    "process",
    "testing",
}

REQUIRED_SECTIONS = {
    "proposed": [
        "## Problem",
        "## Proposal",
        "## Alternatives considered",
        "## Acceptance criteria",
        "## Risks",
    ],
    "implemented": [
        "## Problem",
        "## Decision",
        "## Alternatives considered",
        "## Consequences",
    ],
    "rejected": [
        "## Problem",
        "## Proposal",
        "## Alternatives considered",
    ],
}

FORBIDDEN_IN_IMPLEMENTED = [
    "## Proposal",
    "## Plan",
    "## Migration plan",
    "## Acceptance criteria",
]

FILE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}-[a-z0-9][a-z0-9-]*\.md$")


def note_files() -> list[Path]:
    if not NOTES.exists():
        return []
    files: list[Path] = []
    for lifecycle in LIFECYCLES:
        for class_name in CLASSES:
            directory = NOTES / lifecycle / class_name
            if directory.is_dir():
                files.extend(sorted(directory.glob("*.md")))
    return files


def collect_issues() -> list[str]:
    issues: list[str] = []

    for directory in NOTES.iterdir() if NOTES.exists() else []:
        if directory.name in LIFECYCLES:
            continue
        if directory.name == "README.md":
            continue
        issues.append(f"unknown top-level entry: {directory.relative_to(ROOT)}")

    for directory in (NOTES / lifecycle for lifecycle in LIFECYCLES):
        if directory.is_dir():
            for child in directory.iterdir():
                if child.name in CLASSES:
                    continue
                issues.append(f"unknown class entry: {child.relative_to(ROOT)}")

    for path in note_files():
        rel = path.relative_to(ROOT)
        lifecycle = path.parent.parent.name
        class_name = path.parent.name
        if lifecycle not in LIFECYCLES or class_name not in CLASSES:
            issues.append(f"{rel}: invalid lifecycle/class path")
            continue
        if not FILE_RE.match(path.name):
            issues.append(f"{rel}: filename must match YYYY-MM-DD-slug.md")
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            issues.append(f"{rel}: not valid UTF-8")
            continue

        lines = text.splitlines()
        if not lines or not lines[0].startswith("# Agent Note: "):
            issues.append(f"{rel}: first line must be '# Agent Note: <title>'")
        status_line = lines[1] if len(lines) > 1 else ""
        status_match = re.match(r"^Status:\s+(proposed|implemented|rejected)\b", status_line)
        if not status_match:
            issues.append(f"{rel}: second line must be 'Status: <lifecycle>'")
        else:
            status = status_match.group(1)
            if status != lifecycle:
                issues.append(
                    f"{rel}: Status '{status}' does not match directory '{lifecycle}'"
                )
            if status == "rejected" and status_line.strip() == "Status: rejected":
                issues.append(f"{rel}: rejected Status must carry a one-line reason")

        for section in REQUIRED_SECTIONS.get(lifecycle, []):
            if not re.search(rf"(?m)^{re.escape(section)}\s*$", text):
                issues.append(f"{rel}: missing required section {section}")

        if lifecycle == "implemented":
            for section in FORBIDDEN_IN_IMPLEMENTED:
                if re.search(rf"(?m)^{re.escape(section)}\s*$", text):
                    issues.append(f"{rel}: implemented note must not contain {section}")

    return issues


def main() -> int:
    issues = collect_issues()
    if issues:
        print("Agent Notes check failed:")
        for issue in issues:
            print(f"- {issue}")
        return 1
    count = len(note_files())
    print(f"Agent Notes check passed ({count} note(s)).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
