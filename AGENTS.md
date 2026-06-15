# SuiLearn Agent Rules

## Core Rule

SuiLearn uses one native workflow:

```text
Explore -> Spec -> Build -> Verify -> Archive
```

This workflow absorbs the OpenSpec-style SDD lifecycle, Superpowers-style
subagent/TDD/debug/verification discipline, and SuiLearn role/file policy. Agents
must follow this file, `docs/development-workflow.md`, and the active role file.

## Precedence

1. User explicit instructions.
2. This `AGENTS.md` and the active role file.
3. `docs/development-workflow.md`.
4. Active `openspec/changes/<change-name>` artifacts.
5. Tool or skill defaults.

If a tool or skill tries to create a parallel design or planning flow, this
project workflow wins.

## Mandatory Gates

### Gate A: Before Modifying Files

Before any file edit:

1. Load the active role file from `agents/<role>.md`.
2. List planned files and compare each path with the role policy.
3. Record `base_ref`, normally current `HEAD`.
4. Check lock/worktree requirements from `docs/development-workflow.md`.
5. Run the relevant baseline test before business-code edits.

Documentation-only, workflow-only, and read-only review tasks may skip module
tests, but must state why tests are not applicable.

### Gate B: During Edits

Before every edit batch, declare:

```text
📝 本次修改: <file list>
```

If a new file is needed outside the declared list, stop and declare the expanded
scope before editing. If a task fails after three fix rounds on the same file,
stop and request workflow re-splitting or context reset.

### Gate C: Before Completion

Before claiming completion:

1. Run the required verification command, or state why it is not applicable.
2. Run `git diff <base_ref> --stat` or `git diff --stat` if no `base_ref`.
3. Check every changed file against the active role policy and task scope.
4. Report original test output for any command that was run.

Completion format:

```text
✅ 完成
改了什么: <summary>
测试结果: <raw output or not-applicable reason>
文件核对: <N files, all in scope / out-of-scope files: X>
```

### Gate D: Self Review

At task end, perform a quick reviewer-style self review:

```text
🔍 自我审查
[P0/P1/P2] issue — file
无阻塞问题 / 发现 N 个问题
```

## Workflow Entry

- Explore/spec/design/planning work belongs to the SuiLearn Workflow `Explore`
  and `Spec` states.
- Business-code implementation must come from an approved task in
  `openspec/changes/<change-name>/tasks.md`.
- Fast Track exception: low-risk single-role changes may use a lightweight task
  note instead of a full proposal/design package when `docs/development-workflow.md`
  classifies them as `Tiny`.
- Bug fixes should be represented by an OpenSpec change, an existing active task,
  or a Fast Track task note when the change is low-risk and does not alter product,
  architecture, contracts, storage, or cross-role behavior.
- `docs/proposals/**` is retired for new work.
- `docs/superpowers/specs/**` and `docs/superpowers/plans/**` are not project
  fact sources.

## Documentation Rules

- Current facts live in:
  - `docs/product-requirements.md`
  - `docs/architecture.md`
  - `docs/tech-selection.md`
  - `contracts/**`
- Future changes live in `openspec/changes/<change-name>/**`.
- Stable conclusions from completed changes must be synced back to the current
  fact documents before archive.
- `docs/chat.md` is inspiration and discussion material only. It is read-only by
  default and is not an implementation source.
- `docs/proposals/**` is historical migration material only.

## Role Directory

- Leader Agent: `agents/leader.md`
- Product Agent: `agents/product.md`
- Architect Agent: `agents/architect.md`
- Content Agent: `agents/content.md`
- Android Agent: `agents/android.md`
- Server Backend Agent: `agents/server-backend.md`
- Web Frontend Agent: `agents/web-frontend.md`
- Test Agent: `agents/test.md`
- Reviewer Agent: `agents/reviewer.md`

When no role is specified, infer the primary role, state the reasoning, and keep
the task inside that role's policy. Cross-role work is coordinated by Leader.

## Role Isolation

- Do not modify files outside the active role policy unless the user or Leader
  explicitly authorizes the expanded scope.
- Shared files and contracts require serial execution or worktree isolation.
- Implementation agents do not change product scope, technical baseline, or
  contracts for local convenience.
- If documents and implementation conflict, stop and ask whether to update the
  spec or the implementation.

## Subagent Policy

In `Build`, the main agent acts as coordinator. Implementation, testing, review,
and fixes should be delegated to fresh subagents when the task is non-trivial,
cross-role, high-risk, or user-requested.

Loop strength is risk-based:

```text
L1: Implementer -> Verify
L2: Implementer -> Test Agent -> Review
L3: Implementer -> Test Agent -> Spec Reviewer -> Code Reviewer -> Fix Agent
```

The implementer cannot self-certify completion. P0/P1 test or review issues
return to a fix round. Spec ambiguity, scope changes, architecture conflicts, and
out-of-scope edits return to `Spec` or require user confirmation.

## Retired Flows

Do not create new files under:

```text
docs/proposals/**
docs/superpowers/specs/**
docs/superpowers/plans/**
```

Use `openspec/changes/<change-name>/**` and the SuiLearn Workflow instead.
