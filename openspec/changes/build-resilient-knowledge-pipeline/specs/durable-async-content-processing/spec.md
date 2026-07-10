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

### Requirement: 任务可观测与可恢复
系统 SHALL 记录任务阶段、进度、尝试、关联 ID、错误和时间，并 SHALL 暴露队列、Outbox、死信、OCR/AI、任务耗时与依赖健康指标。

#### Scenario: 应用重启恢复任务
- **WHEN** 应用重启后存在未发送 Outbox 或超时 RUNNING 任务
- **THEN** 系统重新发布或恢复任务，并保持原 taskId/correlationId 可追踪

#### Scenario: 后台处理依赖异常
- **WHEN** RabbitMQ 暂时异常但 HTTP 与已完成资料读取仍可用
- **THEN** readiness/指标明确后台处理降级，且系统继续允许用户阅读已完成资料
