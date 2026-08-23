# Agent-Native Turn Runtime 策略

Status: Approved
批准者: 用户
批准日期: 2026-08-23
批准依据: 用户指令“执行SuiLearn改造计划……之后按spec执行即可”；不确定项采用 spec 中记录的推荐方案。

- Change: `agent-native-turn-runtime`
- 级别: Major
- base_ref: `6de3ec5caeead9e85ad18bc94c3886a7fe9f1e5e`
- 当前阶段: Spec -> Build
- 执行模式: serial（L3）
- 决策记录: `.agents/notes/implemented/architecture/2026-08-23-agent-native-turn-runtime.md`

## 角色归属

- Architect Agent：`contracts/**` 契约与 WS companion schema。
- Server Backend Agent：`services/api/**` 实现与相邻测试。
- Test Agent：干净 shell 独立执行定向验证并保留原始输出。
- Reviewer Agent：单人自审，`review_mode: single-agent`。
- Leader Agent：任务卡、文件范围、Approval/Sync Gate 与归档收口。

## 允许修改文件

- `openspec/changes/agent-native-turn-runtime/**`
- `.agents/notes/implemented/architecture/2026-08-23-agent-native-turn-runtime.md`
- `.agents/notes/implemented/architecture/2026-08-23-agent-native-turn-runtime.md`
- `contracts/openapi/suilearn-v2.yaml`
- `contracts/schemas/suilearn-ws.yaml`
- `services/api/pom.xml`
- `services/api/src/main/resources/application.properties`
- `services/api/config/local.properties.example`
- `.env.example`
- `compose.yml`
- `services/api/src/main/java/com/suilearn/api/agent/runtime/**`
- `services/api/src/main/java/com/suilearn/api/agent/capability/**`
- `services/api/src/main/java/com/suilearn/api/agent/tool/**`
- `services/api/src/main/java/com/suilearn/api/agent/controller/**`
- `services/api/src/main/java/com/suilearn/api/agent/infrastructure/turn/**`
- `services/api/src/test/java/com/suilearn/api/agent/runtime/**`
- `services/api/src/test/java/com/suilearn/api/agent/contract/**`
- `services/api/src/test/java/com/suilearn/api/agent/controller/**`
- `services/api/src/test/java/com/suilearn/api/agent/infrastructure/turn/**`
- `services/api/src/test/resources/agent-turn/**`

## 禁止修改文件

- `apps/android/**`
- `apps/web/**`
- `services/api/src/main/java/com/suilearn/api/agent/application/LearningAgentPort.java`
- `services/api/src/main/java/com/suilearn/api/agent/infrastructure/springai/**`
- 旧 Agent Prompt 资源 `services/api/src/main/resources/agents/**`
- `docs/**` 当前事实文档与 `docs/proposals/**`
- 其他 active change 目录
- `openspec/specs/**`
- Redis、RabbitMQ、MinIO、知识库、资料、正式题目相关模块

## 基线测试

- 业务代码编辑前运行 `mvn -f services/api/pom.xml test -q`（本地 JDK 21 + Maven 3.9，仓库 `/home/hanw/AgentProject/.tooling/m2-repo`）。
- 基线结果：`Tests run: 363, Failures: 0, Errors: 35, Skipped: 5`；35 个 error 全部源于无本地 PostgreSQL（`Connection to localhost:5432 refused`），发生在业务编辑前。Testcontainers 集成测试 5 skipped。
- 本 change 的通过门禁使用新增定向测试；完整后端回归因缺少 PostgreSQL 在沙箱不可用，已在 verification 记录不适用原因，不得静默跳过。

## 验收矩阵

| 场景 | 默认值/覆盖语义 | 必需验证 |
|---|---|---|
| Agent 总开关 | `suilearn.agent.enabled` 默认 false；关闭时新 REST/WS 返回 `AGENT_FEATURE_DISABLED` | 单元测试 |
| WS 子开关 | `suilearn.agent.websocket.enabled` 默认 true；关闭时 WS 返回 `AGENT_WEBSOCKET_DISABLED` | 单元测试 |
| 事件 seq | 从 1 连续递增，唯一键 `(turn_id, seq)` | TurnEventBus/RuntimeService 测试 |
| replay | `subscribe`/`resumeFrom` 按 afterSeq 持久化重放后接实时流 | RuntimeService 测试 |
| 队列 | 默认容量 256，满时丢弃实时帧不阻塞 publisher | TurnEventBus 慢消费者测试 |
| 终态 | done/cancelled/failed 唯一，终态后拒绝 publish | TurnEventBus 测试 |
| 取消 | 只允许 CREATED/RUNNING/WAITING_INPUT，取消后无新事件 | RuntimeService 测试 |
| submitReply | 只允许 WAITING_INPUT，否则 `AGENT_TURN_NOT_WAITING_FOR_INPUT` | RuntimeService 测试 |
| 孤儿恢复 | 启动时 RUNNING -> FAILED_ORPHANED + 唯一 failed 事件 | RuntimeService/Persistence 测试 |
| payload | 单条事件 UTF-8 上限 64 KiB，超限拒绝 | RuntimeService 测试 |
| 占位执行器 | 只发 unavailable error + failed，不写正式内容 | RuntimeService 测试 |
| 旧路径 | 本 change 不修改旧 Agent 类与旧资源 | 文件范围核对 |

## 高风险事件立即审查

- 契约或 schema 变更。
- 事务/唯一约束/seq 并发。
- WebSocket 广播与背压。
- 任何无法解释的测试失败。

## 审查重点

- OpenAPI 只做 additive 扩展，旧 `/api/v2/agents/study/runs` 语义不变。
- 事件先落库后推送，实时推送不产生持久化假成功。
- 日志/错误/事件 metadata 不含用户正文、Prompt、原始模型输出或 API key。
- 不引入 WebFlux/Reactor；Spring AI 类型不越过既有 `infrastructure/springai/**` 边界。
- 不把旧 Agent 桥接进新 runtime。
