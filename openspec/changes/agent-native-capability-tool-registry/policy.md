# Agent-Native Capability/Tool Registry 策略

Status: Approved
批准者: 用户
批准日期: 2026-08-23
批准依据: 用户指令“执行SuiLearn改造计划”及“按你的推荐即可”。

- Change: `agent-native-capability-tool-registry`
- 级别: Major
- base_ref: `24720c11369caf1a0b06d569046083d3932f2266`
- 当前阶段: Spec -> Build
- 执行模式: serial（L3）
- 决策记录: `.agents/notes/implemented/architecture/2026-08-23-agent-native-capability-tool-registry.md`

## 角色归属

- Architect Agent：`contracts/**` 契约与 schema。
- Server Backend Agent：`services/api/**` 实现与相邻测试。
- Test Agent：干净 shell 独立执行定向验证并保留原始输出。
- Reviewer Agent：单人自审，`review_mode: single-agent`。
- Leader Agent：任务卡、文件范围、Approval/Sync Gate 与归档收口。

## 允许修改文件

- `openspec/changes/agent-native-capability-tool-registry/**`
- `.agents/notes/implemented/architecture/2026-08-23-agent-native-capability-tool-registry.md`
- `.agents/notes/implemented/architecture/2026-08-23-agent-native-capability-tool-registry.md`
- `contracts/openapi/suilearn-v2.yaml`
- `services/api/src/main/java/com/suilearn/api/agent/runtime/**`
- `services/api/src/main/java/com/suilearn/api/agent/capability/**`
- `services/api/src/main/java/com/suilearn/api/agent/tool/**`
- `services/api/src/main/java/com/suilearn/api/agent/controller/**`
- `services/api/src/test/java/com/suilearn/api/agent/runtime/**`
- `services/api/src/test/java/com/suilearn/api/agent/tool/**`
- `services/api/src/test/java/com/suilearn/api/agent/controller/**`
- `services/api/src/test/java/com/suilearn/api/agent/contract/**`

## 禁止修改文件

- `apps/android/**`
- `apps/web/**`
- 旧 `LearningAgentPort.java`、`SpringAiAlibabaLearningAgentAdapter.java`、旧 ReactAgent 拓扑
- `services/api/src/main/java/com/suilearn/api/agent/infrastructure/springai/**`
- `docs/**` 当前事实文档与 `docs/proposals/**`
- Redis、RabbitMQ、MinIO、知识库、资料、正式题目相关模块
- 其他 active change 目录

## 基线测试

- 基线提交 `24720c1` 已含 change-1：新增 27 个测试全绿。
- 本 change 编辑前定向回归：
  `mvn -f services/api/pom.xml test -q -Dtest=AgentTurnRuntimeTypesTest,AgentTurnOpenApiContractTest,AgentTurnWsContractTest,AgentTurnConfigurationTest,AgentTurnPersistenceModelTest,TurnEventBusTest,TurnRuntimeServiceTest,AgentTurnControllerTest,AgentTurnWebSocketHandlerTest`
- 沙箱无 PostgreSQL：完整 `mvn test` 保持 35 个既有环境 errors 基线；本 change 通过新增定向测试与受影响回归验证。

## 验收矩阵

| 场景 | 默认值/覆盖语义 | 必需验证 |
|---|---|---|
| capability 默认 | 空 capability -> study_agent | CapabilityRegistry 测试 |
| capability 未知 | AGENT_CAPABILITY_UNKNOWN | Registry/Orchestrator 测试 |
| study_agent 工具面 | 6 个工具 | Registry/契约测试 |
| rag_qa 工具面 | search_knowledge/read_evidence | Registry 测试 |
| question_generation 工具面 | generate_practice/ask_user（deferred） | Registry 测试 |
| 工具 schema | OpenAI function-calling wrapper | ToolRegistry 测试 |
| 越权工具 | 运行时拒绝 | ToolRegistry 测试 |
| 证据工具 | 复用 scope/删除校验 | Tool 测试 |
| 练习工具 | 无模型 -> success=false, AGENT_MODEL_UNAVAILABLE | Tool 测试 |
| memory 工具 | 不可用 -> MEMORY_UNAVAILABLE | Tool 测试 |
| ask_user | pauseForUser，非成功 | Tool 测试 |
| orchestrator | capability source + unavailable failed | Orchestrator 测试 |
| capabilities API | agent enabled=false -> AGENT_FEATURE_DISABLED | Controller 测试 |

## 高风险事件立即审查

- 契约或 schema 变更。
- 工具权限计算或 scope 校验。
- 任何写入语义：persist_memory、generate_practice。
- 任何无法解释的测试失败。

## 审查重点

- 不写正式内容 store；persist_memory 只写 Agent 语义记忆。
- 工具 schema 不含 Prompt、正文或 secret。
- 旧 `AgentToolCatalog`/旧 SubAgent 保持不变。
- 能力清单 additive，不改变旧端点语义。
