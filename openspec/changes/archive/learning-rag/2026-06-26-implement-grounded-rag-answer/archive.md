# 归档

## 变更名称

`implement-grounded-rag-answer`

## 最终状态

已实现并验证。RAG 问答从固定提示文案升级为由检索证据驱动、调用当前 AI Provider 生成的 grounded answer，保留 citations 与不确定性保护。

## 实现引用

- 代码已合并至 `main`，最新提交 `f2258c4`。
- base_ref：`3b8aababf1e49294a32a41eb8ed1780632364ad5`。
- 涉及 `AiProvider`、`OpenAiCompatibleAiProvider`、`RagService`、`SuiLearnV2Workflow` 及 `SuiLearnV2ServiceTest`。

## 验证摘要

- `mvn -f services/api/pom.xml test -q`：通过，退出码 0（2026-06-26），PostgreSQL 16.14。
- 完整后端套件 53 个测试，0 失败、0 错误。详见 `verification.md`。

## 已同步的当前事实文档

- 产品事实：不受影响（沿用既有 `/api/v2/rag/ask` 行为，仅 answer 内容由证据生成）。
- 架构事实：不受影响。
- 技术事实：不受影响。
- 契约：不受影响；未新增或修改 API 契约字段，`RagAnswer` 结构保持不变。

## 延期项

- 无。

## 最终审查摘要

- P0：无。
- P1：无。
- P2：无。
