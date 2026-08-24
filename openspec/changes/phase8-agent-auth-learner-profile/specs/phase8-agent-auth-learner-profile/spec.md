## ADDED Requirements

### Requirement: Agent 端点必须支持可关闭的 Bearer 鉴权
系统 MUST 在 `suilearn.auth.enabled=true` 时要求 Agent REST/WS 提供有效 Bearer token，并将 principal learnerId 作为权威 learner。

#### Scenario: 关闭
- **WHEN** auth disabled
- **THEN** Agent 端点保持既有行为

#### Scenario: 开启
- **WHEN** 无 token / 错 token 访问 Agent REST
- **THEN** 分别返回 401 / 403，不泄露资源

### Requirement: learner 资源必须按 principal 隔离
系统 MUST 对 turn/events/cancel/reply/active-turn 校验 learner 归属；跨 learner 访问返回 not found。

#### Scenario: 越权读取
- **WHEN** learner A 用有效 token 查询 learner B 的 turn
- **THEN** 返回 AGENT_TURN_NOT_FOUND，不返回事件

### Requirement: learner profile 必须可管理并注入 Prompt
系统 MUST 提供 learner profile GET/PUT，并在回合构建时注入 persona/skills PromptBlock。

#### Scenario: 注入
- **WHEN** profile 含 persona 与 skills
- **THEN** PromptBlock 列表包含 persona、skills 且内容稳定
