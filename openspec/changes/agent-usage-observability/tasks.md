# Agent Usage 任务

- Change: `agent-usage-observability`
- Owner: Server Backend
- 级别: Major
- 基线引用: `e10b91f2427dc83fb2f9fa8086fcec603165dc45`
- 决策记录: `.agents/notes/proposed/architecture/2026-08-23-agent-usage-observability.md`

## 待办

- [x] 1.1 创建 change 包与 Agent Note
- [x] 2.1 实现 UsageTracker 并接入 AgentLoop
  - Allowed: `services/api/src/main/java/com/suilearn/api/agent/llm/**`, `services/api/src/main/java/com/suilearn/api/agent/loop/**`, `services/api/src/test/java/com/suilearn/api/agent/llm/**`, `services/api/src/test/java/com/suilearn/api/agent/loop/**`
  - Test: `mvn -f services/api/pom.xml test -q -Dtest=UsageTrackerTest,AgentLoopTest`
- [x] 3.1 回归、验证与归档
  - Test: 既有 71 + 新增
