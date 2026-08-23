# 三层记忆设计（4c）

## Decisions

- 六表实体 + repository；沿用 Hibernate ddl-auto。
- Consolidator 用 PostgreSQL 命令表 + `@Scheduled`，不接 RabbitMQ。
- L2 输入来自 snapshot 实体内容，不读 L1 trace。
- L3 按 slot 从 L2 文档合并。
- 命令幂等键 `(learnerId, surface, operationKey)` 唯一。

## Non-Goals

- 不迁移旧 Redis/pgvector 记忆；不删除旧 MemoryManager（Phase 6 再退役）。
- 不做 pgvector 索引版本。
