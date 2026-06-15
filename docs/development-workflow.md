# SuiLearn Workflow

SuiLearn Workflow is the single development lifecycle for this repository. It
absorbs an OpenSpec-inspired SDD state machine, Superpowers-inspired
subagent/TDD/debug/verification discipline, and SuiLearn role/file policy.

Project-level state machine:

```text
Explore -> Spec --[Approval Gate]--> Build -> Verify --[Sync Gate]--> Archive
             ^                           |
             +---- spec issue found -----+
```

## Principles

- One lifecycle: do not run a parallel proposal, design, or plan system.
- One change home: new changes live under `openspec/changes/<change-name>/**`.
- One current-fact set: stable facts live in the product, architecture,
  technology, and contract documents.
- Main agents coordinate; focused subagents implement, test, review, and fix.
- Evidence precedes completion claims.

## Change Class

Use the smallest class that protects the work. The class controls artifact
weight, approval strictness, and Build loop strength.

| Class | Use when | Required artifacts | Default Build loop |
|---|---|---|---|
| Tiny | Single role, no product/architecture/contract/storage change, normally <=2 files | `tasks.md` task note and `policy.md` entry | L1 |
| Normal | User-facing behavior, multi-file implementation, or meaningful test work | `proposal.md`, `design.md`, `tasks.md`, `policy.md` | L2 |
| Major | Cross-role, shared files, contracts, storage, architecture, workflow, or high-risk changes | proposal, design, specs, tasks, policy, verification, archive notes | L3 |

Fast Track is allowed only for `Tiny` work. A Tiny task still needs `base_ref`,
allowed files, forbidden files, verification command, and final diff/file-scope
evidence. If implementation reveals broader scope, immediately reclassify to
`Normal` or `Major` and return to `Spec`.

## State: Explore

Purpose: understand the problem, constraints, risks, and whether the work should
become a change.

Allowed:

- Read docs, code, tests, logs, and CodeGraph context.
- Ask clarifying questions.
- Compare approaches and identify risks.

Not allowed:

- Write business code.
- Treat `docs/chat.md` or conversation ideas as confirmed requirements.

Exit criteria:

- The problem and intended outcome can be stated clearly, or the work is stopped
  as not ready.

## State: Spec

Purpose: produce the complete change package.

Location:

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

Minimum artifacts by class:

- `Tiny`: `tasks.md` and `policy.md`.
- `Normal`: `proposal.md`, `design.md`, `tasks.md`, and `policy.md`.
- `Major`: `proposal.md`, `design.md`, `specs/**`, `tasks.md`, `policy.md`,
  `verification.md`, and `archive.md`.

Artifact purposes:

- `proposal.md`: what, why, scope, non-goals, acceptance criteria.
- `design.md`: approach, data/API effects, alternatives, risks.
- `tasks.md`: executable tasks with owners, allowed files, tests, and review
  expectations.
- `policy.md`: role ownership, class, `base_ref`, file locks, worktree mode,
  allowed paths, and forbidden paths.

OpenSpec CLI may be used to create and inspect changes. When CLI output provides
`changeRoot`, `artifactPaths`, or artifact ordering, use those concrete values
instead of guessing paths.

Exit criteria: the Approval Gate passes.

## Approval Gate

Before entering `Build`, Leader must confirm:

- Scope, non-goals, acceptance criteria, and affected current-fact documents are
  explicit.
- Every task has an owner, allowed files, forbidden files, test command, and
  review focus.
- `base_ref` is recorded.
- Existing worktree changes are classified as in-scope or pre-existing.
- Shared files use strict serial execution or worktree isolation.
- Active lock records do not overlap the new task scope.
- Business-code tasks record one baseline state:
  - `passed`: command and output recorded.
  - `unavailable`: missing tool, dependency, emulator, service, or network reason
    recorded with fallback verification.
  - `skipped`: allowed only for Tiny or doc-only work, with reason.

## State: Build

Purpose: execute each approved task through a risk-appropriate loop.

Build loop levels:

```text
L1 Tiny:
  Implementer -> Verify

L2 Normal:
  Implementer -> Test -> Review -> Fix when needed

L3 Major:
  Implementer -> Test -> Spec Review -> Code Review -> Fix -> repeat
```

Coordinator responsibilities:

- Select the loop level from the change class and task risk.
- Dispatch fresh subagents with only task-local context when the chosen level
  requires subagents.
- Provide the exact task text, relevant artifact excerpts, allowed/forbidden
  paths, test command, and expected return format.
- Keep tasks serial when files overlap.
- Decide whether failures return to Fix, return to Spec, or block for user
  confirmation.

Implementer rules:

- Behavior changes follow RED -> GREEN -> REFACTOR.
- Bug fixes start with a failing regression test or explicit reproduction steps.
- Refactors require protection tests or equivalent verification.
- Do the smallest change that satisfies the task.
- Return changed files, tests run, assumptions, and blockers.

Test subagent rules:

- Run the task's required commands independently when possible.
- Report raw command output, exit code, failures, and manual-test fallback.
- Do not accept implementer self-test as final evidence.

Review subagent rules:

- L2 may use a combined review when the task is single-role and low-risk.
- L3 always separates Spec Review from Code Review.
- Spec Review runs before Code Review.
- Spec review checks proposal/design/specs/tasks compliance.
- Code review checks role boundaries, quality, maintainability, test sufficiency,
  overbuilding, and regression risk.
- P0/P1 issues must be fixed and re-reviewed before the task can complete.

Stop conditions:

- Same task needs more than three fix rounds.
- A required file is outside the approved scope.
- Product, architecture, contract, or data decisions are ambiguous.
- Tests cannot run and no credible manual verification can be defined.

## State: Verify

Purpose: prove the whole change is ready to close.

Leader must:

- Run or collect final module test/build output.
- Run `git diff <base_ref> --stat`.
- Check changed files against `policy.md` and role policies.
- Confirm every task in `tasks.md` is complete or explicitly deferred.
- Dispatch final review for broad changes.
- Record verification evidence in `verification.md` when the change is large or
  implementation spans multiple tasks.

Exit criteria: the Sync Gate passes.

## Sync Gate

Before archive:

- Stable product conclusions are synced to `docs/product-requirements.md`.
- Stable architecture conclusions are synced to `docs/architecture.md`.
- Stable technical baseline decisions are synced to `docs/tech-selection.md`.
- Stable contracts are synced to `contracts/**`.
- If a category is unaffected, record "not affected" in the archive note.
- Unfinished items are moved to a new change or listed as deferred.

## State: Archive

Purpose: close the change and preserve history.

Archive record must include:

- Change name.
- Final status.
- Implementation reference: commit, PR, task card, or working-tree reference.
- Verification summary.
- Synced current-fact documents.
- Deferred items, if any.

Completed changes move to `openspec/changes/archive/` when using the OpenSpec
directory layout. If an OpenSpec CLI archive command is available, prefer it.

## Current Fact Documents

```text
docs/chat.md                  # inspiration only, read-only by default
docs/product-requirements.md  # product truth source
docs/architecture.md          # architecture and module boundaries
docs/tech-selection.md        # technology decisions and constraints
contracts/**                  # cross-platform contract truth source
```

Retired:

```text
docs/proposals/**             # historical proposal material only
docs/superpowers/specs/**     # not a project fact source
docs/superpowers/plans/**     # not a project fact source
```

## File Ownership

| Scope | Default owner |
| --- | --- |
| `docs/product-requirements.md` | Product Agent |
| `docs/architecture*.md` | Architect Agent |
| `docs/tech-selection.md` | Architect Agent |
| `contracts/**` | Architect Agent |
| `apps/android/**` | Android Agent |
| `services/api/**` | Server Backend Agent |
| `apps/web/**` | Web Frontend Agent |
| Content source files and content guidelines | Content Agent |
| Test code and test reports | Test Agent, or owning implementation Agent when task-scoped |
| `AGENTS.md`, `docs/development-workflow.md`, `docs/index.md` | Leader Agent |
| `openspec/changes/**` | Leader coordinates; owner depends on change scope |

Implementation agents may modify only their owned scope unless `policy.md` or
Leader explicitly authorizes more.

## Lock And Worktree Rules

- Same file, same time, one owner.
- Shared files require strict serial execution or worktree isolation.
- Contract changes happen before consumer adaptation.
- Parallel consumer adaptation may start only after contracts are stable.
- Persistent locks may be stored in `.agents/locks/<task-id>.json`.

Minimum lock record:

```json
{
  "task_id": "short-task-name",
  "owner": "Agent name",
  "base_ref": "commit sha",
  "mode": "serial | worktree",
  "status": "active | released",
  "locked_paths": ["path/**"],
  "created_at": "YYYY-MM-DD"
}
```

## Test Commands

| Scope | Windows / PowerShell | Unix shell |
|---|---|---|
| Android unit tests | `.\gradlew.bat :app:testDebugUnitTest --no-daemon` | `./gradlew :app:testDebugUnitTest --no-daemon` |
| Android build | `.\gradlew.bat :app:assembleDebug --no-daemon` | `./gradlew :app:assembleDebug --no-daemon` |
| Backend tests | `mvn -f services/api/pom.xml test -q` | `mvn -f services/api/pom.xml test -q` |
| Web build | `npm --prefix apps/web run build` | `npm --prefix apps/web run build` |
| Workflow policy check | `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1` | Use PowerShell Core if available |

If a tool is unavailable, report the exact reason and provide a manual
verification checklist.

## Subagent Dispatch Template

Each subagent prompt must include:

- Role.
- Task name and exact task text.
- Relevant artifact excerpts.
- Allowed files and forbidden files.
- `base_ref` and review diff command.
- Required tests.
- Return format.
- Whether writing files is allowed.

Return format:

```text
STATUS: DONE / DONE_WITH_CONCERNS / NEEDS_CONTEXT / BLOCKED
Changed files:
Tests:
Summary:
Assumptions:
Blockers:
```

## Review Severity

- P0: blocking runtime, data loss, security, severe scope violation.
- P1: core behavior wrong, major architecture violation, important missing test.
- P2: maintainability, edge-case, or moderate regression risk.
- P3: style, naming, or minor documentation issue.

P0/P1 blocks completion.

## Workflow Policy Checker

Run the checker before closing workflow changes and in CI when possible:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef <base_ref>
```

The checker rejects changes under retired documentation flows and flags common
workflow drift, including protected implementation/fact-document changes without
an active OpenSpec change containing `tasks.md` and `policy.md`.
