# 验证记录

Status: passed.

Owner: Test Agent（单人执行）

review_mode: single-agent

## Spec 基线

- base_ref: `e801f849cb464d7f4498616d89d19baabc5fbad1`
- 变更等级: Major
- 决策记录: `.agents/notes/implemented/architecture/2026-08-23-agent-native-loop.md`（归档时迁移为 implemented）

## 编辑前基线

- 命令: change-1/2 的 38 个定向测试。
- 结果: 退出码 0；38 tests 通过（干净 shell 输出 `/tmp/change3-baseline.log`）。

## 定向验证（干净 shell）

- 命令:
  `mvn -f services/api/pom.xml test -Dmaven.repo.local=/home/hanw/AgentProject/.tooling/m2-repo -Dtest=AgentTurnRuntimeTypesTest,AgentTurnOpenApiContractTest,AgentTurnWsContractTest,AgentTurnConfigurationTest,AgentTurnPersistenceModelTest,TurnEventBusTest,TurnRuntimeServiceTest,AgentTurnControllerTest,AgentTurnWebSocketHandlerTest,CapabilityToolRegistryTest,AgentDeclarativeToolsTest,TurnOrchestratorTest,AgentCapabilitiesControllerTest,AgentCapabilitiesOpenApiContractTest,OpenAiCompatibleLlmClientTest,ToolDispatcherTest,AgentLoopTest,TurnRuntimePauseResumeTest,AgentLoopOrchestratorTest,AgentLoopEvalTest`
- 环境: `env -i HOME=... JAVA_HOME=... PATH=... MAVEN_OPTS=... bash --noprofile --norc -c ...`
- 原始输出: `/tmp/change3-clean-targeted.log`
- 退出码: `0`
- 结果: `Tests run: 54, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`。
- 覆盖: 38 个 change-1/2 回归 + 16 个 change-3a 新增（SSE adapter、ToolDispatcher、AgentLoop、暂停恢复、Orchestrator、Eval）。

## 完整后端回归

- 命令: `mvn -f services/api/pom.xml test -Dmaven.repo.local=/home/hanw/AgentProject/.tooling/m2-repo`
- 原始输出: `/tmp/change3-full.log`
- 退出码: `1`
- 结果: `Tests run: 417, Failures: 0, Errors: 35, Skipped: 5`。
- 根因: 与基线相同的无 PostgreSQL/Docker 环境；417 = 401 基线 + 16 新增，错误数未增加。

## 工作流与范围检查

- `python3 scripts/change_scope.py --base e801f849cb464d7f4498616d89d19baabc5fbad1`：退出码 0。
- `python3 scripts/check_agent_notes.py`：退出码 0（4 notes）。
- `python3 scripts/check_suilearn_workflow.py --base-ref e801f849cb464d7f4498616d89d19baabc5fbad1`：退出码 0。
- `git diff --check`：退出码 0。
- `git diff e801f849cb464d7f4498616d89d19baabc5fbad1 --stat`：已核对，文件落在 policy 允许范围。

## 注意

- `.env.example` 与 `docs/plans/suilearn-refactor-plan.md` 未提交修改属于既有遗留，不属于本 change。
- 旧 ReactAgent/旧 REST/Alibaba 依赖按 spec 未修改，删除为 3b follow-up。
- 真实 OpenAI-compatible runtime fixture 冒烟与真实模型质量 Eval 未执行，属 3b 前条件。
