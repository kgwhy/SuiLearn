# RAG Pipeline 与索引版本（change-5a）

## Why

现有检索只有单一 KeywordRetriever，没有 pipeline 工厂、embedding 签名或索引版本；换 embedding 模型无法提示 needs_reindex。5b 再实现 SmartRetriever 多查询合成。

## What Changes

- 新增 `RagPipeline` 接口与 `PgvectorHybridRagPipeline`（包装 KeywordRetriever），`PipelineFactory` 默认 pgvector-hybrid。
- 新增 `EmbeddingSignature`（binding/model/dim/baseUrl/apiVersion 哈希）与 `index_versions` 表/manager。
- 新增 `ParseEngine` 接口、`ParsedDocument` IR、text-only 引擎与注册表。
- 不修改现有检索行为；仅新增可替换边界。

## Acceptance Criteria

- 两种 embedding 签名生成不同 hash；换签名返回 `needs_reindex`。
- PipelineFactory 返回默认 pgvector-hybrid。
- ParseEngineRegistry 按媒体类型路由，text 引擎可用。
- 既有 64 个定向测试通过。
