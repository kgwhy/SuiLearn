# Policy: 让 search_knowledge 返回模型可用的证据指针内容

## 变更信息

- 变更：`fix-agent-search-result-pointers`
- 状态：`Status: Approved`
- 批准者：用户（2026-08-28 反馈模型只 search 不 read）
- 等级：Standard
- Owner：Server Backend Agent
- base_ref：`65f520e`
- Build 循环：L2

## 允许的文件

- `openspec/changes/fix-agent-search-result-pointers/**`
- `services/api/src/main/java/com/suilearn/api/agent/tool/SearchKnowledgeTool.java`
- `services/api/src/test/java/com/suilearn/api/agent/tool/AgentDeclarativeToolsTest.java`
- `.agents/notes/implemented/architecture/2026-08-28-agent-search-returns-pointer-content.md`

## 禁止的文件

- `apps/**`、`contracts/**`、`docs/**`、CLI 与检索 pipeline。

## 验证计划

- 静态检查 `git diff --check`
- 本环境无 Java/Maven；待用户本地执行：
  ```bash
  mvn -f services/api/pom.xml -Dtest=AgentDeclarativeToolsTest test
  ```
