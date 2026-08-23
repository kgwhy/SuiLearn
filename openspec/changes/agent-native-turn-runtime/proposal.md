# Agent-Native Turn Runtime（改造计划 change-1）

## Why

`docs/plans/suilearn-refactor-plan.md` 已批准改造方向：把现有固定 ReactAgent 学习 MVP 演进为可路由、可流式、可暂停恢复、可扩展的 Agent-Native 运行时。该计划要求按 change-1 → change-6 顺序拆分，且每个 change 归档后再启动下一个。

本 change 是 change-1：先锁定回合契约，并落地 `TurnRuntimeService`、`TurnEventBus`、PostgreSQL 事件持久化、REST 同步入口和 WebSocket 骨架。本阶段不实现真实 LLM 能力，旧 `/api/v2/agents/study/runs` 在 change-3 验收前保持不变。

## What Changes

- 新增回合核心类型：`TurnContext`、`StudyScope`、`SourceSelection`、`ChatMessage`、`Attachment`、`StreamEvent`、`EventType`、`TurnStatus`、`TurnResult`。
- 新增能力/工具最小声明协议：`Capability`、`CapabilityManifest`、`Tool`、`ToolDefinition`、`ToolResult`（本阶段只定义边界，不做 Spring 注册表；注册与工具化在 change-2）。
- 在 `contracts/openapi/suilearn-v2.yaml` 新增 v2 REST 回合端点与 schema。
- 新增 WS companion schema `contracts/schemas/suilearn-ws.yaml` 与 golden files；WS 命令/事件/错误码以 companion schema 为契约真相源。
- 新增 `TurnEventBus`：每回合一个、有界队列、事件先落库后推送、按 `afterSeq` 重放。
- 新增 `TurnRuntimeService`：start / subscribe / resume / cancel / submitReply / checkActiveTurn，支持同步 REST 等待终态。
- 新增 PostgreSQL 模型：`turn`、`turn_events`、`session_message`；事件唯一键 `(turn_id, seq)`，查询索引 `(session_id, created_at)`；应用启动时把残留 `RUNNING` 回合标记为 `FAILED_ORPHANED`。
- 新增 Spring MVC WebSocket endpoint `/api/v2/ws`（`TextWebSocketHandler`，不引入 WebFlux/Reactor）。
- 新增 v2 REST 端点直接调用 `TurnRuntimeService`，作为 WS 的同步便捷封装。
- `services/api/pom.xml` 增加 `spring-boot-starter-websocket`；配置仅新增 `suilearn.agent.websocket.enabled`。
- 本阶段 `TurnExecutor` 使用显式 `TURN_EXECUTOR_UNAVAILABLE` 终态，不伪造 Agent 回答、证据或练习。

## Capabilities

### New

- `agent-turn-runtime`: 回合生命周期、事件流、WS/REST 入口、事件持久化、续流和孤儿恢复。
- `agent-turn-contract`: v2 回合 REST 契约、WS companion schema、golden files 与契约测试。

### Modified

无。旧 `react-study-agent` 同步运行接口与固定 ReactAgent 拓扑保持不变，待 change-3 验收后删除。

## Impact

- `contracts/openapi/suilearn-v2.yaml`：新增 additive v2 回合端点与 schema；不修改既有端点语义。
- `contracts/schemas/suilearn-ws.yaml`：新增 WS companion schema。
- `services/api/pom.xml`：新增 `spring-boot-starter-websocket`。
- `services/api/src/main/java/com/suilearn/api/agent/**`：新增 runtime/controller/infrastructure 代码；不修改旧 Agent 实现。
- `services/api/src/main/resources/application.properties`、`services/api/config/local.properties.example`、`.env.example`、`compose.yml`：新增并透传 `suilearn.agent.websocket.enabled` / `SUILEARN_AGENT_WEBSOCKET_ENABLED`。
- `services/api/src/test/java/com/suilearn/api/agent/**`、`services/api/src/test/resources/agent-turn/**`：新增单元/契约测试与 golden files。
- `.agents/notes/implemented/architecture/2026-08-23-agent-native-turn-runtime.md`：记录本 change 的架构取舍。
- 不修改 `apps/android/**`、`apps/web/**`、旧 Agent 业务路径、`docs/**` 当前事实文档。

## Non-Goals

- 不实现 `AgentLoop`、`LlmClient`、`ToolDispatcher` 或真实 `study_agent` 能力（change-2/change-3）。
- 不删除旧 `LearningAgentPort`、ReactAgent、Spring AI Alibaba 依赖或旧 REST 端点。
- 不迁移、不删除 Redis 或旧语义记忆；新运行时在本 change 不读旧记忆。
- 不实现 Web/Android 新客户端；客户端切换按计划排在 change-6。
- 不引入 Flyway/Liquibase、WebFlux/Reactor、Rabbit fanout 或多实例实时推送。
- 不实现鉴权；`learnerId` 继续是调用方提供的逻辑范围标识，不是身份。

## Acceptance Criteria

- OpenAPI v2 新增回合端点/ schema 通过结构校验；旧端点路径与旧 schema 保持不变。
- WS companion schema 与 golden files 可解析，并由契约测试锁定。
- `TurnRuntimeService` 能完成 start / subscribe / resume / cancel / submitReply / checkActiveTurn。
- 事件按 `turnId` 从 1 单调递增、不跳号，`(turn_id, seq)` 唯一；断线后 `afterSeq` 重放不丢不重。
- 慢消费者只丢弃实时推送，不增长无界内存；从 `afterSeq` 续流仍完整。
- 应用重启时残留 `RUNNING` 回合标记为 `FAILED_ORPHANED`，并产生唯一终态事件。
- REST 同步入口等待终态并返回 `TurnResult`；新入口不要求与旧 `/api/v2/agents/study/runs` 行为一致。
- `suilearn.agent.enabled=false` 时新 REST/WS 统一返回现有 `AGENT_FEATURE_DISABLED` 语义；`suilearn.agent.websocket.enabled=false` 时 WS 命令返回 `AGENT_WEBSOCKET_DISABLED`。
- 本阶段默认 executor 不生成内容、不写正式题库；只能产生显式 unavailable 终态。
- 新增代码不引入 WebFlux bean；Spring AI 类型仍只出现在 `infrastructure/springai/**`。
