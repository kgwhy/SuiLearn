# 滚动会话摘要与水位（change-4b）

## Why

4a 的 ContextBuilder 只有窗口裁剪，长会话会丢早期信息。4b 落地 PostgreSQL 会话摘要、summary watermark 与反漂移重建；三层记忆/Consolidator 属于 Phase 5，另行变更。

## What Changes

- 新增 `session_summary` JPA 表：session_id 主键、summary、summary_up_to_message_id、summary_up_to_created_at。
- 新增 `RollingSessionSummary`：LLM 摘要只推进水位；原始消息 ≤ 半窗口时从原文重建；失败不阻塞回合。
- ContextBuilder 将当前摘要作为 memory block 注入；AgentLoop 在回合开始时确保摘要。
- 不删除 Redis；Phase 5 再处理旧记忆退役。

## Acceptance Criteria

- 水位只在摘要成功后推进；重复 ensure 幂等。
- 半窗口反漂移重建有测试。
- AgentLoop 在 fake LLM 下仍全绿；Docker 完整回归除 Testcontainers socket 外全绿。
- 新表仅 session_summary，不建 memory 表。
