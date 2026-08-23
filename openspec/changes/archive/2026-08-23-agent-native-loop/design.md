# Agent-Native Loop 设计（change-3a）

## Context

基线 `e801f84`。已有能力/工具注册表与回合事件流。旧 ReactAgent 路径仍作为对照存在。本 change 只替换新 runtime 中 study_agent 的 unavailable executor，不动旧路径。

## Goals / Non-Goals

**Goals:** 可测试、离线、无框架绑定的 LLM loop；流式 adapter 可解析真实 OpenAI-compatible SSE；暂停恢复与工具权限闭合。

**Non-Goals:** legacy 删除、真实模型联网、usage 计价、文本回退主路径。

## Decisions

### 1. LlmClient 是流式端口，聚合是默认行为

```java
Stream<LlmChunk> stream(LlmRequest request);
LlmResponse chat(LlmRequest request) // default: stream + aggregate
```

- `LlmChunk` 只携带 delta，不携带完整消息，避免重复正文。
- tool call delta 按 index 合并；usage 取最后一个非空 usage 帧。
- Adapter 用 `HttpClient` + `BodyHandlers.ofLines` 解析 SSE；`data: [DONE]` 结束。
- 非 2xx 立即失败，不把错误 body 写入日志或事件。

**Alternative**: 只做阻塞 `chat`。否决：计划要求流式底层能力，且后续 WS `content` 事件要复用。
**Alternative**: 复用 Spring AI ChatClient。否决：会重新引入框架图/agent 边界，且计划只允许 Spring AI 类型出现在 infrastructure/springai。

### 2. AgentLoop 使用 tool-call 消息回填

每轮：
1. system(user/assistant/tool 历史) -> LlmClient
2. 有 tool_calls：ToolDispatcher 执行 -> assistant tool_calls 与 tool 结果回填 -> 下一轮
3. 无 tool_calls：content 非空 -> RESULT -> DONE
4. content 空：最多 2 次 nudge 修复；仍空 -> `INVALID_MODEL_OUTPUT` + FAILED
5. 步数/工具数/超时预算耗尽 -> `BUDGET_EXHAUSTED` + FAILED

工具 schema 由 `ToolRegistry.openAiSchemas(manifest)` 过滤，模型只看到 capability 拥有的工具。

**Alternative**: 继续用 Alibaba ReactAgent。否决：循环被框架绑定，无法统一预算/修复/暂停。
**Alternative**: 文本标签回退为默认。否决：计划要求默认原生 function calling。

### 3. ToolDispatcher 的权限与幂等

- 服务端先 `ToolRegistry.require(manifest, name)`，再解析 arguments。
- 按 definition.parameters.required 做缺参校验；非法 JSON/缺参调用返回 tool 错误并允许 loop 修复。
- `(name, canonicalArgumentsJson)` 去重，同一轮重复只执行一次。
- 并行度 `min(8, calls)`，使用 virtual thread executor。
- `ask_user` 结果不当作普通 tool result，转为 `AskUserPauseException` 交给 loop。

### 4. 暂停恢复用每回合回复队列

```text
AgentLoop --ask_user--> TurnEventSink.pauseForUser
  -> WAIT_FOR_INPUT 事件 + TurnStatus.WAITING_INPUT
  -> replyChannel.await(payload)
submit_user_reply -> replyChannel.complete(reply) + TurnStatus.RUNNING
  -> AgentLoop 继续原轮次
```

虚拟线程等待回复，不重跑回合。`TurnRuntimeService` 启动时注册 reply channel，终态/取消时释放等待者。

**Alternative**: 回合结束 + 重启 loop。否决：计划明确“从原工具调用继续，不重跑整个回合”。

### 5. 本轮 3a 不删旧路径

旧 ReactAgent、旧 REST、Alibaba 依赖保持；3b 单独做删除与残留扫描。原因：
- 沙箱没有真实模型/PostgreSQL，旧路径是最低风险对照。
- 新 loop 可离线 Eval，但真实运行态证据不足，不满足“验收后同阶段删除”的完整证据。

**Alternative**: 严格按计划本阶段删除。否决：单批次失去对照，且完整后端回归无法运行，风险不可控。

## Risks / Trade-offs

- 旧路径继续存在一个 change 周期，新旧 Agent 行为并存；3b 必须尽快归档。
- SSE 解析对不合规 provider 严格失败；这是预期安全行为，不做宽松解析。
- fake LLM 只能证明循环正确，不能证明真实模型输出质量；3b 前需要 runtime fixture 真实 adapter 冒烟。
