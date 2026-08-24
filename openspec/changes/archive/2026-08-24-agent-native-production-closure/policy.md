# Agent-Native 生产收口策略

Status: Approved
批准者: 用户
批准日期: 2026-08-24
批准依据: 用户指令“启动这些收口”。

- Change: `agent-native-production-closure`
- 级别: Major
- base_ref: `120b382`
- 执行模式: L3（单人执行）
- 决策记录: `.agents/notes/proposed/architecture/2026-08-24-agent-native-production-closure.md`

## 允许修改文件

- `openspec/changes/agent-native-production-closure/**`
- `.agents/notes/**`
- `services/api/src/main/java/com/suilearn/api/agent/{runtime,loop,context,tool,memory,config}/**`
- `services/api/src/main/resources/agents/agent-loop/v1/**`
- `services/api/src/main/java/com/suilearn/api/rag/**`
- `services/api/src/main/java/com/suilearn/api/search/application/SearchService.java`
- `services/api/src/main/java/com/suilearn/api/material/application/MaterialImportService.java`
- `services/api/src/main/java/com/suilearn/api/retrieval/EmbeddingProvider.java`
- 上述路径对应测试 `services/api/src/test/java/com/suilearn/api/{agent,rag,material,retrieval,search}/**`
- 最终事实同步：`docs/product-requirements.md`、`docs/architecture.md`、`docs/tech-selection.md`

## 禁止修改文件

- `apps/**`（Android 继续延后；Web not affected）
- `contracts/**`
- `services/api/pom.xml`
- 其他后端模块与测试
- `docs/proposals/**`

## 验收矩阵

| 场景 | 默认值/覆盖语义 | 必需验证 |
|---|---|---|
| rag_qa 循环 | 仅 search_knowledge/read_evidence 工具面；不再 unavailable | AgentLoopOrchestratorTest/PromptBlockAssemblerTest |
| question_generation 循环 | 仅 generate_practice/ask_user 工具面；不再 unavailable | AgentLoopOrchestratorTest/PromptBlockAssemblerTest |
| 记忆在线 | loop 终态后记录 trace/snapshot/command；失败不阻断回合 | MemoryTurnRecorderTest/AgentLoopOrchestratorTest |
| recall_memory | 在线 bean 注入 L2/L3 repo，文本并入 metadata | AgentDeclarativeToolsTest |
| RagPipeline | RagService/SearchService 使用 pipeline，缺 bean 时回退包装 Retriever | RagPipelineFactoryTest/RagServiceTest/SearchServiceTest |
| index_versions | embedding 成功记录 ready 版本；同签名幂等；换签名 needs_reindex | EmbeddingIndexVersionRecorderTest |
| ParseEngineRegistry | Spring bean 可路由 text/pdf/doc/docx/ocr | ParseEngineRegistryTest |
| 文件边界 | 不修改 apps/contracts/pom | change_scope |
