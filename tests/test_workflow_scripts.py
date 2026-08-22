"""Smoke tests for SuiLearn workflow helpers."""
from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "check_suilearn_workflow.py"
spec = importlib.util.spec_from_file_location("suilearn_workflow_check", SCRIPT)
mod = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(mod)


class ProtectedPathTests(unittest.TestCase):
    def test_protected_paths(self):
        for path in (
            "apps/android/src/Main.kt",
            "services/api/pom.xml",
            "contracts/openapi/suilearn-v2.yaml",
            "docs/architecture.md",
            "docs/tech-selection.md",
        ):
            self.assertTrue(mod.is_protected(path), path)

    def test_unprotected_paths(self):
        for path in (
            "docs/plans/example.md",
            "scripts/check.sh",
            "AGENTS.md",
            "openspec/changes/example/tasks.md",
        ):
            self.assertFalse(mod.is_protected(path), path)


class PatternTests(unittest.TestCase):
    def test_match_pattern(self):
        self.assertTrue(mod.match_pattern("apps/android/**", "apps/android/src/Main.kt"))
        self.assertTrue(mod.match_pattern("services/api/**", "services/api/pom.xml"))
        self.assertFalse(mod.match_pattern("apps/web/**", "services/api/pom.xml"))

    def test_pattern_candidates(self):
        text = "允许文件：`apps/android/**`\n- services/api/**\n- 普通说明"
        candidates = mod.pattern_candidates(text)
        self.assertIn("apps/android/**", candidates)
        self.assertIn("services/api/**", candidates)


class StatusTests(unittest.TestCase):
    def test_approved_status(self):
        self.assertTrue(mod.is_approved("Status: Approved"))
        self.assertTrue(mod.is_approved("状态：已批准"))
        self.assertFalse(mod.is_approved("Status: open"))

    def test_open_tasks(self):
        self.assertTrue(mod.has_open_tasks("- [ ] add tests"))
        self.assertTrue(mod.has_open_tasks("Status: in progress"))
        self.assertFalse(mod.has_open_tasks("- [x] done"))


if __name__ == "__main__":
    unittest.main()


import importlib.util
import subprocess
import tempfile

AGENT_NOTES_SCRIPT = ROOT / "scripts" / "check_agent_notes.py"
agent_notes_spec = importlib.util.spec_from_file_location("suilearn_agent_notes_check", AGENT_NOTES_SCRIPT)
agent_notes_mod = importlib.util.module_from_spec(agent_notes_spec)
assert agent_notes_spec.loader is not None
agent_notes_spec.loader.exec_module(agent_notes_mod)

CHANGE_SCOPE_SCRIPT = ROOT / "scripts" / "change_scope.py"
change_scope_spec = importlib.util.spec_from_file_location("suilearn_change_scope", CHANGE_SCOPE_SCRIPT)
change_scope_mod = importlib.util.module_from_spec(change_scope_spec)
assert change_scope_spec.loader is not None
change_scope_spec.loader.exec_module(change_scope_mod)


class AgentNotesFormatTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        root = Path(self.tmp.name)
        self.old_root = agent_notes_mod.ROOT
        self.old_notes = agent_notes_mod.NOTES
        agent_notes_mod.ROOT = root
        agent_notes_mod.NOTES = root / ".agents" / "notes"

    def tearDown(self):
        agent_notes_mod.ROOT = self.old_root
        agent_notes_mod.NOTES = self.old_notes
        self.tmp.cleanup()

    def write_note(self, lifecycle: str, class_name: str, name: str, body: str):
        path = agent_notes_mod.NOTES / lifecycle / class_name / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(body, encoding="utf-8")

    def test_valid_implemented_note(self):
        self.write_note(
            "implemented",
            "process",
            "2026-08-19-test.md",
            "# Agent Note: 测试\nStatus: implemented\n\n## Problem\n## Decision\n"
            "## Alternatives considered\n## Consequences\n",
        )
        self.assertEqual(agent_notes_mod.collect_issues(), [])

    def test_status_must_match_directory(self):
        self.write_note(
            "proposed",
            "process",
            "2026-08-19-test.md",
            "# Agent Note: 测试\nStatus: implemented\n\n## Problem\n## Proposal\n"
            "## Alternatives considered\n## Acceptance criteria\n## Risks\n",
        )
        issues = agent_notes_mod.collect_issues()
        self.assertTrue(any("does not match directory" in issue for issue in issues))

    def test_implemented_rejects_acceptance_criteria(self):
        self.write_note(
            "implemented",
            "process",
            "2026-08-19-test.md",
            "# Agent Note: 测试\nStatus: implemented\n\n## Problem\n## Decision\n"
            "## Alternatives considered\n## Acceptance criteria\n## Consequences\n",
        )
        issues = agent_notes_mod.collect_issues()
        self.assertTrue(any("must not contain ## Acceptance criteria" in issue for issue in issues))

    def test_rejected_status_needs_reason(self):
        self.write_note(
            "rejected",
            "process",
            "2026-08-19-test.md",
            "# Agent Note: 测试\nStatus: rejected\n\n## Problem\n## Proposal\n"
            "## Alternatives considered\n",
        )
        issues = agent_notes_mod.collect_issues()
        self.assertTrue(any("must carry a one-line reason" in issue for issue in issues))


class ChangeScopeNormTests(unittest.TestCase):
    def test_norm_backslashes(self):
        self.assertEqual(change_scope_mod.norm("apps\\android\\Main.kt"), "apps/android/Main.kt")


class ChangeScopeRepoTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        subprocess.run(["git", "init", "-q"], cwd=self.root, check=True)
        subprocess.run(["git", "config", "user.email", "test@example.com"], cwd=self.root, check=True)
        subprocess.run(["git", "config", "user.name", "Test"], cwd=self.root, check=True)
        (self.root / "a.txt").write_text("base\n", encoding="utf-8")
        subprocess.run(["git", "add", "a.txt"], cwd=self.root, check=True)
        subprocess.run(["git", "commit", "-q", "-m", "base"], cwd=self.root, check=True)
        self.base = subprocess.run(
            ["git", "rev-parse", "HEAD"], cwd=self.root, check=True,
            stdout=subprocess.PIPE, text=True,
        ).stdout.strip()
        self.old_root = change_scope_mod.ROOT
        change_scope_mod.ROOT = self.root

    def tearDown(self):
        change_scope_mod.ROOT = self.old_root
        self.tmp.cleanup()

    def test_changed_paths_classification(self):
        (self.root / "a.txt").write_text("modified\n", encoding="utf-8")
        (self.root / "b.txt").write_text("staged\n", encoding="utf-8")
        subprocess.run(["git", "add", "b.txt"], cwd=self.root, check=True)
        (self.root / "d.txt").write_text("untracked\n", encoding="utf-8")
        paths = change_scope_mod.changed_paths(self.base)
        self.assertEqual(paths["staged"], ["b.txt"])
        self.assertEqual(paths["unstaged"], ["a.txt"])
        self.assertEqual(paths["untracked"], ["d.txt"])
        self.assertEqual(paths["committed"], [])


if __name__ == "__main__":
    unittest.main()
