# Agent ContextBuilder 设计（change-4a）

## Context

基线 `3376a6f`。旧 ContextManager 仍存在但新 loop 未使用。4a 建立新 loop 专用 ContextBuilder，避免依赖旧 ContextAssembler。

## Decisions

### 1. PromptBlock 分块

`PromptBlockAssembler` 固定顺序：general/policy/capability/memory/tools/skills。capability 与 tools 来自 manifest；memory 本轮只放安全空块，4b 接真实召回。

### 2. 窗口守卫

- `historyBudget = contextMaxTokens * 0.35`。
- 历史消息从 `session_message` 按 session 倒序取最近 20 条，逆序后交给 builder。
- 超预算时优先删除旧 tool 消息，保留 user/assistant；仍超则从最旧消息删除。
- 首个被裁位置插入 user 截断标记 `[older messages truncated]`。

### 3. 真实请求预算

AgentLoop 每轮累计 `LlmUsage.promptTokens`；context 报表 metadata 同时含 `estimatedContextTokens` 与 `actualPromptTokens`。估算用现有 TokenEstimator。

### 4. 摘要水位延后

`summary_up_to_msg_id`、反漂移重建与滚动摘要写入依赖 4b 的 session_summary/memory 表，本轮不落库。

## Risks / Trade-offs

- 没有滚动摘要时，长会话只靠窗口裁剪；4b 前长会话可能丢失早期信息，属计划内过渡。
- 旧 ContextManager 暂留；4b 或 Phase4 完成后删除。
