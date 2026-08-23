# Agent Note: ContextBuilder 先做窗口与真实预算，摘要水位随 4b 记忆表落地
Status: implemented

## Problem

change-3 的 AgentLoop 没有会话历史窗口、分块 prompt 或真实请求预算。Phase 4 摘要/水位依赖尚未建立的记忆表。

## Decision

- 4a 已实现 PromptBlock、ContextBuilder 窗口守卫、session_message 历史加载、真实 usage 报表。
- 4b 再实现滚动摘要、summary_up_to_msg_id、反漂移与三层记忆。

## Alternatives considered

- **一次完成 Phase 4/5**：否决，记忆 schema 和 Consolidator 范围过大。
- **继续使用旧 ContextManager**：否决，其输入模型与 TurnContext 不匹配。
- **在 4a 就新增 session_summary 表**：否决，避免半套记忆 schema。

## Consequences

- 57 个定向测试全绿；Docker 完整回归 365 tests 0 errors。
- 长会话在 4b 前依赖窗口裁剪，可能丢早期信息；计划内过渡。
