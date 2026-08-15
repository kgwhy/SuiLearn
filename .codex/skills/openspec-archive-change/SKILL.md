---
name: openspec-archive-change
description: Thin wrapper around SuiLearn Archive state. Use when the user wants to archive a completed change.
license: MIT
compatibility: Requires openspec CLI.
---

# Archive

This is a compatibility wrapper. Follow SuiLearn Workflow instead:

1. Ask the user which active change to archive if it is not clear.
2. Run `python3 scripts/check_suilearn_workflow.py --closing-change <change-name>`.
3. Confirm facts are synced or `not affected`.
4. Run `python3 scripts/archive_openspec_change.py --change-name <change-name>`.
5. Do not use a flat manual `mv` and do not maintain a domain archive index.

The change is moved to `openspec/changes/archive/YYYY-MM-DD-<change-name>/`.
