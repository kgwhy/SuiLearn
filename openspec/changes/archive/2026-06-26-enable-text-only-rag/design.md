# 设计

## Embedding 能力探测

在 `EmbeddingProvider` 增加 `supportsEmbeddings()` 默认方法。默认返回 `true`，OpenAI-compatible embedding 实现根据 `SuiLearnAiProperties.hasOpenAiCompatibleEmbeddingConfiguration()` 返回真实状态。

调用方以该能力判断是否执行 embedding：

- `true`：导入时生成向量，chunk 标记为 `READY`。
- `false`：导入时保存文本 chunk，chunk 标记为 `TEXT_ONLY`。

## Chunk 状态

`EmbeddingStatus` 新增 `TEXT_ONLY`，表示该 chunk 没有向量，但可作为文本检索和 RAG 证据使用。

可检索状态为：

- `READY`
- `TEXT_ONLY`

删除资料时仍将 chunk 标记为 `INVALIDATED`。

## 检索策略

`KeywordRetriever` 调整为：

- 无 embedding：不调用 `embeddingProvider.embed(request.query())`。
- 有 embedding：保留现有关键词 + 语义加权逻辑。
- chunk 过滤从只接受 `READY` 改为接受 `READY` 和 `TEXT_ONLY`。
- 语义分数在无 query embedding 或 chunk embedding 时为 `0`。

## Provider 状态

`SuiLearnAiProperties.hasOpenAiCompatibleConfiguration()` 改为只要求聊天配置完整。状态消息说明：

- 聊天配置可用。
- embedding 已配置时启用向量检索。
- embedding 未配置时使用文本检索。

## 风险

- 无 embedding 模式无法召回纯语义同义查询，必须依赖关键词命中。
- `TEXT_ONLY` 状态会暴露到已有 API JSON，但不改变字段结构。
