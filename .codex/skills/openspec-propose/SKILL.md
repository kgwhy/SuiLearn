---
name: openspec-propose
description: Thin wrapper around SuiLearn Spec state. Use when the user wants to propose a change.
license: MIT
compatibility: Requires openspec CLI.
---

# Propose

This is a compatibility wrapper. Follow SuiLearn Workflow instead:

1. Read `docs/development-workflow.md` and `.agents/skills/suilearn-workflow/references/change-levels.md`.
2. Light: create only `tasks.md` with owner, allowed/forbidden files, verification and completion.
3. Standard: create `tasks.md` + `policy.md`; add `proposal.md` only for new features or cross-module work.
4. Major: create proposal, design, specs, tasks, policy, verification, archive.
5. Do not enter Build until `Status: Approved` / `状态：已批准` is recorded.
