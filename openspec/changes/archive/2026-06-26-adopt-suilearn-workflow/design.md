# Design

## State Machine

SuiLearn uses five project-level states:

```text
Explore -> Spec --[Approval Gate]--> Build -> Verify --[Sync Gate]--> Archive
```

Artifact-level details stay inside the state instead of becoming separate
project states.

## Artifact Model

New change artifacts live under:

```text
openspec/changes/<change-name>/
  proposal.md
  design.md
  tasks.md
  specs/
  policy.md
  verification.md
  archive.md
```

Current facts remain in formal product, architecture, technology, and contract
documents.

## Subagent Build Loop

Each approved task runs through:

```text
Implementer -> Test -> Spec Review -> Code Review -> Fix
```

The main agent coordinates and adjudicates. Implementers do not self-certify.
P0/P1 issues loop back to Fix. Spec ambiguity returns to Spec.

## Policy Integration

`AGENTS.md` becomes the thin entry and mandatory gate document.
`docs/development-workflow.md` becomes the workflow body.
`agents/*.md` become role policy files.
`docs/proposals/**` is retired.

## Checker

`scripts/check-suilearn-workflow.ps1` rejects new files under retired
documentation paths and can be used before closing workflow changes.
