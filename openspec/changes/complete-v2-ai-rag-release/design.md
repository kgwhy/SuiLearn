# 设计

## 当前 Reviewer 发现

- P0：`OpenAiCompatibleAiProvider` 未执行真实生成；OpenAI-compatible 状态仍不可用。
- P1：Android 可以远程确认或丢弃生成内容，但已保存 AI 题目没有进入本地练习/错题/收藏/统计闭环。
- P1：后端统计宣称具备答题/错题记录语义，但缺少服务端学习记录模型或端点。
- P1：搜索需要语义行为，同时不能过度承诺某个具体向量数据库实现。
- P2：Web 缺少第二版工作台流程的源码级测试。
- P3：部分当前事实文档仍提到退役的 `docs/proposals/**` 流程。

## 架构决策

- V2 保留 Android 本地练习闭环，同时通过 V2 API 增加服务端答题记录，用于知识库统计。
- Android 将已保存 AI 题目导入 Room，使现有本地练习、错题、收藏、搜索和统计流程复用这些题目。
- 语义检索在服务层使用已存储 embeddings 和余弦相似度；fake/local 模式保留关键词回退，避免产生虚假的 RAG 证据。
- Provider 集成测试使用本地 HTTP server，使真实请求/响应代码在无外网依赖下被覆盖。

## 构建策略

本变更跨角色且涉及契约，使用 L3：

```text
Architect/Contracts -> Backend -> Web -> Android -> Test -> Spec Review -> Code Review
```

契约变更必须串行完成，之后再进行消费端适配。
