# 退役旧 ReactAgent 与旧 Agent REST（change-3b）

## Why

change-3a 已让 study_agent 通过 LlmClient/AgentLoop/ToolDispatcher 运行，完整后端回归在 Docker 依赖下仅剩 Testcontainers socket 环境例外。旧 ReactAgent、旧 REST、Alibaba Agent Framework 与固定 SubAgent 拓扑已无新调用路径，按计划应在固定 Eval 通过后删除，避免双轨。

## What Changes

- 删除旧 `LearningAgentPort`、`SpringAiAlibabaLearningAgentAdapter`、`AgentFrameworkConfiguration` 及旧 ReactAgent 图。
- 删除旧 `LearningAgentController`、`StudyAgentDtos`、旧异常映射与旧 OpenAPI 路径/schema。
- 删除 `KnowledgeResearchSubAgent` 与旧 Supervisor/Practice 独立拓扑；练习生成逻辑保留在 `PracticeCoachSubAgent` 作为 `generate_practice` 工具的确定性 helper，并移除旧 catalog/budget 依赖。
- 工具端口从 `LearningAgentPort.AgentScope` 迁移到 `StudyScope`；练习难度迁移到 `PracticeDifficulty`。
- 新增 `AgentInfrastructureConfiguration`，保留检索证据、练习模型、会话/语义记忆等新工具所需 bean；`PracticeModelPort` 改用 `LlmClient` 实现，移除 Spring AI ChatModel 依赖。
- `services/api/pom.xml` 移除 `spring-ai-alibaba-agent-framework`。
- 删除旧 Agent 兼容/控制器/适配器/Eval 测试；新增 3b 残留扫描测试与更新工具测试。

## Capabilities

### Modified

- `agent-native-loop`: 旧路径删除后成为唯一 Agent 运行入口。
- `agent-capability-registry`: 工具实现不再依赖旧 scope/难度类型。

## Impact

- `services/api/**` 与 `contracts/openapi/suilearn-v2.yaml`。
- 不修改 `apps/**`、`docs/**` 当前事实文档。

## Non-Goals

- 不删除仍计划复用的记忆/上下文/prompt 类（Phase 4/5 再演进）。
- 不迁移 Redis/pgvector 数据，不实现 usage 计价。
- 不删除 Spring AI BOM（仅移除 Alibaba Agent Framework 依赖）。

## Acceptance Criteria

- 源码无 `com.alibaba.cloud.ai` / `ReactAgent` / `LearningAgentPort` / 旧 REST 注解残留。
- OpenAPI 不再包含 `/api/v2/agents/study/**` 或 `StudyAgent*` schema。
- 新工具测试与 AgentLoop Eval 全绿；完整后端回归（Docker 下）除 Testcontainers socket 环境用例外全绿。
- `spring-ai-alibaba-agent-framework` 不再出现在 `mvn dependency:tree`。
