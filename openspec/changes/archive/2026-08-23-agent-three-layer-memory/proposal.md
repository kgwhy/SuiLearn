# 三层记忆与合并任务（change-4c）

## Why

4b 已交付会话摘要。Phase 5 的三层记忆仍缺失：L1 审计 trace、领域 snapshot、L2/L3 Markdown 文档、Consolidator 与幂等命令。change-5 有自己的范围，因此 Phase 4/5 剩余记忆需求在本 change 完成。

## What Changes

- 新增 PostgreSQL 表：`memory_trace`、`memory_l2_doc`、`memory_l3_doc`、`memory_meta`、`memory_snapshot`、`memory_consolidation_command`。
- 新增 `MemoryTraceRecorder`：append-only L1 审计事件摘要，不存原文。
- 新增 `MemorySnapshotRecorder`：领域实体指纹与增量命令。
- 新增 `MemoryConsolidator`：单实例 `@Scheduled` 消费命令，用独立 LlmClient 预算生成 L2；L3 由 L2 按 slot 合并。
- `recall_memory` 工具扩展：会话摘要 + L2/L3 文本召回（语义向量仍走旧 recall，Phase 6 统一）。
- 不读旧 `AgentSemanticMemory` 或 Redis 摘要路径；旧数据不迁移。

## Capabilities

### New

- `agent-three-layer-memory`: L1/L2/L3、snapshot、consolidation command 与幂等调度。

### Modified

- `agent-capability-registry`: recall_memory 使用 L2/L3 全文 + 旧语义召回合并。
- `agent-context-builder`: L3 摘要可进入 memory block。

## Impact

- `services/api/src/main/java/com/suilearn/api/agent/{memory,infrastructure/turn,runtime,tool}/**`
- 对应测试。
- 不修改 `contracts/**`、`apps/**`。

## Acceptance Criteria

- 六张新表由 JPA 创建，无 Flyway。
- L1 只追加；snapshot 变更才触发 L2 update 命令。
- 重复命令幂等，不重复执行。
- Consolidator 失败不阻塞学习回合。
- 60 个既有测试保持通过，新增 4c 测试通过。
