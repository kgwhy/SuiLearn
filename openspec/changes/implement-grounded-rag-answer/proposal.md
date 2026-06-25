# 提案

## 做什么

将 RAG 问答从“检索证据后返回固定提示文案”升级为“检索证据后调用当前 AI Provider 生成基于证据的回答”。

## 为什么

当前 `/api/v2/rag/ask` 和后端工作流 `ask` 已能检索资料片段并返回引用，但 `answer` 字段仍是固定文案，用户无法得到真正的资料问答结果。该变更让回答内容由证据片段驱动生成，同时保留引用和不确定性保护。

## 范围

- 新增 AI Provider 的资料问答能力。
- RAG 服务和 V2 工作流在检索到证据后调用该能力生成回答。
- 保持现有 `RagAnswer` 返回结构、API 路径和存储结构不变。
- 补充后端回归测试，证明回答来自 AI Provider 且仍保留 citations/evidence。

## 非目标

- 不新增或修改 API 契约字段。
- 不引入新的向量数据库、BM25、reranker 或 agentic RAG。
- 不修改 Android/Web 客户端。
- 不自动保存 RAG 回答为 AI 笔记。

## 验收标准

- 当检索不到证据时，仍返回 `uncertain=true` 和空证据。
- 当检索到证据时，RAG 调用 AI Provider 生成 answer，并返回 `uncertain=false`、citations 和 evidenceChunks。
- AI Provider 的 OpenAI-compatible 实现要求模型只依据 sourceRefs/excerpts 回答；证据不足时返回不确定回答。
- 后端编译通过；完整测试若依赖 PostgreSQL 不可用，必须记录原始失败原因。
