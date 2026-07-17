## ADDED Requirements

### Requirement: 耗时处理异步执行
系统 SHALL 在持久化异步消费者中执行资料解析、OCR、索引、知识点生成和题目生成，且 SHALL NOT 在 HTTP 请求线程执行这些主流程。

#### Scenario: 上传请求快速返回
- **WHEN** 原始文件已安全保存且资料、任务与 Outbox 已提交
- **THEN** API 返回 202、materialId 和 taskId，而不等待解析、OCR、索引或 AI 完成

### Requirement: Transactional Outbox 可靠投递
系统 SHALL 在创建业务任务的同一 PostgreSQL 事务中写入 OutboxEvent，并 SHALL 在 RabbitMQ 暂时不可用时保留未发送事件供恢复后继续投递。

#### Scenario: RabbitMQ 在任务创建后中断
- **WHEN** 业务事务已提交但消息暂时无法发布
- **THEN** Outbox 保持未发送状态，任务保持可追踪，并在 RabbitMQ 恢复后完成投递

### Requirement: 至少一次投递与幂等处理
系统 SHALL 使用 RabbitMQ 持久化队列、手动确认和至少一次投递；每个处理阶段 MUST 使用幂等键防止重复 revision、知识点或题目。

#### Scenario: 消费者提交前崩溃
- **WHEN** 消费者收到消息后在 ACK 前崩溃
- **THEN** RabbitMQ 重新投递消息，消费者从持久化状态恢复且不重复创建结果

#### Scenario: 同一消息重复到达
- **WHEN** 相同 taskId、stage 和 processingVersion 的消息被消费多次
- **THEN** 后续消费返回或复用已有结果，不产生重复业务记录

### Requirement: 持久化 adapter operation 幂等恢复
系统 MUST 将消息/阶段幂等与 adapter operation 幂等分离。消息幂等键 SHALL 使用 taskId、stage 与 revision/processingVersion 防止重复提交阶段业务结果；每个 adapter operation MUST 通过持久化 `ProcessingOperation` 或字段语义等价模型执行 claim/result，记录唯一 operationKey、task/stage、状态、累计 attempt、result reference、adapterVersion、时间戳和脱敏错误。OCR operation key MUST 至少包含 revisionId、pageNumber 与 ocrAdapterVersion；parser、preview 和 AI operation key MUST 分别由稳定输入与对应 adapter/model version 构成。成功 operation MUST 在 ProcessingTask 重试、消息重投和应用重启后复用，只允许重新调度未完成、租约过期或可重试失败的 operation。

#### Scenario: 部分 OCR 页面成功后恢复
- **WHEN** 一份多页 PDF 的部分 OCR 页面已成功持久化 operation 结果，随后消费者消息重投或应用重启
- **THEN** 系统复用已成功页面的 result reference 且不再次调用这些页面的 OCR adapter，只为剩余未完成、租约过期或可重试失败页面执行 claim 和调用

#### Scenario: 单 operation 上限不限制 500 页资料
- **WHEN** 一份合法的 500 页 PDF 每页都需要 OCR
- **THEN** 系统把每页作为独立 operation 分别应用调用上限，不得把单 operation 上限当作文档累计上限而拒绝、跳过或截断剩余页面

#### Scenario: 非 OCR operation 结果复用
- **WHEN** parser、preview 或 AI operation 已以稳定输入和 adapter/model version 保存成功 result reference，随后相同 ProcessingTask 阶段恢复
- **THEN** 系统复用该结果而不重复调用对应 adapter；只有稳定输入或 adapter/model version 改变时才形成新的 operationKey

### Requirement: 有界重试与死信
系统 SHALL 区分暂时错误与永久错误；暂时错误 SHALL 使用有界退避重试，达到最大尝试次数后进入死信队列，永久错误 SHALL 直接失败。

#### Scenario: 外部服务暂时超时
- **WHEN** OCR 或 AI 调用发生可重试超时且未达到最大尝试次数
- **THEN** 任务进入 RETRY_WAIT，并在 nextRetryAt 后重新排队

#### Scenario: 文件永久损坏
- **WHEN** 解析器确认文件损坏、格式伪造或无法安全处理
- **THEN** 任务直接进入 FAILED，不执行无意义重试，并向用户展示恢复建议

### Requirement: 依赖故障不产生静默降级
系统 MUST NOT 在 RabbitMQ、MinIO、AI 或 OCR 缺失时静默切换到同步处理、关键词 fallback 或虚假成功。

#### Scenario: AI 未配置
- **WHEN** 资料阅读版已完成但 AI chat 未配置
- **THEN** 资料保持 READY，知识点生成明确不可用或失败，且不创建关键词占位知识点

#### Scenario: MinIO 不可用
- **WHEN** 用户上传原始文件而 MinIO 无法持久化原件
- **THEN** 系统拒绝上传或明确失败，不创建可被误认为成功的资料

### Requirement: 默认配置与环境覆盖
系统 SHALL 默认开启异步处理、按需 OCR、原件保留和知识点自动生成；默认最大文件 50 MB、PDF 500 页、处理并发 2、OCR 并发 1、最大尝试 3，并 SHALL 允许通过环境变量覆盖。

#### Scenario: 使用默认配置启动完整栈
- **WHEN** 用户通过项目 Compose 且未覆盖处理参数启动系统
- **THEN** 系统使用记录的安全默认值，并通过健康检查说明 PostgreSQL、RabbitMQ 和 MinIO 状态

#### Scenario: 禁用异步资料处理
- **WHEN** 异步处理功能开关被显式关闭
- **THEN** 系统禁用新文件上传并说明原因，而不是回退旧同步主路径

### Requirement: Adapter retry 配置兼容迁移
系统 SHALL 以 `SUILEARN_ADAPTER_MAX_RETRIES` 作为 adapter 即时重试的 canonical key，应用层默认 `0` 且只接受整数 `0..1`。系统 SHALL 将 `SUILEARN_AI_MAX_RETRIES` 保留一个 deprecated 兼容周期；兼容周期内，Compose SHALL 同时对新旧键执行无默认值可选透传，空字符串 SHALL 视为未显式提供，且 `.env.example` SHALL 只记录新键的非敏感目标默认 `0`，不得继续提供旧键或旧默认 `2`。兼容周期后的第一个具名 removal change SHALL 保留 Compose 旧键可选透传并以专用 detector 替代 legacy 映射；只有再后续 cleanup change 在残留扫描和运行态证据确认无 legacy 输入后，才可删除旧键透传与 detector。

#### Scenario: 新旧键均未非空提供
- **WHEN** 新旧 retry 键均缺失、为空或只有 Compose 的空值透传
- **THEN** Backend 使用应用层默认 `0`，且 Compose 未为新键注入默认值来伪造显式输入

#### Scenario: 仅 canonical 新键非空
- **WHEN** 仅 `SUILEARN_ADAPTER_MAX_RETRIES` 以非空值显式提供
- **THEN** Backend 按整数 `0..1` 校验并使用，超出范围或非整数时 fail-fast

#### Scenario: 旧 env 配置在兼容周期继续生效
- **WHEN** 根 `.env` 或部署环境只以非空值提供 deprecated `SUILEARN_AI_MAX_RETRIES`
- **THEN** Compose 将旧值透传给 Backend 而不注入新键默认，Backend 将 `0` 映射为 `0`、正整数映射为 `1`，并记录 `SUILEARN_RETRY_CONFIG_LEGACY_MAPPED`

#### Scenario: 新旧键同时非空
- **WHEN** `SUILEARN_ADAPTER_MAX_RETRIES` 与 `SUILEARN_AI_MAX_RETRIES` 同时以非空值显式提供
- **THEN** Backend 启动 fail-fast 并记录 `SUILEARN_RETRY_CONFIG_CONFLICT`，无论两个值是否相同都不得静默选择优先级

#### Scenario: removal change 提供 tombstone 错误窗口
- **WHEN** 当前兼容周期结束后的第一个具名 removal change 已部署，且 Compose 收到非空 `SUILEARN_AI_MAX_RETRIES`
- **THEN** Compose 仍将旧键无默认透传给 Backend，Backend 不再映射或业务绑定旧值，而是启动 fail-fast 并记录 `SUILEARN_RETRY_CONFIG_REMOVED`

#### Scenario: 有证据后清理 tombstone
- **WHEN** tombstone 错误窗口已完整运行，且残留扫描和运行态证据确认部署环境、根 `.env`、CI 与启动脚本均未再提供 legacy 键
- **THEN** 再后续 cleanup change 可以同时删除 Compose 旧键透传与 Backend removed-key detector，不得在缺少上述证据时提前删除检测链路

### Requirement: 任务可观测与可恢复
系统 SHALL 记录任务阶段、进度、尝试、关联 ID、错误和时间，并 SHALL 暴露队列、Outbox、死信、OCR/AI、任务耗时与依赖健康指标。

#### Scenario: 应用重启恢复任务
- **WHEN** 应用重启后存在未发送 Outbox 或超时 RUNNING 任务
- **THEN** 系统重新发布或恢复任务，并保持原 taskId/correlationId 可追踪

#### Scenario: 后台处理依赖异常
- **WHEN** RabbitMQ 暂时异常但 HTTP 与已完成资料读取仍可用
- **THEN** readiness/指标明确后台处理降级，且系统继续允许用户阅读已完成资料
