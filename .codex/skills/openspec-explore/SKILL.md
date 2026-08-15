---
name: openspec-explore
description: Thin wrapper around SuiLearn Explore state. Use when the user wants to explore an idea before creating a change.
license: MIT
compatibility: Requires openspec CLI.
---

# Explore

This is a compatibility wrapper. Follow SuiLearn Workflow instead:

1. Read `AGENTS.md` and `.agents/skills/suilearn-workflow/SKILL.md`.
2. Stay in Explore: read code/docs/tests, compare options, ask clarifying questions.
3. Do not write business code and do not create OpenSpec artifacts until the user approves entering Spec.
4. If the user confirms the change, create or reuse one `openspec/changes/<change-name>/**` directory using the Light/Standard/Major levels from `references/change-levels.md`.
