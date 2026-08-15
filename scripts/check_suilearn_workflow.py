#!/usr/bin/env python3
"""SuiLearn workflow policy checker (cross-platform)."""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from fnmatch import fnmatchcase
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

PROTECTED_EXACT = {
    "docs/product-requirements.md",
    "docs/architecture.md",
    "docs/tech-selection.md",
}
PROTECTED_PREFIXES = (
    "apps/",
    "services/",
    "contracts/",
    "docs/architecture",
)
RETIRED_EXACT_ALLOWED = {
    "docs/proposals/README.md",
    "docs/proposals/_template.md",
}
RETIRED_PREFIXES = (
    "docs/proposals/",
    "docs/superpowers/specs/",
    "docs/superpowers/plans/",
)


def git(*args: str) -> str:
    return subprocess.run(
        ["git", "-c", f"safe.directory={ROOT}", *args],
        cwd=ROOT,
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    ).stdout.strip()


def norm(path: str) -> str:
    return path.replace("\\", "/").strip()


def default_base_ref() -> str:
    for remote in ("origin/dev", "origin/main"):
        ref = git("merge-base", "HEAD", remote)
        if ref:
            return ref
    ref = git("rev-parse", "HEAD")
    return ref or "HEAD"


def changed_paths(base_ref: str) -> set[str]:
    paths: set[str] = set()
    for line in git("diff", "--name-status", base_ref).splitlines():
        parts = line.split(None, 1)
        if len(parts) == 2:
            paths.add(norm(parts[1]))
    for line in git("status", "--porcelain").splitlines():
        if len(line) >= 4:
            paths.add(norm(line[3:]))
    return {p for p in paths if p}


def is_protected(path: str) -> bool:
    return path in PROTECTED_EXACT or path.startswith(PROTECTED_PREFIXES)


def is_retired_violation(path: str, new_or_modified: bool = True) -> bool:
    if path in RETIRED_EXACT_ALLOWED:
        return False
    if not path.startswith(RETIRED_PREFIXES):
        return False
    return new_or_modified or True


def active_changes() -> list[Path]:
    root = ROOT / "openspec" / "changes"
    if not root.exists():
        return []
    return [p for p in sorted(root.iterdir()) if p.is_dir() and p.name != "archive"]


def read_text(path: Path | None) -> str:
    if path is None or not path.exists():
        return ""
    try:
        return path.read_text(encoding="utf-8")
    except Exception:
        return ""


def change_texts(change: Path) -> tuple[str, str, str]:
    return (
        read_text(change / "tasks.md"),
        read_text(change / "policy.md"),
        read_text(change / "proposal.md"),
    )


def pattern_candidates(text: str) -> set[str]:
    candidates: set[str] = set()
    candidates.update(re.findall(r"`([^`\n]+)`", text))
    for line in text.splitlines():
        line = line.strip()
        if line.startswith("- "):
            token = line[2:].strip()
            if token and "/" in token:
                candidates.add(token)
    return {norm(c) for c in candidates if any(k in c for k in ("apps/", "services/", "contracts/", "docs/", "openspec/", "scripts/", ".agents/"))}


def match_pattern(pattern: str, path: str) -> bool:
    pattern = norm(pattern).strip()
    if not pattern:
        return False
    if pattern == path:
        return True
    if pattern.endswith("/**"):
        pattern = pattern[:-3]
    if pattern.endswith("/*"):
        pattern = pattern[:-2]
    if pattern.endswith("/"):
        pattern = pattern[:-1]
    if path == pattern or path.startswith(pattern + "/"):
        return True
    # Glob fallback.
    return fnmatchcase(path, pattern)


def covers(change_text: str, path: str) -> bool:
    return any(match_pattern(p, path) for p in pattern_candidates(change_text))


APPROVED_RE = re.compile(
    r"(?im)^\s*(?:(?:status|状态)\s*[:：]\s*)?(?:approved|已批准)(?:\s|$)|approved\s+by"
)


def is_approved(text: str) -> bool:
    return bool(APPROVED_RE.search(text))


OPEN_TASK_RE = re.compile(r"(?i)status\s*[:：]\s*(open|in progress|pending)|^\s*- \[ \]\s*", re.M)


def has_open_tasks(text: str) -> bool:
    return bool(OPEN_TASK_RE.search(text))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-ref", default="")
    parser.add_argument("--closing-change", default="")
    parser.add_argument("--self-test-efficient-batch-policy", action="store_true",
                        help="Deprecated; accepted for compatibility.")
    args = parser.parse_args()

    issues: list[str] = []
    base_ref = args.base_ref or default_base_ref()
    paths = changed_paths(base_ref)
    if args.self_test_efficient_batch_policy:
        print("SelfTestEfficientBatchPolicy is deprecated; structural checks are always active.")

    for path in sorted(paths):
        if is_retired_violation(path):
            issues.append(f"retired path changed: {path}")

    protected = sorted(p for p in paths if is_protected(p))
    if protected:
        changes = active_changes()
        if not changes:
            issues.append("protected paths changed without an active openspec change.")
            issues.extend(f"  protected: {p}" for p in protected)
        else:
            for path in protected:
                covering = []
                approved = []
                for change in changes:
                    tasks, policy, proposal = change_texts(change)
                    combined = "\n".join((tasks, policy, proposal))
                    if covers(combined, path):
                        covering.append(change.name)
                        if is_approved(combined):
                            approved.append(change.name)
                if not covering:
                    issues.append(f"protected path not covered by any active change: {path}")
                elif not approved:
                    issues.append(
                        f"protected path has no approved covering change: {path} "
                        f"(candidate changes: {', '.join(covering)})"
                    )

    lock_dir = ROOT / ".agents" / "locks"
    if lock_dir.is_dir():
        for lock_file in sorted(lock_dir.glob("*.json")):
            try:
                lock = json.loads(lock_file.read_text(encoding="utf-8"))
            except Exception:
                continue
            if str(lock.get("status", "")).lower() != "active":
                continue
            locked = lock.get("locked_paths") or []
            for pattern in locked:
                pattern_norm = norm(str(pattern))
                for path in paths:
                    if path == pattern_norm or path.startswith(pattern_norm.rstrip("/") + "/") or fnmatchcase(path, pattern_norm):
                        issues.append(
                            f"changed path is locked by {lock_file.name}: {path} "
                            f"(owner={lock.get('owner', 'unknown')})"
                        )

    if args.closing_change:
        change_root = ROOT / "openspec" / "changes" / args.closing_change
        if not change_root.exists():
            issues.append(f"closing change does not exist: {change_root}")
        else:
            tasks_path = change_root / "tasks.md"
            proposal_path = change_root / "proposal.md"
            policy_path = change_root / "policy.md"
            design_path = change_root / "design.md"
            verification_path = change_root / "verification.md"
            archive_path = change_root / "archive.md"
            specs_dir = change_root / "specs"

            tasks = read_text(tasks_path)
            proposal = read_text(proposal_path)
            policy = read_text(policy_path)
            combined = "\n".join((tasks, policy, proposal))

            is_major = specs_dir.is_dir() or (design_path.exists() and archive_path.exists())
            is_standard = proposal_path.exists() or policy_path.exists() or is_major

            if not tasks_path.exists():
                issues.append(f"{args.closing_change}: tasks.md required")
            if is_standard and not policy_path.exists():
                issues.append(f"{args.closing_change}: policy.md required for Standard/Major")
            if is_major:
                for required, name in (
                    (design_path, "design.md"),
                    (verification_path, "verification.md"),
                    (archive_path, "archive.md"),
                ):
                    if not required.exists():
                        issues.append(f"{args.closing_change}: {name} required for Major")
                if not specs_dir.is_dir():
                    issues.append(f"{args.closing_change}: specs/ required for Major")

            if tasks and has_open_tasks(tasks):
                issues.append(f"{args.closing_change}: tasks.md still has open tasks")
            if is_standard and not is_approved(combined):
                issues.append(f"{args.closing_change}: missing approval status")

            if is_major:
                verification = read_text(verification_path)
                archive = read_text(archive_path)
                if verification and not re.search(
                    r"(?im)^\s*(?:Status:\s*passed\.?|状态：\s*已通过。?)", verification
                ):
                    issues.append(f"{args.closing_change}: verification.md not passed")
                if archive and re.search(r"(?i)Status:\s*open", archive):
                    issues.append(f"{args.closing_change}: archive.md is still open")
                if archive and not re.search(r"(?im)^\s*(?:Deferred items:|延期项：)", archive):
                    issues.append(f"{args.closing_change}: archive.md missing deferred items")
                if archive and not re.search(r"(?i)review|审查", archive):
                    issues.append(f"{args.closing_change}: archive.md missing review summary")

    if issues:
        print("SuiLearn Workflow policy check failed:")
        for item in issues:
            print(f"- {item}")
        return 1

    print("SuiLearn Workflow policy check passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
