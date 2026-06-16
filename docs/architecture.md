# 随心学 SuiLearn 当前架构

## 0. 文档职责

本文是 SuiLearn 当前代码结构、模块边界、数据流和契约关系的真相源，由架构 Agent 维护。

本文只描述当前最新约定，不保留历史版本设计。旧架构、废弃方案和历史取舍通过 Git 历史追溯；未来架构变更先进入 `openspec/changes/<change-name>/**`，批准并实现后再将稳定结论合并回本文。

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
- Backend 承载 AI、知识库、资料导入、RAG 和跨端持久化。
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

首轮保持 `/api/v2/*` HTTP 契约和 DB schema 不变。

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
- 模块 Application Service 通过本模块 `infrastructure` Store / adapter 访问持久化；不得直接注入 `SuiLearnV2Store`。
- `SuiLearnV2Store` 只作为底层兼容持久化 facade 被模块 Store / adapter 或兼容层使用，后续新增聚合访问优先补充对应模块 Store 方法。
- Entity/domain mapper 优先归属模块 `infrastructure` 边界；不新增全局大 mapper 包。

### 3.4 AI 与 RAG 边界

AI Provider 边界：

```text
Generation / KnowledgePoint Application Service
  -> AiProvider or SuiLearn AI Port
     -> FakeAiProvider / OpenAiCompatibleAiProvider
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

- Fake Provider 用于默认开发和测试流程。
- 真实 Provider 只作为基础设施适配，不让业务层感知具体厂商。
- Spring AI 只允许出现在 `ai/infrastructure/springai/**`，业务模块只能依赖 `ChatPort`、`EmbeddingPort`、`StructuredGenerationPort`、`RetrievalPort` 等 SuiLearn 自有端口。
- 首轮只定义 Chat、Structured Output、Embedding 的 adapter 边界；VectorStore、Advisor 和 Tool Calling 后续单独确认。
- Provider 状态接口只能返回脱敏信息。
- RAG 必须受 `knowledgeBaseId` 或 `materialId` 范围约束。
- 回答需要返回来源引用；证据不足时表达不确定。
- 搜索和问答必须排除已删除资料、失效 chunk 或不可用 embedding。

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

### 6.2 知识库工作台

```text
Web App
  -> api.ts
  -> Backend Controller
  -> Module Application Service
  -> Module Store / AI Port / Retrieval / Material
  -> Backend Response
  -> Web UI
```

Web 是资料和知识库重流程主入口。

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

## 8. 测试与验证边界

| 范围 | 测试位置 | 验证重点 |
|---|---|---|
| Android 本地 | `apps/android/src/test/**`、`apps/android/src/androidTest/**` | JSON 解析、Room 导入、Repository、UseCase、远程 client、Smoke UI |
| Backend | `services/api/src/test/**` | Service 规则、资料切片、任务状态、AI/RAG 边界 |
| Web | `npm --prefix apps/web run build` | TypeScript 类型、Vite 构建、API client 调用形态 |
| Contracts | `contracts/**` diff 审查 | API 兼容性、字段语义、消费端适配范围 |

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
- Backend 当前以单服务承载全部知识库和 AI 能力；当任务处理复杂度上升时，再评估队列或 worker 拆分。
- pgvector 能力在当前代码中仍有 Fake / 关键词检索兜底，真实向量能力落地时需要补契约、配置和集成验证。
