# 验证记录

状态：Verify 进行中；实现与任务级 TDD 已完成，完整运行态/全量回归与稳定事实同步待完成。
负责人：Test Agent，由 Leader Agent 协调。

## Spec 基线

- 基线引用：`6f1434ef849bd8e467cc8e0e1c68c37fa9e998c4`。
- `openspec validate add-react-learning-agent-mvp --strict`：Spec 创建期间及 2026-07-18 实现后均通过。
- Build `base_ref`：`83d035b3d6cca290422a0f83b0245fd0a899f796`。
- 编辑前完整后端基线因本机 `localhost:5432` 未运行而失败；失败发生在业务编辑前，首个根因为 PostgreSQL connection refused。
- 用户明确要求本轮不运行全量后端测试；实现采用任务级 red-green 与独立定向复测，完整回归因此仍是 Sync Gate 缺口。

### 2026-07-18 全量测试尝试

- 用户在提交前授权执行 `mvn -f services/api/pom.xml test -q`；命令运行约 8 分钟后 exit 1，因此本 change 不声明 Verify/Archive 完成。
- `MaterialImportFailureTransactionIntegrationTest`：1 error；`SuiLearnV2ServiceTest`：33 errors。共同首因是测试配置要求 PostgreSQL 数据库 `suilearn_test`，初次 Compose 环境只存在默认库，报告为 `FATAL: database "suilearn_test" does not exist`。之后已在本地测试容器创建该数据库，但按用户指令未继续重跑。
- `RuntimeFixtureFaultScriptContractTest`：2 failures。既有 `scripts/verify-runtime-faults.ps1` 缺少测试要求的 OCR timeout metric 与 AI circuit-open metric 断言片段；脚本和对应测试相对 `HEAD` 均无 diff，属于本 change 范围外的既有不一致，本轮不越界修复。
- 用户明确决定保留以上失败用例和原因，先提交到 `dev`，后续另行修复。任务 4.3、5.1、5.2、5.4 继续保持未完成。

## 2026-07-18 实现与验证证据

- 兼容性：Java 21、Spring Boot 3.5.14、Spring AI 1.1.2、Spring AI Alibaba Agent Framework 1.1.2.2；真实 `ReactAgent` disabled/enabled Context 定向测试通过，依赖树无 exclusion 或 Spring AI 版本漂移。
- OpenAPI：PyYAML 安全解析通过；Controller/OpenAPI/Metrics 定向测试最终 9/9 通过。
- Context/Prompt/ReAct/Wiring/Health：任务级命令与最终 fresh 复测均 exit 0；最终 ReAct/Wiring/Health 组合通过，覆盖每请求局部 Agent-as-Tool、scope、共享预算、总 deadline、Context/Memory 接线、repair trace 与 readiness。
- Memory：最终 14/14 通过，其中真实 Testcontainers Redis 7.4 + PostgreSQL 16/pgvector 集成测试 3/3 通过，覆盖 TTL、隔离、删除、DDL 幂等和 learner/type-first vector recall。
- Agent Eval：12/12 deterministic offline 场景通过，不访问真实模型或公网。
- Runtime/config：`docker compose --env-file .env.example config` 通过；Agent 默认 disabled，API 不以 Redis health 作为默认启动依赖；配置范围、AI/Redis 缺失、readiness 与错误映射由定向 Context/Health/Controller 测试覆盖。
- 质量检查：`git diff --check`、workflow skill check、`scripts/check-suilearn-workflow.ps1`（通过进程级 safe.directory 注入）及 strict OpenSpec validate 均通过。
- 独立审查：首轮 6 P1/2 P2、次轮 7 P1/4 P2 均已定向修复；最终独立静态复审剩余 P0/P1/P2 为 0，Leader 补齐 fresh 动态命令后闭环。

## 完成前所需证据

- 依赖收敛和最小 Agent Context 启动证据。
- OpenAPI 校验及实现一致性。
- Context/Prompt、memory、orchestration、Controller/metrics 和 Agent Eval 的测试数量/结果。
- 完整后端测试结果及任何合理的排除项。
- Compose 渲染以及 disabled/enabled 运行态探针。
- Redis 不可用、AI 不可用、非法配置、无证据、跨 scope、记忆隔离和删除失败证据。
- `policy.md` 定义的残留扫描。
- 稳定事实同步 diff，以及 Android/Web/既有正式题目流程未受影响的证据。
- 独立 Spec Review 与 Code Review，且所有 P0/P1/P2 已关闭。
- `git diff --check`、工作流检查、严格 OpenSpec 校验和基线 diff stat/文件范围。

## 运行态矩阵状态

| 探针 | 预期结果 |
|---|---|
| Agent disabled、Redis 缺失 | 定向 Application Context 通过；未执行完整应用进程探针。 |
| Agent enabled、依赖健康 | Wiring/Health 与真实 Redis/pgvector 分层集成通过；未执行真实 HTTP socket + fake AI 的完整进程探针。 |
| Redis 停止 | Readiness/Controller 故障注入通过，返回 session-memory unavailable；未执行容器运行中断探针。 |
| ChatModel 缺失 | Wiring/Health 故障注入通过，返回 model unavailable。 |
| 非法预算/TTL/范围 | 配置与 Controller 定向测试通过。 |
| 空/跨 scope evidence | ReAct/Eval/Controller 定向测试通过，不产生伪 citation/practice。 |
| 两个 learner/session | 单元、Eval 与真实存储 learner/type filter 测试通过。 |
| semantic-memory 写入失败 | 定向状态映射通过；未执行真实数据库运行中断探针。 |
| 记忆删除 | 单元、Controller 与真实 Redis/PostgreSQL 定向删除通过。 |

尚未完成：用户要求跳过的完整后端测试、真实应用 HTTP/curl 进程探针、Redis/数据库运行中断探针，以及 Android/Web/无关 Backend 的全量回归。因此任务 4.3、Sync Gate 与 Archive 保持打开。

## 审查状态

- Spec Review：已执行；所有 P0/P1/P2 已关闭。
- Code Review：已执行；所有 P0/P1/P2 已关闭。
- 最终 reviewer-style 自审：待完整运行态/全量回归与稳定事实同步后执行。
