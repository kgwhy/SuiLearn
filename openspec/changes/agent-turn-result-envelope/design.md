# TurnResult 信封设计（change-6b）

## Decisions

- REST `TurnResultResponse` 在保留 terminalEvent 语义的前提下增加五个扁平汇总字段：
  `promptTokens`、`completionTokens`、`usageCostUsd`、`actionTraceCount`、`estimatedContextTokens`。
- usage/action/context 汇总来自本回合**最后一个 `RESULT` 事件**的 metadata；没有 RESULT 或字段缺失时返回 0。
  不把汇总塞进终态事件，避免改变 `done/failed/cancelled` 的终态事件语义。
- OpenAPI `AgentTurnResult` 将五个汇总字段设为 required，并用 integer/double + minimum 约束锁定稳定可解析。
- Web 不消费旧 Agent 路径，not affected；Android 由用户明确延后，不修改 `apps/**`。

## Alternatives considered

- 在 DONE 终态事件 metadata 复制 usage：会污染终态事件语义，且失败/取消路径仍缺统一来源，否决。
- 把 usage 汇总上移到 `TurnResult` 领域 record：会扩大 change 到 runtime/loop 文件，超出 6b 后端信封边界，否决。

## Data flow

```text
AgentLoop publishes RESULT(usage metadata) -> persisted turn_events
TurnRuntimeService.awaitResult returns TurnResult(terminalEvent)
Controller reads eventsAfter(turnId, 0) -> latest RESULT metadata
-> TurnResultResponse flat usage fields (0 when absent)
```
