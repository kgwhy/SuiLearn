# Subagent Build Loop

Use the smallest loop level that protects the task. Use fresh subagents with
task-local context only when the loop level requires them.

```text
L1 Tiny: Implementer -> Verify
L2 Normal: Implementer -> Test -> Review -> Fix
L3 Major: Implementer -> Test -> Spec Review -> Code Review -> Fix
```

## Implementer

Input:

- exact task text
- relevant artifact excerpts
- allowed and forbidden paths
- test command
- expected output format

Rules:

- Use TDD for behavior changes.
- Reproduce bugs before fixing.
- Keep changes minimal.

## Test

Run required commands independently and report raw output.

## Spec Review

Check the implementation against proposal, design, specs, and tasks.

## Code Review

Check quality, boundaries, maintainability, tests, and regression risk.

## Fix

Fix only the reported issue, then re-run test and review. Stop after three
unsuccessful fix rounds.
