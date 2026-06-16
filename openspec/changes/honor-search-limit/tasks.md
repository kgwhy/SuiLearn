# 任务

## 任务 1：后端遵守 search limit

- 状态：已完成
- Owner：Server Backend Agent
- 等级：Normal
- 允许文件：
  - `services/api/src/main/java/com/suilearn/api/controller/SearchController.java`
  - `services/api/src/main/java/com/suilearn/api/search/application/SearchService.java`
  - `services/api/src/main/java/com/suilearn/api/retrieval/Retriever.java`
  - `services/api/src/main/java/com/suilearn/api/retrieval/KeywordRetriever.java`
  - `services/api/src/main/java/com/suilearn/api/service/SuiLearnV2Service.java`
  - `services/api/src/main/java/com/suilearn/api/service/internal/SuiLearnV2Workflow.java`
  - `services/api/src/test/java/com/suilearn/api/service/SuiLearnV2ServiceTest.java`
- 禁止文件：
  - `contracts/**`
  - `apps/android/**`
  - `apps/web/**`
  - `docs/product-requirements.md`
  - `docs/architecture.md`
  - `docs/tech-selection.md`
- 必需基线：
  - `passed`：`mvn -f services/api/pom.xml test -q`
- 必需验证：
  - `mvn -f services/api/pom.xml test -q`
- 审查重点：
  - Search 保持显式 scoped。
  - Limit 默认值/最小值/最大值与 OpenAPI 一致。
  - 测试覆盖 limit 应用和非法边界。

## 审查说明

Reviewer Agent 同步审查更广的 V2 完成缺口。本窄范围后端契约修复之外的发现记录为 follow-up 范围，不属于本任务。
