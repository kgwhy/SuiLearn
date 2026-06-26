# 策略

## 变更

- 名称：`complete-v2-ai-rag-release`
- 等级：Major
- base_ref：`3f3fe48b8c940ed3be2d922e6739d143c7e122c1`
- Worktree 模式：契约和共享事实文档串行执行；契约稳定后消费端适配可以拆分。

## 允许路径

本变更有意跨多个角色，但每个任务必须使用 `tasks.md` 中自己的角色本地允许文件。

协调者拥有：

- `openspec/changes/complete-v2-ai-rag-release/**`

## 禁止路径

- `docs/proposals/**`
- `docs/superpowers/**`
- 相关任务获批前，不得跨角色修改实现文件。

## 基线

本 Major 变更启动时尚未开始业务代码实现。进入 Build 前，必须按任务记录所需基线。
