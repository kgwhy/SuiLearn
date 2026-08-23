# 三层记忆任务

- Change: `agent-three-layer-memory`
- Owner: Server Backend
- 级别: Major
- 基线引用: `e76c928cb716638441f54068f3496ca58cca21d5`
- 决策记录: `.agents/notes/implemented/architecture/2026-08-23-agent-three-layer-memory.md`

## 待办

- [x] 1.1 创建 change 包与 Agent Note
- [x] 2.1 新增六张表实体与 repository
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/infrastructure/turn/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=MemoryPersistenceEntitiesTest`
- [x] 3.1 实现 trace/snapshot recorder 与 MemoryConsolidator
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/memory/**`, `services/api/src/main/java/com/suilearn/api/agent/llm/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=MemoryConsolidatorTest`
- [x] 4.1 recall_memory 合并 L2/L3 文本召回
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/tool/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=AgentDeclarativeToolsTest`
- [x] 5.1 回归并记录 verification
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=AgentTurnRuntimeTypesTest,AgentTurnOpenApiContractTest,AgentTurnWsContractTest,AgentTurnConfigurationTest,AgentTurnPersistenceModelTest,TurnEventBusTest,TurnRuntimeServiceTest,AgentTurnControllerTest,AgentTurnWebSocketHandlerTest,CapabilityToolRegistryTest,AgentDeclarativeToolsTest,TurnOrchestratorTest,AgentCapabilitiesControllerTest,AgentCapabilitiesOpenApiContractTest,OpenAiCompatibleLlmClientTest,ToolDispatcherTest,AgentLoopTest,TurnRuntimePauseResumeTest,AgentLoopOrchestratorTest,AgentLoopEvalTest,LegacyRetirementScanTest,PromptBlockAssemblerTest,ContextBuilderTest,RollingSessionSummaryTest,MemoryPersistenceEntitiesTest,MemoryConsolidatorTest`
- [x] 6.1 单人自审与归档
