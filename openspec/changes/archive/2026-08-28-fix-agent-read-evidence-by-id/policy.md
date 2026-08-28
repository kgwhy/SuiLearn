# Policy: 修复 Agent read_evidence 找不到可读证据

## 变更信息

- 变更：`fix-agent-read-evidence-by-id`
- 状态：`Status: Approved`
- 批准者：用户（2026-08-28 反馈 Agent 反复 search/read 后 BUDGET_EXHAUSTED）
- 等级：Standard
- Owner：Server Backend Agent（本环境无 Java/Maven，无法执行后端测试，按 policy 记录原因）
- base_ref：`d95c11df5118ca188ae49fb01c9f85bc1dfa4a27`
- Build 循环：L2（实现 + 新增测试，但因环境无 Maven 只能静态审阅）

## 允许的文件

- `openspec/changes/fix-agent-read-evidence-by-id/**`
- `services/api/src/main/java/com/suilearn/api/agent/tool/RetrievalEvidenceTools.java`
- `services/api/src/main/java/com/suilearn/api/agent/config/AgentInfrastructureConfiguration.java`
- `services/api/src/test/java/com/suilearn/api/agent/tool/RetrievalEvidenceToolsTest.java`
- `.agents/notes/implemented/architecture/2026-08-28-fix-agent-read-evidence-by-id.md`

## 禁止的文件与行为

- `apps/**`、`contracts/**`、`docs/**`、`agent`、CLI 或测试脚本
- 修改 OpenAI/检索排序语义
- 引入新的前端/CLI 行为

## 验证计划

- 静态检查：`git diff --check`
- Java 编译/测试：本环境无 `java`/`mvn`，无法执行；需在 Windows/本地 Java 环境运行：
  ```bash
  mvn -f services/api/pom.xml -Dtest=RetrievalEvidenceToolsTest test
  ```

## 完成定义

- `read_evidence` 在 material chunk 场景可通过 chunkId 直接读到内容。
- 新测试存在，并在后续有 Java 环境的 CI/local 中通过。
