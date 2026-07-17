## Purpose

定义耗时内容处理在客户端与持久化任务之间的可追踪交互契约。

## Requirements

### Requirement: 知识点提取客户端使用持久化异步任务

Web 客户端 SHALL 通过 `POST /api/v2/materials/{materialId}/knowledge-point-generations` 提交知识点生成，并以返回的 `taskId` 查询和展示持久化任务状态。Web 客户端 MUST NOT 将已弃用的同步提取端点作为主交互路径，也不得把可预期的结构化生成失败表现为 HTTP 500 或成功提示之外的无上下文错误。

#### Scenario: 用户手动提取知识点

- **WHEN** 用户在 READY 资料上选择提取知识点
- **THEN** Web 收到 HTTP 202 和 taskId，展示该任务状态，并在任务完成后刷新资料和知识点列表

#### Scenario: 结构化生成失败

- **WHEN** 持久化知识点生成任务进入 FAILED
- **THEN** Web 展示任务返回的安全失败说明和重试入口，不将失败呈现为 `Internal Server Error`
