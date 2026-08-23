# Agent Note: 会话摘要落 PostgreSQL，水位成功才推进
Status: implemented

## Problem

4a 只有窗口裁剪，长会话丢早期上下文。计划要求 PostgreSQL 会话摘要与 summary watermark。

## Decision

- session_summary 表保存摘要与水位；只有 LLM 成功才推进。
- 半窗口内从原文重建，避免摘要套摘要。
- 摘要失败返回旧摘要，不阻塞回合。
- 三层记忆留给 4c。

## Alternatives considered

- **继续使用 Redis 会话摘要**：否决，计划明确 PostgreSQL 是事件与记忆真相源。
- **每次回合都重摘要全部原文**：否决，成本高且无水位。
- **与三层记忆一次交付**：否决，范围过大。

## Consequences

- 60 定向测试全绿；Docker 完整回归 368 tests 0 errors。
