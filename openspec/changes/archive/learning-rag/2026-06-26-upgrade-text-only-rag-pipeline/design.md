# 设计：Text-only RAG Pipeline

## 总体决策

本变更选择“结构化 chunk + PostgreSQL FTS/BM25 主召回 + 邻接扩展 + context packing + statement citation”的 text-only RAG 方案。embedding 作为未来增强能力，不参与本次主路径。

## 目标架构

```text
MaterialImportService
  -> MaterialParser
  -> SemanticMaterialChunker
  -> MaterialChunkStore
  -> TextSearchIndexer

RagService
  -> QueryNormalizer
  -> TextOnlyRetriever
  -> EvidenceExpander
  -> ContextPacker
  -> AiProvider.answerQuestion
  -> CitationValidator
  -> GroundingPolicy
```

## Chunk 设计

### 规则

- 默认目标大小：`400-600 tokens`。
- 默认 overlap：`80 tokens`。
- 最小 chunk：`120 tokens`，短标题段落可合并到相邻正文。
- 最大 chunk：`800 tokens`，超过后按段落或句子边界拆分。
- Markdown 标题进入 `headingPath`，不单独作为孤立 chunk。
- 列表、表格和代码块保持块级完整；超长代码块按行窗口拆分。
- 每个 chunk 记录 `ordinal`，并能定位前后 chunk。

### 元数据

建议扩展 `MaterialChunk`：

```text
id
knowledgeBaseId
materialId
content
ordinal
sourceRef
embedding
embeddingStatus
embeddingModel
embeddingDimensions
headingPath
startOffset
endOffset
tokenCount
previousChunkId
nextChunkId
retrievalStatus
```

`embedding*` 字段保留兼容，但 text-only 主路径不依赖它们。

## 检索设计

### 查询归一化

`QueryNormalizer` 负责：

- 统一大小写、全角半角、空白和标点。
- 提取中英文 token。
- 保留原始 query 用于 LLM。
- 生成 `plainQuery`、`terms`、`requiredTerms` 和 `optionalTerms`。

### 主召回

优先使用 PostgreSQL full-text search：

- 英文使用 PostgreSQL 内置 text search。
- 中文先采用应用层 n-gram/tokenization 生成 `searchText`，后续可替换为专用中文分词。
- 按 `knowledgeBaseId`、`materialId`、`MaterialStatus` 和 `retrievalStatus` 过滤。
- 候选召回数量默认 `50`。

### fallback

测试环境或数据库能力不可用时，使用 Java BM25 fallback：

- 对限定 scope 内 chunk 构建临时 BM25 分数。
- 仅作为降级和单元测试路径，不作为大规模生产主路径。

### 排序信号

第一版不使用 embedding，综合分数为：

```text
score =
  BM25/FTS rank * 0.60
  + phrase match * 0.15
  + title/heading match * 0.10
  + coverage * 0.10
  + recency/status boost * 0.05
```

排序后保留 Top20 进入 evidence 阶段。

## Evidence 扩展

`EvidenceExpander` 对 Top20 候选执行：

- 每个高分 chunk 自动拉取 `previousChunkId` 和 `nextChunkId`。
- 同一资料内相邻 chunk 合并为 evidence group。
- 严格遵守 `materialId` scope；跨资料只在 knowledge base scope 下允许。
- 合并后去重，保留原始 chunk 顺序。

默认策略：

```text
候选召回：50
排序候选：20
邻接扩展：±1
最终 evidence：5 groups 或 context budget 上限
```

## Context Packing

`ContextPacker` 根据模型窗口和配置预算组装上下文：

- 默认上下文预算：`6000 tokens`。
- 单个 evidence group 最大：`1600 tokens`。
- 超预算时优先保留高分 group 的核心 chunk，再裁剪邻接 chunk。
- 每个 evidence 包含完整 `content`，并保留 `SourceRef` 作为引用元信息。

Evidence payload 示例：

```json
{
  "citationNumber": 1,
  "materialId": "mat_1",
  "chunkIds": ["chunk_10", "chunk_11"],
  "title": "TCP 协议",
  "headingPath": ["网络基础", "传输层"],
  "content": "完整证据文本...",
  "score": 0.87
}
```

## 回答与 Citation

AI Provider 输入改为 `evidence`，不再只传 `sourceRefs`。

输出建议扩展为：

```json
{
  "answer": "TCP 是一种面向连接的传输层协议。[1]",
  "uncertain": false,
  "statements": [
    {
      "text": "TCP 是一种面向连接的传输层协议。",
      "citations": [1]
    }
  ]
}
```

兼容策略：

- `RagAnswer.answer`、`uncertain`、`citations`、`evidenceChunks` 保持兼容。
- 新增字段可放入 `metadata` 或扩展契约后加入 `statements`。
- 客户端未适配前仍显示 `answer` 和原引用列表。

## Validation

`CitationValidator` 执行硬校验：

- 引用编号必须存在。
- 非 `uncertain` 回答至少包含一个 citation。
- 每个 statement 至少有一个 citation。
- 如果模型输出 JSON 不合法，返回保守不确定回答。

`GroundingPolicy` 执行轻量校验：

- statement 中的关键名词或数字应在 cited evidence 中出现。
- 明显超出资料的回答改为 `uncertain=true`。
- 校验失败时不让模型自由补写，只返回“资料中证据不足”。

## API 与契约影响

建议更新 `RagAnswer`：

```text
answer: string
uncertain: boolean
citations: SourceRef[]
evidenceChunks: MaterialChunk[]
metadata: object | null
statements: RagStatement[] 可选
```

如果保持当前字段不变，则先将 `statements` 放入 `metadata`，后续再升级正式契约。

建议新增内部模型：

- `RetrievedEvidence`
- `EvidenceGroup`
- `PackedEvidence`
- `RagStatement`
- `CitationValidationResult`

## 数据库影响

建议新增或扩展 chunk 表字段：

- `search_text`
- `heading_path_json`
- `start_offset`
- `end_offset`
- `token_count`
- `previous_chunk_id`
- `next_chunk_id`
- `retrieval_status`
- `tsvector` 或等价索引字段

迁移要求：

- 旧 chunk 需在资料重新导入或后台 reindex 后获得新字段。
- 迁移期间旧 chunk 可走 Java BM25 fallback。
- 新导入资料必须使用新 chunker 和 text-only index。

## 替代方案

### 继续优化 KeywordRetriever

优点：改动小。缺点：仍然受制于换行 chunk、全量扫描和 excerpt context，不能解决架构问题。

### 直接上 embedding

优点：语义召回能力更强。缺点：chunk 和 context 没修好时收益有限，还会引入 API 成本、模型兼容和向量索引复杂度。

### 引入外部搜索服务

优点：能力强。缺点：部署复杂度高，不适合当前阶段。PostgreSQL FTS 足以支撑第一阶段 text-only RAG。

## 风险

- 中文检索质量受 tokenizer 影响。第一版用 n-gram 保底，后续可引入中文分词。
- context 增大会增加 LLM 成本。通过 budget packing 控制。
- API 扩展会影响客户端。先保持兼容字段，新增字段可选。
- 数据迁移需要处理旧 chunk。用 fallback 和 reindex 任务降低风险。
