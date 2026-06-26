# 策略

## 变更

- 名称：`implement-grounded-rag-answer`
- 等级：Normal
- base_ref：`3b8aababf1e49294a32a41eb8ed1780632364ad5`
- Worktree 模式：serial

## 角色归属

- Leader Agent：变更包协调与门禁。
- Server Backend Agent：`services/api/**` 实现与测试。

## 允许路径

- `openspec/changes/implement-grounded-rag-answer/**`
- `services/api/src/main/java/com/suilearn/api/ai/AiProvider.java`
- `services/api/src/main/java/com/suilearn/api/ai/OpenAiCompatibleAiProvider.java`
- `services/api/src/main/java/com/suilearn/api/rag/application/RagService.java`
- `services/api/src/main/java/com/suilearn/api/service/internal/SuiLearnV2Workflow.java`
- `services/api/src/test/java/com/suilearn/api/service/SuiLearnV2ServiceTest.java`

## 禁止路径

- `apps/**`
- `contracts/**`
- `docs/proposals/**`
- `docs/superpowers/**`
- `docs/product-requirements.md`
- `docs/tech-selection.md`

## 基线

- 进入 Build 前运行 `mvn -f services/api/pom.xml test -q 2>&1`。
- 如果本地 PostgreSQL 不可用，记录为 `unavailable`，并使用 `mvn -f services/api/pom.xml test -DskipTests -q 2>&1` 验证编译。
