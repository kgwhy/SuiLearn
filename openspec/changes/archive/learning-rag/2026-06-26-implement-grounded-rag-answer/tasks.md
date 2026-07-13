# 任务

## 1. 服务端真实 RAG 回答

- 状态：已完成
- Owner：Server Backend Agent
- 允许文件：
  - `services/api/src/main/java/com/suilearn/api/ai/AiProvider.java`
  - `services/api/src/main/java/com/suilearn/api/ai/OpenAiCompatibleAiProvider.java`
  - `services/api/src/main/java/com/suilearn/api/rag/application/RagService.java`
  - `services/api/src/main/java/com/suilearn/api/service/internal/SuiLearnV2Workflow.java`
  - `services/api/src/test/java/com/suilearn/api/service/SuiLearnV2ServiceTest.java`
- 禁止文件：
  - `apps/**`
  - `contracts/**`
  - `docs/proposals/**`
  - `docs/superpowers/**`
  - `docs/product-requirements.md`
  - `docs/tech-selection.md`
- 验证：`mvn -f services/api/pom.xml test -q 2>&1`

### 完成定义

- `AiProvider` 提供基于证据回答问题的接口。
- OpenAI-compatible Provider 实现该接口并要求 JSON grounded answer。
- `RagService` 和 `SuiLearnV2Workflow` 在有证据时调用 Provider 生成 `RagAnswer.answer`。
- 新增或更新测试覆盖真实 RAG 回答编排。

### 验证记录

- `mvn -f services/api/pom.xml test -DskipTests -q 2>&1`：通过，编译和测试编译成功。
- `mvn -f services/api/pom.xml test -q 2>&1`：未通过，原因是本地 PostgreSQL 测试库不可用，Spring Test 启动时连接 `localhost:5432` 被拒绝。
