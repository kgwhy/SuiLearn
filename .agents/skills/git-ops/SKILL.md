---
name: git-ops
description: Git operation workflow for inspecting repository state, creating or switching branches, staging intentional changes, writing Conventional Commits messages, committing, pushing, and preparing PR handoff notes. Use when Codex is asked to perform git status/diff/log/branch/stage/commit/push operations, draft commit messages, split changes into commits, or verify git hygiene before handing work back.
---

# Git Ops

## Core Rules

- Inspect repository state before every write operation with `git status --short --branch`.
- Treat uncommitted changes as user-owned unless this session created them or the user explicitly says otherwise.
- Stage only files that belong to the requested change. Do not stage unrelated edits just because they are present.
- Prefer non-interactive commands. Avoid interactive rebase, patch staging prompts, or editor-driven commit flows unless the user asks.
- When committing and publishing work, use the order `commit -> pull --rebase -> push`.
- Do not run destructive history or working-tree commands (`reset --hard`, `checkout -- <file>`, force push, branch delete, rebase) unless the user clearly requested that operation and the risk is understood.
- When creating a branch in Codex desktop, use the project branch prefix `codex/` unless the user requested another prefix.

## Workflow

1. Read the current state:

```powershell
git status --short --branch
```

2. Inspect changes before staging:

```powershell
git diff -- <path>
git diff --cached -- <path>
```

3. Decide commit boundaries:

- Keep unrelated work in separate commits.
- If the user asks for one commit and the changed files are coherent, make one commit.
- If files cross clear responsibilities, propose or create multiple commits only when the user asked for commit splitting.

4. Validate before committing when practical:

- Run the smallest relevant test, build, formatter, or static check for the changed area.
- If validation is unavailable or too expensive, state that in the handoff.

5. Stage intentionally:

```powershell
git add -- <path1> <path2>
```

6. Commit with Conventional Commits:

```powershell
git commit -m "type(scope): 中文提交说明" `
  -m "变更摘要：
- 说明主要改动 1
- 说明主要改动 2

验证：
- 说明已运行的验证命令或未运行原因

风险与备注：
- 说明兼容性、迁移、遗留风险或“无”"
```

7. Pull after committing and before pushing:

```powershell
git pull --rebase
```

If pull reports conflicts, stop, inspect the conflict files, resolve only conflicts related to the committed work, rerun the relevant validation, and continue only after the rebase completes cleanly.

8. Push only when requested:

```powershell
git push -u origin <branch>
```

## Commit Message Standard

Use Conventional Commits 1.0.0 with Chinese commit message text. See `references/conventional-commits.md` for details and examples.

Format:

```text
<type>[optional scope][optional !]: <description>

<required body>

[optional footer(s)]
```

Default rule: do **not** create title-only commits. Every commit created by this skill should include a body unless the user explicitly asks for a title-only commit.

The commit body must be concise but useful, and should include these sections in Chinese:

```text
变更摘要：
- <说明用户可见或架构相关的主要改动>
- <说明第二个关键改动；没有则省略>

验证：
- <已运行的测试、构建、检查命令及结果；或说明未运行原因>

风险与备注：
- <兼容性、迁移、未验证范围、遗留风险；没有则写“无”>
```

For very small commits, keep the same section headers but use one bullet per section. Do not pad with vague text.

Choose the type by intent:

- `feat`: introduces a user-visible or API feature.
- `fix`: patches a bug.
- `docs`: documentation-only change.
- `style`: formatting-only change with no behavior impact.
- `refactor`: code change that is neither a feature nor a bug fix.
- `perf`: performance improvement.
- `test`: adds or corrects tests.
- `build`: build system or dependency change.
- `ci`: CI configuration or pipeline change.
- `chore`: maintenance that does not fit the above.
- `revert`: reverts a previous commit.

Use a scope when it clarifies the affected area, such as `android`, `server`, `docs`, `gradle`, `ui`, `auth`, or a package/module name.

Write the commit description, body, and explanatory footer values in Chinese. Keep the Conventional Commits type, scope, `!`, and footer tokens such as `BREAKING CHANGE` in English for tooling compatibility.

Mark breaking changes in either of these ways:

```text
feat(api)!: 调整课程进度响应结构
```

```text
BREAKING CHANGE: 课程进度现在返回检查点对象，而不是 ID 列表。
```

## Handoff

After git operations, report:

- branch name when created or switched;
- files staged or committed;
- commit hash and message when a commit succeeds;
- pull result before push, including any conflict resolution;
- push destination when a push succeeds;
- validation command and result;
- any unrelated dirty files left untouched.
