## ADDED Requirements

### Requirement: AgentTurnResult 必须携带稳定的 usage/action/context 汇总
系统 MUST 在 REST `AgentTurnResult` 中返回 `promptTokens`、`completionTokens`、`usageCostUsd`、`actionTraceCount`、`estimatedContextTokens` 五个必填字段，数值非负。

#### Scenario: 成功回合有 RESULT usage metadata
- **WHEN** 本回合最后一个 RESULT 事件 metadata 含 promptTokens=120、completionTokens=30、usageCostUsd=0.0004、toolCalls=3、estimatedContextTokens=2400
- **THEN** `AgentTurnResult` 五个汇总字段分别为 120、30、0.0004、3、2400，terminalEvent 仍为 DONE 终态事件

#### Scenario: 无 RESULT 或字段缺失
- **WHEN** 回合以 FAILED/CANCELLED 终态结束，或 RESULT metadata 缺少任一汇总字段
- **THEN** 对应汇总字段返回 0，不破坏 terminalEvent 与 lastSeq

#### Scenario: 契约稳定可解析
- **WHEN** OpenAPI 消费者解析 `AgentTurnResult`
- **THEN** 五个汇总字段均为 required，且类型为 integer/double 且 minimum >= 0
