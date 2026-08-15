# 归档

Status: passed

## 变更名称

添加 GitHub Actions CI。

## 实现引用

working tree：

- `.github/workflows/ci.yml`
- `openspec/changes/2026-06-30-add-github-actions-ci/tasks.md`
- `openspec/changes/2026-06-30-add-github-actions-ci/policy.md`
- `openspec/changes/2026-06-30-add-github-actions-ci/verification.md`
- `openspec/changes/2026-06-30-add-github-actions-ci/archive.md`

## 验证摘要

- 工作流检查、Android 单元测试、Android debug 构建、Web 测试和 Web 构建已通过。
- 后端 CI job 使用 PostgreSQL service 补齐本地失败的数据库前置条件。
- `push` 触发限制为 `main` 分支，`pull_request` 正常运行。

## 当前事实同步

README 已由用户确认同步新增 CI 使用说明。本变更不修改产品、架构、技术选型、契约或业务代码。

## 延期项

延期项：无。

## 最终审查

无 P0/P1/P2 阻塞问题。保留 `pgvector/pgvector:pg16` 作为 CI 后端数据库镜像，与本地 compose 和现有技术基线一致。
