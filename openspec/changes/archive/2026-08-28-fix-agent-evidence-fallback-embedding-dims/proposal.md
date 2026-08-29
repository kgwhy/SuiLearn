# Proposal: read_evidence 容错增强与 embedding dimensions 回填

## 背景

用户使用本地模型后：
- `read_evidence` 从 “No readable evidence found” 变成 “Evidence reading failed”，说明按 ID 读取路径抛出了运行时异常。
- 前端展示 `Embedding Dimensions` 一直为 0，因为 `OpenAiCompatibleEmbeddingProvider.dimensions()` 硬编码返回 0。

## 变更内容

- `RetrievalEvidenceTools.read()`
  - 按 ID 读取和检索回退都捕获运行时异常，避免把内部异常直接变成 tool failure。
  - chunk content 为空时回退到 `source.excerpt()` / `pointer.excerpt()`，保证有可读片段。
- `OpenAiCompatibleEmbeddingProvider`
  - 第一次成功调用 embedding 后缓存实际向量维度，`dimensions()` 返回真实值，前端不再一直显示 0。
- 新增/更新测试覆盖 content 为空时回退 excerpt 的场景。

## 验收标准

- `read_evidence` 对 material chunk 正常返回内容；content 缺失时至少返回 excerpt，不再 `Evidence reading failed`。
- 首次 embedding 成功后，`GET /api/v2/ai/provider-status` 返回真实 dimensions。
