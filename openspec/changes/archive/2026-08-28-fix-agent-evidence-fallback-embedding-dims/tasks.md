# Tasks: read_evidence 容错增强与 embedding dimensions 回填

Status: Approved

批准者：用户（2026-08-28）

- [x] 1.1 增强 `RetrievalEvidenceTools.read()` 容错与 excerpt 回退
  - Owner: Server Backend Agent
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/tool/RetrievalEvidenceTools.java`
  - Forbidden: 其他模块
  - Test: `mvn -f services/api/pom.xml -Dtest=RetrievalEvidenceToolsTest test`
- [x] 1.2 更新 `RetrievalEvidenceToolsTest`
  - Owner: Test Agent
  - Allowed: `services/api/src/test/java/com/suilearn/api/agent/tool/RetrievalEvidenceToolsTest.java`
  - Forbidden: 其他模块
  - Test: `mvn -f services/api/pom.xml -Dtest=RetrievalEvidenceToolsTest test`
- [x] 1.3 让 embedding dimensions 返回真实维度
  - Owner: Server Backend Agent
  - Allowed: `services/api/src/main/java/com/suilearn/api/retrieval/OpenAiCompatibleEmbeddingProvider.java`
  - Forbidden: 其他模块
  - Test: `mvn -f services/api/pom.xml -Dtest=AiProviderStatusServiceTest test`
