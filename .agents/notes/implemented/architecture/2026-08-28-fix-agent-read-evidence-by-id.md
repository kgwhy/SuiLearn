# Agent Note: read_evidence 按证据 ID 直接读取而不是全文检索
Status: implemented

## Problem

Agent 的 `search_knowledge` 返回 material chunk 的 sourceRef/chunkId，但 `read_evidence` 把该 ID 当作普通查询文本去 `retrieveEvidence()`，导致搜索不到原始 chunk，Agent 一直 search/read 到 `BUDGET_EXHAUSTED`。

## Decision

让 `RetrievalEvidenceTools` 持有 `MaterialChunkStore`。`read()` 先按 `blockId`/`stableId`/`sourceRef` 调用 `findChunk()` 直接读取 material chunk 内容；查不到时再回退到原来的 `retrieveEvidence()` 搜索。由于 material chunk 的 `SourceRef.id` 就是 chunkId，这是最直接、最稳定的读证据方式。

## Alternatives considered

- **扩展 RetrievalPort 增加 readById**：设计更“端口化”，但当前只有 agent 内部使用该端口，且需要改接口与 adapter，改动面更大。
- **把 excerpt 当作证据内容**：不是完整证据，不能支持详细讲解，且可能截断关键内容。
- **保留原逻辑只在 CLI 显示更多错误**：没有解决后端无法读取证据的根本问题。

## Consequences

- `read_evidence` 对 material chunk 能读到完整内容，Agent 不再因为读不到证据而空转耗尽预算。
- 对非 chunk 引用（知识点、题目）仍走原 retrieval 回退，避免行为回退。
- `RetrievalEvidenceTools` 从 agent tool 层直接依赖 material infrastructure store；后续如果要严格分层，可再扩 `RetrievalPort`。
