---
name: suilearn-workflow
description: Coordinate SuiLearn development through the native Explore -> Spec -> Build -> Verify -> Archive workflow. Use for SuiLearn changes, implementation tasks, workflow decisions, or when deciding how OpenSpec-style SDD, subagent-driven development, TDD, testing, review, and project role/file policy fit together.
---

# SuiLearn Workflow

Use this skill as the single workflow router for SuiLearn. It is not a parallel
planning system and must not create `docs/superpowers/specs/**`,
`docs/superpowers/plans/**`, or new `docs/proposals/**`.

## State Machine

```text
Explore -> Spec --[Approval Gate]--> Build -> Verify --[Sync Gate]--> Archive
             ^                           |
             +---- spec issue found -----+
```

- **Explore:** clarify the problem; do not write business code.
- **Spec:** create or update `openspec/changes/<change-name>/**` artifacts.
- **Build:** execute tasks through fresh subagents.
- **Verify:** collect final tests, diff, and review evidence.
- **Archive:** sync stable facts and close the change.

Read `docs/development-workflow.md` for the full project policy. Use the
reference files in this skill only when you need compact reminders:

- `references/state-machine.md`
- `references/subagent-loop.md`
- `references/policy-gates.md`

## Routing Rules

When the request is exploratory, stay in `Explore`. You may read files and ask
questions, but do not write code.

When the request changes behavior, architecture, product scope, contracts, or
workflow, enter `Spec` and use `openspec/changes/<change-name>/**` as the change
home. Classify the change as `Tiny`, `Normal`, or `Major`; use Fast Track only
for Tiny work.

When an approved task is ready, enter `Build`. The main agent coordinates; fresh
subagents implement, test, review, and fix.

When claiming completion, enter `Verify` first. Run or collect fresh evidence
before making success claims.

When the change is complete, enter `Archive`: sync stable facts, record
verification and implementation references, and archive the change.

## Build Loop

For each task:

```text
L1 Tiny:  Implementer -> Verify
L2 Normal: Implementer -> Test -> Review -> Fix
L3 Major: Implementer -> Test -> Spec Review -> Code Review -> Fix
```

P0/P1 test or review issues return to Fix. Scope ambiguity, architecture
conflict, contract change, or out-of-policy files return to Spec or require user
confirmation.

## Non-Negotiables

- Use one change home: `openspec/changes/<change-name>/**`.
- Do not create new `docs/proposals/**`.
- Do not create Superpowers design or plan documents.
- Use the smallest valid change class; reclassify upward as soon as scope grows.
- Business-code changes require TDD or explicit reproduction steps.
- The implementer cannot self-certify completion.
- Evidence comes before completion claims.
