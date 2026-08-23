## ADDED Requirements

### Requirement: LlmClient 必须提供流式函数调用与 usage
系统 MUST 提供 `LlmClient` 端口，以 `Stream<LlmChunk>` 返回增量 content、按 index 合并的 tool-call delta 与 usage；`chat` 默认聚合流。实现 MUST NOT 依赖 Spring AI 类型。

#### Scenario: SSE 工具调用
- **WHEN** OpenAI-compatible fixture 返回 content delta、tool_calls delta 与 usage
- **THEN** 聚合响应包含完整 content、完整 tool call name/arguments 和 usage 数字

#### Scenario: 非 2xx
- **WHEN** 上游返回 4xx/5xx
- **THEN** LlmClient 抛稳定失败，不返回部分成功响应，不记录错误 body 到事件

### Requirement: AgentLoop 必须完成通用工具循环
系统 MUST 让 study_agent 通过 AgentLoop 执行：LLM 请求 -> tool_calls -> ToolDispatcher -> 回填 -> 最终回答；工具只能来自 capability manifest 的允许集合。

#### Scenario: 三步工具循环
- **WHEN** fake LLM 先请求 search_knowledge、再请求 read_evidence、最后返回回答
- **THEN** 事件顺序包含 tool_call/tool_result，并以 RESULT + DONE 结束

#### Scenario: 空回答 nudge
- **WHEN** LLM 返回空 content 且无 tool calls
- **THEN** 系统追加 nudge 消息重试最多 2 次；仍空则 `INVALID_MODEL_OUTPUT` + FAILED

#### Scenario: 预算耗尽
- **WHEN** 步数或工具调用超过配置上限
- **THEN** 停止新调用并返回 `BUDGET_EXHAUSTED` + FAILED，不伪造成功

### Requirement: ToolDispatcher 必须并行、去重、缺参拒绝与权限拒绝
系统 MUST 对允许工具并行执行（上限 8），对同一轮相同 name+arguments 去重，缺必填参数返回可修复工具错误，capability 未拥有工具直接拒绝。

#### Scenario: 重复调用
- **WHEN** 模型同一轮两次请求相同工具与参数
- **THEN** 底层工具只执行一次，结果回填两次引用同一结果

#### Scenario: 越权工具
- **WHEN** 模型请求 manifest 未拥有工具
- **THEN** 服务端拒绝且不执行底层端口

### Requirement: ask_user 必须暂停并原位恢复
系统 MUST 在 `ask_user` 时发布 WAIT_FOR_INPUT 并等待 `submit_user_reply`；回复投递后 AgentLoop 从原工具调用继续，不重跑整个回合。

#### Scenario: 暂停恢复
- **WHEN** fake LLM 请求 ask_user
- **THEN** 回合状态为 WAITING_INPUT 且事件含 wait_for_input；提交回复后状态回到 RUNNING 并最终 DONE

#### Scenario: 非等待回合回复
- **WHEN** 对非 WAITING_INPUT 回合调用 submit_user_reply
- **THEN** 返回 `AGENT_TURN_NOT_WAITING_FOR_INPUT`，不改变状态

### Requirement: Orchestrator 只替换 study_agent 执行器
系统 MUST 让 `TurnOrchestrator` 使用 AgentLoop 执行 study_agent；未接线能力保持显式 unavailable，不得伪成功。

#### Scenario: study_agent 路由
- **WHEN** 默认能力启动回合
- **THEN** 不再出现 TURN_EXECUTOR_UNAVAILABLE 的 study_agent 终态（fake LLM 下应 DONE）

#### Scenario: 未接线能力
- **WHEN** question_generation 启动回合
- **THEN** 返回显式 unavailable failed，不调用循环

### Requirement: 离线 fixed Eval 必须覆盖核心行为
系统 MUST 提供 deterministic offline Eval，至少覆盖工具循环、空回答、预算、缺参/重复调用、越权工具、暂停恢复；Eval MUST NOT 访问公网或真实模型。

#### Scenario: CI 运行 Eval
- **WHEN** 后端测试运行默认 profile
- **THEN** Eval 全部通过并报告场景计数
