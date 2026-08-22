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
    "verification-selection.md",
    "ui-evidence.md",
    "archive-organization.md",
]


def fail(msg: str) -> None:
    print(f"- {msg}")


def check_skill_frontmatter(skill_dir: Path, expected_name: str) -> list[str]:
    issues: list[str] = []
    skill = skill_dir / "SKILL.md"
    if not skill.exists():
        return ["missing SKILL.md"]
    text = skill.read_text(encoding="utf-8")
    front = re.match(r"^---\s*\n(.*?)\n---\s*\n", text, re.S)
    if not front:
        issues.append("SKILL.md missing YAML frontmatter")
        return issues
    body = front.group(1)
    if f"name: {expected_name}" not in body:
        issues.append(f"frontmatter missing name: {expected_name}")
    if "description:" not in body:
        issues.append("frontmatter missing description")
    return issues


def main() -> int:
    issues: list[str] = []
    issues.extend(check_skill_frontmatter(SKILL_DIR, "suilearn-workflow"))
    text = ""
    skill = SKILL_DIR / "SKILL.md"
    if skill.exists():
        text = skill.read_text(encoding="utf-8")

    for ref in REQUIRED_REFS:
        path = SKILL_DIR / "references" / ref
        if not path.exists():
            issues.append(f"missing reference: references/{ref}")
        if f"references/{ref}" not in text:
            issues.append(f"SKILL.md does not link references/{ref}")

    if not (SKILL_DIR / "scripts").exists():
        issues.append("missing scripts directory")

    skills_root = ROOT / ".agents" / "skills"
    for skill_dir in sorted(skills_root.glob("*/")):
        name = skill_dir.name
        if name == "suilearn-workflow":
            continue
        if (skill_dir / "SKILL.md").exists():
            issues.extend(check_skill_frontmatter(skill_dir, name))

    if issues:
        print("SuiLearn workflow skill check failed:")
        for issue in issues:
            print(f"- {issue}")
        return 1
    print("SuiLearn workflow skill check passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
