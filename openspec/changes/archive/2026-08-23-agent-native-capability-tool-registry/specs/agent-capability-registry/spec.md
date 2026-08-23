## ADDED Requirements

### Requirement: CapabilityRegistry 必须提供稳定内置能力与默认路由
系统 MUST 注册 `study_agent`、`rag_qa`、`question_generation` 三个能力；`TurnContext.capability()` 为空时 MUST 使用 `study_agent`，未知能力 MUST 返回 `AGENT_CAPABILITY_UNKNOWN`。

#### Scenario: 默认能力
- **WHEN** 启动回合时 capability 为 null 或空白
- **THEN** 系统解析为 `study_agent`，不创建其他能力状态

#### Scenario: 未知能力
- **WHEN** 请求 capability 不在注册表
- **THEN** 系统在回合执行前返回 `AGENT_CAPABILITY_UNKNOWN`

### Requirement: 工具权限必须由 manifest 与 definition 计算
系统 MUST 通过 `CapabilityManifest.ownedTools()` 与 `ToolDefinition.requiredScopes()` 计算允许工具；运行时 MUST 拒绝 capability 未拥有的工具，且 MUST NOT 因模型请求动态扩大权限。

#### Scenario: study_agent 完整工具面
- **WHEN** 查询 `study_agent`
- **THEN** 允许工具包含 `search_knowledge / read_evidence / generate_practice / recall_memory / persist_memory / ask_user`

#### Scenario: rag_qa 受限工具面
- **WHEN** 查询 `rag_qa`
- **THEN** 允许工具只包含检索与证据读取工具

#### Scenario: 越权调用
- **WHEN** 代码请求 capability 未拥有的工具
- **THEN** `ToolRegistry` 抛出禁止动作错误，不执行工具

### Requirement: ToolRegistry 必须生成 OpenAI 兼容 schema
系统 MUST 对每个注册工具输出 `{type:"function", function:{name, description, parameters}}`，并 MUST 拒绝重复工具名或缺少 parameters 的定义。

#### Scenario: 工具 schema
- **WHEN** 调用 `/api/v2/agent/capabilities`
- **THEN** 每个工具包含 name/description/parameters，且 parameters 是 JSON Schema object

#### Scenario: 重复工具名
- **WHEN** 两个 Tool bean 声明相同 name
- **THEN** 注册表构造失败，不静默覆盖

### Requirement: 六个工具必须保持产品安全边界
`search_knowledge` 与 `read_evidence` MUST 复用现有 `EvidenceSearchPort/EvidenceReadPort` 的 scope、删除状态与 learner 可见性校验；`generate_practice` MUST 只返回临时练习；`persist_memory` MUST 只写 Agent 语义记忆；`ask_user` MUST 只返回 pause 请求，不自行终结回合。

#### Scenario: 证据工具越界
- **WHEN** 工具参数请求 scope 外或已删除 sourceRef
- **THEN** 返回空结果或失败，不生成越界 citation

#### Scenario: 练习工具无模型
- **WHEN** `PracticeCoachSubAgent` 依赖不可用
- **THEN** `generate_practice` 返回 `success=false` 且 code 为 `AGENT_MODEL_UNAVAILABLE`

#### Scenario: ask_user 暂停请求
- **WHEN** 模型请求用户补充信息
- **THEN** `ask_user` 返回非 success ToolResult 与结构化 `pauseForUser`

### Requirement: TurnOrchestrator 必须按 capability 路由且不伪造成功
系统 MUST 将 `TurnRuntimeService` 的默认 executor 指向 `TurnOrchestrator`；orchestrator 按 capability manifest 发布路由元数据。本阶段 MUST 以 `TURN_EXECUTOR_UNAVAILABLE + failed` 终态结束，MUST NOT 生成回答、练习或正式内容。

#### Scenario: study_agent 路由
- **WHEN** 新 runtime 启动一个默认回合
- **THEN** 事件 source 为 `study_agent`，且终态为 `failed(TURN_EXECUTOR_UNAVAILABLE)`

#### Scenario: 无伪造成功
- **WHEN** 任意已注册 capability 执行
- **THEN** 事件流不包含 `result/done`，不写正式内容 store

### Requirement: capabilities 契约端点必须 additive 且受总开关约束
系统 MUST 新增 `GET /api/v2/agent/capabilities`；`suilearn.agent.enabled=false` 时 MUST 返回 `AGENT_FEATURE_DISABLED`，启用时 MUST 返回 capability 与 tool 两个列表。

#### Scenario: 能力枚举
- **WHEN** Agent 已启用且调用 capabilities
- **THEN** 返回 3 个 capability、6 个工具，且 schema 不含 secret、Prompt 或用户正文
