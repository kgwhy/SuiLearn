# Agent Note: read_evidence 容错与 embedding dimensions 回填
Status: implemented

## Problem

本地模型环境下 `read_evidence` 从检索不到变成了内部异常 `Evidence reading failed`。同时前端 embedding dimensions 一直为 0，无法确认 embedding 是否真的可用。

## Decision

- `RetrievalEvidenceTools.read()` 不再把内部异常直接抛给 `ReadEvidenceTool`；捕获后走空结果，避免工具失败干扰 Agent。
- 如果 chunk.content 为空，优先用 `source.excerpt`，再回退 `pointer.excerpt`，保证模型至少拿到可读片段。
- `OpenAiCompatibleEmbeddingProvider` 在首次成功 embedding 后缓存实际向量长度，`dimensions()` 返回真实维度。

## Alternatives considered

- **在前端硬编码常见维度**：不同 embedding 模型维度不同，不可靠。
- **每次 status 都调用 embedding 服务取维度**：会让状态页触发外部请求，增加延迟和失败面。
- **完全不捕获异常**：会把底层异常暴露成 `Evidence reading failed`，不利于 Agent 继续回答。

## Consequences

- `read_evidence` 更稳健，content 缺失时仍可基于 excerpt 继续。
- 真实 dimensions 在第一次 embedding 成功后出现在 status 中，后续无需重启。
