# Agent Note: 采用回合契约先行的 Agent-Native Turn Runtime 骨架
Status: implemented

## Problem

SuiLearn 原有学习 Agent 是同步 REST + 固定 ReactAgent 拓扑，没有回合持久化、事件流、断线续流、暂停恢复或 WebSocket 入口。改造计划要求先建 agent-native runtime，但 change-1 阶段还没有 `LlmClient`/`AgentLoop`，必须明确“这一阶段允许失败终态”以及“实时推送与持久化的关系”，否则占位实现容易被误当成功路径，或过早引入 WebFlux/Rabbit fanout。

## Decision

change-1 只锁定回合生命线与契约：

- OpenAPI 新增 `/api/v2/agent/turns` 资源族；WS 协议使用 `contracts/schemas/suilearn-ws.yaml` companion schema + golden files。
- `TurnRuntimeService` + 每回合 `TurnEventBus`；事件先事务化写入 PostgreSQL `turn` / `turn_events` / `session_message`，再进入有界实时队列。
- 队列容量 256，满时丢弃实时帧但持久化不丢；客户端用 `afterSeq` replay。首版不做多实例实时 fanout。
- change-1 的 `UnavailableTurnExecutor` 只发布 `TURN_EXECUTOR_UNAVAILABLE` + `failed`，旧同步 Agent 路径保持可用，直到 change-3 验收后删除。
- 不引入 Flyway；沿用 Hibernate `ddl-auto=update` + 幂等 `ApplicationRunner` 初始化器模式。
- 配置新增 `suilearn.agent.websocket.enabled`（默认 true），`suilearn.agent.enabled` 继续是总开关；`.env.example`、Compose 和 local properties 模板同步透传。
- WS endpoint 始终注册，`agent.enabled=false` 或 `websocket.enabled=false` 时命令返回稳定错误，避免握手阶段无法表达 disabled 语义。

## Alternatives considered

- **把旧 `LearningAgentPort` 包成新 runtime executor**：否决，计划明确禁止 legacy 双跑，且会把旧模型阻塞语义带入新 runtime。
- **用 echo 能力演示成功终态**：否决，echo 看起来像真实回答，容易让测试和调用方误用。
- **引入 Flyway/Liquibase**：否决，改变全项目迁移基线，超出 change-1 工具链范围。
- **引入 WebFlux/Reactor 做广播**：否决，与 Spring MVC 栈约束冲突。
- **WS schema 用 JSON Schema draft**：否决，项目现有契约测试基于 SnakeYAML + OpenAPI 风格，额外 validator 依赖不划算。
- **REST 路径复用 `/api/v2/agent/runs`**：否决，与旧 `StudyAgentRunResponse` 语义绑定，不利于客户端切换。

## Consequences

- 新 REST/WS 契约与 golden files 由自动化测试锁定；27 个新增定向测试通过。
- 回合事件 seq 唯一连续、可 replay、终态唯一；慢消费者不拖垮执行器。
- 启动时残留 RUNNING 回合幂等转为 FAILED_ORPHANED。
- 本阶段不修改旧 Agent 路径，不伪造成功回答。
- `turn` 表名在 PostgreSQL 可用；未来换库需复核。
- 无本地 PostgreSQL 时完整后端回归不能全绿；验证以干净 shell 定向测试 + 完整回归环境根因记录为准，不静默跳过。
