---
name: openspec-apply-change
description: Thin wrapper around SuiLearn Build state. Use when the user wants to implement an approved change.
license: MIT
compatibility: Requires openspec CLI.
---

# Apply

This is a compatibility wrapper. Follow SuiLearn Workflow instead:

1. Confirm an active change exists and is approved.
2. Read `docs/development-workflow.md` and `.agents/skills/suilearn-workflow/references/subagent-loop.md`.
3. Use the selected loop: L1 Light, L2 Standard, L2 Auto, or L3 Major.
4. Keep every edit inside the change's allowed paths.
5. Use the unified return format and do not self-certify completion.
