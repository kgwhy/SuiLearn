# Agent-Native Loop 任务

- Change: `agent-native-loop`
- Owner: Leader 协调；Server Backend 拥有 `services/api/**`，Test 独立验证，Reviewer 单人自审
- 级别: Major
- 基线引用: `e801f849cb464d7f4498616d89d19baabc5fbad1`
- 执行模式: serial（L3）
- 决策记录: `.agents/notes/implemented/architecture/2026-08-23-agent-native-loop.md`

## 待办

- [x] 1.1 创建 change 包与 proposed Agent Note
  - Owner: Leader
  - Allowed: `openspec/changes/agent-native-loop/**`, `.agents/notes/implemented/architecture/2026-08-23-agent-native-loop.md`
  - Test: `python3 scripts/check_agent_notes.py`
  - Review focus: 3a/3b 拆分理由、Major 产物完整
- [x] 2.1 实现 LlmClient 端口与 OpenAI-compatible streaming adapter
  - Owner: Server Backend
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/llm/**`, `services/api/src/test/java/com/suilearn/api/agent/llm/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=OpenAiCompatibleLlmClientTest`
  - Review focus: SSE delta 合并、usage、非 2xx、不依赖 Spring AI
- [x] 3.1 实现 ToolDispatcher
  - Owner: Server Backend
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/loop/**`, `services/api/src/main/java/com/suilearn/api/agent/runtime/ToolRegistry.java`, `services/api/src/test/java/com/suilearn/api/agent/loop/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=ToolDispatcherTest`
  - Review focus: 并行≤8、去重、缺参、权限
- [x] 4.1 实现 AgentLoop 与受控 prompt 资源
  - Owner: Server Backend
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/loop/**`, `services/api/src/main/resources/agents/agent-loop/**`, `services/api/src/test/java/com/suilearn/api/agent/loop/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=AgentLoopTest`
  - Review focus: 工具回填、空回答 nudge、预算、终态唯一
- [x] 5.1 实现暂停恢复并接入 TurnRuntimeService/Orchestrator
  - Owner: Server Backend
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/runtime/**`, `services/api/src/test/java/com/suilearn/api/agent/runtime/**`, `services/api/src/main/java/com/suilearn/api/agent/controller/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=TurnRuntimePauseResumeTest,AgentLoopOrchestratorTest`
  - Review focus: WAITING_INPUT/RUNNING 状态、终态/取消释放等待者
- [x] 6.1 新增 offline fixed Eval 并运行回归
  - Owner: Test
  - Allowed: `services/api/src/test/java/com/suilearn/api/agent/loop/**`, `openspec/changes/agent-native-loop/verification.md`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=AgentLoopEvalTest,AgentTurnRuntimeTypesTest,AgentTurnOpenApiContractTest,AgentTurnWsContractTest,AgentTurnConfigurationTest,AgentTurnPersistenceModelTest,TurnEventBusTest,TurnRuntimeServiceTest,AgentTurnControllerTest,AgentTurnWebSocketHandlerTest,CapabilityToolRegistryTest,AgentDeclarativeToolsTest,TurnOrchestratorTest,AgentCapabilitiesControllerTest,AgentCapabilitiesOpenApiContractTest`
  - Review focus: 离线确定性、38 回归 + 新测试
- [x] 7.1 单人自审与归档准备
  - Owner: Reviewer
  - Allowed: `openspec/changes/agent-native-loop/**`, `.agents/notes/implemented/architecture/2026-08-23-agent-native-loop.md`
  - Test: `python3 scripts/check_suilearn_workflow.py --closing-change agent-native-loop`
  - Review focus: review_mode single-agent、P0/P1/P2、3b follow-up 命名
