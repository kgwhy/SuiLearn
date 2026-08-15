---
name: openspec-sync-specs
description: Thin wrapper around SuiLearn Sync Gate. Use when delta specs should be synced to main specs.
license: MIT
compatibility: Requires openspec CLI.
---

# Sync Specs

This is a compatibility wrapper. Follow SuiLearn Workflow instead:

1. Read `docs/development-workflow.md` Sync Gate section.
2. Compare delta specs with `openspec/specs/<capability>/spec.md`.
3. Apply additions/modifications/removals intelligently and idempotently.
4. Record synchronized or not-affected facts before archive.
