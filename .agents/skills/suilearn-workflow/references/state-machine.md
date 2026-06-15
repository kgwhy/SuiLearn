# SuiLearn Workflow State Machine

```text
Explore -> Spec --[Approval Gate]--> Build -> Verify --[Sync Gate]--> Archive
             ^                           |
             +---- spec issue found -----+
```

## Explore

Clarify intent, constraints, options, and risks. Do not write business code.

## Spec

Create or update:

- `proposal.md`
- `design.md`
- `tasks.md`
- `specs/**`
- `policy.md`

Use change classes:

- `Tiny`: tasks + policy.
- `Normal`: proposal + design + tasks + policy.
- `Major`: proposal + design + specs + tasks + policy + verification + archive.

## Approval Gate

Confirm owner, file scope, forbidden files, tests, `base_ref`, locks, and
worktree mode.

## Build

Run the subagent loop for each approved task.

## Verify

Collect final test output, diff stat, file-scope review, and final review.

## Archive

Sync stable facts to current documents and record implementation references.
