## ADDED Requirements

### Requirement: 三个内置能力必须全部可执行
系统 MUST 对 `study_agent`、`rag_qa`、`question_generation` 路由到 AgentLoop，并按 manifest 限制工具面。

#### Scenario: rag_qa
- **WHEN** capability=rag_qa 启动回合
- **THEN** loop 只允许 search_knowledge/read_evidence，且不再发布 TURN_EXECUTOR_UNAVAILABLE

#### Scenario: question_generation
- **WHEN** capability=question_generation 启动回合
- **THEN** loop 只允许 generate_practice/ask_user，且不再发布 TURN_EXECUTOR_UNAVAILABLE

### Requirement: 回合终态后记忆记录必须幂等且失败不阻断
系统 MUST 在 AgentLoop 终态后记录 L1 trace、turn snapshot 并提交 consolidation command；重复 turn 不重复提交。

#### Scenario: 成功回合
- **WHEN** 回合 COMPLETED
- **THEN** 一条 trace、一条 turn snapshot、一条 command 可见

#### Scenario: 记忆失败
- **WHEN** recorder 抛异常
- **THEN** 回合仍返回原终态，异常仅记录日志

### Requirement: 生产检索必须经过 RagPipeline
系统 MUST 让 RagService 与 SearchService 通过 RagPipeline 调用检索；默认 pipeline 为 pgvector-hybrid。

#### Scenario: pipeline 路径
- **WHEN** RagService/SearchService 执行检索
- **THEN** 请求经 RagPipeline 到达默认 pgvector-hybrid 包装的 Retriever

### Requirement: embedding 成功必须记录 ready index version
系统 MUST 在资料 embedding 成功后按签名记录 ready 版本；同签名不重复建版本，换签名返回 needs_reindex。

#### Scenario: 版本幂等
- **WHEN** 同一 KB 同签名第二次成功 embedding
- **THEN** 不新增 index_versions 行，status 仍为 current

#### Scenario: 换签名
- **WHEN** embedding model 或 baseUrl/dim 变化
- **THEN** 新签名 status=needs_reindex，成功重建后 markReady
