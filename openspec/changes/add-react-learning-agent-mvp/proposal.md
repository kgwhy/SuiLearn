## 背景与动机

SuiLearn 已具备知识库、RAG、结构化生成和题目草稿能力，但仍缺少一个能够围绕学习目标自主选择检索与练习能力、保持短期和长期学习上下文的真实 Agent 用例。本变更以与典型简历 Agent 项目相当的规模，引入一个后端优先、无 Web 依赖、可演示 ReAct、SubAgent、分层记忆、上下文工程和 Prompt 工程的知识库学习 Agent MVP。

## 变更内容

- 新增一个后端 `Supervisor ReAct Agent`，接收学习目标或问题，在有界执行预算内选择并编排知识研究与练习辅导能力。
- 新增 `KnowledgeResearchSubAgent` 与 `PracticeCoachSubAgent`，通过 Agent-as-Tool 接受最小上下文、最小工具集和结构化委派输入，并只返回结构化结果。
- 新增三层记忆：单次运行的 working memory、Redis 中带 TTL 的 session memory，以及 PostgreSQL/pgvector 中带来源和置信度的 semantic memory。
- 新增统一 ContextManager，对当前任务、近期会话、长期记忆、RAG 证据和工具观察结果执行相关性排序、去重、预算裁剪和最小化 SubAgent 快照构建。
- 新增版本化 Prompt Registry，以外部资源文件管理 Supervisor、SubAgent 和记忆提炼 Prompt，并通过 JSON Schema 约束模型输出。
- 新增一个 REST 运行接口和记忆删除边界，返回结构化回答、来源引用、临时练习题、下一步建议和不含思维链的 action trace。
- 新增最大轮数、总工具调用数、上下文预算、超时和失败语义；AI、Redis、embedding 或检索依赖异常不得产生虚假成功。
- 新增 Micrometer 基础指标及小型 Agent Eval 数据集，覆盖工具路由、知识库隔离、引用、结构化输出、记忆晋升、上下文裁剪和 Prompt Injection 防护。
- 引入并锁定与 Java 21、Spring Boot 3.5.14 兼容的 Spring AI / Spring AI Alibaba Agent Framework 与 Redis 客户端依赖；进入 Build 前必须通过依赖兼容性门禁。
- 更新 OpenAPI、产品规格、架构和技术选型稳定事实，并在 Compose/环境模板中增加非敏感 Redis 与 Agent 配置。

范围内：

- 后端 REST API、Supervisor、两个 SubAgent、只读检索工具、临时练习生成、三层记忆、上下文和 Prompt 管理、基础指标和自动化测试。
- 单用户/显式 `learnerId` 与 `sessionId` 范围下的学习 Agent MVP；不新增登录系统。

非目标：

- Web 或 Android Agent UI、正式题库自动写入、长期任务调度、Run Ledger、Checkpoint/Resume/Fork、通用 HITL 审批平台、MCP、插件市场、Shell/浏览器工具、容器沙箱、深层嵌套 Agent、Agent Team、动态模型路由和线上自动改写 Skill/Prompt。

验收标准：

- 用户可通过 REST 在指定知识库或资料范围内请求讲解与练习，Supervisor 能在预算内调用正确 SubAgent，并返回可验证的 JSON 结果和有效来源引用。
- 同一会话可读取 Redis 近期摘要，后续会话可按 `learnerId` 召回经策略允许的长期学习记忆；记忆可删除，且不同学习者、知识库和资料范围不得串扰。
- 上下文超过预算时按既定优先级裁剪，外部资料只能作为不可信 evidence，不能覆盖系统指令或工具边界。
- 未配置或不可用的 AI、Redis、embedding、知识库或资料依赖以明确错误或显式降级状态返回，不伪造证据、记忆或练习结果。
- 自动化测试和 Agent Eval 覆盖成功路径、预算停止、错误路由、无证据、引用越界、记忆隔离、Prompt Injection 和依赖故障。

## 能力范围

### 新增能力

- `react-study-agent`: 有界 Supervisor ReAct 循环、两个 Agent-as-Tool SubAgent、知识检索与临时练习生成、结构化响应和 action trace。
- `layered-agent-memory`: working、Redis session、PostgreSQL/pgvector semantic 三层记忆，以及隔离、晋升、去重、TTL、来源、置信度和删除语义。
- `agent-context-prompt-management`: 上下文组装与预算裁剪、SubAgent 上下文隔离、Prompt Registry 版本化、结构化输出、注入防护和 Agent Eval。

### 修改的既有能力

无。现有 `knowledge-point-interview-questions` 的正式题目草稿审核和保存语义保持不变；本变更的练习题在 MVP 中仅作为 Agent 响应中的临时内容。

## 影响范围

- **Backend**: `services/api/**` 新增 Agent、上下文、Prompt 和记忆边界，并适配现有 AI、RAG、检索与指标能力。
- **Contracts**: `contracts/openapi/suilearn-v2.yaml` 新增 Agent 运行和记忆删除契约。
- **Runtime**: `services/api/pom.xml`、`compose.yml`、`.env.example` 与后端配置新增 Spring AI Alibaba、Redis 和 Agent 配置；所有默认值、覆盖键、禁用/回退语义和运行态验证必须在设计与验证产物中明确。
- **Persistence**: PostgreSQL 新增长期记忆事实和向量索引；Redis 新增带 TTL 的会话记忆，不作为长期事实源。
- **Stable facts**: `docs/product-requirements.md`、`docs/architecture.md`、`docs/tech-selection.md` 在 Sync Gate 前同步已验证结论。
- **Testing**: 后端单元/集成测试、Testcontainers PostgreSQL/Redis、契约测试和固定 Agent Eval 用例。
- **Unaffected**: Android 本地离线学习闭环、Web 工作台和现有正式题目审核/保存流程保持不变。
