# 支持 Search Limit

## 背景

V2 OpenAPI 契约暴露了 `/api/v2/search?limit=...`，Web 工作台也已经发送 `limit=10`。后端此前忽略该参数，导致搜索响应可能超过契约限制，也让工作台结果更难扫描。

## 范围

- 在后端 Controller 解析可选 search `limit` 请求参数。
- 应用与 OpenAPI 契约一致的默认值、最小值和最大值语义：默认 `10`，最小 `1`，最大 `50`。
- 确保模块搜索和兼容搜索路径使用相同行为。
- 增加后端回归测试。

## 非目标

- 不修改 OpenAPI 契约。
- 不引入真实向量存储、新排序模型或全局搜索。
- 不改变 Web 或 Android 行为；客户端已经能兼容修复后的响应形态。

## 验收标准

- `/api/v2/search` 在提供 `limit` 时遵守该值。
- 缺省 `limit` 时默认使用 10。
- 小于 1 或大于 50 的值在检索执行前以 `IllegalArgumentException` 拒绝。
- 现有 scoped-search 保护保持不变。
