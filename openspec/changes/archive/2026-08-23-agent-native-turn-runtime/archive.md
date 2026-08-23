# 归档记录

Status: archived

Owner: Leader

## 归档内容

- 实现引用：本 change 的 proposal、design、specs、tasks、policy、verification。
- 验证摘要：27 个新增定向测试全绿；完整后端 390 run / 35 个既有 PostgreSQL 环境 errors / 5 skipped；工作流检查、change-scope、git diff check 全部通过。详细命令与原始输出位置见 verification.md。
- 当前事实同步：
  - `docs/product-requirements.md`：not affected。
  - `docs/architecture.md`：not affected（本 change 是新 runtime 骨架，未替换旧路径）。
  - `docs/tech-selection.md`：not affected。
  - `contracts/openapi/suilearn-v2.yaml`：已新增 additive 回合契约。
  - `contracts/schemas/suilearn-ws.yaml`：新增 WS companion schema。
- 决策记录：已迁移为 `.agents/notes/implemented/architecture/2026-08-23-agent-native-turn-runtime.md`。

Deferred items:

- `study_agent`、`rag_qa`、`question_generation` 能力注册与工具化 -> change-2。
- `LlmClient`、`AgentLoop`、删除旧 ReactAgent -> change-3。
- 上下文压缩、三层记忆、RAG 引擎化、UsageTracker、客户端切换 -> change-4/5/6/7 按计划继续。
- 真实 PostgreSQL 运行态、真实 WS 联调、跨实例 replay 和 Web/Android 客户端证据 -> 具名 follow-up：`agent-turn-runtime-live-verification`。

## 关闭条件

- tasks 无 open 任务。
- 定向测试与契约测试证据完整。
- P0/P1 全部修复并复审；P2 修复、延期或由用户接受。
- Sync Gate 的 not affected 记录完成。

## 审查摘要

- review_mode: single-agent
- Spec Review 先行：proposal/design/specs/tasks/policy/verification 已对照 `docs/plans/suilearn-refactor-plan.md` change-1 与工作流政策复核。
- Code Review：文件范围、事务/seq/终态/慢消费者/孤儿恢复、配置默认值、契约 additive 和 sanitized 错误均已检查；发现并修复了 WS 订阅按连接隔离、终端 turn replay 不重复推送、event type wire 大小写、events lastSeq 等 P2 问题。
- 最终 P0/P1: 0；P2: 0 未关闭（真实 PostgreSQL/WS 运行态联调为具名 follow-up）。
