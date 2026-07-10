## Why

当前知识库把本地文件先当作文本读取，不能可靠解析 PDF/Word、保留并查看原始资料，也会在 AI 不可用时把关键词和占位描述冒充为知识点。用户需要一条可恢复、可追溯的学习资料流水线：完整导入与阅读原始资料，生成真正有总结和证据的知识点，再从知识点直接生成面试题。

## What Changes

- Web 改为上传 Markdown、TXT、PDF、DOC/DOCX 原始文件，不再对 PDF 使用 `file.text()` 或要求用户先粘贴/转换正文。
- 同时保留原始文件、统一阅读版、解析 revision、结构化正文块和精确来源引用；用户可切换阅读版与原始文件并下载原件。
- 对 PDF 优先提取文本，文本不足的页面自动 OCR；Word 和 Markdown/TXT 通过格式适配器生成阅读版。
- 将资料解析、OCR、索引、知识点生成和题目生成改为 RabbitMQ 驱动的异步任务；使用 PostgreSQL Transactional Outbox 保证投递，MinIO 保存原件和衍生产物。
- 知识点改为“标题 + 简短总结 + 定义 + 原理 + 应用场景 + 易错点 + 原文引用”的待确认草稿；取消关键词和统一占位描述 fallback。
- 知识点详情提供默认一键生成面试题和可展开的题型、难度、数量设置；生成结果继续经过用户审核后才进入正式题库。
- 引入 Resilience4j、Actuator/Micrometer、重试队列和死信队列，提供超时、熔断、幂等、恢复和运行态观测能力。
- 兼容现有文本资料和知识点；旧正文迁移为 legacy revision，旧 JSON 文本导入接口保留一个兼容周期并标记 deprecated。

## Scope

- Web 知识库工作台的资料上传、处理状态、双视图阅读、知识点审核和基于知识点生成题目。
- Backend 的文件存储、格式解析/OCR、revision/block、任务/Outbox/RabbitMQ、结构化知识点和题目生成。
- OpenAPI、PostgreSQL 持久化、Docker Compose、环境变量与健康/指标端点。
- 现有资料和知识点的兼容迁移，以及端到端故障恢复验证。

## Non-goals

- 不拆分独立微服务或单独部署 Worker；消费者先运行在同一 Backend 应用的隔离线程池中。
- 不引入 Redis、独立向量数据库、账号、多租户、云同步或知识库市场。
- 不修改 Android 本地刷题闭环；Android 仅继续按需消费远程能力。
- 不承诺复杂 Office 宏、脚本、嵌入对象或任意未知格式的执行/渲染。
- 当前个人使用阶段不强制引入 ClamAV；若开放非可信用户上传再单独评估。

## Capabilities

### New Capabilities

- `multi-format-material-ingestion`: 原始资料上传、MinIO 资产、PDF/Word/Markdown/TXT 解析、按页 OCR、双视图阅读、revision 与来源定位。
- `durable-async-content-processing`: Outbox、RabbitMQ、幂等消费、重试/死信、恢复、配置默认值、健康检查和指标。
- `structured-knowledge-points`: 基于资料证据生成分层、可审核、可版本追溯的结构化知识点，禁止关键词占位 fallback。
- `knowledge-point-interview-questions`: 从已确认知识点一键或按题型/难度/数量生成可审核的面试题草稿。

### Modified Capabilities

- 无；当前 `openspec/specs/` 尚无可增量修改的主规格，本变更以新 capability 规格记录行为。

## Acceptance Criteria

- Markdown、TXT、PDF、DOC/DOCX 均可作为原始文件导入；文本 PDF 不触发 OCR，扫描或混合 PDF 仅对文本不足页面触发 OCR。
- 导入后可以阅读完整统一阅读版，并查看或下载原始资料；解析/OCR 失败不丢失原件。
- 资料处理通过持久化异步任务完成，API 不在请求线程执行解析、OCR、知识点或题目生成。
- RabbitMQ 中断、消费者崩溃和重复消息场景可恢复且不产生重复 revision、知识点或题目。
- AI 未配置或调用失败时，资料仍可阅读，但系统不得生成关键词或占位描述充当知识点。
- 知识点列表显示标题与简短总结；详情包含定义、原理、场景、易错点和可跳转的原文引用。
- 用户可从已确认知识点默认一键生成一道中等难度简答题，也可设置题型、难度和数量；题目审核保存后才进入正式题库。
- 现有资料、知识点和已保存题目不丢失；Android 本地刷题闭环不依赖新中间件。

## Impact

- Backend：`services/api/**` 的 material、task、knowledgepoint、generation、persistence、config、health/metrics 模块与依赖。
- Web：`apps/web/src/**` 的资料上传、阅读、任务状态、知识点与生成交互。
- Contracts：`contracts/openapi/suilearn-v2.yaml` 的 multipart、资产/revision、任务、结构化知识点和生成请求/响应。
- Infrastructure：`compose.yml`、`.env.example`、RabbitMQ、MinIO、OCR adapter 与相关 Docker/运行配置。
- Persistence：新增资料资产、revision/block、Outbox、幂等与结构化知识点字段；迁移保持增量兼容。
- Dependencies：RabbitMQ client、MinIO client、文档解析/OCR adapter、Resilience4j、Actuator/Micrometer 和集成测试依赖。

## Affected Current-Fact Documents

- `docs/product-requirements.md`：资料导入、完整阅读、结构化知识点和知识点出题行为。
- `docs/architecture.md`：模块边界、异步数据流、资产/revision、Outbox 与中间件关系。
- `docs/tech-selection.md`：RabbitMQ、MinIO、OCR/Office/PDF 解析、Resilience4j、Actuator/Micrometer 的技术基线与配置约束。
- `contracts/openapi/suilearn-v2.yaml`：跨端 API 单点真相。
