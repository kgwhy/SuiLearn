# Archive

Status: passed

## Change

`update-readme-runtime-config`

## Implementation Reference

Working tree changes from base ref `3b8aababf1e49294a32a41eb8ed1780632364ad5`.

## Summary

- 更新 README 中后端运行配置说明。
- 去除 README 对默认 H2 和 Fake AI Provider 的旧描述。
- 补充 PostgreSQL、OpenAI-compatible Provider、本地配置文件和环境变量说明。

## Verification

`rg -n "H2|Fake|fake|默认 H2|FakeAiProvider" README.md`：未返回匹配项。

`powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 3b8aababf1e49294a32a41eb8ed1780632364ad5 -ClosingChange update-readme-runtime-config`：通过。

## Deferred Items

Deferred items: none

## Review

无 P0/P1/P2 发现。
