# Agent ContextBuilder 任务

- Change: `agent-context-builder`
- Owner: Server Backend
- 级别: Major
- 基线引用: `3376a6f6e106cb2894e1c4b055c449d8f355802f`
- 决策记录: `.agents/notes/implemented/architecture/2026-08-23-agent-context-builder.md`

## 待办

- [x] 1.1 创建 change 包与 Agent Note
- [x] 2.1 实现 PromptBlock/PromptBlockAssembler
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/context/**`, `services/api/src/test/java/com/suilearn/api/agent/context/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=PromptBlockAssemblerTest`
- [x] 3.1 实现 ContextBuilder 与历史查询
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/context/**`, `services/api/src/main/java/com/suilearn/api/agent/infrastructure/turn/SessionMessageJpaRepository.java`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=ContextBuilderTest`
- [x] 4.1 AgentLoop 接入 ContextBuilder 与预算报表
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/loop/**`, `services/api/src/test/java/com/suilearn/api/agent/loop/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=AgentLoopTest,AgentLoopEvalTest`
- [x] 5.1 运行回归并记录 verification
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=AgentTurnRuntimeTypesTest,AgentTurnOpenApiContractTest,AgentTurnWsContractTest,AgentTurnConfigurationTest,AgentTurnPersistenceModelTest,TurnEventBusTest,TurnRuntimeServiceTest,AgentTurnControllerTest,AgentTurnWebSocketHandlerTest,CapabilityToolRegistryTest,AgentDeclarativeToolsTest,TurnOrchestratorTest,AgentCapabilitiesControllerTest,AgentCapabilitiesOpenApiContractTest,OpenAiCompatibleLlmClientTest,ToolDispatcherTest,AgentLoopTest,TurnRuntimePauseResumeTest,AgentLoopOrchestratorTest,AgentLoopEvalTest,LegacyRetirementScanTest,PromptBlockAssemblerTest,ContextBuilderTest`
- [x] 6.1 单人自审与归档
