# Agent-Native 改造当前事实收口（change-final-sync）

## Why

change-1 至 change-6b 已按顺序归档，但各 change 归档时为避免中间态漂移均记录 `docs/** not affected`。现在旧 ReactAgent/旧 Agent REST 已删除，新 TurnRuntime/AgentLoop/LlmClient/ContextBuilder/记忆/RAG 引擎/UsageTracker/TurnResult 信封均已落地，三份当前事实文档仍描述旧 MVP，需要一次 Sync Gate 收口。

## What Changes

- `docs/product-requirements.md`：新增 Agent 学习助手能力、入口、能力路由、工具面、暂停追问、续流与用量信封等当前规格。
- `docs/architecture.md`：新增 agent-native runtime 包结构、REST/WS 入口、能力/工具注册表、循环与上下文、记忆与持久化、RAG 引擎化边界、跨端数据流；修正旧 Spring AI 预留描述。
- `docs/tech-selection.md`：更新 Backend Agent 技术基线、Agent 配置默认值、WebSocket/SSE/PostgreSQL 选择；修正 Spring AI 与 Redis 表述。

## Non-Goals

- 不修改 `apps/**`，不实现 Android 新协议客户端。
- 不启动 Phase 8。
- 不修改任何业务代码、契约或测试。
- 不把尚未完全接入在线路径的组件写成已全量接通；未接通项写入开放风险。

## Acceptance Criteria

- 三份文档新增内容均可由当前源码/契约/测试验证，不包含计划态或 Phase 8 目标。
- `python3 scripts/check_suilearn_workflow.py --base-ref d450780` 通过。
- 完成归档与单人自审。
