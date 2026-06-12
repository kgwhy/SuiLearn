# Backend V4/V5 Modular AI Architecture

| 项目 | 内容 |
|---|---|
| Status | Implemented |
| Primary Owner | Architect Agent |
| Impacted Agents | Server Backend Agent / Web Frontend Agent / Android Agent / Test Agent / Reviewer Agent |
| Target Stage | Backend V4/V5 preparation |
| Created | 2026-06-10 |
| Updated | 2026-06-11 |
| Change Intent | 将 `services/api` 重构为面向 V4/V5 的模块化单体，并把 Spring AI 纳入后端 AI 基础设施层。 |
| Scope | Architecture / Tech Baseline / Backend / Web / Android / Test |
| Spec Key | SPEC-BACKEND-MODULAR-AI / SPEC-SOURCE-TRACEABILITY / SPEC-TASK-LIFECYCLE |
| Implementation Reference | 2026-06-10 Leader implementation in current working tree |

## 背景

`services/api` 当前已经承载知识库、资料导入、知识点抽取、AI 生成、内容确认、RAG 问答、语义搜索、任务状态和来源追踪等 V4 能力雏形，但实现集中在少数总管类中：

- `service/SuiLearnV2Service.java` 同时编排知识库、资料、知识点、生成、笔记、搜索、RAG 和任务状态。
- `persistence/SuiLearnV2Store.java` 聚合多个 JPA repository、entity/domain 映射、JSON 序列化和跨聚合删除。

本提案只解决后端架构边界问题：把当前 V4 能力拆成可维护的模块化单体，并为 V5 的多学习包、多题型和更复杂 AI 能力保留边界。它不直接实现 V5 产品能力。

## 要做的事

本 Proposal 批准后，只做以下范围内的架构重构。

### 1. 后端模块化单体

目标结构：

```text
com.suilearn.api
├── common/            通用基础能力：错误、ID、时间、JSON、通用结果类型
├── config/            Spring Boot 配置
├── ai/                AI 端口与 Spring AI 基础设施适配
├── source/            来源引用、来源解析、引用构造、删除影响判断
├── pack/              LearningPack 服务边界抽象
├── knowledgebase/     知识库 CRUD、详情、统计与边界规则
├── material/          资料查询、删除策略、解析、切片、导入流水线
├── knowledgepoint/    知识点抽取、确认、编辑、删除
├── generation/        生成内容草稿、审核门禁、保存与丢弃
├── rag/               RAG 问答、证据约束、不确定性表达
├── search/            关键词/语义搜索入口
└── task/              长任务生命周期、状态、结果引用和执行模板
```

模块内部采用统一边界：

```text
<module>/
├── api/               Controller 与 API DTO
├── application/       UseCase / Application Service
├── domain/            领域模型、状态、策略、不变量
└── infrastructure/    JPA Entity、Repository Adapter、Mapper、外部适配
```

依赖方向：

```text
api -> application -> domain
application -> port/interface
infrastructure -> port/interface implementation
```

### 2. 拆分大 Service

将 `SuiLearnV2Service` 拆成按领域负责的 Application Service：

- `KnowledgeBaseService`
- `MaterialImportService`
- `MaterialQueryService`
- `KnowledgePointService`
- `GeneratedContentService`
- `RagService`
- `SearchService`
- `TaskService`

Controller 改为依赖对应领域服务。本轮收口后，`SuiLearnV2Service` 仅作为旧测试和兼容入口保留，不再作为 Controller 或新增业务入口。

### 3. 拆分大 Store

将 `SuiLearnV2Store` 拆成按聚合负责的 Store / Adapter：

- `KnowledgeBaseStore`
- `MaterialStore`
- `MaterialChunkStore`
- `KnowledgePointStore`
- `GeneratedContentStore`
- `QuestionStore`
- `AiNoteStore`
- `TaskStore`

Mapper 按边界归属放置：

- API DTO mapper 放在模块 `api` 边界。
- Entity/domain mapper 放在模块 `infrastructure` 边界。
- 不建立全局大 `mapper` 包。

本轮收口后，`SuiLearnV2Store` 保留为底层兼容持久化 facade；新增业务不得绕过模块 `infrastructure` Store / adapter 直接依赖它。

### 4. 建立 Task 中心

抽出 `TaskService` 和 `TaskExecutor`，统一资料导入、知识点抽取、AI 生成、embedding 等长任务的生命周期。

Task 中心负责：

- 创建任务。
- 标记运行中。
- 标记成功和结果引用。
- 标记失败和错误信息。
- 提供统一查询入口。

业务服务只写业务步骤，不重复编写任务状态模板。

### 5. 建立 Source 中心

抽出 `source` 模块，统一来源引用和来源追踪。

首轮只做逻辑边界，不新增图谱数据库或复杂持久化模型。

Source 中心负责：

- 标准化 `SourceRef`。
- 解析来源是否存在、是否可用。
- 构造 RAG 引用和生成内容引用。
- 判断资料删除对 pending / saved 内容的影响。

RAG、generation、material、knowledgepoint 不再各自维护来源规则。

### 6. 引入 Spring AI 基础设施层

批准后更新 `docs/tech-selection.md`，将 Spring AI 纳入后端 AI 基础设施标准。

允许 Spring AI 出现的位置：

```text
ai/infrastructure/springai/**
```

业务模块不得直接 import：

- `ChatClient`
- `ChatModel`
- `EmbeddingModel`
- `VectorStore`
- `Advisor`
- Spring AI Tool Calling 类型

业务模块只能依赖 SuiLearn 自己的端口，例如：

- `ChatPort`
- `EmbeddingPort`
- `StructuredGenerationPort`
- `RetrievalPort`

首轮 Spring AI 接入只要求建立 Chat / Structured Output / Embedding 的端口与适配边界。是否启用 VectorStore、Advisor、Tool Calling 由后续任务单独确认。

### 7. 明确 generation 边界

`generation` 模块只负责“生成内容生命周期”：

- 草稿。
- 状态。
- 审核门禁。
- 保存。
- 丢弃或删除。
- 来源引用。

它不承载所有 AI 生成业务逻辑。

具体生成用例仍归属对应业务模块，例如：

- 知识点抽取归属 `knowledgepoint`。
- RAG 问答归属 `rag`。
- AI 笔记保存归属生成内容门禁和现有笔记模型的协作边界。
- 题目生成以当前后端 `Question` / `GeneratedContent` 模型为基础，不新建独立 `question` 模块。

### 8. 引入 LearningPack 服务边界抽象

`pack` 只作为服务层作用域抽象，用于避免后端继续把所有范围概念写死为 `knowledgeBaseId`。

首轮约束：

- 不修改 DB schema。
- 不修改 OpenAPI。
- 不新增多学习包产品能力。
- 当前可通过 `packId == knowledgeBaseId` 适配已有知识库场景。

### 9. 引入生成侧题型契约

在 generation 相关边界引入 `QuestionTypeContract`，用于 AI 生成结果的字段校验和规整。

首轮只覆盖现有题型：

- 单选题。
- 多选题。
- 判断题。
- 简答题。

后端不负责 Android 本地刷题判分。V5 卡片题、拼写题不在本提案实现范围内。

## 明确不做的事

本 Proposal 不做以下内容：

- 不实现 V5 多学习包功能。
- 不实现英语单词、古诗词、卡片题、拼写题或长期复习引擎。
- 不把 Android 本地刷题、判分、错题、收藏、统计和间隔重复迁移到后端。
- 不引入账号、多租户、云同步或权限系统。
- 不拆微服务。
- 不引入 Redis、MQ、独立向量数据库或工作流引擎。
- 不改变现有 HTTP 契约。
- 不改变现有 DB schema。
- 不创建空 `agent` 模块。
- 不把 Spring AI 类型泄漏到业务模块。
- 不修改 `docs/chat.md`。
- 不越过产品 Agent 修改正式产品范围。

## Agent 边界

当前不单独拆 `agent` 模块。

原因：

- 现阶段已确认能力可以通过 UseCase、RAG、结构化生成和 Spring AI adapter 完成。
- Agent 只有在出现多步自主规划、动态工具选择、长期上下文任务或学习规划助手等真实用例时才成立。
- 过早建立 `agent` 模块会制造空抽象。

保留原则：

- Spring AI Tool Calling 可作为基础设施能力预留，但不在首轮暴露给业务模块。
- 第一个真实 Agent 用例出现时，再新增独立 Proposal 明确边界。

## 影响分区

### 产品影响

暂不修改 `docs/product-requirements.md`。

- 不新增已确认产品范围。
- 不把 V5 的英语、古诗词、长期复习、Agent 助手提前视为已确认实现内容。
- 保持当前产品原则：AI 生成内容必须经过用户确认后才能进入正式学习内容。

### 架构影响

批准并实现后，需要修改 `docs/architecture.md`，记录：

- 后端目标模块结构。
- 各模块职责边界。
- Task 生命周期边界。
- SourceRef 来源追踪边界。
- Spring AI 只能位于 infrastructure adapter 的依赖规则。

### 技术基线影响

批准并实现 Spring AI 接入前，需要修改 `docs/tech-selection.md`，记录：

- Spring AI port-first 技术基线。
- 当前阶段不新增 Spring AI starter / Maven 依赖。
- Spring AI 类型不得泄漏到业务模块的约束。
- VectorStore、Advisor、Tool Calling 的启用条件。

### 契约影响

首轮不修改 `contracts/**`。

若后续引入 `packId`、新的来源接口、Agent 接口或新的题型 schema，必须先由架构 Agent 更新 OpenAPI，再派发 Backend/Web/Android 适配任务。

## 角色与文件影响

- Primary Owner：Architect Agent
- 主要实现：Server Backend Agent
- 受影响适配：Web Frontend Agent、Android Agent
- 验证：Test Agent、Reviewer Agent

可修改文件：

- `docs/proposals/backend-v4-v5-modular-ai-architecture.md`
- 批准后：`docs/architecture.md`
- 批准后：`docs/tech-selection.md`
- 批准后：`services/api/**`
- 如契约变化：`contracts/**`
- 如客户端远程能力变化：`apps/web/**`、`apps/android/**`

禁止在本 Proposal 实现阶段直接修改：

- `docs/chat.md`
- 未经产品 Agent 确认的 `docs/product-requirements.md` 产品范围
- 与后端重构无关的 Android 本地学习闭环代码
- 与后端重构无关的 Web UI 重设计

共享文件要求：

- `docs/architecture.md`、`docs/tech-selection.md`、`contracts/**` 属共享文件，若进入实现阶段并行协作，必须采用 worktree 隔离或严格串行。

## 分阶段迁移计划

每个阶段都应保持现有 HTTP 契约兼容。除阶段 6 外，不应新增外部依赖。

### 阶段 0：回归安全网

- 运行后端测试作为基线。
- 必要时补充关键 HTTP 契约 smoke test。
- 明确当前响应字段和错误语义。

### 阶段 1：拆 Store 与 Mapper

- 将 `SuiLearnV2Store` 拆成按聚合的 Store。
- 将 entity/domain 映射迁移到各模块 infrastructure mapper。
- 可保留临时 facade 以降低一次性切换风险。

### 阶段 2：建立 Task 中心

- 抽出 `TaskService`。
- 抽出 `TaskExecutor`，统一任务状态模板。
- 消除导入、抽取、生成中的重复任务生命周期代码。

### 阶段 3：按领域拆 Application Service

- 逐步迁移 `SuiLearnV2Service` 方法到对应领域服务。
- Controller 改为依赖对应领域服务。
- 每迁移一个领域运行一次后端测试。

### 阶段 4：收紧事务边界

- 移除类级长事务。
- 外部 AI / embedding 调用移到事务外。
- DB 写入、状态更新、失败补偿使用短事务。

### 阶段 5：建立 Source 中心

- 将来源标准化、引用构造、来源可用性检查、资料删除影响判断迁入 `source`。
- 不新增图谱数据库。
- 不新增 OpenAPI。

### 阶段 6：引入 Spring AI Adapter

- 修改 `docs/tech-selection.md`，确认 Spring AI port-first 边界；首轮不启用 starter。
- 建立 AI port 与 Spring AI infrastructure adapter。
- 首轮只接 Chat / Structured Output / Embedding 边界。
- VectorStore、Advisor、Tool Calling 不在首轮启用，除非另行批准。

### 阶段 7：引入 LearningPack 服务边界抽象

- 在服务边界引入 `packId` / `LearningPack` 概念。
- 第一阶段通过 `packId == knowledgeBaseId` 适配现有数据。
- 不改 DB schema。
- 不改 OpenAPI。

### 阶段 8：题型契约 SPI

- 引入 `QuestionTypeContract`。
- 先覆盖现有四种题型。
- 不新增 V5 题型。
- 不改 Android 判分逻辑。

## 实现就绪检查（已完成）

进入实现前，以下项目必须明确：

- [x] Status 已推进为 `Implemented`。
- [x] Spring AI 采用 port-first 边界，首轮不启用 starter，不替换现有 OpenAI-compatible provider。
- [x] 确认保持 HTTP 契约完全不变。
- [x] 确认保持 DB schema 完全不变。
- [x] 后端测试基线已记录。
- [x] 文件范围和角色分工已拆成任务卡。
- [x] 本轮采用严格串行，未并行修改共享文件。

## 验收标准

- `SuiLearnV2Service` 不再作为主要业务编排入口，仅保留为旧测试和兼容入口。
- `SuiLearnV2Store` 保留为底层兼容持久化 facade；新增业务不得绕过模块 Store / adapter 直接使用它。
- 后端领域服务按知识库、资料、知识点、生成内容、RAG、搜索、任务拆分。
- Spring AI 只出现在 `ai/infrastructure/springai/**`，不泄漏到业务模块。
- 生成内容门禁仍然保证：未确认内容不会进入正式学习内容。
- SourceRef / Citation / 删除影响规则有统一边界，不散落在 generation、rag 和 material 中。
- 任务生命周期通过 `TaskService` / `TaskExecutor` 统一表达。
- `/api/v2/*` 契约保持兼容。
- DB schema 保持兼容。
- Android 本地学习闭环不因后端重构新增服务端依赖。
- 后端测试通过：`mvn -f services/api/pom.xml test -q`，`ApplicationStoreBoundaryTest` 1 个，`DefaultMaterialChunkerTest` 1 个，`SuiLearnV2ServiceTest` 23 个，0 failure / error / skipped，总 25 个。

## 后续独立迁移项

- 是否启用 Spring AI starter、替换现有 OpenAI-compatible provider、引入 VectorStore / Advisor / Tool Calling，后续单独提案确认。
- 是否补充 `SourceRef` 轻量后端单元测试作为来源追踪保护网，后续测试强化任务确认。
- 是否新增 ArchUnit 或等价检查，阻止业务模块直接 import Spring AI 类型，后续架构守护任务确认。
- 是否进一步删除或收缩 `SuiLearnV2Service` / `SuiLearnV2Store` 兼容 facade，后续兼容清理任务确认。

## 实现后关闭

- [x] 模块边界已落地：`knowledgebase`、`material`、`knowledgepoint`、`generation`、`rag`、`search`、`task`、`source`、`pack`、`ai` 承担各自 Application Service / Store / adapter 边界。
- [x] Controller 已不依赖 `SuiLearnV2Service`，新增入口应继续依赖对应模块 Application Service。
- [x] `TaskService` 已接管任务创建、启动、成功、失败和查询；`TaskExecutor` 已接入长任务模板，导入流程通过 `runManagedTask` 覆盖 `MATERIAL_IMPORT` 与 `EMBEDDING`。
- [x] `SourceService` 已接管 `SourceRef` normalize、usable 和删除影响标记等来源规则。
- [x] `KnowledgeBaseService`、`MaterialQueryService`、`SearchService`、`RagService`、`MaterialImportService`、`KnowledgePointService`、`GeneratedContentService`、`LearningPackService` 已不再薄代理 `SuiLearnV2Workflow`。
- [x] Spring AI 采用 port-first 边界，保持在基础设施 adapter 方向；业务模块不得直接依赖 Spring AI 类型。
- [x] HTTP 契约、DB schema 和 `contracts/**` 均未在本 Proposal 收口中改变。
- [x] `SuiLearnV2Service` 保留为旧测试 / 兼容入口；`SuiLearnV2Store` 保留为底层兼容持久化 facade，不作为新增业务绕过模块 Store 的入口。
- [x] 稳定结论已合并回 `docs/architecture.md`。
- [x] Spring AI 技术基线已合并回 `docs/tech-selection.md`。
- [x] Proposal 状态已更新为 `Implemented`。
- [x] Implementation Reference 已记录。
- [x] 未完成项已转入后续独立迁移项。
