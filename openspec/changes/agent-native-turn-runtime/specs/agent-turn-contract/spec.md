## ADDED Requirements

### Requirement: OpenAPI 必须提供 v2 Agent 回合资源端点
系统 MUST 在 `contracts/openapi/suilearn-v2.yaml` 新增以下 additive 端点，且 MUST NOT 修改既有 `/api/v2/agents/study/runs` 路径语义：

- `POST /api/v2/agent/turns`
- `GET /api/v2/agent/turns/{turnId}/events`
- `POST /api/v2/agent/turns/{turnId}/cancel`
- `POST /api/v2/agent/turns/{turnId}/reply`
- `GET /api/v2/agent/sessions/{sessionId}/active-turn`

#### Scenario: 启动回合
- **WHEN** 调用方提交 `{learnerId, message, scope, capability?, sessionId?}` 且至少提供一个 scope 标识
- **THEN** 服务端创建 turn 与 session_message，同步等待终态并返回 `AgentTurnResult`

#### Scenario: REST 续流
- **WHEN** 调用方用 `afterSeq` 查询既有 turn events
- **THEN** 返回该 turn 中 `seq > afterSeq` 的全部事件和当前 `lastSeq`，不返回其他 turn 的事件

### Requirement: 回合请求必须服务端校验 scope 与 capability
系统 MUST 校验 `learnerId`、`message`、`scope`、可选 `capability` 和可选 `sessionId`；非法 capability 必须在回合启动前返回稳定契约错误。`StudyScope` MUST 至少包含 `knowledgeBaseId` 或 `materialId`。

#### Scenario: 缺少 scope
- **WHEN** 请求既没有 knowledgeBaseId 也没有 materialId
- **THEN** 系统返回 `AGENT_SCOPE_REQUIRED`，不创建 turn 或事件

#### Scenario: 非法 capability
- **WHEN** 请求指定未登记 capability
- **THEN** 系统返回 `AGENT_CAPABILITY_UNKNOWN`，不创建 turn 或事件

### Requirement: WS companion schema 必须定义命令、事件、确认和错误
系统 MUST 在 `contracts/schemas/suilearn-ws.yaml` 提供可解析的 companion schema，定义 `kind=command|event|ack|error|pong` 外层信封，以及 `start_turn / subscribe_turn / resume_from / cancel_turn / submit_user_reply / check_active_turn / ping` 命令字段和错误码枚举。

#### Scenario: 协议样本校验
- **WHEN** 契约测试加载 companion schema 与 golden files
- **THEN** 每个 golden 样本只含 schema 声明的 kind/command/event type，且必填字段完整

### Requirement: 服务端事件类型必须使用契约枚举
系统 MUST 只发布 companion schema 中声明的 `EventType` 枚举值；`turn_started` 是 seq=1 的首个事件，终态事件 `done / cancelled / failed` 必须且只能出现一个。

#### Scenario: 事件顺序
- **WHEN** 一个 turn 成功运行
- **THEN** seq 从 1 开始连续递增，且首个事件类型为 `turn_started`

#### Scenario: 事件类型越界
- **WHEN** 内部代码尝试发布未知 EventType
- **THEN** 序列化或校验拒绝，不写入 `turn_events`

### Requirement: REST 错误响应必须稳定且 sanitized
系统 MUST 为回合端点定义 `AgentTurnError`，错误 message 不含用户正文、Prompt、原始模型输出、内部队列名或 API key；`correlationId` 只用于运维定位。

#### Scenario: Agent 总开关关闭
- **WHEN** `suilearn.agent.enabled=false` 且调用方访问任一新回合端点
- **THEN** 返回 `AGENT_FEATURE_DISABLED` 错误，不创建 turn

#### Scenario: WS 子开关关闭
- **WHEN** `suilearn.agent.websocket.enabled=false` 且 WS 连接发送任何命令
- **THEN** 返回 `AGENT_WEBSOCKET_DISABLED` 错误
