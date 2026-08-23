# RAG Pipeline/Index 任务

- Change: `rag-pipeline-index-versioning`
- Owner: Server Backend
- 级别: Major
- 基线引用: `87b9493c18f591297186e31d8ce2a7a632ad900a`
- 决策记录: `.agents/notes/implemented/architecture/2026-08-23-rag-pipeline-index-versioning.md`

## 待办

- [x] 1.1 创建 change 包与 Agent Note
- [x] 2.1 实现 RagPipeline/PipelineFactory/PgvectorHybridRagPipeline
  - Allowed: `services/api/src/main/java/com/suilearn/api/rag/**`, `services/api/src/test/java/com/suilearn/api/rag/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=RagPipelineFactoryTest`
- [x] 3.1 实现 EmbeddingSignature 与 IndexVersionManager
  - Allowed: `services/api/src/main/java/com/suilearn/api/rag/**`, `services/api/src/test/java/com/suilearn/api/rag/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=EmbeddingSignatureTest`
- [x] 4.1 实现 ParseEngineRegistry 与 text 引擎
  - Allowed: `services/api/src/main/java/com/suilearn/api/rag/**`, `services/api/src/test/java/com/suilearn/api/rag/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=ParseEngineRegistryTest`
- [x] 5.1 回归并记录 verification
  - Test: 既有 64 测试 + 新增
- [x] 6.1 单人自审与归档
