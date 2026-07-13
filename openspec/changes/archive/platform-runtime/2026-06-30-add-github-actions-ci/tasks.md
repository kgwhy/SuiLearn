# 任务：添加 GitHub Actions CI

## 状态

- [x] 创建 GitHub Actions workflow，覆盖工作流检查、Android 单元测试/构建、后端测试和 Web 测试/构建；`push` 仅在 `main` 分支运行，PR 正常运行。
- [x] 使用仓库现有验证命令，不修改业务代码、契约或事实文档。
- [x] 本地运行 CI 对应的可用验证命令，并记录不可用项。

## Owner

- Leader Agent 协调。
- Test Agent 范围：自动化测试与 CI 验证命令。

## 允许修改文件

- `.github/workflows/ci.yml`
- `openspec/changes/2026-06-30-add-github-actions-ci/tasks.md`
- `openspec/changes/2026-06-30-add-github-actions-ci/policy.md`

## 禁止修改文件

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

## 验证命令

- `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 2379f957b6e8c047aed8f09d4993a0627c70dcde`
- `.\gradlew.bat :app:testDebugUnitTest --no-daemon`
- `.\gradlew.bat :app:assembleDebug --no-daemon`
- `mvn -f services/api/pom.xml test -q`（需要可用 PostgreSQL；CI 使用 `pgvector/pgvector:pg16` service）
- `npm --prefix apps/web test`
- `npm --prefix apps/web run build`

## 审查重点

- CI 只使用现有构建和测试命令。
- Android、Backend、Web 使用彼此独立的 job 和运行时版本。
- Workflow 权限保持最小化，默认只读仓库内容。
- `push` 触发限制在 `main`，避免 PR 分支 push 与 `pull_request` 重复全量运行。
