# 归档记录

## 变更名称

`upgrade-text-only-rag-pipeline`（等级 Major）

## 状态

已实现并验证。无 embedding 的 text-only RAG Pipeline 已落地：结构化 chunk、text-only 检索、上下文扩展、证据打包、带引用回答与回答校验。

## 最终实现引用

- 代码已合并至 `main`，最新提交 `f2258c4`（含 `05128f9` 文本分块与关键词检索优化、`54d6aa7`/`3b8aaba` AI 知识点提取与检索优化）。
- base_ref：`3b8aababf1e49294a32a41eb8ed1780632364ad5`。
- 涉及 `material/**`、`retrieval/**`、`rag/**`、`ai/**`、`persistence/**` 及对应测试。

## 验证摘要

- `mvn -f services/api/pom.xml test -q`：通过，退出码 0（2026-06-26），PostgreSQL 16.14。
- 完整后端套件 53 个测试，0 失败、0 错误，含 `DefaultMaterialChunkerTest`、`CitationValidatorTest`、`KeywordRetrieverTest`、`SuiLearnV2ServiceTest` 等。详见 `verification.md`。
- 工作流检查器：`SuiLearn Workflow policy check passed.`

## 同步到当前事实文档

- 产品事实：不受影响（RAG 问答能力增强，未改变产品范围结论）。
- 架构事实：不受影响（沿用既有 `content`、`ordinal`、PostgreSQL FTS 与 Java BM25 fallback，未强制迁移旧数据）。
- 技术事实：不受影响。
- 契约：后端已兼容返回 `statements` 字段；客户端展示增强为非阻塞项。

## 延期项

- 客户端 statement-level citation 展示：非阻塞增强，后端已兼容返回 `statements` 字段，Web/Android 展示增强可另行处理。
- 持久化 `search_text`、`heading_path_json`、`tsvector` 与 reindex 任务：非阻塞增强；本批使用现有 `content`、`ordinal`、PostgreSQL FTS 查询和 Java BM25 fallback，未强制迁移旧数据。
- Owner：Server Backend Agent / Web Frontend Agent，按需在后续具名 change 中处理。

## 最终审查摘要

- P0：无。
- P1：无。
- P2：无阻塞归档项；上述两条记录为非阻塞增强延期。
