# 验证记录

Status: passed.

Owner: Test Agent（单人执行）

review_mode: single-agent

## Spec 基线

- base_ref: `6de3ec5caeead9e85ad18bc94c3886a7fe9f1e5e`
- 变更等级: Major
- 决策记录: `.agents/notes/implemented/architecture/2026-08-23-agent-native-turn-runtime.md`

## 施工基线（编辑前）

- 命令: `mvn -f services/api/pom.xml test -q`（本地 JDK 21 + Maven 3.9，repo `/home/hanw/AgentProject/.tooling/m2-repo`）。
- 结果: `Tests run: 363, Failures: 0, Errors: 35, Skipped: 5`。
- 根因: 本机 `localhost:5432` 无 PostgreSQL；35 个 error 均为 `Connection to localhost:5432 refused` 导致 Spring Context 加载失败，Testcontainers 集成测试 5 skipped。失败发生在业务编辑前。

## 定向验证（干净 shell 独立执行）

- 命令:
  `mvn -f services/api/pom.xml test -Dmaven.repo.local=/home/hanw/AgentProject/.tooling/m2-repo -Dtest=AgentTurnRuntimeTypesTest,AgentTurnOpenApiContractTest,AgentTurnWsContractTest,AgentTurnConfigurationTest,AgentTurnPersistenceModelTest,TurnEventBusTest,TurnRuntimeServiceTest,AgentTurnControllerTest,AgentTurnWebSocketHandlerTest`
- 环境: `env -i HOME=... JAVA_HOME=... PATH=... MAVEN_OPTS=... bash --noprofile --norc -c ...`
- 原始输出: `/tmp/turn-targeted-final.log`
- 退出码: `0`
- 结果: `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`。
- 覆盖: 核心类型、OpenAPI 新增路径/schema、WS companion schema + golden files、配置/依赖、持久化模型、有界 event bus、runtime 生命周期/replay/cancel/orphan/payload、REST controller、WS handler。

## 完整后端回归（干净 shell）

- 命令: `mvn -f services/api/pom.xml test -Dmaven.repo.local=/home/hanw/AgentProject/.tooling/m2-repo`
- 原始输出: `/tmp/suilearn-full-final.log`
- 退出码: `1`
- 结果: `Tests run: 390, Failures: 0, Errors: 35, Skipped: 5`。
- 根因: 与基线相同的环境缺失。35 errors 全部为 PostgreSQL Context 加载失败（`Connection to localhost:5432 refused` 出现 6 次，Testcontainers 环境不可用 3 次）。本 change 的 27 个新增定向测试在完整回归中全部通过；390 = 363 基线 + 27 新增，错误数保持 35，证明本 change 未新增失败面。
- 不适用说明: 沙箱无可用 PostgreSQL/Docker；不把该环境失败声明为本 change 回归。

## 工作流与范围检查

- `python3 scripts/change_scope.py --base 6de3ec5caeead9e85ad18bc94c3886a7fe9f1e5e`：退出码 0；输出 committed/staged/unstaged/untracked 四类。
- `python3 scripts/check_agent_notes.py`：退出码 0；`Agent Notes check passed (2 note(s)).`
- `python3 scripts/check_suilearn_workflow.py --base-ref 6de3ec5caeead9e85ad18bc94c3886a7fe9f1e5e`：退出码 0；`SuiLearn Workflow policy check passed.`
- `git diff --check`：退出码 0。
- `git diff 6de3ec5caeead9e85ad18bc94c3886a7fe9f1e5e --stat`：已执行；本 change 文件均落在 `policy.md` 允许范围内。
- OpenAPI/companion schema: `PyYAML safe_load` 解析通过；`AgentTurnOpenApiContractTest` 与 `AgentTurnWsContractTest` 覆盖新增路径、枚举和 golden files。

## 注意

- `docs/plans/suilearn-refactor-plan.md` 的未提交修改在本次执行前已存在，不属于本 change，未纳入完成声明。
- `apps/android/**`、`apps/web/**` 未修改；Android/Web 构建测试不适用。
- 真实 PostgreSQL/WS 运行态联调、跨实例 replay 和 UI 证据延期到具名 follow-up，见 archive.md。
