# 政策：ReAct 学习 Agent MVP

## 变更信息

- 变更：`add-react-learning-agent-mvp`
- 状态：已通过 Approval Gate
- 等级：Major
- 负责人：Leader Agent
- 基线引用：`6f1434ef849bd8e467cc8e0e1c68c37fa9e998c4`
- 阶段：Verify（完整运行态/全量回归与稳定事实同步待完成）
- 工作树/锁：当前工作区；创建 proposal 时不存在活动文件锁。Build 批次只能锁定已声明文件，范围扩大时必须停止。

## 角色归属

- Product Agent：产品边界、验收文案和最终 `docs/product-requirements.md` 同步。
- Architect Agent：依赖/版本决策、模块/数据/配置/API 设计、`contracts/**`、`docs/architecture.md`、`docs/tech-selection.md` 及经 Leader 授权的根运行时模板。
- Server Backend Agent：`services/api/**` 实现及实现相邻测试。
- Test Agent：独立 Agent Eval、集成/运行态证据和 `verification.md` 证据。
- Reviewer Agent：独立 Spec Review 与 Code Review，不直接修复。
- Leader Agent：任务卡、文件锁、批次协调、Approval/Sync Gate 和归档收口。

## 允许的实现文件

- `openspec/changes/add-react-learning-agent-mvp/**`
- `contracts/openapi/suilearn-v2.yaml`
- `services/api/pom.xml`
- `services/api/src/main/java/com/suilearn/api/agent/**`
- `services/api/src/main/resources/agents/**`
- `services/api/src/main/resources/application.properties`
- `services/api/config/local.properties.example`
- `services/api/src/test/java/com/suilearn/api/agent/**`
- `services/api/src/test/resources/agents/**`
- `services/api/src/test/resources/agent-eval/**`
- `compose.yml`
- `.env.example`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/tech-selection.md`

列表外的任何必要修改都属于范围扩大，修改前 MUST 返回 Spec。

## 禁止的文件与行为

- `apps/android/**`、`apps/web/**`、题包/内容文件及无关 Backend 模块。
- `docs/proposals/**`、`docs/superpowers/specs/**`、`docs/superpowers/plans/**`。
- 既有 `QuestionStore`、`GeneratedContentStore`、任务派发/outbox 或正式题目审核行为。
- 位于 `services/api/src/main/java/com/suilearn/api/agent/infrastructure/springai/**` 之外的 Spring AI/Spring AI Alibaba 类型。
- 动态工具/插件加载、MCP、Shell/浏览器/网络抓取工具、第三个 SubAgent、嵌套委派、Agent Team、Run Ledger、checkpoint/resume、流式 API、Web UI 或认证。
- Java 中硬编码 Prompt（短错误/校验消息除外）、持久化原始 Prompt/transcript/model response，或在 metric tag 中写入正文/ID。
- 向仓库加入真实 AI 凭据、生产 Redis 凭据或其他 secret。

## Build 进入门禁

1. 用户显式批准本 proposal、design、specs、tasks 和本 policy。
2. `openspec validate add-react-learning-agent-mvp --strict` 通过。
3. 任务 1.1 是首个 Build spike；在其证明兼容依赖组合且保持 Java 21/Spring Boot 3.5.14 前，不得开始 Agent 业务实现。
4. Architect Agent 必须在 Controller 实现前完成增量 OpenAPI 契约。
5. 每个实现批次都声明精确文件、负责人、禁止文件、TDD 测试与审查命令。
6. 业务实现遵循 red-green-refactor；最终 Test/Reviewer 证据不得由 Implementer 自证。

## 验收矩阵

| 范围 | 默认值/覆盖语义 | 必需验证 |
|---|---|---|
| Agent 功能 | 默认禁用；必须显式启用；禁用时不影响既有 API。 | disabled/enabled Context 测试和 HTTP 探针。 |
| ReAct 预算 | Supervisor 4 step、SubAgent 3、全局 8 次调用、90 秒超时；范围校验并显式停止。 | 预算单元测试和超时探针。 |
| Agent 拓扑 | 一个 Supervisor、恰好两个不可递归的 Agent Tool，并使用固定 allowlist。 | 路由/工具拒绝测试和架构扫描。 |
| Context | 12k token 估算；system/task/scope 不可变；固定裁剪优先级。 | 边界和注入测试；裁剪指标。 |
| Prompt | 固定资源 allowlist、version/hash、类型化变量、一次 schema repair。 | Registry/schema 测试和硬编码 Prompt 残留扫描。 |
| Session memory | Redis、24 小时滑动 TTL、最多 20 turn；故障返回 503。 | Redis Testcontainers、TTL/中断/隔离测试。 |
| Semantic memory | PostgreSQL/pgvector、Top-K 5、最小置信度 0.80、四种允许类型和 learner 过滤。 | 持久化/vector/去重/冲突/删除测试。 |
| 练习 | 默认 3/最多 5；仅响应内临时结果；不写正式 store/task。 | Store 非交互和回归测试。 |
| RAG scope | 必须有 knowledgeBaseId 或 materialId；仅有效稳定 citation。 | 跨 scope/已删除/无证据测试。 |
| 可观测性 | 仅 outcome/agent/tool/layer 等低基数 tag；禁止正文/ID/Prompt。 | Meter/log 检查测试。 |
| 运行时配置 | 环境变量覆盖已文档化的非敏感默认值；非法范围拒绝启动/readiness。 | 配置测试、Compose 渲染、运行态探针和残留扫描。 |

## 必需残留扫描

- 允许的 infrastructure package 外出现 Spring AI/Spring AI Alibaba import。
- Agent 调用正式 question/generated-content/task store。
- 动态工具注册、MCP、plugin、Shell、browser 或任意 URL 行为。
- Java 中硬编码多行 Agent Prompt 或请求任意选择 Prompt 路径。
- 缺少 learner 过滤的 semantic-memory repository 查询。
- metric tag 或未脱敏日志中出现 raw transcript、Prompt、model response、learner/session/run/source 值。
- 未登记的 `SUILEARN_AGENT_*`/Redis key、不一致默认值或真实凭据。
- Web/Android 改动或 Agent UI 产物。

## 验证计划

- 运行全部任务级 TDD 命令和完整后端测试套件。
- 校验 OpenAPI 及其 Controller 契约测试。
- 运行 PostgreSQL/Redis Testcontainers 和确定性 Agent Eval。
- 使用 `.env.example` 渲染 Compose；启动 disabled/enabled 配置；注入 Redis/AI/配置故障。
- 用 curl/API 覆盖仅研究、研究加练习、无证据、预算耗尽和记忆删除。
- 运行残留扫描、`git diff --check`、严格 OpenSpec 校验、工作流检查器和基线 diff/文件范围审计。
- 先进行独立 Spec Review，再 Code Review，并关闭所有 P0/P1/P2。

## 审批记录

- 用户已于 2026-07-17 批准概念/设计方向。
- 用户已于 2026-07-18 通过“实现 agent”指令批准本 change package 进入 Build。
- `openspec validate add-react-learning-agent-mvp --strict` 已于 2026-07-18 通过；任务 1.1 仍是业务实现前的阻塞兼容性门禁。
