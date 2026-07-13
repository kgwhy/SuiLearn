# 设计

## 方案

Search limit 属于后端应用层职责，因为 OpenAPI 契约定义的参数边界独立于当前 retriever 实现。

实现将 `limit` 加入 `SearchController` 和 `SearchService`，并扩展 `Retriever.RetrievalRequest`，使每个 retriever 都能看到请求的 limit。`KeywordRetriever` 在收集 scoped、filtered 结果后应用 limit。

兼容门面 `SuiLearnV2Service` 和 `SuiLearnV2Workflow` 保留现有三参数 `search` 方法，供旧测试和调用方使用，默认值为 10；新增四参数重载用于传递显式 limit。

## 数据/API 影响

- HTTP 契约不变。
- 数据库 schema 不变。
- 不需要 Web 或 Android 变更。

## 风险

- 自定义 retriever 如果忽略 `RetrievalRequest.limit`，仍可能返回过多结果。内置 `KeywordRetriever` 已更新，service 测试保护当前默认行为。

## 替代方案

- 只在 `SearchController` 限制：拒绝，因为内部 service 调用方和未来消费者仍会绕过契约。
- 在 `SearchService` 中等 retriever 返回后再裁剪：可行，但对未来能提前停止的 retriever 不够友好。
