# Agent-Native Turn Runtime 设计（change-1）

## Context

基线 `dev` HEAD `6de3ec5`。当前 Backend 是 Spring MVC + PostgreSQL/pgvector + RabbitMQ 模块化单体，Agent 路径是同步 REST + 固定 ReactAgent 拓扑；没有回合持久化、事件流、WS 或暂停恢复。本 change 只搭新运行时的“回合生命线”，不替换 LLM 循环。

## Goals / Non-Goals

**Goals:**

- 契约先行：OpenAPI 定义新 REST，companion schema 定义 WS 命令/事件。
- 回合事件以 PostgreSQL 为唯一事实源，实时推送只是本实例有界加速通道。
- 新 REST/WS 入口与旧 Agent 入口共存，旧入口在 change-3 验收后删除。
- 核心运行时可内存化测试；JPA 持久化与索引脚本同批交付。

**Non-Goals:**

- 不实现真实能力/工具执行、上下文压缩、三层记忆、RAG 引擎化、用量计费、多租户。
- 不把旧 Agent 桥接到新 runtime（禁止 legacy 双跑）。
- 不做 RabbitMQ fanout 或 WebFlux。

## Decisions

### 1. REST 采用 `/api/v2/agent/turns` 资源模型

```text
POST /api/v2/agent/turns                            同步启动并等待终态
GET  /api/v2/agent/turns/{turnId}/events?afterSeq=  事件续流（REST 版 replay）
POST /api/v2/agent/turns/{turnId}/cancel            取消执行中的回合
POST /api/v2/agent/turns/{turnId}/reply             投递 WAITING_INPUT 回复
GET  /api/v2/agent/sessions/{sessionId}/active-turn 查询会话当前回合
```

**Alternative**: 复用 `POST /api/v2/agent/runs`。否决：`runs` 与旧 `StudyAgentRunResponse` 语义绑定，且“turn”更能表达新运行时回合模型。
**Alternative**: REST 只返回 202 + taskId 轮询。否决：计划要求 REST 是 WS 的同步便捷封装，且客户端切换成本会上升。

### 2. WS 消息使用同一外层信封

所有 WS 文本消息都是 JSON 对象，以 `kind` 做判别：

```text
kind=command | event | ack | error | pong
```

命令字段见 `contracts/schemas/suilearn-ws.yaml`；事件直接复用 `StreamEvent` 结构并加 `kind=event`。协议错误返回 `kind=error` 的稳定 `AgentTurnError`，不关闭连接；二进制帧返回错误后可关闭。

**Alternative**: 顶层无 `kind`，用 `command`/`type` 字段存在性推断。否决：判别歧义且解析器脆弱。
**Alternative**: 每个命令一个 WebSocket path。否决：与 `/api/v2/ws` 单端点计划冲突，也不利于连接复用。

### 3. companion schema 采用 OpenAPI 风格 YAML + golden files

`contracts/schemas/suilearn-ws.yaml` 不是独立 OpenAPI 文档，但复用 `components.schemas` 组织方式，定义 `TurnWsEnvelope`、`TurnCommand`、`TurnEventEnvelope`、`TurnAck`、`AgentTurnError` 与所有枚举。golden files 放在 `services/api/src/test/resources/agent-turn/golden/**`，测试用 SnakeYAML 解析 schema 并用 Jackson 序列化样本做结构比对。

**Alternative**: JSON Schema draft。否决：项目现有契约测试都基于 SnakeYAML + OpenAPI 风格，引入 JSON Schema validator 是额外依赖且不与现有 CI 复用。
**Alternative**: 只放 JSON 样本。否决：枚举、必填字段和错误码没有机器可读约束。

### 4. 事件模型与持久化

```text
turn(id, session_id, learner_id, capability, status, scope_json,
     source_selection_json, input_message_id, last_seq, created_at,
     started_at, finished_at, updated_at, version)

turn_events(turn_id, session_id, seq, type, payload, created_at)
  unique(turn_id, seq)
  index(session_id, created_at)

session_message(id, session_id, learner_id, turn_id, role, content, created_at)
```

- `payload` 是 sanitized `StreamEvent` JSON，单条 UTF-8 上限 64 KiB；正文/证据大对象只存引用。
- 事件先事务化写入 `turn_events`，成功后 seq 才对外可见；实时 push 失败不影响一致性。
- 项目未使用 Flyway/Liquibase：表结构由 JPA `ddl-auto=update` 创建，Hibernate 不管理的索引/孤儿恢复用幂等 `ApplicationRunner` 初始化器完成，与 `PostgresChunkSearchIndexMigration` 模式一致。
- 首版只支持单实例实时推送；任何实例都可以从 `turn_events` 重放。

**Alternative**: 引入 Flyway。否决：超出本 change 工具链范围，也改变全项目迁移基线。
**Alternative**: 事件表只存引用不存 payload。否决：replay 会依赖外部对象生命周期，增加一致性问题。

### 5. 运行时拓扑

```text
TurnController / TurnWebSocketHandler
  -> TurnRuntimeService
     -> TurnStore (InMemoryTurnStore 测试 / JpaTurnStore 生产)
     -> TurnEventBus (per turn)
        -> JpaTurnStore.appendEvent (事务)
        -> bounded queue -> WS subscribers
     -> TurnExecutor (本阶段: UnavailableTurnExecutor)
```

- `TurnRuntimeService.startTurn` 在同一事务创建 turn、session_message 和 seq=1 `turn_started`，提交后再提交虚拟线程执行。
- 每回合一个 `TurnEventBus`，队列容量默认 256；满时 `offer` 丢弃实时帧并计数，持久化不受影响，客户端可 `resume_from`。
- 终态（done/cancelled/failed）只能出现一次；终态后 bus 拒绝新事件并唤醒等待者。
- `cancelTurn` 只允许 `CREATED/RUNNING/WAITING_INPUT`；取消后 executor 下一次发布事件时被拒绝。
- `submitReply` 只允许 `WAITING_INPUT`；本阶段 executor 不会进入该状态，但命令与错误码先稳定。

**Alternative**: 用 Spring `ApplicationEventPublisher` 做跨 turn bus。否决：缺少每回合有界背压与 replay 语义。
**Alternative**: REST 同步等待复用 WS handler。否决：REST 不持有 WebSocketSession，直接等 `CompletableFuture` 更简单。

### 6. 阶段内占位 executor

本阶段 bean `UnavailableTurnExecutor` 发布：

```text
turn_started(1) -> progress -> error(TURN_EXECUTOR_UNAVAILABLE) -> failed
```

不发布 `result/done`，避免把“尚无能力”伪装成成功回合。旧同步 Agent 端点继续可用，因此不存在用户可用性回退。

**Alternative**: 把旧 `LearningAgentPort` 包成 executor。否决：计划明确不做 legacy 双跑，且会把旧模型阻塞语义带入新 runtime。
**Alternative**: 用 echo 能力演示成功终态。否决：echo 会看起来像真实回答，且没有证据/引用约束，测试可能误用。

### 7. 配置语义

- `suilearn.agent.enabled=false`：总开关，新 REST/WS 都返回 `AGENT_FEATURE_DISABLED`。
- `suilearn.agent.websocket.enabled=true`（默认 true）：仅控制 WS endpoint 是否接受命令；总开关仍优先。
- 只新增这一个配置键；队列容量、payload 上限和 REST 等待时间先作为代码常量，不提前参数化。
- `.env.example` 记录 `SUILEARN_AGENT_WEBSOCKET_ENABLED=true`；Compose 使用 `${SUILEARN_AGENT_WEBSOCKET_ENABLED:-true}` 透传，应用默认值同为 true，三处语义一致。

**Alternative**: websocket 默认 false。否决：计划把 websocket 作为总开关下的子能力，默认 false 会让“启用 Agent”后仍无法用 WS，违背 Phase 1 验收。

### 8. 测试策略

- 单元测试用 `InMemoryTurnStore` 覆盖 start/subscribe/resume/cancel/orphan 与队列背压。
- Controller 用直接调用 + 轻量 MockMvc；WS handler 用 mock `WebSocketSession`。
- 契约测试解析 `contracts/openapi/suilearn-v2.yaml` 与 `contracts/schemas/suilearn-ws.yaml`，并用 golden files 锁定样本。
- 无本地 PostgreSQL/Docker 时，完整 `mvn test` 只作为施工基线记录；变更门禁执行新增的定向 `-Dtest=...` 测试。Testcontainers/JPA 集成测试不在本沙箱默认门禁。

## Risks / Trade-offs

- 表名 `turn` 是 PostgreSQL 非保留字；如未来切换数据库需复核，JPA 实体是唯一表名定义点。
- 首版实时推送是尽力而为：慢消费者丢实时帧但可 replay；需要在验证中明确。
- 占位 executor 返回失败终态，产品可见价值要到 change-3 才出现；这是计划内分期，不视为回归。
- 完整后端回归在沙箱无 PostgreSQL 时无法全绿；以干净 shell 定向测试 + 基线 35 errors 根因记录作为验证边界。
