# Agent Note: 统一 TurnResult 只扩后端信封，Android 客户端延后
Status: implemented

## Problem

REST TurnResult 缺少 usage/cost/context/action 统计，6a 的 usage 只存在 RESULT 事件 metadata。

## Decision

- `AgentTurnResult` 增加 promptTokens、completionTokens、usageCostUsd、actionTraceCount、estimatedContextTokens 五个必填字段。
- Controller 从本回合最后一个 RESULT 事件 metadata 取汇总，缺省 0；不改变终态事件语义。
- Web not affected；Android 由用户明确延后。

## Alternatives considered

- **全量扩展 practice/memory/citations**：否决，Phase 7 结构尚未稳定。
- **把 usage 复制到 DONE 终态事件**：否决，污染终态事件且失败/取消路径缺统一来源。
- **Android 同步实现**：用户明确延后。

## Consequences

- 7 个定向测试全绿；OpenAPI 与 Controller 映射同时锁定。
- 后续 Android 客户端需按同一 schema 实现；改造计划整体当前事实收口另有 follow-up。
