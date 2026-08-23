# 归档记录

Status: archived

Owner: Leader

## 归档内容

- 实现引用：本 change 的 proposal、design、specs、tasks、policy、verification。
- 验证摘要：54 个定向测试全绿（38 回归 + 16 新增）；完整后端 417 run / 35 个既有 PostgreSQL 环境 errors / 5 skipped；工作流检查、change-scope、git diff check 通过。
- 当前事实同步：
  - `docs/**`：not affected。
  - `contracts/**`：not affected。
- 决策记录：已迁移为 `.agents/notes/implemented/architecture/2026-08-23-agent-native-loop.md`。

Deferred items:
- legacy ReactAgent/旧 REST/Alibaba 依赖删除 -> 具名 follow-up `agent-native-loop-retire-legacy`（change-3b）。
- 真实 OpenAI-compatible runtime fixture 冒烟与真实模型 Eval -> 同上 follow-up。
- rag_qa/question_generation 循环策略 -> 后续 change。

## 审查摘要

- review_mode: single-agent
- Spec Review 先行：3a/3b 拆分、LlmClient 流式端口、tool-call 回填、回复队列暂停恢复均已对照计划复核。
- Code Review：SSE 解析、ToolDispatcher 权限/去重/并行、AgentLoop nudge/预算/终态、TurnRuntimeService WAITING_INPUT 转换均已检查；修复了 check 编译、测试期望和 prompt 资源问题。
- 最终 P0/P1: 0；P2: 0 未关闭（3b legacy 删除与真实冒烟为具名 follow-up）。
