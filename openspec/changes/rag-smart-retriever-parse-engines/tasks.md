# SmartRetriever/ParseEngine 任务

- Change: `rag-smart-retriever-parse-engines`
- Owner: Server Backend
- 级别: Major
- 基线引用: `5936b5ca9ae7ee76f2e96bdd88ff39df25c2e195`
- 决策记录: `.agents/notes/proposed/architecture/2026-08-23-rag-smart-retriever-parse-engines.md`

## 待办

- [x] 1.1 创建 change 包与 Agent Note
- [x] 2.1 实现 SmartRetriever
  - Allowed: `services/api/src/main/java/com/suilearn/api/rag/**`, `services/api/src/test/java/com/suilearn/api/rag/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=SmartRetrieverTest`
- [x] 3.1 实现 PDF/Office/OCR ParseEngine 并注册
  - Allowed: `services/api/src/main/java/com/suilearn/api/rag/**`, `services/api/src/test/java/com/suilearn/api/rag/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=ComplexParseEngineTest`
- [x] 4.1 回归、验证与归档
  - Test: 既有 68 + 新增
