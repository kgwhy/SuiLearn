# Agent ContextBuilder 与 PromptBlock（change-4a）

## Why

change-3 已删除旧路径，但 AgentLoop 只使用固定 system prompt 和当前问题，没有会话历史窗口、分块 prompt 或真实请求预算。Phase 4 要求 ContextBuilder、滚动摘要、反漂移与预算报表；其中摘要/水位依赖 4b 记忆表，本 change 先完成窗口、分块和真实请求计量。

## What Changes

- 新增 `PromptBlock`/`PromptBlockAssembler`：general/policy/capability/memory/tools/skills 分块，整轮字节稳定。
- 新增 `ContextBuilder`：历史预算=有效窗口×0.35；超限先裁旧 tool 消息并插入截断标记；输出估算 token 与保留消息。
- `AgentLoop` 接入 ContextBuilder，加载 `session_message` 最近消息，并发布 context 预算报表与实际 usage。
- 不新增数据库表；滚动摘要与 `summary_up_to_msg_id` 水位随 4b 记忆表落地。

## Capabilities

### New

- `agent-context-builder`: prompt 分块、会话窗口守卫、预算报表与真实请求计量。

### Modified

- `agent-native-loop`: AgentLoop 使用 ContextBuilder 构建消息。

## Impact

- `services/api/src/main/java/com/suilearn/api/agent/{context,loop,runtime}/**`
- `services/api/src/main/java/com/suilearn/api/agent/infrastructure/turn/SessionMessageJpaRepository.java`
- 对应测试。
- 不修改契约与 `apps/**`。

## Non-Goals

- 不实现滚动摘要、摘要水位、反漂移重建（4b）。
- 不实现三层记忆/Consolidator。
- 不引入新配置键。

## Acceptance Criteria

- PromptBlock 输出稳定字节和分块 token 估算。
- 历史超窗时按“旧 tool 消息优先”裁剪并加截断标记。
- AgentLoop 请求的 prompt token 来自真实 LlmUsage；context 报表事件含 estimated/actual。
- 既有 55 个新 runtime 测试保持通过。
