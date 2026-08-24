# 生产收口任务

- Change: `agent-native-production-closure`
- Owner: Leader 协调；Server Backend 拥有 `services/api/**`，Test 独立验证，Reviewer 单人自审
- 级别: Major
- 基线引用: `120b382`

## 待办

- [x] 1.1 创建 change 包与 proposed Agent Note
  - Allowed: `openspec/changes/agent-native-production-closure/**`, `.agents/notes/**`
  - Test: `python3 scripts/check_agent_notes.py`
- [x] 2.1 三个内置能力全部路由 AgentLoop 并按 capability 切换 prompt
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/{runtime,context}/**`, `services/api/src/main/resources/agents/agent-loop/v1/**`, 对应测试
  - Test: `mvn -f services/api/pom.xml test -Dtest=AgentLoopOrchestratorTest,PromptBlockAssemblerTest`
- [x] 3.1 实现 MemoryTurnRecorder 并挂接 TurnOrchestrator 与 RecallMemoryTool
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/{memory,runtime,tool,config}/**`, 对应测试
  - Test: `mvn -f services/api/pom.xml test -Dtest=MemoryTurnRecorderTest,AgentLoopOrchestratorTest,AgentDeclarativeToolsTest`
- [x] 4.1 RAG pipeline 生产装配与 RagService/SearchService 切换
  - Allowed: `services/api/src/main/java/com/suilearn/api/rag/**`, `services/api/src/main/java/com/suilearn/api/search/application/SearchService.java`, 对应测试
  - Test: `mvn -f services/api/pom.xml test -Dtest=RagPipelineFactoryTest,RagServiceTest,SearchServiceTest`
- [x] 5.1 embedding 成功后记录 index_versions
  - Allowed: `services/api/src/main/java/com/suilearn/api/{rag/index,material/application,retrieval}/**`, 对应测试
  - Test: `mvn -f services/api/pom.xml test -Dtest=EmbeddingIndexVersionRecorderTest,EmbeddingSignatureTest`
- [x] 6.1 定向回归、验证与归档
  - Test: 上述全部测试；`python3 scripts/change_scope.py --base 120b382`；workflow checks
