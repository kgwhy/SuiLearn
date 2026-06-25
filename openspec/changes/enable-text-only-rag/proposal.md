# 启用无 embedding 版 RAG

## 背景

当前资料导入、搜索和 RAG 问答都硬依赖 `EmbeddingProvider`。当用户只配置 DeepSeek 这类聊天模型、没有可用 embedding 端点时，资料导入会在 embedding 阶段失败，RAG 无法进入“文本召回 + LLM 基于证据回答”的工作流。

用户明确要求先做无 embedding 版本，因此需要把 embedding 从硬依赖改为可选增强。

## 目标

- 没有 embedding 配置时，资料导入仍能完成并保存可检索 chunk。
- 搜索和 RAG 问答在无 embedding 模式下走文本关键词召回，不调用 `embed`。
- 有 embedding 配置时保留现有语义召回能力。
- Provider 状态以聊天配置为基本可用条件，embedding 状态作为能力补充。

## 非目标

- 不引入新的外部检索服务。
- 不新增数据库全文索引或 pgvector 迁移。
- 不实现 LLM rerank。
- 不改变前端和 OpenAPI 契约。

## 验收标准

- 只配置聊天模型时，后端 Provider 状态为可用。
- 只配置聊天模型时，导入资料不会因为 embedding 缺失失败。
- 无 embedding 模式下搜索和 RAG 问答能从 chunk 文本命中证据。
- 有 embedding 配置时，原有语义召回测试仍能通过。
