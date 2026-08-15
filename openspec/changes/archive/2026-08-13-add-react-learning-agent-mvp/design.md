## 背景

SuiLearn Backend 已有 OpenAI-compatible `AiProvider`、Spring AI 端口预留、RAG、混合检索、知识库/资料范围约束、结构化生成、题目草稿和 Micrometer 基础能力。当前没有真实 Agent 业务边界，Spring AI adapter 也没有启用 starter；Android 本地学习闭环必须继续离线可用，Web 当前只承载知识库重流程。

本变更由 Leader 协调，产品范围与验收由 Product Agent 拥有，Agent 模块/依赖/数据/API 边界由 Architect Agent 拥有，`services/api/**` 实现由 Server Backend Agent 拥有，验证由 Test Agent 独立执行，Reviewer 负责 Spec Review 与 Code Review。稳定事实只在 Verify/Sync Gate 通过后由对应角色同步。

约束：

- MVP 规模必须保持在“一个后端 Supervisor、两个 SubAgent、三层记忆、一个运行接口、一组 Prompt 和测试”内。
- Agent 不得直接保存正式题目、修改知识库、执行 Shell、访问任意 URL 或加载动态插件。
- Android 本地闭环和现有 API 行为不变；Agent 未启用或不可用时不得影响其他 Backend 能力。
- Spring AI Alibaba 类型不得泄漏到产品、领域、Controller DTO 或现有 AI/RAG 业务端口。
- 外部资料、历史消息、记忆和工具结果都视为不可信上下文，不能覆盖系统指令、工具白名单或 scope。
- 配置、依赖和运行态能力必须有默认值、覆盖口、合法范围、残留扫描和真实启动验证。

## 目标与非目标

**目标：**

- 通过 Spring AI Alibaba Agent Framework 实现可识别的 Supervisor ReAct + Agent-as-Tool 模式。
- 通过 `KnowledgeResearchSubAgent` 与 `PracticeCoachSubAgent` 完成“证据检索/阅读 → 讲解/临时练习”的受控学习闭环。
- 通过 working、Redis session、PostgreSQL/pgvector semantic 三层记忆支持单次执行、多轮会话和跨会话学习画像。
- 通过 ContextManager 和版本化 Prompt Registry 控制上下文预算、隔离、注入风险和结构化输出。
- 提供 REST/curl 可演示接口、低基数指标和可在 CI 中稳定运行的小型 Agent Eval。

**非目标：**

- 不做 Web/Android Agent UI、账号/鉴权、云同步、多租户安全承诺或多渠道 Gateway。
- 不做 Run Ledger、Event Sourcing、Checkpoint/Resume/Fork、长任务队列或通用 Agent Runtime。
- 不做通用权限/HITL 平台；MVP 通过只读工具和临时草稿消除不可逆副作用。
- 不做 MCP、插件市场、动态 Tool/Skill 安装、Shell/浏览器/网络抓取、容器沙箱或多层递归委派。
- 不做第三个 Memory SubAgent、线上 Skill/Prompt 自我改写、完整 OTel 平台或动态多模型路由。

## 设计决策

### 1. 使用基础设施隔离的 Spring AI Alibaba Supervisor ReAct

新增应用端口 `LearningAgentPort` 与基础设施 adapter。`agent/application/**` 只依赖 SuiLearn 自有 DTO、Context/Memory/Tool 端口；Spring AI 和 Spring AI Alibaba 类型只允许出现在 `agent/infrastructure/springai/**`。基础设施层构造一个 Supervisor `ReactAgent`，把两个专门 `ReactAgent` 包装为 Agent Tool。

```text
LearningAgentController
  -> LearningAgentPort
     -> SpringAiAlibabaLearningAgentAdapter
        -> Supervisor ReactAgent
           -> KnowledgeResearchSubAgent as AgentTool
           -> PracticeCoachSubAgent as AgentTool
```

选择它而非自研 Planner 循环，是为了直接展示标准 ReAct、Tool Calling 和 Agent-as-Tool，同时复用框架的结构化调用能力。选择它而非完整 Supervisor/Graph/Team，是为了限制拓扑、Token 和测试复杂度。

依赖门禁：在任何业务实现前，Architect/Backend 必须用独立 Maven 兼容性测试确认 Java 21、Spring Boot 3.5.14、Spring AI BOM 与 Spring AI Alibaba Agent Framework 的可解析版本组合，并运行最小 Context 加载测试。预期候选为 Spring AI 1.1.2 与 Spring AI Alibaba 1.1.2.2；若依赖无法无排除冲突地解析或最小 Agent 无法启动，任务返回 Spec，禁止静默改用自研循环或升级 Spring Boot。

### 2. 固定两个 SubAgent，采用显式委派和共享预算

- `KnowledgeResearchSubAgent` 只拥有 `searchKnowledge`、`readEvidence` 工具；输入包含问题、`knowledgeBaseId`/`materialId` scope、最大证据数和最小必要记忆摘要；输出为 `EvidenceBundle`。
- `PracticeCoachSubAgent` 不直接检索数据库；输入包含学习目标、已验证 `EvidenceBundle`、难度和数量；输出为 `CoachingResult`，包含讲解、临时练习题和下一步建议。
- Supervisor 负责选择是否只研究、研究后练习或在无证据时停止。SubAgent 不能创建更多 Agent，不能访问 MemoryStore，不能修改正式内容。

默认预算：Supervisor 最大 4 个 ReAct step；每个 SubAgent 最大 3 step；全局最大 8 次 tool/agent 调用；单次请求总超时 90 秒；练习题默认 3、最大 5。达到预算必须返回 `BUDGET_EXHAUSTED` 和已验证的部分结果，不得继续调用。

选择显式固定拓扑而非动态 Agent discovery，是为了可测、可解释并控制简历 MVP 范围。

### 3. REST 契约是单次同步运行，无流式和持久 Run

新增：

```text
POST   /api/v2/agents/study/runs
DELETE /api/v2/agents/study/learners/{learnerId}/memories
```

运行请求包含 `learnerId`、可选 `sessionId`、`question`、`knowledgeBaseId` 或 `materialId`、可选 `practiceCount` 与 `difficulty`。至少提供一个知识范围；服务端生成缺失的 `sessionId`。响应包含 `runId`、`sessionId`、回答、不确定标志、引用、临时练习题、下一步建议、记忆状态、预算使用和不含模型思维链的 action trace。

该 API 是单用户/可信调用方 MVP。`learnerId` 只用于逻辑隔离，不构成身份认证。进入多用户或公网部署前必须新建鉴权变更，不能把此契约宣称为安全租户边界。

### 4. 三层记忆由普通 MemoryManager 管理

```text
L1 Working  : 当前 AgentState；请求结束即释放
L2 Session  : Redis；近期消息摘要、未完成目标；滑动 TTL
L3 Semantic : PostgreSQL + pgvector；目标、偏好、薄弱点、掌握状态
```

Redis key 使用受控前缀与 learner/session 哈希，不把原始输入拼成 key；默认 TTL 24 小时、最多保留最近 20 个 turn 的摘要事实。Redis 是 L2 唯一事实源，应用重启不保留 L1。

`AgentSemanticMemory` 至少保存 `id`、`learnerId`、`memoryType`、`content`、`contentFingerprint`、`embedding`、`confidence`、`sourceRunId`、`sourceRef`、`createdAt`、`updatedAt`。允许类型仅为 `GOAL`、`PREFERENCE`、`WEAKNESS`、`MASTERY`。默认召回 Top-K 5，必须先按 `learnerId` 过滤，再做向量相似度排序。

运行结束后由结构化 memory extraction Prompt 产生候选，确定性 `MemoryPromotionPolicy` 校验类型、最小置信度 0.80、长度、来源和指纹；只有通过候选才 upsert。原始 transcript、Prompt Injection 文本、临时情绪和无来源推断不得进入 L3。删除接口清除目标 learner 的 L2 key 与 L3 事实，并返回删除计数。

选择 Redis + PostgreSQL，而非全部放 PostgreSQL，是为了展示明确的短期 TTL 与长期语义召回；不新增第三方 Memory SaaS，避免不可控数据边界。

### 5. ContextManager 实施固定的上下文优先级和最小快照

`ContextAssembler` 从当前任务、L2 摘要、L3 Top-K、RAG Evidence 和最近 Observation 构建 `AgentContextSnapshot`。默认上下文预算 12,000 tokens，按以下优先级保留：

```text
System/Safety/Tool contract
  > 当前问题与 scope
  > 已验证 RAG evidence
  > 最近 session 摘要
  > 长期 semantic memory
  > 历史 observation
```

同一来源按稳定 ID 去重；超预算时先删除低相关 observation，再裁剪低分长期记忆、旧 session 摘要和低分 evidence。系统/安全/工具契约、当前问题和 scope 不得被压缩或删除。Evidence 以带 ID/source 的数据段注入并显式标记 untrusted；任何其中的指令样文本只作为内容。

MVP 不实现递归摘要或完整 token-cache engine。`ContextBudgetPolicy` 必须可测试并记录各来源 token/字符估算与裁剪原因，但日志不记录正文。

### 6. Prompt Registry 使用版本化资源与类型化变量

Prompt 放置于：

```text
services/api/src/main/resources/agents/
  supervisor/v1/system.md
  knowledge-research/v1/system.md
  practice-coach/v1/system.md
  memory-extraction/v1/system.md
  few-shot/*.json
```

`PromptRegistry` 按固定 allowlist 读取资源，暴露 `promptName/version/hash`；模板变量使用类型化映射和框架模板 API，不允许通过文件路径或请求参数选择任意 Prompt。每个 Prompt 明确 role、goal、constraints、工具前置条件、停止条件、evidence policy、failure policy 和 output schema。

模型输出必须映射到 Java record 并通过 Bean Validation/JSON Schema 验证；缺字段、未知 action、越界引用或空白必填内容最多修复一次，仍失败则明确返回 `INVALID_MODEL_OUTPUT`。

### 7. 工具均为只读或临时生成，正式内容门禁不变

`searchKnowledge` 与 `readEvidence` 复用现有 Retriever/RAG scope 校验，不允许跨知识库、读取已删除资料或返回无稳定 source ref 的证据。`PracticeCoachSubAgent` 只在响应中生成临时练习，不写 `QuestionStore`、`GeneratedContentStore` 或现有任务中心。

因此 MVP 不引入通用 PolicyEngine/HITL。工具注册采用每 Agent 固定 allowlist；发现任何需要保存正式题目、修改学习进度、上传资料或调用外部系统的新需求时必须返回 Spec。

### 8. 故障与降级语义必须显式

- Agent 未启用：接口返回 404 或稳定的 feature-disabled 错误；其他 Backend 能力正常。
- AI 未配置/不可用：返回 503 `AGENT_MODEL_UNAVAILABLE`，不生成占位回答。
- Redis 不可用：返回 503 `AGENT_SESSION_MEMORY_UNAVAILABLE`；不静默退化为进程内会话。
- scope 不存在或资料已删除：返回 4xx 或不确定结果，不能跨 scope 搜索。
- 无证据：返回 `uncertain=true`，不生成伪引用；PracticeCoach 不生成声称基于资料的题目。
- embedding 不可用：知识检索可沿用现有明确的关键词路径；L3 语义召回标记 `LONG_TERM_MEMORY_DEGRADED`，不得把任意记录当语义命中。
- L3 写入失败：回答可返回，但 `memoryStatus=PERSIST_FAILED`，不得声称已记住。
- 模型超时、预算耗尽、结构输出无效：返回稳定错误/部分状态和 action trace，不泄露原始模型响应。

### 9. 配置与运行态验收矩阵

| 配置 | 默认值 | 环境变量覆盖 | 合法范围/语义 |
|---|---:|---|---|
| `suilearn.agent.enabled` | `false` | `SUILEARN_AGENT_ENABLED` | 未启用不创建 Agent 运行入口依赖，不影响既有 API |
| `suilearn.agent.max-steps` | `4` | `SUILEARN_AGENT_MAX_STEPS` | 1..8 |
| `suilearn.agent.subagent-max-steps` | `3` | `SUILEARN_AGENT_SUBAGENT_MAX_STEPS` | 1..6 |
| `suilearn.agent.max-tool-calls` | `8` | `SUILEARN_AGENT_MAX_TOOL_CALLS` | 1..16 |
| `suilearn.agent.run-timeout` | `90s` | `SUILEARN_AGENT_RUN_TIMEOUT` | 10s..180s |
| `suilearn.agent.context-max-tokens` | `12000` | `SUILEARN_AGENT_CONTEXT_MAX_TOKENS` | 2048..32768 |
| `suilearn.agent.practice-default-count` | `3` | `SUILEARN_AGENT_PRACTICE_DEFAULT_COUNT` | 1..5 |
| `suilearn.agent.session.ttl` | `24h` | `SUILEARN_AGENT_SESSION_TTL` | 1h..168h |
| `suilearn.agent.session.max-turns` | `20` | `SUILEARN_AGENT_SESSION_MAX_TURNS` | 1..50 |
| `suilearn.agent.memory.top-k` | `5` | `SUILEARN_AGENT_MEMORY_TOP_K` | 1..10 |
| `suilearn.agent.memory.min-confidence` | `0.80` | `SUILEARN_AGENT_MEMORY_MIN_CONFIDENCE` | 0.50..1.00 |
| `spring.data.redis.host` | `localhost` | `SPRING_DATA_REDIS_HOST` | 非空 host；Compose 使用服务名覆盖 |
| `spring.data.redis.port` | `6379` | `SPRING_DATA_REDIS_PORT` | 1..65535 |

启动时校验所有范围。Agent enabled 时必须能构造 ChatModel、Agent Framework、Redis repository 和 semantic memory store；任一缺失必须使 Agent readiness 降级并阻止 Agent 请求，不能影响 HTTP liveness 与其他业务 readiness。

残留扫描必须覆盖：生产代码中硬编码 Prompt、大范围动态工具注册、`QuestionStore`/正式内容写入、未受控原始 transcript 落库、无 `learnerId` 过滤的长期记忆查询、Prompt/正文进入 metric tag，以及未在 `.env.example`/Compose/技术基线登记的 Agent/Redis 配置。

运行态验证至少覆盖：默认 disabled 启动；enabled + PostgreSQL/Redis/AI mock 启动；Redis 停机；AI 未配置；非法配置拒绝；两个 learner/session 隔离；pgvector/关键词路径；真实 HTTP 请求与指标变化。

### 10. 可观测性和 Eval 保持轻量

Micrometer 记录 `agent.run` duration/outcome、`agent.tool.calls`、`agent.subagent.calls`、`agent.context.tokens`、`agent.memory.operations`；tag 仅允许受控的 `agent`、`tool`、`outcome`、`memoryLayer`，禁止 learner/session/run/source/prompt 正文等高基数值。结构化日志可记录 `runId`、`sessionId`、prompt version/hash 和 action name，不记录完整正文、原始模型响应或思维链。

固定 Eval 至少 10 个场景，使用 deterministic fake ChatModel/tool fixtures，比较工具路由、schema、引用、scope、预算、记忆和注入防护。默认测试不访问真实模型或公网；可选 live smoke 只在显式 profile 和密钥存在时运行，不能作为 CI 必需条件。

## 风险与取舍

- [Spring AI Alibaba 与 Spring Boot 3.5.14 依赖不兼容] → 把兼容性 spike 作为首个阻塞任务；失败返回 Spec，不升级 Boot 或偷换实现。
- [ReAct 路由非确定导致测试脆弱] → 固定 topology、严格工具描述、低 step 预算、结构化 Agent Tool 输入/输出和 deterministic fake model。
- [三层记忆增加 Redis 运维和数据污染风险] → Agent 默认关闭、Redis 强依赖显式失败、受控类型/置信度/来源/指纹晋升和删除接口。
- [caller-provided learnerId 不是安全身份] → 将 API 限定为单用户/可信调用 MVP；禁止宣称租户隔离，公网/多用户前单独加鉴权。
- [Prompt Injection 通过资料或记忆影响工具选择] → evidence/context 数据段隔离、系统契约不可裁剪、SubAgent 固定工具白名单、无副作用工具。
- [上下文预算估算与模型真实 tokenizer 有偏差] → 预留安全余量、记录估算、超限失败时返回明确错误；不在 MVP 自建 tokenizer 服务。
- [同步 90 秒接口延迟较高] → 限制 scope、证据数、步骤和练习数；流式/异步 Run 留给后续变更。
- [过度包装简历技术导致范围回膨胀] → policy 禁止 Run Ledger、MCP、插件、第三 Agent 和正式内容写入；新增必须回到 Spec。

## 迁移与回滚计划

1. 先完成依赖兼容性和最小 Agent Context 测试，不修改业务契约。
2. 由 Architect 更新 OpenAPI 与技术边界；Backend 先以测试定义 DTO、预算、Context、Prompt 和 Memory 端口。
3. 增加 Redis Compose/环境配置和 PostgreSQL semantic memory 表/向量索引；Agent 默认保持 disabled。
4. 实现两个只读/临时 SubAgent 与 Supervisor adapter，完成契约、集成和 Eval。
5. 在完整本地栈显式启用 Agent，执行运行态矩阵；通过后同步稳定事实。

回滚：设置 `SUILEARN_AGENT_ENABLED=false` 即停止暴露 Agent 能力；Redis key 由 TTL 自动回收，PostgreSQL semantic memory 表保留以避免破坏性回滚。若需要删除表或配置，使用后续具名迁移，不在紧急回滚中丢数据。所有新增 API 为 additive，无现有客户端迁移。

## 待确认事项

无未决产品或架构问题。Spring AI/Spring AI Alibaba 精确依赖组合由兼容性任务用真实 Maven 与 Context 启动证据确定；若候选版本不满足约束，必须返回 Spec。
