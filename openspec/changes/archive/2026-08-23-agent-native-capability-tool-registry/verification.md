# 验证记录

Status: passed.

Owner: Test Agent（单人执行）

review_mode: single-agent

## Spec 基线

- base_ref: `24720c11369caf1a0b06d569046083d3932f2266`
- 变更等级: Major
- 决策记录: `.agents/notes/implemented/architecture/2026-08-23-agent-native-capability-tool-registry.md`（归档时迁移为 implemented）

## 编辑前基线

- 命令: `mvn -f services/api/pom.xml test -q -Dtest=AgentTurnRuntimeTypesTest,AgentTurnOpenApiContractTest,AgentTurnWsContractTest,AgentTurnConfigurationTest,AgentTurnPersistenceModelTest,TurnEventBusTest,TurnRuntimeServiceTest,AgentTurnControllerTest,AgentTurnWebSocketHandlerTest`
- 结果: 退出码 0；change-1 的 27 个测试通过（干净 shell 输出 `/tmp/change2-baseline.log`）。

## 定向验证（干净 shell）

- 命令:
  `mvn -f services/api/pom.xml test -Dmaven.repo.local=/home/hanw/AgentProject/.tooling/m2-repo -Dtest=AgentTurnRuntimeTypesTest,AgentTurnOpenApiContractTest,AgentTurnWsContractTest,AgentTurnConfigurationTest,AgentTurnPersistenceModelTest,TurnEventBusTest,TurnRuntimeServiceTest,AgentTurnControllerTest,AgentTurnWebSocketHandlerTest,CapabilityToolRegistryTest,AgentDeclarativeToolsTest,TurnOrchestratorTest,AgentCapabilitiesControllerTest,AgentCapabilitiesOpenApiContractTest`
- 环境: `env -i HOME=... JAVA_HOME=... PATH=... MAVEN_OPTS=... bash --noprofile --norc -c ...`
- 原始输出: `/tmp/change2-clean-targeted.log`（归档前最终执行）
- 退出码: `0`
- 结果: `Tests run: 38, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`。
- 覆盖: change-1 27 个回归 + change-2 11 个新增（registry、6 tools、orchestrator、controller、OpenAPI）。

## 完整后端回归

- 命令: `mvn -f services/api/pom.xml test -Dmaven.repo.local=/home/hanw/AgentProject/.tooling/m2-repo`
- 原始输出: `/tmp/change2-full.log`
- 退出码: `1`
- 结果: `Tests run: 401, Failures: 0, Errors: 35, Skipped: 5`。
- 根因: 与基线相同的无 PostgreSQL/Docker 环境。35 errors 均为既有 Context 加载失败（`Connection to localhost:5432 refused`），本 change 未新增失败面；401 = 390 基线 + 11 新增。

## 工作流与范围检查

- `python3 scripts/change_scope.py --base 24720c11369caf1a0b06d569046083d3932f2266`：退出码 0。
- `python3 scripts/check_agent_notes.py`：退出码 0。
- `python3 scripts/check_suilearn_workflow.py --base-ref 24720c11369caf1a0b06d569046083d3932f2266`：退出码 0。
- `git diff --check`：退出码 0。
- `git diff 24720c11369caf1a0b06d569046083d3932f2266 --stat`：已核对，文件落在 policy 允许范围。

## 注意

- `.env.example` 与 `docs/plans/suilearn-refactor-plan.md` 的未提交修改属于 change-1/既有工作区遗留，不属于本 change。
- `apps/android/**`、`apps/web/**` 未修改；Android/Web 构建测试不适用。
- 真实 PostgreSQL/WS 运行态与客户端消费延期到具名 follow-up。
