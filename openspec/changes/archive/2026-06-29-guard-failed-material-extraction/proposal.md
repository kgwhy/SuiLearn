# 修复失败资料的后续处理

## 背景

当前资料导入在 embedding 远端调用失败时会把资料标记为 `FAILED`，但前端仍会继续触发知识点提取。后端知识点提取也未要求资料处于 `READY`，导致失败资料仍可能生成知识点。

同时，已有无 embedding 文本检索链路只覆盖 embedding 未配置的情况；当 embedding 已配置但远端返回 404 等运行时错误时，导入没有降级为文本检索。

## 范围

- embedding 运行时失败时，资料导入降级保存为 text-only chunks，并以 `READY` 完成导入任务。
- embedding 子任务保留失败状态和错误信息，便于排查配置问题。
- 知识点提取只允许对 `READY` 资料执行。
- 前端仅在导入返回 `READY` 时自动提取知识点。

## 非目标

- 不修改 OpenAPI 契约。
- 不新增异步队列或任务重试机制。
- 不更改 AI provider 配置项命名。

## 验收标准

- embedding provider 支持 embedding 但 `embed` 抛错时，导入资料仍返回 `READY`，chunk 为 `TEXT_ONLY`。
- 同场景下 embedding 任务为 `FAILED`，导入任务为 `SUCCEEDED`。
- 对 `FAILED` 资料调用知识点提取会返回错误，不写入知识点。
- 前端不会对 `FAILED` 导入结果自动调用知识点提取。
