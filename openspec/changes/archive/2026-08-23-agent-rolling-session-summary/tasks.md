# 滚动会话摘要任务

- Change: `agent-rolling-session-summary`
- Owner: Server Backend
- 级别: Major
- 基线引用: `faf4241abe242c7ac6471350c92b3002239f4a39`
- 决策记录: `.agents/notes/implemented/architecture/2026-08-23-agent-rolling-session-summary.md`

## 待办

- [x] 1.1 创建 change 包与 Agent Note
- [x] 2.1 新增 session_summary entity/repo 与 RollingSessionSummary
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/infrastructure/turn/**`, `services/api/src/main/java/com/suilearn/api/agent/context/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=RollingSessionSummaryTest`
- [x] 3.1 ContextBuilder 注入摘要并接入 AgentLoop/config
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/context/**`, `services/api/src/main/java/com/suilearn/api/agent/loop/**`, `services/api/src/main/java/com/suilearn/api/agent/runtime/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=ContextBuilderTest,AgentLoopTest`
- [x] 4.1 回归并记录 verification
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=AgentTurnRuntimeTypesTest,AgentTurnOpenApiContractTest,AgentTurnWsContractTest,AgentTurnConfigurationTest,AgentTurnPersistenceModelTest,TurnEventBusTest,TurnRuntimeServiceTest,AgentTurnControllerTest,AgentTurnWebSocketHandlerTest,CapabilityToolRegistryTest,AgentDeclarativeToolsTest,TurnOrchestratorTest,AgentCapabilitiesControllerTest,AgentCapabilitiesOpenApiContractTest,OpenAiCompatibleLlmClientTest,ToolDispatcherTest,AgentLoopTest,TurnRuntimePauseResumeTest,AgentLoopOrchestratorTest,AgentLoopEvalTest,LegacyRetirementScanTest,PromptBlockAssemblerTest,ContextBuilderTest,RollingSessionSummaryTest`
- [x] 5.1 单人自审与归档
