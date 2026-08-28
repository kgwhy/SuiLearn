# Tasks: 修复 Agent read_evidence 找不到可读证据

Status: Approved

批准者：用户（2026-08-28）

- [x] 1.1 修改 `RetrievalEvidenceTools` 支持按 chunkId 直接读取
  - Owner: Server Backend Agent
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/tool/RetrievalEvidenceTools.java`
  - Forbidden: 其他模块
  - Test: `mvn -f services/api/pom.xml -Dtest=RetrievalEvidenceToolsTest test`
- [x] 1.2 注入 `MaterialChunkStore`
  - Owner: Server Backend Agent
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/config/AgentInfrastructureConfiguration.java`
  - Forbidden: 其他模块
  - Test: `mvn -f services/api/pom.xml test -q`
- [x] 1.3 新增 `RetrievalEvidenceToolsTest`
  - Owner: Test Agent
  - Allowed: `services/api/src/test/java/com/suilearn/api/agent/tool/RetrievalEvidenceToolsTest.java`
  - Forbidden: 其他模块
  - Test: `mvn -f services/api/pom.xml -Dtest=RetrievalEvidenceToolsTest test`
