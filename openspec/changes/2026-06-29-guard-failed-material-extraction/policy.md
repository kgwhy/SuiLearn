# 策略

- 变更等级: Major
- 理由: 同时触达后端与 Web 前端，改变用户可见导入后续行为，但不修改契约或存储结构。
- base_ref: `46472606357a68dfb92966851213228fbbaa2541`
- worktree 模式: serial
- 锁定路径:
  - `openspec/changes/2026-06-29-guard-failed-material-extraction/**`
  - `services/api/src/main/java/com/suilearn/api/material/application/MaterialImportService.java`
  - `services/api/src/main/java/com/suilearn/api/knowledgepoint/application/KnowledgePointService.java`
  - `services/api/src/main/java/com/suilearn/api/service/internal/SuiLearnV2Workflow.java`
  - `services/api/src/test/java/com/suilearn/api/service/SuiLearnV2ServiceTest.java`
  - `apps/web/src/App.tsx`
- 允许路径:
  - `openspec/changes/2026-06-29-guard-failed-material-extraction/**`
  - `services/api/**`
  - `apps/web/**`
- 禁止路径:
  - `contracts/**`
  - `apps/android/**`
  - `docs/proposals/**`
  - `docs/product-requirements.md`
  - `docs/architecture.md`
  - `docs/tech-selection.md`

## 基线测试

- `mvn -f services/api/pom.xml -Dtest=SuiLearnV2ServiceTest test -q`: passed
- `npm --prefix apps/web run build`: passed
