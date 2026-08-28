# Proposal: 修复 Agent read_evidence 找不到可读证据

## 背景

Agent 使用 `search_knowledge` 能检索到证据指针，但 `read_evidence` 返回“No readable evidence found”，导致 `study_agent` 反复检索直到 `BUDGET_EXHAUSTED`。

根因：`RetrievalEvidenceTools.read()` 把 `sourceRef`/`stableId` 当作检索 query 调用 `retrieveEvidence()`。对 material chunk，sourceRef 是 chunkId，拿 chunkId 去语义/关键词检索全文不会命中原始 chunk，因此读不到内容。

## 变更内容

- `RetrievalEvidenceTools` 增加 `MaterialChunkStore`，`read()` 优先按 `blockId`/`stableId`/`sourceRef` 直接 `findChunk`，读取当前 material chunk 的完整内容。
- 如果按 ID 找不到（例如知识库知识点/题目引用），保留原有 retrieval 搜索回退逻辑。
- `AgentInfrastructureConfiguration` 注入 `MaterialChunkStore`。
- 新增 `RetrievalEvidenceToolsTest` 覆盖“按 chunkId 直接读取证据”。

## 非目标

- 不修改搜索排序、RAG pipeline、web/Android、OpenAPI。
- 不改变 `read_evidence` 的对外工具名和参数。

## 验收标准

- `read_evidence` 能读取 `search_knowledge` 返回的 material chunk 指针内容。
- 找不到时可回退到原 retrieval 行为，不抛出未处理异常。
