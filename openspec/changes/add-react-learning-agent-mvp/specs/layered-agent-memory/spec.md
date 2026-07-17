## ADDED Requirements

### Requirement: Agent 必须提供职责分离的三层记忆
系统 MUST 将当前执行状态保存于 working memory，将近期会话摘要保存于 Redis session memory，将经晋升的学习事实保存于 PostgreSQL/pgvector semantic memory；三层存储不得互相伪装为成功替代。

#### Scenario: 同一会话连续提问
- **WHEN** 相同 learnerId 和 sessionId 在 TTL 内发起后续请求
- **THEN** ContextManager 可读取 Redis 中的近期摘要和未完成目标，而不依赖上一请求的进程内状态

#### Scenario: 新会话召回长期事实
- **WHEN** 相同 learnerId 开始新 session 并提出与既有学习目标相关的问题
- **THEN** 系统从 PostgreSQL/pgvector 召回满足过滤和相似度策略的长期记忆，而不读取其他 learner 的记忆

### Requirement: Session memory 必须有界且自动过期
Redis session memory MUST 使用受控 key、滑动 TTL 和最大 turn 数；不得把原始用户内容直接拼入 key，也不得无限保留完整 transcript。

#### Scenario: 会话持续使用
- **WHEN** 同一 session 在 TTL 内成功完成一轮交互
- **THEN** 系统更新受控摘要、裁剪超过最大 turn 的旧内容并刷新 TTL

#### Scenario: 会话过期
- **WHEN** Redis key 超过配置 TTL 未被访问
- **THEN** 会话记忆自动删除，后续请求只能从当前输入和长期记忆重新构建上下文

### Requirement: 长期记忆必须经过确定性晋升策略
系统 MUST 只允许 `GOAL`、`PREFERENCE`、`WEAKNESS`、`MASTERY` 类型进入长期记忆，并 MUST 校验置信度、来源、长度、learner scope 和内容指纹；模型不得直接写数据库。

#### Scenario: 高置信度薄弱点被晋升
- **WHEN** memory extraction 返回带有效来源且置信度不低于阈值的 WEAKNESS 候选
- **THEN** MemoryPromotionPolicy 对其校验和去重后 upsert 长期记忆，并记录 sourceRunId/sourceRef

#### Scenario: 指令注入文本要求永久记住
- **WHEN** 资料或用户输入包含要求覆盖系统规则或永久保存秘密的指令样文本
- **THEN** 该文本被视为不可信内容且不得绕过允许类型、来源和置信度策略进入长期记忆

### Requirement: 长期记忆检索必须隔离且可追溯
semantic memory 查询 MUST 先按 learnerId 和允许类型过滤，再进行 Top-K 相似度排序；每个结果 MUST 带稳定 ID、分数、来源和更新时间。

#### Scenario: 两个 learner 有相似目标
- **WHEN** learner A 与 learner B 存在语义相似的学习目标
- **THEN** learner A 的召回结果中不包含 learner B 的任何长期记忆

#### Scenario: embedding 不可用
- **WHEN** semantic recall 无法取得 embedding
- **THEN** 系统标记 `LONG_TERM_MEMORY_DEGRADED`，不得返回未经匹配的任意长期记忆作为语义结果

### Requirement: 重复和冲突记忆必须受控
系统 MUST 使用规范化内容指纹阻止完全重复，并 MUST 对同 learner、同类型、同主题的冲突候选保留来源和更新时间，不得无审计地覆盖高置信度事实。

#### Scenario: 相同目标被重复提取
- **WHEN** 多次运行产生规范化后相同的 GOAL 候选
- **THEN** 系统更新已有记录的来源/时间或保持不变，而不是创建重复行

#### Scenario: 掌握状态发生变化
- **WHEN** 新 MASTERY 候选与旧状态冲突且有更高置信度和更新来源
- **THEN** 系统按确定性策略更新并保留可追溯来源，不得同时把两者无区分注入上下文

### Requirement: 用户必须能够删除 Agent 记忆
系统 SHALL 提供按 learnerId 删除 session 与 semantic memory 的 API，并 MUST 返回各层删除结果；删除不得影响知识库、资料、正式题目或学习记录。

#### Scenario: 删除全部 Agent 记忆
- **WHEN** 调用方请求删除指定 learner 的 Agent 记忆
- **THEN** 该 learner 的 Redis session keys 和 PostgreSQL semantic memories 被删除，其他 learner 及非 Agent 数据保持不变

### Requirement: 记忆写入失败不得被报告为成功
系统 MUST 在响应中区分 session memory、semantic recall 和 semantic persistence 状态；任一层失败不得声称 Agent 已记住该信息。

#### Scenario: 回答成功但长期记忆写入失败
- **WHEN** Agent 已生成有效回答而 PostgreSQL memory upsert 失败
- **THEN** 回答可返回但 memoryStatus 标记 `PERSIST_FAILED`，并记录低基数失败指标
