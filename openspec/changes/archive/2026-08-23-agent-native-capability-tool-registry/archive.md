# 归档记录

Status: archived

Owner: Leader

## 归档内容

- 实现引用：本 change 的 proposal、design、specs、tasks、policy、verification。
- 验证摘要：change-1 27 个回归 + change-2 11 个新增测试在干净 shell 全绿；完整后端 401 run / 35 个既有 PostgreSQL 环境 errors / 5 skipped；工作流检查、change-scope、git diff check 通过。
- 当前事实同步：
  - `docs/product-requirements.md`：not affected。
  - `docs/architecture.md`：not affected（新注册表未替换旧路径）。
  - `docs/tech-selection.md`：not affected。
  - `contracts/openapi/suilearn-v2.yaml`：新增 additive capabilities 契约。
- 决策记录：已迁移为 `.agents/notes/implemented/architecture/2026-08-23-agent-native-capability-tool-registry.md`。

Deferred items:
- `AgentLoop`、`LlmClient`、`ToolDispatcher` 与真实 LLM 循环 -> change-3。
- `generate_questions` durable task 工具与 SourceSelection 校验 -> change-3 或 generation change。
- 旧 `AgentToolCatalog`/ReactAgent 删除 -> change-3。
- 真实 PostgreSQL 运行态与客户端消费 -> 具名 follow-up。

## 审查摘要

- review_mode: single-agent
- Spec Review 先行：proposal/design/specs/tasks/policy/verification 已对照改造计划 change-2 与工作流政策复核。
- Code Review：能力默认路由、工具权限交集、OpenAI schema、scope/删除校验、临时练习与 memory 降级、unavailable 终态、OpenAPI additive 均已检查；修复了 ToolResult null metadata、MemoryManager 测试替身等 P2 问题。
- 最终 P0/P1: 0；P2: 0 未关闭（真实运行态与客户端消费为具名 follow-up）。
