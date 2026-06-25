# 提案：无 embedding RAG Pipeline 一步到位

## 做什么

将 SuiLearn 当前的资料问答从“按换行切块、Java 全量扫描、Top3 excerpt、LLM 总结”的 Document QA 原型，升级为不依赖 embedding 的完整 text-only RAG Pipeline。

目标链路：

```text
导入资料
  -> 结构化解析
  -> 语义 chunk + overlap
  -> text-only 索引
  -> 查询改写/归一化
  -> 候选召回
  -> BM25/FTS 排序
  -> 相邻 chunk 扩展
  -> context budget packing
  -> LLM grounded answer
  -> statement-level citation
  -> answer validation
```

## 为什么做

当前 RAG 的主要限制不是 embedding 缺失，而是 chunk、context、retrieval 和 grounding 四层基础能力不足：

- chunk 由换行驱动，语义经常被切碎。
- LLM 只收到 `SourceRef.excerpt`，默认最多 160 字符，检索命中后仍可能缺上下文。
- 检索通过 `findAll()` 后 Java 内存扫描，不适合长期知识库。
- citation 只是资料片段列表，不能证明每个结论由哪段证据支持。
- 没有引用编号校验、答案越界校验和不确定性保护。

如果 SuiLearn 要演进为学习 Agent，RAG 必须先能稳定回答资料内问题，并能说明“答案来自哪里”。本变更先不引入 embedding，避免把架构问题误判为模型问题。

## 范围

本变更覆盖后端 RAG 架构和契约扩展：

- 重写资料 chunk 策略。
- 新增 chunk 元数据：标题路径、token/字符范围、相邻关系、索引状态。
- 新增 text-only 检索能力，优先使用 PostgreSQL full-text search；保留 Java BM25 fallback 作为测试和降级路径。
- RAG evidence 从 `SourceRef` 摘要升级为完整证据对象。
- RAG 回答支持 statement-level citations。
- 增加回答引用校验和基础 grounding 校验。
- 更新后端测试和必要 API 契约。

## 非目标

- 不引入 embedding provider、向量数据库或向量索引。
- 不引入外部 rerank 模型。
- 不实现长期记忆、规划执行或多 Agent 工具调用。
- 不改动移动端和 Web 的复杂交互；如契约新增字段，前端适配另行按任务执行。
- 不自动把 RAG 答案保存为笔记或题目。

## 成功标准

- 对同一份资料提问时，LLM 能收到完整 Top evidence chunk，而不是 160 字符 excerpt。
- 单个命中 chunk 能自动携带前后文，回答不再依赖孤立句子。
- 检索候选由索引或 BM25 驱动，不再以全量 Java 扫描作为主路径。
- 回答中每个核心陈述都能绑定至少一个证据编号。
- 返回前会校验引用编号存在；引用缺失或证据不足时返回 `uncertain=true`。
- 后端测试覆盖 chunk、检索、context packing、citation validation 和 no-evidence 场景。

## 相关变更

- `enable-text-only-rag`：已让无 embedding 配置可导入并可检索，本变更在其基础上升级质量和架构。
- `implement-grounded-rag-answer`：已引入 AI provider 问答能力，本变更扩展 evidence payload、引用粒度和校验。
- `improve-rag-retrieval`：已改善当前 `KeywordRetriever` 排序，本变更替换为正式 text-only 检索管线。
