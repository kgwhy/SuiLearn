# Tasks: 让 search_knowledge 返回模型可用的证据指针内容

Status: Approved

批准者：用户（2026-08-28）

- [x] 1.1 修改 `SearchKnowledgeTool` 的 tool content
  - Owner: Server Backend Agent
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/tool/SearchKnowledgeTool.java`
  - Forbidden: 其他模块
  - Test: `mvn -f services/api/pom.xml -Dtest=AgentDeclarativeToolsTest test`
- [x] 1.2 更新工具测试断言
  - Owner: Test Agent
  - Allowed: `services/api/src/test/java/com/suilearn/api/agent/tool/AgentDeclarativeToolsTest.java`
  - Forbidden: 其他模块
  - Test: `mvn -f services/api/pom.xml -Dtest=AgentDeclarativeToolsTest test`
