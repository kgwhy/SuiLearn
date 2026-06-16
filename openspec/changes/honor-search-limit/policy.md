# 策略

## 变更

- 名称：`honor-search-limit`
- 等级：Normal
- base_ref：`3f3fe48b8c940ed3be2d922e6739d143c7e122c1`
- Worktree 模式：串行
- 锁状态：未创建持久锁文件；当前任务在本线程串行执行。

## 角色归属

- Leader Agent 协调 `openspec/changes/honor-search-limit/**`。
- Server Backend Agent 负责 `services/api/**` 实现和测试。
- Reviewer Agent 审查更广的 V2 缺口和最终 diff。

## 允许路径

- `openspec/changes/honor-search-limit/**`
- `services/api/src/main/java/com/suilearn/api/controller/SearchController.java`
- `services/api/src/main/java/com/suilearn/api/search/application/SearchService.java`
- `services/api/src/main/java/com/suilearn/api/retrieval/Retriever.java`
- `services/api/src/main/java/com/suilearn/api/retrieval/KeywordRetriever.java`
- `services/api/src/main/java/com/suilearn/api/service/SuiLearnV2Service.java`
- `services/api/src/main/java/com/suilearn/api/service/internal/SuiLearnV2Workflow.java`
- `services/api/src/test/java/com/suilearn/api/service/SuiLearnV2ServiceTest.java`

## 禁止路径

- `contracts/**`
- `apps/android/**`
- `apps/web/**`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/tech-selection.md`
- `docs/proposals/**`
- `docs/superpowers/**`

## 基线

业务代码编辑前，`mvn -f services/api/pom.xml test -q` 已通过。
