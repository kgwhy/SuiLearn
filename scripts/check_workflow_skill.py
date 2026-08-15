#!/usr/bin/env python3
"""Structural check for .agents/skills/suilearn-workflow."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SKILL_DIR = ROOT / ".agents" / "skills" / "suilearn-workflow"
REQUIRED_REFS = [
    "state-machine.md",
    "usage-examples.md",
    "forward-testing.md",
    "change-levels.md",
    "policy-gates.md",
    "subagent-loop.md",
    "verification.md",
    "archive-organization.md",
]


def fail(msg: str) -> None:
    print(f"- {msg}")


def main() -> int:
    issues: list[str] = []
    skill = SKILL_DIR / "SKILL.md"
    if not skill.exists():
        issues.append("missing SKILL.md")
        print("SuiLearn workflow skill check failed:")
        for issue in issues:
            print(f"- {issue}")
        return 1

    text = skill.read_text(encoding="utf-8")
    front = re.match(r"^---\s*\n(.*?)\n---\s*\n", text, re.S)
    if not front:
        issues.append("SKILL.md missing YAML frontmatter")
    else:
        body = front.group(1)
        if "name: suilearn-workflow" not in body:
            issues.append("frontmatter missing name: suilearn-workflow")
        if "description:" not in body:
            issues.append("frontmatter missing description")

    for ref in REQUIRED_REFS:
        path = SKILL_DIR / "references" / ref
        if not path.exists():
            issues.append(f"missing reference: references/{ref}")
        if f"references/{ref}" not in text:
            issues.append(f"SKILL.md does not link references/{ref}")

    if not (SKILL_DIR / "scripts").exists():
        issues.append("missing scripts directory")

    if issues:
        print("SuiLearn workflow skill check failed:")
        for issue in issues:
            print(f"- {issue}")
        return 1
    print("SuiLearn workflow skill check passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
