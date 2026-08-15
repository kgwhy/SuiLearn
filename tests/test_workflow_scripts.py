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
