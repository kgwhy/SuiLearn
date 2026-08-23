# Agent-Native Loop 与 LlmClient（改造计划 change-3a）

## Why

change-1/2 已交付回合运行时、能力/工具注册表，但 `TurnOrchestrator` 仍以 `TURN_EXECUTOR_UNAVAILABLE` 终态结束。change-3 计划要求通用 AgentLoop、LlmClient、ToolDispatcher 与暂停恢复。原计划还要求同一阶段删除旧 ReactAgent；沙箱无真实模型且完整后端回归不可用，一次性删除会使旧 MVP 与所有兼容测试在同一批次失去对照，故本 change 按推荐拆为 3a/3b：3a 先落地新循环并用离线 fixed Eval 证明；3b 删除 legacy 作为具名 follow-up。

## What Changes

- 新增 `agent/llm` 端口：`LlmClient`、流式 chunk、tool-call delta、usage 与统一请求/响应模型。
- 新增 OpenAI-compatible streaming adapter：复用现有 `java.net.http` 模式解析 SSE，聚合 content/tool_calls/usage；不引入 Spring AI 或 LangChain4j。
- 新增 `agent/loop`：`AgentLoop`（默认原生 function calling）、`ToolDispatcher`（最多 8 并行、缺参拒绝、重复调用去重、`ask_user` 暂停）。
- `TurnOrchestrator` 接入 AgentLoop：`study_agent` 走真实循环；`rag_qa`/`question_generation` 仍保持显式 unavailable，待对应能力策略 change。
- `TurnEventSink` 增加 `pauseForUser` 回复通道；`TurnRuntimeService.submitReply` 从占位校验升级为实际投递并恢复 `RUNNING`。
- 新增离线 fixed Eval：fake LlmClient + 真实 ToolRegistry/TurnEventSink 覆盖工具循环、空回答 nudge、预算、缺参/重复调用修复、ask_user 暂停恢复。
- 不删除旧 `LearningAgentPort`、ReactAgent、旧 REST 或 Alibaba 依赖；不修改旧路径测试。

## Capabilities

### New

- `agent-native-loop`: LlmClient 端口、OpenAI 兼容流式 adapter、AgentLoop、ToolDispatcher、暂停恢复与离线 Eval。

### Modified

- `agent-turn-runtime`: `TurnOrchestrator` 执行 `study_agent` 循环；`submitReply` 实际投递 WAITING_INPUT 回复。
- `agent-capability-registry`: `ToolRegistry` 增加按 capability 过滤的 OpenAI tool schemas。

## Impact

- `services/api/pom.xml`：无新依赖；仅新增 Java 文件与资源。
- `services/api/src/main/java/com/suilearn/api/agent/{llm,loop,runtime,controller,tool}/**`。
- `services/api/src/main/resources/agents/agent-loop/v1/system.md`：受控 Prompt 资源。
- `services/api/src/test/java/com/suilearn/api/agent/**`：新增 adapter/loop/dispatcher/Eval 测试。
- 不修改 `contracts/**`、旧 Agent 路径、`apps/**`。

## Non-Goals

- 不删除旧 ReactAgent/旧 REST/Alibaba 依赖（3b follow-up）。
- 不实现 usage 计费价格表、ContextBuilder 升级、三层记忆、RAG 引擎化。
- 不让 `rag_qa`/`question_generation` 走循环；不实现文本标签回退主路径。
- 不引入真实模型调用测试；CI 全部离线 deterministic。

## Acceptance Criteria

- `LlmClient` 端口可由本地 SSE fixture 验证 content、tool_calls、usage 聚合，且不依赖 Spring AI 类型。
- `AgentLoop` 在 fake LLM 驱动下完成 ≥3 步工具循环并发布 RESULT/DONE；空回答触发 nudge；预算耗尽返回稳定失败；缺参/重复 tool call 可修复。
- `ToolDispatcher` 并行上限 8，重复调用只执行一次，非法工具由服务端权限拒绝。
- `ask_user` 发布 WAIT_FOR_INPUT，`submit_user_reply` 投递后同一 loop 继续并最终完成。
- `TurnOrchestrator` 对 study_agent 不再发布 TURN_EXECUTOR_UNAVAILABLE；未接线能力仍 unavailable。
- 离线 fixed Eval 全部通过；change-1/2 的 38 个测试保持通过。
