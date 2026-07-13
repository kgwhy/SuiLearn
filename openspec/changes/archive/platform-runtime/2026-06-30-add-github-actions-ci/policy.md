# Policy：添加 GitHub Actions CI

## 变更等级

Tiny。

理由：单次工程流程配置变更，不改变产品、架构、契约、存储或业务行为。修改文件限定为 CI workflow 和本 Fast Track 任务记录。

## base_ref

`2379f957b6e8c047aed8f09d4993a0627c70dcde`

## 角色归属

- Leader Agent：协调 Fast Track 任务、记录范围和门禁。
- Test Agent：CI 自动化验证命令归属。

## 文件锁

模式：serial。

锁定路径：

- `.github/workflows/ci.yml`
- `openspec/changes/2026-06-30-add-github-actions-ci/tasks.md`
- `openspec/changes/2026-06-30-add-github-actions-ci/policy.md`

当前检查：未发现 `.agents/locks` 中的 active lock 冲突。

## 允许路径

- `.github/workflows/ci.yml`
- `openspec/changes/2026-06-30-add-github-actions-ci/tasks.md`
- `openspec/changes/2026-06-30-add-github-actions-ci/policy.md`

## 禁止路径

- `apps/android/**`
- `services/api/**`
- `apps/web/**`
- `contracts/**`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/tech-selection.md`
- `docs/proposals/**`
- `docs/superpowers/specs/**`
- `docs/superpowers/plans/**`

## 基线状态

业务代码基线测试：skipped。

原因：本任务不修改业务代码、契约或事实文档；最终验证将运行 CI 中声明的本地等价命令。

## 验收标准

- GitHub Actions 在 `main` 分支 `push` 和所有 `pull_request` 时运行。
- 工作流检查、Android 单元测试/构建、后端测试、Web 测试和 Web 构建均在 CI 中有明确 job。
- CI 使用最小仓库权限：`contents: read`。
- 本地验证结果在最终报告中记录。
