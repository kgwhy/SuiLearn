# Phase 8 任务

- Change: `phase8-agent-auth-learner-profile`
- Owner: Leader 协调；Architect 拥有契约，Server Backend 拥有服务，Test 独立验证
- 级别: Major
- 基线引用: `d6c7a3b`

## 待办

- [x] 1.1 创建 change 包与 Agent Note
  - Allowed: `openspec/changes/phase8-agent-auth-learner-profile/**`, `.agents/notes/**`
  - Test: `python3 scripts/check_agent_notes.py`
- [x] 2.1 引入 Spring Security 与 token registry
  - Allowed: `services/api/pom.xml`, `services/api/src/main/resources/application.properties`, `services/api/config/local.properties.example`, `.env.example`, `services/api/src/main/java/com/suilearn/api/security/**`, 对应测试
  - Test: `mvn -B -f services/api/pom.xml test -Dtest=LearnerTokenRegistryTest,AgentSecurityConfigurationTest`
- [x] 3.1 learner 隔离与 REST/WS 接入
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/{controller,runtime}/**`, `services/api/src/main/java/com/suilearn/api/security/**`, 对应测试
  - Test: `mvn -B -f services/api/pom.xml test -Dtest=AgentTurnControllerTest,AgentTurnWebSocketHandlerTest,TurnRuntimeServiceTest,AgentSecurityControllerTest`
- [x] 4.1 learner profile 持久化、API 与 Prompt 注入
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/{learner,context,loop,config,runtime}/**`, `contracts/openapi/suilearn-v2.yaml`, 对应测试
  - Test: `mvn -B -f services/api/pom.xml test -Dtest=AgentLearnerProfileTest,PromptBlockAssemblerTest,ContextBuilderTest,AgentLoopTest`
- [x] 5.1 事实同步、全量回归与归档
  - Test: `mvn -B -f services/api/pom.xml test`；workflow checks
