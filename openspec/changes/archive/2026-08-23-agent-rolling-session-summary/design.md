# 滚动摘要设计（4b）

## Decisions

- 摘要表由 Hibernate ddl-auto 创建，沿用现有模式。
- 窗口=session.maxTurns；半窗口=window/2。
- 摘要 LLM 使用 LlmClient；失败返回旧摘要。
- ContextBuilder memory block 注入摘要。

## Non-Goals

- L1/L2/L3 memory、snapshot、consolidator -> Phase 5。
