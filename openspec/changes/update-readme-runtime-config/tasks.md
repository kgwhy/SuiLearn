# 任务

## 1. 更新 README 运行配置说明

- 状态：done
- Owner：Leader Agent
- 允许文件：
  - `README.md`
  - `openspec/changes/update-readme-runtime-config/**`
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
  - `rg -n "H2|Fake|fake|默认 H2|FakeAiProvider" README.md`
  - `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 3b8aababf1e49294a32a41eb8ed1780632364ad5 -ClosingChange update-readme-runtime-config`
- 审查重点：
  - README 不再宣称后端默认 H2 或 Fake AI Provider。
  - README 明确 PostgreSQL、OpenAI-compatible Provider、`config/local.properties` 和环境变量配置方式。
