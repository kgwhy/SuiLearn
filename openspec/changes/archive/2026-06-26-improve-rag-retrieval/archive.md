# 归档

## 变更名称

`improve-rag-retrieval`

## 最终状态

已实现并验证。等级 Tiny。在不改动 API、数据模型和存储结构的前提下，将检索升级为更稳定的混合检索（关键词召回、词项覆盖、短片段轻量加权、同资料去重）。

## 实现引用

- 代码已合并至 `main`，最新提交 `f2258c4`。
- base_ref：`cc8b0c1c5172088229e37948fa2989f868f5a831`。
- 涉及 `KeywordRetriever.java` 及 `SuiLearnV2ServiceTest`。

## 验证摘要

- `mvn -f services/api/pom.xml test -q`：通过，退出码 0（2026-06-26），PostgreSQL 16.14。
- 完整后端套件 53 个测试，0 失败、0 错误。详见 `verification.md`。

## 已同步的当前事实文档

- 产品事实：不受影响。
- 架构事实：不受影响。
- 技术事实：不受影响。
- 契约：不受影响；保持现有 API、数据模型与存储结构。

## 延期项

- 无。
- 说明：本变更的 Tiny 级排序优化后续被 `upgrade-text-only-rag-pipeline` 吸收为正式 Major 级 RAG 架构方案。

## 最终审查摘要

- P0：无。
- P1：无。
- P2：无。
