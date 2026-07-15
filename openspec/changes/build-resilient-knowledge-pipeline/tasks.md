## 执行批次

- Batch A：1.2 契约。
- Batch B：2.1–2.5 可靠底座。
- Batch C：3.1–3.4 导入/OCR。
- Batch D：4.1–4.3 知识点/出题。
- Batch E：5.1–5.3 Web。
- Batch F：6.1–7.3 集成收口。

每个任务仍执行任务内 TDD/局部测试；独立 Test、Spec Review、Code Review 在批次末统一执行。批次审查通过后再统一勾选该批次任务，Task 1.1 已按原逐任务 L3 完成。

## 1. 架构与契约先行

- [x] 1.1 更新技术与架构基线，正式记录 RabbitMQ、MinIO、Tika/PDFBox/POI、LibreOffice/Tesseract adapter、Resilience4j、Actuator/Micrometer、模块边界、默认值和回退语义。
  - Owner: Architect Agent
  - Allowed files: `docs/architecture.md`, `docs/tech-selection.md`, `openspec/changes/build-resilient-knowledge-pipeline/**`
  - Forbidden files: `services/api/**`, `apps/web/**`, `apps/android/**`, `contracts/**`, `docs/proposals/**`, `docs/superpowers/**`
  - Test command: `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef ff08b45e58b50ae3cef15c6f96c8d8874dbce0b0`
  - Review focus: 技术依赖有明确收益/替代/默认值/覆盖口/回退方式；不把模块边界误写成独立微服务；当前事实与 Build 目标分离；operation 级重试、页级 OCR 幂等、指标基数、Markdown 安全和 retry 配置兼容语义无歧义。

- [x] 1.2 先更新 OpenAPI，定义 multipart 上传、202 material/task submission、原件/阅读版/revision、结构化知识点审核和知识点面试题批量生成契约。
  - Owner: Architect Agent
  - Allowed files: `contracts/openapi/suilearn-v2.yaml`, `openspec/changes/build-resilient-knowledge-pipeline/**`
  - Forbidden files: `services/api/**`, `apps/web/**`, `apps/android/**`, `docs/proposals/**`, `docs/superpowers/**`
  - Test command: `mvn -f services/api/pom.xml test -q`
  - Review focus: 兼容旧 JSON 导入一个周期并标记 deprecated；契约字段足以表达异步任务、版本来源和批量草稿，且无密钥/对象 key 泄露。

- [x] 1.3 固化风险自适应批次工作流，在不降低 TDD、独立审查、问题闭环和最终验证的前提下，以证据指纹复用、取消协议、紧凑日志/Git 摘要和 worktree 安全目录初始化减少重复上下文、重复全量测试、重复审查与冗长日志传递。
  - Owner: Leader Agent
  - Allowed files: `docs/development-workflow.md`, `agents/leader.md`, `.agents/skills/suilearn-workflow/references/subagent-loop.md`, `scripts/check-suilearn-workflow.ps1`, `openspec/changes/build-resilient-knowledge-pipeline/**`
  - Forbidden files: `services/**`, `apps/**`, `contracts/**`, `docs/product-requirements.md`, `docs/architecture.md`, `docs/tech-selection.md`, `docs/proposals/**`, `docs/superpowers/**`
  - Test command: `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -SelfTestEfficientBatchPolicy`; `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef ff08b45e58b50ae3cef15c6f96c8d8874dbce0b0`; `openspec validate build-resilient-knowledge-pipeline --strict`; `git diff --check`
  - Review focus: 批次按依赖、文件重叠和风险域划分；高风险事件即时审查；任务内局部测试、批次独立审查和最终全量验证不被省略；成功证据紧凑但可追溯，失败证据保留关键原始输出。

## 2. 中间件、持久化与可靠任务底座

- [x] 2.1 在用户已确认的中间件范围内编排 RabbitMQ、MinIO、持久卷、健康检查和根环境变量示例，并按兼容周期配置 adapter retry 新旧键的可选透传。
  - Owner: Leader Agent
  - Allowed files: `compose.yml`, `.env.example`, `openspec/changes/build-resilient-knowledge-pipeline/verification.md`, `openspec/changes/build-resilient-knowledge-pipeline/tasks.md`
  - Forbidden files: `services/**`, `apps/**`, `contracts/**`, `docs/**`, 其他 `openspec/changes/**`
  - Test command: `docker compose config`; `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef ff08b45e58b50ae3cef15c6f96c8d8874dbce0b0`
  - Review focus: 本地默认值非敏感；服务名、持久卷、依赖和健康条件稳定；根共享配置不混入业务实现；不新增 Redis 或独立 Worker 服务；`.env.example` 只记录 `SUILEARN_ADAPTER_MAX_RETRIES=0`，Compose 对新旧 retry 键都使用无默认值可选透传且空值视为未提供，缺失/空值/仅旧键/新旧并存矩阵可由 `docker compose config` 验证。

- [x] 2.2 以测试先行方式加入 RabbitMQ、MinIO、解析/OCR、Resilience4j、Actuator/Micrometer 和 Testcontainers 依赖及 Backend 运行配置；此任务已提供 DOCX 所需的 `poi-ooxml`，二进制 OLE `.doc` 所需的 `poi-scratchpad` 由未完成的 Task 3.2 单独补充。
  - Owner: Server Backend Agent
  - Allowed files: `services/api/pom.xml`, `services/api/src/main/resources/**`, `services/api/src/main/java/com/suilearn/api/config/**`, `services/api/src/main/java/com/suilearn/api/ai/OpenAiCompatibleAiProvider.java`, `services/api/src/main/java/com/suilearn/api/retrieval/OpenAiCompatibleEmbeddingProvider.java`, `services/api/src/test/**`, `services/api/Dockerfile`, `services/api/config/**`
  - Forbidden files: `apps/**`, `contracts/**`, `docs/**`, `compose.yml`, `.env.example`, `openspec/changes/**`（任务勾选和验证记录除外）
  - Test command: `mvn -f services/api/pom.xml test -q`
  - Review focus: 异步开关关闭时禁用上传而非同步 fallback；API/消费者线程池隔离；健康检查区分 HTTP 与处理依赖；测试依赖不泄漏到业务接口；应用层默认 adapter retry 为 0，空值视为未提供，仅旧键执行 `0→0/正整数→1` 映射并诊断，新旧键同时非空以 `SUILEARN_RETRY_CONFIG_CONFLICT` fail-fast，Provider SDK/旧手写 retry 不得形成第二套计数器。

- [x] 2.3 以失败测试定义并实现 MaterialAsset、DocumentRevision、DocumentBlock、扩展 ProcessingTask、ProcessingOperation、OutboxEvent 和结构化知识点的增量持久化映射。
  - Owner: Server Backend Agent
  - Allowed files: `services/api/src/main/java/com/suilearn/api/model/**`, `services/api/src/main/java/com/suilearn/api/persistence/**`, `services/api/src/main/java/com/suilearn/api/**/infrastructure/**`, `services/api/src/test/**`
  - Forbidden files: `apps/**`, `contracts/**`, `docs/**`, `compose.yml`, `.env.example`, `openspec/changes/**`（任务勾选和验证记录除外）
  - Test command: `mvn -f services/api/pom.xml test -q`
  - Review focus: schema 只增量演进；revision 不可变；ProcessingOperation 持久化 operationKey、task/stage、状态、attempt、result reference、adapterVersion、时间和脱敏错误；唯一约束保护消息级与 operation 级幂等；现有资料/知识点/题目不丢失。

- [x] 2.4 以失败测试定义并实现 Transactional Outbox、RabbitMQ publisher confirm/manual ack、队列隔离、有界 retry/DLQ、消息与 adapter operation 两级幂等 claim 和重启恢复。
  - Owner: Server Backend Agent
  - Allowed files: `services/api/src/main/java/com/suilearn/api/task/**`, `services/api/src/main/java/com/suilearn/api/config/**`, `services/api/src/main/java/com/suilearn/api/persistence/**`, `services/api/src/test/**`
  - Forbidden files: `apps/**`, `contracts/**`, `docs/**`, `compose.yml`, `.env.example`, `openspec/changes/**`（任务勾选和验证记录除外）
  - Test command: `mvn -f services/api/pom.xml test -q`
  - Review focus: 业务提交与事件投递无丢失窗口；ACK 在结果提交后；重复消息不重复写；operation 只调度未完成或可重试失败项并复用成功 result reference；永久错误不重试；死信可追踪/可人工重试。

- [x] 2.5 以失败测试定义并实现 MinIO AssetStorage port、流式上传、私有读取、校验、临时对象提升、孤儿清理和删除清理任务。
  - Owner: Server Backend Agent
  - Allowed files: `services/api/src/main/java/com/suilearn/api/material/**`, `services/api/src/main/java/com/suilearn/api/config/**`, `services/api/src/main/java/com/suilearn/api/persistence/**`, `services/api/src/test/**`
  - Forbidden files: `apps/**`, `contracts/**`, `docs/**`, `compose.yml`, `.env.example`, `openspec/changes/**`（任务勾选和验证记录除外）
  - Test command: `mvn -f services/api/pom.xml test -q`
  - Review focus: 文件不整份载入内存；bucket 不公开；object key 不使用用户文件名；数据库失败可补偿；API 不泄露永久凭据。

## 3. 多格式解析、OCR 与资料版本

- [x] 3.1 先建立 Markdown/TXT/文本 PDF/扫描 PDF/混合 PDF/DOC/DOCX/损坏与伪造文件测试语料和 parser contract 测试；`.doc` 语料必须是最小化、生成或版权安全的真实二进制 OLE `.doc`，不得用改名 RTF 伪造。
  - Owner: Test Agent
  - Allowed files: `services/api/src/test/**`, `services/api/src/test/resources/**`, `openspec/changes/build-resilient-knowledge-pipeline/verification.md`
  - Forbidden files: `services/api/src/main/**`, `apps/**`, `contracts/**`, `docs/**`, `compose.yml`, `.env.example`
  - Test command: `mvn -f services/api/pom.xml test -q`
  - Review focus: fixtures 小而合法、覆盖页码/结构/OCR 判定/安全边界，不使用含隐私或版权风险的真实用户资料。

- [x] 3.2 实现格式检测、Tika/PDFBox/POI/CommonMark 解析、文本密度判断、DocumentRevision/Block 生成和索引接入，使 3.1 测试通过；为支持真实二进制 OLE `.doc`，可且仅可在 `services/api/pom.xml` 中于既有 `poi-ooxml` 旁新增 `org.apache.poi:poi-scratchpad` 依赖。
  - Owner: Server Backend Agent
  - Allowed files: `services/api/pom.xml`（仅新增 `org.apache.poi:poi-scratchpad`，与既有 `poi-ooxml` 配套）, `services/api/src/main/java/com/suilearn/api/material/**`, `services/api/src/main/java/com/suilearn/api/retrieval/**`, `services/api/src/test/**`
  - Forbidden files: `apps/**`, `contracts/**`, `docs/**`, `compose.yml`, `.env.example`, `openspec/changes/**`（任务勾选和验证记录除外）
  - Test command: `mvn -f services/api/pom.xml test -q`
  - Review focus: 文本 PDF 不 OCR；混合 PDF 仅 OCR 缺失页；章节/页码/block 顺序稳定；宏/脚本/嵌入对象不执行。

- [x] 3.3 实现 Tesseract OCR adapter、LibreOffice headless 高保真预览、并发/超时/熔断与失败隔离，使扫描 PDF 和 Word 验收通过。
  - Owner: Server Backend Agent
  - Allowed files: `services/api/src/main/java/com/suilearn/api/material/**`, `services/api/src/main/java/com/suilearn/api/config/**`, `services/api/src/test/**`, `services/api/Dockerfile`
  - Forbidden files: `apps/**`, `contracts/**`, `docs/**`, `compose.yml`, `.env.example`, `openspec/changes/**`（任务勾选和验证记录除外）
  - Test command: `mvn -f services/api/pom.xml test -q`; `docker compose config`
  - Review focus: OCR 并发默认 1；外部进程参数不可注入；超时可终止；预览失败不丢原件；OCR operation key 固定到 revision/page/adapterVersion，重试或重启复用成功页面、只处理剩余页面；单 operation 调用上限不得误限整份 500 页 PDF。

- [x] 3.4 将 material import orchestration 切换为 multipart + Outbox 异步流水线，并实现 legacy content 到 LEGACY_TEXT revision 的增量兼容迁移。
  - Owner: Server Backend Agent
  - Allowed files: `services/api/src/main/java/com/suilearn/api/controller/**`, `services/api/src/main/java/com/suilearn/api/dto/**`, `services/api/src/main/java/com/suilearn/api/material/**`, `services/api/src/main/java/com/suilearn/api/task/**`, `services/api/src/main/java/com/suilearn/api/persistence/**`, `services/api/src/test/**`
  - Forbidden files: `apps/**`, `contracts/**`, `docs/**`, `compose.yml`, `.env.example`, `openspec/changes/**`（任务勾选和验证记录除外）
  - Test command: `mvn -f services/api/pom.xml test -q`
  - Review focus: Controller 与契约一致；请求线程不跑耗时处理；旧 JSON 路径仅兼容/deprecated；旧 READY/知识点/题目保持可读。

## 4. 结构化知识点与面试题

- [x] 4.1 先补知识点 schema、无 AI、AI 不完整、失败重试、审核状态、revision 过期和 legacy 兼容测试。
  - Owner: Test Agent
  - Allowed files: `services/api/src/test/java/com/suilearn/api/knowledgepoint/**`, `services/api/src/test/java/com/suilearn/api/ai/**`, `services/api/src/test/java/com/suilearn/api/generation/**`, `services/api/src/test/resources/**`
  - Forbidden files: `services/api/src/main/**`, `apps/**`, `contracts/**`, `docs/**`, `compose.yml`, `.env.example`
  - Test command: `mvn -f services/api/pom.xml test -q`
  - Review focus: 必需字段不可被占位补齐；AI 未配置/失败不产生关键词；已确认知识点不被重新生成覆盖。

- [x] 4.2 实现结构化知识点生成/审核/来源定位/过期标记，删除候选词与统一 description fallback，使 4.1 测试通过。
  - Owner: Server Backend Agent
  - Allowed files: `services/api/src/main/java/com/suilearn/api/knowledgepoint/**`, `services/api/src/main/java/com/suilearn/api/ai/**`, `services/api/src/main/java/com/suilearn/api/source/**`, `services/api/src/main/java/com/suilearn/api/material/**`, `services/api/src/main/java/com/suilearn/api/model/**`, `services/api/src/main/java/com/suilearn/api/persistence/**`, `services/api/src/test/**`（Batch D Review 扩展：持久化结构化字段、revision 引用与过期标记）
  - Forbidden files: `apps/**`, `contracts/**`, `docs/**`, `compose.yml`, `.env.example`, `openspec/changes/**`（任务勾选和验证记录除外）
  - Test command: `mvn -f services/api/pom.xml test -q`
  - Review focus: DRAFT/CONFIRMED/REJECTED/ARCHIVED 状态清晰；只有 CONFIRMED 可正式消费；引用固定到 revision/block；无旧 fallback 残留。

- [x] 4.3 先补默认/高级参数、批量数量、证据关联、审核门禁和失败隔离测试，再实现知识点面试题异步生成。
  - Owner: Server Backend Agent
  - Allowed files: `services/api/src/main/java/com/suilearn/api/generation/**`, `services/api/src/main/java/com/suilearn/api/ai/**`, `services/api/src/main/java/com/suilearn/api/controller/**`, `services/api/src/main/java/com/suilearn/api/dto/**`, `services/api/src/main/java/com/suilearn/api/model/**`, `services/api/src/main/java/com/suilearn/api/persistence/**`, `services/api/src/main/java/com/suilearn/api/task/**`, `services/api/src/test/**`（Batch D Review 扩展：durable 异步提交、任务结果持久化与草稿查询）
  - Forbidden files: `apps/**`, `contracts/**`, `docs/**`, `compose.yml`, `.env.example`, `openspec/changes/**`（任务勾选和验证记录除外）
  - Test command: `mvn -f services/api/pom.xml test -q`
  - Review focus: 默认 1/中等/简答；高级参数受限；只有 CONFIRMED 知识点可生成；每题保留证据；未经保存不进入正式闭环。

## 5. Web 工作台适配

- [ ] 5.1 先更新 Web 类型/API contract tests，使其消费已稳定的 multipart、202 task、资产/revision、结构化知识点和批量题目契约。
  - Owner: Web Frontend Agent
  - Allowed files: `apps/web/src/api.ts`, `apps/web/src/types.ts`, `apps/web/src/api.contract.test.mjs`, `apps/web/src/workbench-ui.test.mjs`
  - Forbidden files: `services/**`, `apps/android/**`, `contracts/**`, `docs/**`, `compose.yml`, `.env.example`, `openspec/changes/**`（任务勾选和验证记录除外）
  - Test command: `npm --prefix apps/web test`; `npm --prefix apps/web run build`
  - Review focus: 前端不自行扩展契约；不再把 PDF 当文本；异步状态与批量草稿类型准确。

- [ ] 5.2 实现拖放/文件选择 multipart 上传、任务进度、失败重试、资料导航、阅读版/原件/下载和来源定位交互。
  - Owner: Web Frontend Agent
  - Allowed files: `apps/web/src/App.tsx`, `apps/web/src/styles.css`, `apps/web/src/api.ts`, `apps/web/src/types.ts`, `apps/web/src/*.test.mjs`
  - Forbidden files: `services/**`, `apps/android/**`, `contracts/**`, `docs/**`, `compose.yml`, `.env.example`, `openspec/changes/**`（任务勾选和验证记录除外）
  - Test command: `npm --prefix apps/web test`; `npm --prefix apps/web run build`
  - Review focus: 完整正文无固定小滚动框；原件/阅读版可达；状态非仅颜色；键盘/aria/44px；返回保留阅读位置。

- [ ] 5.3 实现知识点标题+短总结列表、结构化详情/引用跳转/审核，以及默认一键与渐进设置的面试题生成/审核体验。
  - Owner: Web Frontend Agent
  - Allowed files: `apps/web/src/App.tsx`, `apps/web/src/styles.css`, `apps/web/src/api.ts`, `apps/web/src/types.ts`, `apps/web/src/*.test.mjs`
  - Forbidden files: `services/**`, `apps/android/**`, `contracts/**`, `docs/**`, `compose.yml`, `.env.example`, `openspec/changes/**`（任务勾选和验证记录除外）
  - Test command: `npm --prefix apps/web test`; `npm --prefix apps/web run build`
  - Review focus: 技术字段默认隐藏；只有已确认知识点可出题；草稿审核门禁清楚；手机/平板/桌面信息层级不丢失。

## 6. 韧性、安全与集成验证

- [ ] 6.1 加入 Testcontainers 集成测试，覆盖 PostgreSQL/Outbox、RabbitMQ 中断/恢复/重复投递/DLQ、MinIO 临时对象/清理、消费者重启恢复和部分 OCR 页面 operation 复用。
  - Owner: Test Agent
  - Allowed files: `services/api/src/test/**`, `openspec/changes/build-resilient-knowledge-pipeline/verification.md`
  - Forbidden files: `services/api/src/main/**`, `services/api/pom.xml`, `apps/**`, `contracts/**`, `docs/**`, `compose.yml`, `.env.example`
  - Test command: `mvn -f services/api/pom.xml test -q`
  - Review focus: 测试证明消息与 operation 两级故障恢复和幂等，不只验证 happy path；已成功 OCR 页面在重投/重启后调用次数不增加，未完成页面继续处理；容器/测试数据可重复清理；无网络或外部真实 AI 依赖。

- [ ] 6.2 完成文件安全、模型提示注入边界、日志脱敏、Actuator/Micrometer 指标和健康分层测试与实现。
  - Owner: Server Backend Agent
  - Allowed files: `services/api/src/main/java/com/suilearn/api/config/**`, `services/api/src/main/java/com/suilearn/api/material/**`, `services/api/src/main/java/com/suilearn/api/ai/**`, `services/api/src/main/java/com/suilearn/api/task/**`, `services/api/src/main/resources/**`, `services/api/src/test/**`
  - Forbidden files: `apps/**`, `contracts/**`, `docs/**`, `compose.yml`, `.env.example`, `openspec/changes/**`（任务勾选和验证记录除外）
  - Test command: `mvn -f services/api/pom.xml test -q`
  - Review focus: 文件签名/解压限制/外部进程参数安全；正文不覆盖系统指令；日志不含正文/密钥/临时 URL；指标不含高基数正文标签。

- [ ] 6.3 在 Compose 中执行格式与故障验收矩阵，验证 API 重启、RabbitMQ 暂停恢复、MinIO 失败、OCR/AI 超时、重复消息、删除清理和指标。
  - Owner: Test Agent
  - Allowed files: `openspec/changes/build-resilient-knowledge-pipeline/verification.md`, `services/api/src/test/**`, `apps/web/src/*.test.mjs`, `scripts/**`（仅经 Leader 批准的验证脚本）
  - Forbidden files: 未经另行声明的业务实现文件、`contracts/**`, `docs/proposals/**`, `docs/superpowers/**`
  - Test command: `docker compose config`; `docker compose up -d --build`; `docker compose ps`; `mvn -f services/api/pom.xml test -q`; `npm --prefix apps/web test`; `npm --prefix apps/web run build`
  - Review focus: 记录原始证据、默认值与覆盖值；验证 retry 新旧键缺失/空值/仅新/仅旧/双非空/非法值及诊断码，验证 operation 调用上限和页级恢复、Markdown raw HTML/危险 URL/远程资源策略、metric tags 低基数；失败项不得用单元测试替代运行态验证；完成后停止测试资源但保留持久数据策略说明。

- [ ] 6.4 运行 Android 本地回归，确认新中间件和远程契约变化不破坏离线刷题闭环。
  - Owner: Android Agent
  - Allowed files: `apps/android/src/test/**`, `apps/android/src/main/**`（仅契约兼容确有需要时，须先扩展声明）, `openspec/changes/build-resilient-knowledge-pipeline/verification.md`
  - Forbidden files: `services/**`, `apps/web/**`, `contracts/**`, `docs/**`, `compose.yml`, `.env.example`
  - Test command: `.\gradlew.bat :app:testDebugUnitTest --no-daemon`; `.\gradlew.bat :app:assembleDebug --no-daemon`
  - Review focus: 未配置 Backend/RabbitMQ/MinIO/AI 时内置 Java 学习包、记录、错题、收藏和统计仍可用。

## 7. 同步、残留扫描与审查闭环

- [ ] 7.1 同步已实现且已验证的稳定产品事实，并明确 legacy/deprecated/后续移除边界。
  - Owner: Product Agent
  - Allowed files: `docs/product-requirements.md`, `openspec/changes/build-resilient-knowledge-pipeline/verification.md`
  - Forbidden files: `services/**`, `apps/**`, `contracts/**`, `docs/architecture.md`, `docs/tech-selection.md`, `docs/proposals/**`, `docs/superpowers/**`
  - Test command: `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef ff08b45e58b50ae3cef15c6f96c8d8874dbce0b0`
  - Review focus: 只同步已验证稳定事实；依据标明为本轮用户对话；不把未实现假设写入 Current Spec。

- [ ] 7.2 执行残留扫描、OpenSpec/工作流检查、全量测试和 `git diff <base_ref> --stat`，补齐 verification 证据。
  - Owner: Leader Agent
  - Allowed files: `openspec/changes/build-resilient-knowledge-pipeline/verification.md`, `openspec/changes/build-resilient-knowledge-pipeline/tasks.md`
  - Forbidden files: 未经任务卡授权的业务/契约/当前事实文件、`docs/proposals/**`, `docs/superpowers/**`
  - Test command: `mvn -f services/api/pom.xml test -q`; `npm --prefix apps/web test`; `npm --prefix apps/web run build`; `.\gradlew.bat :app:testDebugUnitTest --no-daemon`; `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef ff08b45e58b50ae3cef15c6f96c8d8874dbce0b0`; `git diff ff08b45e58b50ae3cef15c6f96c8d8874dbce0b0 --stat`
  - Review focus: 清除 `file.text()` PDF、旧文案、PDF/Office 不支持声明、关键词 fallback、同步主路径和敏感日志；扫描旧 retry 默认 `2`、错误 Compose 默认注入、Provider/SDK/手写 retry、legacy 键非允许位置、ID metric tags、Markdown raw HTML/危险 URL/远程资源自动加载和未复用成功 operation；所有变更在 policy 范围。

- [ ] 7.3 由独立 Reviewer 完成 Spec Review 与 Code Review，所有 P0/P1/P2 均已修复，或经用户批准迁移到具名 follow-up change 后，记录 Major Review 结论。
  - Owner: Reviewer Agent
  - Allowed files: `openspec/changes/build-resilient-knowledge-pipeline/verification.md`, `openspec/changes/build-resilient-knowledge-pipeline/tasks.md`
  - Forbidden files: 业务实现文件、`contracts/**`, `docs/**`（除上述 verification/tasks）, `docs/proposals/**`, `docs/superpowers/**`
  - Test command: 复核 7.2 原始命令结果，并按发现要求责任 Agent 重跑受影响验证
  - Review focus: 规格一致性、数据迁移、消息/operation 两级幂等、对象补偿、安全、契约兼容、UI 可访问性、运行态故障矩阵；所有 P0/P1/P2 已修复或经用户批准迁移到具名 follow-up change；Implementer 不自证完成。
