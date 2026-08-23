# 统一 TurnResult 信封（change-6b）

## Why

6a 已采集 usage/cost，但 REST TurnResult 仍只有 terminalEvent。6b 扩展信封，Web 当前无旧 Agent 调用故 not affected，Android 用户已明确延后。

## What Changes

- `AgentTurnResult` OpenAPI 增加 promptTokens、completionTokens、usageCostUsd、actionTraceCount、estimatedContextTokens 五个必填字段。
- Controller/DTO 从本回合最后一个 RESULT 事件的 metadata 取 usage/cost/action/context 汇总，缺省 0；terminalEvent 仍原样返回终态事件。
- 不修改 Web；Android 延后。

## Acceptance Criteria

- 信封字段稳定可解析；71+ 回归通过。
