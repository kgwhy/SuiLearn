# 退役旧 ReactAgent 任务

- Change: `agent-native-loop-retire-legacy`
- Owner: Server Backend
- 级别: Major
- 基线引用: `9dfa79724f0091da6fbdc75a0ee77e0d1d730181`
- 执行模式: serial（L3）
- 决策记录: `.agents/notes/implemented/architecture/2026-08-23-agent-native-loop-retire-legacy.md`

## 待办

- [x] 1.1 创建 change 包与 Agent Note
  - Allowed: `openspec/changes/agent-native-loop-retire-legacy/**`, `.agents/notes/implemented/architecture/2026-08-23-agent-native-loop-retire-legacy.md`
  - Test: `python3 scripts/check_agent_notes.py`
- [x] 2.1 迁移工具类型到 StudyScope/PracticeDifficulty
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/tool/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=AgentDeclarativeToolsTest,CapabilityToolRegistryTest`
- [x] 3.1 新增 LlmPracticeModelPort 与 AgentInfrastructureConfiguration
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/config/**`, `services/api/src/main/java/com/suilearn/api/agent/llm/**`, `services/api/src/main/java/com/suilearn/api/agent/tool/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=AgentDeclarativeToolsTest`
- [x] 4.1 删除旧 ReactAgent、controller、REST 与依赖
  - Allowed: `services/api/**`、`contracts/openapi/suilearn-v2.yaml`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=AgentTurnOpenApiContractTest,AgentTurnRuntimeTypesTest`
- [x] 5.1 删除旧测试并新增残留扫描测试
  - Allowed: `services/api/src/test/java/com/suilearn/api/agent/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=LegacyRetirementScanTest`
- [x] 6.1 Docker 下完整回归并记录 verification
  - Allowed: `openspec/changes/agent-native-loop-retire-legacy/verification.md`
  - Test: `mvn -f services/api/pom.xml test`（Docker 依赖已启动）
- [x] 7.1 单人自审与归档
  - Allowed: `openspec/changes/agent-native-loop-retire-legacy/**`
  - Test: `python3 scripts/check_suilearn_workflow.py --closing-change agent-native-loop-retire-legacy`
