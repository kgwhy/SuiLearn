# 任务

## 1. 后端修复

- Owner: Server Backend Agent
- 状态: done
- 允许文件:
  - `services/api/src/main/java/com/suilearn/api/material/application/MaterialImportService.java`
  - `services/api/src/main/java/com/suilearn/api/knowledgepoint/application/KnowledgePointService.java`
  - `services/api/src/main/java/com/suilearn/api/service/internal/SuiLearnV2Workflow.java`
  - `services/api/src/test/java/com/suilearn/api/service/SuiLearnV2ServiceTest.java`
- 禁止文件:
  - `contracts/**`
  - `docs/proposals/**`
- 验证:
  - `mvn -f services/api/pom.xml -Dtest=SuiLearnV2ServiceTest test -q`

## 2. Web 兜底

- Owner: Web Frontend Agent
- 状态: done
- 允许文件:
  - `apps/web/src/App.tsx`
- 禁止文件:
  - `contracts/**`
  - `docs/proposals/**`
- 验证:
  - `npm --prefix apps/web run build`

## 3. 收尾验证

- Owner: Leader Agent
- 状态: done
- 验证:
  - `git diff 46472606357a68dfb92966851213228fbbaa2541 --stat`
