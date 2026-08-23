# 归档记录

Status: archived

Owner: Leader

## 归档内容

- 当前事实同步：
  - `docs/product-requirements.md`：新增需求 7 Agent 学习助手与 `SPEC-AGENT-NATIVE-RUNTIME`。
  - `docs/architecture.md`：新增 Agent-Native 包结构、运行时、持久化、数据流、安全边界与 RAG 引擎化边界；修正 Spring AI 描述。
  - `docs/tech-selection.md`：新增 WebSocket/LLM SSE/Agent 配置与约束；修正 Spring AI/Redis 描述。
  - `contracts/**`：本 change 未修改，6b 已完成 OpenAPI 信封扩展与 WS companion schema。
- 验证摘要：workflow skill/check、workflow 单测、agent notes、diff check 全部通过。

Deferred items:
- Android 新 Agent 协议客户端 -> 用户明确延后。
- `rag_qa`、`question_generation` 独立循环策略 -> 新 change。
- L1 trace/snapshot 在线生产者挂接与 L2/L3 在线召回 -> 具名 follow-up `agent-memory-online-wiring`。
- RAG engine 组件切换生产检索主路径 -> 具名 follow-up `rag-engine-production-switch`。
- 真实模型 / 真实 PostgreSQL / WS 运行态联调 -> 既有具名 follow-up。

## 审查摘要

- review_mode: single-agent
- 对照 10 个归档 change、当前源码与契约逐条核对；未把未接线组件写成全量接通。
- P0/P1: 0；P2: 0 未关闭（上述 follow-up 均具名）。
