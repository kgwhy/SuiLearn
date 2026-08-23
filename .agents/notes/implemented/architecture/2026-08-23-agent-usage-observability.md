# Agent Note: UsageTracker 先做聚合与成本，信封扩展随 6b
Status: implemented

## Problem

usage 只有数字，无成本折算；日志边界需白名单。

## Decision

- UsageTracker 价格表配置化，默认价兜底。
- AgentLoop 输出 usage cost 元数据。
- 客户端切换与 TurnResult 扩展放 6b。

## Alternatives considered

- **一次扩展 OpenAPI TurnResult**：否决，需要 Web/Android 契约同步。
- **价格表硬编码单一模型**：否决，计划要求可覆盖。

## Consequences

- UsageTracker/AgentLoop 测试全绿。
- 6b 需完成信封与客户端切换。
