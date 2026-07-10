# 随心学 SuiLearn 当前架构

## 0. 文档职责

本文是 SuiLearn 当前代码结构、模块边界、数据流和契约关系，以及已批准目标架构基线的真相源，由架构 Agent 维护。

本文默认描述已实现的当前事实；标有“已批准 Build 目标”的内容是 active change 的规范性实施目标，不表示代码、契约、依赖、配置或运行态已经落地。旧架构、废弃方案和历史取舍通过 Git 历史追溯；未来架构变更先进入 `openspec/changes/<change-name>/**`，批准并实现、验证后再转为未标注的当前事实。

`build-resilient-knowledge-pipeline` 的实时实施状态以 `openspec/changes/build-resilient-knowledge-pipeline/tasks.md` 为准，验证状态以同目录 `verification.md` 为准；本文不复制会过期的任务进度。目标能力只有在对应任务完成、运行态证据已记录且 Review 闭环后，才可改写为未标注的当前事实。

本文回答：

- 当前代码应该放在哪些目录和 package。
- Android、Backend、Web、Contracts 各自负责什么。
- 数据如何在本地、服务端和 Web 工作台之间流转。
- 哪些边界不能由单个实现 Agent 私自改变。

本文不回答：

- 技术栈版本、最低版本、升级规则和暂不引入项。这些由 `docs/tech-selection.md` 维护。
- 产品范围、验收标准和阶段优先级。这些由 `docs/product-requirements.md` 维护。
- 具体 API 字段细节。稳定接口以 `contracts/openapi/suilearn-v2.yaml` 为准。

## 1. 总体结构

当前 SuiLearn 包含四个主要工程面：

```text
SuiLearn
├─ apps/android          Android 本地学习 App
├─ services/api          Java Spring Boot API
├─ apps/web              React Web 知识库工作台
└─ contracts             跨端 API 契约
```

职责边界：

| 范围 | Owner | 职责 |
|---|---|---|
| `apps/android/**` | Android Agent | 本地学习闭环、Android UI、ViewModel、本地域模型、Room、本地题库导入、远程能力轻量入口 |
| `services/api/**` | Server Backend Agent | 知识库、资料导入、AI 生成、RAG、搜索、任务状态、服务端持久化 |
| `apps/web/**` | Web Frontend Agent | 知识库工作台、资料导入、生成确认、语义搜索、资料问答 |
| `contracts/**` | 架构 Agent | OpenAPI、JSON schema 预留、跨端模型语义 |
| `docs/product-requirements.md` | 产品 Agent | 当前产品规格 |
| `docs/tech-selection.md` | 架构 Agent | 技术和版本基线 |

核心原则：

- Android 本地学习闭环必须离线可用，不依赖 Backend 或 Web。
- Backend 当前承载 AI、知识库、资料导入、RAG 和跨端持久化。
- 已批准 Build 目标继续保持一个模块化单体 Backend；计划中的 RabbitMQ listener 与 HTTP API 同应用部署，并使用隔离的有界线程池。目标章节中的模块、port 和 adapter 均不是独立微服务或单独部署的 Worker。
- Web 当前是知识库工作台，不是完整学习端。
- Contracts 是跨端 API 单点真相，消费端不得为局部便利私自改变契约语义。
- 技术版本和依赖升级不写在本文，统一回到 `docs/tech-selection.md`。

## 2. Android 当前结构

Android module 名称为 `:app`，物理目录为 `apps/android`。

当前代码同时使用 `src/main/java` 和 `src/main/kotlin`，package 均为 `com.suilearn`。新增 Kotlin 代码优先放在 `src/main/kotlin`；已有文件保持原位置，除非任务明确要求迁移。

```text
apps/android/src/main/
├─ AndroidManifest.xml
├─ assets/
│  └─ question_pack_java_interview.json
├─ java/com/suilearn/
│  ├─ MainActivity.kt
│  ├─ SuiLearnApp.kt
│  ├─ data/
│  ├─ di/
│  ├─ feature/
│  ├─ navigation/
│  ├─ theme/
│  ├─ ui/
│  └─ viewmodel/
└─ kotlin/com/suilearn/
   ├─ App.kt
   └─ core/
      ├─ common/
      ├─ database/
      ├─ importer/
      ├─ model/
      ├─ remote/
      ├─ repository/
      └─ usecase/
```

### 2.1 Android 分层

| 层 | 目录 | 职责 |
|---|---|---|
| App entry | `MainActivity.kt`、`SuiLearnApp.kt`、`App.kt` | 应用入口、主题挂载、全局初始化 |
| Navigation | `navigation/**` | 页面路由定义和 NavHost |
| Feature UI | `feature/*/**` | 页面、UiState、Event、ViewModel |
| Shared UI | `ui/**`、`theme/**` | 复用组件、UI model、主题 |
| DI | `di/AppDependencies.kt` | 手动装配 Repository、UseCase、数据库和远程入口 |
| Domain model | `core/model/**` | 学习包、题目、练习、进度、搜索等本地域模型 |
| UseCase | `core/usecase/**` | 练习构建、提交答案、统计等业务编排 |
| Repository | `core/repository/**` | 本地和远程数据访问抽象及实现 |
| Database | `core/database/**` | Room database、DAO、Entity、类型转换 |
| Importer | `core/importer/**`、`data/**` | assets 题库读取、JSON 解析、导入 Room |
| Remote | `core/remote/**` | 后端 API client 和网络 DTO |

### 2.2 Android Feature 边界

```text
feature/
├─ ai               AI / 知识库远程能力入口
├─ categories       分类列表和分类练习入口
├─ common           Feature 间复用的题目摘要展示
├─ favorites        收藏题目
├─ home             首页、当前学习包、今日概览和入口
├─ knowledge        知识点详情和专项练习入口
├─ practice         练习会话、答题、解析、自评
├─ search           本地搜索
├─ settings         设置、重置等
├─ statistics       本地学习统计
└─ wrongbook        错题本和错题复盘
```

规则：

- Feature 层通过 ViewModel 消费 UseCase 或 Repository，不直接访问 Room DAO。
- UI 状态使用明确的 UiState / Event 类型承载；不要把数据库 Entity 直接暴露给 Composable。
- `feature/common` 只放跨 Feature UI 复用逻辑，不放业务规则。
- `feature/ai` 只能作为远程能力入口和状态展示，不承接完整知识库工作台。

### 2.3 Android 本地数据流

```text
assets/question_pack_java_interview.json
  -> AssetQuestionPackSource
  -> QuestionPackJsonParser
  -> QuestionPackRoomImporter
  -> Room Entity / DAO
  -> Repository
  -> UseCase
  -> ViewModel
  -> Compose Screen
```

本地事实源：

- 题库内容来自 assets JSON，导入 Room 后供学习流程读取。
- 用户数据来自 Room，包括答题记录、错题状态、收藏状态和统计所需原始数据。
- 正确率、进度、薄弱知识点等统计以原始记录计算或派生，不写死占位值。

题库和用户数据规则：

- 题目、分类、知识点 ID 必须稳定。
- 更新题库不得覆盖用户答题记录、错题状态或收藏状态。
- 简答题在用户自评“通过 / 未通过”后写入答题记录；未通过进入错题状态。
- 题库 JSON schema 和 Room schema 的版本策略见 `docs/tech-selection.md`。

### 2.4 Android 远程能力

Android 远程能力位于：

```text
core/remote/
core/repository/AiKnowledgeRemoteRepository.kt
feature/ai/
```

规则：

- Android 通过后端 API 消费 AI、任务状态和知识库能力。
- 未配置服务端、服务端不可用或 AI Provider 不可用时，本地刷题、错题、收藏、统计和搜索仍必须可用。
- Android 不直接存储 API key，不直接调用模型厂商接口。
- Android 网络模型应与 `contracts/openapi/suilearn-v2.yaml` 和服务端 DTO 对齐。

## 3. Backend 当前结构

Backend 位于 `services/api`，根 package 为 `com.suilearn.api`。

```text
services/api/src/main/java/com/suilearn/api/
├─ SuiLearnApiApplication.java
├─ ai/
├─ config/
├─ controller/
├─ dto/
├─ generation/
├─ knowledgebase/
├─ knowledgepoint/
├─ material/
├─ model/
├─ pack/
├─ persistence/
│  ├─ entity/
│  └─ repository/
├─ rag/
├─ retrieval/
├─ search/
├─ source/
└─ service/
```

### 3.1 Backend 分层

| 层 | 目录 | 职责 |
|---|---|---|
| Application entry | `SuiLearnApiApplication.java` | Spring Boot 启动入口 |
| Controller | `controller/**` | REST API 入参、响应、HTTP 状态和异常边界 |
| DTO | `dto/**` | 请求 DTO，避免 Controller 暴露内部 Entity |
| Application Service | `<module>/application/**` | 按领域承载用例边界，例如知识库、资料、知识点、生成内容、RAG、搜索和任务 |
| Domain model | `model/**` | 服务端领域模型、状态枚举、结果模型 |
| Module Infrastructure | `<module>/infrastructure/**` | 按聚合承载 Store / adapter 边界，供 Application Service 访问持久化或外部能力 |
| Compatibility | `service/SuiLearnV2Service`、`service/internal/SuiLearnV2Workflow`、`persistence/SuiLearnV2Store` | V2 兼容 facade 和底层兼容持久化 facade，供旧入口留存，不作为新增业务入口 |
| Persistence | `persistence/**` | JPA Entity、Repository 和兼容持久化 facade |
| AI | `ai/**` | SuiLearn AI port、Fake Provider、OpenAI-compatible Provider、Spring AI adapter 边界 |
| Retrieval | `retrieval/**` | 检索接口、关键词检索、Embedding Provider |
| Material | `material/**` | 资料解析、切片 |
| Config | `config/**` | CORS、AI Provider、应用配置 |

规则：

- Controller 不写业务规则，只做 HTTP 边界和参数转发。
- Application Service 负责编排业务流程、校验跨实体规则、生成任务和返回领域结果。
- Persistence 层负责 Entity 与数据库访问，不把 JPA Entity 直接暴露给 Controller 或 Web。
- AI 生成、RAG、embedding 和检索都必须经由明确接口，业务层不直接依赖厂商 SDK。
- `SuiLearnV2Service` 只保留为兼容 facade；新增 Controller 依赖应指向对应领域 Application Service。
- `SuiLearnV2Workflow` 仅随 `SuiLearnV2Service` 兼容 facade 留存；`SuiLearnV2Store` 是底层兼容持久化 facade，后续新增逻辑不得继续扩展为总管类。
- 新增业务不得直接依赖兼容 facade；业务编排必须进入模块 Application Service，持久化访问必须进入模块 `infrastructure` Store / adapter。
- Application Service 不得直接注入或 import `SuiLearnV2Store`；如需访问旧持久化能力，必须通过本模块 Store / adapter 包装。
- 长任务状态创建、启动、成功、失败和查询统一进入 `task` 模块；业务流程通过 `TaskService` / `TaskExecutor` 表达任务生命周期。
- `SourceRef` 标准化、可用性校验、引用构造和资料删除影响标记统一进入 `source` 模块。

### 3.1.1 Backend 模块边界

当前后端采用模块化单体，模块内优先使用统一边界：

```text
<module>/
├─ api/               Controller / API DTO mapper（按需）
├─ application/       UseCase / Application Service
├─ domain/            领域模型、状态、策略、契约
└─ infrastructure/    Store、Repository Adapter、外部适配
```

当前模块职责：

| 模块 | 职责 |
|---|---|
| `knowledgebase` | 知识库 CRUD、详情、统计和知识库内问题查询边界；通过 `KnowledgeBaseStore` 访问知识库聚合 |
| `material` | 资料导入、资料查询、删除策略和 chunk 存取边界；导入流程使用 `TaskExecutor` 管理导入和 embedding 任务 |
| `knowledgepoint` | 知识点抽取、查询、编辑和删除边界；通过 `KnowledgePointStore` 访问知识点聚合 |
| `generation` | 生成内容草稿、审核、保存、丢弃、AI 笔记和题型契约边界；通过 `GeneratedContentStore`、`QuestionStore`、`AiNoteStore` 协作 |
| `rag` | 资料问答、证据约束和不确定性表达边界；只通过检索接口读取证据 |
| `search` | 关键词 / 语义搜索入口边界；查询范围必须由知识库或资料约束 |
| `task` | 长任务状态查询、执行模板和任务持久化边界 |
| `source` | `SourceRef` 标准化、来源可用性、引用构造和删除影响规则边界 |
| `pack` | `LearningPack` 服务层抽象；当前通过 `packId == knowledgeBaseId` 适配现有知识库 |
| `ai` | SuiLearn 自有 AI port 与基础设施 adapter，业务模块不得直接依赖厂商 SDK 或 Spring AI 类型 |

依赖方向：

```text
controller -> <module>.application -> domain / port
application -> <module>.infrastructure Store / adapter
infrastructure -> JPA repository / external implementation
```

已批准目标要求 `/api/v2/*` 兼容演进：先稳定 multipart/202、资产/revision、结构化知识点和题目草稿契约，Backend 才能做增量 DB schema 迁移；具体顺序和完成状态以 active change 的 `tasks.md`/`verification.md` 为准。旧 JSON 文本导入保留一个兼容周期并标记 deprecated，现有资料、知识点和题目不得丢失。

### 3.1.2 已批准 Build 目标：资料知识流水线逻辑边界

以下内容是 active change 的目标模块边界；其落地与验证状态以 `tasks.md`/`verification.md` 为准，不能仅凭本节推断当前 package、依赖或运行进程。实现后这些边界全部位于同一个 `services/api` Backend 中，通过 application/domain port 协作，基础设施实现留在 `infrastructure`；不得据此新建微服务、独立 Worker 项目或第二套部署单元。

| 逻辑边界 | 归属 | 职责与依赖方向 |
|---|---|---|
| Material API | `material` application/api | 校验 multipart 元数据并流式保存原件；原件、Material、ProcessingTask 与 Outbox 提交后返回 `202`，不在请求线程解析、OCR、索引或调用 AI |
| Asset Storage | `material` domain port + infrastructure adapter | 以 MinIO adapter 保存私有 `ORIGINAL`、`READING`、`PREVIEW`、`OCR_PAGE` 资产；使用临时 object key、校验、提升与孤儿/删除清理，业务层不感知 MinIO SDK |
| Processing Orchestrator | `task` application/infrastructure | 在 PostgreSQL 事务内创建任务与 Outbox；发布 RabbitMQ 持久化消息，驱动阶段状态、幂等、重试、死信和重启恢复 |
| Document Parser | `material` domain port + infrastructure adapter | CommonMark 处理 Markdown，并通过 renderer 内建 HTML 转义与 URL sanitize 生成安全阅读版；Tika 负责格式探测/通用提取，PDFBox 负责 PDF 页数、按页文本与定位，POI 负责 DOC/DOCX 结构提取 |
| Office Preview Adapter | `material` infrastructure | 以受限 LibreOffice headless 外部进程生成高保真预览；参数固定、资源受限、可超时终止，不能执行宏、脚本或外部嵌入对象 |
| OCR Adapter | `material` infrastructure | 以受限 Tesseract 外部进程仅处理文本不足页面，并把结果合并回对应 page/block；每个 page/revision/OCR version 是独立幂等 operation，已成功持久化的页面在任务恢复时直接复用；与普通文档处理分开限流 |
| Normalizer / Indexer | `material` / `retrieval` | 生成稳定顺序、章节和页码映射的 DocumentBlock，再写入检索索引；解析产物不直接耦合检索实现 |
| Knowledge Point | `knowledgepoint` | 从当前 revision 的证据异步生成结构化 DRAFT；审核、来源定位和 revision 过期语义由本模块管理 |
| Question Generation | `generation` | 只接受 CONFIRMED 知识点，异步生成带证据的待审核题目草稿；失败不改变资料或知识点状态 |

目标部署中 RabbitMQ listener 使用与 HTTP executor 完全隔离的有界线程池；文档处理默认并发 2，OCR 默认并发 1。只有对应实现任务完成且运行态证据记入 `verification.md` 后，listener 和线程池才可视为当前运行事实。将 listener 移到独立进程、拆分 Worker 或微服务属于未来架构变更，不是本 change 的目标部署方式。

### 3.2 Backend Controller 边界

当前主要 Controller：

| Controller | 依赖边界 | 职责 |
|---|---|---|
| `KnowledgeBaseController` | `KnowledgeBaseService`、`MaterialImportService`、`MaterialQueryService`、`KnowledgePointService` | 知识库 CRUD、资料导入、资料详情、知识点、题目和统计 |
| `AiGenerationController` | `GeneratedContentService` | AI 生成题、解释、复习建议、生成内容审核、AI 笔记保存 |
| `RagController` | `RagService` | 知识库或资料范围内问答 |
| `SearchController` | `SearchService` | 语义/关键词搜索入口 |
| `TaskController` | `TaskService` | 任务状态查询 |
| `AiProviderController` | `AiProviderStatusService` | AI Provider 脱敏状态 |
| `ApiExceptionHandler` | Spring MVC exception boundary | API 异常统一响应 |

规则：

- Controller 不直接依赖 `SuiLearnV2Service` 或 `SuiLearnV2Workflow`。
- Controller 不直接依赖 JPA repository、JPA Entity 或 `SuiLearnV2Store`。
- 新增 `/api/v2/*` 端点时，先选择或创建对应模块 Application Service，再由 Controller 转发。

### 3.3 Backend 持久化模型

当前持久化围绕知识库工作台展开：

```text
KnowledgeBase
  ├─ LearningMaterial
  │  └─ MaterialChunk
  ├─ KnowledgePoint
  ├─ Question
  ├─ GeneratedContent
  ├─ AiNote / AiNoteDraft
  └─ TaskStatus
```

关键规则：

- `KnowledgeBase` 是资料、问答、搜索和生成内容的隔离边界。
- `LearningMaterial` 保留标题、来源、导入状态和内容元数据。
- `MaterialChunk` 是检索和 RAG 的资料片段单位，可携带 embedding 状态。
- `GeneratedContent` 在用户确认前不得进入正式题库。
- `TaskStatus` 记录资料导入、embedding、生成等任务状态，避免不可追踪副作用。
- 删除资料时，待确认内容和已保存内容的处理策略必须显式表达。
- 后端长文本和 JSON 字符串字段使用普通数据库 `text` 列；旧 PostgreSQL Large Object (`oid`) 列由后端启动迁移转换为 `text`，避免运行时依赖 LOB stream。
- 模块 Application Service 通过本模块 `infrastructure` Store / adapter 访问持久化；不得直接注入 `SuiLearnV2Store`。
- `SuiLearnV2Store` 只作为底层兼容持久化 facade 被模块 Store / adapter 或兼容层使用，后续新增聚合访问优先补充对应模块 Store 方法。
- Entity/domain mapper 优先归属模块 `infrastructure` 边界；不新增全局大 mapper 包。

### 3.3.1 已批准 Build 目标：持久化模型与异步数据流

本节定义 active change 的目标模型；表/字段、中间件接入和迁移证据的实时状态以 `tasks.md`/`verification.md` 为准，不能仅凭目标模型推断当前数据库或运行依赖。目标持久化关系为：

```text
KnowledgeBase
  ├─ LearningMaterial
  │  ├─ MaterialAsset (ORIGINAL / READING / PREVIEW / OCR_PAGE -> MinIO object reference)
  │  ├─ DocumentRevision
  │  │  └─ DocumentBlock
  │  └─ MaterialChunk / retrieval index
  ├─ KnowledgePoint
  ├─ Question
  ├─ GeneratedContent
  ├─ AiNote / AiNoteDraft
  ├─ ProcessingTask / TaskStatus
  └─ OutboxEvent
```

目标规则：PostgreSQL 保存 Material、资产元数据、不可变 revision/block、结构化知识点、任务、Outbox 和幂等事实；MinIO 保存原件和衍生二进制，数据库不保存大文件正文二进制。`MaterialAsset` 只保存不可猜测的 object key、资产类型、MIME、大小、校验值和生命周期元数据；bucket 保持私有，API 不泄露永久凭据或内部 object key。每次成功处理创建新的不可变 `DocumentRevision` 与有序 `DocumentBlock`，重新处理不覆盖旧 revision；知识点和题目引用固定到生成时的 revision/page/block/excerpt。

在 active change 记录的契约与对应实现门禁满足后，目标数据流为：

```text
Web multipart upload
  -> Material API streams ORIGINAL to private MinIO
  -> one PostgreSQL transaction writes Material + ProcessingTask + OutboxEvent
  -> API returns 202 materialId/taskId
  -> Outbox publisher uses publisher confirm to RabbitMQ durable exchange/queue
  -> same Backend RabbitMQ listener (isolated bounded executor)
  -> parse / optional page OCR / normalize / index / knowledge point or question generation
  -> commit derived asset metadata + revision/block/domain result
  -> manual ACK
```

队列按 `document.processing`、`knowledge-point.generation`、`question.generation` 隔离，每类使用短/长两级 retry queue 和 dead-letter queue。投递语义为至少一次；消费者以 `taskId + stage + documentRevision/processingVersion` 作为幂等键，并在数据库结果与资产元数据提交后 ACK。永久错误直接失败，暂时错误有界退避；timeout、retry、circuit breaker 的目标默认值、覆盖口、合法范围和总调用上限统一以 `docs/tech-selection.md` 的韧性配置矩阵为准，禁止在架构文档复制参数形成漂移。

PostgreSQL 事务与 MinIO 不能原子提交，因此衍生资产先写临时 key，校验且数据库提交成功后再提升；失败补偿和孤儿扫描负责回收。RabbitMQ 不可用时 Outbox 保留未发送事件，恢复后继续投递；消费者在 ACK 前崩溃时由 RabbitMQ 重投并通过幂等状态恢复。

### 3.3.2 已批准 Build 目标：默认值与环境覆盖

下表是实现必须采用的目标默认值；配置键是否已落盘以及是否已有运行态证据，分别以 active change 的 `tasks.md` 和 `verification.md` 为准。

| 语义 | 默认值 | 回退/禁用语义 |
|---|---|---|
| 异步资料处理 | 开启 | 显式关闭时禁用新文件上传并说明原因；绝不切回同步请求路径 |
| OCR | 开启 | 仅文本不足页面触发；有足够文本的 PDF 不调用 OCR |
| 最大文件 | 50 MB | 超限在进入处理流水线前拒绝，不创建虚假 READY |
| PDF 最大页数 | 500 | 超限视为永久校验失败，不重试 |
| 文档处理并发 | 2 | 只改变隔离 consumer executor 并发，不占用 HTTP executor |
| OCR 并发 | 1 | 独立于普通文档处理并发，避免外部进程耗尽资源 |
| 任务最大尝试 | 见 `docs/tech-selection.md` 韧性配置矩阵 | 包含首次执行；达到上限进入 DLQ/FAILED，永久错误不重试 |
| 原始文件保留 | 开启 | 未删除资料必须保留原件；显式关闭时禁用新上传，既有资产仍保留，不能接受上传后丢弃原件 |
| 知识点自动生成 | 开启 | 关闭只停止自动创建任务，仍允许阅读资料和手动生成 |

环境变量覆盖 RabbitMQ host/port/username/password/vhost，MinIO endpoint/access key/secret key/bucket，以上处理开关与限制，以及 parser/OCR/LibreOffice/AI 的 timeout/retry/circuit breaker。具体键、默认值和合法范围以 `docs/tech-selection.md` 为唯一技术基线，根 `.env.example` 只提供非敏感本地值。启动时必须校验覆盖值；配置缺失、依赖不可用或 adapter 失败均不得产生静默 fallback 或虚假成功。

### 3.4 AI 与 RAG 边界

AI Provider 边界：

```text
Generation / KnowledgePoint Application Service
  -> AiProvider or SuiLearn AI Port
     -> OpenAiCompatibleAiProvider
     -> ai/infrastructure/springai adapter（预留）
```

检索边界：

```text
RagController / SearchController
  -> RagService / SearchService
  -> Retriever
     -> KeywordRetriever
     -> EmbeddingProvider
```

规则：

- OpenAI-compatible Provider 是当前运行时 AI 实现；默认开发和测试流程不得依赖生产替身 Provider。
- Provider 只作为基础设施适配，不让业务层感知具体厂商。
- Spring AI 只允许出现在 `ai/infrastructure/springai/**`，业务模块只能依赖 `ChatPort`、`EmbeddingPort`、`StructuredGenerationPort`、`RetrievalPort` 等 SuiLearn 自有端口。
- 首轮只定义 Chat、Structured Output、Embedding 的 adapter 边界；VectorStore、Advisor 和 Tool Calling 后续单独确认。
- Provider 状态接口只能返回脱敏信息。
- RAG 必须受 `knowledgeBaseId` 或 `materialId` 范围约束。
- 回答需要返回来源引用；证据不足时表达不确定。
- 搜索和问答必须排除已删除资料、失效 chunk 或不可用 embedding。
- text-only 候选召回必须走持久化检索索引（chunk 写入时由 `TextSearchTokenizer` 生成 `search_text`，PostgreSQL 生成列 `search_tsv = to_tsvector('simple', search_text)` + GIN 索引），不以全表 `findAll()` Java 扫描为主路径；中文经应用层 n-gram 进入索引。
- 索引收窄仅用于 text-only 路径；当 embedding 可用时，对 scope 候选全量打分以保留语义召回。
- BM25 打分按候选集每次查询预计算一次语料统计（词频/长度/document frequency），不得按候选重复分词。

## 4. Web 当前结构

Web 位于 `apps/web`，当前是 React + TypeScript + Vite 单页知识库工作台。

```text
apps/web/
├─ index.html
├─ package.json
├─ src/
│  ├─ main.tsx
│  ├─ App.tsx
│  ├─ api.ts
│  ├─ types.ts
│  ├─ styles.css
│  └─ vite-env.d.ts
├─ tsconfig.json
├─ tsconfig.node.json
└─ vite.config.ts
```

职责：

- `App.tsx` 承载知识库工作台主要界面和交互状态。
- `api.ts` 集中封装 `/api/v2` 请求、错误解析和 API 方法。
- `types.ts` 集中维护 Web 消费的 TypeScript 类型。
- `styles.css` 维护工作台样式。

规则：

- Web 当前只做知识库工作台，不做完整刷题端。
- API 调用只能通过 `api.ts` 集中封装，避免页面内散落 fetch。
- `types.ts` 的类型语义必须与 OpenAPI 和服务端 DTO 对齐。
- `VITE_API_BASE_URL` 可覆盖 API base，默认 `/api/v2`。
- Web 不保存密钥，不直接调用 AI Provider。

## 5. Contracts 当前结构

```text
contracts/
└─ openapi/
   └─ suilearn-v2.yaml
```

职责：

- `contracts/openapi/suilearn-v2.yaml` 是 Backend、Web 和 Android 远程能力的 API 单点真相。
- `contracts/schemas/**` 预留给后续 JSON schema 或内容契约。

规则：

- 契约由架构 Agent 维护。
- 实现 Agent 发现契约不足时，先向 Leader / 架构 Agent 返回越界申请或阻塞问题。
- 契约变更必须先完成并稳定，再派发 Backend、Web、Android 或 Content 适配任务。
- 契约变更与消费端适配不得并行写代码。

## 6. 跨端数据流

### 6.1 本地学习闭环

```text
Android assets JSON
  -> Android Room
  -> Android Repository / UseCase
  -> Android ViewModel
  -> Compose UI
```

本闭环不依赖 Backend 或 Web。

### 6.2 知识库工作台（当前）与资料知识流水线（Build 目标）

当前数据流：

```text
Web App
  -> api.ts
  -> Backend Controller
  -> Module Application Service
  -> Module Store / AI Port / Retrieval / Material
  -> Backend Response
  -> Web UI
```

active change 的契约和对应实现门禁满足后，目标数据流为：

```text
Web App
  -> api.ts
  -> Backend Material API (multipart)
  -> private MinIO ORIGINAL + PostgreSQL Material/Task/Outbox
  -> 202 + materialId/taskId
  -> RabbitMQ durable queues
  -> same Backend isolated listeners
  -> parser / optional OCR / normalizer / indexer
  -> DocumentRevision/Block + READING/PREVIEW/OCR_PAGE assets
  -> structured knowledge point / question draft tasks
  -> Web polls task and reads versioned results through API
```

Web 当前是资料和知识库重流程主入口。目标流水线落地后，Web 和 Android 仍不直接访问 RabbitMQ、MinIO、LibreOffice 或 Tesseract；所有外部依赖由 Backend port/adapter 封装。

### 6.3 Android 远程 AI 入口

```text
Android feature/ai
  -> AiKnowledgeViewModel
  -> AiKnowledgeRemoteRepository
  -> AiRemoteApiClient
  -> Backend API
```

Android 只消费必要远程能力；本地学习数据不因远程不可用而失效。

### 6.4 契约变更流

```text
Architecture Agent updates contracts
  -> Backend adapts Controller / DTO / Service
  -> Web adapts api.ts / types.ts
  -> Android adapts remote models / API client
  -> Test / Reviewer validates cross-end consistency
```

## 7. 关键业务边界

### 7.1 学习内容与用户记录

- 题库内容和用户记录分离。
- 题库更新不能破坏答题记录、错题状态、收藏状态和统计。
- 题目、分类、知识点、资料、chunk、生成内容都需要稳定 ID。

### 7.2 AI 生成内容

- AI 生成内容默认是待确认草稿。
- 未经用户保存或审核通过的内容不得进入正式学习内容。
- 用户可以保存、编辑后保存、丢弃或删除生成结果。
- 已保存内容必须保留来源引用，便于追溯。

### 7.3 资料与知识库

- 知识库隔离资料、题目、知识点、生成内容、搜索和问答范围。
- 单份资料问答不得混入其他资料。
- 删除资料时必须明确 pending content 和 saved content 如何处理。

### 7.4 统计

- Android 本地统计以本地答题记录、错题和收藏为事实来源。
- Backend 知识库统计以持久化题目、答题记录、错题和笔记计算。
- 没有记录时正确率应为 null 或省略，不返回看似真实的占位常量。

### 7.5 已批准 Build 目标：故障、回退与安全边界

以下是 active change 的实现约束与验收语义；故障路径是否已经通过运行态验证，只以 `verification.md` 的真实证据为准。

- RabbitMQ 暂时不可用：Outbox 保留且后台处理健康组降级；已完成资料读取和 HTTP liveness 不因消息中间件故障被误判不可用。
- MinIO 不可用：新上传拒绝或任务明确退避/失败，不创建 READY；不得改存数据库 blob、本地临时目录或丢失原件语义。
- OCR 最终失败且页面无足够文本：资料处理失败，但已保存原件仍可查看、下载和重试；不得以空白阅读版冒充 READY。
- LibreOffice 预览失败：保留原件并明确预览失败；不得执行用户文件中的宏、脚本、外链或嵌入对象。
- AI 未配置、超时或结构输出不完整：资料阅读不受影响，知识点/题目任务明确不可用或失败；不得生成关键词、统一描述或占位知识点。
- Resilience4j 只对外部 adapter/AI 提供有界超时、重试和熔断；消息级退避由 RabbitMQ retry queue 与 ProcessingTask 状态管理，两层不得形成无界乘法重试。
- Markdown 原文是不可信输入：CommonMark renderer 必须优先启用等价于 `escapeHtml(true)`、`sanitizeUrls(true)` 的内建配置，转义或禁用 raw HTML。普通链接只允许 `http`、`https`、`mailto` scheme；`javascript`、`data`、`file`、`vbscript` 及未知 scheme 一律禁止。远程图片和其他外部资源默认不得自动加载，应渲染为无远程 `src` 的占位/替代文本；若未来使用受控代理，必须限制目标 allowlist 并阻断回环、私网和重定向绕过。只有测试证明内建能力不足时，才可另行评估经过测试的 sanitizer，不得在本任务预选大型依赖。
- 文件扩展名、MIME、签名、大小、页数、解压后大小和嵌套深度必须同时校验；原文视为不可信证据，不能覆盖模型系统指令。
- `correlationId`、`taskId`、`materialId` 只进入结构化日志或 trace；可通过 trace exemplar 关联指标样本，但不得成为 metric tags。日志、trace 和 exemplar 均不得记录完整正文、原始模型响应、密钥、永久凭据或临时授权地址。
- Actuator/Micrometer 将 HTTP liveness/readiness 与后台处理依赖健康分层：PostgreSQL、RabbitMQ、MinIO 状态和队列/Outbox/DLQ/阶段/OCR/AI 指标必须可区分，依赖降级不得伪装为全系统健康。metric tags 只允许 `stage`、`outcome`、`dependency`、`queue`、`taskType`、`assetType` 等固定集合；其中 `queue`、`taskType`、`assetType` 必须使用代码定义的受控枚举，不得使用 ID、文件名、object key、错误消息或其他无界值。

## 8. 测试与验证边界

| 范围 | 测试位置 | 验证重点 |
|---|---|---|
| Android 本地 | `apps/android/src/test/**`、`apps/android/src/androidTest/**` | JSON 解析、Room 导入、Repository、UseCase、远程 client、Smoke UI |
| Backend | `services/api/src/test/**` | 当前 Service/任务/AI/RAG 规则；Build 目标还需覆盖格式解析/OCR、资产补偿、Outbox/RabbitMQ 幂等恢复、健康和指标，以及 Markdown raw HTML、危险/混淆 URL、远程资源不自动加载边界 |
| Web | `npm --prefix apps/web run build` | TypeScript 类型、Vite 构建、API client 调用形态 |
| Contracts | `contracts/**` diff 审查 | API 兼容性、字段语义、消费端适配范围 |
| Compose 运行态（Build 目标，状态见 `verification.md`） | 根 `compose.yml` + Testcontainers / 故障验收矩阵 | PostgreSQL/Outbox、RabbitMQ 中断恢复/重复投递/DLQ、MinIO 临时对象/清理、OCR/AI 超时、API/消费者重启、分层健康和指标 |

文档-only 架构调整不需要运行模块测试，但必须执行 diff/stat 检查并说明原因。

后端模块边界需要自动化保护：

- `ApplicationStoreBoundaryTest` 检查模块 `application` 包不得直接引用 `SuiLearnV2Store`。
- 后端回归测试应覆盖 `TaskService` / `TaskExecutor` 的任务生命周期委托，以及 `SourceService` 的来源标准化和删除影响规则。
- Spring AI adapter 真正启用前，应继续通过源码扫描或架构测试确认业务模块没有直接 import Spring AI 类型。

## 9. 变更规则

架构变更必须遵守：

- 改代码结构、模块边界、数据流或契约关系时，更新本文。
- 改技术栈、版本、依赖或升级规则时，更新 `docs/tech-selection.md`。
- 改产品行为、验收标准或阶段范围时，先交产品 Agent 更新 `docs/product-requirements.md`。
- 改 API 字段或跨端模型时，先更新 `contracts/**`，再派发消费端适配。
- 不把聊天里的临时判断当成稳定架构，除非已经写回本文或已批准的 `openspec/changes/<change-name>/**`。

## 10. 当前开放风险

- Android 仍同时使用 `src/main/java` 和 `src/main/kotlin`，短期允许共存；是否统一迁移需单独任务评估。
- Web 类型当前手写维护，后续若契约变化频繁，可评估从 OpenAPI 生成类型。
- 已批准 Build 目标以模块化单体承载 HTTP 与 RabbitMQ listener，并用隔离有界线程池控制任务资源；其实现和运行态状态以 active change 的 `tasks.md`/`verification.md` 为准。只有未来出现经验证的独立扩缩容或故障隔离需求时，才通过新架构变更评估拆分 Worker，本 change 不得提前拆分。
- pgvector 能力仍需按 PostgreSQL 部署环境验证；关键词检索保留为非语义兜底，真实向量能力落地时需要补契约、配置和集成验证。
