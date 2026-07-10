## Context

SuiLearn 当前 Web 通过 `File.text()` 读取本地文件，再以 JSON `content` 导入；Backend 的 `TextMaterialParser` 只做换行规范化。结果是 PDF 二进制没有真实解析，DOC/DOCX 未被支持，系统也没有保存原始文件。资料详情只能依赖规范化字符串，无法稳定提供“阅读版 + 原始文件”双视图。

知识点模型当前只有 `name`、`description` 和来源引用。AI 未配置时，本地 fallback 从正文抽取词项，并以统一句子填充 description；这与用户要求的“可学习的知识点总结”冲突。现有任务模型会记录状态，但 `TaskExecutor.runManagedTask` 仍在调用线程同步执行，不能保护上传请求免受解析、OCR、Embedding 或 AI 延迟影响。

本变更由用户后续对话确认，属于跨 Product、Architect、Backend、Web、Test、Reviewer 的 Major 变更。约束如下：

- 保持一个仓库和一个 Backend 应用，不拆微服务或独立 Worker 系统。
- 允许在 Docker Compose 中引入 RabbitMQ、MinIO 等中间件增强可靠性。
- Android 本地刷题闭环不得依赖新中间件。
- 未经用户确认的知识点和题目不得进入正式学习内容。
- 契约必须先稳定，再适配 Backend/Web；实现发现产品、架构或契约歧义时返回 Spec。

## Goals / Non-Goals

**Goals:**

- 支持 Markdown、TXT、PDF、DOC/DOCX 原始文件导入，并完整保留原件。
- 提供统一阅读版和原始文件双视图；PDF/Word 保留可定位的页码或段落来源。
- 对扫描或混合 PDF 自动按页 OCR，不让用户预判 PDF 类型。
- 使用 PostgreSQL Outbox + RabbitMQ 构建同项目内的持久化异步处理流水线。
- 使用 MinIO 保存原件、阅读版、预览和 OCR 衍生产物。
- 生成结构化、可审核、可追溯的知识点，而不是关键词或占位描述。
- 从已确认知识点快速生成面试题，并保留高级题型、难度和数量设置。
- 为中间件故障、消费者崩溃、重复投递、AI/OCR 超时提供恢复、观测和验证闭环。
- 增量兼容现有资料、知识点和已保存题目。

**Non-Goals:**

- 不拆分单独微服务、独立部署 Worker 或多仓库文档平台。
- 不同时引入 Redis；RabbitMQ 专用于消息，PostgreSQL 保存业务事实和 Outbox。
- 不引入账号、多租户、云同步、公开知识库市场或复杂权限系统。
- 不执行 Office 宏、PDF 脚本、外部链接或嵌入对象。
- 不修改 Android 本地题库、答题记录、错题、收藏或统计闭环。
- 当前个人使用阶段不强制引入 ClamAV；非可信用户上传另行立项。

## Decisions

### 1. 采用模块化单体与隔离消费者线程池

Backend 仍为 `services/api` 单应用，内部划分 Material API、Asset Storage、Processing Orchestrator、Document Parser、OCR Adapter、Normalizer、Indexer、Knowledge Point 和 Question Generation 边界。RabbitMQ listener 在同一应用中运行，但使用与 HTTP 完全隔离、带并发上限的线程池。

选择原因：满足当前部署规模，同时让耗时任务脱离请求线程；未来若需要独立 Worker，只改变启动配置和消费实例，不改变消息契约或业务模型。

替代方案：

- 继续同步执行：实现简单，但无法可靠处理中大文件、OCR 和外部 AI 延迟，否决。
- 仅使用 `@Async`/内存队列：重启丢任务且难以重试，否决。
- 立即拆微服务：扩展性高但运维和一致性成本不符合当前阶段，否决。

### 2. 使用 MinIO 保存文件资产，PostgreSQL 保存业务事实

`MaterialAsset` 保存 object key、类型、MIME、大小、校验值和创建时间；资产类型至少包括 `ORIGINAL`、`READING`、`PREVIEW`、`OCR_PAGE`。PostgreSQL 不保存大文件二进制，只保存资产引用。

上传通过 Backend 以 multipart 流式写入 MinIO，第一期不暴露浏览器直传。bucket 保持私有；查看原件使用短时授权地址或 API 代理。衍生文件先写临时 key，校验成功并提交数据库后提升为正式资产，孤儿对象由清理任务回收。

选择 MinIO 而非挂载目录，是为了在 Compose、本地和未来部署中保持一致的对象语义、校验和生命周期管理。

### 3. 使用 versioned revision 和 block，而非覆盖正文字符串

一次成功解析生成一个不可变 `DocumentRevision`；`DocumentBlock` 记录 revision、章节路径、页码、顺序和正文。Material 指向当前 revision。重新处理创建新 revision，不原地覆盖旧内容。

知识点和题目来源固定引用生成时使用的 revision/block。资料更新后，旧知识点继续可见但标记 `SOURCE_OUTDATED`，系统创建新草稿供比较，不能静默覆盖用户确认内容。

### 4. 格式适配器与按页 OCR

解析端口按类型路由：

- Markdown：CommonMark 解析并生成安全 HTML/结构化 block。
- TXT：字符集检测、规范化文本和段落 block。
- PDF：Apache Tika/PDFBox 提取文本、页数和页面映射。
- DOC/DOCX：Apache Tika/POI 提取结构；LibreOffice headless 生成高保真预览资产。
- OCR：Tesseract adapter 对文本密度不足页面识别，并合并回对应页 block。

先直接提取 PDF 文本，再按页判断是否 OCR；混合 PDF 只处理缺失页面。默认最大文件 50 MB、PDF 500 页，防止资源滥用。解析器不得执行宏、脚本或外部嵌入对象。

### 5. 使用 Transactional Outbox 与 RabbitMQ at-least-once 投递

上传事务同时写 Material、ProcessingTask 和 OutboxEvent。Outbox publisher 可重复尝试发布，发布成功后记录时间。RabbitMQ 使用持久化 exchange/queue、publisher confirm、manual ack 和至少一次投递。

队列按任务类型隔离：

- `document.processing`
- `knowledge-point.generation`
- `question.generation`

每类主队列配短/长两级 retry queue 和 dead-letter queue；具体退避默认值、合法范围和覆盖口只以 `docs/tech-selection.md` 的韧性配置矩阵为准，design 不复制参数。消费者完成资产与数据库事务后才 ACK。消息/阶段幂等键使用 `taskId + stage + documentRevision/processingVersion`，用于阻止同一消息重投重复提交阶段业务结果；它不是 adapter operation 幂等键，不能代替页级 OCR 或其他外部调用的持久化 claim/result。

PostgreSQL 还必须持久化 `ProcessingOperation`（或字段语义等价的明确模型），至少包含唯一 `operationKey`、`taskId`、`stage`、状态、跨 ProcessingTask 尝试与重启累计的 `attemptCount`、`resultReference`、`adapterVersion`、创建/更新时间与开始/完成时间，以及脱敏 `errorCode/errorMessage`。状态至少能区分待执行、执行中、成功、可重试失败和永久失败；`resultReference` 指向已经提交的 revision、OCR page asset/block、preview asset 或 AI generation result。

operation key 必须由稳定输入和 adapter/version 构成，并与消息幂等键分离：OCR 至少使用 `revisionId + pageNumber + ocrAdapterVersion`；parser 使用原始资产 checksum、processingVersion 与 parser adapter version；preview 使用 revision/原始资产 checksum、preview profile 与 preview adapter version；AI 使用稳定 logical input hash、generationVersion 与 AI adapter/model version。每次外部调用前先原子 claim 或读取 operation；已成功且结果引用有效的 operation 在消息重投、ProcessingTask 重试和应用重启后直接复用，只调度未完成、租约过期或可重试失败的 operation。单 operation 调用上限逐 operation 计算，不能作为整份文档页数上限；500 个待 OCR 页面是最多 500 个独立页级 operation，不能因其中若干 operation 达到上限而跳过其他页面。具体调用上限公式与数值仍只由 `docs/tech-selection.md` 定义。

### 6. 状态模型分离用户结果与执行细节

Material 只暴露 `UPLOADED → PROCESSING → READY/FAILED → DELETED`。ProcessingTask 使用 `QUEUED → RUNNING → SUCCEEDED`，并支持 `RETRY_WAIT`、`FAILED`、`CANCELLED`。执行阶段通过 `currentStep` 表达：`VALIDATING`、`PARSING`、`OCR`、`NORMALIZING`、`INDEXING`、`GENERATING_KNOWLEDGE_POINTS`。

任务补充 attemptCount、nextRetryAt、correlationId、processingVersion、idempotencyKey、errorCode 和脱敏 errorMessage。永久错误不重试；暂时错误按 Resilience4j/队列策略退避，达到上限进入死信队列。

### 7. 知识点必须是结构化草稿，不允许关键词 fallback

KnowledgePoint 保存：

- title/name
- shortSummary
- definition
- principles
- applicationScenarios
- pitfalls
- status：`DRAFT`、`CONFIRMED`、`REJECTED`、`ARCHIVED`
- SourceCitation：material、revision、page/block、excerpt

资料 READY 后默认创建知识点生成任务；用户可手动重新生成。AI 未配置或失败时，资料仍可阅读，但知识点任务明确失败/不可用，不运行本地关键词 fallback。只有 `CONFIRMED` 知识点参与正式搜索、专项学习和知识点面试题生成。

### 8. 知识点题目生成采用默认快捷操作与渐进设置

知识点详情的主要 CTA 为“生成面试题”。默认生成 1 道、中等难度、简答题；“更多设置”允许选择现有题型、难度和数量。生成请求携带已确认知识点的结构化内容及其证据引用，返回一个或多个待审核草稿。

现有 AI 内容确认门禁保持不变：用户可编辑、保存或丢弃，保存后才进入正式题库。知识点或题目生成失败不得改变资料 READY 或已确认知识点状态。

### 9. Web 使用三栏内容优先工作台

桌面端为知识库/资料导航、中央阅读区、右侧知识点区；平板折叠知识点侧栏，手机拆分连续页面。阅读区提供“阅读版”“原始文件”“下载原件”“重新处理”。正文使用可读行宽，不放入固定高度的小滚动框。

知识点列表显示标题、短总结、状态和来源数；详情展示完整结构和可跳转引用。状态必须同时使用文字、图标和颜色，异步进度可离开页面继续，失败提供原因和恢复动作。返回时保留阅读位置和筛选状态。

### 10. API 与契约采用兼容演进

Architect 先更新 OpenAPI，至少覆盖：

- multipart 原始资料上传，响应 `202` 与 material/task 引用。
- 资料原件、阅读版、当前 revision、资产和处理状态查询。
- 知识点生成任务、结构化字段、审核状态和来源引用。
- 基于知识点的面试题生成选项与批量草稿结果。

旧 JSON 文本导入保留一个兼容周期并标记 deprecated。新 Web 只使用 multipart 主路径。大正文通过专用阅读端点/资产返回，列表和普通详情不返回完整二进制。

### 11. 配置默认值与覆盖口

| 配置语义 | 默认值 |
| --- | --- |
| 异步资料处理 | 开启；关闭时禁用新文件上传，不回退同步路径 |
| OCR | 开启；仅文本不足页面触发 |
| 最大文件 | 50 MB |
| PDF 最大页数 | 500 |
| 文档处理并发 | 2 |
| OCR 并发 | 1 |
| 最大尝试次数 | 3 |
| Adapter 即时重试次数 | 0；合法范围 0..1 |
| 原始文件保留 | 开启 |
| 知识点自动生成 | 开启 |

环境变量至少覆盖 RabbitMQ host/port/user/password/vhost、MinIO endpoint/access/secret/bucket、处理/OCR 并发、文件/页数限制、超时、重试和功能开关；`.env.example` 只提供非敏感默认值。配置缺失不得产生虚假成功或静默 fallback。

`SUILEARN_ADAPTER_MAX_RETRIES` 是 adapter 即时重试的 canonical key，应用层默认值为 `0`，只接受整数 `0..1`。deprecated 的 `SUILEARN_AI_MAX_RETRIES` 保留一个兼容周期。兼容周期内，任务 2.1 必须在当前无 `env_file`、逐项 `environment` 映射的 Compose 模型中同时保留新旧两个键的无默认值可选透传：只透传部署环境或根 `.env` 中显式提供的值，不得为新键使用 `${SUILEARN_ADAPTER_MAX_RETRIES:-0}` 或任何等价的 Compose 默认注入；缺失时不得合成非空值，空字符串按未显式提供处理。根 `.env.example` 则只记录 canonical 新键的非敏感目标默认 `0`，不再列出 deprecated 旧键或旧默认 `2`。

Backend 配置绑定以“非空显式存在”判定新旧输入：

- 新旧键均缺失或为空：使用应用层默认 `0`。
- 仅新键非空：按整数 `0..1` 校验并使用，非法值 fail-fast。
- 仅旧键非空：旧值 `0` 映射为 `0`，正整数映射为 `1`，并记录 `SUILEARN_RETRY_CONFIG_LEGACY_MAPPED`；负数或非整数 fail-fast。
- 新旧键同时非空：无论值是否相同均 fail-fast，并记录 `SUILEARN_RETRY_CONFIG_CONFLICT`，不得静默选择优先级。

任务 2.1 只负责 `.env.example` 的新默认和 Compose 新旧键可选透传；任务 2.2 负责应用绑定、legacy 映射/冲突诊断，以及关闭 Provider SDK 和旧手写 retry，确保 Resilience4j 是唯一 adapter retry owner。本兼容周期继续执行上述 legacy 映射，不能提前静默切断旧 `.env` 配置。

兼容周期后的第一个具名 removal change 必须实现一个完整 tombstone 错误窗口：Compose 继续对 `SUILEARN_AI_MAX_RETRIES` 做无默认值可选透传，Backend 删除 legacy 映射和业务配置绑定，但保留专用 removed-key detector；检测到非空旧键时启动 fail-fast 并记录 `SUILEARN_RETRY_CONFIG_REMOVED`，空值仍视为未提供。只有再后续 cleanup change 在残留扫描和运行态证据确认部署环境、根 `.env`、CI/启动脚本均无 legacy 输入后，才能同时删除 Compose 旧键透传与 Backend removed-key detector。最终清理阶段不再承诺对已移除且无法送达 Backend 的旧键 fail-fast。

### 12. 安全、韧性与可观测性

上传同时校验扩展名、MIME、文件签名、大小、页数、解压后大小和嵌套深度。资料正文视为不可信证据，模型提示中不得允许其覆盖系统指令。日志不得包含完整正文、原始模型响应、密钥或临时授权地址。

Actuator/Micrometer 暴露队列深度、最老消息等待时间、阶段耗时/成功率/失败率/重试、OCR 页数、AI 超时/限流/熔断、Outbox 未发送数、死信数，以及 PostgreSQL/RabbitMQ/MinIO 健康状态。日志统一携带 correlationId、taskId、materialId、stage。

健康检查区分 HTTP 服务可用与后台处理依赖健康；RabbitMQ 暂时不可用不能让已完成资料不可阅读。

### 13. 角色与文件所有权

- Product：proposal/specs 的产品行为、验收标准，稳定后同步 `docs/product-requirements.md`。
- Architect：design、`contracts/**`、`docs/architecture.md`、`docs/tech-selection.md` 与依赖选择。
- Server Backend：`services/api/**` 的模型、持久化、任务、解析/OCR、消息、生成和测试。
- Web Frontend：`apps/web/**` 的 multipart 上传、阅读器、知识点与题目交互。
- Test：故障矩阵、契约、集成和端到端运行态验证。
- Reviewer：Spec Review 与 Code Review，Implementer 不自证完成。
- Leader：负责任务顺序、文件锁、范围、验证和 Review 闭环；在用户明确授权的本 change 内串行编排根 `compose.yml` 与 `.env.example`，但不编写 Backend/Web 业务实现。

## Acceptance and Runtime Matrix

| 场景 | 期望 |
| --- | --- |
| Markdown/TXT | 保存原件并生成完整阅读版与结构化知识点草稿 |
| DOC/DOCX | 保存原件，生成阅读版与高保真预览 |
| 文本 PDF | 直接提取，不调用 OCR |
| 扫描 PDF | 自动按页 OCR |
| 混合 PDF | 仅 OCR 文本不足页面 |
| 损坏/伪造文件 | 永久失败并给出原因，不生成知识点 |
| AI 未配置/失败 | 资料可读，不生成关键词占位物 |
| RabbitMQ 中断 | Outbox 保留，恢复后继续投递 |
| 消费者崩溃 | 未 ACK 消息重投且幂等恢复 |
| MinIO 中断 | 新上传失败或任务退避，不返回虚假 READY |
| 重复消息 | 不重复创建 revision、知识点或题目 |
| 删除资料 | 异步清理资产/索引，已保存内容保留删除来源标识 |

运行态必须在 Compose 中验证 API 重启、RabbitMQ 暂停恢复、MinIO 不可用、重复消息、AI/OCR 超时、重试/DLQ、删除清理和指标健康。Android 本地测试证明新中间件不可用时本地学习闭环仍可用。

## Risks / Trade-offs

- [RabbitMQ/MinIO 增加部署复杂度] → Compose 提供健康检查、持久卷、非敏感本地默认值和显式启动矩阵；不再引入 Redis。
- [至少一次投递导致重复执行] → 所有阶段以幂等键、不可变 revision 和唯一约束保护。
- [OCR/LibreOffice/Tesseract 消耗资源] → 独立线程池、并发 1、文件/页数上限、超时和队列隔离。
- [格式解析结果不完全保真] → 原件始终保留；阅读版与高保真预览分开，用户可重新处理或下载原件。
- [Outbox 与对象存储无法原子提交] → 临时 object key、提交后提升、失败补偿和孤儿扫描。
- [AI 结构输出不完整] → 强 schema 校验；不完整结果使任务失败，不保存关键词/占位草稿。
- [旧资料没有原件] → 迁移为 LEGACY_TEXT revision 并明确展示“无原始文件”，不伪造资产。
- [Web 三栏在小屏过密] → 平板折叠侧栏、手机拆路由，保留状态和返回路径。

## Migration Plan

1. 先增加 PostgreSQL 表/列、资产/解析/消息端口和兼容 DTO；不删除旧字段。
2. 在 Compose 和配置中加入 RabbitMQ、MinIO、解析/OCR 运行依赖及健康检查。
3. 更新 OpenAPI 后实现 Backend multipart、资产、revision/block、Outbox 和异步消费者。
4. 将现有 content 映射为 `LEGACY_TEXT` revision；现有 READY、知识点和题目保持可读。
5. 实现结构化知识点与基于知识点的题目生成，移除 AI 失败后的关键词 fallback。
6. Web 切换 multipart 上传和双视图/知识点工作台；旧 JSON 接口仅保留兼容。
7. 运行格式、故障、迁移、契约、Web 和 Android 回归矩阵，完成 Review 闭环。
8. 稳定事实同步到产品、架构、技术选型和契约；后续变更再移除 deprecated JSON 路径。

回滚策略：数据库和契约先做增量兼容；旧读取路径保留到本变更验证结束。若新消费者异常，可停止 listener 并保留 Outbox/队列消息，不回退为同步处理。旧版本仍可读取原 content；新上传的 MinIO 原件和任务保留，修复后继续处理。

## Residual Scan

完成前必须扫描并清除或显式保留为 deprecated：

- Web 对 PDF 使用 `file.text()`。
- “粘贴正文/已转成文本 PDF”旧提示。
- 契约声称不支持真实 PDF/Office 解析的旧说明。
- 关键词候选和统一占位 description fallback。
- import/knowledge/question 在 HTTP 请求线程同步执行的主路径。
- 密钥、正文、模型原始响应或临时授权地址日志。
- `.env.example`、Compose、应用配置或启动脚本中残留旧默认 `SUILEARN_AI_MAX_RETRIES=2`，以及 Compose 对 canonical 新键注入 `0` 或对 legacy 旧键注入任何非空默认值。
- Provider/SDK 内部 retry、自定义/旧手写 retry 与 Resilience4j 形成第二套计数器；Resilience4j 必须是唯一 adapter 即时 retry owner。
- legacy retry 键超出阶段允许位置：当前兼容期只允许 Compose 无默认可选透传、Backend legacy mapper/detector、测试和迁移文档；tombstone 窗口只允许 Compose 无默认可选透传、Backend removed-key detector、测试和移除文档，禁止继续映射或业务绑定；最终 cleanup 后不得残留透传或 detector。
- `correlationId`、`taskId`、`materialId`、文件名、object key、错误消息或其他无界/敏感值进入 metric tags；这些 ID 只能进入结构化日志/trace 或受控 exemplar。
- Markdown raw HTML 未转义、危险/混淆 URL scheme 可点击，或远程图片/外部资源默认产生可加载地址和网络请求。
- 消息幂等键被误作 adapter operation key、页级 `ProcessingOperation`/claim-result 缺失、成功 operation 在消息重投/重启后仍重复调用 adapter，或把单 operation 调用上限误用为 500 页文档的总调用/页数上限。

## Open Questions

已确认的产品与架构决策足以进入实现计划；实现若发现具体解析库许可、部署镜像或契约兼容性与本设计冲突，必须返回 Spec，不在 Build 中自行扩大或改变范围。
