# Agent Note: 三层记忆由 snapshot 驱动，命令表幂等调度
Status: implemented

## Problem

Phase 4 剩余需求要求 L1 审计、领域 snapshot、L2/L3 文档与 Consolidator。现有语义记忆无法提供审计和分面文档。

## Decision

- 六表落地 L1/L2/L3/meta/snapshot/command。
- L2 只消费 snapshot，不读 L1。
- Consolidator 用 PostgreSQL 命令表 + @Scheduled，幂等键唯一。
- recall_memory 合并 L2/L3 文本与旧语义召回，Phase 6 统一向量索引。

## Alternatives considered

- **用 RabbitMQ 调度合并**：否决，计划明确 PostgreSQL 命令表。
- **L2 读 L1 trace**：否决，L1 是审计不是内容输入。
- **一次删除旧记忆路径**：否决，Phase 6 与 RAG 索引一起退役。

## Consequences

- 64 定向测试全绿；Docker 完整回归 372 tests 0 errors。
- 旧语义与新 L2/L3 并存；Phase 6 必须收口。
