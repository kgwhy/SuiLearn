## ADDED Requirements

### Requirement: L1 审计 trace 必须 append-only 且不含原文
系统 MUST 提供 `memory_trace` append-only 记录，只存 turn_id/surface/kind/摘要 payload，不复制用户正文或原始模型输出。

#### Scenario: 记录回合
- **WHEN** 回合完成
- **THEN** 一条 trace 追加，内容长度有界

### Requirement: L2 必须由 snapshot 驱动
Consolidator MUST 只消费 `memory_snapshot` 新增/变更实体生成 L2；仅追加 L1 trace MUST NOT 触发 L2。

#### Scenario: 空 snapshot
- **WHEN** 只有 trace 无 snapshot
- **THEN** 无 L2 更新命令执行

### Requirement: Consolidator 命令必须幂等且后台执行
`memory_consolidation_command` MUST 有唯一幂等键；重复提交只执行一次；失败不阻塞学习回合。

#### Scenario: 重复命令
- **WHEN** 同一 learner/surface/operationKey 提交两次
- **THEN** 命令表只有一行，处理器只运行一次

### Requirement: L3 必须按 slot 合并
系统 MUST 从该 learner 的 L2 文档生成 recent/profile/scope/preferences 四槽 L3 文档，且结果以 Markdown 存 PostgreSQL。

#### Scenario: 合并
- **WHEN** 手动触发 merge
- **THEN** 生成或更新四个 slot 文档，源引用可追溯
