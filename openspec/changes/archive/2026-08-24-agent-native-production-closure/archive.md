# 归档记录

Status: archived

Owner: Leader

## 归档内容

- 实现引用：本 change 的 proposal、design、specs、tasks、policy、verification。
- 验证摘要：88 个定向测试全绿；workflow/notes/diff 检查通过。
- 当前事实同步：
  - `docs/product-requirements.md`：三能力全部可执行，记忆在线与 RAG 生产路径更新。
  - `docs/architecture.md`：AgentLoop 分流、MemoryTurnRecorder、RagService/SearchService pipeline、index_versions 生产记录更新。
  - `docs/tech-selection.md`：MemoryTurnRecorder / EmbeddingIndexVersionRecorder / RagPipeline 生产路径更新。
  - `contracts/**`：not affected。
- 决策记录：已迁移为 `.agents/notes/implemented/architecture/2026-08-24-agent-native-production-closure.md`。

Deferred items:
- Android 新 Agent 协议客户端 -> 用户明确延后。
- 真实模型 / 真实 PostgreSQL / 真实 WS 运行态联调 -> 具名 follow-up `agent-native-live-verification`。
- `SmartRetriever` 多查询改写保持可选，不默认替换 `pgvector-hybrid`。
- ParseEngineRegistry 已提供 Spring bean，但 material revision/block 主路径继续使用 `DocumentParser`。

## 审查摘要

- review_mode: single-agent
- Spec Review 先行：proposal/design/specs/tasks/policy 与归档依据对照通过。
- Code Review：同环分流、终态后记忆失败隔离、pipeline 构造器兼容、embedding 成功后写 ready 版本；未修改 apps/contracts/pom。
- P0/P1: 0；P2: 0 未关闭（真实运行态与 Android 客户端为具名 follow-up）。
