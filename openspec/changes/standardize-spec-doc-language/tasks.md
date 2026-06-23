# 任务

## 1. 统一 Spec 文档语言

- 状态：done
- Owner：Leader Agent
- 允许文件：
  - `AGENTS.md`
  - `docs/development-workflow.md`
  - `openspec/changes/standardize-spec-doc-language/**`
- 禁止文件：
  - `apps/**`
  - `services/**`
  - `contracts/**`
  - `docs/product-requirements.md`
  - `docs/architecture.md`
  - `docs/tech-selection.md`
  - `docs/proposals/**`
  - `docs/superpowers/**`
- 验证：
  - `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef cc8b0c1c5172088229e37948fa2989f868f5a831 -ClosingChange standardize-spec-doc-language`
- 审查重点：
  - 后续 `openspec/changes/**` 产物必须默认使用中文。
  - 不影响业务代码、契约或产品范围。
