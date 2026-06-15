# SuiLearn Proposal Directory Retired

`docs/proposals/**` is retired for new work.

SuiLearn now uses `openspec/changes/<change-name>/**` as the single change home
for proposals, designs, tasks, specs, verification notes, and archive records.

## Current Rule

- Do not create new proposal files in this directory.
- Do not use files in this directory as implementation authority for new tasks.
- Use `docs/development-workflow.md` for the SuiLearn Workflow.
- Use `openspec/changes/**` for new change artifacts.

## Historical Material

Existing files, including `_template.md`, remain only as migration or historical
reference. Do not copy `_template.md` for new work. If a historical
proposal still contains useful stable conclusions, move those conclusions into
the current fact documents:

- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/tech-selection.md`
- `contracts/**`

If the historical proposal contains unfinished work, create a new
`openspec/changes/<change-name>/` package and carry the unfinished items there.

## Replacement Mapping

| Old proposal flow | SuiLearn Workflow replacement |
|---|---|
| `docs/proposals/*.md` | `openspec/changes/<change-name>/proposal.md` |
| Proposal impact sections | `proposal.md` and `design.md` |
| Proposal migration plan | `tasks.md` |
| Proposal implementation gate | Approval Gate in `policy.md` |
| Proposal closeout | Sync Gate and `archive.md` |
| Proposal status | SuiLearn Workflow state |
