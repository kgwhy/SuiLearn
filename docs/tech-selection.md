# 随心学 SuiLearn 技术与版本基线

## 1. 文档职责

本文是 SuiLearn 的技术选型与版本基线真相源，由架构 Agent 维护。

本文只描述已落盘并可由当前工程验证的技术。计划、目标和实施状态只存在于 `openspec/changes/<change-name>/**`；只有验证和 Review 闭环后才允许把新技术事实写入本文。

本文回答：

- 当前使用哪些技术栈。
- 每项技术的最低版本、当前项目版本和升级约束。
- 哪些依赖、平台或基础设施暂不引入。
- 技术升级需要谁确认、修改哪些配置、运行哪些验证。

本文不回答：

- 代码目录如何组织、模块职责如何切分、数据如何流转。这些由 `docs/architecture.md` 维护。
- 产品范围、验收标准和阶段优先级。这些由 `docs/product-requirements.md` 维护。
- API 字段细节和跨端 schema。稳定契约由 `contracts/**` 维护。

## 2. 当前技术路线

SuiLearn 当前采用三端渐进路线：

| 范围 | 定位 | 技术选择 |
|---|---|---|
| Android App | 本地学习闭环，以及轻量消费 AI/知识库能力 | Native Android、Kotlin、Jetpack Compose、Room |
| Java Backend | AI 生成、知识库、资料导入、RAG、语义搜索、任务状态与 Agent-Native 学习助手 | Java、Spring Boot、JPA、PostgreSQL、OpenAI-compatible Provider、`agent/llm` streaming client、Spring MVC WebSocket、RAG pipeline 边界 |
| Web Frontend | 知识库工作台，承载资料导入、生成确认、搜索和问答 | React、TypeScript、Vite |
| Contracts | 跨端 API 单点真相 | OpenAPI |

当前不做 iOS，不做 Flutter，不做账号系统、云同步、社区和多租户权限。

Java Backend 当前支持多格式原始资料导入和持久化异步任务，采用 PostgreSQL Transactional Outbox、RabbitMQ、MinIO、受限解析/OCR adapter、Resilience4j、Actuator/Micrometer。Backend 是单个模块化单体，不新增独立 Worker/微服务。

## 3. 全局工程基线

| 项目 | 当前基线 | 约束 |
|---|---|---|
| 开发 JDK | 推荐 JDK 21 | 本地和 CI 可使用 JDK 21；Android 编译字节码目标仍保持 17 |
| Backend Java | Java 21 | 后端 source/target 目标基线为 21；升级工程配置需单独修改 `services/api/pom.xml` 并跑后端测试 |
| Android Kotlin JVM target | 17 | Android 源码语言为 Kotlin；Kotlin 编译目标保持 JVM 17，不使用 Java 21 字节码目标或专属 API |
| Git 分支 | `codex/` 前缀用于 Agent 分支 | 除非用户另有要求 |
| API 契约 | `contracts/openapi/suilearn-v2.yaml` | 服务端、Web 和 Android 远程能力必须按契约消费或适配 |
| 密钥管理 | 环境变量 / 本地配置文件 | API key、Authorization header 和原始密钥不得进入响应、任务表、日志或文档示例 |
| 测试命令 | 以 `AGENTS.md` 和 `docs/development-workflow.md` 为准 | Windows/PowerShell 优先使用 `.bat` 或 `npm --prefix` 命令 |

## 4. Android 基线

| 项目 | 当前版本 / 约束 |
|---|---|
| Gradle module | `:app`，物理目录 `apps/android` |
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 2.0.21 |
| Compose compiler plugin | Kotlin Compose plugin 2.0.21 |
| KSP | 2.0.21-1.0.25 |
| compileSdk | 35 |
| targetSdk | 35 |
| minSdk | 26 |
| applicationId | `com.suilearn` |
| versionName / versionCode | 1.0.1 / 2 |
| 源码语言 | Kotlin |
| Kotlin JVM target | 17 |
| UI | Jetpack Compose + Material 3 |
| Compose BOM | 2024.06.00 |
| Navigation | Navigation Compose 2.7.7 |
| Lifecycle | Lifecycle 2.8.3 |
| Room | 2.7.2 |
| AndroidX Test Core | 1.6.1 |
| Robolectric | 4.13 |

Android 约束：

- 保持单 Android module，不因 package 增长提前拆 Gradle 多 module。
- Android 本地闭环必须在未配置服务端、未配置 AI Provider 或网络不可用时继续可用。
- 第一阶段正式发布前可以破坏性重建开发数据；正式发布后 Room schema 变化必须提供 Migration 和测试。
- 依赖注入当前保持手动注入，不引入 Hilt，除非架构 Agent 更新本文并说明收益。
- 本地搜索当前使用 Room 查询；题量扩大到明显影响体验前，不引入 SQLite FTS。
- Android 不直接实现复杂 AI / RAG 逻辑，只消费服务端已定义能力。
- Android 不提升到 Java 21 字节码目标；`compileOptions` 和 `kotlinOptions.jvmTarget` 保持 17。

推荐验证：

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
```

## 5. Backend 基线

| 项目 | 当前版本 / 约束 |
|---|---|
| 目录 | `services/api` |
| 语言 | Java 21 |
| Framework | Spring Boot 3.5.14 |
| Build | Maven |
| API | REST + JSON + WebSocket（`/api/v2/ws`） |
| Agent WebSocket | `spring-boot-starter-websocket` + Spring MVC `TextWebSocketHandler`；不引入 WebFlux/Reactor |
| Agent LLM client | `agent/llm/OpenAiCompatibleLlmClient`：`java.net.http` SSE streaming、原生 tool-call delta 与 usage 聚合；不引入 LangChain4j / Spring AI 运行时依赖 |
| Persistence | Spring Data JPA |
| 开发 / 测试数据库 | PostgreSQL；本地统一使用根目录 `compose.yml` 启动数据库 |
| 目标数据库 | PostgreSQL |
| text-only 检索索引 | 应用层 n-gram tokenizer 生成 `search_text` → PostgreSQL 生成列 `search_tsv = to_tsvector('simple', search_text)` + GIN 索引；零扩展，适配镜像 `pgvector/pgvector:pg16`。pg_jieba/zhparser 真正中文分词需自定义镜像，列为后续可选升级 |
| 向量检索 | pgvector / OpenAI-compatible embedding 优先，关键词检索作为无语义命中时的兜底 |
| 检索索引迁移 | 项目无 Flyway/Liquibase，schema 由 Hibernate `ddl-auto` 管理；生成列、GIN 索引与 `search_text` 回填由运行时 `ApplicationRunner` 组件完成（沿用 `PostgresLargeObjectTextMigration` 模式） |
| AI Provider | 业务层依赖 `AiProvider`；当前运行时实现为 OpenAI-compatible |
| Spring AI | `pom.xml` 只保留 `spring-ai-bom` 1.1.2 做版本管理，无任何 Spring AI runtime 依赖；`ai/infrastructure/springai/**` 仅保留未启用的 port adapter 占位类，Agent 循环不使用 Spring AI 类型 |
| 测试 | Spring Boot Test / JUnit |

Backend 约束：

- 后端 Java source/target 目标基线为 21；当前工程配置升级需由 Backend 任务修改 `services/api/pom.xml` 的 `java.version` 并运行测试确认。
- 业务层不得直接依赖具体 AI 厂商 SDK；结构化生成通过 `AiProvider` 边界，Agent 通过 `agent/llm/LlmClient` 边界。
- 业务模块和 Agent 循环不得直接 import Spring AI 类型，例如 `ChatClient`、`ChatModel`、`EmbeddingModel`、`VectorStore`、`Advisor` 或 Tool Calling 类型。
- Spring AI 相关占位代码只允许位于 `services/api/src/main/java/com/suilearn/api/ai/infrastructure/springai/**`。
- 当前不新增 Spring AI runtime 依赖；`spring-ai-bom` 仅保留为版本管理。真正启用 Spring AI starter 时，必须由架构 Agent 更新本文、修改 `services/api/pom.xml` 并运行后端测试。
- Agent 路径固定为：`OpenAiCompatibleLlmClient` 走 `/chat/completions` SSE，使用 `prompt_tokens/completion_tokens` 聚合 usage，不支持时 fail closed，不静默切回结构化 Provider。
- Provider 状态接口只能暴露脱敏配置，例如 base URL、模型名、超时、重试和 API key 环境变量名。
- Agent 总开关 `suilearn.agent.enabled` 默认 false；`suilearn.agent.websocket.enabled` 默认 true。总开关关闭时 REST/WS 返回 `AGENT_FEATURE_DISABLED`，WS 子开关关闭时返回 `AGENT_WEBSOCKET_DISABLED`。
- Agent 回合事件必须先把结构化事件持久化到 `turn_events`，再进入每回合有界实时总线；客户端用 `afterSeq` 从 PostgreSQL 重放，不把内存广播当持久化事实源。
- Agent 事件 `metadata` 只允许聚合数字、受控错误码、能力名和工具名；不得写入用户正文、Prompt、原始模型输出、文件名、object key 或 API key。
- 新 Agent 运行时不使用 Redis 作为会话摘要事实源；会话摘要和记忆表均在 PostgreSQL。现有 `spring-data-redis` starter 与 legacy `RedisSessionMemoryStore` 仍保留在仓库，待后续清理 change 处理。
- `learnerId` 只是逻辑范围标识；Phase 8 鉴权/多租户未启动，任何依赖 learnerId 的隔离都不能视为安全边界。
- 资料导入、embedding、生成内容必须有任务状态或可追踪结果，避免不可解释的后台副作用。
- RAG 回答必须受知识库或资料范围约束；证据不足时表达不确定。
- RabbitMQ、MinIO、CommonMark、Tika/PDFBox/POI、LibreOffice/Tesseract、Resilience4j 与 Actuator/Micrometer 是当前 Backend 运行时基线，升级或替换时必须在本文更新版本与验证证据。

### 5.1 当前 Backend 技术基线

| 项目 | 当前基线 |
|---|---|
| 部署形态 | 单个模块化单体 Backend；RabbitMQ listener 与 HTTP API 同应用部署，使用隔离有界线程池，不创建独立 Worker/微服务 |
| 持久化异步消息 | PostgreSQL Transactional Outbox + RabbitMQ 持久化 exchange/queue、publisher confirm、manual ack、at-least-once、retry queue / DLQ |
| 文件资产 | MinIO 私有 bucket；保存原件、阅读版、预览和 OCR 页面，PostgreSQL 只保存资产引用和业务事实 |
| Markdown | CommonMark；renderer 转义 raw HTML、sanitize URL，并生成安全阅读版和结构化 block，不用正则自行解释 Markdown |
| PDF / 通用探测 | Apache Tika + PDFBox；Tika 做格式探测/通用提取，PDFBox 做页数、按页文本与定位 |
| DOC / DOCX | Apache Tika + Apache POI 提取结构；LibreOffice headless adapter 生成高保真预览 |
| OCR | Tesseract adapter；仅处理文本不足页面，独立并发限制与超时 |
| 韧性 | Resilience4j；外部 adapter 和 AI 使用有界 timeout/retry/circuit breaker，消息退避仍由 RabbitMQ retry queue 管理 |
| 可观测性 | Spring Boot Actuator + Micrometer；分层健康以及队列、Outbox、DLQ、阶段、OCR、AI 指标 |
| Agent runtime | `TurnRuntimeService` + 每回合 `TurnEventBus` + PostgreSQL `turn/turn_events/session_message`；虚拟线程执行，终态唯一，重启孤儿恢复 `FAILED_ORPHANED` |
| Agent context/memory | `ContextBuilder` 窗口守卫 + `RollingSessionSummary` PostgreSQL 水位；L1/L2/L3 记忆表、`MemoryTurnRecorder` 在线生产者、snapshot/command 与 `@Scheduled` Consolidator |
| RAG engine | `RagPipeline`/`PipelineFactory`（默认 `pgvector-hybrid`）已接入 RagService/SearchService；`EmbeddingIndexVersionRecorder` 在 embedding 成功后写 ready 版本；`ParseEngineRegistry` 提供统一解析 IR；`SmartRetriever` 可选 |

约束：

- 资料解析、OCR、embedding、知识点和题目生成必须经持久化 ProcessingTask + Outbox + RabbitMQ 执行，不得在 HTTP 请求线程、`@Async` 内存队列或同步 fallback 中运行主流程。
- RabbitMQ listener 与 HTTP API 属于同一 Backend 应用，只允许通过模块 port/adapter 和隔离有界线程池分离；不得把模块边界误写或实现为独立 Worker/微服务。
- PostgreSQL 保存业务事实、Outbox、幂等状态、revision/block 和资产元数据；MinIO 保存大文件二进制，bucket 不公开，object key 不使用用户文件名，API 不泄露永久凭据。
- CommonMark、Tika/PDFBox、POI 是进程内解析依赖；LibreOffice/Tesseract 是受限外部进程 adapter，必须固定参数、禁用宏/脚本/外链、限制资源，并可在超时后终止。
- 不可信 Markdown 必须优先使用 CommonMark renderer 内建的 `escapeHtml(true)`、`sanitizeUrls(true)` 或版本等价配置：raw HTML 转义/禁用；普通链接 scheme allowlist 仅为 `http`、`https`、`mailto`；`javascript`、`data`、`file`、`vbscript` 和未知 scheme 禁止。远程图片/外部资源默认渲染为不带远程 `src` 的占位或替代文本，不自动发起网络请求；受控代理属于后续可选实现，启用时必须有目标 allowlist、回环/私网阻断和重定向复检。只有安全测试证明内建能力不足时，才选择额外 sanitizer 并锁定版本，不默认引入新的大型 sanitizer 依赖。
- Resilience4j 的 adapter 级 timeout/retry/circuit breaker 必须遵守 5.4 的统一矩阵；RabbitMQ retry queue 是跨 ProcessingTask 尝试的唯一退避调度者，SDK 内部 retry 不得再叠加。
- Actuator/Micrometer 的 liveness、HTTP/read readiness 与 processing dependencies health 必须分层。`correlationId`、`taskId`、`materialId` 只进入结构化日志/trace，可作为 exemplar 关联但不得成为 metric tags；metric tags 只允许 `stage`、`outcome`、`dependency`、`queue`、`taskType`、`assetType` 等固定集合，且 `queue`、`taskType`、`assetType` 必须是代码定义的受控枚举。正文、文件名、object key、错误消息、模型原始响应及任何 ID 不得进入 tags。
- Redis 不进入新 Agent 运行时的会话/记忆事实路径；现有 Redis starter 与 legacy session store 只作为待清理历史资产。独立向量库、独立 Worker/微服务仍不引入；RabbitMQ 只承载消息，不能替代 PostgreSQL 业务事实或 Outbox。

### 5.2 资料知识流水线选型与回退

| 选型 | 明确收益 | 未采用替代 | 故障与回退语义 |
|---|---|---|---|
| PostgreSQL Outbox + RabbitMQ | 业务任务与待投递事件同事务提交；持久化消息、publisher confirm、manual ack 和 at-least-once 支持中断恢复 | 同步请求线程、`@Async`/内存队列会阻塞或丢任务；Redis Streams 增加第二套状态语义；独立 Worker/微服务超出当前规模 | RabbitMQ 不可用时 Outbox 保留，恢复后续投；消费者在 ACK 前崩溃时重投并幂等恢复。绝不回退同步处理 |
| MinIO | 本地 Compose 与未来部署保持一致对象语义，支持私有访问、校验、临时对象提升和生命周期清理 | PostgreSQL blob 放大数据库与备份；本地挂载目录难以保持一致对象/访问语义；浏览器直传不适合首轮安全边界 | MinIO 不可用时拒绝新上传或明确退避/失败，不改存其他介质、不返回虚假 READY；已保存资产待恢复后继续读取/处理 |
| CommonMark | 按标准语义解析 Markdown，并可安全生成阅读版/block | 正则或 `File.text()` 不能正确表达 Markdown 结构 | 解析失败使任务明确失败并保留原件，不用纯文本静默冒充成功 |
| Tika + PDFBox | Tika 统一探测 MIME/容器，PDFBox 提供页数、按页文本和稳定定位，支持先文本后按页 OCR | 仅依赖 Tika 难以控制页面级 OCR；浏览器 `file.text()` 不会解析 PDF；要求用户手工转文本破坏体验 | 损坏、伪造或超限文件永久失败；文本不足页进入 OCR，文本充分页不调用 OCR |
| Tika + POI + LibreOffice adapter | Tika/POI 提取 DOC/DOCX 结构，LibreOffice 单独提供高保真预览，阅读语义和视觉预览可独立失败 | 只用 LibreOffice 会把结构提取绑到外部进程；自研 Office renderer 成本过高；云转换引入数据外传 | LibreOffice 超时/熔断时明确预览失败并保留原件；不执行宏、脚本、外链或嵌入对象 |
| Tesseract adapter | 本地可控、无资料外传，适合扫描/混合 PDF 的按页识别 | 全量 OCR 浪费资源且降低文本 PDF 质量；云 OCR 增加隐私、成本和外部依赖 | 默认仅 OCR 文本不足页；最终失败且无足够文本时资料处理失败，但原件可查看、下载和重试，不生成空白 READY |
| Resilience4j | 标准化外部 adapter/AI 的 timeout、retry、circuit breaker，避免自研状态机 | 手写重试/熔断易造成无界等待和策略不一致 | 只做有界调用级保护；任务级延迟重试和 DLQ 由 RabbitMQ/ProcessingTask 负责，熔断时明确失败或退避 |
| Actuator + Micrometer | 使用 Spring 原生健康和指标生态，统一分层健康、延迟、失败、重试和积压观测 | 只看日志无法量化队列积压和恢复；自研监控端点增加维护成本 | RabbitMQ 故障只使 processing health 降级，不应误杀 HTTP liveness 或已完成资料读取；指标不得泄露敏感/高基数内容 |

解析库和 Spring 集成依赖版本与 Spring Boot 3.5.x/JDK 21 兼容并在 `services/api/pom.xml` 锁定；LibreOffice/Tesseract 镜像或系统包版本在 Backend 运行镜像中固定。不得使用浮动 `latest` 作为可复现基线。

### 5.3 默认配置与环境变量覆盖

下表是当前实现采用的配置。

| 配置 | 环境变量 | 默认值 / 约束 |
|---|---|---|
| 异步资料处理 | `SUILEARN_ASYNC_PROCESSING_ENABLED` | `true`；设为 `false` 时禁用新文件上传，绝不同步 fallback |
| OCR | `SUILEARN_OCR_ENABLED` | `true`；仅文本不足页面触发 |
| 最大文件 | `SUILEARN_MAX_FILE_SIZE_MB` | `50`；超限永久拒绝 |
| PDF 最大页数 | `SUILEARN_PDF_MAX_PAGES` | `500`；超限永久拒绝 |
| 文档处理并发 | `SUILEARN_PROCESSING_CONCURRENCY` | `2`；隔离有界 consumer executor |
| OCR 并发 | `SUILEARN_OCR_CONCURRENCY` | `1`；独立于普通文档并发 |
| 原件保留 | `SUILEARN_RETAIN_ORIGINAL` | `true`；设为 `false` 时禁用新上传，既有资产仍保留，不能接受上传后丢弃原件 |
| 知识点自动生成 | `SUILEARN_KNOWLEDGE_POINT_AUTO_GENERATION_ENABLED` | `true`；AI 不可用时任务失败/不可用，不生成 fallback |
| RabbitMQ 连接 | `SUILEARN_RABBITMQ_HOST`、`SUILEARN_RABBITMQ_PORT`、`SUILEARN_RABBITMQ_USERNAME`、`SUILEARN_RABBITMQ_PASSWORD`、`SUILEARN_RABBITMQ_VHOST` | 环境覆盖；凭据无生产默认值，不写日志/响应 |
| MinIO 连接 | `SUILEARN_MINIO_ENDPOINT`、`SUILEARN_MINIO_ACCESS_KEY`、`SUILEARN_MINIO_SECRET_KEY`、`SUILEARN_MINIO_BUCKET` | 环境覆盖；bucket 私有，凭据无生产默认值，不暴露给客户端 |
| Agent 总开关 | `SUILEARN_AGENT_ENABLED` | `false`；关闭时 Agent REST/WS 返回 `AGENT_FEATURE_DISABLED` |
| Agent WS 子开关 | `SUILEARN_AGENT_WEBSOCKET_ENABLED` | `true`；关闭时 WS 返回 `AGENT_WEBSOCKET_DISABLED` |
| Agent 循环预算 | `SUILEARN_AGENT_MAX_STEPS`、`SUILEARN_AGENT_SUBAGENT_MAX_STEPS`、`SUILEARN_AGENT_MAX_TOOL_CALLS` | `4` / `3` / `8`；配置绑定 fail-fast 校验 |
| Agent 回合超时 | `SUILEARN_AGENT_RUN_TIMEOUT` | `90s`；REST 同步等待与回合 deadline 共用语义 |
| Agent 上下文 | `SUILEARN_AGENT_CONTEXT_MAX_TOKENS` | `12000`；`ContextBuilder` 按 0.35 历史预算守卫 |
| Agent 会话 | `SUILEARN_AGENT_SESSION_TTL`、`SUILEARN_AGENT_SESSION_MAX_TURNS` | `24h` / `20` |
| Agent 记忆 | `SUILEARN_AGENT_MEMORY_TOP_K`、`SUILEARN_AGENT_MEMORY_MIN_CONFIDENCE` | `5` / `0.80` |

根 `.env.example` 只承载非敏感本地默认值。配置绑定必须 fail-fast 校验非法数值，生产凭据必须由部署环境注入。RabbitMQ/MinIO 凭据缺失、OCR/LibreOffice 不可执行或 AI 未配置时，系统必须暴露真实健康/任务状态，不能静默改走其他实现。

### 5.4 timeout / retry / circuit breaker 配置矩阵

| 配置语义 | 环境变量 | 默认值 | 合法范围与单位 |
|---|---|---|---|
| ProcessingTask 最大尝试 | `SUILEARN_PROCESSING_MAX_ATTEMPTS` | `3`（含首次） | 整数 `1..3`；只能降低，最终一次失败后进入 DLQ/FAILED |
| RabbitMQ 短退避 | `SUILEARN_RABBITMQ_RETRY_SHORT_DELAY_MS` | `30000`（30 秒） | `1000..300000` ms；允许覆盖 |
| RabbitMQ 长退避 | `SUILEARN_RABBITMQ_RETRY_LONG_DELAY_MS` | `300000`（5 分钟） | `30000..3600000` ms；允许覆盖，且必须大于短退避 |
| Parser 单文档超时 | `SUILEARN_PARSER_TIMEOUT_MS` | `120000`（2 分钟） | `5000..600000` ms；覆盖 CommonMark/Tika/PDFBox/POI 单次解析 |
| OCR 单页超时 | `SUILEARN_OCR_TIMEOUT_MS` | `60000`（1 分钟） | `5000..300000` ms；超时必须终止对应 Tesseract 进程 |
| LibreOffice 单文档超时 | `SUILEARN_LIBREOFFICE_TIMEOUT_MS` | `120000`（2 分钟） | `10000..600000` ms；超时必须终止进程树并清理临时文件 |
| AI 单调用超时 | `SUILEARN_AI_TIMEOUT_MS` | `30000`（30 秒） | `1000..120000` ms；超时计为暂时错误，不保存不完整结果 |
| Adapter 即时重试次数 | `SUILEARN_ADAPTER_MAX_RETRIES` | `0` | 整数 `0..1`；Resilience4j 是唯一 retry owner，SDK 内部 retry 必须禁用或计入同一上限 |
| 熔断失败率阈值 | `SUILEARN_CIRCUIT_BREAKER_FAILURE_RATE_PERCENT` | `50` | 整数 `10..100`，百分比；仅暂时性依赖失败计入，文件校验等永久错误排除 |
| 熔断滑动窗口 | `SUILEARN_CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE` | `10` | 整数 `5..100`，按调用计数 |
| 熔断最小调用数 | `SUILEARN_CIRCUIT_BREAKER_MINIMUM_CALLS` | `5` | 整数 `1..slidingWindowSize` |
| OPEN 等待时间 | `SUILEARN_CIRCUIT_BREAKER_OPEN_STATE_MS` | `60000`（1 分钟） | `1000..600000` ms |
| HALF_OPEN 探测调用数 | `SUILEARN_CIRCUIT_BREAKER_HALF_OPEN_CALLS` | `2` | 整数 `1..10` |

RabbitMQ 退避顺序固定为：首次失败后使用短退避，第二次失败后使用长退避，第三次失败后进入 DLQ/FAILED；若最大尝试被降低，对应后续退避不执行。消息/阶段幂等键 `taskId + stage + revision/processingVersion` 只防止消息重投重复提交阶段业务结果，不得代替 adapter operation key。重试计数单元是一个可幂等恢复的 adapter operation，而不是整个 stage：单次文档解析、单页 OCR、单次预览和单次 AI logical request 分别使用稳定输入与对应 adapter/model version 构造 operation key。

单个 adapter operation 的外部调用硬上限为 `ProcessingTask 最大尝试 × (Adapter 即时重试次数 + 1)`：默认 `3 × 1 = 3` 次，所有合法覆盖组合最多 `3 × 2 = 6` 次。文档级 OCR 的理论调用上限为 `待 OCR 页数 × ProcessingTask 最大尝试 × (Adapter 即时重试次数 + 1)`；因此 500 个待 OCR 页面对应默认最多 1500 次、所有合法覆盖组合最多 3000 次理论调用，不得用单 operation 的 3/6 次上限限制整份 PDF 页数。

PostgreSQL 持久化 `ProcessingOperation` 的唯一 operationKey、task/stage、状态、跨任务尝试和重启累计的 attemptCount、resultReference、adapterVersion、timestamps 与脱敏 error。OCR key 为 `revisionId + pageNumber + ocrAdapterVersion`；parser、preview、AI key 的稳定输入/version 规则以 capability spec 和源码为准。每个 operation 在调用 adapter 前先原子 claim/查询持久化结果；已成功持久化的 OCR 页面、解析 revision、预览或 AI 结果在 ProcessingTask 重试和重启恢复时必须复用，只重新调度未完成、租约过期或可重试失败的 operation。若实现批处理，必须以 `revision + ordered page set/batch index + adapter version` 定义稳定幂等键，并清楚记录页/批次完成状态；批次设计不能降低 500 页 PDF 验收上限。不得同时启用 Resilience4j、Provider SDK 和自定义循环的独立 retry 计数器。

#### 5.4.1 旧 AI retry 配置迁移

`SUILEARN_ADAPTER_MAX_RETRIES` 是当前流水线唯一的 canonical adapter 即时重试配置，应用默认值为 `0`，合法范围为整数 `0..1`。`SUILEARN_AI_MAX_RETRIES` 处于 deprecated 兼容周期。根 `.env.example` 只记录 `SUILEARN_ADAPTER_MAX_RETRIES=0`，不提供旧键或旧默认 `2`；Compose 对两个键都只做无默认值可选透传。Compose 不为新键使用 `${SUILEARN_ADAPTER_MAX_RETRIES:-0}` 或等价默认注入。缺失时不合成非空值，空字符串统一按未显式提供处理。

应用配置按新旧键的非空显式存在进行绑定、legacy 映射与冲突诊断，AI Provider SDK 内部 retry 和旧手写 retry 已关闭，Resilience4j 是唯一 adapter retry owner。用户没有非空显式覆盖时有效 adapter retry 为 `0`，不继承旧默认 `2`。

| 启动输入 | 当前行为与诊断 |
|---|---|
| 新旧键均缺失或为空 | 使用应用默认 `0`；Compose 空值透传不算显式提供 |
| 仅非空提供 `SUILEARN_ADAPTER_MAX_RETRIES` | 按 `0..1` 校验并使用；非法值 fail-fast |
| 仅非空提供 deprecated `SUILEARN_AI_MAX_RETRIES` | 兼容映射 `0 -> 0`、任意正整数 `-> 1`；负数或非整数 fail-fast；启动记录 `SUILEARN_RETRY_CONFIG_LEGACY_MAPPED` 诊断，明确旧值与有效值。旧值 `2` 因而最多产生每 operation `3 × 2 = 6` 次理论调用，不会形成 9 次 |
| 新旧键同时非空提供 | 无论值是否相同均 fail-fast，诊断码 `SUILEARN_RETRY_CONFIG_CONFLICT`，要求删除 deprecated 旧键；不采用静默优先级 |

当前兼容周期继续保留 legacy 映射。兼容周期后的第一个具名 removal change 必须提供完整 tombstone 错误窗口；只有后续 cleanup change 在残留扫描和运行态证据确认无 legacy 输入后，才能同时删除 Compose 旧键透传和 Backend removed-key detector。以上迁移只影响 retry 配置，不改变 RabbitMQ/MinIO/AI 凭据无生产默认值的规则。

所有范围、相互关系和时长单位在配置绑定时 fail-fast 校验；不得夹取非法值后继续启动。上述业务/韧性参数允许覆盖，RabbitMQ/MinIO/AI 凭据仍无生产默认值，只能由部署环境注入。

推荐验证：

```powershell
mvn -f services/api/pom.xml test -q
docker compose config
```

RabbitMQ、MinIO、LibreOffice 或 Tesseract 相关变更必须在 Compose/Testcontainers 中验证中断恢复、重复投递、DLQ、临时对象清理、外部进程超时终止和 Actuator/Micrometer 分层健康；静态配置检查或单元测试不能替代运行态证据。

Markdown 安全测试至少覆盖 raw `<script>`/事件属性被转义，大小写或编码混淆的 `javascript:` 以及 `data:`、`file:`、`vbscript:`/未知 scheme 被拒绝，`http`/`https`/`mailto` 普通链接保留，远程图片默认不产生可加载的 `src` 或网络请求；若启用受控代理，还必须覆盖回环、私网与重定向绕过。

## 6. Web Frontend 基线

| 项目 | 当前版本 / 约束 |
|---|---|
| 目录 | `apps/web` |
| Framework | React 19.0.0 |
| Language | TypeScript 5.7.3 |
| Build | Vite 6.0.7 |
| React plugin | `@vitejs/plugin-react` 5.0.4 |
| Icons | `lucide-react` 0.475.0 |
| Node typings | `@types/node` 22.10.2 |
| API base | `VITE_API_BASE_URL`，默认 `/api/v2` |

Web 约束：

- 当前 Web 是知识库工作台，不承载完整刷题学习端。
- Web API client 必须集中在 `apps/web/src/api.ts`，共享类型集中在 `apps/web/src/types.ts`。
- 前端类型应与 OpenAPI 和服务端 DTO 保持一致；不为局部便利私自改变契约语义。
- 不引入复杂状态管理库；React 本地状态足够时不加 Zustand、Redux 或 TanStack Query。
- 不引入大型 UI 组件库，除非已有页面复杂度证明收益明确。

推荐验证：

```powershell
npm --prefix apps/web run build
```

## 7. Contracts 基线

| 项目 | 当前约定 |
|---|---|
| OpenAPI | `contracts/openapi/suilearn-v2.yaml` |
| JSON Schema | `contracts/schemas/**` 预留 |
| Owner | 架构 Agent |

Contracts 约束：

- 契约变更必须先由架构 Agent 完成，再派发 Backend、Android、Web 或 Content 的适配任务。
- 契约变更与消费端适配不得并行写代码。
- 服务端 Controller / DTO、Web `api.ts` / `types.ts`、Android 远程模型必须围绕同一契约对齐。

## 8. 暂不引入

| 类型 | 暂不引入项 | 触发重新评估的条件 |
|---|---|---|
| 平台 | iOS、Flutter | 产品明确需要跨平台移动端 |
| 账号 | 登录、账号、云同步、多租户权限 | 产品规格进入同步或多人场景 |
| Android | Hilt、多 Gradle module、SQLite FTS | 手动注入或 Room 查询成为明确瓶颈 |
| Backend | 新 Agent 路径引入 Redis、独立 Worker/微服务、Milvus 等独立向量库、浏览器直传、云 OCR/Office 转换 | 只有目标 RabbitMQ listener、MinIO 和本地受限 adapter 已实现、验证，且观测证明它们无法满足扩缩容、隔离或质量要求时，才通过新 change 评估；既有 Redis starter 仅作为 legacy 待清理项 |
| Web | 完整刷题学习端、复杂状态管理、大型组件库 | Web 工作台之外的学习端进入当前规格 |
| AI | 多 Provider 路由、成本平台、模型评测系统、Spring AI starter/LangChain4j | 单 Provider 抽象不足以支撑已确认运营需求，或当前 `agent/llm` OpenAI-compatible 客户端经验证无法覆盖目标模型 |

## 9. 升级规则

任何技术或版本升级必须满足：

- 由架构 Agent 更新本文。
- 说明升级原因、影响范围、替代方案和回退方式。
- 同步修改对应配置文件，例如 `build.gradle.kts`、`apps/android/build.gradle.kts`、`services/api/pom.xml`、`apps/web/package.json`。
- 运行受影响模块的验证命令，并在交付中粘贴原始结果。
- 如果升级影响契约或跨端模型，先更新 `contracts/**`，再派发消费端适配。

常见升级门禁：

| 升级项 | 必跑验证 |
|---|---|
| AGP / Kotlin / Compose / Room | `.\gradlew.bat :app:testDebugUnitTest --no-daemon` + `.\gradlew.bat :app:assembleDebug --no-daemon` |
| Java / Spring Boot / JPA | `mvn -f services/api/pom.xml test -q` |
| RabbitMQ / MinIO / Outbox | `mvn -f services/api/pom.xml test -q` + `docker compose config` + 中断恢复、重复投递、DLQ、对象补偿运行态矩阵 |
| Tika / PDFBox / POI / LibreOffice / Tesseract | 格式语料测试 + `mvn -f services/api/pom.xml test -q` + 外部进程超时/资源限制运行态验证 |
| Resilience4j / Actuator / Micrometer | `mvn -f services/api/pom.xml test -q` + 熔断/重试/分层健康和指标验证 |
| Spring AI starter / model adapter | `mvn -f services/api/pom.xml test -q`，并检查业务模块无 Spring AI 类型 import |
| React / TypeScript / Vite | `npm --prefix apps/web run build` |
| OpenAPI 契约 | 契约 diff 审查 + Backend/Web/Android 相关适配测试 |
