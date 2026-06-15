# Adopt SuiLearn Workflow

## Why

SuiLearn previously had overlapping process sources: project Agent rules,
proposal documents, OpenSpec-style change artifacts, and Superpowers-style
design/plan/execution skills. The overlap created conflict over which workflow
owned design, planning, implementation, testing, and review.

## What

Adopt a single native workflow:

```text
Explore -> Spec -> Build -> Verify -> Archive
```

The workflow absorbs:

- OpenSpec-style SDD artifacts and archive discipline.
- Superpowers-style subagent-driven development, TDD, debugging, and
  verification discipline.
- SuiLearn role, file, test, lock, and worktree policy.

## Scope

- Update global Agent rules.
- Rewrite the development workflow.
- Retire `docs/proposals/**` for new work.
- Add a `suilearn-workflow` skill.
- Update role files to act as policy files.
- Add a workflow policy checker.

## Non-goals

- No business feature implementation.
- No Android, backend, web, or contract behavior changes.
- No deletion of historical proposal files.
- No migration of all historical proposal content in this change.

## Acceptance Criteria

- New workflow is documented as the single process.
- New changes use `openspec/changes/**`.
- `docs/proposals/**` is marked retired.
- Superpowers design/plan document flows are not project fact sources.
- Build uses subagent implement/test/review/fix loops.
- Workflow policy checker runs successfully.
