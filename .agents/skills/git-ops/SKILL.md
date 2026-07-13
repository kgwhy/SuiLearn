---
name: git-ops
description: 当用户要求查看或操作 Git 状态、差异、分支、暂存、提交、推送、提交信息或交接时使用。
---

# Git 操作

以最小、可审查的 Git 操作完成用户请求。未提交的变更默认属于用户；只暂存本次请求涉及的文件。

## 固定规则

- 每次写操作前运行 `git status --short --branch`，再查看相关的 `git diff` 与 `git diff --cached`。
- 避免交互式命令；未经用户明确授权，不执行强制推送、删除分支、`reset --hard`、历史改写或恢复工作区文件。
- 创建 Codex 分支时默认使用 `codex/` 前缀；发布顺序为 `commit -> pull --rebase -> push`。
- 任何提交都必须在暂存审阅后检查要提交代码是否暴露 key、secret 或 access token：运行 `powershell -NoProfile -ExecutionPolicy Bypass -File .agents/skills/git-ops/scripts/scan-staged-secrets.ps1`。预检命中或无法安全完成时，停止提交，不回显匹配内容，并请用户决定处置方式。

## 按需加载

| 场景 | 读取内容 |
| --- | --- |
| 只读状态、差异、日志、分支查询 | 本文件即可 |
| 暂存、提交、检查待提交内容 | [提交前预检](references/commit-preflight.md) 与 [提交信息](references/commit-messages.md) |
| 需要选择提交类型或破坏性变更标记 | [Conventional Commits](references/conventional-commits.md) |
| 创建/切换分支、拉取、推送或处理冲突 | [分支与发布](references/branches-and-publishing.md) |

## 提交顺序

1. 检查状态与相关 diff，确认提交边界。
2. 有意暂存指定文件。
3. 运行 `powershell -NoProfile -ExecutionPolicy Bypass -File .agents/skills/git-ops/scripts/scan-staged-secrets.ps1`，检查暂存代码是否暴露 key、secret 或 access token；只有退出码为 `0` 才准备提交信息并运行 `git commit`。
4. 仅在用户要求发布时，执行 `git pull --rebase` 后再推送。

交接时报告分支、已暂存或提交的文件、提交哈希、拉取/推送结果、已运行的检查以及保持未动的无关脏文件。
