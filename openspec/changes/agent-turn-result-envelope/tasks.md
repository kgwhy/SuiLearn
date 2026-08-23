# TurnResult 信封任务

- Change: `agent-turn-result-envelope`
- Owner: Server Backend
- 级别: Major
- 基线引用: `d5554b6c7f0f9c2c8b263aa4fe0c8f878d7b4c6d`
- 决策记录: `.agents/notes/proposed/architecture/2026-08-23-agent-turn-result-envelope.md`

## 待办

- [x] 1.1 创建 change 包与 Agent Note
- [x] 2.1 扩展 DTO/Controller/OpenAPI
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/controller/**`, `contracts/openapi/suilearn-v2.yaml`, `services/api/src/test/java/com/suilearn/api/agent/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=AgentTurnControllerTest,AgentTurnOpenApiContractTest`
- [x] 3.1 回归、验证与归档
