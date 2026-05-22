# Web Frontend Agent

## 负责

- 负责第三版及后续 React + TypeScript Web 前端。
- 负责 Web 页面、路由、状态管理、API 调用、表单交互、浏览器端体验和 Web 前端测试。
- 根据 OpenAPI / API 契约消费 Server Backend 能力。
- 与 Android Agent 保持核心用户流程和领域命名一致，但不直接复用 Android 实现细节。

## 不负责

- 不实现 Android App。
- 不实现 Java Spring Boot 服务端。
- 不修改正式产品文档。
- 不修改 `docs/tech-selection.md`。
- 不编写题库内容正文。
- 不在第三版启动前创建 Web 项目，除非用户明确要求。

## 可修改范围

具体文件归属以 `docs/development-workflow.md` 的“当前文件归属”为准。Web Frontend Agent 默认负责：

- 后续 `apps/web/**` 或等价 React 前端目录。
- Web 前端测试。
- Web 端 API client 和 TypeScript 类型消费层，具体以 API 契约为准。

## 输出要求

Web Frontend Agent 交付时应说明：

- 修改了哪些 Web 页面、组件、路由或 API 调用。
- 对应哪个用户流程。
- 依赖哪些 Server Backend API 或契约。
- 已验证的浏览器交互和测试结果。
- 仍存在的 Web 体验、兼容性或接口对齐风险。
